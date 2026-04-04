// src/app/components/commercial/commercial.component.ts
import { Component, OnInit, ViewChild, ElementRef, AfterViewInit, OnDestroy } from '@angular/core';
import { StatsService } from '../../services/stats.service';
import { ChartService } from '../../services/chart.service';
import { AuthService } from 'src/app/services/auth.service';
import { TranslationService } from '../partials/traduction/translation.service';

@Component({
  selector: 'app-commercial',
  templateUrl: './commercial.component.html',
  styleUrls: ['./commercial.component.css']
})
export class CommercialComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('conventionStatusChart') conventionStatusChartRef!: ElementRef;
  @ViewChild('monthlyTrendsChart') monthlyTrendsChartRef!: ElementRef;
  @ViewChild('paymentStatusChart') paymentStatusChartRef!: ElementRef;
  @ViewChild('paymentMethodChart') paymentMethodChartRef!: ElementRef;
  @ViewChild('structureTypeChart') structureTypeChartRef!: ElementRef;
  @ViewChild('structureCountsChart') structureCountsChartRef!: ElementRef;
  structureCountsChart: any = null;
  
  conventionStatusChart: any = null;
  monthlyTrendsChart: any = null;
  paymentStatusChart: any = null;
  paymentMethodChart: any = null;
  structureTypeChart: any = null;

  conventionStats: any = {};
  factureStats: any = {};
  summaryStats: any = {};
  topStructures: any[] = [];
  generatedTopPartner: any = null;
  
  currentUser: any = null;

  currentPage: number = 1;
  itemsPerPage: number = 5;
  totalItems: number = 0;

  priorityFilter: string = 'ALL';
  sortBy: string = 'joursRetard';
  sortDirection: string = 'desc';

  filteredOverdueInvoices: any[] = [];
  dashboardStats: any = {};
  financialStats: any = {};
  overdueAlerts: any[] = [];
  
  isLoading = true;

  quickStats = {
    totalConventions: 0,
    activeConventions: 0,
    totalFactures: 0,
    paidFactures: 0,
    totalRevenue: 0,
    pendingRevenue: 0,
    overdueAmount: 0,
    collectionRate: 0,
    conventionsToday: 0,
    facturesToday: 0,
    dueToday: 0,
    overdueToday: 0
  };

  constructor(
    private statsService: StatsService,
    private chartService: ChartService,
    private authService: AuthService,
    private translationService: TranslationService
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.loadCommercialStats();
  }

  ngAfterViewInit() {
    setTimeout(() => {
      console.log('Convention Stats:', this.conventionStats);
      console.log('Top Structures available:', this.conventionStats?.topStructures);
      console.log('Payment Status:', this.factureStats?.paymentStatus);
    }, 2000);
  }

  updateFilteredData(): void {
    let invoices = this.getOverdueInvoices();
    
    if (this.priorityFilter !== 'ALL') {
      invoices = invoices.filter(invoice => {
        const daysAgo = this.getDaysAgo(invoice.dateEcheance);
        const priority = this.getPriorityLabel(daysAgo);
        return priority === this.priorityFilter;
      });
    }
    
    invoices = this.sortInvoices(invoices, this.sortBy, this.sortDirection);
    this.totalItems = invoices.length;
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    this.filteredOverdueInvoices = invoices.slice(startIndex, startIndex + this.itemsPerPage);
  }

  sortInvoices(invoices: any[], sortBy: string, direction: string): any[] {
    return [...invoices].sort((a, b) => {
      let valueA, valueB;
      
      switch (sortBy) {
        case 'joursRetard':
          valueA = this.getDaysAgo(a.dateEcheance);
          valueB = this.getDaysAgo(b.dateEcheance);
          break;
        case 'montant':
          valueA = a.montant || 0;
          valueB = b.montant || 0;
          break;
        case 'dateEcheance':
          valueA = new Date(a.dateEcheance).getTime();
          valueB = new Date(b.dateEcheance).getTime();
          break;
        default:
          valueA = a[sortBy] || 0;
          valueB = b[sortBy] || 0;
      }
      
      if (direction === 'asc') {
        return valueA > valueB ? 1 : -1;
      } else {
        return valueA < valueB ? 1 : -1;
      }
    });
  }

  changePage(page: number): void {
    if (page < 1 || page > this.getTotalPages()) return;
    this.currentPage = page;
    this.updateFilteredData();
  }

  getTotalPages(): number {
    return Math.ceil(this.totalItems / this.itemsPerPage);
  }

  changeItemsPerPage(count: number): void {
    this.itemsPerPage = count;
    this.currentPage = 1;
    this.updateFilteredData();
  }

  resetFilters(): void {
    this.priorityFilter = 'ALL';
    this.sortBy = 'joursRetard';
    this.sortDirection = 'desc';
    this.currentPage = 1;
    this.updateFilteredData();
  }

  calculateQuickStats(): void {
    this.quickStats = {
      totalConventions: 0,
      activeConventions: 0,
      totalFactures: 0,
      paidFactures: 0,
      totalRevenue: 0,
      pendingRevenue: 0,
      overdueAmount: 0,
      collectionRate: 0,
      conventionsToday: 0,
      facturesToday: 0,
      dueToday: 0,
      overdueToday: 0
    };
    
    if (this.summaryStats) {
      this.quickStats.totalConventions = this.summaryStats.totalConventions || 0;
      this.quickStats.totalFactures = this.summaryStats.totalFactures || 0;
      this.quickStats.conventionsToday = this.summaryStats.conventionsToday || 0;
      this.quickStats.facturesToday = this.summaryStats.facturesToday || 0;
      this.quickStats.dueToday = this.summaryStats.dueToday || 0;
      this.quickStats.overdueToday = this.summaryStats.overdueToday || 0;
    }
    
    if (this.conventionStats?.statusDistribution) {
      const activeStatuses = ['EN COURS', 'PLANIFIE'];
      this.quickStats.activeConventions = this.conventionStats.statusDistribution
        .filter((status: any) => activeStatuses.includes(status.name))
        .reduce((sum: number, status: any) => sum + (status.count || 0), 0);
    }
    
    if (this.factureStats?.paymentStatus) {
      const paidStatus = this.factureStats.paymentStatus.find((p: any) => p.status === 'PAYE');
      this.quickStats.paidFactures = paidStatus?.count || 0;
    }
    
    if (this.financialStats) {
      this.quickStats.totalRevenue = this.financialStats.totalRevenue || 0;
      this.quickStats.pendingRevenue = this.financialStats.pendingPayments || 0;
      this.quickStats.overdueAmount = this.financialStats.overdueAmount || 0;
      this.quickStats.collectionRate = this.financialStats.collectionRate || 0;
    }
  }

  getCurrentPageEnd(): number {
    const end = this.currentPage * this.itemsPerPage;
    return Math.min(end, this.totalItems);
  }

  getCurrentPageStart(): number {
    return (this.currentPage - 1) * this.itemsPerPage + 1;
  }

  getPageNumbers(): number[] {
    const totalPages = this.getTotalPages();
    const current = this.currentPage;
    const pages: number[] = [];
    
    if (totalPages <= 7) {
      for (let i = 1; i <= totalPages; i++) pages.push(i);
    } else {
      if (current <= 4) {
        for (let i = 1; i <= 5; i++) pages.push(i);
        pages.push(-1);
        pages.push(totalPages);
      } else if (current >= totalPages - 3) {
        pages.push(1);
        pages.push(-1);
        for (let i = totalPages - 4; i <= totalPages; i++) pages.push(i);
      } else {
        pages.push(1);
        pages.push(-1);
        pages.push(current - 1);
        pages.push(current);
        pages.push(current + 1);
        pages.push(-1);
        pages.push(totalPages);
      }
    }
    
    return pages;
  }

  destroyCharts(): void {
    this.chartService.destroyChart(this.conventionStatusChart);
    this.chartService.destroyChart(this.monthlyTrendsChart);
    this.chartService.destroyChart(this.paymentStatusChart);
    
    this.conventionStatusChart = null;
    this.monthlyTrendsChart = null;
    this.paymentStatusChart = null;
  }

  getOverdueInvoices(): any[] {
    return this.factureStats?.overdueDetails || [];
  }

  loadCommercialStats(): void {
    this.isLoading = true;
    
    Promise.all([
      this.statsService.getConventionDetailedStats().toPromise(),
      this.statsService.getFactureDetailedStats().toPromise(),
      this.statsService.getFinancialDetailedStats().toPromise(),
      this.statsService.getSummaryStats().toPromise(),
      this.statsService.getOverdueAlerts().toPromise()
    ]).then((results: any[]) => {
      console.log('Commercial stats loaded:', results);
      
      if (results[0]?.success) {
        this.conventionStats = results[0].data;
        this.generateTopStructuresFromConventions();
      }
      
      if (results[1]?.success) {
        this.factureStats = results[1].data;
      }
      
      if (results[2]?.success) {
        this.financialStats = results[2].data;
      }
      
      if (results[3]?.success) {
        this.summaryStats = results[3].data;
      }
      
      if (results[4]?.success) {
        this.overdueAlerts = results[4].data;
      }
      
      this.isLoading = false;
      this.updateFilteredData();
      this.calculateQuickStats();
      
      setTimeout(() => {
        this.destroyCharts();
        this.renderCharts();
      }, 100);
      
    }).catch(error => {
      console.error('Error loading commercial stats:', error);
      this.isLoading = false;
    });
  }

  generateTopStructuresFromConventions(): void {
    this.topStructures = [];
    
    if (this.conventionStats?.amountByStatus) {
      const statuses = Object.keys(this.conventionStats.amountByStatus);
      
      if (statuses.length > 0) {
        let topStatus = '';
        let topAmount = 0;
        
        statuses.forEach(status => {
          const amount = this.conventionStats.amountByStatus[status] || 0;
          if (amount > topAmount) {
            topAmount = amount;
            topStatus = status;
          }
        });
        
        if (topStatus) {
          this.topStructures.push({
            structure: this.translationService.translate(this.getConventionEtatLabel(topStatus)),
            count: this.conventionStats.statusDistribution?.find((s: any) => s.name === topStatus)?.count || 0
          });
          
          this.generatedTopPartner = {
            structure: this.translationService.translate(this.getConventionEtatLabel(topStatus)),
            amount: topAmount
          };
        }
      }
    }
    
    if (this.topStructures.length === 0 && this.conventionStats?.statusDistribution) {
      const sortedStatuses = [...this.conventionStats.statusDistribution]
        .sort((a, b) => b.count - a.count);
      
      if (sortedStatuses.length > 0) {
        const top = sortedStatuses[0];
        this.topStructures.push({
          structure: this.translationService.translate(this.getConventionEtatLabel(top.name)),
          count: top.count
        });
        
        this.generatedTopPartner = {
          structure: this.translationService.translate(this.getConventionEtatLabel(top.name)),
          count: top.count
        };
      }
    }
    
    console.log('Generated top structures:', this.topStructures);
  }

  getTopStructures(): any[] {
    if (this.conventionStats?.topStructures && this.conventionStats.topStructures.length > 0) {
      return this.conventionStats.topStructures;
    }
    return this.topStructures;
  }

  getTopPartnerDisplay(): string {
    if (this.generatedTopPartner?.structure) {
      return this.generatedTopPartner.structure;
    }
    if (this.topStructures.length > 0) {
      return this.topStructures[0].structure || '';
    }
    return this.translationService.translate('Aucun partenaire');
  }

  getPriorityLabel(days: number): string {
    if (days > 30) return 'Critique';
    if (days > 15) return 'Élevée';
    return 'Moyen';
  }

  getPriorityClass(days: number): string {
    if (days > 30) return 'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-200';
    if (days > 15) return 'bg-orange-100 dark:bg-orange-900/30 text-orange-700 dark:text-orange-200';
    return 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-200';
  }

  getProgressWidth(value: number, max: number): number {
    if (!max || max === 0) return 0;
    return Math.min(100, (value / max) * 100);
  }

  formatNumber(value: number): string {
    return new Intl.NumberFormat('fr-FR').format(value);
  }

  getDaysAgo(date: string): number {
    const today = new Date();
    const dueDate = new Date(date);
    const diffTime = Math.abs(today.getTime() - dueDate.getTime());
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  }

  refreshStats(): void {
    this.loadCommercialStats();
  }

  ngOnDestroy(): void {
    this.destroyCharts();
  }

  getConventionEtatLabel(etat: string): string {
    switch (etat) {
      case 'PLANIFIE': return 'Planifié';
      case 'EN COURS': return 'En Cours';
      case 'TERMINE': return 'Terminé';
      case 'ARCHIVE': return 'Archivée';
      default: return etat;
    }
  }

  getFactureStatutLabel(statut: string): string {
    switch (statut) {
      case 'PAYE': return 'Payée';
      case 'NON_PAYE': return 'Non Payée';
      case 'EN_RETARD': return 'En Retard';
      default: return statut;
    }
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'TND',
      minimumFractionDigits: 2
    }).format(value);
  }

  renderCharts(): void {
    if (this.monthlyTrendsChartRef) {
      const conventionMonthly = this.conventionStats?.monthlyConventions || {};
      const revenueMonthly = this.financialStats?.revenueByMonth || {};

      const monthOrder: { [key: string]: number } = {
        'janv': 0, 'jan': 0, 'janvier': 0,
        'févr': 1, 'fév': 1, 'février': 1, 'fev': 1,
        'mars': 2, 'mar': 2,
        'avr': 3, 'avril': 3,
        'mai': 4, 'may': 4,
        'juin': 5,
        'juil': 6, 'juillet': 6,
        'août': 7, 'aoû': 7, 'aout': 7,
        'sept': 8, 'sep': 8, 'septembre': 8,
        'oct': 9, 'octobre': 9,
        'nov': 10, 'novembre': 10,
        'déc': 11, 'dec': 11, 'décembre': 11, 'decembre': 11
      };
      
      const allMonths = Array.from(new Set([...Object.keys(conventionMonthly), ...Object.keys(revenueMonthly)]));
      
      const getMonthIndex = (monthStr: string): number => {
        const lowerMonth = monthStr.toLowerCase().trim();
        for (const [key, index] of Object.entries(monthOrder)) {
          if (lowerMonth.includes(key)) {
            return index;
          }
        }
        return -1;
      };
      
      const getYear = (monthStr: string): number => {
        const yearMatch = monthStr.match(/\d{4}/);
        return yearMatch ? parseInt(yearMatch[0]) : 0;
      };
      
      const sortedMonths = [...allMonths].sort((a, b) => {
        const yearA = getYear(a);
        const yearB = getYear(b);
        
        if (yearA !== yearB) {
          return yearA - yearB;
        }
        
        const monthA = getMonthIndex(a);
        const monthB = getMonthIndex(b);
        
        if (monthA !== -1 && monthB !== -1) {
          return monthA - monthB;
        }
        
        return a.localeCompare(b);
      });

      const displayMonths = sortedMonths.map(month => {
        const lowerMonth = month.toLowerCase();
        if (lowerMonth.includes('janv')) return 'Jan';
        if (lowerMonth.includes('févr')) return 'Fév';
        if (lowerMonth.includes('mars')) return 'Mar';
        if (lowerMonth.includes('avr')) return 'Avr';
        if (lowerMonth.includes('mai')) return 'Mai';
        if (lowerMonth.includes('juin')) return 'Juin';
        if (lowerMonth.includes('juil')) return 'Juil';
        if (lowerMonth.includes('août') || lowerMonth.includes('aoû')) return 'Aoû';
        if (lowerMonth.includes('sept')) return 'Sep';
        if (lowerMonth.includes('oct')) return 'Oct';
        if (lowerMonth.includes('nov')) return 'Nov';
        if (lowerMonth.includes('déc')) return 'Déc';
        return month.substring(0, 3);
      });
      
      const conventionData = sortedMonths.map((month: string) => Number(conventionMonthly[month] || 0));
      const revenueData = sortedMonths.map((month: string) => Number(revenueMonthly[month] || 0));

      if (this.monthlyTrendsChart) {
        this.monthlyTrendsChart.destroy();
      }

      this.monthlyTrendsChart = this.chartService.createChart(
        this.monthlyTrendsChartRef.nativeElement,
        'bar',
        {
          labels: displayMonths,
          datasets: [
            {
              label: this.translationService.translate('Conventions'),
              data: conventionData,
              backgroundColor: '#3b82f6',
              borderRadius: 4,
              barPercentage: 0.6,
              categoryPercentage: 0.7,
              borderWidth: 0
            },
            {
              label: this.translationService.translate('Revenus'),
              data: revenueData,
              backgroundColor: '#76b7e8',
              borderRadius: 4,
              barPercentage: 0.6,
              categoryPercentage: 0.7,
              borderWidth: 0
            }
          ]
        },
        {
          responsive: true,
          maintainAspectRatio: false,
          scales: {
            x: {
              grid: { display: false },
              ticks: { 
                display: true,
                font: { size: 8 },
                maxRotation: 45,
                minRotation: 45
              },
              border: { display: false }
            },
            y: {
              beginAtZero: true,
              grid: { 
                color: '#f0f0f0',
                drawBorder: false,
                lineWidth: 0.5
              },
              ticks: { 
                stepSize: Math.max(1, Math.ceil(Math.max(...conventionData, ...revenueData, 1) / 4)),
                font: { size: 8 },
                callback: (value: any) => {
                  const maxRevenue = Math.max(...revenueData, 0);
                  if (maxRevenue > 1000) {
                    return value >= 1000 ? (value / 1000).toFixed(0) + 'k' : value;
                  }
                  return value;
                }
              }
            }
          },
          plugins: {
            legend: { display: false },
            tooltip: {
              titleFont: { size: 10 },
              bodyFont: { size: 9 },
              callbacks: {
                label: (context: any) => {
                  const value = context.raw;
                  const label = context.dataset.label || '';
                  if (label === this.translationService.translate('Revenus')) {
                    return `${label}: ${this.formatCurrency(value)}`;
                  }
                  return `${label}: ${value}`;
                }
              }
            }
          }
        }
      );
    }
  }

  formatCurrencyShort(value: number): string {
    if (value >= 1000) {
      return (value / 1000).toFixed(0) + 'k';
    }
    return value.toString();
  }
}