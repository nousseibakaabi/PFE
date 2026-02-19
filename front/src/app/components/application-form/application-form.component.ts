import { Component, OnInit,HostListener } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ApplicationService, Application, ApplicationRequest, ApiResponse } from '../../services/application.service';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { WorkloadService, WorkloadDTO } from '../../services/workload.service'; // ADD THIS
import { Location } from '@angular/common';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-application-form',
  templateUrl: './application-form.component.html',
  styleUrls: ['./application-form.component.css']
})
export class ApplicationFormComponent implements OnInit {
  applicationId: number | null = null;
  isEditing = false;
  loading = false;
  errorMessage = '';
  successMessage = '';

  showChefDropdown = false;


  baseUrl = environment.baseUrl;
  Math = Math;

  // Current user info
  currentUser: any = null;
  isAdmin = false;
  isChefProjet = false;
  chefsProjet: any[] = [];
  
  // NEW: Workload data for chefs
  chefsWorkload: Map<number, WorkloadDTO> = new Map();
  workloadLoading = false;

  // Application form
  applicationForm: ApplicationRequest = {
    code: '',
    name: '',
    description: '',
    chefDeProjetId: null,
    clientName: '',
    clientEmail: '',
    clientPhone: '',
    clientAddress: '',
    minUser: 0,
    maxUser: 0,
    status: 'PLANIFIE'
  };

  // Status options
  statusOptions = [
    { value: 'PLANIFIE', label: 'Planifié' },
    { value: 'EN_COURS', label: 'En Cours' },
    { value: 'TERMINE', label: 'Terminé' },
    { value: 'SUSPENDU', label: 'Suspendu' },
    { value: 'ANNULE', label: 'Annulé' }
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private location: Location,
    private applicationService: ApplicationService,
    private userService: UserService,
    private authService: AuthService,
    private workloadService: WorkloadService // ADD THIS
  ) {}

  ngOnInit(): void {
    this.loadCurrentUser();
    
    // Check if we're editing
    this.route.params.subscribe(params => {
      if (params['id']) {
        this.applicationId = +params['id'];
        this.isEditing = true;
        this.loadApplication(this.applicationId);
      } else {
        // New application - load suggested code
        this.loadSuggestedApplicationCode();
      }
    });

    // Load chefs if admin
    if (this.isAdmin) {
      this.loadChefsProjet();
    }
  }

  loadCurrentUser(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.isAdmin = this.authService.isAdmin();
    this.isChefProjet = this.authService.isChefProjet();
    
    // For chef de projet creating new application, auto-assign themselves
    if (this.isChefProjet && !this.isAdmin && !this.isEditing) {
      this.applicationForm.chefDeProjetId = this.currentUser?.id;
    }
  }

  loadChefsProjet(): void {
    this.userService.getChefsProjet().subscribe({
      next: (response) => {
        if (response.success) {
          this.chefsProjet = response.data;
          this.loadChefsWorkload();
        }
      },
      error: (error) => {
        console.error('Error loading chefs:', error);
      }
    });
  }

  // NEW: Load workload for all chefs
  loadChefsWorkload(): void {
    this.workloadLoading = true;
    this.workloadService.getWorkloadDashboard().subscribe({
      next: (response) => {
        if (response.success && response.data?.workloads) {
          response.data.workloads.forEach((w: WorkloadDTO) => {
            this.chefsWorkload.set(w.chefId, w);
          });
        }
        this.workloadLoading = false;
      },
      error: (error) => {
        console.error('Error loading workload:', error);
        this.workloadLoading = false;
      }
    });
  }

  // NEW: Get workload for a specific chef
  getChefWorkload(chefId: number): WorkloadDTO | undefined {
    return this.chefsWorkload.get(chefId);
  }

  // NEW: Get workload color
  getWorkloadColor(workload: number): string {
    return this.workloadService.getWorkloadClass(workload);
  }

  // NEW: Get workload status
  getWorkloadStatus(workload: number): string {
    return this.workloadService.getWorkloadStatus(workload);
  }

