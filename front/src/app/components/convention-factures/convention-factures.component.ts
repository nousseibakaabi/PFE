import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FactureService, Facture } from '../../services/facture.service';
import { ConventionService } from '../../services/convention.service';
import { AuthService } from '../../services/auth.service';
import { Location } from '@angular/common';

@Component({
  selector: 'app-convention-factures',
  templateUrl: './convention-factures.component.html',
  styleUrls: ['./convention-factures.component.css']
})
export class ConventionFacturesComponent implements OnInit {
  conventionId!: number;
  conventionReference: string = '';
  conventionArchived: boolean = false;
  factures: Facture[] = [];
  loading = true;
  currentDate = new Date();

  // Add this property
viewMode: 'card' | 'list' = 'card';

  
  // For payment modal
  showPaymentModal = false;
  selectedFacture: Facture | null = null;
  paymentData = {
    factureId: 0,
    referencePaiement: '',
    datePaiement: new Date().toISOString().split('T')[0]
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private factureService: FactureService,
    private conventionService: ConventionService,
    private authService: AuthService,
    private location: Location,

  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.conventionId = +params['id'];
      this.loadData();
    });
  }

  loadData(): void {
    this.loading = true;
    
    // Get convention info
    this.conventionService.getConvention(this.conventionId).subscribe({
      next: (response) => {
        if (response.success) {
          this.conventionReference = response.data.referenceConvention;
          this.conventionArchived = response.data.archived || false;
        }
      }
    });
    
    // Get all invoices
    this.factureService.getFacturesByConvention(this.conventionId).subscribe({
      next: (response) => {
        if (response.success) {
          this.factures = response.data;
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading invoices:', error);
        this.loading = false;
      }
    });
  }

  goBack(): void {
    this.location.back();
  }

  viewFactureDetails(id: number): void {
    this.router.navigate(['/factures', id]);
  }

  openPaymentModal(facture: Facture): void {
    this.selectedFacture = facture;
    this.paymentData = {
      factureId: facture.id,
      referencePaiement: '',
      datePaiement: new Date().toISOString().split('T')[0]
    };
    this.showPaymentModal = true;
  }

  closePaymentModal(): void {
    this.showPaymentModal = false;
    this.selectedFacture = null;
  }

  registerPayment(): void {
    if (!this.paymentData.referencePaiement.trim()) {
      return;
    }

    this.loading = true;
    this.factureService.registerPayment(this.paymentData).subscribe({
      next: (response) => {
        if (response.success) {
          this.loadData(); // Reload
          this.closePaymentModal();
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error registering payment:', error);
        this.loading = false;
      }
    });
  }

  getInvoiceStatusClass(statut: string): string {
    switch (statut) {
      case 'PAYE': return 'bg-green-100 text-green-800';
      case 'NON_PAYE': return 'bg-red-100 text-red-800';
      case 'EN_RETARD': return 'bg-orange-100 text-orange-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  getInvoiceStatusLabel(statut: string): string {
    switch (statut) {
      case 'PAYE': return 'Payée';
      case 'NON_PAYE': return 'Non Payée';
      case 'EN_RETARD': return 'En Retard';
      default: return statut;
    }
  }

  formatMontant(montant: number): string {
    return new Intl.NumberFormat('fr-TN', { 
      minimumFractionDigits: 2, 
      maximumFractionDigits: 2 
    }).format(montant || 0);
  }

  canEdit(): boolean {
    return this.authService.isAdmin() || this.authService.isCommercial();
  }


calculateTotalTTC(): string {
  const total = this.factures.reduce((sum, f) => sum + (f.montantTTC || 0), 0);
  return this.formatMontant(total);
}

calculatePaidCount(): number {
  return this.factures.filter(f => f.statutPaiement === 'PAYE').length;
}
}