// src/app/services/application.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Application {
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
  status: string;
  createdAt: string;
  updatedAt: string;
  minUser : number;
  maxUser:number;
  chefDeProjetProfileImage?: string;
  
  // Calculated fields
  daysRemaining: number;
  totalDays: number;
  daysElapsed: number;
  timeBasedProgress: number;
  isDelayed: boolean;
  statusColor: string;
  dateRange: string;
  timeRemainingString: string;
  
  // Related entity info
  chefDeProjetId: number;
  chefDeProjetUsername: string;
  chefDeProjetFullName: string;
  chefDeProjetEmail: string;
  
  // Statistics
  conventionsCount: number;
  recentConventions: any[];


  terminatedAt?: string;
  terminatedBy?: string;
  terminationReason?: string;
  daysRemainingAtTermination?: number;
  terminationInfo?: string;
  terminatedEarly?: boolean;
  terminatedOnTime?: boolean;
  terminatedLate?: boolean;
}

export interface ApplicationRequest {
  code: string;
  name: string;
  description: string;
  chefDeProjetId: number | null;
  clientName: string;
  clientEmail?: string;
  clientPhone?: string;
  dateDebut?: string;
  dateFin?: string;
  status?: string;
  minUser : number;
  maxUser:number;
}


// In application.service.ts - Update the ApiResponse interface

export interface ApiResponse {
  success: boolean;
  message?: string;
  data?: any;
  applications?: Application[];
  // Add terminationInfo property
  terminationInfo?: {
    terminatedAt: string;
    terminatedBy: string;
    daysRemaining: number;
    isEarly: boolean;
    isOnTime: boolean;
    isLate: boolean;
    info: string;
  };
}

@Injectable({
  providedIn: 'root'
})
export class ApplicationService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}



// Update the methods that expect success property:
getAllApplications(): Observable<ApiResponse> {
  return this.http.get<ApiResponse>(`${this.apiUrl}/api/applications`);
}

getApplication(id: number): Observable<ApiResponse> {
  return this.http.get<ApiResponse>(`${this.apiUrl}/api/applications/${id}`);
}

createApplication(data: ApplicationRequest): Observable<ApiResponse> {
  return this.http.post<ApiResponse>(`${this.apiUrl}/api/applications`, data);
}

updateApplication(id: number, data: ApplicationRequest): Observable<ApiResponse> {
  return this.http.put<ApiResponse>(`${this.apiUrl}/api/applications/${id}`, data);
}

deleteApplication(id: number): Observable<ApiResponse> {
  return this.http.delete<ApiResponse>(`${this.apiUrl}/api/applications/${id}`);
}

  // Get applications by Chef de Projet
  getApplicationsByChefDeProjet(chefDeProjetId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/applications/chef-projet/${chefDeProjetId}`);
  }

  // Search applications
  searchApplications(filters: any): Observable<any> {
    let params = new URLSearchParams();
    
    for (const key in filters) {
      if (filters[key] !== null && filters[key] !== undefined && filters[key] !== '') {
        params.set(key, filters[key].toString());
      }
    }
    
    return this.http.get(`${this.apiUrl}/api/applications/search?${params.toString()}`);
  }

  // Calculate application status automatically
  calculateApplicationStatus(id: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/api/applications/${id}/calculate-status`, {});
  }

  // Get application dashboard
  getApplicationDashboard(): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/applications/dashboard`);
  }

  // Get or create client structure for application
  getClientStructureForApplication(applicationId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/applications/${applicationId}/client-structure`);
  }

  getUnassignedApplications(): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/applications/unassigned`);
  }


  getSuggestedApplicationCode(): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/applications/generate-code`);
  }

  getConventionsByChefDeProjet(chefDeProjetId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/applications/conventions/${chefDeProjetId}`);
  }


  getApplicationDateSummary(applicationId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/applications/${applicationId}/date-summary`);
  }

  getApplicationsWithoutConventions(): Observable<ApiResponse> {
  return this.http.get<ApiResponse>(`${this.apiUrl}/api/applications/without-conventions`);
  }


  manuallyTerminateApplication(id: number, reason?: string): Observable<ApiResponse> {
    const body = reason ? { reason } : {};
    return this.http.post<ApiResponse>(`${this.apiUrl}/api/applications/${id}/terminate`, body);
  }

  checkCanTerminate(application: Application): boolean {
    return application.status === 'PLANIFIE' || application.status === 'EN_COURS';
  }

  updateApplicationWithStatusHandling(id: number, data: ApplicationRequest): Observable<ApiResponse> {
    // If status is being set to TERMINE, use the terminate endpoint
    if (data.status === 'TERMINE') {
      return this.manuallyTerminateApplication(id, 'Terminé via formulaire');
    }
    
    // Otherwise use regular update
    return this.updateApplication(id, data);
  }


  getArchivedApplications(): Observable<ApiResponse> {
  return this.http.get<ApiResponse>(`${this.apiUrl}/api/applications/archived`);
}



}