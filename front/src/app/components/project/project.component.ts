// src/app/components/project/project.component.ts
import { Component, OnInit } from '@angular/core';
import { ProjectService, Project, ProjectRequest } from '../../services/project.service';
import { NomenclatureService } from '../../services/nomenclature.service';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-project',
  templateUrl: './project.component.html',
  styleUrls: ['./project.component.css'],
  standalone: false
})
export class ProjectComponent implements OnInit {
  projects: Project[] = [];
  filteredProjects: Project[] = [];
  applications: any[] = [];
  loading = false;
  errorMessage = '';
  successMessage = '';
  searchTerm = '';
  filterStatus = '';

  // Current user info
  currentUser: any = null;

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
    chefDeProjetId: 0, // Will be auto-filled with current user
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

  // Status options
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
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadCurrentUser();
    this.loadProjects();
    this.loadApplications();
  }

  loadCurrentUser(): void {
    this.currentUser = this.authService.getCurrentUser();
    if (this.currentUser) {
      // Auto-fill chefDeProjetId with current user ID
      this.projectForm.chefDeProjetId = this.currentUser.id;
    }
  }

  loadProjects(): void {
    this.loading = true;
    this.projectService.getAllProjects().subscribe({
      next: (response: any) => {
        if (response.success) {
          // Filter projects to show only those where current user is chef de projet
          this.projects = response.data.filter((project: Project) => 
            project.chefDeProjetId === this.currentUser?.id
          );
          this.filteredProjects = [...this.projects];
        }
        this.loading = false;
      },
      error: (error: any) => {
        console.error('Error loading projects:', error);
        this.errorMessage = error.error?.message || 'Échec du chargement des projets';
        this.loading = false;
      }
    });
  }

  loadApplications(): void {
    this.nomenclatureService.getApplications().subscribe({
      next: (applications: any) => {
        this.applications = applications;
      },
      error: (error: any) => {
        console.error('Error loading applications:', error);
      }
    });
  }

  searchProjects(): void {
    if (!this.searchTerm.trim()) {
      this.filteredProjects = this.filterByStatus(this.projects);
      return;
    }

    const term = this.searchTerm.toLowerCase();
    this.filteredProjects = this.filterByStatus(this.projects).filter((project: Project) =>
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
    return projects.filter((project: Project) => project.status === this.filterStatus);
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
      chefDeProjetId: this.currentUser?.id || 0, // Auto-fill with current user
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
  // Check if project belongs to current user
  if (project.chefDeProjetId !== this.currentUser?.id) {
    this.errorMessage = 'Vous ne pouvez modifier que vos propres projets';
    return;
  }

  this.selectedProject = project;
  this.projectForm = {
    code: project.code,
    name: project.name,
    description: project.description || '',
    applicationId: project.applicationId,
    chefDeProjetId: project.chefDeProjetId, // Keep original chef de projet
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

// Add this method to handle slider changes
updateProgressValue(event: any): void {
  this.projectForm.progress = parseInt(event.target.value, 10);
}

  openDeleteModal(project: Project): void {
    // Check if project belongs to current user
    if (project.chefDeProjetId !== this.currentUser?.id) {
      this.errorMessage = 'Vous ne pouvez supprimer que vos propres projets';
      return;
    }
    
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

  // Ensure chefDeProjetId is current user
  this.projectForm.chefDeProjetId = this.currentUser?.id;
  
  // Always set status to PLANIFIE when creating (backend will auto-update)
  this.projectForm.status = 'PLANIFIE';

  this.loading = true;
  this.projectService.createProject(this.projectForm).subscribe({
    next: (response: any) => {
      if (response.success) {
        this.successMessage = 'Projet créé avec succès';
        this.loadProjects();
        this.closeModal();
      }
      this.loading = false;
    },
    error: (error: any) => {
      this.errorMessage = error.error?.message || 'Échec de la création du projet';
      this.loading = false;
    }
  });
}

updateProject(): void {
  if (!this.selectedProject || !this.validateProjectForm()) {
    return;
  }

  // For PLANIFIE or EN_COURS projects that are being manually changed to SUSPENDU/ANNULE
  // OR for SUSPENDU/ANNULE projects that are being changed
  // Send the status in the update
  const autoStatuses = ['PLANIFIE', 'EN_COURS', 'TERMINE'];
  const currentStatus = this.selectedProject.status;
  const newStatus = this.projectForm.status;
  
  // If changing from auto-status to manual status, send the status
  // If changing between manual statuses, send the status
  // If it's TERMINE, don't allow status changes
  if (currentStatus === 'TERMINE') {
    this.errorMessage = 'Un projet terminé ne peut pas être modifié';
    return;
  }
  
  this.loading = true;
  this.projectService.updateProject(this.selectedProject.id, this.projectForm).subscribe({
    next: (response: any) => {
      if (response.success) {
        this.successMessage = 'Projet mis à jour avec succès';
        this.loadProjects();
        this.closeModal();
      }
      this.loading = false;
    },
    error: (error: any) => {
      this.errorMessage = error.error?.message || 'Échec de la mise à jour du projet';
      this.loading = false;
    }
  });
}

  deleteProject(): void {
    if (!this.selectedProject) return;

    this.loading = true;
    this.projectService.deleteProject(this.selectedProject.id).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.successMessage = 'Projet supprimé avec succès';
          this.loadProjects();
          this.closeModal();
        }
        this.loading = false;
      },
      error: (error: any) => {
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
  
  // Validate dates logic
  if (this.projectForm.dateDebut && this.projectForm.dateFin) {
    const debut = new Date(this.projectForm.dateDebut);
    const fin = new Date(this.projectForm.dateFin);
    
    if (fin < debut) {
      this.errorMessage = 'La date de fin ne peut pas être antérieure à la date de début';
      return false;
    }
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

canReturnToAutoStatus(): boolean {
  if (!this.projectForm.dateDebut) return false;
  
  const today = new Date();
  const startDate = new Date(this.projectForm.dateDebut);
  
  // Always can return to auto-status if today is on or after start date
  if (today >= startDate) {
    return true;
  }
  
  // Check if there's an end date and we're before it
  if (this.projectForm.dateFin) {
    const endDate = new Date(this.projectForm.dateFin);
    return today <= endDate;
  }
  
  // If no end date but we're before start date, can't return to auto-status
  return false;
}

  getProgressClass(progress: number): string {
    if (progress >= 90) return 'bg-green-500';
    if (progress >= 70) return 'bg-blue-500';
    if (progress >= 50) return 'bg-yellow-500';
    if (progress >= 30) return 'bg-orange-500';
    return 'bg-red-500';
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  isChefProjet(): boolean {
    return this.authService.isChefProjet();
  }


  incrementProgress(project: Project): void {
  // Check if user is authorized to update this project
  const currentUser = this.authService.getCurrentUser();
  
  // If user is not admin and not the project's chef de projet, show error
  if (!this.isAdmin() && project.chefDeProjetId !== currentUser?.id) {
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
        
        // When progress reaches 100%, status becomes TERMINE automatically
        // (Backend handles this)
        
        // Refresh the projects list to get updated status from backend
        this.loadProjects();
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
    return this.projects.filter((p: Project) => p.status === 'EN_COURS').length;
  }

  getPlanifieCount(): number {
    return this.projects.filter((p: Project) => p.status === 'PLANIFIE').length;
  }

  getDelayedCount(): number {
    return this.projects.filter((p: Project) => p.isDelayed).length;
  }

  // Get current user full name
  getCurrentUserName(): string {
    if (!this.currentUser) return '';
    return `${this.currentUser.firstName} ${this.currentUser.lastName}`;
  }
}