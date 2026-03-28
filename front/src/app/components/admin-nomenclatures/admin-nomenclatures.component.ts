import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { NomenclatureService, Nomenclature, Structure } from '../../services/nomenclature.service';
import { AuthService } from '../../services/auth.service';
import { TranslationService } from '../partials/traduction/translation.service';

@Component({
  selector: 'app-admin-nomenclatures',
  templateUrl: './admin-nomenclatures.component.html',
  styleUrls: ['./admin-nomenclatures.component.css'],
  standalone: false
})
export class AdminNomenclaturesComponent implements OnInit {
  activeTab:  'zones' | 'structuresR' |'structuresB' = 'zones';
  
  // Données
  zones: Nomenclature[] = [];
  structuresR: Structure[] = [];
  structuresB : Structure[]=[];
    Math = Math;
  
  // Filtres
  searchTerm: string = '';
  filteredZones: Nomenclature[] = [];
  filteredStructuresR: Structure[] = [];
  filteredStructuresB: Structure[] = [];

  
  // Pagination
  currentPage: number = 1;
  itemsPerPage: number = 10;
  
  // Formulaires
  showForm: boolean = false;
  isEditing: boolean = false;
  currentFormType:  'zone' | 'structure' = 'zone';
  currentId: number | null = null;
  
  zoneForm: FormGroup;
  structureForm: FormGroup;
  
  // Loading et erreurs
  loading: boolean = false;
  errorMessage: string = '';
  successMessage: string = '';
  
  // Stats
  stats: any = {};

  showCustomTypeInput: boolean = false;
  customTypeValue: string = '';

  showBeneficialForm: boolean = false;

  
  // Types de structures
  structureTypes: string[] = [
    'Entreprise',
    'Ministère',
    'Université',
    'Association',
    'Organisme Public',
    'Autre'
  ];

  constructor(
    private nomenclatureService: NomenclatureService,
    private authService: AuthService,
    private fb: FormBuilder,
    private router: Router,
    private translationService: TranslationService
  ) {
 
    
    // Formulaire Zone
    this.zoneForm = this.fb.group({
      code: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(20)]],
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      description: ['']
    });
    
 this.structureForm = this.fb.group({
  code: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(20)]],
  name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
  description: [''],
  phone: ['', [Validators.pattern('^[0-9+ ]*$')]],
  email: ['', [Validators.email]],
  typeStructure: ['Entreprise'],
  zoneId: [null]
});
  }



  ngOnInit(): void {
    if (!this.authService.isAdmin()) {
      this.router.navigate(['/']);
      return;
    }
    
    this.loadData();
    this.loadStats();
    this.loadZonesForDropdown();
  }


  validateCustomType(): boolean {
  if (this.showCustomTypeInput && !this.customTypeValue.trim()) {
    this.errorMessage = 'Veuillez spécifier le type de structure';
    return false;
  }
  return true;
}

  onTypeChange(): void {
  const selectedType = this.structureForm.get('typeStructure')?.value;
  this.showCustomTypeInput = selectedType === 'Autre';
  
  // Clear custom value when not showing
  if (!this.showCustomTypeInput) {
    this.customTypeValue = '';
  }
}

  loadZonesForDropdown(): void {
  this.nomenclatureService.getZones().subscribe({
    next: (zones) => {
      this.zones = zones;
    },
    error: (error) => {
      console.error('Failed to load zones for dropdown:', error);
    }
  });
}

  loadData(): void {
    this.loading = true;
    
    switch (this.activeTab) {
     
      case 'zones':
        this.loadZones();
        break;
      case 'structuresB':
        this.loadStructuresB();
        break;
      case 'structuresR':
        this.loadStructuresR();
        break;
    }
  }



  loadZones(): void {
    this.nomenclatureService.getZones().subscribe({
      next: (data) => {
        this.zones = data;
        this.filteredZones = [...data];
        this.loading = false;
      },
      error: (error) => {
        this.errorMessage = error.message;
        this.loading = false;
      }
    });
  }

  loadStructuresR(): void {
    this.nomenclatureService.getResponsableStructures().subscribe({
      next: (data) => {
        this.structuresR = data;
        this.filteredStructuresR = [...data];
        this.loading = false;
      },
      error: (error) => {
        this.errorMessage = error.message;
        this.loading = false;
      }
    });
  }


  loadStructuresB(): void {
    this.nomenclatureService.getBeneficielStructures().subscribe({
      next: (data) => {
        this.structuresB = data;
        this.filteredStructuresB = [...data];
        this.loading = false;
      },
      error: (error) => {
        this.errorMessage = error.message;
        this.loading = false;
      }
    });
  }

  loadStats(): void {
    this.nomenclatureService.getStats().subscribe({
      next: (data) => {
        this.stats = data;
      },
      error: (error) => {
        console.error('Failed to load stats:', error);
      }
    });
  }

  setActiveTab(tab:  'zones' | 'structuresR' | 'structuresB'): void {
    this.activeTab = tab;
    this.showForm = false;
    this.isEditing = false;
    this.currentId = null;
    this.resetForms();
    this.loadData();
  }

  // Recherche
  onSearch(): void {
    const term = this.searchTerm.toLowerCase();
    
    switch (this.activeTab) {
    
      case 'zones':
        this.filteredZones = this.zones.filter(item =>
          item.code.toLowerCase().includes(term) ||
          item.name.toLowerCase().includes(term) ||
          (item.description && item.description.toLowerCase().includes(term))
        );
        break;
      case 'structuresR':
        this.filteredStructuresR = this.structuresR.filter(item =>
          item.code.toLowerCase().includes(term) ||
          item.name.toLowerCase().includes(term) ||
          (item.description && item.description.toLowerCase().includes(term)) ||
          (item.email && item.email.toLowerCase().includes(term))
        );
        break;

      case 'structuresB':
        this.filteredStructuresB = this.structuresB.filter(item =>
          item.code.toLowerCase().includes(term) ||
          item.name.toLowerCase().includes(term) ||
          (item.description && item.description.toLowerCase().includes(term)) ||
          (item.email && item.email.toLowerCase().includes(term))
        );
        break;
    }
    
    this.currentPage = 1;
  }




  showEditForm(item: any, type: 'zone' | 'structure'): void {
  this.currentFormType = type;
  this.isEditing = true;
  this.currentId = item.id;
  this.showForm = true;
  
  switch (type) {
    case 'zone':
      this.zoneForm.patchValue({
        code: item.code,
        name: item.name,
        description: item.description || ''
      });
      break;
    case 'structure':
      // Check if it's a beneficiary structure (type is Client or starts with CLI-)
      const isBeneficial = item.typeStructure === 'Client' || item.code?.startsWith('CLI-');
      this.showBeneficialForm = isBeneficial;
      
      this.structureForm.patchValue({
        code: item.code,
        name: item.name,
        description: item.description || '',
        phone: item.phone || '',
        email: item.email || '',
        typeStructure: item.typeStructure || 'Entreprise',
        zoneId: item.zoneGeographique?.id || null
      });
      
      // Disable type for beneficiary structures in edit mode
      if (isBeneficial) {
        this.structureForm.get('typeStructure')?.disable();
      } else {
        this.structureForm.get('typeStructure')?.enable();
      }
      break;
  }
  }

