import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

// src/app/services/stats.service.ts - Update DashboardStats interface

export interface DashboardStats {
  success: boolean;
  userRole?: string;
  data: {
    // Convention stats
    totalConventions: number;
    activeConventions: number;
    planifiedConventions: number;
    terminatedConventions: number;
    archivedConventions: number;
    conventionCompletionRate: number;
    
    // Invoice stats
    totalFactures: number;
    paidFactures: number;
    unpaidFactures: number;
    overdueFactures: number;
    paymentRate: number;
    totalPaidAmount: number;
    totalUnpaidAmount: number;
    
    // Application stats
    totalApplications: number;
    activeApplications: number;
    plannedApplications: number;
    completedApplications: number;
    averageProgress: number;
    applicationCompletionRate: number;
    
    // User stats
    totalUsers?: number;
    activeUsers?: number;
    chefDeProjetCount?: number;
    commercialMetierCount?: number;
    decideurCount?: number;
    adminCount?: number;
    
    // Nomenclature stats
    structuresResponsable?: number;
    structuresBeneficiaire?: number;
    totalStructures?: number;
    tunisianZones?: number;
    customZones?: number;
    totalZones?: number;
    
    // Financial stats
    totalRevenue: number;
    pendingRevenue: number;
    totalContractValue: number;
    
    // Recent activity
    recentActivity?: {
      recentApplications: any[];
      recentConventions: any[];
      recentFactures: any[];
    };
    
    // Monthly trends
    monthlyTrends?: {
      applicationTrends: { [key: string]: number };
      conventionTrends: { [key: string]: number };
      revenueTrends: { [key: string]: number };
    };
  };
}

export interface UserDetailedStats {
  success: boolean;
  data: {
    roleDistribution: { [key: string]: number };
    userActivity: {
      total: number;
      active: number;
      locked: number;
      inactive: number;
    };
    usersByRole: {
      chefDeProjet: number;
      commercialMetier: number;
      decideur: number;
      admin: number;
    };
  };
}

export interface NomenclatureDetailedStats {
  success: boolean;
  userRole?: string;
  data: {
    structuresByType: { [key: string]: number };
    structuresResponsable: number;
    structuresBeneficiaire: number;
    zonesByType: { [key: string]: number };
    tunisianZones: number;
    customZones: number;
    nomenclatureCounts: {
      applications: number;
      zones: number;
      structures: number;
      structuresResponsable: number;
      structuresBeneficiaire: number;
    };
  };
}

export interface ConventionDetailedStats {
  success: boolean;
  userRole?: string;
  data: {
    statusDistribution: Array<{ name: string; count: number }>;
    monthlyConventions: { [key: string]: number };
    amountByStatus: { [key: string]: number };
    topStructures: Array<{ structure: string; count: number }>;
  };
}

export interface FactureDetailedStats {
  success: boolean;
  userRole?: string;
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
    topConventionAmounts: Array<{
      convention: string;
      totalAmount: number;
      etat: string;
    }>;
  };
}

export interface ApplicationDetailedStats {
  success: boolean;
  userRole?: string;
  data: {
    statusDistribution: Array<{ name: string; count: number }>;
    monthlyApplications: { [key: string]: number };
    applicationsByChef: Array<{ chef: string; count: number }>;
    unassignedApplications: number;
    progressStats: {
      averageProgress: number;
      onTrackApplications: number;
      delayedApplications: number;
    };
  };
}

export interface FinancialDetailedStats {
  success: boolean;
  userRole?: string;
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
  userRole?: string;
  data: {
    totalConventions: number;
    totalFactures: number;
    totalApplications: number;
    conventionsToday: number;
    facturesToday: number;
    dueToday: number;
    overdueToday: number;
    todayRevenue: number;
  };
}

export interface OverdueAlert {
  type: string;
  message: string;
  convention?: string;
  project?: string;
  application?: string;
  code?: string;
  amount?: number;
  dueDate?: string;
  endDate?: string;
  progress?: number;
  priority: string;
}

export interface OverdueAlertsResponse {
  success: boolean;
  userRole?: string;
  data: OverdueAlert[];
  count: number;
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

  getApplicationDetailedStats(): Observable<ApplicationDetailedStats> {
    return this.http.get<ApplicationDetailedStats>(`${this.apiUrl}/api/stats/applications/detailed`);
  }

  getFinancialDetailedStats(): Observable<FinancialDetailedStats> {
    return this.http.get<FinancialDetailedStats>(`${this.apiUrl}/api/stats/financial/detailed`);
  }

  getUserDetailedStats(): Observable<UserDetailedStats> {
    return this.http.get<UserDetailedStats>(`${this.apiUrl}/api/stats/users/detailed`);
  }

  getNomenclatureDetailedStats(): Observable<NomenclatureDetailedStats> {
    return this.http.get<NomenclatureDetailedStats>(`${this.apiUrl}/api/stats/nomenclatures/detailed`);
  }

  // ==================== ALERTS ====================

  getOverdueAlerts(): Observable<OverdueAlertsResponse> {
    return this.http.get<OverdueAlertsResponse>(`${this.apiUrl}/api/stats/overdue/alert`);
  }

  // ==================== ROLE-BASED STATS LOADING ====================

  getStatsByUserRole(userRole: string): Promise<any[]> {
    switch (userRole) {
      case 'ADMIN':
        return this.getAdminStats();
      case 'DECIDEUR':
        return this.getDecideurStats();
      case 'CHEF_PROJET':
        return this.getChefProjetStats();
      case 'COMMERCIAL_METIER':
        return this.getCommercialStats();
      default:
        return Promise.resolve([]);
    }
  }

  public getCommercialStats() {
    return Promise.all([
      this.getDashboardStats().toPromise(),
      this.getConventionDetailedStats().toPromise(),
      this.getFactureDetailedStats().toPromise(),
      this.getFinancialDetailedStats().toPromise(),
      this.getSummaryStats().toPromise(),
      this.getOverdueAlerts().toPromise()
    ]);
  }

  public getChefProjetStats() {
    return Promise.all([
      this.getDashboardStats().toPromise(),
      this.getConventionDetailedStats().toPromise(),
      this.getFactureDetailedStats().toPromise(),
      this.getApplicationDetailedStats().toPromise(),
      this.getFinancialDetailedStats().toPromise(),
      this.getSummaryStats().toPromise(),
      this.getOverdueAlerts().toPromise()
    ]);
  }

  public getDecideurStats() {
    return Promise.all([
      this.getDashboardStats().toPromise(),
      this.getConventionDetailedStats().toPromise(),
      this.getFactureDetailedStats().toPromise(),
      this.getApplicationDetailedStats().toPromise(),
      this.getFinancialDetailedStats().toPromise(),
      this.getSummaryStats().toPromise(),
      this.getOverdueAlerts().toPromise()
    ]);
  }

  public getAdminStats() {
    return Promise.all([
     this.getDashboardStats().toPromise(),           // index 0
    this.getConventionDetailedStats().toPromise(),  // index 1
    this.getFactureDetailedStats().toPromise(),     // index 2
    this.getUserDetailedStats().toPromise(),        // index 3
    this.getNomenclatureDetailedStats().toPromise(),// index 4
    this.getFinancialDetailedStats().toPromise(),   // index 5
    this.getSummaryStats().toPromise(),             // index 6
    this.getApplicationDetailedStats().toPromise()  // index 7
    ]);
  }
}