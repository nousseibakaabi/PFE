import { Component, OnInit , HostListener} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ConventionService, Convention, ConventionRequest } from '../../services/convention.service';
import { NomenclatureService, Structure } from '../../services/nomenclature.service';
import { ApplicationService, ApiResponse } from '../../services/application.service';
import { AuthService } from '../../services/auth.service';
import { TranslationService } from '../partials/traduction/translation.service';


@Component({
  selector: 'app-convention-form',
  templateUrl: './convention-form.component.html',
  styleUrls: ['./convention-form.component.css'],
  standalone: false
})
export class ConventionFormComponent implements OnInit {
  conventionId: number | null = null;
  isEditing = false;
  loading = false;
  errorMessage = '';
  successMessage = '';

  // Form Data
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
    nbUsers: 0,
    montantHT: 0,
    montantTTC: 0,
    tva: 0,
  };

  // Lists for dropdowns
  applications: any[] = [];
  internesStructures: Structure[] = [];
  externesStructures: Structure[] = [];
  periodicites = ['MENSUEL', 'TRIMESTRIEL', 'SEMESTRIEL', 'ANNUEL'];

  // User limits and rules
  userLimits: { minUser?: number; maxUser?: number } | null = null;
  userRuleMessage: string = '';
  isCalculatingTTC: boolean = false;
  isDeterminingUsers: boolean = false;

  // Real-time validation errors
  validationErrors: { [key: string]: string } = {};

  // Date max values for blocking future dates
  dateDebut_maxDate: string = ''; // No future date limit for debut
  dateFin_minDate: string = ''; // Min 15 days after debut
  dateFin_maxDate: string = ''; // No max date for fin
  dateSignature_minDate: string = ''; // Cannot be before today

  showApplicationDropdown: boolean = false;
  applicationSearchTerm: string = '';
  filteredApplications: any[] = [];

  showBeneficielDropdown: boolean = false;
  showResponsableDropdown: boolean = false;
  showPeriodiciteDropdown: boolean = false;

  // Search terms
  beneficielSearchTerm: string = '';
  responsableSearchTerm: string = '';

  // Filtered lists
  filteredBeneficielStructures: any[] = [];
  filteredResponsableStructures: any[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private conventionService: ConventionService,
    private nomenclatureService: NomenclatureService,
    private applicationService: ApplicationService,
    private authService: AuthService,
    private translationService: TranslationService
  ) {
    // Initialize min date for date signature (today)
    this.setMinDateSignature();
  }

  /**
   * ===== REAL-TIME VALIDATION METHODS =====
   */
  
  getTodayDateString(): string {
    const today = new Date();
    return today.toISOString().split('T')[0];
  }

  private addDays(dateStr: string, days: number): string {
    const date = new Date(dateStr);
    date.setDate(date.getDate() + days);
    return date.toISOString().split('T')[0];
  }

  setMinDateSignature(): void {
    this.dateSignature_minDate = this.getTodayDateString();
  }

  validateReferenceConvention(): void {
    const ref = this.formData.referenceConvention?.trim() || '';
    
    delete this.validationErrors['referenceConvention'];
    
    if (!ref) {
this.validationErrors['referenceConvention'] = this.translationService.translate('Référence Convention est requise');
      return;
    }
    
    const pattern = /^CONV-\d{4}-\d{3}$/;
    if (!pattern.test(ref)) {
this.validationErrors['referenceConvention'] = this.translationService.translate('Format: CONV-YYYY-XXX (ex: CONV-2024-001)');
      return;
    }
  }

  validateReferenceERP(): void {
    const ref = this.formData.referenceERP?.trim() || '';
    delete this.validationErrors['referenceERP'];
    
    if (!ref) {
this.validationErrors['referenceERP'] = this.translationService.translate('Référence ERP est requise');    }
  }

  validateLibelle(): void {
    const libelle = this.formData.libelle?.trim() || '';
    delete this.validationErrors['libelle'];
    
    if (!libelle) {
this.validationErrors['libelle'] = this.translationService.translate('Libellé est requis');    }
  }

  validateApplication(): void {
    delete this.validationErrors['applicationId'];
    
    if (!this.formData.applicationId) {
this.validationErrors['applicationId'] = this.translationService.translate('Application est requise');    }
  }

  validateStructureBeneficiel(): void {
    delete this.validationErrors['structureBeneficielId'];
    
    if (!this.formData.structureBeneficielId) {
this.validationErrors['structureBeneficielId'] = this.translationService.translate('Structure Bénéficiaire est requise');    }
  }

  validateStructureResponsable(): void {
    delete this.validationErrors['structureResponsableId'];
    
    if (!this.formData.structureResponsableId) {
this.validationErrors['structureResponsableId'] = this.translationService.translate('Structure Responsable est requise');    }
  }

  validatePeriodicite(): void {
    delete this.validationErrors['periodicite'];
    
    if (!this.formData.periodicite) {
      this.validationErrors['periodicite'] = this.translationService.translate('Modalités de paiement est requise');
    }
  }

  validateDateDebut(): void {
    const dateDebut = this.formData.dateDebut?.trim() || '';
    delete this.validationErrors['dateDebut'];
    
    if (!dateDebut) {
      this.validationErrors['dateDebut'] = this.translationService.translate('Date de début est requise');
      return;
    }
    
    const today = this.getTodayDateString();
    if (dateDebut < today) {
      this.validationErrors['dateDebut'] = this.translationService.translate('La date de début ne peut pas être antérieure à aujourd\'hui');
      return;
    }
    
    // Update min date for dateFin (15 days after debut)
    this.dateFin_minDate = this.addDays(dateDebut, 15);
  }

  validateDateFin(): void {
    const dateFin = this.formData.dateFin?.trim() || '';
    const dateDebut = this.formData.dateDebut?.trim() || '';
    
    delete this.validationErrors['dateFin'];
    
    if (!dateFin) {
      this.validationErrors['dateFin'] = this.translationService.translate('Date de fin est requise');
      return;
    }
    
    if (!dateDebut) {
      this.validationErrors['dateFin'] = this.translationService.translate('Sélectionnez d\'abord la date de début');
      return;
    }
    
    const minFin = this.addDays(dateDebut, 15);
    if (dateFin < minFin) {
      this.validationErrors['dateFin'] = this.translationService.translate('La date de fin doit être au minimum 15 jours après la date de début');
      return;
    }
  }

  validateDateSignature(): void {
    const dateSignature = this.formData.dateSignature?.trim() || '';
    delete this.validationErrors['dateSignature'];
    
    if (!dateSignature) {
      return; // Date signature is optional
    }
    
    const today = this.getTodayDateString();
    if (dateSignature < today) {
      this.validationErrors['dateSignature'] = this.translationService.translate('La date de signature ne peut pas être antérieure à aujourd\'hui');
      return;
    }
  }

  validateMontantHT(): void {
    delete this.validationErrors['montantHT'];
    
    if (!this.formData.montantHT || this.formData.montantHT <= 0) {
      this.validationErrors['montantHT'] = this.translationService.translate('Montant HT doit être supérieur à 0');
    }
  }

  validateTVA(): void {
    delete this.validationErrors['tva'];
    
    if (this.formData.tva < 0 || this.formData.tva > 100) {
      this.validationErrors['tva'] = this.translationService.translate('TVA doit être entre 0 et 100%');
    }
  }

  validateNbUsers(): void {
    delete this.validationErrors['nbUsers'];
    
    if (!this.formData.nbUsers || this.formData.nbUsers <= 0) {
      this.validationErrors['nbUsers'] = this.translationService.translate('Nombre d\'utilisateurs doit être supérieur à 0');
      return;
    }
    
    if (this.userLimits) {
      if (this.userLimits.minUser && this.formData.nbUsers < this.userLimits.minUser) {
        this.validationErrors['nbUsers'] = this.translationService.translate(`Minimum: ${this.userLimits.minUser} utilisateurs`);
        return;
      }
      if (this.userLimits.maxUser && this.formData.nbUsers > this.userLimits.maxUser) {
        this.validationErrors['nbUsers'] = this.translationService.translate(`Maximum: ${this.userLimits.maxUser} utilisateurs`);
        return;
      }
    }
  }

  hasError(fieldName: string): boolean {
    return !!this.validationErrors[fieldName];
  }

  getErrorMessage(fieldName: string): string {
    return this.validationErrors[fieldName] || '';
  }

  /**
   * ===== END VALIDATION METHODS =====
   */

