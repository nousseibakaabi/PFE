import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FactureService, Facture } from '../../services/facture.service';
import { AuthService } from '../../services/auth.service';
import { TimeFormatService } from '../../services/time-format.service';
import { Location } from '@angular/common';
import { NomenclatureService, Structure } from 'src/app/services/nomenclature.service';
import { ConventionService, Convention } from '../../services/convention.service';
import { HistoryService, HistoryEntry } from '../../services/history.service';


@Component({
  selector: 'app-facture-detail',
  templateUrl: './facture-detail.component.html',
  styleUrls: ['./facture-detail.component.css'],
  standalone: false
})
export class FactureDetailComponent implements OnInit {
  factureId!: number;
  facture: Facture | null = null;
  loading = true;
  errorMessage = '';
  successMessage = '';
  factureHistory: HistoryEntry[] = [];
  loadingHistory = false;
  groupedFactureHistory: { date: string, entries: HistoryEntry[] }[] = [];
  
  // For payment modal
  showPaymentModal = false;
  paymentData = {
    factureId: 0,
    referencePaiement: '',
    datePaiement: new Date().toISOString().split('T')[0]
  };


  conventionDetails: Convention | null = null;


  // For edit modal
  showEditModal = false;
  editData = {
    dateFacturation: '',
    dateEcheance: '',
    montantHT: 0,
    tva: 19,
    notes: ''
  };


  
  // Store structure details
  structureResponsable: Structure | null = null;
  structureBeneficiel: Structure | null = null;
  
  // Store zone names
  zoneResponsableName: string = '';
  zoneBeneficielName: string = '';
 
  Object = Object;


  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private location: Location,
    private factureService: FactureService,
    private nomenclatureService: NomenclatureService,
    private authService: AuthService,
    public timeFormatService: TimeFormatService,
    private conventionService: ConventionService,
    public historyService: HistoryService

  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.factureId = +params['id'];
      this.loadFacture();
    });

    this.loadFactureHistory();
  }




getInvoiceHistoryActionClass(actionType: string): string {
  switch (actionType) {
    case 'CREATE': return 'bg-green-100 text-green-800';
    case 'UPDATE': return 'bg-blue-100 text-blue-800';
    case 'DELETE': return 'bg-red-100 text-red-800';
    case 'PAYMENT': return 'bg-emerald-100 text-emerald-800';
    case 'STATUS_CHANGE': return 'bg-orange-100 text-orange-800';
    case 'OVERDUE': return 'bg-red-100 text-red-800';
    default: return 'bg-gray-100 text-gray-800';
  }
}

getHistoryIcon(actionType: string): string {
  return this.historyService.getActionIcon(actionType);
}

formatChangeValue(value: any): string {
  if (value === null || value === undefined) return '-';
  if (typeof value === 'object') return JSON.stringify(value);
  if (typeof value === 'number') return value.toString();
  if (value instanceof Date) return value.toLocaleDateString();
  return value.toString();
}



loadFactureHistory(): void {
  if (!this.facture) return;
  
  this.loadingHistory = true;
  this.historyService.getHistoryByEntity('FACTURE', this.factureId).subscribe({
    next: (response) => {
      if (response.success) {
        this.factureHistory = response.data;
        this.groupFactureHistory();
      }
      this.loadingHistory = false;
    },
    error: (error) => {
      console.error('Error loading facture history:', error);
      this.loadingHistory = false;
    }
  });
}

groupFactureHistory(): void {
  const groups: { [key: string]: HistoryEntry[] } = {};
  
  this.factureHistory.forEach(entry => {
    if (!groups[entry.dateFormatted]) {
      groups[entry.dateFormatted] = [];
    }
    groups[entry.dateFormatted].push(entry);
  });
  
  this.groupedFactureHistory = Object.keys(groups)
    .sort((a, b) => {
      const [aDay, aMonth, aYear] = a.split('/');
      const [bDay, bMonth, bYear] = b.split('/');
      const dateA = new Date(`${aYear}-${aMonth}-${aDay}`);
      const dateB = new Date(`${bYear}-${bMonth}-${bDay}`);
      return dateB.getTime() - dateA.getTime();
    })
    .map(date => ({
      date,
      entries: groups[date].sort((a, b) => 
        new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
      )
    }));
}

