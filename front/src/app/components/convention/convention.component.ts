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
  
  // Archive form
  archiveReason = '';

    // Add these properties
  currentPage: number = 1;
  itemsPerPage: number = 6;
  totalPages: number = 1;
  paginatedConventions: Convention[] = [];



  
  // For showing invoice details
  showInvoicesModal = false;
  relatedInvoices: any[] = [];
  selectedConventionForInvoices: Convention | null = null;


activeTab: string = 'ALL'; // 'ALL', 'PLANIFIE', 'EN COURS', 'TERMINE'

  conventionForm = {
  structureBeneficielId: 0
};

  periodicites = ['MENSUEL', 'TRIMESTRIEL', 'SEMESTRIEL', 'ANNUEL'];
etats = [null, 'PLANIFIE', 'EN COURS', 'TERMINE'];

  // Real data from API
  structures: Structure[] = [];
  applications: any[] = []; // ADD THIS

  
  internesStructures: Structure[] = [];
externesStructures: Structure[] = [];

  constructor(
    private conventionService: ConventionService,
    private factureService: FactureService,
    private nomenclatureService: NomenclatureService,
    private authService: AuthService,
    private applicationService : ApplicationService,
    private http: HttpClient,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadConventions();
    this.loadBeneficielsStructures();
    this.loadResponsablesStructures();
  }




  // Set active tab and filter conventions
setActiveTab(tab: string): void {
  this.activeTab = tab;
  this.applyFilters();
}

// Get count of conventions by etat
getConventionCountByEtat(etat: string | null): number {
  if (etat === null) {
    return this.conventions.length;
  }
  return this.conventions.filter(conv => conv.etat === etat).length;
}





// Fix filterByEtat method
filterByEtat(conventions: Convention[]): Convention[] {
  if (!conventions) return [];
  
  if (this.activeTab === 'ALL') {
    return conventions;
  }
  return conventions.filter(conv => conv.etat === this.activeTab);
}




// Add this method to reset search
resetSearch(): void {
  this.searchTerm = '';
  this.applyFilters();
}


// Update pagination when filters change
updatePagination(): void {
  // Calculate total pages
  this.totalPages = Math.ceil(this.filteredConventions.length / this.itemsPerPage);
  
  // Ensure current page is valid
  if (this.currentPage > this.totalPages) {
    this.currentPage = this.totalPages || 1;
  }
  if (this.currentPage < 1) {
    this.currentPage = 1;
  }
  
  // Get current page items
  const startIndex = (this.currentPage - 1) * this.itemsPerPage;
  const endIndex = Math.min(startIndex + this.itemsPerPage, this.filteredConventions.length);
  this.paginatedConventions = this.filteredConventions.slice(startIndex, endIndex);
}

changePage(page: number | string): void {
  if (typeof page === 'number') {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
      this.updatePagination();
      // Scroll to top of grid smoothly
      setTimeout(() => {
        document.querySelector('.grid')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }, 100);
    }
  }
}

getPageNumbers(): (number | string)[] {
  const pages: (number | string)[] = [];
  const maxVisible = 5; // Maximum number of page buttons to show
  
  if (this.totalPages <= maxVisible) {
    // Show all pages
    for (let i = 1; i <= this.totalPages; i++) {
      pages.push(i);
    }
  } else {
    // Show pages with ellipsis
    if (this.currentPage <= 3) {
      // Near the start
      for (let i = 1; i <= 4; i++) pages.push(i);
      pages.push('...');
      pages.push(this.totalPages);
    } else if (this.currentPage >= this.totalPages - 2) {
      // Near the end
      pages.push(1);
      pages.push('...');
      for (let i = this.totalPages - 3; i <= this.totalPages; i++) pages.push(i);
    } else {
      // Middle
      pages.push(1);
      pages.push('...');
      for (let i = this.currentPage - 1; i <= this.currentPage + 1; i++) pages.push(i);
      pages.push('...');
      pages.push(this.totalPages);
    }
  }
  
  return pages;
}



// Get current page start item number
getCurrentPageStart(): number {
  return (this.currentPage - 1) * this.itemsPerPage + 1;
}

// Get current page end item number
getCurrentPageEnd(): number {
  return Math.min(this.currentPage * this.itemsPerPage, this.filteredConventions.length);
}

// Change items per page
changeItemsPerPage(perPage: number): void {
  this.itemsPerPage = perPage;
  this.currentPage = 1;
  this.updatePagination();
}

