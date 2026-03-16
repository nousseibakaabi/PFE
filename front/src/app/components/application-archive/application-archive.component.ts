// application-archive.component.ts
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ApplicationService, Application, ApiResponse } from '../../services/application.service';
import { ConventionService } from '../../services/convention.service';
import { FactureService } from '../../services/facture.service';
import { AuthService } from '../../services/auth.service';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'app-application-archive',
  templateUrl: './application-archive.component.html',
  styleUrls: ['./application-archive.component.css'],
  providers: [DatePipe]
})
export class ApplicationArchiveComponent implements OnInit {
  archivedApplications: Application[] = [];
  filteredApplications: Application[] = [];
  
  loading = false;
  errorMessage = '';
  successMessage = '';
  searchTerm = '';
  
  // Current user info
  currentUser: any = null;
  isAdmin = false;
  isChefProjet = false;

  // Pagination
  currentPage: number = 1;
  itemsPerPage: number = 5;
  
  // For restore modal
  showRestoreModal = false;
  selectedAppForRestore: Application | null = null;
  restoreLoading = false;
  restoreErrorMessage = '';
  
  // For viewing archived conventions
  showConventionsModal = false;
  selectedAppForConventions: Application | null = null;
  archivedConventions: any[] = [];
  loadingConventions = false;

  constructor(
    private applicationService: ApplicationService,
    private conventionService: ConventionService,
    private factureService: FactureService,
    private authService: AuthService,
    private router: Router,
    private datePipe: DatePipe
  ) {}

  ngOnInit(): void {
    this.loadCurrentUser();
    this.loadArchivedApplications();
  }

  loadCurrentUser(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.isAdmin = this.authService.isAdmin();
    this.isChefProjet = this.authService.isChefProjet();
  }

loadArchivedApplications(): void {
  this.loading = true;
  
  this.applicationService.getArchivedApplications().subscribe({
    next: (response: ApiResponse) => {
      if (response.success) {
        let allArchived = response.data || [];
        
        // For chef de projet, filter only their own archived applications
        if (this.isChefProjet && !this.isAdmin && this.currentUser) {
          this.archivedApplications = allArchived.filter((app: Application) => 
            app.chefDeProjetId === this.currentUser.id
          );
        } else {
          this.archivedApplications = allArchived;
        }
        
        this.filteredApplications = [...this.archivedApplications];
      } else {
        this.errorMessage = response.message || 'Erreur lors du chargement';
      }
      this.loading = false;
    },
    error: (error) => {
      console.error('Error loading archived applications:', error);
      this.errorMessage = 'Erreur de connexion au serveur';
      this.loading = false;
    }
  });
}

  searchApplications(): void {
    if (!this.searchTerm.trim()) {
      this.filteredApplications = [...this.archivedApplications];
      return;
    }

    const term = this.searchTerm.toLowerCase();
    this.filteredApplications = this.archivedApplications.filter((app: Application) =>
      app.code?.toLowerCase().includes(term) ||
      app.name?.toLowerCase().includes(term) ||
      app.clientName?.toLowerCase().includes(term)
    );
    
    this.currentPage = 1;
  }

  // Pagination methods
  get paginatedApplications(): Application[] {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    return this.filteredApplications.slice(startIndex, endIndex);
  }

  get totalPages(): number {
    return Math.ceil(this.filteredApplications.length / this.itemsPerPage);
  }

  get startIndex(): number {
    return (this.currentPage - 1) * this.itemsPerPage + 1;
  }

  get endIndex(): number {
    return Math.min(this.currentPage * this.itemsPerPage, this.filteredApplications.length);
  }

  previousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
    }
  }


 


  // View archived conventions for an application
  viewArchivedConventions(application: Application, event: Event): void {
    event.stopPropagation();
    this.selectedAppForConventions = application;
    this.showConventionsModal = true;
    this.loadArchivedConventions(application.id);
  }

  loadArchivedConventions(applicationId: number): void {
    this.loadingConventions = true;
    
    // You need to add this method to your ConventionService
    this.conventionService.getArchivedConventionsByApplication(applicationId).subscribe({
      next: (response) => {
        if (response.success) {
          this.archivedConventions = response.data;
        } else {
          console.error('Failed to load archived conventions');
        }
        this.loadingConventions = false;
      },
      error: (error) => {
        console.error('Error loading archived conventions:', error);
        this.loadingConventions = false;
      }
    });
  }

  closeConventionsModal(): void {
    this.showConventionsModal = false;
    this.selectedAppForConventions = null;
    this.archivedConventions = [];
  }

  viewConventionDetails(id: number): void {
    this.router.navigate(['/conventions', id]);
    this.closeConventionsModal();
  }

  // Navigation
  goBack(): void {
    this.router.navigate(['/applications']);
  }

  viewApplicationDetails(id: number): void {
    this.router.navigate(['/applications', id]);
  }

  // Utility methods
  formatDate(dateString: string): string {
    if (!dateString) return '-';
    return this.datePipe.transform(dateString, 'dd/MM/yyyy') || '-';
  }

  formatDateTime(dateString: string): string {
    if (!dateString) return '-';
    return this.datePipe.transform(dateString, 'dd/MM/yyyy HH:mm') || '-';
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'ARCHIVE': return 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  getStatusLabel(status: string): string {
    return 'Archivée';
  }

  getConventionStatusClass(etat: string): string {
    if (etat === 'ARCHIVE') return 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300';
    if (etat === 'TERMINE') return 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300';
    return 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300';
  }

  getConventionStatusLabel(etat: string): string {
    switch (etat) {
      case 'ARCHIVE': return 'Archivée';
      case 'TERMINE': return 'Terminée';
      case 'EN COURS': return 'En Cours';
      case 'PLANIFIE': return 'Planifiée';
      default: return etat;
    }
  }

  formatMontant(montant: number): string {
    return new Intl.NumberFormat('fr-TN', { 
      minimumFractionDigits: 2, 
      maximumFractionDigits: 2 
    }).format(montant || 0);
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  canRestore(): boolean {
    return this.isAdmin || this.isChefProjet;
  }
}