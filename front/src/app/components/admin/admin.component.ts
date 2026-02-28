// src/app/components/admin/admin.component.ts
import { Component, OnInit, ViewChild, ElementRef, AfterViewInit, OnDestroy } from '@angular/core';
import { StatsService } from '../../services/stats.service';
import { ChartService } from '../../services/chart.service';
import { WorkloadDTO, WorkloadService } from 'src/app/services/workload.service';

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
  
  // Chart instances
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
  applicationStats: any = {};
  
  // Loading state
  isLoading = true;

  workloads: WorkloadDTO[] = [];
  workloadLoading = false;
  workloadStats = {
    totalChefs: 0,
    overloadedChefs: 0,
    highWorkloadChefs: 0,
    availableChefs: 0,
    averageWorkload: 0
  };

  Math = Math;

  constructor(
    private statsService: StatsService,
    private chartService: ChartService,
    private workloadService: WorkloadService 
  ) {}

  ngOnInit(): void {
    this.loadAdminStats();
    this.loadWorkloadDashboard(); 
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.renderCharts(), 100);
  }

  loadAdminStats(): void {
    this.isLoading = true;
    
    this.statsService.getAdminStats().then((results: any[]) => {
      console.log('Admin stats results:', results); // Debug log
      
      // Process all stats results with proper mapping
      if (results[0]?.success) {
        this.dashboardStats = this.mapDashboardStats(results[0].data);
      }
      if (results[1]?.success) {
        this.conventionStats = results[1].data;
      }
      if (results[2]?.success) {
        this.factureStats = results[2].data;
      }
      if (results[3]?.success) {
        this.userStats = results[3].data;
        console.log('User stats:', this.userStats); // Debug log
      }
      if (results[4]?.success) {
        this.nomenclatureStats = results[4].data;
        console.log('Nomenclature stats:', this.nomenclatureStats); // Debug log
      }
      if (results[5]?.success) {
        this.financialStats = results[5].data;
      }
      if (results[6]?.success) {
        this.summaryStats = results[6].data;
      }
      if (results[7]?.success) {
        this.applicationStats = results[7].data;
        console.log('Application stats:', this.applicationStats); // Debug log
      }
      
      this.isLoading = false;
      
      setTimeout(() => {
        this.destroyCharts();
        this.renderCharts();
      }, 100);
      
    }).catch(error => {
      console.error('Error loading admin stats:', error);
      this.isLoading = false;
    });
  }

  /**
   * Map dashboard stats from backend to expected format
   */
  mapDashboardStats(data: any): any {
    return {
      totalConventions: data.totalConventions || 0,
      activeConventions: data.activeConventions || 0,
      planifiedConventions: data.planifiedConventions || 0,
      terminatedConventions: data.terminatedConventions || 0,
      archivedConventions: data.archivedConventions || 0,
      totalFactures: data.totalFactures || 0,
      paidFactures: data.paidFactures || 0,
      unpaidFactures: data.unpaidFactures || 0,
      overdueFactures: data.overdueFactures || 0,
      totalApplications: data.totalApplications || 0,
      activeApplications: data.activeApplications || 0,
      completedApplications: data.completedApplications || 0,
      plannedApplications: data.plannedApplications || 0,
      averageProgress: data.averageProgress || 0
    };
  }

  renderCharts(): void {
    // 1. Convention Status Chart (Pie)
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
            backgroundColor: ['#4CAF50', '#2196F3', '#FF9800', '#9C27B0', '#F44336']
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

    // 2. Payment Status Chart (Bar)
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
            label: 'Nombre de Factures',
            data: data,
            backgroundColor: ['#4CAF50', '#FF9800', '#F44336']
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

    // 3. User Role Chart (Doughnut)
    if (this.userRoleChartRef && this.userStats?.roleDistribution) {
      const labels = Object.keys(this.userStats.roleDistribution).map(role => 
        this.getRoleLabel(role)
      );
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

    // 4. Structure Type Chart (Bar)
    if (this.structureTypeChartRef && this.nomenclatureStats?.structuresByType) {
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

    // 5. Revenue Trend Chart (Line)
    if (this.revenueTrendChartRef && this.financialStats?.revenueByMonth) {
      const labels = Object.keys(this.financialStats.revenueByMonth);
      const data = Object.values(this.financialStats.revenueByMonth) as number[];
      
      this.revenueTrendChart = this.chartService.createChart(
        this.revenueTrendChartRef.nativeElement,
        'line',
        {
          labels: labels,
          datasets: [{
            label: 'Revenus (TND)',
            data: data,
            backgroundColor: 'rgba(33, 150, 243, 0.2)',
            borderColor: '#2196F3',
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
    this.chartService.destroyChart(this.userRoleChart);
    this.chartService.destroyChart(this.structureTypeChart);
    this.chartService.destroyChart(this.revenueTrendChart);
    
    this.conventionStatusChart = null;
    this.paymentStatusChart = null;
    this.userRoleChart = null;
    this.structureTypeChart = null;
    this.revenueTrendChart = null;
  }

  loadWorkloadDashboard(): void {
    this.workloadLoading = true;
    this.workloadService.getWorkloadDashboard().subscribe({
      next: (response) => {
        console.log('Workload response:', response); // Debug log
        if (response.success && response.data) {
          this.workloads = response.data.workloads || [];
          this.workloadStats = {
            totalChefs: response.data.totalChefs || 0,
            overloadedChefs: response.data.overloadedChefs || 0,
            highWorkloadChefs: response.data.highWorkloadChefs || 0,
            availableChefs: response.data.availableChefs || 0,
            averageWorkload: response.data.averageWorkload || 0
          };
        }
        this.workloadLoading = false;
      },
      error: (error) => {
        console.error('Error loading workload dashboard:', error);
        this.workloadLoading = false;
      }
    });
  }

  getWorkloadColor(workload: number): string {
    if (workload > 75) return '#EF4444';
    if (workload >= 45) return '#F97316';
    return '#10B981';
  }

  getProgressBarClass(workload: number): string {
    if (workload > 75) return 'bg-red-500';
    if (workload >= 45) return 'bg-orange-500';
    return 'bg-green-500';
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

  getRoleLabel(role: string): string {
    switch (role) {
      case 'ROLE_ADMIN': return 'Admin';
      case 'ROLE_CHEF_PROJET': return 'Chef de Projet';
      case 'ROLE_COMMERCIAL_METIER': return 'Commercial';
      case 'ROLE_DECIDEUR': return 'Décideur';
      default: return role;
    }
  }

  refreshStats(): void {
    this.loadAdminStats();
    this.loadWorkloadDashboard();
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'TND',
      minimumFractionDigits: 2
    }).format(value);
  }

  formatNumber(value: number): string {
    return new Intl.NumberFormat('fr-FR').format(value);
  }

  getPercentage(value: number): string {
    return (value || 0).toFixed(1) + '%';
  }

  ngOnDestroy(): void {
    this.destroyCharts();
  }

  getApplicationStatusLabel(status: string): string {
    switch (status) {
      case 'PLANIFIE': return 'Planifié';
      case 'EN_COURS': return 'En Cours';
      case 'TERMINE': return 'Terminé';
      default: return status;
    }
  }

  getProgressClass(progress: number): string {
    if (progress >= 90) return 'bg-green-500';
    if (progress >= 70) return 'bg-blue-500';
    if (progress >= 50) return 'bg-yellow-500';
    if (progress >= 30) return 'bg-orange-500';
    return 'bg-red-500';
  }

  /**
   * Calculate percentage value
   */
  getPercentageValue(value: number, total: number): number {
    if (!total || total === 0) return 0;
    return (value / total) * 100;
  }

  /**
   * Get total applications count from status distribution
   */
  getTotalApplicationsCount(): number {
    if (!this.applicationStats?.statusDistribution) return 0;
    return this.applicationStats.statusDistribution.reduce((sum: number, s: any) => sum + (s.count || 0), 0);
  }

  /**
   * Get total conventions count from status distribution
   */
  getTotalConventionsCount(): number {
    if (!this.conventionStats?.statusDistribution) return 0;
    return this.conventionStats.statusDistribution.reduce((sum: number, s: any) => sum + (s.count || 0), 0);
  }

  /**
   * Get total factures count from payment status
   */
  getTotalFacturesCount(): number {
    if (!this.factureStats?.paymentStatus) return 0;
    return this.factureStats.paymentStatus.reduce((sum: number, p: any) => sum + (p.count || 0), 0);
  }
}