import { Component, OnInit ,HostListener } from '@angular/core';
import { FactureService, Facture, FactureRequest, PaiementRequest } from '../../services/facture.service';
import { ConventionService } from '../../services/convention.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { TimeFormatService } from '../../services/time-format.service';
import { ApplicationService } from '../../services/application.service';
import { Router } from '@angular/router';
import jsPDF from 'jspdf';

import { EmailPdfService } from '../../services/email-pdf.service';
import { NomenclatureService, Structure } from '../../services/nomenclature.service';

@Component({
  selector: 'app-facture',
  templateUrl: './facture.component.html',
  styleUrls: ['./facture.component.css'],
  standalone: false
})
export class FactureComponent implements OnInit {
  facture: Facture | null = null;
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

  activeActionMenu: number | null = null;
  menuPosition: 'top' | 'bottom' = 'bottom';



  showPrintModal = false;
printFactureData: Facture | null = null;
printFactureConvention: any = null;

  currentPage = 1;
  itemsPerPage = 7;
  paginatedFactures: Facture[] = [];


  applications: any[] = [];
  filterApplicationName: string | null = null;
  activeFilters: { type: string; value: any; label: string }[] = [];

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
    private authService: AuthService,
    private timeFormat: TimeFormatService,
    private applicationService: ApplicationService,
    private router:Router,
    private emailPdfService: EmailPdfService,

  ) {}

  ngOnInit(): void {
      console.log('Token in localStorage:', localStorage.getItem('token'));
  console.log('Current user:', localStorage.getItem('currentUser'));
  
    this.loadFactures();
    this.loadConventions();
    this.loadStructures(); 
    this.loadApplications();
  }


  loadApplications(): void {
  this.applicationService.getAllApplications().subscribe({
    next: (response) => {
      if (response.success) {
        this.applications = response.data || response.applications || [];
      }
    },
    error: (error) => {
      console.error('Error loading applications:', error);
    }
  });
}


  getPaymentStatus(facture: Facture): string {
  return this.timeFormat.formatPaymentStatus(facture);
}

getPaymentStatusClass(facture: Facture): string {
  return this.timeFormat.getPaymentStatusColor(facture);
}



// Add a helper method for detailed display
getPaymentDetails(facture: Facture): any {
  if (facture.statutPaiement === 'PAYE' && facture.datePaiement && facture.dateEcheance) {
    const paymentDate = new Date(facture.datePaiement);
    const dueDate = new Date(facture.dateEcheance);
    
    if (paymentDate < dueDate) {
      const diffTime = dueDate.getTime() - paymentDate.getTime();
      const diffDays = Math.floor(diffTime / (1000 * 3600 * 24));
      return {
        type: 'advance',
        days: diffDays,
        formatted: this.timeFormat.formatDaysToHumanReadable(diffDays),
        icon: 'fast_forward'
      };
    } else if (paymentDate > dueDate) {
      const diffTime = paymentDate.getTime() - dueDate.getTime();
      const diffDays = Math.floor(diffTime / (1000 * 3600 * 24));
      return {
        type: 'late',
        days: diffDays,
        formatted: this.timeFormat.formatDaysToHumanReadable(diffDays),
      };
    } else {
      return {
        type: 'ontime',
        days: 0,
        formatted: 'À la date d\'échéance',
        icon: 'schedule'
      };
    }
  }
  return null;
}

applyFilters(): void {
  this.filteredFactures = this.factures;
  
  // Apply all active filters
  this.activeFilters.forEach(filter => {
    switch(filter.type) {
      case 'status':
        this.filteredFactures = this.filteredFactures.filter(f => 
          f.statutPaiement === filter.value
        );
        break;
      case 'application':
        this.filteredFactures = this.filteredFactures.filter(f => 
          f.applicationName === filter.value
        );
        break;
    }
  });
  
  if (this.searchTerm.trim()) {
    this.searchFactures();
  }
  
  this.currentPage = 1;
  this.updatePagination();
}

filterByStatus(status: string): void {
  this.filterStatut = status;
  
  // Remove existing status filter if any
  this.activeFilters = this.activeFilters.filter(f => f.type !== 'status');
  
  // Add new status filter if not empty
  if (status) {
    const statusOption = this.statuts.find(s => s === status);
    this.activeFilters.push({
      type: 'status',
      value: status,
      label: `Statut: ${this.getStatutLabel(status)}`
    });
  }
  
  this.applyFilters();
}

// Add method to filter by application
filterByApplication(applicationName: string | null): void {
  this.filterApplicationName = applicationName;
  
  // Remove existing application filter
  this.activeFilters = this.activeFilters.filter(f => f.type !== 'application');
  
  // Add new application filter if not empty
  if (applicationName) {
    const application = this.applications.find(a => a.name === applicationName);
    this.activeFilters.push({
      type: 'application',
      value: applicationName,
      label: `Application: ${application?.name || applicationName}`
    });
  }
  
  this.applyFilters();
}

