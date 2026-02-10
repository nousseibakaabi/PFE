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
  activeTab:  'zones' | 'structures' = 'zones';
  
  // Données
  zones: Nomenclature[] = [];
  structures: Structure[] = [];
  
  // Filtres
  searchTerm: string = '';
  filteredZones: Nomenclature[] = [];
  filteredStructures: Structure[] = [];
  
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
    
    // Formulaire Structure
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
      case 'structures':
        this.loadStructures();
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

  loadStructures(): void {
    this.nomenclatureService.getStructures().subscribe({
      next: (data) => {
        this.structures = data;
        this.filteredStructures = [...data];
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

  setActiveTab(tab:  'zones' | 'structures'): void {
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
      case 'structures':
        this.filteredStructures = this.structures.filter(item =>
          item.code.toLowerCase().includes(term) ||
          item.name.toLowerCase().includes(term) ||
          (item.description && item.description.toLowerCase().includes(term)) ||
          (item.email && item.email.toLowerCase().includes(term))
        );
        break;
    }
    
    this.currentPage = 1;
  }

  // Gestion des formulaires
  showAddForm(type:  'zone' | 'structure'): void {
    this.currentFormType = type;
    this.isEditing = false;
    this.currentId = null;
    this.showForm = true;
    this.resetForms();
  }

  showEditForm(item: any, type:   'zone' | 'structure'): void {
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
        this.structureForm.patchValue({
          code: item.code,
          name: item.name,
          description: item.description || '',
          phone: item.phone || '',
          email: item.email || '',
          typeStructure: item.typeStructure || 'Entreprise',
          zoneId: item.zoneGeographique?.id || null
        });
        break;
    }
  }

resetForms(): void {
  this.zoneForm.reset();
  this.structureForm.reset({ 
    typeStructure: 'Entreprise',
    zoneId: null 
  });
}

  cancelForm(): void {
    this.showForm = false;
    this.isEditing = false;
    this.currentId = null;
    this.resetForms();
    this.errorMessage = '';
    this.successMessage = '';
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
  
  this.loading = true;
  const formData = this.structureForm.value;
  
  // Convert zoneId to zone object if needed (depends on your backend)
  // The backend expects zoneId in the StructureRequest
  
  if (this.isEditing && this.currentId) {
    this.nomenclatureService.updateStructure(this.currentId, formData).subscribe({
      next: (response) => {
        this.handleSuccess(response.message || 'Structure mise à jour avec succès');
        this.loadStructures();
      },
      error: (error) => {
        this.handleError(error.message);
      }
    });
  } else {
    this.nomenclatureService.createStructure(formData).subscribe({
      next: (response) => {
        this.handleSuccess(response.message || 'Structure créée avec succès');
        this.loadStructures();
      },
      error: (error) => {
        this.handleError(error.message);
      }
    });
  }
}

  // Suppression
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
      case 'structures': return this.filteredStructures;
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