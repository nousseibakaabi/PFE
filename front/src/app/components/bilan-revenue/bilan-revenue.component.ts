import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { BilanService, BilanDTO, BilanItem, InvoiceBilanItem } from '../../services/bilan.service';
import { Router } from '@angular/router';
import { TranslationService } from '../partials/traduction/translation.service';


@Component({
  selector: 'app-bilan-revenue',
  templateUrl: './bilan-revenue.component.html',
  styleUrls: ['./bilan-revenue.component.css']
})
export class BilanRevenueComponent implements OnInit {
  bilanType: 'factures' | 'conventions' | 'combined' = 'combined';
  includeOldVersions = false;
  dateRangeForm: FormGroup;
  selectedConvention: BilanItem | null = null;
  bilanData: BilanDTO | null = null;
  loading = false;
  error: string | null = null;
  itemsPerPage = 4;
  currentPage = 1;
  
  availableYears: number[] = [];
// In your component
availableMonths = [
  { value: 1, nameKey: 'MOIS_JANVIER' },
  { value: 2, nameKey: 'MOIS_FEVRIER' },
  { value: 3, nameKey: 'MOIS_MARS' },
  { value: 4, nameKey: 'MOIS_AVRIL' },
  { value: 5, nameKey: 'MOIS_MAI' },
  { value: 6, nameKey: 'MOIS_JUIN' },
  { value: 7, nameKey: 'MOIS_JUILLET' },
  { value: 8, nameKey: 'MOIS_AOUT' },
  { value: 9, nameKey: 'MOIS_SEPTEMBRE' },
  { value: 10, nameKey: 'MOIS_OCTOBRE' },
  { value: 11, nameKey: 'MOIS_NOVEMBRE' },
  { value: 12, nameKey: 'MOIS_DECEMBRE' }
];
  
  periodView: 'custom' | 'month' | 'year' = 'custom';
  selectedYear = new Date().getFullYear();
  selectedMonth = new Date().getMonth() + 1;
  
  expandedItems: Set<number> = new Set();
  
  constructor(
    private fb: FormBuilder,
    private bilanService: BilanService,
    private router: Router,
    private translationService: TranslationService
  ) {
    this.dateRangeForm = this.fb.group({
      startDate: [this.getFirstDayOfCurrentMonth()],
      endDate: [this.getLastDayOfCurrentMonth()]
    });
    
    // Initialize available years
    const currentYear = new Date().getFullYear();
    for (let year = currentYear - 5; year <= currentYear + 1; year++) {
      this.availableYears.push(year);
    }
  }
  
// Add this temporary method to debug
ngOnInit(): void {
  this.loadBilan();
  // Add a subscriber to log the data when loaded
  this.bilanService.getCombinedBilan(
    this.getFirstDayOfCurrentMonth(), 
    this.getLastDayOfCurrentMonth(), 
    false
  ).subscribe({
    next: (response) => {
      if (response.success && response.data) {
        // Log the first facture item to see its structure
        const factureItem = response.data.items.find((item: BilanItem) => item.type === 'FACTURE');
        if (factureItem) {
          console.log('Facture item structure:', factureItem);
          console.log('Facture item keys:', Object.keys(factureItem));
          console.log('Facture status field:', factureItem.etat);
        }
      }
    }
  });
}
  
  viewConventionBilan(conventionId: number): void {
    this.router.navigate(['/revenu-bilan/convention', conventionId]);
  }

  getFirstDayOfCurrentMonth(): string {
    const date = new Date();
    return new Date(date.getFullYear(), date.getMonth(), 1).toISOString().split('T')[0];
  }
  
  getLastDayOfCurrentMonth(): string {
    const date = new Date();
    return new Date(date.getFullYear(), date.getMonth() + 1, 0).toISOString().split('T')[0];
  }
  
