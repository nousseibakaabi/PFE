// src/app/services/stats.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface DashboardStats {
  success: boolean;
  userRole?: string; // Add user role from backend
  data: {
    // Convention stats
    totalConventions: number;
    activeConventions: number;
    terminatedConventions: number; // Changed from expiredConventions
    lateConventions: number; // New field
    noStatusConventions: number; // New field
    conventionCompletionRate: number;
    
    // Invoice stats
    totalFactures: number;
    paidFactures: number;
    unpaidFactures: number;
    overdueFactures: number;
    paymentRate: number;
    totalPaidAmount: number; // New field
    totalUnpaidAmount: number; // New field
    
    // User stats (only for admin)
    totalUsers?: number; // Optional now
    activeUsers?: number;
    lockedUsers?: number;
    chefDeProjetCount?: number;
    commercialMetierCount?: number;
    decideurCount?: number;
    
    // Nomenclature stats (only for admin)
    totalStructures?: number;
    totalZones?: number;
    totalApplications?: number;
    
    // Financial stats
    totalRevenue: number;
    pendingRevenue: number; // Changed from pendingPayments
    totalContractValue: number;
    totalProjectBudget: number;
    
    // Project stats
    totalProjects: number;
    activeProjects: number;
    plannedProjects: number;
    completedProjects: number;
    delayedProjects: number;
    averageProgress: number;
    projectCompletionRate: number;
    
    
    
    // Recent activity
    recentActivity?: {
      recentConventions: any[];
      recentInvoices: any[];
      recentProjects: any[];
    };
    
    // Monthly trends
    monthlyTrends?: {
      conventionTrends: { [key: string]: number };
      revenueTrends: { [key: string]: number };
     
    };
  };
}

export interface ConventionDetailedStats {
  success: boolean;
  userRole?: string; // Add user role
  data: {
    statusDistribution: Array<{ name: string; count: number }>;
    monthlyConventions: { [key: string]: number };
    amountByStatus: { [key: string]: number };
    topStructures: Array<{ structure: string; count: number }>;
  };
}

export interface FactureDetailedStats {
  success: boolean;
  userRole?: string; // Add user role
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
    topConventionAmounts: Array<{ // NEW FIELD
      convention: string;
      totalAmount: number;
    }>;
    
  };
}

export interface UserDetailedStats {
  success: boolean;
  data: {
    roleDistribution: { [key: string]: number };
    userActivity: { [key: string]: number };
  };
}

export interface NomenclatureDetailedStats {
  success: boolean;
  data: {
    structuresByType: { [key: string]: number };
    nomenclatureCounts: { [key: string]: number };
  };
}

export interface FinancialDetailedStats {
  success: boolean;
  userRole?: string; // Add user role
  data: {
    totalRevenue: number;
    pendingPayments: number;
    overdueAmount: number;
    totalContractValue: number;
    revenueByMonth: { [key: string]: number };
    topEarningConventions: Array<{
      reference: string;
      libelle: string;
      structureInterne: string; // Changed from structure
      montantTotal: number;
      etat: string;
    }>;
    collectionRate: number;
  };
}

export interface ProjectDetailedStats {
  success: boolean;
  userRole?: string; // Add user role
  data: {
    statusDistribution: Array<{ name: string; count: number }>;
    monthlyProjects: { [key: string]: number };
    projectsByApplication: { [key: string]: number };
    unassignedProjects: number;
    topApplications: Array<{ application: string; count: number }>;
    budgetStats: {
      totalBudget: number;
      averageBudget: number;
      
    };
    progressStats: {
      averageProgress: number;
      onTrackProjects: number;
      delayedProjects: number;
    };
    
  };
}

export interface SummaryStats {
  success: boolean;
  userRole?: string; // Add user role
  data: {
    totalConventions: number;
    totalFactures: number;
    totalProjects: number;
    conventionsToday: number;
    facturesToday: number;
    dueToday: number;
    overdueToday: number; // New field
    todayRevenue: number; // New field
  
  };
}


export interface OverdueAlert {
  success: boolean;
  userRole?: string;
  data: Array<{
    type: string;
    message: string;
    convention?: string;
    project?: string;
    amount?: number;
    dueDate?: string;
    priority: string;
    code?: string;
    progress?: number;
    endDate?: string;
  }>;
  count: number;
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

  getUserDetailedStats(): Observable<UserDetailedStats> {
    return this.http.get<UserDetailedStats>(`${this.apiUrl}/api/stats/users/detailed`);
  }

  getNomenclatureDetailedStats(): Observable<NomenclatureDetailedStats> {
    return this.http.get<NomenclatureDetailedStats>(`${this.apiUrl}/api/stats/nomenclatures/detailed`);
  }

  getFinancialDetailedStats(): Observable<FinancialDetailedStats> {
    return this.http.get<FinancialDetailedStats>(`${this.apiUrl}/api/stats/financial/detailed`);
  }

  getProjectDetailedStats(): Observable<ProjectDetailedStats> {
    return this.http.get<ProjectDetailedStats>(`${this.apiUrl}/api/stats/projects/detailed`);
  }

  // ==================== NEW ENDPOINTS ====================

  getOverdueAlerts(): Observable<OverdueAlertsResponse> {
    return this.http.get<OverdueAlertsResponse>(`${this.apiUrl}/api/stats/overdue/alert`);
  }

  // ==================== QUICK ACCESS METHODS ====================

  getCommercialStats() {
    return Promise.all([
      this.getConventionDetailedStats().toPromise(),
      this.getFactureDetailedStats().toPromise(),
      this.getFinancialDetailedStats().toPromise(),
      this.getProjectDetailedStats().toPromise(),
      this.getSummaryStats().toPromise(),
      this.getOverdueAlerts().toPromise()
    ]);
  }

  getChefProjetStats() {
    return Promise.all([
      this.getDashboardStats().toPromise(),
      this.getConventionDetailedStats().toPromise(),
      this.getFactureDetailedStats().toPromise(),
      this.getProjectDetailedStats().toPromise(),
      this.getFinancialDetailedStats().toPromise(),
      this.getSummaryStats().toPromise(),
      this.getOverdueAlerts().toPromise()
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
      this.getProjectDetailedStats().toPromise(),
      this.getSummaryStats().toPromise(),
      this.getOverdueAlerts().toPromise()
    ]);
  }

  getDecideurStats() {
    return Promise.all([
      this.getDashboardStats().toPromise(),
      this.getConventionDetailedStats().toPromise(),
      this.getFactureDetailedStats().toPromise(),
      this.getFinancialDetailedStats().toPromise(),
      this.getProjectDetailedStats().toPromise(),
      this.getSummaryStats().toPromise(),
      this.getOverdueAlerts().toPromise()
    ]);
  }

  // Helper method to get stats based on user role
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



  
}