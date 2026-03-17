import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FactureService, Facture } from '../../services/facture.service';
import { AuthService } from '../../services/auth.service';
import { TimeFormatService } from '../../services/time-format.service';
import { Location } from '@angular/common';
import { NomenclatureService, Structure } from 'src/app/services/nomenclature.service';
import { ConventionService, Convention } from '../../services/convention.service';
import { HistoryService, HistoryEntry } from '../../services/history.service';
import { HttpHeaders } from '@angular/common/http'; // Add this import
import jsPDF from 'jspdf';
import html2canvas from 'html2canvas';


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
    case 'PLANIFIE': return 'bg-yellow-100 text-yellow-800';
    case 'EN COURS': return 'bg-blue-100 text-blue-800';
    case 'TERMINE': return 'bg-green-100 text-green-800';
    case 'ARCHIVE': return 'bg-gray-100 text-gray-800';
    default: return 'bg-gray-100 text-gray-800';
  }
}

getConventionEtatLabel(etat: string | null): string {
  if (etat === null) return '-';
  
  switch (etat) {
    case 'PLANIFIE': return 'Planifié';
    case 'EN COURS': return 'En Cours';
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

generatePDF(): void {
  this.loading = true;
  
  // Create a new PDF document
  const pdf = new jsPDF({
    orientation: 'portrait',
    unit: 'mm',
    format: 'a4'
  });

  // Set colors (professional invoice colors)
  const primaryColor = [41, 128, 185]; // Professional blue
  const secondaryColor = [52, 73, 94]; // Dark gray
  const lightGray = [245, 245, 245];
  const borderColor = [220, 220, 220];

  // Set font
  pdf.setFont('helvetica');

  // ===== HEADER with Logo =====
  try {
    // Add logo from assets (adjust path as needed)
    const logoUrl = '/assets/logo.png'; // Your logo path
    const logo = new Image();
    logo.src = logoUrl;
    
    // You'll need to handle this asynchronously
    this.addLogoToPDF(pdf, logoUrl).then(() => {
      this.addInvoiceContent(pdf, primaryColor, secondaryColor, lightGray, borderColor);
    });
  } catch (error) {
    // If logo fails, just add text
    this.addInvoiceContent(pdf, primaryColor, secondaryColor, lightGray, borderColor);
  }
}

private addInvoiceContent(pdf: jsPDF, primaryColor: number[], secondaryColor: number[], 
                          lightGray: number[], borderColor: number[]): void {
  
  let yPos = 36; // Starting Y position

  
  pdf.setFontSize(12);
  pdf.setTextColor(primaryColor[0], primaryColor[1], primaryColor[2]);
  pdf.setFont('helvetica', 'bold');
  pdf.text(`FACTURE N° ${this.facture?.numeroFacture || ''}`, 60, yPos + 4);

 
 

  let Z =42;
  let y=43;
  pdf.setFontSize(10);
  pdf.setTextColor(secondaryColor[0], secondaryColor[1], secondaryColor[2]);
  pdf.setFont('helvetica', 'bold');
  pdf.text('Société', Z, y + 13);

  // Company details (normal size, normal font)
  pdf.setFontSize(8);
  pdf.setTextColor(secondaryColor[0], secondaryColor[1], secondaryColor[2]);
  pdf.setFont('helvetica', 'normal');
  pdf.text('Centre national de l\'informatique', Z, y + 19);
  pdf.text('17, 1005 Av. Belhassen Ben Chaabane ,Tunis', Z, y + 25);
  pdf.text('webcni@cni.tn', Z, y + 31);
  pdf.text('+216 71 781 862', Z, y + 37);

  let W =130;
  pdf.setFontSize(10);
  pdf.setTextColor(secondaryColor[0], secondaryColor[1], secondaryColor[2]);
  pdf.setFont('helvetica', 'bold');
  pdf.text('Facturer à ', W,  y + 13);

  // Company details (normal size, normal font)
  pdf.setFontSize(8);
  pdf.setTextColor(secondaryColor[0], secondaryColor[1], secondaryColor[2]);
  pdf.setFont('helvetica', 'normal');
  pdf.text(this.facture?.structureBeneficielName || 'N/A', W, y + 19);
  pdf.text(this.structureBeneficiel?.zoneGeographique?.name || 'N/A', W, y + 25);
  pdf.text(this.structureBeneficiel?.email || 'N/A', W, y + 31);

  const formattedPhone = this.formatPhoneNumber(this.structureBeneficiel?.phone || 'N/A');
  pdf.text(formattedPhone, W , y + 37);
  
  // ===== DATES SECTION =====
  yPos = 90;

  pdf.setTextColor(secondaryColor[0], secondaryColor[1], secondaryColor[2]);
  pdf.setFontSize(9);
  pdf.setFont('helvetica', 'bold');
  pdf.text('Date facturation :', 129, yPos + 5);
  pdf.text('Date échéance :', 129, yPos + 10);
  pdf.text('Date création :', 129, yPos + 15);

  pdf.setFontSize(9);
  pdf.setFont('helvetica', 'normal');
  pdf.text(this.formatDate(this.facture?.dateFacturation || ''), 156, yPos + 5);
  pdf.text(this.formatDate(this.facture?.dateEcheance || ''), 154, yPos + 10);
  pdf.text(this.formatDate(this.conventionDetails?.createdAt || ''), 153, yPos + 15);

    
  yPos = 115;
  
  // Table headers
  pdf.setFillColor(primaryColor[0], primaryColor[1], primaryColor[2]);
  pdf.rect(20, yPos, 60, 8, 'F');
  pdf.rect(80, yPos, 35, 8, 'F');
  pdf.rect(115, yPos, 30, 8, 'F');
  pdf.rect(145, yPos, 48, 8, 'F');

  pdf.setTextColor(255, 255, 255);
  pdf.setFontSize(9);
  pdf.setFont('helvetica', 'bold');
  pdf.text('Description', 25, yPos + 5);
  pdf.text('Montant HT', 85, yPos + 5);
  pdf.text('TVA', 122, yPos + 5);
  pdf.text('Montant TTC', 155, yPos + 5);

  // Table row
  yPos += 8;
  pdf.setDrawColor(borderColor[0], borderColor[1], borderColor[2]);
  pdf.rect(20, yPos, 173, 20, 'S');

// Description - extract just the first part (FACTURE X/X)
pdf.setTextColor(secondaryColor[0], secondaryColor[1], secondaryColor[2]);
pdf.setFontSize(10);
pdf.setFont('helvetica', 'bold');

// Get full description and extract first 12 characters
const fullDesc = this.facture?.notes || '';
const shortDesc = fullDesc.substring(0, 12); // Takes "FACTURE 1/1" or "FACTURE 1/2"

pdf.text(shortDesc, 25, yPos + 7);

  // Amounts
  pdf.setFontSize(10);
  pdf.setFont('helvetica', 'normal');
  pdf.text(this.formatMontantWithoutCurrency(this.facture?.montantHT || 0), 100, yPos + 7, { align: 'right' });
  pdf.text(`${this.facture?.tva || 0}%`, 130, yPos + 7, { align: 'center' });
  
  pdf.setFont('helvetica', 'bold');
  pdf.setTextColor(primaryColor[0], primaryColor[1], primaryColor[2]);
  pdf.text(this.formatMontant(this.facture?.montantTTC || 0), 185, yPos + 7, { align: 'right' });

  // ===== TOTALS =====
  yPos += 23;
  
  // Totals box on the right
  pdf.setFillColor(lightGray[0], lightGray[1], lightGray[2]);
  pdf.roundedRect(120, yPos, 73, 35, 3, 3, 'F');
  pdf.setDrawColor(borderColor[0], borderColor[1], borderColor[2]);
  pdf.roundedRect(120, yPos, 73, 35, 3, 3, 'S');

  // Sous-total HT
  pdf.setTextColor(secondaryColor[0], secondaryColor[1], secondaryColor[2]);
  pdf.setFontSize(10);
  pdf.setFont('helvetica', 'normal');
  pdf.text('Sous-total HT:', 125, yPos + 7);
pdf.text(this.formatMontantWithoutCurrency(this.facture?.montantHT || 0), 185, yPos + 7, { align: 'right' });
  // TVA
  pdf.text(`TVA (${this.facture?.tva || 0}%):`, 125, yPos + 15);
  const tvaAmount = (this.facture?.montantHT || 0) * (this.facture?.tva || 0) / 100;
pdf.text(this.formatMontantWithoutCurrency(tvaAmount), 185, yPos + 15, { align: 'right' });

  // Total TTC
  pdf.setLineWidth(0.5);
  pdf.line(125, yPos + 20, 190, yPos + 20);
  
  pdf.setFont('helvetica', 'bold');
  pdf.setFontSize(12);
  pdf.text('TOTAL TTC:', 125, yPos + 27);
  pdf.setTextColor(primaryColor[0], primaryColor[1], primaryColor[2]);
pdf.text(this.formatMontantWithoutCurrency(this.facture?.montantTTC || 0), 185, yPos + 27, { align: 'right' });

// Après la section TOTALS, ajoutez ceci:

// Montant en toutes lettres
pdf.setFontSize(8);
pdf.setFont('helvetica', 'normal');
pdf.setTextColor(secondaryColor[0], secondaryColor[1], secondaryColor[2]);

const amountInWords = this.numberToWords(this.facture?.montantTTC || 0);
pdf.text(amountInWords, 22, yPos + 6);




// ===== PAYMENT INFORMATION =====
const paymentDetails = this.getPaymentStatusDetails(this.facture!);

if (this.facture?.statutPaiement === 'PAYE') {
  yPos += 45;

  // Paid invoice - Compact design
  pdf.setFillColor(240, 253, 244); // Light green
  pdf.setDrawColor(187, 247, 208); // Green border
  pdf.roundedRect(20, yPos, 173, 25, 3, 3, 'FD');
  
  pdf.setTextColor(22, 163, 74); // Green text
  pdf.setFontSize(10);
  pdf.setFont('helvetica', 'bold');
  pdf.text(' PAIEMENT EFFECTUÉ', 25, yPos + 7);
  
  pdf.setFontSize(7);
  pdf.setFont('helvetica', 'normal');
  
  let detailY = yPos + 14;
  
  // Payment timing detail
  if (paymentDetails.type === 'early') {
    pdf.text(` Paiement anticipé: ${paymentDetails.days} jour${paymentDetails.days > 1 ? 's' : ''} avant`, 25, detailY);
  } else if (paymentDetails.type === 'late_paid') {
    pdf.text(` Paiement en retard: ${paymentDetails.days} jour${paymentDetails.days > 1 ? 's' : ''}`, 25, detailY);
  } else if (paymentDetails.type === 'ontime') {
    pdf.text(`Paiement à la date d'échéance`, 25, detailY);
  }
  
  // Reference and date on same line
  pdf.text(`Réf: ${this.facture.referencePaiement || '-'}  |  Date: ${this.formatDate(this.facture.datePaiement)}`, 25, detailY + 5);
  
   yPos += 45;
 // Move yPos after payment info
} else {
  // Unpaid invoice
  yPos += 45; // Add small space before status
  
  if (paymentDetails.type === 'overdue') {
    // Overdue invoice - Red
    pdf.setFillColor(254, 242, 242); // Light red
    pdf.setDrawColor(248, 113, 113); // Red border
    pdf.roundedRect(20, yPos, 173, 18, 3, 3, 'FD');
    
    pdf.setTextColor(185, 28, 28); // Dark red
    pdf.setFontSize(9);
    pdf.setFont('helvetica', 'bold');
    pdf.text(' FACTURE EN RETARD', 25, yPos + 6);
    
    pdf.setFontSize(7);
    pdf.setFont('helvetica', 'normal');
    pdf.text(paymentDetails.message, 25, yPos + 12);
    
  yPos += 45;
    
  } else if (paymentDetails.type === 'due_today') {
    // Due today - Orange
    pdf.setFillColor(255, 237, 213); // Light orange
    pdf.setDrawColor(251, 146, 60); // Orange border
    pdf.roundedRect(20, yPos, 173, 18, 3, 3, 'FD');
    
    pdf.setTextColor(194, 65, 12); // Dark orange
    pdf.setFontSize(9);
    pdf.setFont('helvetica', 'bold');
    pdf.text(' ÉCHÉANCE AUJOURD\'HUI', 25, yPos + 6);
    
    pdf.setFontSize(7);
    pdf.setFont('helvetica', 'normal');
    pdf.text('À payer aujourd\'hui', 25, yPos + 12);
    
  yPos += 45;
    
  } else if (paymentDetails.type === 'pending') {
    // Pending with days remaining
    if (paymentDetails.days <= 3) {
      // Urgent - less than 3 days - Orange
      pdf.setFillColor(255, 237, 213); // Light orange
      pdf.setDrawColor(251, 146, 60); // Orange border
    } else {
      // Normal pending - Yellow
      pdf.setFillColor(254, 249, 195); // Light yellow
      pdf.setDrawColor(253, 224, 71); // Yellow border
    }
    pdf.roundedRect(20, yPos, 173, 18, 3, 3, 'FD');
    
    pdf.setTextColor(0, 0, 0);
    pdf.setFontSize(9);
    pdf.setFont('helvetica', 'bold');
    
    if (paymentDetails.days <= 3) {
      pdf.text(' FACTURE URGENTE', 25, yPos + 6);
    } else {
      pdf.text(' FACTURE EN ATTENTE', 25, yPos + 6);
    }
    
    pdf.setFontSize(7);
    pdf.setFont('helvetica', 'normal');
    pdf.text(paymentDetails.message, 25, yPos + 12);
    
  yPos += 45;
  }
}

// ===== FOOTER =====
yPos = 260; // Fixed position for footer
pdf.setDrawColor(borderColor[0], borderColor[1], borderColor[2]);
pdf.line(20, yPos, 190, yPos);

  
  pdf.setTextColor(150, 150, 150);
  pdf.setFontSize(8);
  pdf.setFont('helvetica', 'normal');
  pdf.text(`Générée automatiquement le ${new Date().toLocaleDateString('fr-FR')}`, 105, yPos + 5, { align: 'center' });
  pdf.setTextColor(primaryColor[0], primaryColor[1], primaryColor[2]);
  pdf.text('Merci de votre confiance', 105, yPos + 10, { align: 'center' });

  // Save the PDF
  pdf.save(`facture_${this.facture?.numeroFacture?.replace(/\//g, '-')}.pdf`);
  
  this.loading = false;
  this.successMessage = 'PDF généré avec succès';
  setTimeout(() => this.successMessage = '', 3000);
}


formatPhoneNumber(phone: string): string {
  if (!phone) return '-';
  
  // Remove any existing spaces or special characters
  const cleaned = phone.replace(/\s+/g, '');
  
  // Check if it starts with +216 (Tunisia)
  if (cleaned.startsWith('+216')) {
    const number = cleaned.substring(4); // Remove +216
    // Format as: +216 12 121 212
    const groups = number.match(/(\d{2})(\d{3})(\d{3})/);
    if (groups) {
      return `+216 ${groups[1]} ${groups[2]} ${groups[3]}`;
    }
  }
  
  // Fallback: format as XX XXX XXX for 8-digit numbers
  const groups = cleaned.match(/(\d{2})(\d{3})(\d{3})/);
  if (groups) {
    return `${groups[1]} ${groups[2]} ${groups[3]}`;
  }
  
  return phone;
}

// Helper method to add logo
private addLogoToPDF(pdf: jsPDF, logoUrl: string): Promise<void> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => {
      const canvas = document.createElement('canvas');
      canvas.width = img.width;
      canvas.height = img.height;
      const ctx = canvas.getContext('2d');
      ctx?.drawImage(img, 0, 0);
      const imgData = canvas.toDataURL('image/png');
      
      try {
        pdf.addImage(imgData, 'PNG', 20, 15, 25, 15);
        resolve();
      } catch (e) {
        reject(e);
      }
    };
    img.onerror = reject;
    img.src = logoUrl;
  });
}

