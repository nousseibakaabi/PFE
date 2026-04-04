import { Component, OnInit } from '@angular/core';
import { ConventionService, Convention, ConventionRequest } from '../../services/convention.service';
import { FactureService } from '../../services/facture.service';
import { NomenclatureService, Structure } from '../../services/nomenclature.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { ApiResponse, ApplicationService } from 'src/app/services/application.service';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { TranslationService } from '../partials/traduction/translation.service';

@Component({
  selector: 'app-convention',
  templateUrl: './convention.component.html',
  styleUrls: ['./convention.component.css'],
  standalone: false
})
export class ConventionComponent implements OnInit {
  conventions: Convention[] = [];
  filteredConventions: Convention[] = [];
  selectedConvention: Convention | null = null;
  showArchiveModal = false;
  showRestoreModal = false;
  searchTerm = '';
  filterEtat = '';
  loading = false;
  errorMessage = '';
  successMessage = '';
  showArchived = false;
  projects: any[] = []; 
  
  archiveReason = '';

  currentPage: number = 1;
  itemsPerPage: number = 6;
  totalPages: number = 1;
  paginatedConventions: Convention[] = [];

  showInvoicesModal = false;
  relatedInvoices: any[] = [];
  selectedConventionForInvoices: Convention | null = null;

  activeTab: string = 'ALL';

  conventionForm = {
    structureBeneficielId: 0
  };

  periodicites = ['MENSUEL', 'TRIMESTRIEL', 'SEMESTRIEL', 'ANNUEL'];
  etats = [null, 'PLANIFIE', 'EN COURS', 'TERMINE'];

  structures: Structure[] = [];
  applications: any[] = [];

  internesStructures: Structure[] = [];
  externesStructures: Structure[] = [];

  constructor(
    private conventionService: ConventionService,
    private factureService: FactureService,
    private nomenclatureService: NomenclatureService,
    private authService: AuthService,
    private applicationService: ApplicationService,
    private http: HttpClient,
    private router: Router,
    private translationService: TranslationService
  ) {}

  ngOnInit(): void {
    this.loadConventions();
    this.loadBeneficielsStructures();
    this.loadResponsablesStructures();
  }

  setActiveTab(tab: string): void {
    this.activeTab = tab;
    this.applyFilters();
  }

  getConventionCountByEtat(etat: string | null): number {
    if (etat === null) {
      return this.conventions.length;
    }
    return this.conventions.filter(conv => conv.etat === etat).length;
  }

  filterByEtat(conventions: Convention[]): Convention[] {
    if (!conventions) return [];
    
    if (this.activeTab === 'ALL') {
      return conventions;
    }
    return conventions.filter(conv => conv.etat === this.activeTab);
  }

  resetSearch(): void {
    this.searchTerm = '';
    this.applyFilters();
  }

  updatePagination(): void {
    this.totalPages = Math.ceil(this.filteredConventions.length / this.itemsPerPage);
    
    if (this.currentPage > this.totalPages) {
      this.currentPage = this.totalPages || 1;
    }
    if (this.currentPage < 1) {
      this.currentPage = 1;
    }
    
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = Math.min(startIndex + this.itemsPerPage, this.filteredConventions.length);
    this.paginatedConventions = this.filteredConventions.slice(startIndex, endIndex);
  }

  changePage(page: number | string): void {
    if (typeof page === 'number') {
      if (page >= 1 && page <= this.totalPages) {
        this.currentPage = page;
        this.updatePagination();
        setTimeout(() => {
          document.querySelector('.grid')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }, 100);
      }
    }
  }

  getPageNumbers(): (number | string)[] {
    const pages: (number | string)[] = [];
    const maxVisible = 5;
    
    if (this.totalPages <= maxVisible) {
      for (let i = 1; i <= this.totalPages; i++) {
        pages.push(i);
      }
    } else {
      if (this.currentPage <= 3) {
        for (let i = 1; i <= 4; i++) pages.push(i);
        pages.push('...');
        pages.push(this.totalPages);
      } else if (this.currentPage >= this.totalPages - 2) {
        pages.push(1);
        pages.push('...');
        for (let i = this.totalPages - 3; i <= this.totalPages; i++) pages.push(i);
      } else {
        pages.push(1);
        pages.push('...');
        for (let i = this.currentPage - 1; i <= this.currentPage + 1; i++) pages.push(i);
        pages.push('...');
        pages.push(this.totalPages);
      }
    }
    
    return pages;
  }

