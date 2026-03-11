import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

// In convention.service.ts
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
  nbUsers : number;
  montantHT : number;
  montantTTC : number;
  tva : number;
  
  structureBeneficielId: number;
  structureBeneficielName: string;
  structureBeneficielCode: string;
  structureBeneficielEmail: string;
  structureBeneficielPhone: string;
  
  structureResponsableId: number;
  structureResponsableName: string;
  structureResponsableCode: string;
  structureResponsableEmail: string;
  structureResponsablePhone: string;
  
  
  // These are the zone properties from your mapper
  zoneId: number;
  zoneName: string;
  zoneCode: string;
  
  applicationId: number;
  applicationName: string;
  applicationCode: string;
  applicationClientName: string;
  applicationDateFin: string;
  applicationDateDebut: string;
  minUser?: number;
  maxUser?: number;
  
  renewalVersion?: number;
  
  // Chef de projet info
  chefDeProjetId?: number;
  chefDeProjetName?: string;
  
  // Invoices array if needed
  factures?: any[];
  totalFactures?: number;
  facturesPayees?: number;
  facturesNonPayees?: number;
  facturesEnRetard?: number;
}

export interface ConventionRequest {
  referenceConvention: string;
  referenceERP: string;
  libelle: string;
  dateDebut: string;
  dateFin: string;
  dateSignature: string;
  structureResponsableId: number;
  structureBeneficielId: number;
 applicationId: number; 
  periodicite: string;
  nbUsers : number;
  montantHT : number;
  montantTTC : number;
  tva : number;

}


export interface RenewalRequest {
  referenceERP: string;
  libelle: string;
  dateDebut: string;
  dateFin: string;
  dateSignature: string;
  montantHT: number;
  tva: number;
  montantTTC: number;
  nbUsers: number;
  periodicite: string;
  structureResponsableId?: number;
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


  calculateTTC(montantHT: number, tva: number = 19): Observable<any> {
    const params = new URLSearchParams();
    params.set('montantHT', montantHT.toString());
    params.set('tva', tva.toString());
    return this.http.post(`${this.apiUrl}/api/conventions/calculate-ttc?${params.toString()}`, {});
  }


  determineNbUsers(applicationId: number, selectedUsers?: number): Observable<any> {
    const params = new URLSearchParams();
    params.set('applicationId', applicationId.toString());
    if (selectedUsers !== undefined && selectedUsers !== null) {
      params.set('selectedUsers', selectedUsers.toString());
    }
    return this.http.post(`${this.apiUrl}/api/conventions/determine-nb-users?${params.toString()}`, {});
  }


  syncApplicationDates(applicationId: number): Observable<any> {
  return this.http.post(`${this.apiUrl}/api/conventions/applications/${applicationId}/sync-dates`, {});
  }


  getConventionsByApplication(applicationId: number): Observable<any> {
  return this.http.get(`${this.apiUrl}/api/conventions/by-application/${applicationId}`);
  }

  renewConvention(id: number, data: RenewalRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/api/conventions/${id}/renew`, data);
  }


  getArchivedConventionsByApplication(applicationId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/conventions/application/${applicationId}/archived`);
  }


  getPreviousVersions(id: number): Observable<any> {
  return this.http.get(`${this.apiUrl}/api/conventions/${id}/previous-versions`);
  }

  getOldConventionById(oldId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/conventions/old/${oldId}`);
  }

  
}