ngOnInit(): void {
  // Check if user has permission
  if (!this.canEdit()) {
    this.router.navigate(['/conventions']);
    return;
  }

  // Load structures (these are needed for both create and edit)
  this.loadResponsablesStructures();
  this.loadBeneficielsStructures();

  this.filteredBeneficielStructures = [...this.externesStructures];
  this.filteredResponsableStructures = [...this.internesStructures];

  // Check if we're editing (has id in route)
  this.route.params.subscribe(params => {
    if (params['id']) {
      this.conventionId = +params['id'];
      this.isEditing = true;
      
      // For editing, load ALL applications (not just those without conventions)
      this.loadAllApplicationsForEdit();
      
      // Then load the convention data
      this.loadConvention(this.conventionId);
    } else {
      // For new convention - load ONLY applications without conventions
      this.loadApplicationsWithoutConventions();
      
      // Load suggested reference
      this.loadSuggestedReference();
    }
  });
}

/**
 * Load applications that DON'T have conventions (for creating new convention)
 */
loadApplicationsWithoutConventions(): void {
  this.loading = true;
  this.applicationService.getApplicationsWithoutConventions().subscribe({
    next: (response: ApiResponse) => {
      if (response && response.success) {
        this.applications = response.data || [];
        this.filteredApplications = [...this.applications];
        this.errorMessage = '';
      } else {
        this.errorMessage = this.translationService.translate(response?.message || 'Failed to load applications');
        this.applications = [];
        this.filteredApplications = [];
      }
      this.loading = false;
    },
    error: (error: any) => {
      console.error('Error loading applications:', error);
      this.errorMessage = this.translationService.translate(error.error?.message || 'Failed to load applications');
      this.applications = [];
      this.filteredApplications = [];
      this.loading = false;
    }
  });
}

