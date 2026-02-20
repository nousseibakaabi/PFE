import { Component, OnInit } from '@angular/core';
import { Application, ApplicationService  } from '../../services/application.service';
import { ConventionService } from '../../services/convention.service';
import { AuthService } from '../../services/auth.service';
import { WorkloadService, WorkloadDTO } from '../../services/workload.service'; // Add this
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-chef-projet',
  templateUrl: './chef-projet.component.html',
  styleUrls: ['./chef-projet.component.css'],
  standalone: false
})
export class ChefProjetComponent implements OnInit {
  successMessage = '';
  errorMessage = '';
  originalProgress = 0;

  // Your existing properties...
  myProjects: Application[] = [];
  myConventions: any[] = [];
  dashboardStats: any = {};
  loading = false;

  // NEW: Workload properties
  myWorkload: WorkloadDTO | null = null;
  workloadLoading = false;

  // For project progress updates
  editingProgress: number | null = null;
  selectedProjectForProgress: Application | null = null;

    Math = Math;

  constructor(
    private applicationService: ApplicationService,
    private conventionService: ConventionService,
    private authService: AuthService,
    private workloadService: WorkloadService // Add this
  ) {}

  ngOnInit(): void {
    this.loadDashboardData();
    this.loadMyWorkload();
  }

  loadDashboardData(): void {
    this.loading = true;
    
    // Get current user's projects
    const currentUser = this.authService.getCurrentUser();
    if (currentUser && currentUser.id) {
      this.applicationService.getApplicationsByChefDeProjet(currentUser.id).subscribe({
        next: (response) => {
          if (response.success) {
            this.myProjects = response.data;
            this.loadConventionsForProjects();
          }
          this.loading = false;
        },
        error: (error) => {
          console.error('Error loading projects:', error);
          this.loading = false;
        }
      });
    }
    
    // Get project dashboard
    this.applicationService.getApplicationDashboard().subscribe({
      next: (response) => {
        if (response.success) {
          this.dashboardStats = response.data;
        }
      },
      error: (error) => {
        console.error('Error loading dashboard:', error);
      }
    });
  }

  // NEW: Load current chef's workload
  loadMyWorkload(): void {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser || !currentUser.id) return;

    this.workloadLoading = true;
    
