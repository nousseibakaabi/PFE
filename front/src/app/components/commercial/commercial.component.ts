// src/app/components/commercial/commercial.component.ts
import { Component, OnInit, ViewChild, ElementRef, AfterViewInit, OnDestroy } from '@angular/core';
import { StatsService } from '../../services/stats.service';
import { ChartService } from '../../services/chart.service';

@Component({
  selector: 'app-commercial',
  templateUrl: './commercial.component.html',
  styleUrls: ['./commercial.component.css']
})
export class CommercialComponent implements OnInit, AfterViewInit, OnDestroy {
  // ViewChild references for charts
  @ViewChild('conventionStatusChart') conventionStatusChartRef!: ElementRef;
  @ViewChild('monthlyConventionsChart') monthlyConventionsChartRef!: ElementRef;
  @ViewChild('paymentStatusChart') paymentStatusChartRef!: ElementRef;
  @ViewChild('paymentMethodChart') paymentMethodChartRef!: ElementRef;
  @ViewChild('revenueByMonthChart') revenueByMonthChartRef!: ElementRef;
  @ViewChild('structureTypeChart') structureTypeChartRef!: ElementRef;
  @ViewChild('structureCountsChart') structureCountsChartRef!: ElementRef;
structureCountsChart: any = null;
  
  // Chart instances
  conventionStatusChart: any = null;
  monthlyConventionsChart: any = null;
  paymentStatusChart: any = null;
  paymentMethodChart: any = null;
  revenueByMonthChart: any = null;
  structureTypeChart: any = null;

  // Stats Data
  conventionStats: any = {};
  factureStats: any = {};
  summaryStats: any = {};

  

currentPage: number = 1;
itemsPerPage: number = 10;
totalItems: number = 0;

// Filter properties
priorityFilter: string = 'ALL'; // 'ALL', 'CRITIQUE', 'ÉLEVÉE', 'MOYEN'
sortBy: string = 'joursRetard'; // 'joursRetard', 'montant', 'dateEcheance'
sortDirection: string = 'desc'; // 'asc' or 'desc'

// Filtered and paginated data
filteredOverdueInvoices: any[] = [];
  dashboardStats: any = {}; 
financialStats: any = {};
overdueAlerts: any[] = [];
  
  // Loading state
  isLoading = true;


  // Add this with your other properties
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
    private chartService: ChartService
  ) {}

  ngOnInit(): void {
    this.loadCommercialStats();
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.renderCharts(), 100);
  }



  updateFilteredData(): void {
  let invoices = this.getOverdueInvoices();
  
  // Apply priority filter
  if (this.priorityFilter !== 'ALL') {
    invoices = invoices.filter(invoice => {
      const daysAgo = this.getDaysAgo(invoice.dateEcheance);
      const priority = this.getPriorityLabel(daysAgo);
      return priority === this.priorityFilter;
    });
  }
  
  // Apply sorting
  invoices = this.sortInvoices(invoices, this.sortBy, this.sortDirection);
  
  // Update totals
  this.totalItems = invoices.length;
  
  // Apply pagination
  const startIndex = (this.currentPage - 1) * this.itemsPerPage;
  this.filteredOverdueInvoices = invoices.slice(startIndex, startIndex + this.itemsPerPage);
}

// Sort invoices
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

// Get priority label from days
getPriorityLabel(days: number): string {
  if (days > 30) return 'CRITIQUE';
  if (days > 15) return 'ÉLEVÉE';
  return 'MOYEN';
}

// Change page
changePage(page: number): void {
  if (page < 1 || page > this.getTotalPages()) return;
  this.currentPage = page;
  this.updateFilteredData();
}

// Get total pages
getTotalPages(): number {
  return Math.ceil(this.totalItems / this.itemsPerPage);
}

// Change items per page
changeItemsPerPage(count: number): void {
  this.itemsPerPage = count;
  this.currentPage = 1;
  this.updateFilteredData();
}