showAddForm(type: 'zone' | 'structure'): void {
  this.currentFormType = type;
  this.isEditing = false;
  this.currentId = null;
  this.showForm = true;
  this.resetForms();
  
  // For beneficiary structures - hide code field and set type to Client
  if (type === 'structure' && this.activeTab === 'structuresB') {
    this.showBeneficialForm = true;
    
    // Set type to "Client" in the form
    this.structureForm.patchValue({
      typeStructure: 'Client'
    });
    
    // Make typeStructure field readonly/disabled
    this.structureForm.get('typeStructure')?.disable();
    
    console.log('Beneficial form - type set to:', this.structureForm.get('typeStructure')?.value);
  } else {
    this.showBeneficialForm = false;
    this.structureForm.get('typeStructure')?.enable();
  }
}

resetForms(): void {
  this.zoneForm.reset();
  this.structureForm.reset({ 
    typeStructure: 'Entreprise', // Default for non-beneficiary
    zoneId: null,
    code: '' 
  });
  this.showCustomTypeInput = false;
  this.customTypeValue = '';
  this.showBeneficialForm = false;
  this.structureForm.get('typeStructure')?.enable();
}

  cancelForm(): void {
    this.showForm = false;
    this.isEditing = false;
    this.currentId = null;
    this.resetForms();
    this.errorMessage = '';
    this.successMessage = '';
  }


  onClientNameChange(): void {
  if (this.activeTab === 'structuresB' && !this.isEditing) {
    const clientName = this.structureForm.get('name')?.value;
    if (clientName && clientName.length >= 2) {
      this.nomenclatureService.generateClientCode(clientName).subscribe({
        next: (response) => {
          if (response.success) {
            this.structureForm.patchValue({
              code: response.code
            });
          }
        },
        error: (error) => {
          console.error('Failed to generate client code:', error);
        }
      });
    }
  }
}

