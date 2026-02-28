// src/app/components/chef-projet/chef-projet.component.ts
import { Component, OnInit, ViewChild, ElementRef, AfterViewInit, OnDestroy } from '@angular/core';
import { Application, ApplicationService } from '../../services/application.service';
import { ConventionService } from '../../services/convention.service';
import { StatsService } from '../../services/stats.service';
import { AuthService } from '../../services/auth.service';
import { WorkloadService, WorkloadDTO } from '../../services/workload.service';
import { ChartService } from '../../services/chart.service';

@Component({
  selector: 'app-chef-projet',
  templateUrl: './chef-projet.component.html',
  styleUrls: ['./chef-projet.component.css']
})
export class ChefProjetComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('applicationStatusChart') applicationStatusChartRef!: ElementRef;
  
  // Chart instances
  applicationStatusChart: any = null;

  successMessage = '';
  errorMessage = '';

  // Stats Data
  myProjects: Application[] = [];
  myConventions: any[] = [];
  dashboardStats: any = {};
  conventionStats: any = {};
  factureStats: any = {};
  applicationStats: any = {};
  financialStats: any = {};
  summaryStats: any = {};
  overdueAlerts: any[] = [];

  // Workload properties
  myWorkload: WorkloadDTO | null = null;
  workloadLoading = false;

  // Loading state
  loading = false;

  Math = Math;

  constructor(
    private applicationService: ApplicationService,
    private conventionService: ConventionService,
    private statsService: StatsService,
    private authService: AuthService,
    private workloadService: WorkloadService,
    private chartService: ChartService
  ) {}

  ngOnInit(): void {
    this.loadDashboardData();
    this.loadMyWorkload();
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.renderCharts(), 100);
  }

  loadDashboardData(): void {
    this.loading = true;
    
    const currentUser = this.authService.getCurrentUser();
    if (currentUser && currentUser.id) {
      // Load applications for this chef
      this.applicationService.getApplicationsByChefDeProjet(currentUser.id).subscribe({
        next: (response) => {
          if (response.success) {
            this.myProjects = response.data || [];
            console.log('Loaded applications:', this.myProjects);
          }
          this.loadChefStats();
        },
        error: (error) => {
          console.error('Error loading projects:', error);
          this.loadChefStats();
        }
      });
      
      // Load conventions for this chef
      this.applicationService.getConventionsByChefDeProjet(currentUser.id).subscribe({
        next: (response) => {
          if (response.success) {
            this.myConventions = response.data || [];
            console.log('Loaded conventions:', this.myConventions);
          }
        },
        error: (error) => {
          console.error('Error loading conventions:', error);
        }
      });
    } else {
      this.loadChefStats();
    }
  }

  loadChefStats(): void {
    this.statsService.getChefProjetStats().then((results: any[]) => {
      if (results[0]?.success) this.dashboardStats = results[0].data;
      if (results[1]?.success) this.conventionStats = results[1].data;
      if (results[2]?.success) this.factureStats = results[2].data;
      if (results[3]?.success) this.applicationStats = results[3].data;
      if (results[4]?.success) this.financialStats = results[4].data;
      if (results[5]?.success) this.summaryStats = results[5].data;
      if (results[6]?.success) this.overdueAlerts = results[6].data;
      
      this.loading = false;
      
      setTimeout(() => {
        this.destroyCharts();
        this.renderCharts();
      }, 100);
    }).catch(error => {
      console.error('Error loading chef stats:', error);
      this.loading = false;
    });
  }

  renderCharts(): void {
    // Application Status Chart
    if (this.applicationStatusChartRef && this.applicationStats?.statusDistribution) {
      const labels = this.applicationStats.statusDistribution.map((s: any) => s.name);
      const data = this.applicationStats.statusDistribution.map((s: any) => s.count);
      
      this.applicationStatusChart = this.chartService.createChart(
        this.applicationStatusChartRef.nativeElement,
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
  }

  destroyCharts(): void {
    this.chartService.destroyChart(this.applicationStatusChart);
    this.applicationStatusChart = null;
  }

  loadMyWorkload(): void {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser || !currentUser.id) return;

    this.workloadLoading = true;
    
    this.workloadService.getWorkloadDashboard().subscribe({
      next: (response) => {
        if (response.success && response.data?.workloads) {
          this.myWorkload = response.data.workloads.find(
            (w: WorkloadDTO) => w.chefId === currentUser.id
          ) || null;
        }
        this.workloadLoading = false;
      },
      error: (error) => {
        console.error('Error loading workload:', error);
        this.workloadLoading = false;
      }
    });
  }

  getWorkloadColor(): string {
    if (!this.myWorkload) return '#10b981';
    const workload = this.myWorkload.currentWorkload;
    if (workload > 75) return '#EF4444';
    if (workload >= 45) return '#F97316';
    return '#10B981';
  }

  getWorkloadStatus(): string {
    if (!this.myWorkload) return 'Non disponible';
    const workload = this.myWorkload.currentWorkload;
    if (workload > 75) return 'Critique';
    if (workload >= 45) return 'Élevée';
    return 'Normale';
  }

  getWorkloadBarClass(): string {
    if (!this.myWorkload) return 'bg-green-500';
    const workload = this.myWorkload.currentWorkload;
    if (workload > 75) return 'bg-red-500';
    if (workload >= 45) return 'bg-orange-500';
    return 'bg-green-500';
  }

  getProjectStatusSummary(): any {
    const summary = {
      PLANIFIE: 0,
      EN_COURS: 0,
      TERMINE: 0,
      OTHER: 0
    };

    this.myProjects.forEach(project => {
      const status = project.status || 'OTHER';
      if (summary.hasOwnProperty(status)) {
        summary[status as keyof typeof summary]++;
      } else {
        summary.OTHER++;
      }
    });

    return summary;
  }

  getConventionStatusSummary(): any {
    const summary = {
      PLANIFIE: 0,
      'EN COURS': 0,
      TERMINE: 0,
      ARCHIVE: 0,
      OTHER: 0
    };

    this.myConventions.forEach(conv => {
      const etat = conv.etat || 'OTHER';
      if (summary.hasOwnProperty(etat)) {
        summary[etat as keyof typeof summary]++;
      } else {
        summary.OTHER++;
      }
    });

    return summary;
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'PLANIFIE': return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-300';
      case 'EN_COURS': return 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300';
      case 'TERMINE': return 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300';
      default: return 'bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-300';
    }
  }

  getConventionStatusClass(etat: string): string {
    switch (etat) {
      case 'PLANIFIE': return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-300';
      case 'EN COURS': return 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300';
      case 'TERMINE': return 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300';
      case 'ARCHIVE': return 'bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-300';
      default: return 'bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-300';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'PLANIFIE': return 'Planifié';
      case 'EN_COURS': return 'En Cours';
      case 'TERMINE': return 'Terminé';
      case 'ARCHIVE': return 'Archivé';
      default: return status || 'Non défini';
    }
  }

  getConventionStatusLabel(etat: string): string {
    switch (etat) {
      case 'PLANIFIE': return 'Planifié';
      case 'EN COURS': return 'En Cours';
      case 'TERMINE': return 'Terminé';
      case 'ARCHIVE': return 'Archivé';
      default: return etat || 'Non défini';
    }
  }

  getProgressClass(progress: number): string {
    if (progress >= 90) return 'bg-green-500';
    if (progress >= 70) return 'bg-blue-500';
    if (progress >= 50) return 'bg-yellow-500';
    if (progress >= 30) return 'bg-orange-500';
    return 'bg-red-500';
  }

  formatDate(dateString: string): string {
    if (!dateString) return 'Non spécifié';
    return new Date(dateString).toLocaleDateString('fr-FR');
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'TND',
      minimumFractionDigits: 2
    }).format(value);
  }

  refreshData(): void {
    this.loadDashboardData();
    this.loadMyWorkload();
  }

  clearMessages(): void {
    this.successMessage = '';
    this.errorMessage = '';
  }

  ngOnDestroy(): void {
    this.destroyCharts();
  }
}