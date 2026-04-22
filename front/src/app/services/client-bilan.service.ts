import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface ClientBilan {
  clientId: number;
  clientCode: string;
  clientName: string;
  clientEmail: string;
  clientPhone: string;
  clientType: string;
  bilanStartDate: string;
  bilanEndDate: string;
  generatedAt: string;
  summary: SummaryStats;
  conventions: ConventionBilan[];
  paymentStats: PaymentStats;
  financialSummary: FinancialSummary;
  rating: ClientRating;
  recommendations: string[];
}

export interface SummaryStats {
  totalConventions: number;
  activeConventions: number;
  terminatedConventions: number;
  archivedConventions: number;
  totalApplications: number;
  applicationNames: string[];
}

export interface ConventionBilan {
  conventionId: number;
  referenceConvention: string;
  libelle: string;
  applicationName: string;
  applicationCode: string;
  dateDebut: string;
  dateFin: string;
  etat: string;
  montantHT: number;
  montantTTC: number;
  nbUsers: number;
  periodicite: string;
  invoiceStats: InvoiceStats;
  paymentHistory: PaymentRecord[];
  latePaymentDetails: LatePaymentDetails;
}

export interface InvoiceStats {
  totalInvoices: number;
  paidInvoices: number;
  unpaidInvoices: number;
  lateInvoices: number;
  paidOnTimeInvoices: number;
  paidLateInvoices: number;
  totalAmount: number;
  paidAmount: number;
  unpaidAmount: number;
  lateAmount: number;
  paymentRate: number;
  onTimePaymentRate: number;
}

export interface PaymentRecord {
  invoiceId: number;
  invoiceNumber: string;
  invoiceDate: string;
  dueDate: string;
  paymentDate: string | null;
  amount: number;
  paymentStatus: string;
  paymentReference: string | null;
  daysLate: number | null;
  paymentTiming: string;
}

export interface LatePaymentDetails {
  totalLatePayments: number;
  averageDaysLate: number;
  maxDaysLate: number;
  minDaysLate: number;
  worstLatePayments: LatePaymentRecord[];
  lateByPeriodicite: { [key: string]: number };
}

export interface LatePaymentRecord {
  invoiceNumber: string;
  dueDate: string;
  paymentDate: string;
  daysLate: number;
  amount: number;
  conventionReference: string;
}

export interface PaymentStats {
  totalPayments: number;
  onTimePayments: number;
  latePayments: number;
  advancePayments: number;
  onTimePercentage: number;
  latePercentage: number;
  advancePercentage: number;
  paymentBehavior: string;
  behaviorDescription: string;
}

export interface FinancialSummary {
  totalContractValue: number;
  totalPaid: number;
  totalUnpaid: number;
  totalOverdue: number;
  paymentComplianceRate: number;
  yearlyTotal: { [key: number]: number };
  yearlyPaid: { [key: number]: number };
  yearlyUnpaid: { [key: number]: number };
}

export interface ClientRating {
  overallScore: number;
  rating: string;
  ratingLabel: string;
  paymentScore: number;
  contractComplianceScore: number;
  activityScore: number;
  strengths: string[];
  weaknesses: string[];
}

export interface Structure {
  id: number;
  code: string;
  name: string;
  email: string;
  phone: string;
  typeStructure: string;
}

@Injectable({
  providedIn: 'root'
})
export class ClientBilanService {
  private apiUrl = `${environment.apiUrl}/api/client-bilan`;

  constructor(private http: HttpClient) {}

  getClientBilan(clientId: number, startDate?: string, endDate?: string): Observable<ClientBilan> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get<ClientBilan>(`${this.apiUrl}/client/${clientId}`, { params });
  }

  getAllClientsBilan(startDate?: string, endDate?: string): Observable<ClientBilan[]> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get<ClientBilan[]>(`${this.apiUrl}/all`, { params });
  }

  getPaginatedClients(page: number, size: number, search?: string): Observable<any> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    if (search) params = params.set('search', search);
    return this.http.get<any>(`${this.apiUrl}/paginated`, { params });
  }

  getClientPaymentStats(clientId: number): Observable<PaymentStats> {
    return this.http.get<PaymentStats>(`${this.apiUrl}/client/${clientId}/payment-stats`);
  }

  getPoorPayers(minLatePayments: number, minDaysLate: number): Observable<ClientBilan[]> {
    let params = new HttpParams()
      .set('minLatePayments', minLatePayments.toString())
      .set('minDaysLate', minDaysLate.toString());
    return this.http.get<ClientBilan[]>(`${this.apiUrl}/poor-payers`, { params });
  }

  getBilanSummary(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/summary`);
  }

  exportToPdf(clientId: number, startDate?: string, endDate?: string): Observable<Blob> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get(`${this.apiUrl}/export-pdf/${clientId}`, { params, responseType: 'blob' });
  }

  exportToExcel(clientId: number, startDate?: string, endDate?: string): Observable<Blob> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get(`${this.apiUrl}/export-excel/${clientId}`, { params, responseType: 'blob' });
  }

  getStructuresBeneficiel(): Observable<Structure[]> {
    // The API might return an object with a 'data' property or directly an array
    return this.http.get<any>(`${environment.apiUrl}/admin/nomenclatures/structures/beneficiels`).pipe(
      map(response => {
        // Check if response has a 'data' property (common pattern)
        if (response && response.data && Array.isArray(response.data)) {
          return response.data;
        }
        // If response is directly an array
        if (Array.isArray(response)) {
          return response;
        }
        // If response has a different structure, log and return empty array
        console.warn('Unexpected response structure:', response);
        return [];
      })
    );
  }
}