import { Component, OnInit } from '@angular/core';
import { ConventionService, Convention, ConventionRequest } from '../../services/convention.service';
import { FactureService } from '../../services/facture.service';
import { NomenclatureService, Structure } from '../../services/nomenclature.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { ProjectService } from 'src/app/services/project.service';

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
  showModal = false;
  showArchiveModal = false;
  showRestoreModal = false;
  isEditing = false;
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

  // Form fields - FIXED
  formData: ConventionRequest = {
    referenceConvention: '',
    referenceERP: '',
    libelle: '',
    dateDebut: '',
    dateFin: '',
    dateSignature: '',
    structureInterneId: 0,
    structureExterneId: 0,
    zoneId: 0,
  projectId: 0,  
    montantTotal: 0,
    periodicite: 'MENSUEL'
  };

  conventionForm = {
  structureExterneId: 0
};

  periodicites = ['MENSUEL', 'TRIMESTRIEL', 'SEMESTRIEL', 'ANNUEL'];
etats = [null, 'EN_ATTENTE', 'EN_COURS', 'EN_RETARD', 'TERMINE'];

  // Real data from API
  structures: Structure[] = [];
  zones: any[] = []; // CHANGED FROM "gouvernorats" to "zones"
  applications: any[] = []; // ADD THIS

  

  constructor(
    private conventionService: ConventionService,
    private factureService: FactureService,
    private nomenclatureService: NomenclatureService,
    private authService: AuthService,
    private projectService : ProjectService
  ) {}

  ngOnInit(): void {
    this.loadConventions();
    this.loadStructures();
    this.loadZones(); // CHANGED FROM "loadGouvernorats"
    this.loadApplications(); // ADD THIS
    this.loadProjects();
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
  this.projectService.getAllProjects().subscribe({
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


onProjectSelected(projectId: number): void {
  if (projectId) {
    this.projectService.getClientStructureForProject(projectId).subscribe({
      next: (response) => {
        if (response.success && response.data) {
          // Auto-fill external structure with client
          this.formData.structureExterneId = response.data.id;
          
          // Also, update the structures dropdown to show the selected client
          // You might need to reload structures or ensure the client structure is in the list
        }
      },
      error: (error) => {
        console.error('Error getting client structure:', error);
      }
    });
  }
}

  loadApplications(): void {
    this.nomenclatureService.getApplications().subscribe({
      next: (applications) => {
        this.applications = applications;
      },
      error: (error) => {
        console.error('Error loading applications:', error);
        this.errorMessage = 'Failed to load applications';
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

  loadStructures(): void {
    this.nomenclatureService.getStructures().subscribe({
      next: (structures) => {
        this.structures = structures;
      },
      error: (error) => {
        console.error('Error loading structures:', error);
        this.errorMessage = 'Failed to load structures';
      }
    });
  }

  loadZones(): void { // CHANGED FROM "loadGouvernorats"
    this.nomenclatureService.getZones().subscribe({
      next: (zones) => {
        this.zones = zones;
      },
      error: (error) => {
        console.error('Error loading zones:', error);
        this.errorMessage = 'Failed to load zones';
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
    conv.structureInterneName.toLowerCase().includes(term) ||
    conv.structureExterneName.toLowerCase().includes(term) ||
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

 openCreateModal(): void {
  this.isEditing = false;
  this.formData = {
    referenceConvention: '',
    referenceERP: '',
    libelle: '',
    dateDebut: '',
    dateFin: '',
    dateSignature: '',
    structureInterneId: 0,
    structureExterneId: 0,
    zoneId: 0,
    projectId: 0, // Add this!
    montantTotal: 0,
    periodicite: 'MENSUEL'
  };
  this.showModal = true;
  this.errorMessage = '';
}

openEditModal(convention: Convention): void {
  this.isEditing = true;
  this.selectedConvention = convention;
  this.formData = {
    referenceConvention: convention.referenceConvention,
    referenceERP: convention.referenceERP || '',
    libelle: convention.libelle,
    dateDebut: convention.dateDebut,
    dateFin: convention.dateFin || '',
    dateSignature: convention.dateSignature || '',
    structureInterneId: convention.structureInterneId || 0,
    structureExterneId: convention.structureExterneId || 0,
    zoneId: convention.zoneId || 0,
    projectId: convention.projectId || 0, // Add this!
    montantTotal: convention.montantTotal || 0,
    periodicite: convention.periodicite || 'MENSUEL'
  };
  this.showModal = true;
  this.errorMessage = '';
}

  closeModal(): void {
    this.showModal = false;
    this.selectedConvention = null;
    this.errorMessage = '';
  }

  saveConvention(): void {
    if (!this.validateForm()) {
      return;
    }

    this.loading = true;
    if (this.isEditing && this.selectedConvention) {
      this.conventionService.updateConvention(this.selectedConvention.id, this.formData)
        .subscribe({
          next: (response) => {
            if (response.success) {
              this.successMessage = 'Convention mise à jour avec succès';
              this.loadConventions();
              this.closeModal();
            }
            this.loading = false;
          },
          error: (error) => {
            this.errorMessage = error.error?.message || 'Échec de la mise à jour de la convention';
            this.loading = false;
          }
        });
    } else {
      this.conventionService.createConvention(this.formData)
        .subscribe({
          next: (response) => {
            if (response.success) {
              this.successMessage = 'Convention créée avec succès';
              this.loadConventions();
              this.closeModal();
            }
            this.loading = false;
          },
          error: (error) => {
            this.errorMessage = error.error?.message || 'Échec de la création de la convention';
            this.loading = false;
          }
        });
    }
  }

  deleteConvention(id: number): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer cette convention ?')) {
      this.loading = true;
      this.errorMessage = '';
      
      this.conventionService.deleteConvention(id).subscribe({
        next: (response) => {
          if (response.success) {
            this.successMessage = 'Convention supprimée avec succès';
            this.loadConventions();
          }
          this.loading = false;
        },
        error: (error) => {
          console.error('Delete error:', error);
          
          if (error.status === 400) {
            if (error.error?.errorType === 'UNPAID_INVOICES') {
              this.errorMessage = error.error.message;
              if (error.error.details) {
                this.errorMessage += ' ' + error.error.details;
              }
              
              this.selectedConventionForInvoices = this.conventions.find(c => c.id === id) || null;
              if (this.selectedConventionForInvoices) {
                this.loadRelatedInvoices(id);
              }
              
            } else if (error.error?.message) {
              this.errorMessage = error.error.message;
            }
          } else if (error.status === 404) {
            this.errorMessage = 'Convention non trouvée';
          } else if (error.status === 403) {
            this.errorMessage = 'Vous n\'avez pas la permission de supprimer cette convention';
          } else if (error.status === 500) {
            this.errorMessage = 'Erreur serveur lors de la suppression. Veuillez réessayer.';
          } else {
            this.errorMessage = error.error?.message || 'Échec de la suppression de la convention';
          }
          
          this.loading = false;
        }
      });
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

  validateForm(): boolean {
    if (!this.formData.referenceConvention.trim()) { // CHANGED FROM "reference"
      this.errorMessage = 'Référence Convention est requise';
      return false;
    }
    if (!this.formData.libelle.trim()) {
      this.errorMessage = 'Libellé est requis';
      return false;
    }
    if (!this.formData.dateDebut) {
      this.errorMessage = 'Date de début est requise';
      return false;
    }
    if (!this.formData.structureInterneId) { // CHANGED FROM "structureId"
      this.errorMessage = 'Structure interne est requise';
      return false;
    }
    if (!this.formData.structureExterneId) { // ADD THIS
      this.errorMessage = 'Structure externe est requise';
      return false;
    }
    if (!this.formData.zoneId) { // CHANGED FROM "gouvernoratId"
      this.errorMessage = 'Zone est requise';
      return false;
    }
    if (!this.formData.projectId) { // ADD THIS
      this.errorMessage = 'Application est requise';
      return false;
    }
    if (!this.formData.periodicite) {
      this.errorMessage = 'Périodicité est requise';
      return false;
    }
    return true;
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  getEtatClass(etat: string | null): string {
    if (etat === null) return 'bg-gray-100 text-gray-800';
    
    switch (etat) {
        case 'EN_ATTENTE': return 'bg-yellow-100 text-yellow-800';
        case 'EN_COURS': return 'bg-blue-100 text-blue-800';
        case 'EN_RETARD': return 'bg-red-100 text-red-800';
        case 'TERMINE': return 'bg-green-100 text-green-800';
        case 'RESILIE': return 'bg-purple-100 text-purple-800';
        case 'ARCHIVE': return 'bg-gray-100 text-gray-800';
        default: return 'bg-gray-100 text-gray-800';
    }
}

getEtatLabel(etat: string | null): string {
    if (etat === null) return '-';
    
    switch (etat) {
        case 'EN_ATTENTE': return 'En Attente';
        case 'EN_COURS': return 'En Cours';
        case 'EN_RETARD': return 'En Retard';
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
}