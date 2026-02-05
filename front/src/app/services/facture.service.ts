import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Facture {
  id: number;
  numeroFacture: string;
  convention: any;
  dateFacturation: string;
  dateEcheance: string;
  montantHT: number;
  tva: number;
  montantTTC: number;
  statutPaiement: string;
  datePaiement: string;
  referencePaiement: string;
  notes: string;
  createdAt: string;
  updatedAt: string;
  structureInterneName: string;
  structureExterneName: string;
  conventionReference: string;
  conventionLibelle: string;
  conventionId: number;

  archived: boolean;
  enRetard: boolean;

   joursRetard?: number;
  joursRestants?: number;
  statutPaiementDetail?: string;
  statutPaiementColor?: string;

  paiementType?: string; 
  joursDetails?: string;
  joursNumber?: number;
 
}

export interface FactureRequest {
  conventionId: number;
  dateFacturation: string;
  dateEcheance: string;
  montantHT: number;
  tva: number;
  notes: string;
}

export interface PaiementRequest {
  factureId: number;
  referencePaiement: string;
  datePaiement: string;
}

@Injectable({
  providedIn: 'root'
})
export class FactureService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  // Get all invoices
  getAllFactures(): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/factures`);
  }

  // Get invoice by ID
  getFacture(id: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/factures/${id}`);
  }

  // Generate new invoice
  generateFacture(data: FactureRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/api/factures/generate`, data);
  }

  // Register payment
  registerPayment(data: PaiementRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/api/factures/payer`, data);
  }

  // Get invoices by convention
  getFacturesByConvention(conventionId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/factures/convention/${conventionId}`);
  }

  // Get invoices by status
  getFacturesByStatut(statut: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/factures/statut/${statut}`);
  }

  // Get overdue invoices
  getFacturesEnRetard(): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/factures/retard`);
  }

  // Get invoice statistics
  getStats(): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/factures/stats`);
  }
}