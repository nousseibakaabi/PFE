// request.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Request {
  id: number;
  requestType: string;
  status: string;
  
  requesterId: number;
  requesterName: string;
  requesterEmail: string;
  
  targetUserId?: number;
  targetUserName?: string;
  targetUserEmail?: string;
  
  applicationId?: number;
  applicationName?: string;
  applicationCode?: string;
  
  conventionId?: number;
  conventionReference?: string;
  conventionLibelle?: string;
  
  oldConventionId?: number;
  oldConventionReference?: string;
  
  recommendedChefId?: number;
  recommendedChefName?: string;
  recommendedChefEmail?: string;
  
  reason?: string;
  denialReason?: string;
  recommendations?: string;
  
  createdAt: string;
  processedAt?: string;
  timeAgo: string;
  
  statusColor: string;
  statusIcon: string;
  typeLabel: string;
}

export interface RequestAction {
  requestId: number;
  action: 'APPROVE' | 'DENY';
  reason?: string;
  recommendedChefId?: number;
  recommendations?: string;
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
}

@Injectable({
  providedIn: 'root'
})
export class RequestService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getUserRequests(): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/requests`);
  }

  getRequestsByStatus(status: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/requests/status/${status}`);
  }

  processRequest(action: RequestAction): Observable<any> {
    return this.http.post(`${this.apiUrl}/api/requests/process`, action);
  }
}