import { Component, OnInit } from '@angular/core';
import { ConventionService, Convention, ConventionRequest } from '../../services/convention.service';
import { FactureService } from '../../services/facture.service';
import { NomenclatureService, Structure } from '../../services/nomenclature.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { ApiResponse, ApplicationService } from 'src/app/services/application.service';
import { HttpClient, HttpHeaders } from '@angular/common/http';

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


  userLimits: { minUser?: number; maxUser?: number } | null = null;
  userRuleMessage: string = '';
  isCalculatingTTC: boolean = false;
  isDeterminingUsers: boolean = false;

  
  // For showing invoice details
  showInvoicesModal = false;
  relatedInvoices: any[] = [];
  selectedConventionForInvoices: Convention | null = null;

  formData: ConventionRequest = {
    referenceConvention: '',
    referenceERP: '',
    libelle: '',
    dateDebut: '',
    dateFin: '',
    dateSignature: '',
    structureResponsableId: 0,
    structureBeneficielId: 0,
    applicationId: 0,  
    periodicite: 'MENSUEL',
      nbUsers : 0,
  montantHT : 0,
  montantTTC : 0,
  tva : 0,
  };

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
     private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.loadConventions();
    this.loadInternesStructures();
    this.loadExternesStructures();
    this.loadApplications(); 
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

onProjectSelected(projectId: number): void {
    if (projectId) {
      // Get client structure
      this.applicationService.getClientStructureForApplication(projectId).subscribe({
        next: (response) => {
          if (response.success && response.data) {
            this.formData.structureBeneficielId = response.data.id;
          }
        },
        error: (error) => {
          console.error('Error getting client structure:', error);
        }
      });
      
      // Load application limits and determine initial nb users
      this.loadApplicationLimits(projectId);
    }
  }


   loadApplicationLimits(applicationId: number): void {
    this.applicationService.getApplication(applicationId).subscribe({
      next: (response) => {
        if (response.success && response.data) {
          const app = response.data;
          this.userLimits = {
            minUser: app.minUser,
            maxUser: app.maxUser
          };
          
          // Determine initial nb users
          if (app.minUser || app.maxUser) {
            this.determineNbUsers();
          }
        }
      },
      error: (error) => {
        console.error('Error loading application limits:', error);
      }
    });
  }

  /**
   * Called when user focuses on nb users input
   */
  onNbUsersFocus(): void {
    if (this.formData.applicationId && !this.formData.nbUsers) {
      this.determineNbUsers();
    }
  }

  /**
   * Called when nb users value changes
   */
  onNbUsersChange(): void {
    if (this.formData.applicationId) {
      this.determineNbUsers();
    }
  }

  /**
   * Determine number of users based on application limits
   */
  determineNbUsers(): void {
    if (!this.formData.applicationId) {
      this.userRuleMessage = 'Veuillez d\'abord sélectionner une application';
      return;
    }

    this.isDeterminingUsers = true;
    
    this.conventionService.determineNbUsers(
      this.formData.applicationId,
      this.formData.nbUsers || undefined
    ).subscribe({
      next: (response) => {
        if (response.success) {
          const data = response.data;
          
          // Set the determined nb users
          if (data.nbUsers !== this.formData.nbUsers) {
            this.formData.nbUsers = data.nbUsers;
            this.userRuleMessage = `${data.appliedRule} (${data.nbUsers} utilisateurs)`;
          } else {
            this.userRuleMessage = data.appliedRule;
          }
          
          // Update limits display
          this.userLimits = {
            minUser: data.minUser,
            maxUser: data.maxUser
          };
        }
        this.isDeterminingUsers = false;
      },
      error: (error) => {
        console.error('Error determining nb users:', error);
        this.userRuleMessage = 'Erreur lors de la détermination du nombre d\'utilisateurs';
        this.isDeterminingUsers = false;
      }
    });
  }

  /**
   * Called when montant HT changes
   */
  onMontantHTChange(): void {
    this.calculateTTC();
  }

  /**
   * Called when TVA changes
   */
  onTvaChange(): void {
    this.calculateTTC();
  }

  /**
   * Calculate TTC from HT and TVA
   */
  calculateTTC(): void {
    if (!this.formData.montantHT || this.formData.montantHT <= 0) {
      this.formData.montantTTC = 0;
      return;
    }

    const tva = this.formData.tva || 19;
    
    this.isCalculatingTTC = true;
    
    this.conventionService.calculateTTC(this.formData.montantHT, tva).subscribe({
      next: (response) => {
        if (response.success) {
          const data = response.data;
          this.formData.montantTTC = data.montantTTC;
          
          // Update tva if it was null
          if (!this.formData.tva) {
            this.formData.tva = data.tva;
          }
        }
        this.isCalculatingTTC = false;
      },
      error: (error) => {
        console.error('Error calculating TTC:', error);
        this.isCalculatingTTC = false;
      }
    });
  }