/**
 * Load ALL applications (for editing mode)
 */
loadAllApplicationsForEdit(): void {
  this.loading = true;
  this.applicationService.getAllApplications().subscribe({
    next: (response: ApiResponse) => {
      if (response && response.success) {
        this.applications = response.data || [];
        this.filteredApplications = [...this.applications];
        console.log('All applications loaded for edit:', this.applications);
      } else {
        console.error('Failed to load applications for edit');
        this.applications = [];
        this.filteredApplications = [];
      }
      this.loading = false;
    },
    error: (error) => {
      console.error('Error loading applications for edit:', error);
      this.applications = [];
      this.filteredApplications = [];
      this.loading = false;
    }
  });
}



loadConvention(id: number): void {
  this.loading = true;
  this.conventionService.getConvention(id).subscribe({
    next: (response) => {
      if (response.success) {
        const convention = response.data;
        this.formData = {
          referenceConvention: convention.referenceConvention,
          referenceERP: convention.referenceERP || '',
          libelle: convention.libelle,
          dateDebut: convention.dateDebut,
          dateFin: convention.dateFin || '',
          dateSignature: convention.dateSignature || '',
          structureResponsableId: convention.structureResponsableId || 0,
          structureBeneficielId: convention.structureBeneficielId || 0,
          applicationId: convention.applicationId || 0,  // This should now find the app in the list
          montantHT: convention.montantHT || 0,
          tva: convention.tva || 0,
          montantTTC: convention.montantTTC || 0,
          nbUsers: convention.nbUsers || 0,
          periodicite: convention.periodicite || 'MENSUEL'
        };
        
        console.log('Loaded convention with applicationId:', convention.applicationId);
        console.log('Applications available:', this.applications);
        
        // Load application limits
        if (convention.applicationId) {
          this.loadApplicationLimits(convention.applicationId);
        }
      }
      this.loading = false;
    },
    error: (error) => {
      console.error('Error loading convention:', error);
      this.errorMessage = 'Erreur lors du chargement de la convention';
      this.loading = false;
    }
  });
}


  loadResponsablesStructures(): void {
    this.nomenclatureService.getResponsableStructures().subscribe({
      next: (structures) => {
        this.internesStructures = structures;
      },
      error: (error) => {
        console.error('Error loading Responsables structures:', error);
      }
    });
  }

  loadBeneficielsStructures(): void {
    this.nomenclatureService.getBeneficielStructures().subscribe({
      next: (structures) => {
        this.externesStructures = structures;
      },
      error: (error) => {
        console.error('Error loading Beneficiels structures:', error);
      }
    });
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

  onNbUsersFocus(): void {
    if (this.formData.applicationId && !this.formData.nbUsers) {
      this.determineNbUsers();
    }
  }

  onNbUsersChange(): void {
    if (this.formData.applicationId) {
      this.determineNbUsers();
    }
  }

  determineNbUsers(): void {
    if (!this.formData.applicationId) {
      this.userRuleMessage = this.translationService.translate('Veuillez d\'abord sélectionner une application');
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
          
          if (data.nbUsers !== this.formData.nbUsers) {
            this.formData.nbUsers = data.nbUsers;
            this.userRuleMessage = `${data.appliedRule} (${data.nbUsers} utilisateurs)`;
          } else {
            this.userRuleMessage = data.appliedRule;
          }
          
          this.userLimits = {
            minUser: data.minUser,
            maxUser: data.maxUser
          };
        }
        
        this.isDeterminingUsers = false;
      },
      error: (error) => {
        console.error('Error determining nb users:', error);
this.userRuleMessage = this.translationService.translate('Erreur lors de la détermination du nombre d\'utilisateurs');
        this.isDeterminingUsers = false;
      }
    });
  }

  onMontantHTChange(): void {
    this.validateMontantHT();
    this.calculateTTC();
  }

  onTvaChange(): void {
    this.validateTVA();
    this.calculateTTC();
  }

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

  getApplicationName(applicationId: number): string {
    const app = this.applications.find(a => a.id === applicationId);
    return app ? `${app.code} - ${app.name}` : '';
  }

  validateReference(): boolean {
    const reference = this.formData.referenceConvention;
    const pattern = /^CONV-\d{4}-\d{3}$/;
    if (!pattern.test(reference)) {
this.errorMessage = this.translationService.translate('Le format de référence doit être: CONV-YYYY-XXX (ex: CONV-2024-001)');
      return false;
    }
    return true;
  }


  validateForm(): boolean {
    if (!this.formData.referenceConvention?.trim()) {
  this.errorMessage = this.translationService.translate('Référence Convention est requise');
      return false;
    }

    if (!this.validateReference()) {
      return false;
    }

    if (!this.formData.referenceERP?.trim()) {
      this.errorMessage = this.translationService.translate('Référence ERP est requise');
      return false;
    }
    
    if (!this.formData.libelle?.trim()) {
      this.errorMessage = this.translationService.translate('Libellé est requis');
      return false;
    }
    if (!this.formData.dateDebut) {
      this.errorMessage = this.translationService.translate('Date de début est requise');
      return false;
    }

    if (!this.formData.dateFin) {
      this.errorMessage = this.translationService.translate('Date de fin est requise');
      return false;
    }

    if (!this.formData.structureResponsableId) { 
      this.errorMessage = this.translationService.translate('Structure interne est requise');
      return false;
    }
    if (!this.formData.structureBeneficielId) {
      this.errorMessage = this.translationService.translate('Structure beneficiel est requise');
      return false;
    }
    if (!this.formData.applicationId) {
      this.errorMessage = this.translationService.translate('Application est requise');
      return false;
    }
    if (!this.formData.periodicite) {
      this.errorMessage = this.translationService.translate('Périodicité est requise');
      return false;
    }
    
    if (!this.formData.montantHT || this.formData.montantHT <= 0) {
      this.errorMessage = this.translationService.translate('Le montant HT est requis et doit être supérieur à 0');
      return false;
    }
    
    if (!this.formData.nbUsers || this.formData.nbUsers <= 0) {
      this.errorMessage = this.translationService.translate('Le nombre d\'utilisateurs est requis et doit être supérieur à 0');
      return false;
    }
    
    return true;
  }


  // In convention-form.component.ts, add this method