  loadBilan(): void {
    this.loading = true;
    this.error = null;
    this.currentPage = 1;
    
    let request;
    
    if (this.periodView === 'month') {
      request = this.bilanService.getBilanByMonth(
        this.selectedYear, 
        this.selectedMonth, 
        this.bilanType, 
        this.includeOldVersions
      );
    } else if (this.periodView === 'year') {
      request = this.bilanService.getBilanByYear(
        this.selectedYear, 
        this.bilanType, 
        this.includeOldVersions
      );
    } else {
      const { startDate, endDate } = this.dateRangeForm.value;
      if (this.bilanType === 'factures') {
        request = this.bilanService.getFacturesBilan(startDate, endDate);
      } else if (this.bilanType === 'conventions') {
        request = this.bilanService.getConventionsBilan(startDate, endDate, this.includeOldVersions);
      } else {
        request = this.bilanService.getCombinedBilan(startDate, endDate, this.includeOldVersions);
      }
    }
    
    request.subscribe({
      next: (response) => {
        if (response.success) {
          this.bilanData = response.data;
          this.currentPage = 1;
        } else {
          this.error = response.message;
        }
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.message || 'Erreur lors du chargement du bilan';
        this.loading = false;
      }
    });
  }
  
  get pagedItems(): BilanItem[] {
    if (!this.bilanData?.items?.length) {
      return [];
    }
    const start = (this.currentPage - 1) * this.itemsPerPage;
    return this.bilanData.items.slice(start, start + this.itemsPerPage);
  }

  get totalPages(): number {
    if (!this.bilanData?.items?.length) {
      return 1;
    }
    return Math.max(1, Math.ceil(this.bilanData.items.length / this.itemsPerPage));
  }

  get pageRange(): number[] {
    return Array.from({ length: this.totalPages }, (_, index) => index + 1);
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages) {
      return;
    }
    this.currentPage = page;
  }

  previousPage(): void {
    this.goToPage(this.currentPage - 1);
  }

  nextPage(): void {
    this.goToPage(this.currentPage + 1);
  }

  toggleItemExpand(itemId: number): void {
    if (this.expandedItems.has(itemId)) {
      this.expandedItems.delete(itemId);
    } else {
      this.expandedItems.add(itemId);
    }
  }
  
  isItemExpanded(itemId: number): boolean {
    return this.expandedItems.has(itemId);
  }
  

getEtatClass(etat: string): string {
  const normalizedEtat = etat?.toUpperCase() || '';
  
  switch (normalizedEtat) {
    case 'EN COURS':
      return 'bg-blue-100 dark:bg-blue-900/30 text-blue-800 dark:text-blue-300';
    case 'PLANIFIE':
      return 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-800 dark:text-yellow-300';
    case 'TERMINE':
      return 'bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-300';
    case 'ARCHIVE':
      return 'bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-300';
    default:
      return 'bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-300';
  }
}

getEtatText(etat: string): string {
  const normalizedEtat = etat?.toUpperCase() || '';
  
  switch (normalizedEtat) {
    case 'EN COURS':
      return 'En cours';
    case 'PLANIFIE':
      return 'Planifié';
    case 'TERMINE':
      return 'Terminé';
    case 'ARCHIVE':
      return 'Archivé';
    default:
      return etat || 'N/A';
  }
}

// For facture items - get status from the item
getFactureStatusClass(item: BilanItem): string {
  // For facture items, the status could be in statutPaiement or etat
  const status = item.statutPaiement || item.etat;
  const normalizedStatus = status?.toUpperCase() || '';
  
  switch (normalizedStatus) {
    case 'PAYE':
      return 'bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-300';
    case 'NON_PAYE':
      return 'bg-orange-100 dark:bg-orange-900/30 text-orange-800 dark:text-orange-300';
    case 'EN_RETARD':
      return 'bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-300';
    default:
      // Debug: log what status we're getting
      console.log('Unknown facture status:', status, 'for item:', item.reference);
      return 'bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-300';
  }
}

getFactureStatusText(item: BilanItem): string {
  const status = item.statutPaiement || item.etat;
  const normalizedStatus = status?.toUpperCase() || '';
  
  switch (normalizedStatus) {
    case 'PAYE':
      return 'Payée';
    case 'NON_PAYE':
      return 'Non payée';
    case 'EN_RETARD':
      return 'En retard';
    default:
      return status || 'N/A';
  }
}