getChangedFieldsArray(entry: HistoryEntry): { field: string, old: any, new: any }[] {
  const changes = this.historyService.getChangedFields(entry.oldValues, entry.newValues);
  return Object.keys(changes).map(key => ({
    field: this.formatFieldName(key),
    old: changes[key].old,
    new: changes[key].new
  }));
}

formatFieldName(field: string): string {
  const fieldMap: { [key: string]: string } = {
    'numeroFacture': 'Numéro',
    'dateFacturation': 'Date facturation',
    'dateEcheance': 'Date échéance',
    'montantHT': 'Montant HT',
    'montantTTC': 'Montant TTC',
    'tva': 'TVA',
    'statutPaiement': 'Statut',
    'referencePaiement': 'Référence paiement',
    'notes': 'Notes'
  };
  return fieldMap[field] || field;
}

getFactureHistoryActionClass(actionType: string): string {
  switch (actionType) {
    case 'CREATE': return 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300';
    case 'UPDATE': return 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300';
    case 'DELETE': return 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300';
    case 'PAYMENT': return 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-300';
    case 'STATUS_CHANGE': return 'bg-orange-100 text-orange-800 dark:bg-orange-900/30 dark:text-orange-300';
    case 'OVERDUE': return 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300';
    default: return 'bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-300';
  }
}

  loadFacture(): void {
  this.loading = true;
  this.factureService.getFacture(this.factureId).subscribe({
    next: (response) => {
      if (response.success) {
        this.facture = response.data;
        console.log('Facture loaded:', this.facture);
        
        // After loading facture, load the structure details
        this.loadStructureDetails();
        
        // Load convention details if conventionId exists
        if (this.facture?.conventionId) {
          this.loadConventionDetails(this.facture.conventionId);
        }

        this.loadFactureHistory(); // Add this line

      } else {
        this.errorMessage = 'Facture non trouvée';
      }
      this.loading = false;
    },
    error: (error) => {
      console.error('Error loading facture:', error);
      this.errorMessage = 'Erreur lors du chargement de la facture';
      this.loading = false;
    }
  });
}

loadConventionDetails(conventionId: number): void {
  this.conventionService.getConvention(conventionId).subscribe({
    next: (response) => {
      if (response.success) {
        this.conventionDetails = response.data;
        console.log('Convention details loaded:', this.conventionDetails);
      }
    },
    error: (error) => {
      console.error('Error loading convention details:', error);
    }
  });
}



  loadStructureDetails(): void {
    if (!this.facture) return;
    
      this.nomenclatureService.getStructures().subscribe({
      next: (structures) => {
        // Find structures by name
        if (this.facture?.structureResponsableName) {
          this.structureResponsable = structures.find(s => 
            s.name === this.facture?.structureResponsableName
          ) || null;
          this.zoneResponsableName = this.structureResponsable?.zoneGeographique?.name || '';
        }
        
        if (this.facture?.structureBeneficielName) {
          this.structureBeneficiel = structures.find(s => 
            s.name === this.facture?.structureBeneficielName
          ) || null;
          this.zoneBeneficielName = this.structureBeneficiel?.zoneGeographique?.name || '';
        }
      },
      error: (error) => {
        console.error('Error loading structures:', error);
      }
    });
  }


  // Add to your component class
