// src/app/services/stats.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface DashboardStats {
  success: boolean;
  data: {
    totalConventions: number;
    activeConventions: number;
    expiredConventions: number;
    terminatedConventions: number;
    conventionCompletionRate: number;
    totalFactures: number;
    paidFactures: number;
    unpaidFactures: number;
    overdueFactures: number;
    paymentRate: number;
    totalUsers: number;
    activeUsers: number;
    lockedUsers: number;
    userActivityRate: number;
    totalStructures: number;
    totalZones: number;
    totalApplications: number;
    totalRevenue: number;
    pendingRevenue: number;
    totalContractValue: number;
    revenueGrowth: number;
    recentActivity: {
      recentConventions: any[];
      recentInvoices: any[];
    };
    monthlyTrends: {
      conventionTrends: { [key: string]: number };
      revenueTrends: { [key: string]: number };
    };
  };
}

export interface ConventionDetailedStats {
  success: boolean;
  data: {
    statusDistribution: Array<{ name: string; count: number }>;
    monthlyConventions: { [key: string]: number };
    byStructureType: { [key: string]: number };
    amountByStatus: { [key: string]: number };
    topStructures: Array<{ structure: string; count: number }>;
  };
}

export interface FactureDetailedStats {
  success: boolean;
  data: {
    paymentStatus: Array<{ status: string; count: number; amount: number }>;
    monthlyAmounts: { [key: string]: { total: number; paid: number } };
    overdueDetails: Array<{
      numero: string;
      convention: string;
      montant: number;
      dateEcheance: string;
      joursRetard: number;
    }>;
    paymentMethods: { [key: string]: number };
  };
}

export interface UserDetailedStats {
  success: boolean;
  data: {
    roleDistribution: { [key: string]: number };
    departmentDistribution: { [key: string]: number };
    userActivity: { [key: string]: number };
    newUsersByMonth: { [key: string]: number };
    topActiveUsers: Array<{
      username: string;
      fullName: string;
      lastLogin: string;
      department: string;
      roles: string[];
    }>;
  };
}

export interface NomenclatureDetailedStats {
  success: boolean;
  data: {
    structuresByType: { [key: string]: number };
    conventionsByZone: { [key: string]: number };
    amountByZone: { [key: string]: number };
    topStructuresWithConventions: Array<{
      structure: string;
      type: string;
      conventionCount: number;
    }>;
    nomenclatureCounts: { [key: string]: number };
  };
}

export interface FinancialDetailedStats {
  success: boolean;
  data: {
    totalRevenue: number;
    pendingPayments: number;
    overdueAmount: number;
    totalContractValue: number;
    revenueByMonth: { [key: string]: number };
    topEarningConventions: Array<{
      reference: string;
      libelle: string;
      structure: string;
      montantTotal: number;
      etat: string;
    }>;
    collectionRate: number;
  };
}

export interface SummaryStats {
  success: boolean;
  data: {
    totalConventions: number;
    totalFactures: number;
    totalUsers: number;
    totalStructures: number;
    conventionsToday: number;
    facturesToday: number;
    dueToday: number;
  };
}

@Injectable({
  providedIn: 'root'
})
export class StatsService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  // ==================== DASHBOARD STATS ====================

  getDashboardStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.apiUrl}/api/stats/dashboard`);
  }

  getSummaryStats(): Observable<SummaryStats> {
    return this.http.get<SummaryStats>(`${this.apiUrl}/api/stats/summary`);
  }

  // ==================== DETAILED STATS ====================

  getConventionDetailedStats(): Observable<ConventionDetailedStats> {
    return this.http.get<ConventionDetailedStats>(`${this.apiUrl}/api/stats/conventions/detailed`);
  }

  getFactureDetailedStats(): Observable<FactureDetailedStats> {
    return this.http.get<FactureDetailedStats>(`${this.apiUrl}/api/stats/factures/detailed`);
  }

  getUserDetailedStats(): Observable<UserDetailedStats> {
    return this.http.get<UserDetailedStats>(`${this.apiUrl}/api/stats/users/detailed`);
  }

  getNomenclatureDetailedStats(): Observable<NomenclatureDetailedStats> {
    return this.http.get<NomenclatureDetailedStats>(`${this.apiUrl}/api/stats/nomenclatures/detailed`);
  }

  getFinancialDetailedStats(): Observable<FinancialDetailedStats> {
    return this.http.get<FinancialDetailedStats>(`${this.apiUrl}/api/stats/financial/detailed`);
  }

  // ==================== QUICK ACCESS METHODS ====================

  getCommercialStats() {
    return Promise.all([
      this.getConventionDetailedStats().toPromise(),
      this.getFactureDetailedStats().toPromise(),
      this.getSummaryStats().toPromise()
    ]);
  }

  getAdminStats() {
    return Promise.all([
      this.getDashboardStats().toPromise(),
      this.getConventionDetailedStats().toPromise(),
      this.getFactureDetailedStats().toPromise(),
      this.getUserDetailedStats().toPromise(),
      this.getNomenclatureDetailedStats().toPromise(),
      this.getFinancialDetailedStats().toPromise(),
      this.getSummaryStats().toPromise()
    ]);
  }
}