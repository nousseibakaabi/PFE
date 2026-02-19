import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ApplicationService, Application, ApiResponse } from '../../services/application.service';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { WorkloadService, WorkloadDTO } from '../../services/workload.service'; // ADD THIS
import { DatePipe } from '@angular/common';
import { environment } from 'src/environments/environment';

@Component({
  selector: 'app-application',
  templateUrl: './application.component.html',
  styleUrls: ['./application.component.css'],
  providers: [DatePipe]
})
export class ApplicationComponent implements OnInit {
  applications: Application[] = [];
  filteredApplications: Application[] = [];
  chefsProjet: any[] = []; // Only for admin view
  
  loading = false;
  workloadLoading = false;
  errorMessage = '';
  successMessage = '';
  searchTerm = '';
  filterStatus = '';

  // Current user info
  currentUser: any = null;
  isAdmin = false;
  isChefProjet = false;

  showAssignModal = false;
  selectedAppForAssign: Application | null = null;
  availableChefs: any[] = [];
  selectedChefId: number | null = null;
  assigning = false;
  
  // NEW: Workload check properties
  workloadCheck: any = null;
  showWorkloadWarning = false;
  alternativeChefs: any[] = [];
  workloadLoadingForChef = false;
  forceAssignMode = false;
  chefsWorkload: Map<number, any> = new Map();


  selectedApplication: Application | null = null;

  baseUrl = environment.baseUrl;

  Math = Math; 

  // Status options
  statusOptions = [
    { value: 'PLANIFIE', label: 'Planifié' },
    { value: 'EN_COURS', label: 'En Cours' },
    { value: 'TERMINE', label: 'Terminé' },
    { value: 'SUSPENDU', label: 'Suspendu' },
    { value: 'ANNULE', label: 'Annulé' }
  ];

  constructor(
    private applicationService: ApplicationService,
    private userService: UserService,
    private authService: AuthService,
    private router: Router,
    private datePipe: DatePipe,
    private workloadService: WorkloadService // ADD THIS
  ) {}

  ngOnInit(): void {
    this.loadCurrentUser();
    this.loadApplications();
    
    if (this.isAdmin) {
      this.loadChefsProjet();
    }
  }

  loadCurrentUser(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.isAdmin = this.authService.isAdmin();
    this.isChefProjet = this.authService.isChefProjet();
  }

