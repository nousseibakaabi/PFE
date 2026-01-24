import { Component, OnInit } from '@angular/core';
import { ConventionService, Convention, ConventionRequest } from '../../services/convention.service';
import { NomenclatureService, Structure } from '../../services/nomenclature.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';

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
  isEditing = false;
  searchTerm = '';
  filterEtat = '';
  loading = false;
  errorMessage = '';
  successMessage = '';

  // Form fields
  formData: ConventionRequest = {
    reference: '',
    libelle: '',
    dateDebut: '',
    dateFin: '',
    dateSignature: '',
    structureId: 0,
    gouvernoratId: 0,
    montantTotal: 0,
    modalitesPaiement: '',
    periodicite: '',
    etat: 'EN_COURS'
  };

  // Real data from API
  structures: Structure[] = [];
  gouvernorats: any[] = [];

  periodicites = ['Mensuel', 'Trimestriel', 'Semestriel', 'Annuel'];
  etats = ['EN_COURS', 'TERMINE', 'RESILIE'];

  constructor(
    private conventionService: ConventionService,
    private nomenclatureService: NomenclatureService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
          console.log('Token in localStorage:', localStorage.getItem('token'));
  console.log('Current user:', localStorage.getItem('currentUser'));
    this.loadConventions();
    this.loadStructures();
    this.loadGouvernorats();
  }

  loadConventions(): void {
    this.loading = true;
    this.conventionService.getAllConventions().subscribe({
      next: (response) => {
        if (response.success) {
          this.conventions = response.data;
          this.filteredConventions = [...this.conventions];
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading conventions:', error);
        this.errorMessage = 'Failed to load conventions';
        this.loading = false;
      }
    });
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

  loadGouvernorats(): void {
    this.nomenclatureService.getZones().subscribe({
      next: (zones) => {
        this.gouvernorats = zones;
      },
      error: (error) => {
        console.error('Error loading gouvernorats:', error);
        this.errorMessage = 'Failed to load gouvernorats';
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
      conv.reference.toLowerCase().includes(term) ||
      conv.libelle.toLowerCase().includes(term) ||
      (conv.structure?.name?.toLowerCase().includes(term)) ||
      (conv.gouvernorat?.name?.toLowerCase().includes(term))
    );
  }

  filterByEtat(conventions: Convention[]): Convention[] {
    if (!this.filterEtat) {
      return conventions;
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
      reference: '',
      libelle: '',
      dateDebut: '',
      dateFin: '',
      dateSignature: '',
      structureId: 0,
      gouvernoratId: 0,
      montantTotal: 0,
      modalitesPaiement: '',
      periodicite: '',
      etat: 'EN_COURS'
    };
    this.showModal = true;
    this.errorMessage = '';
  }

  openEditModal(convention: Convention): void {
    this.isEditing = true;
    this.selectedConvention = convention;
    this.formData = {
      reference: convention.reference,
      libelle: convention.libelle,
      dateDebut: convention.dateDebut,
      dateFin: convention.dateFin,
      dateSignature: convention.dateSignature || '',
      structureId: convention.structure?.id || 0,
      gouvernoratId: convention.gouvernorat?.id || 0,
      montantTotal: convention.montantTotal || 0,
      modalitesPaiement: convention.modalitesPaiement || '',
      periodicite: convention.periodicite || '',
      etat: convention.etat
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
              this.successMessage = 'Convention updated successfully';
              this.loadConventions();
              this.closeModal();
            }
            this.loading = false;
          },
          error: (error) => {
            this.errorMessage = error.error?.message || 'Failed to update convention';
            this.loading = false;
          }
        });
    } else {
      this.conventionService.createConvention(this.formData)
        .subscribe({
          next: (response) => {
            if (response.success) {
              this.successMessage = 'Convention created successfully';
              this.loadConventions();
              this.closeModal();
            }
            this.loading = false;
          },
          error: (error) => {
            this.errorMessage = error.error?.message || 'Failed to create convention';
            this.loading = false;
          }
        });
    }
  }

  deleteConvention(id: number): void {
    if (confirm('Are you sure you want to delete this convention?')) {
      this.loading = true;
      this.conventionService.deleteConvention(id).subscribe({
        next: (response) => {
          if (response.success) {
            this.successMessage = 'Convention deleted successfully';
            this.loadConventions();
          }
          this.loading = false;
        },
        error: (error) => {
          this.errorMessage = error.error?.message || 'Failed to delete convention';
          this.loading = false;
        }
      });
    }
  }

  validateForm(): boolean {
    if (!this.formData.reference.trim()) {
      this.errorMessage = 'Reference is required';
      return false;
    }
    if (!this.formData.libelle.trim()) {
      this.errorMessage = 'Libelle is required';
      return false;
    }
    if (!this.formData.dateDebut) {
      this.errorMessage = 'Start date is required';
      return false;
    }
    if (!this.formData.structureId) {
      this.errorMessage = 'Structure is required';
      return false;
    }
    if (!this.formData.gouvernoratId) {
      this.errorMessage = 'Gouvernorat is required';
      return false;
    }
    return true;
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  getEtatClass(etat: string): string {
    switch (etat) {
      case 'EN_COURS': return 'bg-blue-100 text-blue-800';
      case 'TERMINE': return 'bg-green-100 text-green-800';
      case 'RESILIE': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  getEtatLabel(etat: string): string {
    switch (etat) {
      case 'EN_COURS': return 'En Cours';
      case 'TERMINE': return 'Terminé';
      case 'RESILIE': return 'Résilié';
      default: return etat;
    }
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  isCommercial(): boolean {
    return this.authService.hasRole('ROLE_COMMERCIAL_METIER');
  }

  canEdit(): boolean {
    return this.isAdmin() || this.isCommercial();
  }
}