// Reset filters
resetFilters(): void {
  this.priorityFilter = 'ALL';
  this.sortBy = 'joursRetard';
  this.sortDirection = 'desc';
  this.currentPage = 1;
  this.updateFilteredData();
}
 

  calculateQuickStats(): void {
  // Reset quick stats
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
  
  // From summary stats
  if (this.summaryStats) {
    this.quickStats.totalConventions = this.summaryStats.totalConventions || 0;
    this.quickStats.totalFactures = this.summaryStats.totalFactures || 0;
    this.quickStats.conventionsToday = this.summaryStats.conventionsToday || 0;
    this.quickStats.facturesToday = this.summaryStats.facturesToday || 0;
    this.quickStats.dueToday = this.summaryStats.dueToday || 0;
    this.quickStats.overdueToday = this.summaryStats.overdueToday || 0;
  }
  
  // From convention stats (calculate active from status distribution)
  if (this.conventionStats?.statusDistribution) {
    const activeStatuses = ['EN_COURS', 'EN_ATTENTE'];
    this.quickStats.activeConventions = this.conventionStats.statusDistribution
      .filter((status: any) => activeStatuses.includes(status.name))
      .reduce((sum: number, status: any) => sum + (status.count || 0), 0);
  }
  
  // From facture stats
  if (this.factureStats?.paymentStatus) {
    const paidStatus = this.factureStats.paymentStatus.find((p: any) => p.status === 'PAYE');
    this.quickStats.paidFactures = paidStatus?.count || 0;
  }
  
  // From financial stats
  if (this.financialStats) {
    this.quickStats.totalRevenue = this.financialStats.totalRevenue || 0;
    this.quickStats.pendingRevenue = this.financialStats.pendingPayments || 0;
    this.quickStats.overdueAmount = this.financialStats.overdueAmount || 0;
    this.quickStats.collectionRate = this.financialStats.collectionRate || 0;
  }
}

  // In commercial.component.ts - CORRECTED loadCommercialStats method:
loadCommercialStats(): void {
  this.isLoading = true;
  
  // Load ONLY convention and facture stats for commercial user
  Promise.all([
    this.statsService.getConventionDetailedStats().toPromise(),
    this.statsService.getFactureDetailedStats().toPromise(),
    this.statsService.getFinancialDetailedStats().toPromise(),
    this.statsService.getSummaryStats().toPromise(),
    this.statsService.getOverdueAlerts().toPromise()
  ]).then((results: any[]) => {
    console.log('Commercial stats loaded (only conventions/factures):', results);
    
    // Convention detailed stats
    if (results[0]?.success) {
      this.conventionStats = results[0].data;
    }
    
    // Facture detailed stats
    if (results[1]?.success) {
      this.factureStats = results[1].data;
    }
    
    // Financial detailed stats (based on their factures)
    if (results[2]?.success) {
      this.financialStats = results[2].data;
    }
    
    // Summary stats (filtered to their data)
    if (results[3]?.success) {
      this.summaryStats = results[3].data;
    }
    
    // Overdue alerts (only for their factures)
    if (results[4]?.success) {
      this.overdueAlerts = results[4].data;
    }
    
    this.isLoading = false;
    
     this.updateFilteredData();

    // Calculate quick stats from loaded data
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



// Add this method to your CommercialComponent class
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
    // Show all pages
    for (let i = 1; i <= totalPages; i++) {
      pages.push(i);
    }
  } else {
    // Show first page, last page, and pages around current
    if (current <= 4) {
      // Near the beginning
      for (let i = 1; i <= 5; i++) pages.push(i);
      pages.push(-1); // Use -1 for ellipsis
      pages.push(totalPages);
    } else if (current >= totalPages - 3) {
      // Near the end
      pages.push(1);
      pages.push(-1); // Use -1 for ellipsis
      for (let i = totalPages - 4; i <= totalPages; i++) pages.push(i);
    } else {
      // In the middle
      pages.push(1);
      pages.push(-1); // Use -1 for ellipsis
      pages.push(current - 1);
      pages.push(current);
      pages.push(current + 1);
      pages.push(-1); // Use -1 for ellipsis
      pages.push(totalPages);
    }
  }
  
  return pages;
}

// Add priority class helper
getPriorityClass(days: number): string {
  if (days > 30) return 'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300';
  if (days > 15) return 'bg-orange-100 dark:bg-orange-900/30 text-orange-700 dark:text-orange-300';
  return 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-300';
}

renderCharts(): void {
  // 1. Convention Status Chart (Pie) - SMALLER
  if (this.conventionStatusChartRef && this.conventionStats?.statusDistribution) {
    const labels = this.conventionStats.statusDistribution.map((s: any) => 
      this.getConventionEtatLabel(s.name)
    );
    const data = this.conventionStats.statusDistribution.map((s: any) => s.count);
    
    this.conventionStatusChart = this.chartService.createChart(
      this.conventionStatusChartRef.nativeElement,
      'pie',
      {
        labels: labels,
        datasets: [{
          data: data,
          backgroundColor: ['#4CAF50', '#2196F3', '#FF9800', '#9C27B0', '#F44336', '#607D8B']
        }]
      },
      {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              font: {
                size: 10 // Smaller font for legend
              },
              padding: 8
            }
          }
        }
      }
    );
  }

  // 2. Monthly Conventions Chart (Bar) - SMALLER
  if (this.monthlyConventionsChartRef && this.conventionStats?.monthlyConventions) {
    const labels = Object.keys(this.conventionStats.monthlyConventions).reverse();
    const data = Object.values(this.conventionStats.monthlyConventions).reverse() as number[];
    
    this.monthlyConventionsChart = this.chartService.createChart(
      this.monthlyConventionsChartRef.nativeElement,
      'bar',
      {
        labels: labels,
        datasets: [{
          label: 'Conventions',
          data: data,
          backgroundColor: '#2196F3',
          borderColor: '#1976D2',
          borderWidth: 1,
          barPercentage: 0.6,
          categoryPercentage: 0.7
        }]
      },
      {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          x: {
            ticks: {
              font: {
                size: 9 // Smaller x-axis labels
              }
            },
            grid: {
              display: false
            }
          },
          y: {
            beginAtZero: true,
            ticks: {
              font: {
                size: 9 // Smaller y-axis labels
              },
              precision: 0
            },
            grid: {
              color: 'rgba(0,0,0,0.05)'
            }
          }
        },
        plugins: {
          legend: {
            display: false
          }
        }
      }
    );
  }

  // 3. Payment Status Chart (Bar) - SMALLER
  if (this.paymentStatusChartRef && this.factureStats?.paymentStatus) {
    const labels = this.factureStats.paymentStatus.map((p: any) => 
      this.getFactureStatutLabel(p.status)
    );
    const data = this.factureStats.paymentStatus.map((p: any) => p.count);
    
    this.paymentStatusChart = this.chartService.createChart(
      this.paymentStatusChartRef.nativeElement,
      'bar',
      {
        labels: labels,
        datasets: [{
          label: 'Factures',
          data: data,
          backgroundColor: ['#4CAF50', '#FF9800', '#F44336'],
          borderColor: ['#388E3C', '#F57C00', '#D32F2F'],
          borderWidth: 1,
          barPercentage: 0.6,
          categoryPercentage: 0.7
        }]
      },
      {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          x: {
            ticks: {
              font: {
                size: 9 // Smaller x-axis labels
              }
            },
            grid: {
              display: false
            }
          },
          y: {
            beginAtZero: true,
            ticks: {
              font: {
                size: 9 // Smaller y-axis labels
              },
              precision: 0
            },
            grid: {
              color: 'rgba(0,0,0,0.05)'
            }
          }
        },
        plugins: {
          legend: {
            display: false
          }
        }
      }
    );
  }

  // 4. Revenue by Month Chart (Bar) - SMALLER
  if (this.revenueByMonthChartRef && this.financialStats?.revenueByMonth) {
    const labels = Object.keys(this.financialStats.revenueByMonth);
    const revenueData = Object.values(this.financialStats.revenueByMonth) as number[];
    
    this.revenueByMonthChart = this.chartService.createChart(
      this.revenueByMonthChartRef.nativeElement,
      'bar',
      {
        labels: labels,
        datasets: [{
          label: 'Revenus',
          data: revenueData,
          backgroundColor: '#4CAF50',
          borderColor: '#388E3C',
          borderWidth: 1,
          barPercentage: 0.6,
          categoryPercentage: 0.7
        }]
      },
      {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          x: {
            ticks: {
              font: {
                size: 9 // Smaller x-axis labels
              }
            },
            grid: {
              display: false
            }
          },
          y: {
            beginAtZero: true,
            ticks: {
              font: {
                size: 9 // Smaller y-axis labels
              },
              callback: (value: any) => {
                if (value >= 1000) {
                  return (value / 1000) + 'k';
                }
                return value;
              }
            },
            grid: {
              color: 'rgba(0,0,0,0.05)'
            }
          }
        },
        plugins: {
          legend: {
            display: false
          },
          tooltip: {
            callbacks: {
              label: (context: any) => {
                return `Revenu: ${this.formatCurrency(context.raw)}`;
              }
            }
          }
        }
      }
    );
  }
}


  



