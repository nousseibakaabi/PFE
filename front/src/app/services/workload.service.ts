import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface WorkloadCheck {
  chefId: number;
  chefName: string;
  applicationId: number;
  applicationName: string;
  canAssign: boolean;
  assignWithCaution: boolean;
  status: 'OK' | 'WARNING' | 'BLOCKED';
  message: string;
  analysis: WorkloadAnalysis;
  alternativeChefs: AlternativeChef[];
}

export interface WorkloadAnalysis {
  currentWorkload: number;
  currentCount: number;
  currentValue: number;
  currentDuration: number;
  projectedCount: number;
  projectedValue: number;
  projectedDuration: number;
  projectedWorkload: number;
  workloadIncrease: number;
  exceedsCount: boolean;
  exceedsValue: boolean;
  exceedsDuration: boolean;
  recommendation: string;
  reason: string;
}

export interface AlternativeChef {
  chefId: number;
  chefName: string;
  currentWorkload: number;
  projectedWorkload: number;
  workloadIncrease: number;
  canAccept: boolean;
  reason: string;
}

export interface WorkloadDashboard {
  totalChefs: number;
  overloadedChefs: number;
  highWorkloadChefs: number;
  availableChefs: number;
  averageWorkload: number;
  workloads: WorkloadDTO[];
}

export interface WorkloadDTO {
  chefId: number;
  chefName: string;
  currentWorkload: number;
  currentApps: number;
  maxApps: number;
  totalValue: number;
  maxValue: number;
  totalDuration: number;
  maxDuration: number;
  status: string;
  statusColor: string;
}

export interface AssignmentResult {
  success: boolean;
  warning: boolean;
  blocked: boolean;
  message: string;
  data: WorkloadCheck;
  updatedWorkload: number;
}

@Injectable({
  providedIn: 'root'
})
export class WorkloadService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  /**
   * Check if a chef can be assigned to an application
   */
  checkAssignment(chefId: number, applicationId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/applications/workload/check`, {
      params: {
        chefId: chefId.toString(),
        applicationId: applicationId.toString()
      }
    });
  }

  /**
   * Get workload dashboard for all chefs (admin only)
   */
  getWorkloadDashboard(): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/applications/workload/dashboard`);
  }

  /**
   * Assign chef to application with workload check
   */
  assignWithWorkloadCheck(chefId: number, applicationId: number, force: boolean = false): Observable<any> {
    return this.http.post(`${this.apiUrl}/api/applications/workload/assign`, null, {
      params: {
        chefId: chefId.toString(),
        applicationId: applicationId.toString(),
        force: force.toString()
      }
    });
  }


  /**
   * Get workload color based on percentage
   */
    getWorkloadClass(percentage: number): string {
    if (percentage > 75) return '#ef4444';   // 🔴 red (above 75)
    if (percentage >= 45) return '#f59e0b';  // 🟠 orange (45 to 75)
    return '#10b981';                        // 🟢 green (under 45)
    }

  /**
   * Get workload status text
   */
  getWorkloadStatus(percentage: number): string {
    if (percentage > 75) return 'Critique';
    if (percentage  >= 45) return 'Moyenne';
    return 'Faible';
  }

  /**
   * Get progress bar class based on workload
   */
  getProgressBarClass(percentage: number): string {
    if (percentage > 75) return '#ef4444';
    if (percentage >= 45) return '#f59e0b';
    return '#10b981';
  }
}