// Helper method to format dates
private formatDate(dateString: string): string {
  if (!dateString) return '-';
  const date = new Date(dateString);
  const day = date.getDate().toString().padStart(2, '0');
  const month = (date.getMonth() + 1).toString().padStart(2, '0');
  const year = date.getFullYear();
  return `${day}/${month}/${year}`;
}

// Ajoutez cette méthode helper dans votre composant
private formatMontantWithoutCurrency(montant: number): string {
  return new Intl.NumberFormat('fr-TN', { 
    minimumFractionDigits: 2, 
    maximumFractionDigits: 2 
  }).format(montant || 0);
}

private numberToWords(num: number): string {
  if (num === 0) return 'ZERO DINAR';
  
  const units = ['', 'UN', 'DEUX', 'TROIS', 'QUATRE', 'CINQ', 'SIX', 'SEPT', 'HUIT', 'NEUF', 'DIX'];
  const teens = ['DIX', 'ONZE', 'DOUZE', 'TREIZE', 'QUATORZE', 'QUINZE', 'SEIZE', 'DIX-SEPT', 'DIX-HUIT', 'DIX-NEUF'];
  const tens = ['', 'DIX', 'VINGT', 'TRENTE', 'QUARANTE', 'CINQUANTE', 'SOIXANTE', 'SOIXANTE-DIX', 'QUATRE-VINGT', 'QUATRE-VINGT-DIX'];
  
  const whole = Math.floor(num);
  const decimal = Math.round((num - whole) * 100);
  
  function convertLessThanThousand(n: number): string {
    if (n === 0) return '';
    
    let result = '';
    
    // Centaines
    if (n >= 100) {
      const hundreds = Math.floor(n / 100);
      if (hundreds === 1) {
        result += 'CENT';
      } else {
        result += units[hundreds] + ' CENT';
      }
      n %= 100;
      if (n > 0) result += ' ';
    }
    
    // Dizaines et unités
    if (n >= 70 && n < 80) {
      // 70-79 : SOIXANTE-DIX, SOIXANTE-ONZE, etc.
      result += 'SOIXANTE';
      n -= 60;
      if (n === 11) {
        result += ' ET ONZE';
      } else if (n === 12) {
        result += '-DOUZE';
      } else {
        result += '-' + teens[n - 10];
      }
    } else if (n >= 90) {
      // 90-99 : QUATRE-VINGT-DIX, QUATRE-VINGT-ONZE, etc.
      result += 'QUATRE-VINGT';
      n -= 80;
      if (n === 10) {
        result += '-DIX';
      } else if (n === 11) {
        result += '-ONZE';
      } else if (n === 12) {
        result += '-DOUZE';
      } else {
        result += '-' + teens[n - 10];
      }
    } else if (n >= 20) {
      const ten = Math.floor(n / 10);
      result += tens[ten];
      n %= 10;
      if (n === 1) {
        result += ' ET UN';
      } else if (n > 0) {
        result += ' ' + units[n];
      }
    } else if (n >= 10) {
      result += teens[n - 10];
    } else if (n > 0) {
      result += units[n];
    }
    
    return result;
  }
  
  function convert(n: number): string {
    if (n === 0) return '';
    
    let result = '';
    
    // Milliers
    if (n >= 1000) {
      const thousands = Math.floor(n / 1000);
      if (thousands === 1) {
        result += 'MILLE ';
      } else {
        result += convertLessThanThousand(thousands) + ' MILLE ';
      }
      n %= 1000;
    }
    
    // Reste (< 1000)
    if (n > 0) {
      result += convertLessThanThousand(n);
    }
    
    return result.trim();
  }
  
  const wholeInWords = convert(whole);
  
  // Gérer le pluriel de DINAR
  const dinarText = whole > 1 ? 'DINARS' : 'DINAR';
  
  // Convertir les millimes
  let millimesText = '';
  if (decimal > 0) {
    millimesText = this.convertMillimes(decimal);
  } else {
    millimesText = 'ZERO MILLIME';
  }
  
  return `${wholeInWords} ${dinarText} ET ${millimesText}`;
}

