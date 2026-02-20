import { Component, OnInit } from '@angular/core';
import { ApplicationService, Application, ApplicationRequest } from '../../services/application.service';
import { AuthService } from '../../services/auth.service';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-admin-application',
  templateUrl: './admin-application.component.html',
  styleUrls: ['./admin-application.component.css'],
  standalone: false
})
export class AdminApplicationComponent implements OnInit {
  applications: Application[] = [];
  filteredApplications: Application[] = [];
  chefsProjet: any[] = []; // Users with ROLE_CHEF_PROJET
  loading = false;
  errorMessage = '';
  successMessage = '';
  searchTerm = '';
  filterStatus = '';

  // Track current user role
  currentUser: any = null;
  isAdmin = false;
  isChefProjet = false;

  // Modals
  showAddModal = false;
  showEditModal = false;
  showDeleteModal = false;
  selectedApplication: Application | null = null;

  // Application form
  applicationForm: ApplicationRequest = {
    code: '',
    name: '',
    description: '',
    chefDeProjetId: 0,
    clientName: '',
    clientEmail: '',
    clientPhone: '',
    dateDebut: '',
    dateFin: '',
    minUser : 0,
  maxUser:0,
    status: 'PLANIFIE'
  };

  statusOptions = [
    { value: 'PLANIFIE', label: 'Planifié' },
    { value: 'EN_COURS', label: 'En Cours' },
    { value: 'TERMINE', label: 'Terminé' },
  ];

