import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ConventionService, Convention, RenewalRequest } from '../../services/convention.service';
import { FactureService, Facture } from '../../services/facture.service';
import { AuthService } from '../../services/auth.service';
import { TimeFormatService } from '../../services/time-format.service';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HistoryService, HistoryEntry } from '../../services/history.service';
import { Location } from '@angular/common';
import { NomenclatureService } from 'src/app/services/nomenclature.service';

@Component({
  selector: 'app-convention-detail',
  templateUrl: './convention-detail.component.html',
  styleUrls: ['./convention-detail.component.css'],
  standalone: false
})
export class ConventionDetailComponent implements OnInit {
  conventionId!: number;
  convention: Convention | null = null;
  factures: Facture[] = [];
  loading = true;
  errorMessage = '';
  successMessage = '';

  structures: any[] = [];
  loadingStructures = false;
  
  // For payment modal
  showPaymentModal = false;
  selectedFacture: Facture | null = null;
  paymentData = {
    factureId: 0,
    referencePaiement: '',
    datePaiement: new Date().toISOString().split('T')[0]
  };
  
  // For invoice generation
  showGenerateInvoiceModal = false;
  newInvoiceData = {
    conventionId: 0,
    dateFacturation: new Date().toISOString().split('T')[0],
    dateEcheance: '',
    montantHT: 0,
    tva: 19,
    notes: ''
  };
  
  // For editing invoice
  showEditInvoiceModal = false;
  editingFacture: Facture | null = null;
  editInvoiceData = {
    dateFacturation: '',
    dateEcheance: '',
    montantHT: 0,
    tva: 19,
    notes: ''
  };
  
  // For archive modal
  showArchiveModal = false;
  archiveReason = '';
  
  // For restore modal
  showRestoreModal = false;
  
  // Stats
  stats = {
    totalFactures: 0,
    facturesPayees: 0,
    facturesNonPayees: 0,
    facturesEnRetard: 0,
    totalMontantPaye: 0,
    totalMontantNonPaye: 0
  };

  conventionHistory: HistoryEntry[] = [];
  loadingHistory = false;
  showAllHistoryModal = false;
  groupedConventionHistory: { date: string, entries: HistoryEntry[] }[] = [];



  showRenewModal = false;
  renewLoading = false;
  renewalFormData: RenewalRequest = {
    referenceERP: '',
    libelle: '',
    dateDebut: '',
    dateFin: '',
    dateSignature: '',
    montantHT: 0,
    tva: 19,
    montantTTC: 0,
    nbUsers: 0,
    periodicite: 'MENSUEL',
    structureResponsableId: 0 
  };


  showPreviousVersionsModal = false;
  previousVersions: any[] = [];
  selectedOldVersion: any = null;
  selectedOldFactures: any[] = [];
  loadingVersions = false;





  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private conventionService: ConventionService,
    private factureService: FactureService,
    private authService: AuthService,
    public timeFormatService: TimeFormatService,
    public historyService: HistoryService,
    private location: Location,
    private nomenclatureService: NomenclatureService
  
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.conventionId = +params['id'];
      this.loadConventionDetails();
    });

  }




loadConventionHistory(): void {
  if (!this.convention) return;
  
  this.loadingHistory = true;
  this.historyService.getHistoryByEntity('CONVENTION', this.conventionId).subscribe({
    next: (response) => {
      if (response.success) {
        this.conventionHistory = response.data;
        this.groupConventionHistory();
      }
      this.loadingHistory = false;
    },
    error: (error) => {
      console.error('Error loading convention history:', error);
      this.loadingHistory = false;
    }
  });
}