  loadApplication(id: number): void {
    this.loading = true;
    this.applicationService.getApplication(id).subscribe({
      next: (response: ApiResponse) => {
        if (response.success) {
          const app = response.data;
          this.applicationForm = {
            code: app.code,
            name: app.name,
            description: app.description || '',
            chefDeProjetId: app.chefDeProjetId,
            clientName: app.clientName,
            clientEmail: app.clientEmail || '',
            clientPhone: app.clientPhone || '',
            clientAddress: app.clientAddress || '',
            dateDebut: app.dateDebut || '',
            dateFin: app.dateFin || '',
            minUser: app.minUser || 0,
            maxUser: app.maxUser || 0,
            status: app.status || 'PLANIFIE'
          };
          
          // Check if chef de projet can edit this
          if (this.isChefProjet && !this.isAdmin && app.chefDeProjetId !== this.currentUser?.id) {
            this.errorMessage = 'Vous ne pouvez modifier que vos propres applications';
            setTimeout(() => this.goBack(), 2000);
          }
        } else {
          this.errorMessage = 'Application non trouvée';
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading application:', error);
        this.errorMessage = 'Erreur lors du chargement';
        this.loading = false;
      }
    });
  }

  loadSuggestedApplicationCode(): void {
    this.applicationService.getSuggestedApplicationCode().subscribe({
      next: (response: any) => {
        if (response.success && response.suggestedCode) {
          this.applicationForm.code = response.suggestedCode;
        }
      },
      error: (error) => {
        console.error('Failed to load suggested code:', error);
      }
    });
  }

  saveApplication(): void {
    if (!this.validateForm()) return;

    this.loading = true;
    
    if (this.isEditing && this.applicationId) {
      this.applicationService.updateApplication(this.applicationId, this.applicationForm).subscribe({
        next: (response: ApiResponse) => {
          if (response.success) {
            this.successMessage = 'Application mise à jour avec succès';
            setTimeout(() => {
              this.router.navigate(['/applications', this.applicationId]);
            }, 1500);
          } else {
            this.errorMessage = response.message || 'Échec de la mise à jour';
          }
          this.loading = false;
        },
        error: (error) => {
          this.errorMessage = error.error?.message || 'Erreur lors de la mise à jour';
          this.loading = false;
        }
      });
    } else {
      this.applicationService.createApplication(this.applicationForm).subscribe({
        next: (response: ApiResponse) => {
          if (response.success) {
            this.successMessage = 'Application créée avec succès';
            setTimeout(() => {
              this.router.navigate(['/applications', response.data.id]);
            }, 1500);
          } else {
            this.errorMessage = response.message || 'Échec de la création';
          }
          this.loading = false;
        },
        error: (error) => {
          this.errorMessage = error.error?.message || 'Erreur lors de la création';
          this.loading = false;
        }
      });
    }
  }

  validateForm(): boolean {
    if (!this.applicationForm.code?.trim()) {
      this.errorMessage = 'Le code est requis';
      return false;
    }

    if (!this.validateCodeFormat()) {
      return false;
    }

    if (!this.applicationForm.name?.trim()) {
      this.errorMessage = 'Le nom est requis';
      return false;
    }

    if (!this.applicationForm.clientName?.trim()) {
      this.errorMessage = 'Le nom du client est requis';
      return false;
    }

    return true;
  }

  validateCodeFormat(): boolean {
    const pattern = /^APP-\d{4}-\d{3}$/;
    if (!pattern.test(this.applicationForm.code)) {
      this.errorMessage = 'Format invalide. Utilisez APP-AAAA-XXX (ex: APP-2024-001)';
      return false;
    }
    return true;
  }

  canReturnToAutoStatus(): boolean {
    if (!this.applicationForm.dateDebut) return false;
    const today = new Date();
    const startDate = new Date(this.applicationForm.dateDebut);
    return today >= startDate;
  }

  goBack(): void {
    this.location.back();
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  getStatusLabel(status: string): string {
    const option = this.statusOptions.find(opt => opt.value === status);
    return option ? option.label : status;
  }

  shouldShowChefField(): boolean {
    return this.isAdmin;
  }

  getCurrentChefName(): string {
    if (!this.currentUser) return '';
    return `${this.currentUser.firstName} ${this.currentUser.lastName}`;
  }


  // Add these methods for avatar handling (copy from application component)

getChefAvatarUrl(chef: any): string {
  if (!chef || !chef.profileImage) {
    let initials = '?';
    if (chef?.firstName && chef?.lastName) {
      initials = (chef.firstName[0] + chef.lastName[0]).toUpperCase();
    } else if (chef?.firstName) {
      initials = chef.firstName[0].toUpperCase();
    } else if (chef?.username) {
      initials = chef.username[0].toUpperCase();
    }
    return this.generateDefaultChefAvatar(initials);
  }
  
  const profileImage = chef.profileImage;
  if (profileImage.startsWith('http')) {
    return profileImage;
  }
  if (profileImage.startsWith('/uploads/')) {
    return this.baseUrl + profileImage;
  }
  if (profileImage.startsWith('data:image')) {
    return profileImage;
  }
  return this.baseUrl + '/uploads/avatars/' + profileImage;
}

generateDefaultChefAvatar(initials: string): string {
  const colors = ['#e9d709', '#3b82f6', '#10b981', '#8b5cf6', '#ec4899'];
  const colorIndex = initials.charCodeAt(0) % colors.length;
  const color = colors[colorIndex];
  
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="100" height="100">
    <rect width="100" height="100" rx="15" fill="${color}"/>
    <text x="50" y="58" text-anchor="middle" font-family="Arial, Helvetica, sans-serif" 
          font-size="38" font-weight="bold" fill="white" dominant-baseline="middle">
      ${initials}
    </text>
  </svg>`;
  
  return 'data:image/svg+xml;base64,' + btoa(svg);
}

handleChefImageError(event: any, chef: any): void {
  let initials = '?';
  if (chef?.firstName && chef?.lastName) {
    initials = (chef.firstName[0] + chef.lastName[0]).toUpperCase();
  } else if (chef?.firstName) {
    initials = chef.firstName[0].toUpperCase();
  } else if (chef?.username) {
    initials = chef.username[0].toUpperCase();
  }
  event.target.src = this.generateDefaultChefAvatar(initials);
  event.target.onerror = null;
}
  
getWorkloadClass(workload: number): string {
  if (workload >= 90) return 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300';
  if (workload >= 70) return 'bg-orange-100 text-orange-800 dark:bg-orange-900/30 dark:text-orange-300';
  if (workload >= 40) return 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300';
  return 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300';
}



// Add these methods
toggleChefDropdown(): void {
  this.showChefDropdown = !this.showChefDropdown;
}


selectChef(chef: any | null): void {
  this.applicationForm.chefDeProjetId = chef ? chef.id : null; // ✅
  this.showChefDropdown = false;
}

getSelectedChef(): any | null {
  if (this.applicationForm.chefDeProjetId === null) return null;
  return this.chefsProjet.find(c => c.id === this.applicationForm.chefDeProjetId) || null;
}


// Add click outside to close dropdown
@HostListener('document:click', ['$event'])
onDocumentClick(event: MouseEvent): void {
  const target = event.target as HTMLElement;
  if (!target.closest('.relative')) {
    this.showChefDropdown = false;
  }
}
}