// Add method to remove a specific filter
removeFilter(filter: { type: string; value: any; label: string }): void {
  this.activeFilters = this.activeFilters.filter(f => 
    !(f.type === filter.type && f.value === filter.value)
  );
  
  // Reset the corresponding filter property
  switch(filter.type) {
    case 'status':
      this.filterStatut = '';
      break;
    case 'application':
      this.filterApplicationName = null;
      break;
  }
  
  this.applyFilters();
}

// Add method to clear all filters
clearAllFilters(): void {
  this.activeFilters = [];
  this.filterStatut = '';
  this.filterApplicationName = null;
  this.searchTerm = '';
  this.applyFilters();
}

// Update searchFactures to respect active filters
searchFactures(): void {
  if (!this.searchTerm.trim()) {
    this.applyFilters();
    return;
  }

  const term = this.searchTerm.toLowerCase();
  // First get currently filtered factures
  let currentFiltered = [...this.factures];
  
  // Apply active filters first
  this.activeFilters.forEach(filter => {
    switch(filter.type) {
      case 'status':
        currentFiltered = currentFiltered.filter(f => 
          f.statutPaiement === filter.value
        );
        break;
      case 'application':
        currentFiltered = currentFiltered.filter(f => 
          f.applicationName === filter.value
        );
        break;
    }
  });
  
  // Then apply search
  this.filteredFactures = currentFiltered.filter(facture =>
    facture.numeroFacture.toLowerCase().includes(term) ||
    facture.conventionReference?.toLowerCase().includes(term) ||
    facture.conventionLibelle?.toLowerCase().includes(term) ||
    facture.structureBeneficielName?.toLowerCase().includes(term) ||
    facture.structureResponsableName?.toLowerCase().includes(term) ||
    facture.applicationName?.toLowerCase().includes(term) || 
    facture.applicationCode?.toLowerCase().includes(term) || 
    facture.statutPaiement.toLowerCase().includes(term) ||
    facture.datePaiement?.toLowerCase().includes(term) ||
    (facture.referencePaiement?.toLowerCase().includes(term))
  );
  
  this.currentPage = 1;
  this.updatePagination();
}