onPeriodiciteChange(): void {
  console.log('Periodicite changed to:', this.formData.periodicite);
}

saveConvention(): void {
  if (!this.validateForm()) {
    return;
  }

  // Debug logging
  console.log('========== SUBMITTING CONVENTION ==========');
  console.log('Periodicite selected:', this.formData.periodicite);
  console.log('Date Debut:', this.formData.dateDebut);
  console.log('Date Fin:', this.formData.dateFin);
  console.log('Full form data:', JSON.stringify(this.formData, null, 2));

  // Ensure TTC is calculated
  if (this.formData.montantHT && this.formData.montantHT > 0 && !this.formData.montantTTC) {
    this.calculateTTC();
  }

  // Ensure nb users is determined
  if (this.formData.applicationId && !this.formData.nbUsers) {
    this.determineNbUsers();
  }

  this.loading = true;
  
  if (this.isEditing && this.conventionId) {
    this.conventionService.updateConvention(this.conventionId, this.formData)
      .subscribe({
        next: (response) => {
          console.log('Update response:', response);
          if (response.success) {
            this.successMessage = 'Convention mise à jour avec succès';
            setTimeout(() => {
              this.router.navigate(['/conventions', this.conventionId]);
            }, 1500);
          }
          this.loading = false;
        },
        error: (error) => {
          console.error('Update error:', error);
          this.errorMessage = error.error?.message || 'Échec de la mise à jour de la convention';
          this.loading = false;
        }
      });
  } else {
    this.conventionService.createConvention(this.formData)
      .subscribe({
        next: (response) => {
          console.log('Create response:', response);
          if (response.success) {
            this.successMessage = 'Convention créée avec succès';
            setTimeout(() => {
              this.router.navigate(['/conventions', response.data.id]);
            }, 1500);
          }
          this.loading = false;
        },
        error: (error) => {
          console.error('Create error:', error);
          this.errorMessage = error.error?.message || 'Échec de la création de la convention';
          this.loading = false;
        }
      });
  }
}

  cancel(): void {
    if (this.isEditing && this.conventionId) {
      this.router.navigate(['/conventions', this.conventionId]);
    } else {
      this.router.navigate(['/conventions']);
    }
  }

  canEdit(): boolean {
    return  this.authService.isCommercial();
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }



