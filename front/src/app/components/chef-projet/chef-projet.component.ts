import { Component, OnInit, ViewChild, ElementRef, AfterViewInit, OnDestroy } from '@angular/core';
import { Application, ApplicationService } from '../../services/application.service';
import { ConventionService } from '../../services/convention.service';
import { StatsService } from '../../services/stats.service';
import { AuthService } from '../../services/auth.service';
import { WorkloadService, WorkloadDTO } from '../../services/workload.service';
import { ChartService } from '../../services/chart.service';
import Chart from 'chart.js/auto';
import { LayoutService } from '../partials/services/layout.service'; 
import { ChangeDetectorRef } from '@angular/core';

@Component({
  selector: 'app-chef-projet',
  templateUrl: './chef-projet.component.html',
  styleUrls: ['./chef-projet.component.css']
})
export class ChefProjetComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('applicationStatusChart') applicationStatusChartRef!: ElementRef;
  @ViewChild('workloadChartRef') workloadChartRef!: ElementRef;
workloadChart: any = null;
  
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


  currentSlide = 0;
slidesPerView = 1;
autoSlideInterval: any;
  isSidebarOpen: boolean = false;


  constructor(
    private applicationService: ApplicationService,
    private conventionService: ConventionService,
    private statsService: StatsService,
    public authService: AuthService,
    private workloadService: WorkloadService,
    private chartService: ChartService,
        private layoutService: LayoutService ,
          private cdr: ChangeDetectorRef


  ) {}

 ngOnInit(): void {
    this.loadDashboardData();
    this.loadMyWorkload();
    
    // Subscribe to sidebar state changes
    this.layoutService.sidebarOpen$.subscribe((isOpen: boolean) => {
      this.isSidebarOpen = isOpen;
      // Optional: trigger a small delay to recalculate charts if needed
      setTimeout(() => {
        if (this.workloadChart) {
          this.workloadChart.resize();
        }
      }, 100);
    });
  }

  getContainerMaxWidth(): string {
    return this.isSidebarOpen ? '1250px' : '1400px';
  }

