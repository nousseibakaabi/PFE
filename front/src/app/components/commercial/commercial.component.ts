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
  
  // Loading state
  isLoading = true;

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

  loadCommercialStats(): void {
    this.isLoading = true;
    
    this.statsService.getCommercialStats().then((results: any[]) => {
      if (results[0]?.success) this.conventionStats = results[0].data;
      if (results[1]?.success) this.factureStats = results[1].data;
      if (results[2]?.success) this.summaryStats = results[2].data;
      
      this.isLoading = false;
      
      setTimeout(() => {
        this.destroyCharts();
        this.renderCharts();
      }, 100);
      
    }).catch(error => {
      console.error('Error loading commercial stats:', error);
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

    // 2. Monthly Conventions Chart (Bar)
    if (this.monthlyConventionsChartRef && this.conventionStats.monthlyConventions) {
      const labels = Object.keys(this.conventionStats.monthlyConventions).reverse();
      const data = Object.values(this.conventionStats.monthlyConventions).reverse() as number[];
      
      this.monthlyConventionsChart = this.chartService.createChart(
        this.monthlyConventionsChartRef.nativeElement,
        'bar',
        {
          labels: labels,
          datasets: [{
            label: 'Nouvelles Conventions',
            data: data,
            backgroundColor: '#2196F3',
            borderColor: '#1976D2',
            borderWidth: 1
          }]
        }
      );
    }

    // 3. Payment Status Chart (Bar)
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

    // 4. Payment Method Chart (Doughnut)
    if (this.paymentMethodChartRef && this.factureStats.paymentMethods) {
      const labels = Object.keys(this.factureStats.paymentMethods);
      const data = Object.values(this.factureStats.paymentMethods) as number[];
      
      this.paymentMethodChart = this.chartService.createChart(
        this.paymentMethodChartRef.nativeElement,
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

    // 5. Revenue by Month Chart (Bar - grouped)
    if (this.revenueByMonthChartRef && this.factureStats.monthlyAmounts) {
      const labels = Object.keys(this.factureStats.monthlyAmounts);
      const totalData = Object.values(this.factureStats.monthlyAmounts).map((m: any) => m.total);
      const paidData = Object.values(this.factureStats.monthlyAmounts).map((m: any) => m.paid);
      
      this.revenueByMonthChart = this.chartService.createChart(
        this.revenueByMonthChartRef.nativeElement,
        'bar',
        {
          labels: labels,
          datasets: [
            {
              label: 'Montant Total',
              data: totalData,
              backgroundColor: '#2196F3'
            },
            {
              label: 'Montant Payé',
              data: paidData,
              backgroundColor: '#4CAF50'
            }
          ]
        }
      );
    }

    // 6. Structure Type Chart (Bar)
    if (this.structureTypeChartRef && this.conventionStats.byStructureType) {
      const labels = Object.keys(this.conventionStats.byStructureType);
      const data = Object.values(this.conventionStats.byStructureType) as number[];
      
      this.structureTypeChart = this.chartService.createChart(
        this.structureTypeChartRef.nativeElement,
        'bar',
        {
          labels: labels,
          datasets: [{
            label: 'Conventions par Type',
            data: data,
            backgroundColor: '#FF9800'
          }]
        }
      );
    }
  }

  destroyCharts(): void {
    this.chartService.destroyChart(this.conventionStatusChart);
    this.chartService.destroyChart(this.monthlyConventionsChart);
    this.chartService.destroyChart(this.paymentStatusChart);
    this.chartService.destroyChart(this.paymentMethodChart);
    this.chartService.destroyChart(this.revenueByMonthChart);
    this.chartService.destroyChart(this.structureTypeChart);
    
    // Reset chart variables
    this.conventionStatusChart = null;
    this.monthlyConventionsChart = null;
    this.paymentStatusChart = null;
    this.paymentMethodChart = null;
    this.revenueByMonthChart = null;
    this.structureTypeChart = null;
  }

  getOverdueInvoices(): any[] {
    return this.factureStats.overdueDetails || [];
  }

  getTopStructures(): any[] {
    return this.conventionStats.topStructures || [];
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
}