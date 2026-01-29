// src/app/components/chef-projet/chef-projet.component.ts
import { Component, OnInit } from '@angular/core';
import { ProjectService, Project } from '../../services/project.service';
import { ConventionService } from '../../services/convention.service';
import { AuthService } from '../../services/auth.service';
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
  originalProgress = 0; // Track original progress for rollback

  // Your existing properties...
  myProjects: Project[] = [];
  myConventions: any[] = [];
  dashboardStats: any = {};
  loading = false;

  // For project progress updates
  editingProgress: number | null = null;
  selectedProjectForProgress: Project | null = null;

  constructor(
    private projectService: ProjectService,
    private conventionService: ConventionService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.loading = true;
    
    // Get current user's projects
    const currentUser = this.authService.getCurrentUser();
    if (currentUser && currentUser.id) {
      this.projectService.getProjectsByChefDeProjet(currentUser.id).subscribe({
        next: (response) => {
          if (response.success) {
            this.myProjects = response.data;
            
            // Load conventions for these projects
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
    this.projectService.getProjectDashboard().subscribe({
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

  loadConventionsForProjects(): void {
    const projectIds = this.myProjects.map(p => p.id);
    
    // Get all conventions and filter by project
    this.conventionService.getAllConventions().subscribe({
      next: (response) => {
        if (response.success) {
          this.myConventions = response.data.filter((conv: any) => 
            conv.projectId && projectIds.includes(conv.projectId)
          );
        }
      },
      error: (error) => {
        console.error('Error loading conventions:', error);
      }
    });
  }

getConventionsForProject(projectId: number): any[] {
  if (!this.myConventions || !this.myConventions.length) return [];
  return this.myConventions.filter(conv => conv.projectId === projectId);
}


 getProjectStatusSummary(): any {
  const summary = {
    PLANIFIE: 0,
    EN_COURS: 0,
    TERMINE: 0,
    SUSPENDU: 0,
    ANNULE: 0,
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
    EN_ATTENTE: 0,
    EN_COURS: 0,
    EN_RETARD: 0,
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

startProgressEdit(project: Project): void {
    this.selectedProjectForProgress = project;
    this.originalProgress = project.progress || 0; // Store original
    this.editingProgress = project.progress || 0;
  }

  saveProgress(): void {
    if (this.selectedProjectForProgress && this.editingProgress !== null) {
      this.loading = true;
      this.successMessage = '';
      this.errorMessage = '';
      
      // Store original for rollback
      const originalProgress = this.selectedProjectForProgress.progress;
      
      // Update the project locally first (optimistic update)
      this.selectedProjectForProgress.progress = this.editingProgress;
      
      // Update status if 100%
      if (this.editingProgress === 100) {
        this.selectedProjectForProgress.status = 'TERMINE';
      }
      
      this.projectService.updateProjectProgress(
        this.selectedProjectForProgress.id, 
        this.editingProgress
      ).subscribe({
        next: (response) => {
          if (response.success) {
            // Update the project in the local array
            const index = this.myProjects.findIndex(
              p => p.id === this.selectedProjectForProgress?.id
            );
            if (index !== -1) {
              this.myProjects[index].progress = this.editingProgress!;
              if (this.editingProgress === 100) {
                this.myProjects[index].status = 'TERMINE';
              }
            }
            this.successMessage = 'Progression mise à jour avec succès';
          }
          this.loading = false;
          this.cancelProgressEdit();
        },
        error: (error) => {
          console.error('Error updating progress:', error);
          
          // Revert if error
          if (this.selectedProjectForProgress) {
            this.selectedProjectForProgress.progress = originalProgress;
          }
          
          this.errorMessage = error.error?.message || 'Échec de la mise à jour';
          this.loading = false;
          this.cancelProgressEdit();
        }
      });
    }
  }

  cancelProgressEdit(): void {
    this.selectedProjectForProgress = null;
    this.editingProgress = null;
    this.originalProgress = 0;
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'PLANIFIE': return 'bg-blue-100 text-blue-800';
      case 'EN_COURS': return 'bg-green-100 text-green-800';
      case 'TERMINE': return 'bg-gray-100 text-gray-800';
      case 'SUSPENDU': return 'bg-yellow-100 text-yellow-800';
      case 'ANNULE': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }



  refreshData(): void {
    this.loadDashboardData();
  }


  // Add these methods to your existing ChefProjetComponent class



// Add these getter methods for project properties (if they don't exist in Project interface)
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




// For convention status
getConventionStatusClass(etat: string): string {
  switch (etat) {
    case 'EN_ATTENTE': return 'bg-yellow-100 text-yellow-800';
    case 'EN_COURS': return 'bg-blue-100 text-blue-800';
    case 'EN_RETARD': return 'bg-red-100 text-red-800';
    case 'TERMINE': return 'bg-green-100 text-green-800';
    case 'ARCHIVE': return 'bg-gray-100 text-gray-800';
    default: return 'bg-gray-100 text-gray-800';
  }
}

getConventionStatusLabel(etat: string): string {
  switch (etat) {
    case 'EN_ATTENTE': return 'En Attente';
    case 'EN_COURS': return 'En Cours';
    case 'EN_RETARD': return 'En Retard';
    case 'TERMINE': return 'Terminé';
    case 'ARCHIVE': return 'Archivé';
    default: return etat;
  }
}

// Format date for display
formatDate(dateString: string): string {
  if (!dateString) return 'Non spécifié';
  return new Date(dateString).toLocaleDateString('fr-FR');
}

// Get client name from convention
getClientNameFromConvention(conv: any): string {
  return conv.clientNameFromProject || conv.clientName || 'Non spécifié';
}

// Get project name from convention
getProjectNameFromConvention(conv: any): string {
  return conv.projectName || conv.projectCode || 'Non spécifié';
}



// Add these methods to your ChefProjetComponent class:

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
    case 'EN_COURS': return 'En Cours';
    case 'TERMINE': return 'Terminé';
    case 'SUSPENDU': return 'Suspendu';
    case 'ANNULE': return 'Annulé';
    case 'EN_ATTENTE': return 'En Attente';
    case 'EN_RETARD': return 'En Retard';
    case 'ARCHIVE': return 'Archivé';
    default: return status;
  }
}



// Add these methods to your ChefProjetComponent class:

getAverageProgress(): string {
  if (!this.myProjects || this.myProjects.length === 0) {
    return '0.0';
  }
  
  const total = this.myProjects.reduce((sum, project) => {
    return sum + (project.progress || 0);
  }, 0);
  
  return (total / this.myProjects.length).toFixed(1);
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

// Also update your getDaysRemaining method to handle edge cases:
getDaysRemaining(project: any): number {
  if (!project || !project.dateFin) return 0;
  
  try {
    const today = new Date();
    today.setHours(0, 0, 0, 0); // Normalize to start of day
    
    const endDate = new Date(project.dateFin);
    endDate.setHours(0, 0, 0, 0); // Normalize to start of day
    
    const timeDiff = endDate.getTime() - today.getTime();
    const daysDiff = Math.ceil(timeDiff / (1000 * 3600 * 24));
    
    return daysDiff;
  } catch (error) {
    console.error('Error calculating days remaining:', error);
    return 0;
  }
}


// Add these methods to clear messages
clearMessages(): void {
  this.successMessage = '';
  this.errorMessage = '';
}



}