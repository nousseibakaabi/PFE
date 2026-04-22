import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface BilanSummary {
  totalConventions: number;
  activeConventions: number;
  terminatedConventions: number;
  archivedConventions: number;
  totalMontantHT: number;
  totalMontantTTC: number;
  totalTVA: number;
  totalPaid: number;
  totalUnpaid: number;
  totalOverdue: number;
  totalInvoices: number;
  paidInvoices: number;
  unpaidInvoices: number;
  overdueInvoices: number;
  paymentRate: number;
}

export interface InvoiceBilanItem {
  id: number;
  numeroFacture: string;
  dateFacturation: string;
  dateEcheance: string;
  montantHT: number;
  montantTTC: number;
  statutPaiement: string;
  datePaiement: string | null;
  referencePaiement: string | null;
  overdue: boolean;
  paiementType: string;
  joursRetard: number | null;
  tva: number;
}




export interface InvoiceSummary {
  total: number;
  paid: number;
  unpaid: number;
  overdue: number;
  totalAmount: number;
  paidAmount: number;
  unpaidAmount: number;
  paymentRate: number;
}

export interface BilanItem {
  id: number;
  reference: string;
  libelle: string;
  type: string;
  etat: string;
  startDate: string;
  endDate: string;
  montantHT: number;
  montantTTC: number;
  tva: number;
  nbUsers: number;
  periodicite: string;
  renewalVersion: number;
  invoices: InvoiceBilanItem[];
  invoiceSummary: InvoiceSummary;
  structureResponsable: string;
  structureBeneficiel: string;
  applicationName: string;
  statutPaiement?: string;
}

export interface BilanDTO {
  title: string;
  periodType: string;
  year: number;
  month: number;
  startDate: string;
  endDate: string;
  summary: BilanSummary;
  items: BilanItem[];
}

@Injectable({
  providedIn: 'root'
})
export class BilanService {
  private apiUrl = `${environment.apiUrl}/bilan`;

  constructor(private http: HttpClient) {}

  getFacturesBilan(startDate: string, endDate: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/factures`, { params: { startDate, endDate } });
  }

  getConventionsBilan(startDate: string, endDate: string, includeOldVersions: boolean): Observable<any> {
    return this.http.get(`${this.apiUrl}/conventions`, { 
      params: { startDate, endDate, includeOldVersions: String(includeOldVersions) }
    });
  }

  getCombinedBilan(startDate: string, endDate: string, includeOldVersions: boolean): Observable<any> {
    return this.http.get(`${this.apiUrl}/combined`, { 
      params: { startDate, endDate, includeOldVersions: String(includeOldVersions) }
    });
  }

  getConventionBilan(conventionId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/convention/${conventionId}`);
  }

  getBilanByMonth(year: number, month: number, type: string, includeOldVersions: boolean): Observable<any> {
    return this.http.get(`${this.apiUrl}/month`, {
      params: { year, month, type, includeOldVersions: String(includeOldVersions) }
    });
  }

  getBilanByYear(year: number, type: string, includeOldVersions: boolean): Observable<any> {
    return this.http.get(`${this.apiUrl}/year`, {
      params: { year, type, includeOldVersions: String(includeOldVersions) }
    });
  }
}