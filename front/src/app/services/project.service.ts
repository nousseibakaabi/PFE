// src/app/services/project.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Project {
  id: number;
  code: string;
  name: string;
  description: string;
  clientName: string;
  clientEmail: string;
  clientPhone: string;
  clientAddress: string;
  dateDebut: string;
  dateFin: string;
  progress: number;
  budget: number;
  status: string;
  createdAt: string;
  updatedAt: string;
  
  // Calculated fields
  daysRemaining: number;
  totalDays: number;
  daysElapsed: number;
  timeBasedProgress: number;
  isDelayed: boolean;
  statusColor: string;
  progressColor: string;
  dateRange: string;
  timeRemainingString: string;
  
  // Related entity info
  applicationId: number;
  applicationName: string;
  applicationCode: string;
  
  chefDeProjetId: number;
  chefDeProjetUsername: string;
  chefDeProjetFullName: string;
  chefDeProjetEmail: string;
  
  // Statistics
  conventionsCount: number;
  recentConventions: any[];
}

export interface ProjectRequest {
  code: string;
  name: string;
  description: string;
  applicationId: number;
  chefDeProjetId?: number;
  clientName: string;
  clientEmail?: string;
  clientPhone?: string;
  clientAddress?: string;
  dateDebut?: string;
  dateFin?: string;
  progress?: number;
  budget?: number;
  status?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ProjectService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

getAllProjects(): Observable<any> {
  return this.http.get(`${this.apiUrl}/api/projects`);
}

  // Get project by ID
  getProject(id: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/projects/${id}`);
  }

  // Create new project
  createProject(data: ProjectRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/api/projects`, data);
  }

  // Update project
  updateProject(id: number, data: ProjectRequest): Observable<any> {
    return this.http.put(`${this.apiUrl}/api/projects/${id}`, data);
  }

  // Delete project
  deleteProject(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/api/projects/${id}`);
  }

  // Get projects by Chef de Projet
  getProjectsByChefDeProjet(chefDeProjetId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/projects/chef-projet/${chefDeProjetId}`);
  }

  // Search projects
  searchProjects(filters: any): Observable<any> {
    let params = new URLSearchParams();
    
    for (const key in filters) {
      if (filters[key] !== null && filters[key] !== undefined && filters[key] !== '') {
        params.set(key, filters[key].toString());
      }
    }
    
    return this.http.get(`${this.apiUrl}/api/projects/search?${params.toString()}`);
  }

  // Update project progress
  updateProjectProgress(id: number, progress: number): Observable<any> {
    return this.http.put(`${this.apiUrl}/api/projects/${id}/progress`, null, {
      params: { progress: progress.toString() }
    });
  }

  // Calculate project progress automatically
  calculateProjectProgress(id: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/api/projects/${id}/calculate-progress`, {});
  }

  // Get project dashboard
  getProjectDashboard(): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/projects/dashboard`);
  }


  // Add this method to your existing ProjectService
getClientStructureForProject(projectId: number): Observable<any> {
  return this.http.get(`${this.apiUrl}/api/projects/${projectId}/client-structure`);
}


getUnassignedProjects(): Observable<any> {
  return this.http.get(`${this.apiUrl}/api/projects/unassigned`);
}

// Assign chef de projet to project
assignChefDeProjet(projectId: number, chefDeProjetId: number): Observable<any> {
  return this.http.put(`${this.apiUrl}/api/projects/${projectId}/assign-chef/${chefDeProjetId}`, {});
}


getSuggestedProjectCode(): Observable<any> {
  return this.http.get(`${this.apiUrl}/api/projects/generate-code`);
}

}