onSubmitZone(): void {
  if (this.zoneForm.invalid) {
    this.markFormGroupTouched(this.zoneForm);
    return;
  }
  
  const formData = this.zoneForm.value;
  
  // Check if trying to create a Tunisian zone
  if (!this.isEditing && formData.code.startsWith('TN-')) {
    this.errorMessage = 'TN- codes are reserved for Tunisian governorates. Use a different code for custom zones.';
    return;
  }
  
  this.loading = true;
  
  if (this.isEditing && this.currentId) {
    this.nomenclatureService.updateZone(this.currentId, formData).subscribe({
      next: (response) => {
        this.handleSuccess(response.message || 'Zone updated successfully');
        this.loadZones();
      },
      error: (error) => {
        this.handleError(error.message);
      }
    });
  } else {
    this.nomenclatureService.createZone(formData).subscribe({
      next: (response) => {
        this.handleSuccess(response.message || 'Zone created successfully');
        this.loadZones();
      },
      error: (error) => {
        this.handleError(error.message);
      }
    });
  }
}



onSubmitStructure(): void {
  if (this.structureForm.invalid) {
    this.markFormGroupTouched(this.structureForm);
    return;
  }

  if (!this.validateCustomType()) {
    return;
  }
  
  this.loading = true;
  const formData = this.structureForm.getRawValue(); // Use getRawValue() to get disabled fields
  
  // If "Autre" is selected, use the custom type value
  if (formData.typeStructure === 'Autre' && this.customTypeValue.trim()) {
    formData.typeStructure = this.customTypeValue.trim();
  }
  
  // FOR BENEFICIARY STRUCTURES - Force typeStructure to "Client"
  if (this.activeTab === 'structuresB' && !this.isEditing) {
    formData.typeStructure = 'Client';
  }
  
  // Also for editing beneficiary structures, keep it as Client
  if (this.activeTab === 'structuresB' && this.isEditing) {
    formData.typeStructure = 'Client';
  }
  
  if (this.isEditing && this.currentId) {
    this.nomenclatureService.updateStructure(this.currentId, formData).subscribe({
      next: (response) => {
        this.handleSuccess(response.message || 'Structure mise à jour avec succès');
        this.loadStructuresB();
        this.loadStructuresR();
      },
      error: (error) => {
        this.handleError(error.message);
      }
    });
  } else {
    this.nomenclatureService.createStructure(formData).subscribe({
      next: (response) => {
        this.handleSuccess(response.message || 'Structure créée avec succès');
        this.loadStructuresB();
        this.loadStructuresR();
      },
      error: (error) => {
        this.handleError(error.message);
      }
    });
  }
}

  confirmDelete(item: any, type: 'zone' | 'structure'): void {
    if (confirm(`Êtes-vous sûr de vouloir supprimer "${item.name}" ?`)) {
      this.deleteItem(item.id, type);
    }
  }

  deleteItem(id: number, type: 'zone' | 'structure'): void {
    this.loading = true;
    
    let deleteObservable;
    switch (type) {
      
      case 'zone':
        deleteObservable = this.nomenclatureService.deleteZone(id);
        break;
      case 'structure':
        deleteObservable = this.nomenclatureService.deleteStructure(id);
        break;
    }
    
    deleteObservable.subscribe({
      next: (response) => {
        this.handleSuccess(response.message || 'Élément supprimé avec succès');
        this.loadData();
      },
      error: (error) => {
        this.handleError(error.message);
      }
    });
  }

  // Utilitaires
  private handleSuccess(message: string): void {
    this.successMessage = message;
    this.errorMessage = '';
    this.loading = false;
    setTimeout(() => {
      this.showForm = false;
      this.successMessage = '';
    }, 3000);
  }

  private handleError(message: string): void {
    this.errorMessage = message;
    this.successMessage = '';
    this.loading = false;
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.values(formGroup.controls).forEach(control => {
      control.markAsTouched();
      if (control instanceof FormGroup) {
        this.markFormGroupTouched(control);
      }
    });
  }

  // Pagination
  get paginatedItems(): any[] {
    const items = this.getCurrentItems();
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    return items.slice(startIndex, startIndex + this.itemsPerPage);
  }

  get totalPages(): number {
    const items = this.getCurrentItems();
    return Math.ceil(items.length / this.itemsPerPage);
  }

  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }

  private getCurrentItems(): any[] {
    switch (this.activeTab) {
      case 'zones': return this.filteredZones;
      case 'structuresR': return this.filteredStructuresR;
      case 'structuresB': return this.filteredStructuresB;

      default: return [];
    }
  }

  changePage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }


  isTunisianZone(zone: Nomenclature): boolean {
  return zone.type === 'TUNISIAN_ZONE';
}



}