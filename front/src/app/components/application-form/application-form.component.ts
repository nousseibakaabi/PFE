// In application-form.component.ts - Add missing properties and methods

import { Component, OnInit, HostListener } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ApplicationService, Application, ApplicationRequest, ApiResponse } from '../../services/application.service';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { WorkloadService, WorkloadDTO } from '../../services/workload.service';
import { Location } from '@angular/common';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-application-form',
  templateUrl: './application-form.component.html',
  styleUrls: ['./application-form.component.css']
})
export class ApplicationFormComponent implements OnInit {
  applicationId: number | null = null;
  // ADD THIS: application property to store the loaded application
  application: Application | null = null;


  codeValid = false;
codeInvalid = false;
nameValid = false;
nameInvalid = false;
clientValid = false;
clientInvalid = false;
emailValid = false;
emailInvalid = false;
phoneValid = false;
phoneInvalid = false;

codeExists = false;
checkingCode = false;
codeCheckTimeout: any;

  
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
  
  // Workload data for chefs
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
    dateDebut: '',
    dateFin: '',
    minUser: 0,
    maxUser: 0,
    status: 'PLANIFIE'
  };

  // Status options
  statusOptions = [
    { value: 'PLANIFIE', label: 'Planifié' },
    { value: 'EN_COURS', label: 'En Cours' },
    { value: 'TERMINE', label: 'Terminé' },
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private location: Location,
    private applicationService: ApplicationService,
    private userService: UserService,
    private authService: AuthService,
    private workloadService: WorkloadService
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

  getChefWorkload(chefId: number): WorkloadDTO | undefined {
    return this.chefsWorkload.get(chefId);
  }

  getWorkloadColor(workload: number): string {
    return this.workloadService.getWorkloadClass(workload);
  }

  getWorkloadStatus(workload: number): string {
    return this.workloadService.getWorkloadStatus(workload);
  }

  loadApplication(id: number): void {
    this.loading = true;
    this.applicationService.getApplication(id).subscribe({
      next: (response: ApiResponse) => {
        if (response.success) {
          // Store the full application object
          this.application = response.data;
          
          const app = response.data;
          this.applicationForm = {
            code: app.code,
            name: app.name,
            description: app.description || '',
            chefDeProjetId: app.chefDeProjetId,
            clientName: app.clientName,
            clientEmail: app.clientEmail || '',
            clientPhone: app.clientPhone || '',
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
    // Check if we're setting status to TERMINE
    if (this.applicationForm.status === 'TERMINE') {
      // Use special termination method
      this.applicationService.manuallyTerminateApplication(
        this.applicationId, 
        'Terminé via formulaire'
      ).subscribe({
        next: (response: any) => {
          if (response.success) {
            this.successMessage = 'Application marquée comme terminée avec succès';
            
            // Show termination details
            if (response.terminationInfo) {
              const info = response.terminationInfo;
              let terminationDetail = '';
              
              if (info.daysRemaining > 0) {
                terminationDetail = `Terminée ${info.daysRemaining} jours avant l'échéance`;
              } else if (info.daysRemaining < 0) {
                terminationDetail = `Terminée ${Math.abs(info.daysRemaining)} jours après l'échéance`;
              } else if (info.daysRemaining === 0) {
                terminationDetail = 'Terminée le jour de l\'échéance';
              }
              
              if (terminationDetail) {
                this.successMessage += ` - ${terminationDetail}`;
              }
            }
            
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
      // Regular update for non-TERMINE status
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
    }
  } else {
    // Create new application
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

  // Avatar handling methods
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

  toggleChefDropdown(): void {
    this.showChefDropdown = !this.showChefDropdown;
  }

  selectChef(chef: any | null): void {
    this.applicationForm.chefDeProjetId = chef ? chef.id : null;
    this.showChefDropdown = false;
  }

  getSelectedChef(): any | null {
    if (this.applicationForm.chefDeProjetId === null) return null;
    return this.chefsProjet.find(c => c.id === this.applicationForm.chefDeProjetId) || null;
  }

  // ADD THIS: Check if application can be terminated
  checkCanTerminate(application: Application | null): boolean {
    return !!(application && (application.status === 'PLANIFIE' || application.status === 'EN_COURS'));
  }

  // ADD THIS: Terminate application method
  terminateApplication(): void {
    if (!this.application || !this.checkCanTerminate(this.application)) {
      this.errorMessage = 'Cette application ne peut pas être terminée';
      return;
    }
    
    // Optional: Show prompt with reason
    const reason = prompt('Raison de la terminaison (optionnelle):');
    
    if (this.applicationId === null) {
      this.errorMessage = 'ID d\'application invalide';
      return;
    }
    
    this.loading = true;
    this.applicationService.manuallyTerminateApplication(this.applicationId, reason || undefined).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.successMessage = 'Application marquée comme terminée avec succès';
          
          // Show termination details
          if (response.terminationInfo) {
            const info = response.terminationInfo;
            let terminationDetail = '';
            
            if (info.daysRemaining > 0) {
              terminationDetail = `Terminée ${info.daysRemaining} jours avant l'échéance`;
            } else if (info.daysRemaining < 0) {
              terminationDetail = `Terminée ${Math.abs(info.daysRemaining)} jours après l'échéance`;
            } else if (info.daysRemaining === 0) {
              terminationDetail = 'Terminée le jour de l\'échéance';
            }
            
            if (terminationDetail) {
              this.successMessage += ` - ${terminationDetail}`;
            }
          }
          
          // Reload application data
          if (this.applicationId) {
            this.loadApplication(this.applicationId);
          }
        } else {
          this.errorMessage = response.message || 'Erreur lors de la termination';
        }
        this.loading = false;
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Erreur lors de la termination';
        this.loading = false;
      }
    });
  }

 

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.relative')) {
      this.showChefDropdown = false;
    }
  }




validateName(): boolean {
  const isValid = this.applicationForm.name?.trim().length >= 3;
  this.nameValid = isValid;
  this.nameInvalid = !isValid && this.applicationForm.name?.trim().length > 0;
  return isValid;
}

validateClient(): boolean {
  const isValid = this.applicationForm.clientName?.trim().length > 0;
  this.clientValid = isValid;
  this.clientInvalid = !isValid && this.applicationForm.clientName?.trim().length > 0;
  return isValid;
}

validateEmail(): boolean {
  if (!this.applicationForm.clientEmail) {
    this.emailValid = false;
    this.emailInvalid = false;
    return true;
  }
  const pattern = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
  const isValid = pattern.test(this.applicationForm.clientEmail);
  this.emailValid = isValid;
  this.emailInvalid = !isValid;
  return isValid;
}

validatePhone(): boolean {
  if (!this.applicationForm.clientPhone) {
    this.phoneValid = false;
    this.phoneInvalid = false;
    return true;
  }
  const pattern = /^[0-9+\-\s]{8,15}$/;
  const isValid = pattern.test(this.applicationForm.clientPhone);
  this.phoneValid = isValid;
  this.phoneInvalid = !isValid;
  return isValid;
}



onCodeChange(): void {
  // Clear previous timeout
  if (this.codeCheckTimeout) {
    clearTimeout(this.codeCheckTimeout);
  }
  
  // First, validate format
  this.validateCodeFormat();
  
  // If format is invalid, don't check existence
  if (this.codeInvalid || !this.applicationForm.code) {
    this.codeExists = false;
    return;
  }
  
  // Debounce API call (wait 500ms after user stops typing)
  this.codeCheckTimeout = setTimeout(() => {
    this.checkCodeExists();
  }, 500);
}

// Validate code format
validateCodeFormat(): boolean {
  const pattern = /^APP-\d{4}-\d{3}$/;
  const isValid = pattern.test(this.applicationForm.code);
  this.codeValid = isValid;
  this.codeInvalid = !isValid && this.applicationForm.code.length > 0;
  return isValid;
}

// Check if code already exists
checkCodeExists(): void {
  if (!this.applicationForm.code || this.codeInvalid) {
    return;
  }
  
  this.checkingCode = true;
  this.applicationService.checkApplicationCodeExists(this.applicationForm.code).subscribe({
    next: (response: any) => {
      this.codeExists = response.exists === true;
      this.checkingCode = false;
    },
    error: (error) => {
      console.error('Error checking code:', error);
      this.codeExists = false;
      this.checkingCode = false;
    }
  });
}

// Update isFormValid to include code existence check
// User limit validation and increment/decrement methods
userLimitError = '';

validateMinMaxUsers(): void {
  const min = this.applicationForm.minUser || 0;
  const max = this.applicationForm.maxUser || 0;
  
  if (min <= 0) {
    this.userLimitError = 'Le minimum doit être supérieur à 0';
    return;
  }
  if (max <= 0) {
    this.userLimitError = 'Le maximum doit être supérieur à 0';
    return;
  }
  if (min > max) {
    this.userLimitError = 'Le minimum ne peut pas être supérieur au maximum';
    return;
  }
  this.userLimitError = '';
}

incrementMinUser(): void {
  const current = this.applicationForm.minUser || 1;
  this.applicationForm.minUser = current + 1;
  this.validateMinMaxUsers();
}

decrementMinUser(): void {
  const current = this.applicationForm.minUser || 1;
  if (current > 1) {
    this.applicationForm.minUser = current - 1;
    this.validateMinMaxUsers();
  }
}

incrementMaxUser(): void {
  const current = this.applicationForm.maxUser || 1;
  this.applicationForm.maxUser = current + 1;
  this.validateMinMaxUsers();
}

decrementMaxUser(): void {
  const current = this.applicationForm.maxUser || 1;
  if (current > 1) {
    this.applicationForm.maxUser = current - 1;
    this.validateMinMaxUsers();
  }
}

// Update isFormValid to include user limit validation
isFormValid(): boolean {
  if (!this.validateCodeFormat()) return false;
  if (this.codeExists) return false;
  if (!this.validateName()) return false;
  if (!this.validateClient()) return false;
  if (!this.validateEmail()) return false;
  if (!this.validatePhone()) return false;
  if (this.userLimitError) return false;
  return true;
}

// Add getChefInitials method
getChefInitials(chef: any): string {
  if (!chef) return '?';
  let initials = '';
  if (chef.firstName) initials += chef.firstName.charAt(0).toUpperCase();
  if (chef.lastName) initials += chef.lastName.charAt(0).toUpperCase();
  return initials || '?';
}

}



