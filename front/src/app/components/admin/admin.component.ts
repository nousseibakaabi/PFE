// src/app/components/admin/admin.component.ts
import { Component, OnInit, ViewChild, ElementRef, AfterViewInit, OnDestroy } from '@angular/core';
import { StatsService } from '../../services/stats.service';
import { ChartService } from '../../services/chart.service';

@Component({
  selector: 'app-admin',
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.css']
})
export class AdminComponent implements OnInit, AfterViewInit, OnDestroy {
  // ViewChild references for charts
  @ViewChild('conventionStatusChart') conventionStatusChartRef!: ElementRef;
  @ViewChild('paymentStatusChart') paymentStatusChartRef!: ElementRef;
  @ViewChild('userRoleChart') userRoleChartRef!: ElementRef;
  @ViewChild('structureTypeChart') structureTypeChartRef!: ElementRef;
  @ViewChild('revenueTrendChart') revenueTrendChartRef!: ElementRef;
  
  // Chart instances (using any to avoid TypeScript issues)
  conventionStatusChart: any = null;
  paymentStatusChart: any = null;
  userRoleChart: any = null;
  structureTypeChart: any = null;
  revenueTrendChart: any = null;

  // Dashboard stats
  dashboardStats: any = {};
  conventionStats: any = {};
  factureStats: any = {};
  userStats: any = {};
  nomenclatureStats: any = {};
  financialStats: any = {};
  summaryStats: any = {};
  
  // Loading state
  isLoading = true;

  constructor(
    private statsService: StatsService,
    private chartService: ChartService
  ) {}

  ngOnInit(): void {
    this.loadAdminStats();
  }

  ngAfterViewInit(): void {
    // Small delay to ensure view is fully initialized
    setTimeout(() => this.renderCharts(), 100);
  }

  loadAdminStats(): void {
    this.isLoading = true;
    
    this.statsService.getAdminStats().then((results: any[]) => {
      // Process all stats results
      if (results[0]?.success) this.dashboardStats = results[0].data;
      if (results[1]?.success) this.conventionStats = results[1].data;
      if (results[2]?.success) this.factureStats = results[2].data;
      if (results[3]?.success) this.userStats = results[3].data;
      if (results[4]?.success) this.nomenclatureStats = results[4].data;
      if (results[5]?.success) this.financialStats = results[5].data;
      if (results[6]?.success) this.summaryStats = results[6].data;
      
      this.isLoading = false;
      
      // Re-render charts with new data
      setTimeout(() => {
        this.destroyCharts();
        this.renderCharts();
      }, 100);
      
    }).catch(error => {
      console.error('Error loading admin stats:', error);
      this.isLoading = false;
    });
  }

  renderCharts(): void {
    // 1. Convention Status Chart (Pie)
    if (this.conventionStatusChartRef && this.conventionStats.statusDistribution) {
      const labels = this.conventionStats.statusDistribution.map((s: any) => s.name);
      const data = this.conventionStats.statusDistribution.map((s: any) => s.count);
      
      this.conventionStatusChart = this.chartService.createChart(
        this.conventionStatusChartRef.nativeElement,
        'pie',
        {
          labels: labels,
          datasets: [{
            data: data,
            backgroundColor: ['#4CAF50', '#2196F3', '#FF9800', '#9C27B0', '#F44336']
          }]
        },
        {
          plugins: {
            legend: {
              position: 'bottom'
            }
          }
        }
      );
    }

    // 2. Payment Status Chart (Bar)
    if (this.paymentStatusChartRef && this.factureStats.paymentStatus) {
      const labels = this.factureStats.paymentStatus.map((p: any) => p.status);
      const data = this.factureStats.paymentStatus.map((p: any) => p.count);
      
      this.paymentStatusChart = this.chartService.createChart(
        this.paymentStatusChartRef.nativeElement,
        'bar',
        {
          labels: labels,
          datasets: [{
            label: 'Nombre de Factures',
            data: data,
            backgroundColor: ['#4CAF50', '#FF9800', '#F44336']
          }]
        }
      );
    }

    // 3. User Role Chart (Doughnut)
    if (this.userRoleChartRef && this.userStats.roleDistribution) {
      const labels = Object.keys(this.userStats.roleDistribution);
      const data = Object.values(this.userStats.roleDistribution) as number[];
      
      this.userRoleChart = this.chartService.createChart(
        this.userRoleChartRef.nativeElement,
        'doughnut',
        {
          labels: labels,
          datasets: [{
            data: data,
            backgroundColor: ['#4CAF50', '#2196F3', '#FF9800', '#9C27B0']
          }]
        },
        {
          plugins: {
            legend: {
              position: 'bottom'
            }
          }
        }
      );
    }

    // 4. Structure Type Chart (Bar)
    if (this.structureTypeChartRef && this.nomenclatureStats.structuresByType) {
      const labels = Object.keys(this.nomenclatureStats.structuresByType);
      const data = Object.values(this.nomenclatureStats.structuresByType) as number[];
      
      this.structureTypeChart = this.chartService.createChart(
        this.structureTypeChartRef.nativeElement,
        'bar',
        {
          labels: labels,
          datasets: [{
            label: 'Nombre de Structures',
            data: data,
            backgroundColor: '#2196F3'
          }]
        }
      );
    }

    // 5. Revenue Trend Chart (Line)
    if (this.revenueTrendChartRef && this.dashboardStats.monthlyTrends?.revenueTrends) {
      const labels = Object.keys(this.dashboardStats.monthlyTrends.revenueTrends).reverse();
      const data = Object.values(this.dashboardStats.monthlyTrends.revenueTrends).reverse() as number[];
      
      this.revenueTrendChart = this.chartService.createChart(
        this.revenueTrendChartRef.nativeElement,
        'line',
        {
          labels: labels,
          datasets: [{
            label: 'Revenus (€)',
            data: data,
            backgroundColor: 'rgba(33, 150, 243, 0.2)',
            borderColor: '#2196F3',
            borderWidth: 2,
            tension: 0.4,
            fill: true
          }]
        }
      );
    }
  }

  destroyCharts(): void {
    this.chartService.destroyChart(this.conventionStatusChart);
    this.chartService.destroyChart(this.paymentStatusChart);
    this.chartService.destroyChart(this.userRoleChart);
    this.chartService.destroyChart(this.structureTypeChart);
    this.chartService.destroyChart(this.revenueTrendChart);
    
    // Reset chart variables
    this.conventionStatusChart = null;
    this.paymentStatusChart = null;
    this.userRoleChart = null;
    this.structureTypeChart = null;
    this.revenueTrendChart = null;
  }

  refreshStats(): void {
    this.loadAdminStats();
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR',
      minimumFractionDigits: 2
    }).format(value);
  }

  formatNumber(value: number): string {
    return new Intl.NumberFormat('fr-FR').format(value);
  }

  getPercentage(value: number): string {
    return value.toFixed(1) + '%';
  }

  ngOnDestroy(): void {
    this.destroyCharts();
  }
}