  getCurrentPageStart(): number {
    return (this.currentPage - 1) * this.itemsPerPage + 1;
  }

  getCurrentPageEnd(): number {
    return Math.min(this.currentPage * this.itemsPerPage, this.filteredConventions.length);
  }

  changeItemsPerPage(perPage: number): void {
    this.itemsPerPage = perPage;
    this.currentPage = 1;
    this.updatePagination();
  }

  applyFilters(): void {
    if (!this.conventions) {
      this.filteredConventions = [];
      this.paginatedConventions = [];
      return;
    }
    
    this.filteredConventions = this.filterByEtat(this.conventions);
    
    if (this.searchTerm && this.searchTerm.trim()) {
      this.searchConventions();
    } else {
      this.updatePagination();
    }
  }

  searchConventions(): void {
    if (!this.searchTerm.trim()) {
      this.applyFilters();
      return;
    }

    const term = this.searchTerm.toLowerCase().trim();
    const tabFiltered = this.filterByEtat(this.conventions);
    
    this.filteredConventions = tabFiltered.filter(conv => {
      const searchableFields = [
        conv.referenceConvention?.toLowerCase() || '',
        conv.referenceERP?.toLowerCase() || '',
        conv.libelle?.toLowerCase() || '',
        conv.structureResponsableName?.toLowerCase() || '',
        conv.structureBeneficielName?.toLowerCase() || '',
        conv.zoneName?.toLowerCase() || '',
        conv.applicationName?.toLowerCase() || ''
      ];
      
      return searchableFields.some(field => field.includes(term));
    });
    
    this.currentPage = 1;
    this.updatePagination();
  }

