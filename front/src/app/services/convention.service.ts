import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Convention {
  id: number;
  reference: string;
  libelle: string;
  dateDebut: string;
  dateFin: string;
  dateSignature: string;
  structure: any;
  gouvernorat: any;
  montantTotal: number;
  modalitesPaiement: string;
  periodicite: string;
  etat: string;
  createdAt: string;
  updatedAt: string;
}

export interface ConventionRequest {
  reference: string;
  libelle: string;
  dateDebut: string;
  dateFin: string;
  dateSignature: string;
  structureId: number;
  gouvernoratId: number;
  montantTotal: number;
  modalitesPaiement: string;
  periodicite: string;
  etat: string;
}

@Injectable({
  providedIn: 'root'
})
export class ConventionService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  // Get all conventions
  getAllConventions(): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/conventions`);
  }

  // Get convention by ID
  getConvention(id: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/conventions/${id}`);
  }

  // Create new convention
  createConvention(data: ConventionRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/api/conventions`, data);
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
}