// Toggle dropdown
// Toggle application dropdown
toggleApplicationDropdown(): void {
  if (!this.isEditing) {
    this.showApplicationDropdown = !this.showApplicationDropdown;
    if (this.showApplicationDropdown) {
      this.applicationSearchTerm = '';  // Reset search term
      this.filteredApplications = [...this.applications];  // Reset filtered list
    }
  }
}

filterApplications(): void {
  console.log('Searching with term:', this.applicationSearchTerm);
  console.log('Applications count:', this.applications.length);
  
  if (!this.applicationSearchTerm || !this.applicationSearchTerm.trim()) {
    this.filteredApplications = [...this.applications];
    console.log('Reset filtered applications:', this.filteredApplications.length);
    return;
  }
  
  const term = this.applicationSearchTerm.toLowerCase().trim();
  this.filteredApplications = this.applications.filter(app => {
    const candidate = `${app.code || ''} ${app.name || ''}`.toLowerCase();
    return candidate.includes(term);
  });
  console.log('Filtered applications:', this.filteredApplications.length);
}


// Select application
selectApplication(app: any): void {
  this.formData.applicationId = app.id;
  this.showApplicationDropdown = false;
  this.onProjectSelected(app.id);
}

// Get selected application name
getSelectedApplicationName(): string {
  if (!this.formData.applicationId) return '';
  const app = this.applications.find(a => a.id === this.formData.applicationId);
  return app ? `${app.code} - ${app.name}` : '';
}

