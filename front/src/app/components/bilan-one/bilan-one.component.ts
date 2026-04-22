// bilan-one.component.ts
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BilanService, BilanDTO, BilanItem, InvoiceBilanItem } from '../../services/bilan.service';
import { TranslationService } from '../partials/traduction/translation.service';


@Component({
  selector: 'app-bilan-one',
  templateUrl: './bilan-one.component.html',
  styleUrls: ['./bilan-one.component.css']
})
export class BilanOneComponent implements OnInit {
  conventionId: number | null = null;
  bilanData: BilanDTO | null = null;
  loading = false;
  error: string | null = null;
  
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private bilanService: BilanService,
    private translationService: TranslationService
  ) {}
  
  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.conventionId = params['id'];
      if (this.conventionId) {
        this.loadConventionBilan();
      } else {
        this.error = 'ID de convention non spécifié';
      }
    });
  }
  
  loadConventionBilan(): void {
    if (!this.conventionId) return;
    
    this.loading = true;
    this.error = null;
    
    this.bilanService.getConventionBilan(this.conventionId).subscribe({
      next: (response) => {
        if (response.success) {
          this.bilanData = response.data;
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
  
  goBack(): void {
    this.router.navigate(['/revenu-bilan']);
  }
  
  formatDate(dateStr: string): string {
    if (!dateStr) return 'N/A';
    return new Date(dateStr).toLocaleDateString('fr-FR');
  }
  
  formatCurrency(amount: number): string {
    if (amount === undefined || amount === null) return '0,00 TND';
    return amount.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' TND';
  }
  
  formatPercentage(rate: number): string {
    if (rate === undefined || rate === null) return '0%';
    return rate.toFixed(2) + '%';
  }
  
  getStatutPaiementClass(statut: string): string {
    switch (statut) {
      case 'PAYE': return 'payment-paid';
      case 'NON_PAYE': return 'payment-unpaid';
      case 'EN_RETARD': return 'payment-overdue';
      default: return 'payment-default';
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

// Add to your component
getPaiementTypeLabel(paiementType: string): string {
  switch (paiementType) {
    case 'AVANCE': return 'Avance';
    case 'RETARD': return 'Retard';
    case 'PONCTUEL': return 'À temps';
    case 'EN_ATTENTE': return 'En attente';
    case 'EN_RETARD': return 'En retard';
    default: return 'En attente';
  }
}
  

// In your component, add this method
getPeriodiciteText(periodicite: string): string {
  if (!periodicite) return this.translationService.translate('NA');
  
  const normalized = periodicite.toUpperCase();
  
  switch (normalized) {
    case 'MENSUEL':
      return this.translationService.translate('PERIODICITE_MENSUEL');
    case 'TRIMESTRIEL':
      return this.translationService.translate('PERIODICITE_TRIMESTRIEL');
    case 'SEMESTRIEL':
      return this.translationService.translate('PERIODICITE_SEMESTRIEL');
    case 'ANNUEL':
      return this.translationService.translate('PERIODICITE_ANNUEL');
    default:
      return periodicite || this.translationService.translate('NA');
  }
}

  getEtatClass(etat: string): string {
    switch (etat) {
      case 'EN COURS': return 'status-active';
      case 'PLANIFIE': return 'status-planned';
      case 'TERMINE': return 'status-terminated';
      case 'ARCHIVE': return 'status-archived';
      default: return 'status-default';
    }
  }
  
  getPaiementTypeIcon(paiementType: string): string {
    switch (paiementType) {
      case 'AVANCE': return '✅ Payé en avance';
      case 'RETARD': return '⚠️ Payé en retard';
      case 'PONCTUEL': return '✓ Payé à temps';
      case 'EN_ATTENTE': return '⏰ En attente';
      case 'EN_RETARD': return '🔴 En retard';
      default: return '📄';
    }
  }

    getPaiementTypeClass(paiementType: string): string {
   switch (paiementType)  {
      case 'AVANCE': return 'status-active';
      case 'RETARD': return 'status-overdue';
      case 'PONCTUEL': return 'status-active';
      case 'EN_ATTENTE': return 'status-planned';
      case 'EN_RETARD': return 'status-overdue';
      default: return 'status-default';
    }
  }

    getPaiementIcon(paiementType: string): string {
    switch (paiementType) {
      case 'PAYE': return '✓';
      case 'NON_PAYE': return '⚠️';
      case 'EN_RETARD': return '🔴';
      default: return '📄';
    }
  }



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
  if (!dateStr) return 'N/A';
  try {
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return 'N/A';
    return date.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit',year: '2-digit' });
  } catch {
    return 'N/A';
  }
}
}