// Helper to check if item is a facture
isFactureItem(item: BilanItem): boolean {
  return item.type === 'FACTURE';
}

// Helper to check if item is a convention
isConventionItem(item: BilanItem): boolean {
  return item.type === 'CONVENTION' || item.type === 'OLD_CONVENTION';
}
  
// Update these methods to handle different case formats
getStatutPaiementClass(statut: string): string {
  // Normalize the status to uppercase for comparison
  const normalizedStatut = statut?.toUpperCase() || '';
  
  switch (normalizedStatut) {
    case 'PAYE':
      return 'bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-300';
    case 'NON_PAYE':
      return 'bg-orange-100 dark:bg-orange-900/30 text-orange-800 dark:text-orange-300';
    case 'EN_RETARD':
      return 'bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-300';
    default:
      return 'bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-300';
  }
}

getStatutPaiementText(statut: string): string {
  const normalizedStatut = statut?.toUpperCase() || '';
  
  switch (normalizedStatut) {
    case 'PAYE':
      return 'Payée';
    case 'NON_PAYE':
      return 'Non payée';
    case 'EN_RETARD':
      return 'En retard';
    default:
      return statut || 'N/A';
  }
}

  
getPaiementTypeIcon(paiementType: string): string {
  switch (paiementType) {
    case 'AVANCE': 
      return `<svg class="w-3 h-3 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
              </svg>`;
    case 'RETARD': 
      return `<svg class="w-3 h-3 text-orange-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>`;
    case 'PONCTUEL': 
      return `<svg class="w-3 h-3 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>`;
    case 'EN_ATTENTE': 
      return `<svg class="w-3 h-3 text-yellow-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
                <polyline points="12 6 12 12 16 14" stroke="currentColor" stroke-width="2"/>
              </svg>`;
    case 'EN_RETARD': 
      return `<svg class="w-3 h-3 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>`;
    default: 
      return `<svg class="w-3 h-3 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>`;
  }
}



formatDate(dateStr: string): string {
  if (!dateStr) return this.translationService.translate('NA');
  try {
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return this.translationService.translate('NA');
    
    const lang = this.translationService.getCurrentLanguage();
    let options: Intl.DateTimeFormatOptions;
    let locale: string;
    
    switch (lang) {
      case 'fr':
        locale = 'fr-FR';
        options = { day: 'numeric', month: 'long', year: 'numeric' };
        break;
      case 'en':
        locale = 'en-US';
        options = { month: 'long', day: 'numeric', year: 'numeric' };
        break;
      case 'ar':
        locale = 'ar-TN';
        options = { day: 'numeric', month: 'long', year: 'numeric' };
        break;
      default:
        locale = 'fr-FR';
        options = { day: 'numeric', month: 'long', year: 'numeric' };
    }
    
    return date.toLocaleDateString(locale, options);
  } catch {
    return this.translationService.translate('NA');
  }
}
  
  formatCurrency(amount: number): string {
    if (amount === undefined || amount === null) return '0,00 TND';
    return amount.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' TND';
  }
  
  formatPercentage(rate: number): string {
    if (rate === undefined || rate === null) return '0%';
    return rate.toFixed(2) + '%';
  }


  // Add these methods to your BilanRevenueComponent class

// In your component
getPeriodDisplayText(): string {
  switch (this.periodView) {
    case 'custom':
      return 'PERIODE_PERSONNALISEE';
    case 'month':
      const monthName = this.availableMonths.find(m => m.value === this.selectedMonth)?.nameKey || '';
      // Return the month name as is (will be translated via the month names array)
      return `${monthName} ${this.selectedYear}`;
    case 'year':
      return 'ANNEE_WITH_YEAR';
    default:
      return 'PERIODE';
  }
}


