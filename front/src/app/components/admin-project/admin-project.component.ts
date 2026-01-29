// src/app/components/admin-project/admin-project.component.ts
import { Component, OnInit } from '@angular/core';
import { ProjectService, Project, ProjectRequest } from '../../services/project.service';
import { NomenclatureService } from '../../services/nomenclature.service';
import { AuthService } from '../../services/auth.service';
import { UserService } from '../../services/user.service'; // ADD THIS
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-admin-project',
  templateUrl: './admin-project.component.html',
  styleUrls: ['./admin-project.component.css'],
  standalone: false
})
export class AdminProjectComponent implements OnInit {
  projects: Project[] = [];
  filteredProjects: Project[] = [];
  applications: any[] = [];
  chefsProjet: any[] = []; // Users with ROLE_CHEF_PROJET
  loading = false;
  errorMessage = '';
  successMessage = '';
  searchTerm = '';
  filterStatus = '';

  // Track current user role
  currentUser: any = null;
  isAdmin = false;
  isChefProjet = false;

  // Modals
  showAddModal = false;
  showEditModal = false;
  showDeleteModal = false;
  selectedProject: Project | null = null;

  // Project form
  projectForm: ProjectRequest = {
    code: '',
    name: '',
    description: '',
    applicationId: 0,
    chefDeProjetId: 0,
    clientName: '',
    clientEmail: '',
    clientPhone: '',
    clientAddress: '',
    dateDebut: '',
    dateFin: '',
    progress: 0,
    budget: 0,
    status: 'PLANIFIE'
  };

  statusOptions = [
  { value: 'PLANIFIE', label: 'Planifié' },
  { value: 'EN_COURS', label: 'En Cours' },
  { value: 'TERMINE', label: 'Terminé' },
  { value: 'SUSPENDU', label: 'Suspendu' },
  { value: 'ANNULE', label: 'Annulé' }
];

  constructor(
    private projectService: ProjectService,
    private nomenclatureService: NomenclatureService,
    private authService: AuthService,
    private userService: UserService // ADD THIS
  ) {}

  ngOnInit(): void {
    this.loadCurrentUser();
    this.loadProjects();
    this.loadApplications();
    
    // Only load chefs if user is admin
    if (this.isAdmin) {
      this.loadChefsProjet();
    }
  }

  loadCurrentUser(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.isAdmin = this.authService.isAdmin();
    this.isChefProjet = this.authService.isChefProjet();
  }

  loadProjects(): void {
    this.loading = true;
    this.projectService.getAllProjects().subscribe({
      next: (response) => {
        if (response.success) {
          this.projects = response.data;
          
          // If user is chef de projet, filter only their projects
          if (this.isChefProjet && this.currentUser) {
            this.projects = this.projects.filter(project => 
              project.chefDeProjetId === this.currentUser.id
            );
          }
          
          this.filteredProjects = [...this.projects];
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading projects:', error);
        this.errorMessage = error.error?.message || 'Failed to load projects';
        this.loading = false;
      }
    });
  }

  loadApplications(): void {
    this.nomenclatureService.getApplications().subscribe({
      next: (applications) => {
        this.applications = applications;
      },
      error: (error) => {
        console.error('Error loading applications:', error);
      }
    });
  }

  loadChefsProjet(): void {
    // Only load chefs if user is admin
    if (!this.isAdmin) return;
    
    this.userService.getChefsProjet().subscribe({
      next: (response) => {
        if (response.success) {
          this.chefsProjet = response.data;
        }
      },
      error: (error) => {
        console.error('Error loading chefs de projet:', error);
        this.errorMessage = 'Failed to load chefs de projet';
      }
    });
  }

  searchProjects(): void {
    if (!this.searchTerm.trim()) {
      this.filteredProjects = this.filterByStatus(this.projects);
      return;
    }

    const term = this.searchTerm.toLowerCase();
    this.filteredProjects = this.filterByStatus(this.projects).filter(project =>
      project.code.toLowerCase().includes(term) ||
      project.name.toLowerCase().includes(term) ||
      project.clientName.toLowerCase().includes(term) ||
      project.description?.toLowerCase().includes(term) ||
      project.applicationName?.toLowerCase().includes(term)
    );
  }

  filterByStatus(projects: Project[]): Project[] {
    if (!this.filterStatus) {
      return projects;
    }
    return projects.filter(project => project.status === this.filterStatus);
  }

  applyFilters(): void {
    this.filteredProjects = this.filterByStatus(this.projects);
    if (this.searchTerm.trim()) {
      this.searchProjects();
    }
  }

 openAddModal(): void {
  this.projectForm = {
    code: '',
    name: '',
    description: '',
    applicationId: 0,
    chefDeProjetId: this.isChefProjet && this.currentUser ? this.currentUser.id : 0, // Optional now
    clientName: '',
    clientEmail: '',
    clientPhone: '',
    clientAddress: '',
    dateDebut: new Date().toISOString().split('T')[0],
    dateFin: '',
    progress: 0,
    budget: 0,
    status: 'PLANIFIE'
  };
  this.showAddModal = true;
  this.errorMessage = '';
}

openEditModal(project: Project): void {
  this.selectedProject = project;
  this.projectForm = {
    code: project.code,
    name: project.name,
    description: project.description || '',
    applicationId: project.applicationId,
    chefDeProjetId: project.chefDeProjetId,
    clientName: project.clientName,
    clientEmail: project.clientEmail || '',
    clientPhone: project.clientPhone || '',
    clientAddress: project.clientAddress || '',
    dateDebut: project.dateDebut || '',
    dateFin: project.dateFin || '',
    progress: project.progress || 0, // THIS WAS MISSING!
    budget: project.budget || 0,
    status: project.status || 'PLANIFIE'
  };
  
  this.showEditModal = true;
  this.errorMessage = '';
}

// Add this method if not exists
updateProgressValue(event: any): void {
  this.projectForm.progress = parseInt(event.target.value, 10);
}

  openDeleteModal(project: Project): void {
    this.selectedProject = project;
    this.showDeleteModal = true;
  }

  closeModal(): void {
    this.showAddModal = false;
    this.showEditModal = false;
    this.showDeleteModal = false;
    this.selectedProject = null;
    this.errorMessage = '';
  }

createProject(): void {
  if (!this.validateProjectForm()) {
    return;
  }

  // Always set status to PLANIFIE when creating (backend will auto-update later)
  this.projectForm.status = 'PLANIFIE';
  
  this.loading = true;
  this.projectService.createProject(this.projectForm).subscribe({
    next: (response) => {
      if (response.success) {
        this.successMessage = 'Projet créé avec succès';
        this.loadProjects();
        this.closeModal();
      }
      this.loading = false;
    },
    error: (error) => {
      this.errorMessage = error.error?.message || 'Échec de la création du projet';
      this.loading = false;
    }
  });
}

updateProject(): void {
  if (!this.selectedProject || !this.validateProjectForm()) {
    return;
  }

  const currentStatus = this.selectedProject.status;
  
  this.loading = true;
  this.projectService.updateProject(this.selectedProject.id, this.projectForm).subscribe({
    next: (response) => {
      if (response.success) {
        this.successMessage = 'Projet mis à jour avec succès';
        this.loadProjects();
        this.closeModal();
      }
      this.loading = false;
    },
    error: (error) => {
      this.errorMessage = error.error?.message || 'Échec de la mise à jour du projet';
      this.loading = false;
    }
  });
}

  deleteProject(): void {
    if (!this.selectedProject) return;

    this.loading = true;
    this.projectService.deleteProject(this.selectedProject.id).subscribe({
      next: (response) => {
        if (response.success) {
          this.successMessage = 'Projet supprimé avec succès';
          this.loadProjects();
          this.closeModal();
        }
        this.loading = false;
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Échec de la suppression du projet';
        this.loading = false;
      }
    });
  }

  validateProjectForm(): boolean {
    if (!this.projectForm.code.trim()) {
      this.errorMessage = 'Le code du projet est requis';
      return false;
    }
    if (!this.projectForm.name.trim()) {
      this.errorMessage = 'Le nom du projet est requis';
      return false;
    }
    if (!this.projectForm.clientName.trim()) {
      this.errorMessage = 'Le nom du client est requis';
      return false;
    }
    if (!this.projectForm.applicationId) {
      this.errorMessage = 'L\'application est requise';
      return false;
    }
    
    return true;
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'PLANIFIE': return 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300';
      case 'EN_COURS': return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300';
      case 'TERMINE': return 'bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-300';
      case 'SUSPENDU': return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-300';
      case 'ANNULE': return 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-300';
      default: return 'bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-300';
    }
  }