  loadConventions(): void {
    this.loading = true;
    this.conventionService.getAllConventions(this.showArchived).subscribe({
      next: (response) => {
        if (response.success) {
          this.conventions = response.data;
          this.applyFilters();
          this.currentPage = 1;
          this.updatePagination();
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading conventions:', error);
        this.errorMessage = this.translationService.translate('Échec du chargement des conventions');
        this.loading = false;
      }
    });
  }

  loadProjects(): void {
    this.applicationService.getAllApplications().subscribe({
      next: (response) => {
        console.log('Projects response:', response);
        if (response.success) {
          this.projects = response.data;
          console.log('Projects loaded:', this.projects);
        } else {
          console.error('Error in response:', response);
        }
      },
      error: (error) => {
        console.error('Error loading projects:', error);
        console.error('Error details:', error.error);
      }
    });
  }

  loadApplications(): void {
    this.loading = true;
    this.applicationService.getApplicationsWithoutConventions().subscribe({
      next: (response: ApiResponse) => {
        if (response && response.success) {
          this.applications = response.data || [];
          this.errorMessage = '';
        } else {
          this.errorMessage = response?.message || this.translationService.translate('Failed to load applications');
          this.applications = [];
        }
        this.loading = false;
      },
      error: (error: any) => {
        console.error('Error loading applications:', error);
        this.errorMessage = error.error?.message || this.translationService.translate('Failed to load applications');
        this.applications = [];
        this.loading = false;
      }
    });
  }

  toggleArchivedView(): void {
    this.showArchived = !this.showArchived;
    this.loadConventions();
  }

  openArchiveModal(convention: Convention): void {
    this.selectedConvention = convention;
    this.archiveReason = '';
    this.showArchiveModal = true;
    this.errorMessage = '';
  }

  archiveConvention(): void {
    if (!this.selectedConvention) return;
    
    const conventionId = this.selectedConvention.id;
    
    if (!this.archiveReason.trim()) {
      this.errorMessage = this.translationService.translate('Veuillez fournir une raison pour l\'archivage');
      return;
    }

    this.loading = true;
    this.conventionService.archiveConvention(conventionId, this.archiveReason)
      .subscribe({
        next: (response) => {
          if (response.success) {
            this.successMessage = this.translationService.translate('Convention archivée avec succès');
            this.loadConventions();
            this.closeArchiveModal();
          }
          this.loading = false;
        },
        error: (error) => {
          console.error('Archive error:', error);
          
          if (error.status === 400 && error.error?.errorType === 'UNPAID_INVOICES') {
            this.errorMessage = error.error.message;
            if (error.error.details) {
              this.errorMessage += ' ' + error.error.details;
            }
            
            this.selectedConventionForInvoices = this.selectedConvention;
            this.loadRelatedInvoices(conventionId);
            
          } else {
            this.errorMessage = error.error?.message || this.translationService.translate('Échec de l\'archivage de la convention');
          }
          
          this.loading = false;
        }
      });
  }

  closeArchiveModal(): void {
    this.showArchiveModal = false;
    this.selectedConvention = null;
    this.archiveReason = '';
    this.errorMessage = '';
  }

  openRestoreModal(convention: Convention): void {
    this.selectedConvention = convention;
    this.showRestoreModal = true;
    this.errorMessage = '';
  }

  restoreConvention(): void {
    if (!this.selectedConvention) return;

    this.loading = true;
    this.conventionService.restoreConvention(this.selectedConvention.id)
      .subscribe({
        next: (response) => {
          if (response.success) {
            this.successMessage = this.translationService.translate('Convention restaurée avec succès');
            this.loadConventions();
            this.closeRestoreModal();
          }
          this.loading = false;
        },
        error: (error) => {
          console.error('Restore error:', error);
          this.errorMessage = error.error?.message || this.translationService.translate('Échec de la restauration de la convention');
          this.loading = false;
        }
      });
  }

  closeRestoreModal(): void {
    this.showRestoreModal = false;
    this.selectedConvention = null;
    this.errorMessage = '';
  }

  loadResponsablesStructures(): void {
    this.nomenclatureService.getResponsableStructures().subscribe({
      next: (structures) => {
        console.log('Responsables structures loaded:', structures);
        this.internesStructures = structures;
      },
      error: (error) => {
        console.error('Error loading Responsables structures:', error);
        console.error('Full error:', error);
        this.errorMessage = this.translationService.translate('Failed to load Responsables structures');
      }
    });
  }

  loadBeneficielsStructures(): void {
    this.nomenclatureService.getBeneficielStructures().subscribe({
      next: (structures) => {
        console.log('Beneficiels structures loaded:', structures);
        this.externesStructures = structures;
      },
      error: (error) => {
        console.error('Error loading Beneficiels structures:', error);
        console.error('Full error:', error);
        this.errorMessage = this.translationService.translate('Failed to load Beneficiels structures');
      }
    });
  }

  loadRelatedInvoices(conventionId: number): void {
    this.factureService.getFacturesByConvention(conventionId).subscribe({
      next: (response) => {
        if (response.success) {
          this.relatedInvoices = response.data;
          this.showInvoicesModal = true;
        }
      },
      error: (error) => {
        console.error('Failed to load related invoices:', error);
      }
    });
  }

  closeInvoicesModal(): void {
    this.showInvoicesModal = false;
    this.relatedInvoices = [];
    this.selectedConventionForInvoices = null;
  }

  getInvoiceStatusClass(statut: string): string {
    switch (statut) {
      case 'PAYE': return 'bg-green-100 text-green-800';
      case 'NON_PAYE': return 'bg-red-100 text-red-800';
      case 'EN_RETARD': return 'bg-orange-100 text-orange-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  getInvoiceStatusLabel(statut: string): string {
    switch (statut) {
      case 'PAYE': return this.translationService.translate('Payée');
      case 'NON_PAYE': return this.translationService.translate('Non Payée');
      case 'EN_RETARD': return this.translationService.translate('En Retard');
      default: return statut;
    }
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  getEtatClass(etat: string | null): string {
    if (etat === null) return 'bg-gray-100 text-gray-800';
    
    switch (etat) {
      case 'PLANIFIE': return 'bg-yellow-100 text-yellow-800';
      case 'EN COURS': return 'bg-blue-100 text-blue-800';
      case 'TERMINE': return 'bg-green-100 text-green-800';
      case 'ARCHIVE': return 'bg-gray-100 text-gray-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  getEtatLabel(etat: string | null): string {
    if (etat === null) return '';
    switch (etat) {
      case 'PLANIFIE': return this.translationService.translate('Planifiée');
      case 'EN COURS': return this.translationService.translate('En Cours');
      case 'TERMINE': return this.translationService.translate('Terminé');
      default: return etat;
    }
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  isCommercial(): boolean {
    return this.authService.isCommercial();
  }

  canEdit(): boolean {
    return this.isAdmin() || this.isCommercial();
  }

  viewConventionDetails(id: number): void {
    this.router.navigate(['/conventions', id]);
  }

  openCreateModal(): void {
    this.router.navigate(['/conventions/new']);
  }
}