// Nouvelle méthode pour convertir les millimes
private convertMillimes(n: number): string {
  if (n === 0) return 'ZERO MILLIME';
  
  const units = ['', 'UN', 'DEUX', 'TROIS', 'QUATRE', 'CINQ', 'SIX', 'SEPT', 'HUIT', 'NEUF'];
  const teens = ['DIX', 'ONZE', 'DOUZE', 'TREIZE', 'QUATORZE', 'QUINZE', 'SEIZE', 'DIX-SEPT', 'DIX-HUIT', 'DIX-NEUF'];
  const tens = ['', 'DIX', 'VINGT', 'TRENTE', 'QUARANTE', 'CINQUANTE', 'SOIXANTE', 'SOIXANTE-DIX', 'QUATRE-VINGT', 'QUATRE-VINGT-DIX'];
  
  function convertLessThanHundred(n: number): string {
    if (n === 0) return '';
    
    let result = '';
    
    if (n >= 70 && n < 80) {
      result += 'SOIXANTE';
      n -= 60;
      if (n === 11) {
        result += ' ET ONZE';
      } else if (n === 12) {
        result += '-DOUZE';
      } else {
        result += '-' + teens[n - 10];
      }
    } else if (n >= 90) {
      result += 'QUATRE-VINGT';
      n -= 80;
      if (n === 10) {
        result += '-DIX';
      } else if (n === 11) {
        result += '-ONZE';
      } else if (n === 12) {
        result += '-DOUZE';
      } else {
        result += '-' + teens[n - 10];
      }
    } else if (n >= 20) {
      const ten = Math.floor(n / 10);
      result += tens[ten];
      n %= 10;
      if (n === 1) {
        result += ' ET UN';
      } else if (n > 0) {
        result += '-' + units[n];
      }
    } else if (n >= 10) {
      result += teens[n - 10];
    } else if (n > 0) {
      result += units[n];
    }
    
    return result;
  }
  
  const result = convertLessThanHundred(n);
  return result + (n > 1 ? ' MILLIMES' : ' MILLIME');
}


