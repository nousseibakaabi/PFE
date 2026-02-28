// src/app/components/decideur/decideur.component.ts
import { Component, OnInit, ViewChild, ElementRef, AfterViewInit, OnDestroy } from '@angular/core';
import { StatsService } from '../../services/stats.service';
import { ChartService } from '../../services/chart.service';

@Component({
  selector: 'app-decideur',
  templateUrl: './decideur.component.html',
  styleUrls: ['./decideur.component.css']
})
export class DecideurComponent implements OnInit, AfterViewInit, OnDestroy {
  // ViewChild references for charts
  @ViewChild('conventionStatusChart') conventionStatusChartRef!: ElementRef;
  @ViewChild('paymentStatusChart') paymentStatusChartRef!: ElementRef;
  @ViewChild('applicationStatusChart') applicationStatusChartRef!: ElementRef;
  @ViewChild('revenueTrendChart') revenueTrendChartRef!: ElementRef;
  
  // Chart instances
  conventionStatusChart: any = null;
  paymentStatusChart: any = null;
  applicationStatusChart: any = null;
  revenueTrendChart: any = null;

  // Stats Data
  dashboardStats: any = {};
  conventionStats: any = {};
  factureStats: any = {};
  applicationStats: any = {};
  financialStats: any = {};
  summaryStats: any = {};
  overdueAlerts: any[] = [];

  // Loading state
  isLoading = true;

  constructor(
    private statsService: StatsService,
    private chartService: ChartService
  ) {}

  ngOnInit(): void {
    this.loadDecideurStats();
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.renderCharts(), 100);
  }

  loadDecideurStats(): void {
    this.isLoading = true;
    
    this.statsService.getDecideurStats().then((results: any[]) => {
      if (results[0]?.success) this.dashboardStats = results[0].data;
      if (results[1]?.success) this.conventionStats = results[1].data;
      if (results[2]?.success) this.factureStats = results[2].data;
      if (results[3]?.success) this.applicationStats = results[3].data;
      if (results[4]?.success) this.financialStats = results[4].data;
      if (results[5]?.success) this.summaryStats = results[5].data;
      if (results[6]?.success) this.overdueAlerts = results[6].data;
      
      this.isLoading = false;
      
      setTimeout(() => {
        this.destroyCharts();
        this.renderCharts();
      }, 100);
    }).catch(error => {
      console.error('Error loading decideur stats:', error);
      this.isLoading = false;
    });
  }

  renderCharts(): void {
    // Convention Status Chart
    if (this.conventionStatusChartRef && this.conventionStats?.statusDistribution) {
      const labels = this.conventionStats.statusDistribution.map((s: any) => this.getConventionEtatLabel(s.name));
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
              position: 'bottom'
            }
          }
        }
      );
    }

    // Payment Status Chart
    if (this.paymentStatusChartRef && this.factureStats?.paymentStatus) {
      const labels = this.factureStats.paymentStatus.map((p: any) => this.getFactureStatutLabel(p.status));
      const data = this.factureStats.paymentStatus.map((p: any) => p.count);
      
      this.paymentStatusChart = this.chartService.createChart(
        this.paymentStatusChartRef.nativeElement,
        'doughnut',
        {
          labels: labels,
          datasets: [{
            data: data,
            backgroundColor: ['#4CAF50', '#FF9800', '#F44336']
          }]
        },
        {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              position: 'bottom'
            }
          }
        }
      );
    }

    // Application Status Chart
    if (this.applicationStatusChartRef && this.applicationStats?.statusDistribution) {
      const labels = this.applicationStats.statusDistribution.map((s: any) => s.name);
      const data = this.applicationStats.statusDistribution.map((s: any) => s.count);
      
      this.applicationStatusChart = this.chartService.createChart(
        this.applicationStatusChartRef.nativeElement,
        'bar',
        {
          labels: labels,
          datasets: [{
            label: 'Applications',
            data: data,
            backgroundColor: ['#4CAF50', '#2196F3', '#FF9800', '#9C27B0']
          }]
        },
        {
          responsive: true,
          maintainAspectRatio: false,
          scales: {
            y: {
              beginAtZero: true,
              ticks: {
                precision: 0
              }
            }
          }
        }
      );
    }

    // Revenue Trend Chart
    if (this.revenueTrendChartRef && this.financialStats?.revenueByMonth) {
      const labels = Object.keys(this.financialStats.revenueByMonth);
      const data = Object.values(this.financialStats.revenueByMonth) as number[];
      
      this.revenueTrendChart = this.chartService.createChart(
        this.revenueTrendChartRef.nativeElement,
        'line',
        {
          labels: labels,
          datasets: [{
            label: 'Revenus',
            data: data,
            backgroundColor: 'rgba(16, 185, 129, 0.2)',
            borderColor: '#10B981',
            borderWidth: 2,
            tension: 0.4,
            fill: true
          }]
        },
        {
          responsive: true,
          maintainAspectRatio: false,
          scales: {
            y: {
              beginAtZero: true,
              ticks: {
                callback: (value: any) => {
                  if (value >= 1000) {
                    return (value / 1000) + 'k';
                  }
                  return value;
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
    this.chartService.destroyChart(this.paymentStatusChart);
    this.chartService.destroyChart(this.applicationStatusChart);
    this.chartService.destroyChart(this.revenueTrendChart);
    
    this.conventionStatusChart = null;
    this.paymentStatusChart = null;
    this.applicationStatusChart = null;
    this.revenueTrendChart = null;
  }

  refreshStats(): void {
    this.loadDecideurStats();
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'TND',
      minimumFractionDigits: 2
    }).format(value);
  }

  getConventionEtatLabel(etat: string): string {
    switch (etat) {
      case 'PLANIFIE': return 'Planifié';
      case 'EN COURS': return 'En Cours';
      case 'TERMINE': return 'Terminé';
      case 'ARCHIVE': return 'Archivé';
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

  ngOnDestroy(): void {
    this.destroyCharts();
  }
}