    // Since we don't have a specific endpoint for single chef, we'll use the dashboard
    // In a real app, you'd create a dedicated endpoint
    this.workloadService.getWorkloadDashboard().subscribe({
      next: (response) => {
        if (response.success && response.data?.workloads) {
          // Find current chef's workload
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

  // NEW: Get workload color for progress bar
  getWorkloadColor(): string {
    if (!this.myWorkload) return '#10b981'; // Default to green if no data
    return this.workloadService.getWorkloadClass(this.myWorkload.currentWorkload);
  }

  // NEW: Get workload status text
  getWorkloadStatus(): string {
    if (!this.myWorkload) return 'Non disponible';
    return this.workloadService.getWorkloadStatus(this.myWorkload.currentWorkload);
  }

  // NEW: Get progress bar class
  getWorkloadBarClass(): string {
    if (!this.myWorkload) return 'bg-green-500';
    return this.workloadService.getProgressBarClass(this.myWorkload.currentWorkload);
  }

  loadConventionsForProjects(): void {
    const currentUser = this.authService.getCurrentUser();
    if (currentUser && currentUser.id) {
      this.applicationService.getConventionsByChefDeProjet(currentUser.id).subscribe({
        next: (response) => {
          if (response.success) {
            this.myConventions = response.data || [];
            console.log('Loaded conventions for chef de projet:', this.myConventions);
          }
        },
        error: (error) => {
          console.error('Error loading conventions:', error);
        }
      });
    }
  }

  getConventionsForProject(applicationId: number): any[] {
    if (!this.myConventions || this.myConventions.length === 0) {
      return [];
    }
    
    const conventions = this.myConventions.filter(conv => {
      const convAppId = conv.applicationId || 
                       conv.application?.id || 
                       conv.projectId || 
                       conv.project?.id;
      
      return Number(convAppId) === Number(applicationId);
    });
    
    console.log(`Found ${conventions.length} conventions for project ${applicationId}`);
    return conventions;
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
      case 'PLANIFIE': return 'bg-yellow-100 text-yellow-800';
      case 'EN COURS': return 'bg-blue-100 text-blue-800';
      case 'TERMINE': return 'bg-green-100 text-green-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }



  

  refreshData(): void {
    this.loadDashboardData();
    this.loadMyWorkload();
  }

  getProjectProperties(project: any): any {
    return {
      dateRange: this.getDateRange(project),
      timeRemainingString: this.getTimeRemainingString(project),
      daysRemaining: this.getDaysRemaining(project),
      timeBasedProgress: this.getTimeBasedProgress(project),
      isDelayed: this.isProjectDelayed(project),
      clientName: project.clientName || 'Non spécifié',
      applicationName: project.applicationName || 'Non spécifié'
    };
  }

  getConventionStatusClass(etat: string): string {
    switch (etat) {
      case 'PLANIFIE': return 'bg-yellow-100 text-yellow-800';
      case 'EN COURS': return 'bg-blue-100 text-blue-800';
      case 'TERMINE': return 'bg-green-100 text-green-800';
      case 'ARCHIVE': return 'bg-gray-100 text-gray-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  getConventionStatusLabel(etat: string): string {
    switch (etat) {
      case 'PLANIFIE': return 'Planifié';
      case 'EN COURS': return 'En Cours';
      case 'TERMINE': return 'Terminé';
      case 'ARCHIVE': return 'Archivé';
      default: return etat;
    }
  }

  formatDate(dateString: string): string {
    if (!dateString) return 'Non spécifié';
    return new Date(dateString).toLocaleDateString('fr-FR');
  }

  getClientNameFromConvention(conv: any): string {
    return conv.clientNameFromProject || conv.clientName || 'Non spécifié';
  }

  getProjectNameFromConvention(conv: any): string {
    return conv.projectName || conv.projectCode || 'Non spécifié';
  }

  getProgressClass(progress: number): string {
    if (progress >= 90) return 'bg-green-500';
    if (progress >= 70) return 'bg-blue-500';
    if (progress >= 50) return 'bg-yellow-500';
    if (progress >= 30) return 'bg-orange-500';
    return 'bg-red-500';
  }

  getTimeBasedProgress(project: any): number {
    if (!project.dateDebut || !project.dateFin) {
      return 0;
    }
    
    const startDate = new Date(project.dateDebut);
    const endDate = new Date(project.dateFin);
    const today = new Date();
    
    if (today < startDate) return 0;
    if (today > endDate) return 100;
    
    const totalDuration = endDate.getTime() - startDate.getTime();
    const elapsedDuration = today.getTime() - startDate.getTime();
    
    return Math.round((elapsedDuration / totalDuration) * 100);
  }

  getDateRange(project: any): string {
    if (!project.dateDebut) return 'Dates non définies';
    
    const start = new Date(project.dateDebut).toLocaleDateString('fr-FR');
    
    if (!project.dateFin) {
      return 'Depuis ' + start;
    }
    
    const end = new Date(project.dateFin).toLocaleDateString('fr-FR');
    return start + ' - ' + end;
  }

  getTimeRemainingString(project: any): string {
    const daysRemaining = this.getDaysRemaining(project);
    
    if (daysRemaining === 0) {
      return 'Terminé';
    }
    
    if (daysRemaining === 1) {
      return '1 jour restant';
    }
    
    if (daysRemaining < 0) {
      return Math.abs(daysRemaining) + ' jours de retard';
    }
    
    return daysRemaining + ' jours restants';
  }

  getEtaClass(daysRemaining: number): string {
    if (daysRemaining < 0) return 'text-red-600 font-bold';
    if (daysRemaining <= 7) return 'text-orange-600 font-semibold';
    if (daysRemaining <= 30) return 'text-yellow-600';
    return 'text-green-600';
  }

  
  getStatusLabel(status: string): string {
    if (!status) return 'Non défini';
    
    switch (status) {
      case 'PLANIFIE': return 'Planifié';
      case 'EN COURS': return 'En Cours';
      case 'TERMINE': return 'Terminé';
      case 'ARCHIVE': return 'Archivé';
      default: return status;
    }
  }

  getDelayedProjectsCount(): number {
    if (!this.myProjects || this.myProjects.length === 0) {
      return 0;
    }
    
    return this.myProjects.filter(project => this.isProjectDelayed(project)).length;
  }

  isProjectDelayed(project: any): boolean {
    const daysRemaining = this.getDaysRemaining(project);
    const isPastDue = daysRemaining < 0;
    const isNotCompleted = project.status !== 'TERMINE' && (project.progress || 0) < 100;
    
    return isPastDue && isNotCompleted;
  }

  getDaysRemaining(project: any): number {
    if (!project || !project.dateFin) return 0;
    
    try {
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      
      const endDate = new Date(project.dateFin);
      endDate.setHours(0, 0, 0, 0);
      
      const timeDiff = endDate.getTime() - today.getTime();
      const daysDiff = Math.ceil(timeDiff / (1000 * 3600 * 24));
      
      return daysDiff;
    } catch (error) {
      console.error('Error calculating days remaining:', error);
      return 0;
    }
  }

  clearMessages(): void {
    this.successMessage = '';
    this.errorMessage = '';
  }
}