loadFactures(): void {
  this.loading = true;
  this.factureService.getAllFactures().subscribe({
    next: (response) => {
      if (response.success) {
        this.factures = response.data;
        this.filteredFactures = [...this.factures];
        this.currentPage = 1; // Reset to first page
        this.updatePagination();
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


  filterByStatut(factures: Facture[]): Facture[] {
    if (!this.filterStatut) {
      return factures;
    }
    return factures.filter(facture => facture.statutPaiement === this.filterStatut);
  }


  getMath(): any {
  return Math;
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







getDaysUntilDue(dateEcheance: string): number {
  if (!dateEcheance) return 0;
  
  const today = new Date();
  today.setHours(0, 0, 0, 0); // Set to beginning of day
  
  const echeance = new Date(dateEcheance);
  echeance.setHours(0, 0, 0, 0); // Set to beginning of day
  
  // Calculate difference in milliseconds
  const diffTime = echeance.getTime() - today.getTime();
  
  // Convert to days
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  
  return diffDays;
}

// Also add a method to get the color class based on days
getDaysUntilDueClass(days: number): string {
  if (days <= 0) return 'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300';
  if (days <= 3) return 'bg-orange-100 dark:bg-orange-900/30 text-orange-700 dark:text-orange-300';
  if (days <= 7) return 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-300';
  return 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300';
}

// Add a method to get the status text
getDaysUntilDueText(days: number): string {
  if (days === 0) return "Aujourd'hui";
  if (days === 1) return "1 jour";
  if (days === -1) return "1 jour de retard";
  
  if (days > 0) return `${days} jours`;
  return `${Math.abs(days)} jours de retard`;
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


updatePagination(): void {
  const startIndex = (this.currentPage - 1) * this.itemsPerPage;
  const endIndex = startIndex + this.itemsPerPage;
  this.paginatedFactures = this.filteredFactures.slice(startIndex, endIndex);
}

// Get total pages
get totalPages(): number {
  return Math.ceil(this.filteredFactures.length / this.itemsPerPage);
}

// Get page numbers to display
get pageNumbers(): number[] {
  const pages = [];
  const total = this.totalPages;
  const current = this.currentPage;
  
  // Show up to 5 page numbers
  let start = Math.max(1, current - 2);
  let end = Math.min(total, start + 4);
  
  if (end - start < 4) {
    start = Math.max(1, end - 4);
  }
  
  for (let i = start; i <= end; i++) {
    pages.push(i);
  }
  
  return pages;
}

// Change page
goToPage(page: number): void {
  if (page >= 1 && page <= this.totalPages && page !== this.currentPage) {
    this.currentPage = page;
    this.updatePagination();
  }
}



viewFactureDetails(id: number): void {
  this.router.navigate(['/factures', id]);
}


toggleActionMenu(factureId: number, event: MouseEvent): void {
  // Get the button element
  const button = event.currentTarget as HTMLElement;
  const rect = button.getBoundingClientRect();
  
  // Calculate space below the button
  const spaceBelow = window.innerHeight - rect.bottom;
  
  // Menu height (approx 280px for your menu)
  const menuHeight = 280;
  
  // Decide position
  if (spaceBelow < menuHeight) {
    this.menuPosition = 'top';
  } else {
    this.menuPosition = 'bottom';
  }
  
  // Toggle menu
  if (this.activeActionMenu === factureId) {
    this.activeActionMenu = null;
  } else {
    this.activeActionMenu = factureId;
  }
}

// Close menu when clicking outside
@HostListener('document:click', ['$event'])
onDocumentClick(event: MouseEvent): void {
  const target = event.target as HTMLElement;
  if (!target.closest('.relative')) {
    this.activeActionMenu = null;
  }
}

// Close menu on scroll to prevent weird positioning
@HostListener('window:scroll', ['$event'])
onWindowScroll(): void {
  this.activeActionMenu = null;
}


// Send reminder email
async sendReminder(factureId: number): Promise<void> {
  this.loading = true;
  this.activeActionMenu = null;
  
  try {
    // Get the facture details
    const facture = this.factures.find(f => f.id === factureId);
    if (!facture) {
      this.errorMessage = 'Facture non trouvée';
      return;
    }
    
    // Get structure details for PDF generation
    let structureBeneficiel: Structure | null = null;
    if (facture.structureBeneficielName) {
      const structures = await this.nomenclatureService.getStructures().toPromise();
      if (structures) {
        structureBeneficiel = structures.find(s => s.name === facture.structureBeneficielName) || null;
      }
    }
    
    // Generate PDF
    const pdfBlob = await this.emailPdfService.generatePDFBlob(facture, structureBeneficiel, true);
    
    // Convert to base64
    const pdfBase64 = await this.blobToBase64(pdfBlob);
    
    // Send email
    const emailRequest = {
      factureId: factureId,
      to: '', // Will be taken from application
      subject: '',
      message: '',
      pdfBase64: pdfBase64,
      isReminder: true
    };
    
    const response = await this.emailPdfService.sendEmailWithPDF(emailRequest).toPromise();
    
    if (response.success) {
      this.successMessage = response.message;
      setTimeout(() => this.successMessage = '', 3000);
    } else {
      this.errorMessage = response.message || 'Erreur lors de l\'envoi de la relance';
    }
    
  } catch (error: any) {
    console.error('Error sending reminder:', error);
    this.errorMessage = error.error?.message || 'Erreur lors de l\'envoi de la relance';
  } finally {
    this.loading = false;
  }
}

// Send simple email with PDF
async emailPDF(factureId: number): Promise<void> {
  this.loading = true;
  this.activeActionMenu = null;
  
  try {
    const facture = this.factures.find(f => f.id === factureId);
    if (!facture) {
      this.errorMessage = 'Facture non trouvée';
      return;
    }
    
    // Get structure details for PDF generation
    let structureBeneficiel: Structure | null = null;
    if (facture.structureBeneficielName) {
      const structures = await this.nomenclatureService.getStructures().toPromise();
      if (structures) {
        structureBeneficiel = structures.find(s => s.name === facture.structureBeneficielName) || null;
      }
    }
    
    // Generate PDF
    const pdfBlob = await this.emailPdfService.generatePDFBlob(facture, structureBeneficiel, false);
    const pdfBase64 = await this.blobToBase64(pdfBlob);
    
    // Send email
    const emailRequest = {
      factureId: factureId,
      to: '',
      subject: '',
      message: '',
      pdfBase64: pdfBase64,
      isReminder: false
    };
    
    const response = await this.emailPdfService.sendEmailWithPDF(emailRequest).toPromise();
    
    if (response.success) {
      this.successMessage = response.message;
      setTimeout(() => this.successMessage = '', 3000);
    } else {
      this.errorMessage = response.message || 'Erreur lors de l\'envoi de l\'email';
    }
    
  } catch (error: any) {
    console.error('Error sending email:', error);
    this.errorMessage = error.error?.message || 'Erreur lors de l\'envoi de l\'email';
  } finally {
    this.loading = false;
  }
}

// Helper: Convert Blob to Base64
private blobToBase64(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onloadend = () => {
      const base64String = (reader.result as string).split(',')[1];
      resolve(base64String);
    };
    reader.onerror = reject;
    reader.readAsDataURL(blob);
  });
}



}