  loadApplications(): void {
    this.loading = true;
    this.applicationService.getAllApplications().subscribe({
      next: (response: ApiResponse) => {
        if (response.success) {
          let allApps = response.data || response.applications || [];
          
          // Filter for chef de projet
          if (this.isChefProjet && !this.isAdmin && this.currentUser) {
            this.applications = allApps.filter((app: Application) => 
              app.chefDeProjetId === this.currentUser.id
            );
          } else {
            this.applications = allApps;
          }
          
          this.filteredApplications = [...this.applications];
        } else {
          this.errorMessage = response.message || 'Erreur lors du chargement';
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading applications:', error);
        this.errorMessage = 'Erreur de connexion au serveur';
        this.loading = false;
      }
    });
  }

  loadChefsProjet(): void {
    if (!this.isAdmin) return;
    
    this.userService.getChefsProjet().subscribe({
      next: (response) => {
        if (response.success) {
          this.chefsProjet = response.data;
          // Load workload data for all chefs
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

  // NEW: Get progress bar class
  getWorkloadBarClass(workload: number): string {
    return this.workloadService.getProgressBarClass(workload);
  }

  // NEW: Get workload status
  getWorkloadStatus(workload: number): string {
    return this.workloadService.getWorkloadStatus(workload);
  }

  searchApplications(): void {
    if (!this.searchTerm.trim()) {
      this.filteredApplications = this.filterByStatus(this.applications);
      return;
    }

    const term = this.searchTerm.toLowerCase();
    this.filteredApplications = this.filterByStatus(this.applications).filter((app: Application) =>
      app.code?.toLowerCase().includes(term) ||
      app.name?.toLowerCase().includes(term) ||
      app.clientName?.toLowerCase().includes(term) ||
      app.description?.toLowerCase().includes(term)
    );
  }

  filterByStatus(applications: Application[]): Application[] {
    if (!this.filterStatus) {
      return applications;
    }
    return applications.filter((app: Application) => app.status === this.filterStatus);
  }

  applyFilters(): void {
    this.filteredApplications = this.filterByStatus(this.applications);
    if (this.searchTerm.trim()) {
      this.searchApplications();
    }
  }

  // Navigation methods
  viewApplication(id: number): void {
    this.router.navigate(['/applications', id]);
  }

  createApplication(): void {
    this.router.navigate(['/applications/new']);
  }

  editApplication(application: Application): void {
    // For chef de projet, check ownership
    if (this.isChefProjet && !this.isAdmin && application.chefDeProjetId !== this.currentUser?.id) {
      this.errorMessage = 'Vous ne pouvez modifier que vos propres applications';
      return;
    }
    this.router.navigate(['/applications/edit', application.id]);
  }

  // Utility methods
  getStatusClass(status: string): string {
    switch (status) {
      case 'PLANIFIE': return 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300';
      case 'EN_COURS': return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300';
      case 'TERMINE': return 'bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-300';
      case 'SUSPENDU': return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-300';
      case 'ANNULE': return 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-300';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  getStatusLabel(status: string): string {
    const option = this.statusOptions.find(opt => opt.value === status);
    return option ? option.label : status;
  }

  formatDate(dateString: string): string {
    if (!dateString) return '-';
    return this.datePipe.transform(dateString, 'dd/MM/yyyy') || '-';
  }

  // Statistics methods
  getTotalApplicationsCount(): number {
    return this.applications.length;
  }

  getEnCoursCount(): number {
    return this.applications.filter(a => a.status === 'EN_COURS').length;
  }

  getPlanifieCount(): number {
    return this.applications.filter(a => a.status === 'PLANIFIE').length;
  }

  getDelayedCount(): number {
    return this.applications.filter(a => a.isDelayed).length;
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  isApplicationDelayed(application: Application): boolean {
    if (!application.dateFin) return false;
    const today = new Date();
    const endDate = new Date(application.dateFin);
    return today > endDate && application.status === 'EN_COURS';
  }

  loadAvailableChefs(): void {
    this.userService.getChefsProjet().subscribe({
      next: (response) => {
        if (response.success) {
          this.availableChefs = response.data;
          // Load workload data for available chefs
          if (!this.chefsWorkload.size) {
            this.loadChefsWorkload();
          }
        }
      },
      error: (error) => {
        console.error('Error loading chefs:', error);
        this.errorMessage = 'Failed to load chefs';
      }
    });
  }

// Update openAssignModal method
openAssignModal(application: Application): void {
  this.selectedAppForAssign = application;
  this.loadAvailableChefs();
  
  // Also load workload dashboard to get workload data for chefs
  this.workloadService.getWorkloadDashboard().subscribe({
    next: (response) => {
      if (response.success && response.data?.workloads) {
        response.data.workloads.forEach((w: any) => {
          this.chefsWorkload.set(w.chefId, w);
        });
      }
    },
    error: (error) => {
      console.error('Error loading workload:', error);
    }
  });
  
  this.selectedChefId = null;
  this.workloadCheck = null;
  this.showWorkloadWarning = false;
  this.alternativeChefs = [];
  this.forceAssignMode = false;
  this.showAssignModal = true;
}

  // Close assign modal
  closeAssignModal(): void {
    this.showAssignModal = false;
    this.selectedAppForAssign = null;
    this.availableChefs = [];
    this.selectedChefId = null;
    this.assigning = false;
    this.workloadCheck = null;
    this.showWorkloadWarning = false;
    this.alternativeChefs = [];
    this.forceAssignMode = false;
  }

  // Select a chef and check workload
  selectChef(chef: any): void {
    this.selectedChefId = chef.id;
    this.forceAssignMode = false;
    
    if (this.selectedAppForAssign) {
      this.checkWorkload(chef.id);
    }
  }

  // NEW: Check workload for selected chef
  checkWorkload(chefId: number): void {
    if (!this.selectedAppForAssign) return;
    
    this.workloadLoadingForChef = true;
    this.workloadService.checkAssignment(chefId, this.selectedAppForAssign.id).subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.workloadCheck = response.data;
          this.showWorkloadWarning = !response.data.canAssign && !response.data.assignWithCaution;
          this.alternativeChefs = response.data.alternativeChefs || [];
        }
        this.workloadLoadingForChef = false;
      },
      error: (error) => {
        console.error('Error checking workload:', error);
        this.workloadLoadingForChef = false;
      }
    });
  }

  // NEW: Assign chef with workload check
  assignChef(): void {
    if (!this.selectedChefId || !this.selectedAppForAssign) return;
    
    this.assigning = true;
    
    this.workloadService.assignWithWorkloadCheck(
      this.selectedChefId,
      this.selectedAppForAssign.id,
      this.forceAssignMode
    ).subscribe({
      next: (response) => {
        if (response.success) {
          this.successMessage = response.warning 
            ? 'Chef de projet assigné (charge élevée)'
            : 'Chef de projet assigné avec succès';
          this.loadApplications(); // Reload the list
          this.closeAssignModal();
          
          // Refresh workload data
          this.loadChefsWorkload();
        } else {
          this.errorMessage = response.message || 'Failed to assign chef';
        }
        this.assigning = false;
      },
      error: (error) => {
        console.error('Error assigning chef:', error);
        this.errorMessage = error.error?.message || 'Failed to assign chef';
        this.assigning = false;
      }
    });
  }

  // NEW: Toggle force assign mode
  toggleForceAssign(): void {
    this.forceAssignMode = !this.forceAssignMode;
  }

  // Get chef avatar URL for the table
  getChefAvatarUrlForApp(app: Application): string {
    if (!app.chefDeProjetFullName) {
      return this.generateDefaultChefAvatar('?');
    }
    
    if (app.chefDeProjetProfileImage) {
      const profileImage = app.chefDeProjetProfileImage;
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
    
    let initials = '?';
    const names = app.chefDeProjetFullName.split(' ');
    if (names.length >= 2) {
      initials = (names[0][0] + names[1][0]).toUpperCase();
    } else if (names.length === 1 && names[0]) {
      initials = names[0][0].toUpperCase();
    }
    
    return this.generateDefaultChefAvatar(initials);
  }

  // Get chef avatar URL for modal
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

  // Generate default chef avatar with initials
  generateDefaultChefAvatar(initials: string): string {
    const colors = ['#e9d709'];
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

  // Error handler for chef images in table
  handleChefImageErrorForApp(event: any, app: Application): void {
    let initials = '?';
    if (app.chefDeProjetFullName) {
      const names = app.chefDeProjetFullName.split(' ');
      if (names.length >= 2) {
        initials = (names[0][0] + names[1][0]).toUpperCase();
      } else if (names.length === 1) {
        initials = names[0][0].toUpperCase();
      }
    }
    event.target.src = this.generateDefaultChefAvatar(initials);
    event.target.onerror = null;
  }

  // Error handler for chef images in modal
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
}