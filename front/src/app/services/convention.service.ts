import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Convention {
  id: number;
  referenceConvention: string;
  referenceERP: string;
  libelle: string;
  dateDebut: string;
  dateFin: string;
  dateSignature: string;
  montantTotal: number;
  periodicite: string;
  etat: string;
  archived: boolean;
  archivedAt: string;
  archivedBy: string;
  archivedReason: string;
  createdAt: string;
  updatedAt: string;
  
  structureInterneId: number;
  structureInterneName: string;
  structureInterneCode: string;
  
  structureExterneId: number;
  structureExterneName: string;
  structureExterneCode: string;
  
  zoneId: number;
  zoneName: string;
  zoneCode: string;
  
  applicationId: number;
  applicationName: string;
  applicationCode: string;
  
  // Invoices array if needed
  factures?: any[];
  totalFactures?: number;
  facturesPayees?: number;
  facturesNonPayees?: number;
  facturesEnRetard?: number;

  projectId: number;
  projectName: string;
  projectCode: string;
}

export interface ConventionRequest {
  referenceConvention: string;
  referenceERP: string;
  libelle: string;
  dateDebut: string;
  dateFin: string;
  dateSignature: string;
  structureInterneId: number;
  structureExterneId: number;
  zoneId: number;
  projectId: number; 
  montantTotal: number;
  periodicite: string;
}

export interface ArchiveConventionRequest {
  reason: string;
}

@Injectable({
  providedIn: 'root'
})
export class ConventionService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getAllConventions(showArchived: boolean = false): Observable<any> {
    const params = showArchived ? `?showArchived=${showArchived}` : '';
    return this.http.get(`${this.apiUrl}/api/conventions${params}`);
  }

  // Get only active conventions
  getActiveConventions(): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/conventions/active`);
  }

  // Get archived conventions
  getArchivedConventions(): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/conventions/archives`);
  }

  // Archive a convention
  archiveConvention(id: number, reason: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/api/conventions/${id}/archive`, { reason });
  }

  // Restore an archived convention
  restoreConvention(id: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/api/conventions/${id}/restore`, {});
  }

  // Get convention by ID
  getConvention(id: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/conventions/${id}`);
  }

 

  // Update convention
  updateConvention(id: number, data: ConventionRequest): Observable<any> {
    return this.http.put(`${this.apiUrl}/api/conventions/${id}`, data);
  }

  // Delete convention
  deleteConvention(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/api/conventions/${id}`);
  }

  // Get conventions by structure
  getConventionsByStructure(structureId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/conventions/structure/${structureId}`);
  }

  // Get conventions by status
  getConventionsByEtat(etat: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/conventions/etat/${etat}`);
  }

  // Get expired conventions
  getConventionsExpirees(): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/conventions/expirees`);
  }


getSuggestedReference(): Observable<any> {
  return this.http.get(`${this.apiUrl}/api/conventions/generate-reference`);
}


   // Create new convention
  createConvention(data: ConventionRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/api/conventions`, data);
  }

  
}