@HostListener('document:click', ['$event'])
onDocumentClick(event: MouseEvent): void {
  const target = event.target as HTMLElement;
  if (!target.closest('.relative')) {
    this.showApplicationDropdown = false;
    this.showBeneficielDropdown = false;
    this.showResponsableDropdown = false;
    this.showPeriodiciteDropdown = false;
  }
}


// Toggle dropdowns
toggleDropdown(type: string): void {
  if (type === 'beneficiel' && !this.isEditing) {
    this.showBeneficielDropdown = !this.showBeneficielDropdown;
    this.showResponsableDropdown = false;
    this.showPeriodiciteDropdown = false;
    this.showApplicationDropdown = false;
    if (this.showBeneficielDropdown) {
      this.beneficielSearchTerm = '';
      this.filteredBeneficielStructures = [...this.externesStructures];
    }
  } else if (type === 'responsable') {
    this.showResponsableDropdown = !this.showResponsableDropdown;
    this.showBeneficielDropdown = false;
    this.showPeriodiciteDropdown = false;
    this.showApplicationDropdown = false;
    if (this.showResponsableDropdown) {
      this.responsableSearchTerm = '';
      this.filteredResponsableStructures = [...this.internesStructures];
    }
  } else if (type === 'periodicite') {
    this.showPeriodiciteDropdown = !this.showPeriodiciteDropdown;
    this.showBeneficielDropdown = false;
    this.showResponsableDropdown = false;
    this.showApplicationDropdown = false;
  }
}

// Filter beneficiel structures
filterBeneficielStructures(): void {
  if (!this.beneficielSearchTerm.trim()) {
    this.filteredBeneficielStructures = [...this.externesStructures];
    return;
  }
  
  const term = this.beneficielSearchTerm.toLowerCase().trim();
  this.filteredBeneficielStructures = this.externesStructures.filter(s => 
    s.code?.toLowerCase().includes(term) || 
    s.name?.toLowerCase().includes(term)
  );
}

// Filter responsable structures
filterResponsableStructures(): void {
  if (!this.responsableSearchTerm.trim()) {
    this.filteredResponsableStructures = [...this.internesStructures];
    return;
  }
  
  const term = this.responsableSearchTerm.toLowerCase().trim();
  this.filteredResponsableStructures = this.internesStructures.filter(s => 
    s.code?.toLowerCase().includes(term) || 
    s.name?.toLowerCase().includes(term)
  );
}

// Select beneficiel structure
selectBeneficielStructure(structure: any): void {
  this.formData.structureBeneficielId = structure.id;
  this.showBeneficielDropdown = false;
  this.validateStructureBeneficiel();
}

// Select responsable structure
selectResponsableStructure(structure: any): void {
  this.formData.structureResponsableId = structure.id;
  this.showResponsableDropdown = false;
  this.validateStructureResponsable();
}

// Select periodicite
selectPeriodicite(periodicite: string): void {
  this.formData.periodicite = periodicite;
  this.showPeriodiciteDropdown = false;
  this.validatePeriodicite();
  this.onPeriodiciteChange();
}

// Get selected names
getSelectedBeneficielName(): string {
  if (!this.formData.structureBeneficielId) return '';
  const structure = this.externesStructures.find(s => s.id === this.formData.structureBeneficielId);
  return structure ? `${structure.code} - ${structure.name}` : '';
}

getSelectedResponsableName(): string {
  if (!this.formData.structureResponsableId) return '';
  const structure = this.internesStructures.find(s => s.id === this.formData.structureResponsableId);
  return structure ? `${structure.code} - ${structure.name}` : '';
}


// Add this method to your ConventionFormComponent class
onDateChange(type: string, date: string): void {
  console.log(`Date ${type} changed to:`, date);
  
  if (type === 'debut') {
    this.validateDateDebut();
    // Auto-reset dateFin if it's now invalid
    if (this.formData.dateFin && this.formData.dateFin < this.dateFin_minDate) {
      this.formData.dateFin = '';
      delete this.validationErrors['dateFin'];
    }
  } else if (type === 'fin') {
    this.validateDateFin();
  } else if (type === 'signature') {
    this.validateDateSignature();
  }
  
  this.errorMessage = '';
}

}