  getStatusLabel(status: string): string {
    const option = this.statusOptions.find(opt => opt.value === status);
    return option ? option.label : status;
  }

  getProgressClass(progress: number): string {
    if (progress >= 90) return 'bg-green-500';
    if (progress >= 70) return 'bg-blue-500';
    if (progress >= 50) return 'bg-yellow-500';
    if (progress >= 30) return 'bg-orange-500';
    return 'bg-red-500';
  }

  isAd(): boolean {
    return this.authService.isAdmin();
  }

  
incrementProgress(project: Project): void {
  // Check if user is authorized to update this project
  const currentUser = this.authService.getCurrentUser();
  
  // If user is not admin and not the project's chef de projet, show error
  if (!this.isAd() && project.chefDeProjetId !== currentUser?.id) {
    this.errorMessage = 'Vous ne pouvez mettre à jour que vos propres projets';
    return;
  }
  
  const newProgress = Math.min(100, project.progress + 10);
  this.loading = true;
  
  this.projectService.updateProjectProgress(project.id, newProgress).subscribe({
    next: (response: any) => {
      if (response.success) {
        this.successMessage = 'Progression mise à jour avec succès';
        
        // Update the local project
        project.progress = newProgress;
        if (newProgress === 100) {
          project.status = 'TERMINE';
        }
        
        // Refresh if needed
        this.applyFilters();
      }
      this.loading = false;
    },
    error: (error: any) => {
      console.error('Progress update error:', error);
      
      if (error.status === 403 || error.status === 401) {
        this.errorMessage = 'Accès refusé : Vous n\'avez pas la permission de mettre à jour ce projet';
      } else if (error.status === 400) {
        this.errorMessage = error.error?.message || 'Données invalides';
      } else {
        this.errorMessage = 'Erreur lors de la mise à jour de la progression';
      }
      
      this.loading = false;
    }
  });
}

  // Statistics methods
  getTotalProjectsCount(): number {
    return this.projects.length;
  }

  getEnCoursCount(): number {
    return this.projects.filter(p => p.status === 'EN_COURS').length;
  }

  getPlanifieCount(): number {
    return this.projects.filter(p => p.status === 'PLANIFIE').length;
  }

  getDelayedCount(): number {
    return this.projects.filter(p => p.isDelayed).length;
  }

  // Show chef de projet field only for admin
  shouldShowChefField(): boolean {
    return this.isAdmin;
  }

  // Get current chef de projet name for display
  getCurrentChefName(): string {
    if (!this.currentUser) return '';
    return `${this.currentUser.firstName} ${this.currentUser.lastName}`;
  }



}