destroyCharts(): void {
  this.chartService.destroyChart(this.conventionStatusChart);
  this.chartService.destroyChart(this.monthlyConventionsChart);
  this.chartService.destroyChart(this.paymentStatusChart);
  this.chartService.destroyChart(this.revenueByMonthChart);
  
  
  
  // Reset chart variables
  this.conventionStatusChart = null;
  this.monthlyConventionsChart = null;
  this.paymentStatusChart = null;
  this.revenueByMonthChart = null;
}




getOverdueInvoices(): any[] {
  return this.factureStats?.overdueDetails || [];
}

getTopStructures(): any[] {
  return this.conventionStats?.topStructures || [];
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
    case 'NO_STATUS': return 'Pas de Statut';
    case 'EN_ATTENTE': return 'En Attente';
    case 'EN_COURS': return 'En Cours';
    case 'EN_RETARD': return 'En Retard';
    case 'TERMINE': return 'Terminé';
    case 'ARCHIVE': return 'Archivé';
    case 'RESILIE': return 'Résilié';
    case 'ANNULE': return 'Annulé';
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

// Make sure you have this method for currency formatting
formatCurrency(value: number): string {
  return new Intl.NumberFormat('fr-FR', {
    style: 'currency',
    currency: 'TND', // or 'EUR' depending on your currency
    minimumFractionDigits: 2
  }).format(value);
}


}