loadApplications(): void {
  this.loading = true; // Add loading state
  this.applicationService.getAllApplications().subscribe({
    next: (response: ApiResponse) => {
      if (response && response.success) {
        if (response.data) {
          this.applications = response.data;
        } else if (response.applications) {
          this.applications = response.applications;
        } else {
          // If response itself is the array
          this.applications = response as any;
        }
        
        this.errorMessage = ''; // Clear any previous error
      } else {
        // Handle unsuccessful response
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

loadInternesStructures(): void {
  this.nomenclatureService.getInternesStructures().subscribe({
    next: (structures) => {
      console.log('Internes structures loaded:', structures); // Debug log
      this.internesStructures = structures;
    },
    error: (error) => {
      console.error('Error loading internes structures:', error);
      console.error('Full error:', error); // More details
      this.errorMessage = 'Failed to load internes structures';
    }
  });
}

loadExternesStructures(): void {
  this.nomenclatureService.getExternesStructures().subscribe({
    next: (structures) => {
      console.log('Externes structures loaded:', structures); // Debug log
      this.externesStructures = structures;
    },
    error: (error) => {
      console.error('Error loading externes structures:', error);
      console.error('Full error:', error); // More details
      this.errorMessage = 'Failed to load externes structures';
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
      structureResponsableId: convention.structureResponsableId || 0,
      structureBeneficielId: convention.structureBeneficielId || 0,
      applicationId: convention.applicationId || 0,
      montantHT: convention.montantHT || 0,
      tva: convention.tva || 19,
      montantTTC: convention.montantTTC || 0,
      nbUsers: convention.nbUsers || 0,
      periodicite: convention.periodicite || 'MENSUEL'
    };
    
    // Load application limits
    if (convention.applicationId) {
      this.loadApplicationLimits(convention.applicationId);
    }
    
    this.showModal = true;
    this.errorMessage = '';
  }

  /**
   * Override openCreateModal to initialize financial fields
   */
  openCreateModal(): void {
    this.isEditing = false;
    this.formData = {
      referenceConvention: '',
      referenceERP: '',
      libelle: '',
      dateDebut: '',
      dateFin: '',
      dateSignature: '',
      structureResponsableId: 0,
      structureBeneficielId: 0,
      applicationId: 0,
      // FINANCIAL FIELDS
      montantHT: 0,
      tva: 19,
      montantTTC: 0,
      nbUsers: 0,
      periodicite: 'MENSUEL'
    };
    
    this.userLimits = null;
    this.userRuleMessage = '';
    
    // Load suggested reference
    this.loadSuggestedReference();
    
    this.showModal = true;
    this.errorMessage = '';
  }


validateReference(): boolean {
  const reference = this.formData.referenceConvention;
  
  // Check if reference follows pattern
  const pattern = /^CONV-\d{4}-\d{3}$/;
  if (!pattern.test(reference)) {
    this.errorMessage = 'Le format de référence doit être: CONV-YYYY-XXX (ex: CONV-2024-001)';
    return false;
  }
  
  return true;
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

    // Ensure TTC is calculated
    if (this.formData.montantHT && this.formData.montantHT > 0 && !this.formData.montantTTC) {
      this.calculateTTC();
    }

    // Ensure nb users is determined
    if (this.formData.applicationId && !this.formData.nbUsers) {
      this.determineNbUsers();
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
    if (!this.formData.referenceConvention.trim()) {
      this.errorMessage = 'Référence Convention est requise';
      return false;
    }

    if (!this.validateReference()) {
      return false;
    }

    if (!this.formData.referenceERP.trim()) {
      this.errorMessage = 'Référence ERP est requise';
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

    if (!this.formData.dateFin) {
      this.errorMessage = 'Date de fin est requise';
      return false;
    }

    if (!this.formData.structureResponsableId) { 
      this.errorMessage = 'Structure interne est requise';
      return false;
    }
    if (!this.formData.structureBeneficielId) {
      this.errorMessage = 'Structure beneficiel est requise';
      return false;
    }
    if (!this.formData.applicationId) {
      this.errorMessage = 'Application est requise';
      return false;
    }
    if (!this.formData.periodicite) {
      this.errorMessage = 'Périodicité est requise';
      return false;
    }
    
    // NEW FINANCIAL VALIDATIONS
    if (!this.formData.montantHT || this.formData.montantHT <= 0) {
      this.errorMessage = 'Le montant HT est requis et doit être supérieur à 0';
      return false;
    }
    
    if (!this.formData.nbUsers || this.formData.nbUsers <= 0) {
      this.errorMessage = 'Le nombre d\'utilisateurs est requis et doit être supérieur à 0';
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


loadSuggestedReference(): void {
  this.conventionService.getSuggestedReference().subscribe({
    next: (response: any) => {
      if (response.success && response.suggestedReference) {
        this.formData.referenceConvention = response.suggestedReference;
      }
    },
    error: (error) => {
      console.error('Failed to load suggested reference:', error);
  
    }
  });
}

}