// In your component
getBilanTitleTranslationKey(title: string): string {
  switch (title) {
    case 'Bilan Global (Conventions + Factures)':
      return 'BILAN_GLOBAL';
    case 'Bilan des Conventions':
      return 'BILAN_CONVENTIONS';
    case 'Bilan des Factures':
      return 'BILAN_FACTURES';
    default:
      return 'BILAN';
  }
}

// Helper method to get the month name key for translation
getMonthTranslationKey(monthIndex: number): string {
  const monthKeys = [
    'MOIS_JANVIER', 'MOIS_FEVRIER', 'MOIS_MARS', 'MOIS_AVRIL',
    'MOIS_MAI', 'MOIS_JUIN', 'MOIS_JUILLET', 'MOIS_AOUT',
    'MOIS_SEPTEMBRE', 'MOIS_OCTOBRE', 'MOIS_NOVEMBRE', 'MOIS_DECEMBRE'
  ];
  return monthKeys[monthIndex - 1];
}

getBilanTypeDisplayText(): string {
  switch (this.bilanType) {
    case 'combined':
      return 'Bilan complet';
    case 'conventions':
      return 'Bilan des conventions';
    case 'factures':
      return 'Bilan des factures';
    default:
      return 'Bilan';
  }
}

// Add these methods to your BilanRevenueComponent class

getPercentage(value: number, total: number): string {
  if (!total || total === 0) return '0';
  return ((value / total) * 100).toFixed(1);
}

getPaymentRatePercentage(): string {
  if (!this.bilanData?.summary?.totalInvoices || this.bilanData.summary.totalInvoices === 0) {
    return '0';
  }
  return ((this.bilanData.summary.paidInvoices / this.bilanData.summary.totalInvoices) * 100).toFixed(1);
}


openInvoiceModal(convention: BilanItem): void {
  this.selectedConvention = convention;
  document.body.style.overflow = 'hidden';
}

closeModal(): void {
  this.selectedConvention = null;
  document.body.style.overflow = '';
}

// Fix formatCurrencyShort method
formatCurrencyShort(amount: number): string {
  if (amount === undefined || amount === null) return '0';
  if (amount >= 1000000) {
    return (amount / 1000000).toFixed(1) + this.translationService.translate('CURRENCY_MILLION');
  }
  if (amount >= 1000) {
    return (amount / 1000).toFixed(0) + this.translationService.translate('CURRENCY_THOUSAND');
  }
  // Fix: Get current language from translationService
  const currentLang = this.translationService.getCurrentLanguage();
  return amount.toLocaleString(currentLang, { minimumFractionDigits: 0, maximumFractionDigits: 0 });
}

formatDateShort(dateStr: string): string {
  if (!dateStr) return this.translationService.translate('NA');
  try {
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return this.translationService.translate('NA');
    
    // Get current language
    const lang = this.translationService.getCurrentLanguage();
    
    // Define options based on language
    let options: Intl.DateTimeFormatOptions;
    let locale: string;
    
    switch (lang) {
      case 'fr':
        // French: DD/MM/YYYY
        locale = 'fr-FR';
        options = { day: '2-digit', month: '2-digit', year: 'numeric' };
        break;
      case 'en':
        // English: MM/DD/YYYY
        locale = 'en-US';
        options = { month: '2-digit', day: '2-digit', year: 'numeric' };
        break;
      case 'ar':
        // Arabic: DD/MM/YYYY (same as French but with Arabic numerals)
        locale = 'ar-TN';
        options = { day: '2-digit', month: '2-digit', year: 'numeric' };
        break;
      default:
        locale = 'fr-FR';
        options = { day: '2-digit', month: '2-digit', year: 'numeric' };
    }
    
    return date.toLocaleDateString(locale, options);
  } catch {
    return this.translationService.translate('NA');
  }
}
// Helper method to get locale for date formatting
private getLocaleForLanguage(lang: string): string {
  switch (lang) {
    case 'fr': return 'fr-FR';
    case 'ar': return 'ar-TN';  // Tunisian Arabic locale
    case 'en': return 'en-US';
    default: return 'fr-FR';
  }
}

}