  constructor(
    private applicationService: ApplicationService,
    private authService: AuthService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    this.loadCurrentUser();
    this.loadApplications();
    
    // Only load chefs if user is admin
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
      next: (response) => {
        if (response.success) {
          this.applications = response.data;
          
          // If user is chef de projet, filter only their applications
          if (this.isChefProjet && this.currentUser) {
            this.applications = this.applications.filter(application => 
              application.chefDeProjetId === this.currentUser.id
            );
          }
          
          this.filteredApplications = [...this.applications];
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading applications:', error);
        this.errorMessage = error.error?.message || 'Failed to load applications';
        this.loading = false;
      }
    });
  }

  loadChefsProjet(): void {
    // Only load chefs if user is admin
    if (!this.isAdmin) return;
    
    this.userService.getChefsProjet().subscribe({
      next: (response) => {
        if (response.success) {
          this.chefsProjet = response.data;
        }
      },
      error: (error) => {
        console.error('Error loading chefs de projet:', error);
        this.errorMessage = 'Failed to load chefs de projet';
      }
    });
  }

  searchApplications(): void {
    if (!this.searchTerm.trim()) {
      this.filteredApplications = this.filterByStatus(this.applications);
      return;
    }

    const term = this.searchTerm.toLowerCase();
    this.filteredApplications = this.filterByStatus(this.applications).filter(application =>
      application.code.toLowerCase().includes(term) ||
      application.name.toLowerCase().includes(term) ||
      application.clientName.toLowerCase().includes(term) ||
      application.description?.toLowerCase().includes(term)
    );
  }

  filterByStatus(applications: Application[]): Application[] {
    if (!this.filterStatus) {
      return applications;
    }
    return applications.filter(application => application.status === this.filterStatus);
  }

  applyFilters(): void {
    this.filteredApplications = this.filterByStatus(this.applications);
    if (this.searchTerm.trim()) {
      this.searchApplications();
    }
  }

  openAddModal(): void {
    this.applicationForm = {
      code: '',
      name: '',
      description: '',
      chefDeProjetId: this.isChefProjet && this.currentUser ? this.currentUser.id : 0, // Optional now
      clientName: '',
      clientEmail: '',
      clientPhone: '',
      minUser : 0,
  maxUser:0,
      status: 'PLANIFIE'
    };
    this.loadSuggestedApplicationCode();
    this.showAddModal = true;
    this.errorMessage = '';
  }

  openEditModal(application: Application): void {
    this.selectedApplication = application;
    this.applicationForm = {
      code: application.code,
      name: application.name,
      description: application.description || '',
      chefDeProjetId: application.chefDeProjetId,
      clientName: application.clientName,
      clientEmail: application.clientEmail || '',
      clientPhone: application.clientPhone || '',
      dateDebut: application.dateDebut || '',
      dateFin: application.dateFin || '',
      minUser: application.minUser || 0,
      maxUser: application.maxUser || 0,
      status: application.status || 'PLANIFIE'
    };
    
    this.showEditModal = true;
    this.errorMessage = '';
  }

  openDeleteModal(application: Application): void {
    this.selectedApplication = application;
    this.showDeleteModal = true;
  }

  closeModal(): void {
    this.showAddModal = false;
    this.showEditModal = false;
    this.showDeleteModal = false;
    this.selectedApplication = null;
    this.errorMessage = '';
  }

  createApplication(): void {
    if (!this.validateApplicationForm()) {
      return;
    }

    this.applicationForm.status = 'PLANIFIE';
    
    this.loading = true;
    this.applicationService.createApplication(this.applicationForm).subscribe({
      next: (response) => {
        if (response.success) {
          this.successMessage = 'Application créée avec succès';
          this.loadApplications();
          this.closeModal();
        }
        this.loading = false;
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Échec de la création de l\'application';
        this.loading = false;
      }
    });
  }

  updateApplication(): void {
    if (!this.selectedApplication || !this.validateApplicationForm()) {
      return;
    }

    this.loading = true;
    this.applicationService.updateApplication(this.selectedApplication.id, this.applicationForm).subscribe({
      next: (response) => {
        if (response.success) {
          this.successMessage = 'Application mise à jour avec succès';
          this.loadApplications();
          this.closeModal();
        }
        this.loading = false;
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Échec de la mise à jour de l\'application';
        this.loading = false;
      }
    });
  }

  deleteApplication(): void {
    if (!this.selectedApplication) return;

    this.loading = true;
    this.applicationService.deleteApplication(this.selectedApplication.id).subscribe({
      next: (response) => {
        if (response.success) {
          this.successMessage = 'Application supprimée avec succès';
          this.loadApplications();
          this.closeModal();
        }
        this.loading = false;
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Échec de la suppression de l\'application';
        this.loading = false;
      }
    });
  }

  validateApplicationForm(): boolean {
    if (!this.applicationForm.code.trim()) {
      this.errorMessage = 'Le code de l\'application est requis';
      return false;
    }
    if (!this.applicationForm.name.trim()) {
      this.errorMessage = 'Le nom de l\'application est requis';
      return false;
    }
    if (!this.applicationForm.clientName.trim()) {
      this.errorMessage = 'Le nom du client est requis';
      return false;
    }
    
    return true;
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'PLANIFIE': return 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300';
      case 'EN_COURS': return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300';
      case 'TERMINE': return 'bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-300';
      default: return 'bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-300';
    }
  }

  getStatusLabel(status: string): string {
    const option = this.statusOptions.find(opt => opt.value === status);
    return option ? option.label : status;
  }

  isAd(): boolean {
    return this.authService.isAdmin();
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

  // Show chef de projet field only for admin
  shouldShowChefField(): boolean {
    return this.isAdmin;
  }

  // Get current chef de projet name for display
  getCurrentChefName(): string {
    if (!this.currentUser) return '';
    return `${this.currentUser.firstName} ${this.currentUser.lastName}`;
  }

  loadSuggestedApplicationCode(): void {
    this.applicationService.getSuggestedApplicationCode().subscribe({
      next: (response: any) => {
        if (response.success && response.suggestedCode) {
          // Direct assignment instead of patchValue
          this.applicationForm.code = response.suggestedCode;
        }
      },
      error: (error) => {
        console.error('Failed to load suggested code:', error);
      }
    });
  }

  validateApplicationCode(): boolean {
    const code = this.applicationForm.code; 
    const pattern = /^APP-\d{4}-\d{3}$/;
    
    if (!pattern.test(code)) {
      this.errorMessage = 'Format de code invalide. Utilisez APP-AAAA-XXX (ex: APP-2024-001)';
      return false;
    }
    return true;
  }
}