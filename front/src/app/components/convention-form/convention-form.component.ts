import { Component, OnInit , HostListener} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ConventionService, Convention, ConventionRequest } from '../../services/convention.service';
import { NomenclatureService, Structure } from '../../services/nomenclature.service';
import { ApplicationService, ApiResponse } from '../../services/application.service';
import { AuthService } from '../../services/auth.service';

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
    tva: 19,
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
    private authService: AuthService
  ) {}

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

/**
 * Load ALL applications (for editing mode)
 */
loadAllApplicationsForEdit(): void {
  this.loading = true;
  this.applicationService.getAllApplications().subscribe({
    next: (response: ApiResponse) => {
      if (response && response.success) {
        this.applications = response.data || [];
        console.log('All applications loaded for edit:', this.applications);
      } else {
        console.error('Failed to load applications for edit');
      }
      this.loading = false;
    },
    error: (error) => {
      console.error('Error loading applications for edit:', error);
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
          tva: convention.tva || 19,
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
        this.userRuleMessage = 'Erreur lors de la détermination du nombre d\'utilisateurs';
        this.isDeterminingUsers = false;
      }
    });
  }

  onMontantHTChange(): void {
    this.calculateTTC();
  }

  onTvaChange(): void {
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
      this.errorMessage = 'Le format de référence doit être: CONV-YYYY-XXX (ex: CONV-2024-001)';
      return false;
    }
    return true;
  }

  validateForm(): boolean {
    if (!this.formData.referenceConvention?.trim()) {
      this.errorMessage = 'Référence Convention est requise';
      return false;
    }

    if (!this.validateReference()) {
      return false;
    }

    if (!this.formData.referenceERP?.trim()) {
      this.errorMessage = 'Référence ERP est requise';
      return false;
    }
    
    if (!this.formData.libelle?.trim()) {
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
toggleApplicationDropdown(): void {
  if (!this.isEditing) {
    this.showApplicationDropdown = !this.showApplicationDropdown;
    if (this.showApplicationDropdown) {
      this.applicationSearchTerm = '';
      this.filteredApplications = [...this.applications];
    }
  }
}

// Filter applications
filterApplications(): void {
  if (!this.applicationSearchTerm.trim()) {
    this.filteredApplications = [...this.applications];
    return;
  }
  
  const term = this.applicationSearchTerm.toLowerCase().trim();
  this.filteredApplications = this.applications.filter(app => 
    app.code?.toLowerCase().includes(term) || 
    app.name?.toLowerCase().includes(term)
  );
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
}

// Select responsable structure
selectResponsableStructure(structure: any): void {
  this.formData.structureResponsableId = structure.id;
  this.showResponsableDropdown = false;
}

// Select periodicite
selectPeriodicite(periodicite: string): void {
  this.formData.periodicite = periodicite;
  this.showPeriodiciteDropdown = false;
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
  
  // You can add validation logic here
  if (type === 'debut' && this.formData.dateFin) {
    // Check if date fin is before date debut
    const debut = new Date(date);
    const fin = new Date(this.formData.dateFin);
    
    if (fin < debut) {
      // If fin is before debut, reset fin
      this.formData.dateFin = '';
      this.errorMessage = 'La date de fin ne peut pas être antérieure à la date de début';
    }
  }
  
  if (type === 'fin' && this.formData.dateDebut) {
    // Check if date fin is before date debut
    const debut = new Date(this.formData.dateDebut);
    const fin = new Date(date);
    
    if (fin < debut) {
      this.errorMessage = 'La date de fin ne peut pas être antérieure à la date de début';
    } else {
      this.errorMessage = ''; // Clear error if valid
    }
  }
}

}