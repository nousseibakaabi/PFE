import { Component, OnInit } from '@angular/core';
import { FactureService, Facture, FactureRequest, PaiementRequest } from '../../services/facture.service';
import { ConventionService } from '../../services/convention.service';
import { NomenclatureService } from '../../services/nomenclature.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-facture',
  templateUrl: './facture.component.html',
  styleUrls: ['./facture.component.css'],
  standalone: false
})
export class FactureComponent implements OnInit {
  factures: Facture[] = [];
  filteredFactures: Facture[] = [];
  conventions: any[] = [];
  structures: any[] = []; // Added for reference
  selectedFacture: Facture | null = null;
  showInvoiceModal = false;
  showPaymentModal = false;
  isGenerating = false;
  searchTerm = '';
  filterStatut = '';
  loading = false;
  errorMessage = '';
  successMessage = '';

  // Invoice form
  invoiceForm: FactureRequest = {
    conventionId: 0,
    dateFacturation: '',
    dateEcheance: '',
    montantHT: 0,
    tva: 19,
    notes: ''
  };

  // Payment form
  paymentForm: PaiementRequest = {
    factureId: 0,
    referencePaiement: '',
    datePaiement: ''
  };

  // Real data from API
  modesPaiement = ['Virement', 'Chèque', 'Espèces'];
  statuts = ['PAYE', 'NON_PAYE', 'EN_RETARD'];

  constructor(
    private factureService: FactureService,
    private conventionService: ConventionService,
    private nomenclatureService: NomenclatureService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
      console.log('Token in localStorage:', localStorage.getItem('token'));
  console.log('Current user:', localStorage.getItem('currentUser'));
  
    this.loadFactures();
    this.loadConventions();
    this.loadStructures(); // Optional: for reference
  }

  loadFactures(): void {
    this.loading = true;
    this.factureService.getAllFactures().subscribe({
      next: (response) => {
        if (response.success) {
          this.factures = response.data;
          this.filteredFactures = [...this.factures];
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading invoices:', error);
        this.errorMessage = 'Failed to load invoices';
        this.loading = false;
      }
    });
  }

  loadConventions(): void {
    this.conventionService.getAllConventions().subscribe({
      next: (response) => {
        if (response.success) {
          this.conventions = response.data;
        }
      },
      error: (error) => {
        console.error('Error loading conventions:', error);
      }
    });
  }

  loadStructures(): void {
    // Optional: Load structures for reference or filtering
    this.nomenclatureService.getStructures().subscribe({
      next: (structures) => {
        this.structures = structures;
      },
      error: (error) => {
        console.error('Error loading structures:', error);
      }
    });
  }

  searchFactures(): void {
    if (!this.searchTerm.trim()) {
      this.filteredFactures = this.filterByStatut(this.factures);
      return;
    }

    const term = this.searchTerm.toLowerCase();
    this.filteredFactures = this.filterByStatut(this.factures).filter(facture =>
      facture.numeroFacture.toLowerCase().includes(term) ||
      facture.convention?.reference?.toLowerCase().includes(term) ||
      facture.convention?.libelle?.toLowerCase().includes(term) ||
      (facture.referencePaiement?.toLowerCase().includes(term))
    );
  }

  filterByStatut(factures: Facture[]): Facture[] {
    if (!this.filterStatut) {
      return factures;
    }
    return factures.filter(facture => facture.statutPaiement === this.filterStatut);
  }

  applyFilters(): void {
    this.filteredFactures = this.filterByStatut(this.factures);
    if (this.searchTerm.trim()) {
      this.searchFactures();
    }
  }

  openInvoiceModal(): void {
    this.invoiceForm = {
      conventionId: 0,
      dateFacturation: new Date().toISOString().split('T')[0],
      dateEcheance: '',
      montantHT: 0,
      tva: 19,
      notes: ''
    };
    this.showInvoiceModal = true;
    this.errorMessage = '';
  }

  openPaymentModal(facture: Facture): void {
    this.selectedFacture = facture;
    this.paymentForm = {
      factureId: facture.id,
      referencePaiement: '',
      datePaiement: new Date().toISOString().split('T')[0]
    };
    this.showPaymentModal = true;
    this.errorMessage = '';
  }

  closeModal(): void {
    this.showInvoiceModal = false;
    this.showPaymentModal = false;
    this.selectedFacture = null;
    this.errorMessage = '';
  }

  generateInvoice(): void {
    if (!this.validateInvoiceForm()) {
      return;
    }

    this.isGenerating = true;
    this.factureService.generateFacture(this.invoiceForm).subscribe({
      next: (response) => {
        if (response.success) {
          this.successMessage = 'Facture générée avec succès';
          this.loadFactures();
          this.closeModal();
        }
        this.isGenerating = false;
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Failed to generate invoice';
        this.isGenerating = false;
      }
    });
  }

  registerPayment(): void {
    

    this.loading = true;
    this.factureService.registerPayment(this.paymentForm).subscribe({
      next: (response) => {
        if (response.success) {
          this.successMessage = 'Paiement enregistré avec succès';
          this.loadFactures();
          this.closeModal();
        }
        this.loading = false;
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Failed to register payment';
        this.loading = false;
      }
    });
  }

  validateInvoiceForm(): boolean {
    if (!this.invoiceForm.conventionId) {
      this.errorMessage = 'Convention est requise';
      return false;
    }
    if (!this.invoiceForm.dateEcheance) {
      this.errorMessage = 'Date d\'échéance est requise';
      return false;
    }
    if (!this.invoiceForm.montantHT || this.invoiceForm.montantHT <= 0) {
      this.errorMessage = 'Montant HT doit être supérieur à 0';
      return false;
    }
    return true;
  }

  

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  getStatutClass(statut: string): string {
    switch (statut) {
      case 'PAYE': return 'bg-green-100 text-green-800';
      case 'NON_PAYE': return 'bg-yellow-100 text-yellow-800';
      case 'EN_RETARD': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  getStatutLabel(statut: string): string {
    switch (statut) {
      case 'PAYE': return 'Payé';
      case 'NON_PAYE': return 'Non Payé';
      case 'EN_RETARD': return 'En Retard';
      default: return statut;
    }
  }

  isOverdue(facture: Facture): boolean {
    if (facture.statutPaiement === 'PAYE') return false;
    const today = new Date();
    const echeance = new Date(facture.dateEcheance);
    return echeance < today;
  }

  calculateMontantTTC(montantHT: number, tva: number): number {
    return montantHT * (1 + tva / 100);
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  isCommercial(): boolean {
    return this.authService.hasRole('ROLE_COMMERCIAL_METIER');
  }

  canGenerate(): boolean {
    return this.isAdmin() || this.isCommercial();
  }

  canRegisterPayment(): boolean {
    return this.isAdmin() || this.isCommercial();
  }


  // Ajoutez ces propriétés calculées
get facturesPayeesCount(): number {
  return this.factures.filter(f => f.statutPaiement === 'PAYE').length;
}

get facturesNonPayeesCount(): number {
  return this.factures.filter(f => f.statutPaiement === 'NON_PAYE').length;
}

get facturesEnRetardCount(): number {
  return this.factures.filter(f => this.isOverdue(f)).length;
}

}