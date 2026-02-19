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



  
  // For showing invoice details
  showInvoicesModal = false;
  relatedInvoices: any[] = [];
  selectedConventionForInvoices: Convention | null = null;


  conventionForm = {
  structureBeneficielId: 0
};

  periodicites = ['MENSUEL', 'TRIMESTRIEL', 'SEMESTRIEL', 'ANNUEL'];
etats = [null, 'EN_ATTENTE', 'EN_COURS', 'EN_RETARD', 'TERMINE'];

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





  loadConventions(): void {
    this.loading = true;
    this.conventionService.getAllConventions(this.showArchived).subscribe({
      next: (response) => {
        if (response.success) {
          this.conventions = response.data;
          this.filteredConventions = [...this.conventions];
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



searchConventions(): void {
  if (!this.searchTerm.trim()) {
    this.filteredConventions = this.filterByEtat(this.conventions);
    return;
  }

  const term = this.searchTerm.toLowerCase();
  this.filteredConventions = this.filterByEtat(this.conventions).filter(conv =>
    conv.referenceConvention.toLowerCase().includes(term) ||
    conv.referenceERP?.toLowerCase().includes(term) ||
    conv.libelle.toLowerCase().includes(term) ||
    conv.structureResponsableName.toLowerCase().includes(term) ||
    conv.structureBeneficielName.toLowerCase().includes(term) ||
    conv.zoneName.toLowerCase().includes(term) ||
    conv.applicationName.toLowerCase().includes(term)
  );
}

  filterByEtat(conventions: Convention[]): Convention[] {
    if (!this.filterEtat) {
      return conventions;
    }
    if (this.filterEtat === 'null') {
      return conventions.filter(conv => conv.etat === null);
    }
    return conventions.filter(conv => conv.etat === this.filterEtat);
  }

  applyFilters(): void {
    this.filteredConventions = this.filterByEtat(this.conventions);
    if (this.searchTerm.trim()) {
      this.searchConventions();
    }
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
    if (etat === null) return '-';
    
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

openEditModal(convention: Convention): void {
  this.router.navigate(['/conventions/edit', convention.id]);
}

}