ngAfterViewInit() {
  this.initAutoSlide();
  this.updateSlidesPerView();
  window.addEventListener('resize', () => this.updateSlidesPerView());
  setTimeout(() => this.renderCharts(), 100);
  
  // Force change detection for chart
  this.cdr.detectChanges();
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
              position: 'bottom',
              display: false
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

loadMyWorkload() {
  const currentUser = this.authService.getCurrentUser();
  if (!currentUser || !currentUser.id) return;

  this.workloadLoading = true;
  
  this.workloadService.getWorkloadDashboard().subscribe({
    next: (response) => {
      if (response.success && response.data?.workloads) {
        this.myWorkload = response.data.workloads.find(
          (w: WorkloadDTO) => w.chefId === currentUser.id
        ) || null;
        setTimeout(() => this.initWorkloadChart(), 100);
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



  refreshData(): void {
    this.loadDashboardData();
    this.loadMyWorkload();
  }

  clearMessages(): void {
    this.successMessage = '';
    this.errorMessage = '';
  }




updateSlidesPerView() {
  if (window.innerWidth >= 1024) {
    this.slidesPerView = 3;
  } else if (window.innerWidth >= 768) {
    this.slidesPerView = 2;
  } else {
    this.slidesPerView = 1;
  }
  this.currentSlide = Math.min(this.currentSlide, this.totalSlides - 1);
}

get totalSlides(): number {
  return Math.ceil(this.overdueAlerts.length / this.slidesPerView);
}

initAutoSlide() {
  this.autoSlideInterval = setInterval(() => {
    if (this.overdueAlerts.length > 0) {
      this.nextSlide();
    }
  }, 4000);
}

nextSlide() {
  if (this.currentSlide < this.totalSlides - 1) {
    this.currentSlide++;
  } else {
    this.currentSlide = 0;
  }
}

prevSlide() {
  if (this.currentSlide > 0) {
    this.currentSlide--;
  } else {
    this.currentSlide = this.totalSlides - 1;
  }
}

goToSlide(index: number) {
  this.currentSlide = index;
}

ngOnDestroy() {
  if (this.autoSlideInterval) {
    clearInterval(this.autoSlideInterval);
  }
  this.destroyCharts();
}




  // Add these methods to your ChefProjetComponent class

// Get connected user full name
getConnectedFullName(): string {
  const currentUser = this.authService.getCurrentUser();
  if (currentUser) {
    // Adjust property names based on your User model
    return `${currentUser.firstName || ''} ${currentUser.lastName || ''}`.trim() || currentUser.email || 'Utilisateur';
  }
  return 'Chef de Projet';
}

// Get total financial value from conventions
getTotalFinancialValue(): number {
  if (!this.myConventions || this.myConventions.length === 0) return 0;
  
  let total = 0;
  this.myConventions.forEach(conv => {
    // Adjust property name based on your convention model (montant, valeur, etc.)
    total += conv.montantTotal || conv.valeur || conv.montant || 0;
  });
  return total;
}

getTotalDuration(): number {
  if (!this.myProjects || this.myProjects.length === 0) return 0;
  
  let totalDays = 0;
  this.myProjects.forEach(project => {
    if (project.dateDebut && project.dateFin) {
      const start = new Date(project.dateDebut);
      const end = new Date(project.dateFin);
      const diffTime = Math.abs(end.getTime() - start.getTime());
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
      totalDays += diffDays;
    }
  });
  return totalDays;
}


initWorkloadChart() {
  // Add safety checks
  if (!this.workloadChartRef) {
    console.warn('Workload chart ref not available');
    setTimeout(() => this.initWorkloadChart(), 200);
    return;
  }
  
  const canvasEl = this.workloadChartRef.nativeElement;
  if (!canvasEl) {
    console.warn('Canvas element not found');
    return;
  }
  
  const ctx = canvasEl.getContext('2d');
  if (!ctx) {
    console.warn('Could not get canvas context');
    return;
  }
  
  if (!this.myWorkload) {
    console.warn('Workload data not available yet');
    setTimeout(() => this.initWorkloadChart(), 500);
    return;
  }
  
  const used = this.myWorkload.currentWorkload || 0;
  const remaining = 100 - used;
  
  if (this.workloadChart) {
    this.workloadChart.destroy();
  }
  
  this.workloadChart = new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: ['Charge utilisée', 'Capacité disponible'],
      datasets: [{
        data: [used, remaining],
        backgroundColor: ['#3b82f6', '#76b7e8'],
        borderWidth: 0,
        hoverOffset: 4,
        borderRadius: 10,
        spacing: 5
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: true,
      layout: {
        padding: {
          bottom: 10
        }
      },
      plugins: {
        legend: {
           display: false,
          position: 'bottom',
          align: 'center',
          labels: {
            font: { size: 11, weight: '500' },
            boxWidth: 10,
            boxHeight: 10,
            usePointStyle: true,
            pointStyle: 'circle',
            padding: 15
          }
        },
        tooltip: {
          callbacks: {
            label: (context: any) => {
              const label = context.label || '';
              const value = context.parsed || 0;
              return `${label}: ${value.toFixed(1)}%`;
            }
          }
        }
      },
      cutout: '75%'
    }
  });
  
  // Add percentage text inside the chart - FIXED for better positioning and smaller text
  if (this.workloadChart) {
    const originalDraw = this.workloadChart.draw;
    this.workloadChart.draw = function(this: any) {
      originalDraw.apply(this, arguments);
      const ctx = this.ctx;
      const width = this.width;
      const height = this.height;
      
      ctx.save();
      // Make the font smaller - using absolute pixel size
      const fontSize = Math.min(height * 0.10, 26);
      ctx.font = `700 ${fontSize}px "Inter", system-ui, sans-serif`;
      ctx.fillStyle = '#1f2937';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      
      const text = `${used.toFixed(1)}%`;
      const textX = width / 2;
      const textY = height / 2;
      
      ctx.fillText(text, textX, textY);
      ctx.restore();
    };
    
    this.workloadChart.update();
  }
}

}