// Override applyFilters to update pagination
applyFilters(): void {
  if (!this.conventions) {
    this.filteredConventions = [];
    this.paginatedConventions = [];
    return;
  }
  
  // First filter by active tab
  this.filteredConventions = this.filterByEtat(this.conventions);
  
  // Then apply search if needed
  if (this.searchTerm && this.searchTerm.trim()) {
    this.searchConventions();
  } else {
    // Update pagination
    this.updatePagination();
  }
}

// Override searchConventions to update pagination
searchConventions(): void {
  if (!this.searchTerm.trim()) {
    this.applyFilters();
    return;
  }

  const term = this.searchTerm.toLowerCase().trim();
  
  // First get conventions filtered by active tab
  const tabFiltered = this.filterByEtat(this.conventions);
  
  // Then search within those filtered conventions
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
  
  // Reset to first page and update pagination
  this.currentPage = 1;
  this.updatePagination();
}

// Update loadConventions to set pagination
loadConventions(): void {
  this.loading = true;
  this.conventionService.getAllConventions(this.showArchived).subscribe({
    next: (response) => {
      if (response.success) {
        this.conventions = response.data;
        this.applyFilters(); // Apply filters after loading
        this.currentPage = 1; // Reset to first page
        this.updatePagination();
      }
      this.loading = false;
    },
    error: (error) => {
      console.error('Error loading conventions:', error);
      this.errorMessage = 'Échec du chargement des conventions';
      this.loading = false;
    }
  });
}


loadProjects(): void {
  this.applicationService.getAllApplications().subscribe({
    next: (response) => {
      console.log('Projects response:', response); // Debug log
      if (response.success) {
        this.projects = response.data; 
        console.log('Projects loaded:', this.projects); // Debug log
      } else {
        console.error('Error in response:', response);
      }
    },
    error: (error) => {
      console.error('Error loading projects:', error);
      console.error('Error details:', error.error); // More details
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
        this.errorMessage = response?.message || 'Failed to load applications';
        this.applications = [];
      }
      this.loading = false;
    },
    error: (error: any) => {
      console.error('Error loading applications:', error);
      this.errorMessage = error.error?.message || 'Failed to load applications';
      this.applications = [];
      this.loading = false;
    }
  });
}

  // Toggle archived view
  toggleArchivedView(): void {
    this.showArchived = !this.showArchived;
    this.loadConventions();
  }

  // Archive convention
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
      this.errorMessage = 'Veuillez fournir une raison pour l\'archivage';
      return;
    }

    this.loading = true;
    this.conventionService.archiveConvention(conventionId, this.archiveReason)
      .subscribe({
        next: (response) => {
          if (response.success) {
            this.successMessage = 'Convention archivée avec succès';
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
            this.errorMessage = error.error?.message || 'Échec de l\'archivage de la convention';
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

  // Restore convention
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
            this.successMessage = 'Convention restaurée avec succès';
            this.loadConventions();
            this.closeRestoreModal();
          }
          this.loading = false;
        },
        error: (error) => {
          console.error('Restore error:', error);
          this.errorMessage = error.error?.message || 'Échec de la restauration de la convention';
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
        console.log('Responsables structures loaded:', structures); // Debug log
        this.internesStructures = structures;
      },
      error: (error) => {
        console.error('Error loading Responsables structures:', error);
        console.error('Full error:', error); // More details
        this.errorMessage = 'Failed to load Responsables structures';
      }
    });
  }

  loadBeneficielsStructures(): void {
    this.nomenclatureService.getBeneficielStructures().subscribe({
      next: (structures) => {
        console.log('Beneficiels structures loaded:', structures); // Debug log
        this.externesStructures = structures;
      },
      error: (error) => {
        console.error('Error loading Beneficiels structures:', error);
        console.error('Full error:', error); // More details
        this.errorMessage = 'Failed to load Beneficiels structures';
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

  // Close invoices modal
  closeInvoicesModal(): void {
    this.showInvoicesModal = false;
    this.relatedInvoices = [];
    this.selectedConventionForInvoices = null;
  }

  // Get status badge class for invoices
  getInvoiceStatusClass(statut: string): string {
    switch (statut) {
      case 'PAYE': return 'bg-green-100 text-green-800';
      case 'NON_PAYE': return 'bg-red-100 text-red-800';
      case 'EN_RETARD': return 'bg-orange-100 text-orange-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  // Get status label for invoices
  getInvoiceStatusLabel(statut: string): string {
    switch (statut) {
      case 'PAYE': return 'Payée';
      case 'NON_PAYE': return 'Non Payée';
      case 'EN_RETARD': return 'En Retard';
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
    if(etat === null) return ''
    switch (etat) {
        case 'PLANIFIE': return 'Planifiée';
        case 'EN COURS': return 'En Cours';
        case 'TERMINE': return 'Terminé';
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