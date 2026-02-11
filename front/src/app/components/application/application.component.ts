import { Component, OnInit } from '@angular/core';
import { ApplicationService, Application, ApplicationRequest, ApiResponse } from '../../services/application.service';
import { NomenclatureService } from '../../services/nomenclature.service';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'app-application',
  templateUrl: './application.component.html',
  styleUrls: ['./application.component.css'],
  providers: [DatePipe]
})
export class ApplicationComponent implements OnInit {
  // Use applications array instead of projects
  applications: Application[] = [];
  filteredApplications: Application[] = [];
  loading = false;
  errorMessage = '';
  successMessage = '';
  searchTerm = '';
  filterStatus = '';

  // Current user info
  currentUser: any = null;

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
    clientAddress: '',
    dateDebut: '',
    dateFin: '',
    minUser:0,
    maxUser:0,
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
    private applicationService: ApplicationService,
    private nomenclatureService: NomenclatureService,
    private authService: AuthService,
    private datePipe: DatePipe
  ) {}

  ngOnInit(): void {
    this.loadCurrentUser();
    this.loadApplications();
  }

  loadCurrentUser(): void {
    this.currentUser = this.authService.getCurrentUser();
    if (this.currentUser) {
      this.applicationForm.chefDeProjetId = this.currentUser.id;
    }
  }

  loadApplications(): void {
    this.loading = true;
    this.applicationService.getAllApplications().subscribe({
      next: (response: ApiResponse) => {
        if (response.success) {
          // Check if data is in response.data or response.applications
          this.applications = response.data || response.applications || [];
          this.filteredApplications = [...this.applications];
        } else {
          this.errorMessage = response.message || 'Erreur lors du chargement des applications';
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

  searchApplications(): void {
    if (!this.searchTerm.trim()) {
      this.filteredApplications = this.filterByStatus(this.applications);
      return;
    }

    const term = this.searchTerm.toLowerCase();
    this.filteredApplications = this.filterByStatus(this.applications).filter((application: Application) =>
      application.code?.toLowerCase().includes(term) ||
      application.name?.toLowerCase().includes(term) ||
      application.clientName?.toLowerCase().includes(term) ||
      application.description?.toLowerCase().includes(term)
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

  openAddModal(): void {
    this.applicationForm = {
      code: '',
      name: '',
      description: '',
      chefDeProjetId: this.currentUser?.id || 0,
      clientName: '',
      clientEmail: '',
      clientPhone: '',
      clientAddress: '',
      dateDebut: new Date().toISOString().split('T')[0],
      dateFin: '',
      minUser:0,
      maxUser:0,
      status: 'PLANIFIE'
    };
    
    this.loadSuggestedApplicationCode();
    this.showAddModal = true;
    this.errorMessage = '';
  }

  openEditModal(application: Application): void {
    if (application.chefDeProjetId !== this.currentUser?.id) {
      this.errorMessage = 'Vous ne pouvez modifier que vos propres applications';
      return;
    }

    this.selectedApplication = application;
    this.applicationForm = {
      code: application.code,
      name: application.name,
      description: application.description || '',
      chefDeProjetId: application.chefDeProjetId,
      clientName: application.clientName,
      clientEmail: application.clientEmail || '',
      clientPhone: application.clientPhone || '',
      clientAddress: application.clientAddress || '',
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
    if (application.chefDeProjetId !== this.currentUser?.id) {
      this.errorMessage = 'Vous ne pouvez supprimer que vos propres applications';
      return;
    }
    
    this.selectedApplication = application;
    this.showDeleteModal = true;
  }

  closeModal(): void {
    this.showAddModal = false;
    this.showEditModal = false;
    this.showDeleteModal = false;
    this.selectedApplication = null;
    this.errorMessage = '';
    this.successMessage = '';
  }

  createApplication(): void {
    if (!this.validateApplicationForm()) {
      return;
    }

    this.applicationForm.chefDeProjetId = this.currentUser?.id;
    this.applicationForm.status = 'PLANIFIE';

    this.loading = true;
    this.applicationService.createApplication(this.applicationForm).subscribe({
      next: (response: ApiResponse) => {
        if (response.success) {
          this.successMessage = 'Application créée avec succès';
          this.closeModal();
          this.loadApplications(); // Reload the list
        } else {
          this.errorMessage = response.message || 'Échec de la création de l\'application';
        }
        this.loading = false;
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Erreur lors de la création de l\'application';
        this.loading = false;
      }
    });
  }

  updateApplication(): void {
    if (!this.selectedApplication || !this.validateApplicationForm()) {
      return;
    }

    if (this.selectedApplication.status === 'TERMINE') {
      this.errorMessage = 'Une application terminée ne peut pas être modifiée';
      return;
    }
    
    this.loading = true;
    this.applicationService.updateApplication(this.selectedApplication.id, this.applicationForm).subscribe({
      next: (response: ApiResponse) => {
        if (response.success) {
          this.successMessage = 'Application mise à jour avec succès';
          this.closeModal();
          this.loadApplications(); // Reload the list
        } else {
          this.errorMessage = response.message || 'Échec de la mise à jour de l\'application';
        }
        this.loading = false;
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Erreur lors de la mise à jour de l\'application';
        this.loading = false;
      }
    });
  }

  deleteApplication(): void {
    if (!this.selectedApplication) return;

    this.loading = true;
    this.applicationService.deleteApplication(this.selectedApplication.id).subscribe({
      next: (response: ApiResponse) => {
        if (response.success) {
          this.successMessage = 'Application supprimée avec succès';
          this.closeModal();
          this.loadApplications(); // Reload the list
        } else {
          this.errorMessage = response.message || 'Échec de la suppression de l\'application';
        }
        this.loading = false;
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Erreur lors de la suppression de l\'application';
        this.loading = false;
      }
    });
  }

  validateApplicationForm(): boolean {
    if (!this.applicationForm.code?.trim()) {
      this.errorMessage = 'Le code de l\'application est requis';
      return false;
    }
    
    if (!this.validateApplicationCode()) {
      return false;
    }
    
    if (!this.applicationForm.name?.trim()) {
      this.errorMessage = 'Le nom de l\'application est requis';
      return false;
    }
    
    if (!this.applicationForm.clientName?.trim()) {
      this.errorMessage = 'Le nom du client est requis';
      return false;
    }
    
    // Validate dates
    if (this.applicationForm.dateDebut && this.applicationForm.dateFin) {
      const debut = new Date(this.applicationForm.dateDebut);
      const fin = new Date(this.applicationForm.dateFin);
      
      if (fin < debut) {
        this.errorMessage = 'La date de fin ne peut pas être antérieure à la date de début';
        return false;
      }
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
      case 'SUSPENDU': return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-300';
      case 'ANNULE': return 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-300';
      default: return 'bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-300';
    }
  }

  getStatusLabel(status: string): string {
    const option = this.statusOptions.find(opt => opt.value === status);
    return option ? option.label : status;
  }

  canReturnToAutoStatus(): boolean {
    if (!this.applicationForm.dateDebut) return false;
    
    const today = new Date();
    const startDate = new Date(this.applicationForm.dateDebut);
    
    if (today >= startDate) {
      return true;
    }
    
    if (this.applicationForm.dateFin) {
      const endDate = new Date(this.applicationForm.dateFin);
      return today <= endDate;
    }
    
    return false;
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  isChefProjet(): boolean {
    return this.authService.isChefProjet();
  }

  // Statistics methods
  getTotalApplicationsCount(): number {
    return this.applications.length;
  }

  getEnCoursCount(): number {
    return this.applications.filter((app: Application) => app.status === 'EN_COURS').length;
  }

  getPlanifieCount(): number {
    return this.applications.filter((app: Application) => app.status === 'PLANIFIE').length;
  }

  getDelayedCount(): number {
    return this.applications.filter((app: Application) => app.isDelayed).length;
  }

  getCurrentUserName(): string {
    if (!this.currentUser) return '';
    return `${this.currentUser.firstName} ${this.currentUser.lastName}`;
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

  validateApplicationCode(): boolean {
    const code = this.applicationForm.code;
    const pattern = /^APP-\d{4}-\d{3}$/; // Changed from PROJ- to APP- for applications
    
    if (!pattern.test(code)) {
      this.errorMessage = 'Format de code invalide. Utilisez APP-AAAA-XXX (ex: APP-2024-001)';
      return false;
    }
    return true;
  }

  // Format date for display
  formatDate(dateString: string): string {
    if (!dateString) return '';
    return this.datePipe.transform(dateString, 'dd/MM/yyyy') || '';
  }

  // Check if application is delayed
  isApplicationDelayed(application: Application): boolean {
    if (!application.dateFin) return false;
    
    const today = new Date();
    const endDate = new Date(application.dateFin);
    
    return today > endDate && application.status === 'EN_COURS';
  }

  // Get time remaining string
  getTimeRemaining(application: Application): string {
    if (!application.dateFin) return 'Pas de date de fin';
    
    const today = new Date();
    const endDate = new Date(application.dateFin);
    const diffTime = endDate.getTime() - today.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    if (diffDays < 0) {
      return `${Math.abs(diffDays)} jours de retard`;
    } else if (diffDays === 0) {
      return 'Se termine aujourd\'hui';
    } else {
      return `${diffDays} jours restants`;
    }
  }

  // Get progress percentage (example calculation)
  getProgress(application: Application): number {
    if (!application.dateDebut || !application.dateFin) return 0;
    
    const start = new Date(application.dateDebut);
    const end = new Date(application.dateFin);
    const today = new Date();
    
    const totalDuration = end.getTime() - start.getTime();
    const elapsedDuration = today.getTime() - start.getTime();
    
    if (totalDuration <= 0) return 100;
    
    const progress = (elapsedDuration / totalDuration) * 100;
    
    // Ensure progress is between 0 and 100
    return Math.min(Math.max(progress, 0), 100);
  }

  getProgressClass(progress: number): string {
    if (progress >= 90) return 'bg-green-500';
    if (progress >= 70) return 'bg-blue-500';
    if (progress >= 50) return 'bg-yellow-500';
    if (progress >= 30) return 'bg-orange-500';
    return 'bg-red-500';
  }
}