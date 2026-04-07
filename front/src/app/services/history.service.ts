// src/app/services/history.service.ts - Add these helper methods

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface HistoryEntry {
  id: number;
  timestamp: string;
  timeFormatted: string;
  dateFormatted: string;
  dateTimeFormatted: string;
  
  actionType: string;
  actionTypeLabel: string;
  
  entityType: string;
  entityTypeLabel: string;
  
  entityId: number;
  entityCode: string;
  entityName: string;
  
  userId: number;
  username: string;
  userFullName: string;
  userRole: string;
  
  description: string;
  
  oldValues: any;
  newValues: any;
  hasChanges: boolean;
  changedFieldsCount: number;
  
  ipAddress: string;
  userAgent: string;
  
}

@Injectable({
  providedIn: 'root'
})
export class HistoryService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getAllHistory(): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/history`);
  }

  getRecentHistory(limit: number = 50): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/history/recent?limit=${limit}`);
  }

  getHistoryByUser(userId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/history/user/${userId}`);
  }

  getHistoryByEntity(entityType: string, entityId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/history/entity/${entityType}/${entityId}`);
  }

  getHistoryByDate(date: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/history/date/${date}`);
  }

  getHistoryGroupedByDay(): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/history/grouped-by-day`);
  }

  searchHistory(entityType?: string, actionType?: string, userId?: number, date?: string): Observable<any> {
    let params: any = {};
    if (entityType) params.entityType = entityType;
    if (actionType) params.actionType = actionType;
    if (userId) params.userId = userId;
    if (date) params.date = date;
    
    return this.http.get(`${this.apiUrl}/api/history/search`, { params });
  }

  getHistoryStats(): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/history/stats`);
  }

  // Helper method to format change values
  formatChangeValue(value: any): string {
    if (value === null || value === undefined) return '-';
    if (typeof value === 'object') {
      if (value instanceof Date) return value.toLocaleDateString('fr-FR');
      return JSON.stringify(value);
    }
    if (typeof value === 'number') {
      // Format currency if it looks like money
      if (value.toString().includes('.')) {
        return new Intl.NumberFormat('fr-TN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value) + ' TND';
      }
      return value.toString();
    }
    if (typeof value === 'boolean') return value ? 'Oui' : 'Non';
    return value.toString();
  }

  // Get only changed fields (exclude unchanged ones)
  getChangedFields(oldValues: any, newValues: any): { [key: string]: { old: any, new: any } } {
    const changed: { [key: string]: { old: any, new: any } } = {};
    
    if (!oldValues || !newValues) return changed;
    
    // Special handling for nested objects from backend
    Object.keys(newValues).forEach(key => {
      const oldValue = oldValues[key];
      const newValue = newValues[key];
      
      // Skip if values are the same
      if (JSON.stringify(oldValue) === JSON.stringify(newValue)) {
        return;
      }
      
      // Handle nested change objects from backend
      if (oldValue && typeof oldValue === 'object' && oldValue.old !== undefined && oldValue.new !== undefined) {
        changed[key] = { old: oldValue.old, new: oldValue.new };
      } 
      // Handle direct comparison
      else {
        changed[key] = { old: oldValue, new: newValue };
      }
    });
    
    return changed;
  }

  // Get icon for action type
  getActionIcon(actionType: string): string {
    switch (actionType) {
      case 'CREATE': return 'fas fa-plus-circle text-green-500';
      case 'UPDATE': return 'fas fa-edit text-blue-500';
      case 'DELETE': return 'fas fa-trash-alt text-red-500';
      case 'LOGIN': return 'fas fa-sign-in-alt text-indigo-500';
      case 'LOGOUT': return 'fas fa-sign-out-alt text-gray-500';
      case 'PASSWORD_CHANGE': return 'fas fa-key text-yellow-500';
      case 'LOCK': return 'fas fa-lock text-red-500';
      case 'UNLOCK': return 'fas fa-unlock text-green-500';
      case 'ROLE_CHANGE': return 'fas fa-user-tag text-purple-500';
      case 'DEPARTMENT_CHANGE': return 'fas fa-building text-blue-500';
      case 'ASSIGN_CHEF': return 'fas fa-user-plus text-indigo-500';
      case 'STATUS_CHANGE': return 'fas fa-exchange-alt text-orange-500';
      case 'DATES_SYNC': return 'fas fa-calendar-alt text-cyan-500';
      case 'ARCHIVE': return 'fas fa-archive text-gray-500';
      case 'RESTORE': return 'fas fa-undo-alt text-green-500';
      case 'FINANCIAL_UPDATE': return 'fas fa-chart-line text-emerald-500';
      case 'PAYMENT': return 'fas fa-credit-card text-emerald-500';
      case 'OVERDUE': return 'fas fa-exclamation-triangle text-red-500';
case 'RENEW': return 'fas fa-sync-alt text-cyan-500';   case 'REASSIGN_CHEF': return 'fas fa-user-friends text-indigo-500';
      case 'REQUEST_PROCESSED': return 'fas fa-check-double text-green-500';
      default: return 'fas fa-history text-gray-500';
    }
  }

  // Get entity icon
  getEntityIcon(entityType: string): string {
    switch (entityType) {
      case 'USER': return 'fas fa-user';
      case 'APPLICATION': return 'fas fa-project-diagram';
      case 'CONVENTION': return 'fas fa-file-contract';
      case 'FACTURE': return 'fas fa-file-invoice';
      default: return 'fas fa-circle';
    }
  }

  // Should show user info for this entity?
  shouldShowUserInfo(entityType: string): boolean {
    return entityType === 'USER' || entityType === 'APPLICATION';
  }



}