groupConventionHistory(): void {
  const groups: { [key: string]: HistoryEntry[] } = {};
  
  this.conventionHistory.forEach(entry => {
    if (!groups[entry.dateFormatted]) {
      groups[entry.dateFormatted] = [];
    }
    groups[entry.dateFormatted].push(entry);
  });
  
  this.groupedConventionHistory = Object.keys(groups)
    .sort((a, b) => {
      // Parse dates for sorting (DD/MM/YYYY format)
      const [aDay, aMonth, aYear] = a.split('/');
      const [bDay, bMonth, bYear] = b.split('/');
      const dateA = new Date(`${aYear}-${aMonth}-${aDay}`);
      const dateB = new Date(`${bYear}-${bMonth}-${bDay}`);
      return dateB.getTime() - dateA.getTime(); // Descending (newest first)
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
    'referenceConvention': 'Référence',
    'libelle': 'Libellé',
    'dateDebut': 'Date début',
    'dateFin': 'Date fin',
    'montantHT': 'Montant HT',
    'montantTTC': 'Montant TTC',
    'tva': 'TVA',
    'nbUsers': 'Utilisateurs',
    'etat': 'Statut',
    'periodicite': 'Périodicité'
  };
  return fieldMap[field] || field;
}

getConventionHistoryActionClass(actionType: string): string {
  switch (actionType) {
    case 'CREATE': return 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300';
    case 'UPDATE': return 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300';
    case 'DELETE': return 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300';
    case 'ARCHIVE': return 'bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-300';
    case 'RESTORE': return 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300';
    case 'STATUS_CHANGE': return 'bg-orange-100 text-orange-800 dark:bg-orange-900/30 dark:text-orange-300';
    case 'FINANCIAL_UPDATE': return 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-300';
    case 'RENEW': return 'bg-cyan-100 text-cyan-800 dark:bg-cyan-900/30 dark:text-cyan-300';
case 'REASSIGN_CHEF': return 'bg-indigo-100 text-indigo-800 dark:bg-indigo-900/30 dark:text-indigo-300';
case 'REQUEST_PROCESSED': return 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300';
    default: return 'bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-300';
  }
}

openAllHistoryModal(): void {
  this.showAllHistoryModal = true;
}

closeAllHistoryModal(): void {
  this.showAllHistoryModal = false;
}  



getHistoryIcon(actionType: string): string {
  return this.historyService.getActionIcon(actionType);
}




  loadConventionDetails(): void {
    this.loading = true;
    this.conventionService.getConvention(this.conventionId).subscribe({
      next: (response) => {
        if (response.success) {
          this.convention = response.data;
          this.loadFactures();
          this.loadConventionHistory();
          
          // Set initial data for modals
          this.newInvoiceData.conventionId = this.conventionId;
          if (this.convention?.dateFin) {
            // Set default due date to end date or +30 days from now
            const dueDate = new Date(this.convention.dateFin);
            if (dueDate > new Date()) {
              this.newInvoiceData.dateEcheance = this.convention.dateFin;
            } else {
              const thirtyDays = new Date();
              thirtyDays.setDate(thirtyDays.getDate() + 30);
              this.newInvoiceData.dateEcheance = thirtyDays.toISOString().split('T')[0];
            }
          } else {
            const thirtyDays = new Date();
            thirtyDays.setDate(thirtyDays.getDate() + 30);
            this.newInvoiceData.dateEcheance = thirtyDays.toISOString().split('T')[0];
          }
        } else {
          this.errorMessage = 'Convention non trouvée';
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

  loadFactures(): void {
    this.factureService.getFacturesByConvention(this.conventionId).subscribe({
      next: (response) => {
        if (response.success) {
          this.factures = response.data;
          this.calculateStats();
        }
      },
      error: (error) => {
        console.error('Error loading invoices:', error);
      }
    });
  }

  calculateStats(): void {
    this.stats.totalFactures = this.factures.length;
    this.stats.facturesPayees = this.factures.filter(f => f.statutPaiement === 'PAYE').length;
    this.stats.facturesNonPayees = this.factures.filter(f => f.statutPaiement === 'NON_PAYE' || f.statutPaiement === 'EN_RETARD').length;
    this.stats.facturesEnRetard = this.factures.filter(f => f.enRetard).length;
    
    this.stats.totalMontantPaye = this.factures
      .filter(f => f.statutPaiement === 'PAYE')
      .reduce((sum, f) => sum + (f.montantTTC || 0), 0);
    
    this.stats.totalMontantNonPaye = this.factures
      .filter(f => f.statutPaiement !== 'PAYE')
      .reduce((sum, f) => sum + (f.montantTTC || 0), 0);
  }

  // Navigation
  goBack(): void {
    this.location.back();
  }

  editConvention(): void {
    // You can navigate to edit page or open modal
    // For now, go back to conventions page and open edit modal there
    this.router.navigate(['/conventions'], { 
      queryParams: { edit: this.conventionId } 
    });
  }

  // Payment methods
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
    this.paymentData = {
      factureId: 0,
      referencePaiement: '',
      datePaiement: new Date().toISOString().split('T')[0]
    };
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
          this.loadFactures(); // Reload invoices to update status
          this.closePaymentModal();
          
          // Also reload convention to update status
          this.loadConventionDetails();
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

  // Generate invoice methods
  openGenerateInvoiceModal(): void {
    // Set default values
    this.newInvoiceData = {
      conventionId: this.conventionId,
      dateFacturation: new Date().toISOString().split('T')[0],
      dateEcheance: '',
      montantHT: this.convention?.montantHT || 0,
      tva: this.convention?.tva || 19,
      notes: ''
    };
    
    // Set due date based on periodicity
    if (this.convention?.periodicite) {
      const today = new Date();
      switch (this.convention.periodicite) {
        case 'MENSUEL':
          today.setMonth(today.getMonth() + 1);
          break;
        case 'TRIMESTRIEL':
          today.setMonth(today.getMonth() + 3);
          break;
        case 'SEMESTRIEL':
          today.setMonth(today.getMonth() + 6);
          break;
        case 'ANNUEL':
          today.setFullYear(today.getFullYear() + 1);
          break;
      }
      this.newInvoiceData.dateEcheance = today.toISOString().split('T')[0];
    }
    
    this.showGenerateInvoiceModal = true;
  }

  closeGenerateInvoiceModal(): void {
    this.showGenerateInvoiceModal = false;
  }

  generateInvoice(): void {
    if (!this.newInvoiceData.dateEcheance) {
      this.errorMessage = 'La date d\'échéance est requise';
      return;
    }
    
    if (this.newInvoiceData.montantHT <= 0) {
      this.errorMessage = 'Le montant HT doit être supérieur à 0';
      return;
    }

    this.loading = true;
    this.factureService.generateFacture(this.newInvoiceData).subscribe({
      next: (response) => {
        if (response.success) {
          this.successMessage = 'Facture générée avec succès';
          this.loadFactures(); // Reload invoices
          this.closeGenerateInvoiceModal();
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error generating invoice:', error);
        this.errorMessage = error.error?.message || 'Erreur lors de la génération de la facture';
        this.loading = false;
      }
    });
  }

  // Edit invoice methods
  openEditInvoiceModal(facture: Facture): void {
    this.editingFacture = facture;
    this.editInvoiceData = {
      dateFacturation: facture.dateFacturation,
      dateEcheance: facture.dateEcheance,
      montantHT: facture.montantHT || 0,
      tva: facture.tva || 19,
      notes: facture.notes || ''
    };
    this.showEditInvoiceModal = true;
  }

  closeEditInvoiceModal(): void {
    this.showEditInvoiceModal = false;
    this.editingFacture = null;
  }

  updateInvoice(): void {
    if (!this.editingFacture) return;
    
    this.loading = true;
    // You'll need to add this method to your FactureService
    this.factureService.updateFacture(this.editingFacture.id, this.editInvoiceData).subscribe({
      next: (response) => {
        if (response.success) {
          this.successMessage = 'Facture mise à jour avec succès';
          this.loadFactures(); // Reload invoices
          this.closeEditInvoiceModal();
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error updating invoice:', error);
        this.errorMessage = error.error?.message || 'Erreur lors de la mise à jour de la facture';
        this.loading = false;
      }
    });
  }

  // Delete invoice
  deleteInvoice(factureId: number): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer cette facture ?')) {
      this.loading = true;
      this.factureService.deleteFacture(factureId).subscribe({
        next: (response) => {
          if (response.success) {
            this.successMessage = 'Facture supprimée avec succès';
            this.loadFactures(); // Reload invoices
          }
          this.loading = false;
        },
        error: (error) => {
          console.error('Error deleting invoice:', error);
          this.errorMessage = error.error?.message || 'Erreur lors de la suppression de la facture';
          this.loading = false;
        }
      });
    }
  }

  // Archive methods
  openArchiveModal(): void {
    this.archiveReason = '';
    this.showArchiveModal = true;
  }

  closeArchiveModal(): void {
    this.showArchiveModal = false;
    this.archiveReason = '';
  }

  archiveConvention(): void {
    if (!this.archiveReason.trim()) {
      this.errorMessage = 'Veuillez fournir une raison pour l\'archivage';
      return;
    }

    this.loading = true;
    this.conventionService.archiveConvention(this.conventionId, this.archiveReason).subscribe({
      next: (response) => {
        if (response.success) {
          this.successMessage = 'Convention archivée avec succès';
          this.loadConventionDetails(); // Reload to show updated status
          this.closeArchiveModal();
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error archiving convention:', error);
        this.errorMessage = error.error?.message || 'Erreur lors de l\'archivage de la convention';
        this.loading = false;
      }
    });
  }

  // Restore methods
  openRestoreModal(): void {
    this.showRestoreModal = true;
  }

  closeRestoreModal(): void {
    this.showRestoreModal = false;
  }

  restoreConvention(): void {
    this.loading = true;
    this.conventionService.restoreConvention(this.conventionId).subscribe({
      next: (response) => {
        if (response.success) {
          this.successMessage = 'Convention restaurée avec succès';
          this.loadConventionDetails(); // Reload to show updated status
          this.closeRestoreModal();
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error restoring convention:', error);
        this.errorMessage = error.error?.message || 'Erreur lors de la restauration de la convention';
        this.loading = false;
      }
    });
  }

  // Utility methods
  getEtatClass(etat: string | null): string {
    if (etat === null) return 'bg-gray-100 text-gray-800';
    
    switch (etat) {
      case 'PLANIFIE': return 'bg-yellow-100 text-yellow-800';
      case 'EN COURS': return 'bg-blue-100 text-blue-800';
      case 'TERMINE': return 'bg-green-100 text-green-800';
      case 'ARCHIVE': return 'bg-gray-100 text-gray-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  getEtatLabel(etat: string | null): string {
    if (etat === null) return '-';
    
    switch (etat) {
      case 'PLANIFIE': return 'Planifiée';
      case 'EN COURS': return 'En Cours';
      case 'TERMINE': return 'Terminée';
      case 'ARCHIVE': return 'Archivée';
      default: return etat;
    }
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

  isCommercial(): boolean {
    return this.authService.isCommercial();
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }




  calculateDuration(): number {
  if (!this.convention?.dateDebut || !this.convention?.dateFin) {
    return 0;
  }
  const start = new Date(this.convention.dateDebut);
  const end = new Date(this.convention.dateFin);
  const diffTime = Math.abs(end.getTime() - start.getTime());
  return Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
}

calculateElapsedDays(): number {
  if (!this.convention?.dateDebut) {
    return 0;
  }
  const start = new Date(this.convention.dateDebut);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  
  if (today < start) {
    return 0;
  }
  
  if (this.convention.dateFin) {
    const end = new Date(this.convention.dateFin);
    if (today > end) {
      return this.calculateDuration();
    }
  }
  
  const diffTime = Math.abs(today.getTime() - start.getTime());
  return Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
}

calculateRemainingDays(): number {
  if (!this.convention?.dateFin) {
    return 0;
  }
  const end = new Date(this.convention.dateFin);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  
  if (today > end) {
    return -1; // Negative means overdue/expired
  }
  
  const diffTime = Math.abs(end.getTime() - today.getTime());
  return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
}

calculateProgress(): number {
  const totalDays = this.calculateDuration();
  if (totalDays === 0) return 0;
  
  const elapsedDays = this.calculateElapsedDays();
  const progress = Math.round((elapsedDays * 100) / totalDays);
  return Math.min(progress, 100); // Cap at 100%
}

getRemainingDaysText(): string {
  const days = this.calculateRemainingDays();
  if (days < 0) return 'Expirée';
  if (days === 0) return 'Aujourd\'hui';
  if (days === 1) return '1 jour';
  return `${days} jours`;
}

openEditModal(convention: Convention): void {
  this.router.navigate(['/conventions/edit', convention.id]);
}


viewFactureDetails(id: number): void {
  this.router.navigate(['/factures', id]);
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


// Add these methods to your ConventionDetailComponent class

// For the optional chain warning - this is just a warning, not an error
// But to fix it, you can use convention.referenceConvention directly when convention is not null

// Add formatChangeValue method
formatChangeValue(value: any): string {
  if (value === null || value === undefined) return '-';
  if (typeof value === 'object') return JSON.stringify(value);
  if (typeof value === 'number') return value.toString();
  if (value instanceof Date) return value.toLocaleDateString();
  return value.toString();
}


getAllHistoryEntries(): HistoryEntry[] {
  if (!this.groupedConventionHistory) return [];
  return this.groupedConventionHistory.reduce((acc, group) => {
    return [...acc, ...group.entries];
  }, [] as HistoryEntry[]);
}

// Navigate to all invoices page
viewAllInvoices(): void {
  this.router.navigate(['/conventions', this.conventionId, 'factures']);
}

// Navigate to all history page
viewAllHistory(): void {
  this.router.navigate(['/conventions', this.conventionId, 'history']);
}


openRenewModal(): void {
  // Pre-fill with current convention data
    this.loadResponsableStructures();

  if (this.convention) {
    this.renewalFormData = {
      referenceERP: this.convention.referenceERP || '',
      libelle: this.convention.libelle || '',
      dateDebut: this.convention.dateDebut || '',
      dateFin: this.convention.dateFin || '',
      dateSignature: this.convention.dateSignature || '',
      montantHT: this.convention.montantHT || 0,
      tva: this.convention.tva || 19,
      montantTTC: this.convention.montantTTC || 0,
      nbUsers: this.convention.nbUsers || 0,
      periodicite: this.convention.periodicite || 'MENSUEL',
      structureResponsableId: this.convention.structureResponsableId || 0

    };
  }
  this.showRenewModal = true;
}

closeRenewModal(): void {
  this.showRenewModal = false;
}

// In convention-detail.component.ts
renewConvention(): void {
  // Validate form data
  if (!this.renewalFormData.libelle?.trim()) {
    this.errorMessage = 'Le libellé est requis';
    return;
  }
  
  if (!this.renewalFormData.dateDebut) {
    this.errorMessage = 'La date de début est requise';
    return;
  }
  
  if (!this.renewalFormData.dateFin) {
    this.errorMessage = 'La date de fin est requise';
    return;
  }
  
  if (this.renewalFormData.montantHT <= 0) {
    this.errorMessage = 'Le montant HT doit être supérieur à 0';
    return;
  }
  
  if (this.renewalFormData.nbUsers <= 0) {
    this.errorMessage = 'Le nombre d\'utilisateurs doit être supérieur à 0';
    return;
  }

    if (!this.renewalFormData.structureResponsableId || this.renewalFormData.structureResponsableId === 0) {
    this.errorMessage = 'Veuillez sélectionner une structure responsable';
    return;
  }

  this.renewLoading = true;
  console.log('Sending renewal data:', this.renewalFormData);
  
  this.conventionService.renewConvention(this.conventionId, this.renewalFormData).subscribe({
    next: (response) => {
      if (response.success) {
        this.successMessage = 'Convention renouvelée avec succès';
        this.closeRenewModal();
       this.loadConventionDetails();     // Reload convention data
this.loadFactures();               // Reload invoices
this.loadConventionHistory();       // Reload history
      } else {
        this.errorMessage = response.message || 'Erreur lors du renouvellement';
        this.renewLoading = false;
      }
    },
    error: (error) => {
      console.error('Error renewing convention:', error);
      this.errorMessage = error.error?.message || 'Erreur lors du renouvellement';
      this.renewLoading = false;
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
  if (this.renewalFormData.montantHT <= 0) {
    this.renewalFormData.montantTTC = 0;
    return;
  }

  const tva = this.renewalFormData.tva || 19;
  const tvaAmount = this.renewalFormData.montantHT * tva / 100;
  this.renewalFormData.montantTTC = this.renewalFormData.montantHT + tvaAmount;
}




viewPreviousVersions(): void {
  if (!this.convention?.id) return;
  
  this.loadingVersions = true;
  this.conventionService.getPreviousVersions(this.convention.id).subscribe({
    next: (response) => {
      console.log('Previous versions response:', response); // Add logging
      if (response.success && response.data) {
        this.previousVersions = response.data;
        this.showPreviousVersionsModal = true;
        
        if (this.previousVersions.length === 0) {
          this.errorMessage = 'Aucune ancienne version trouvée';
        }
      } else {
        this.errorMessage = response.message || 'Aucune ancienne version trouvée';
      }
      this.loadingVersions = false;
    },
    error: (error) => {
      console.error('Error loading previous versions:', error);
      this.errorMessage = 'Erreur lors du chargement des anciennes versions';
      this.loadingVersions = false;
    }
  });
}

closePreviousVersionsModal(): void {
  this.showPreviousVersionsModal = false;
  this.previousVersions = [];
  this.selectedOldVersion = null;
  this.selectedOldFactures = [];
}

selectOldVersion(version: any): void {
  this.selectedOldVersion = version.oldConvention;
  this.selectedOldFactures = version.oldFactures || [];
}

formatDate(dateString: string): string {
  if (!dateString) return '-';
  const date = new Date(dateString);
  const day = date.getDate().toString().padStart(2, '0');
  const month = (date.getMonth() + 1).toString().padStart(2, '0');
  const year = date.getFullYear();
  return `${day}/${month}/${year}`;
}

// Format date with time
formatDateTime(dateString: string): string {
  if (!dateString) return '-';
  const date = new Date(dateString);
  return date.toLocaleDateString('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
}

loadResponsableStructures(): void {
  console.log('Loading responsable structures...');
  this.loadingStructures = true;

  this.nomenclatureService.getResponsableStructures().subscribe({
    next: (data) => {
      console.log('Structures loaded:', data);
      this.structures = data.map((structure: any) => ({
        ...structure,
        zoneName: structure.zoneGeographique?.name || 'Sans zone'
      }));
      this.loadingStructures = false;
    },
    error: (error) => {
      console.error('Error loading structures:', error);
      this.loadingStructures = false;
    }
  });
}

}