// Add this method to calculate payment details
public getPaymentStatusDetails(facture: Facture): { type: string; days: number; message: string; color: string } {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  
  const echeance = new Date(facture.dateEcheance);
  echeance.setHours(0, 0, 0, 0);
  
  const diffTime = echeance.getTime() - today.getTime();
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  
  if (facture.statutPaiement === 'PAYE') {
    if (facture.datePaiement) {
      const paymentDate = new Date(facture.datePaiement);
      paymentDate.setHours(0, 0, 0, 0);
      
      if (paymentDate < echeance) {
        const daysEarly = Math.ceil((echeance.getTime() - paymentDate.getTime()) / (1000 * 60 * 60 * 24));
        return {
          type: 'early',
          days: daysEarly,
          message: `Paiement effectué ${daysEarly} jour${daysEarly > 1 ? 's' : ''} avant échéance`,
          color: 'green'
        };
      } else if (paymentDate > echeance) {
        const daysLate = Math.ceil((paymentDate.getTime() - echeance.getTime()) / (1000 * 60 * 60 * 24));
        return {
          type: 'late_paid',
          days: daysLate,
          message: `Paiement effectué avec ${daysLate} jour${daysLate > 1 ? 's' : ''} de retard`,
          color: 'orange'
        };
      } else {
        return {
          type: 'ontime',
          days: 0,
          message: 'Paiement effectué à la date d\'échéance',
          color: 'blue'
        };
      }
    }
    return {
      type: 'paid',
      days: 0,
      message: 'Paiement effectué',
      color: 'green'
    };
  } else {
    if (diffDays < 0) {
      const daysLate = Math.abs(diffDays);
      return {
        type: 'overdue',
        days: daysLate,
        message: `${daysLate} jour${daysLate > 1 ? 's' : ''} de retard`,
        color: 'red'
      };
    } else if (diffDays === 0) {
      return {
        type: 'due_today',
        days: 0,
        message: 'À payer aujourd\'hui',
        color: 'orange'
      };
    } else {
      return {
        type: 'pending',
        days: diffDays,
        message: `${diffDays} jour${diffDays > 1 ? 's' : ''} restants`,
        color: 'yellow'
      };
    }
  }
}

}