getConventionEtatClass(etat: string | null): string {
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

getConventionEtatLabel(etat: string | null): string {
  if (etat === null) return '-';
  
  switch (etat) {
    case 'EN_ATTENTE': return 'En Attente';
    case 'EN_COURS': return 'En Cours';
    case 'EN_RETARD': return 'En Retard';
    case 'TERMINE': return 'Terminé';
    case 'ARCHIVE': return 'Archivé';
    default: return etat;
  }
}

viewConventionDetails(conventionId: number | undefined | null): void {
  if (conventionId) {
    this.router.navigate(['/conventions', conventionId]);
  }
}

  goBack(): void {
    this.location.back();
  }

  // Payment methods
  openPaymentModal(): void {
    if (!this.facture) return;
    
    this.paymentData = {
      factureId: this.facture.id,
      referencePaiement: '',
      datePaiement: new Date().toISOString().split('T')[0]
    };
    this.showPaymentModal = true;
  }

  closePaymentModal(): void {
    this.showPaymentModal = false;
  }

  registerPayment(): void {
    if (!this.paymentData.referencePaiement.trim()) {
      this.errorMessage = 'La référence de paiement est requise';
      return;
    }

    this.loading = true;
    this.factureService.registerPayment(this.paymentData).subscribe({
      next: (response) => {
        if (response.success) {
          this.successMessage = 'Paiement enregistré avec succès';
          this.loadFacture(); // Reload to get updated status
          this.closePaymentModal();
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error registering payment:', error);
        this.errorMessage = error.error?.message || 'Erreur lors de l\'enregistrement du paiement';
        this.loading = false;
      }
    });
  }

  // Edit methods
  openEditModal(): void {
    if (!this.facture) return;
    
    this.editData = {
      dateFacturation: this.facture.dateFacturation,
      dateEcheance: this.facture.dateEcheance,
      montantHT: this.facture.montantHT || 0,
      tva: this.facture.tva || 19,
      notes: this.facture.notes || ''
    };
    this.showEditModal = true;
  }

  closeEditModal(): void {
    this.showEditModal = false;
  }

  updateFacture(): void {
    if (!this.facture) return;

    this.loading = true;
    this.factureService.updateFacture(this.facture.id, this.editData).subscribe({
      next: (response) => {
        if (response.success) {
          this.successMessage = 'Facture mise à jour avec succès';
          this.loadFacture(); // Reload to get updated data
          this.closeEditModal();
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error updating facture:', error);
        this.errorMessage = error.error?.message || 'Erreur lors de la mise à jour';
        this.loading = false;
      }
    });
  }

  // Delete method
  deleteFacture(): void {
    if (!this.facture) return;
    
    if (confirm('Êtes-vous sûr de vouloir supprimer cette facture ?')) {
      this.loading = true;
      this.factureService.deleteFacture(this.facture.id).subscribe({
        next: (response) => {
          if (response.success) {
            this.successMessage = 'Facture supprimée avec succès';
            setTimeout(() => {
              this.router.navigate(['/factures']);
            }, 1500);
          }
          this.loading = false;
        },
        error: (error) => {
          console.error('Error deleting facture:', error);
          this.errorMessage = error.error?.message || 'Erreur lors de la suppression';
          this.loading = false;
        }
      });
    }
  }

  // Print invoice
  printInvoice(): void {
    window.print();
  }

  // Utility methods
  formatMontant(montant: number): string {
    return new Intl.NumberFormat('fr-TN', { 
      minimumFractionDigits: 2, 
      maximumFractionDigits: 2 
    }).format(montant || 0);
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
      case 'PAYE': return 'Payée';
      case 'NON_PAYE': return 'Non Payée';
      case 'EN_RETARD': return 'En Retard';
      default: return statut;
    }
  }

  isOverdue(): boolean {
    if (!this.facture) return false;
    if (this.facture.statutPaiement === 'PAYE') return false;
    const today = new Date();
    const echeance = new Date(this.facture.dateEcheance);
    return echeance < today;
  }

  canEdit(): boolean {
    return this.authService.isAdmin() || this.authService.isCommercial();
  }

  canRegisterPayment(): boolean {
    return this.facture?.statutPaiement !== 'PAYE' && (this.authService.isAdmin() || this.authService.isCommercial());
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  calculateTTC(): number {
    if (!this.editData.montantHT) return 0;
    const tva = this.editData.tva || 19;
    return this.editData.montantHT * (1 + tva / 100);
  }


  // Add to component class
objectKeys(obj: any): string[] {
  return obj ? Object.keys(obj) : [];
}

groupHistoryByDate(history: HistoryEntry[]): { date: string, entries: HistoryEntry[] }[] {
  const groups: { [key: string]: HistoryEntry[] } = {};
  
  history.forEach(entry => {
    if (!groups[entry.dateFormatted]) {
      groups[entry.dateFormatted] = [];
    }
    groups[entry.dateFormatted].push(entry);
  });
  
  return Object.keys(groups)
    .sort((a, b) => b.localeCompare(a)) // Sort descending (newest first)
    .map(date => ({
      date,
      entries: groups[date].sort((a, b) => b.timestamp.localeCompare(a.timestamp))
    }));
}
}