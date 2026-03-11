// src/app/components/history/history.component.ts
import { Component, OnInit } from '@angular/core';
import { HistoryService, HistoryEntry } from '../../services/history.service';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-history',
  templateUrl: './history.component.html',
  styleUrls: ['./history.component.css']
})
export class HistoryComponent implements OnInit {
  historyEntries: HistoryEntry[] = [];
  groupedHistory: { date: string, entries: HistoryEntry[] }[] = [];
  loading = true;
  
  // Filters
  entityTypeFilter = '';
  actionTypeFilter = '';
  dateFilter = '';
  
  // Stats
  stats: any = null;
  showStats = false;
  Object = Object;

  
  // Available filter options
  entityTypes = [
    { value: '', label: 'All Entities' },
    { value: 'USER', label: 'Users' },
    { value: 'APPLICATION', label: 'Applications' },
    { value: 'CONVENTION', label: 'Conventions' },
    { value: 'FACTURE', label: 'Factures' }
  ];
  
  actionTypes = [
    { value: '', label: 'All Actions' },
    { value: 'CREATE', label: 'Création' },
    { value: 'UPDATE', label: 'Modification' },
    { value: 'DELETE', label: 'Suppression' },
    { value: 'LOGIN', label: 'Connexion' },
    { value: 'LOGOUT', label: 'Déconnexion' },
    { value: 'PASSWORD_CHANGE', label: 'Changement de mot de passe' },
    { value: 'LOCK', label: 'Verrouillage' },
    { value: 'UNLOCK', label: 'Déverrouillage' },
    { value: 'ROLE_CHANGE', label: 'Changement de rôle' },
    { value: 'ASSIGN_CHEF', label: 'Assignation chef' },
    { value: 'STATUS_CHANGE', label: 'Changement de statut' },
    { value: 'ARCHIVE', label: 'Archivage' },
    { value: 'RESTORE', label: 'Restauration' },
    { value: 'PAYMENT', label: 'Paiement' },
    { value: 'RENEW' , label :'Renouvellement'},
    { value: 'REQUEST_PROCESSED' , label :'Traitement de demande'},
    { value: 'REASSIGN_CHEF' , label :'Réassignation chef de projet'}
  ];

  constructor(
    public historyService: HistoryService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    if (!this.authService.isAdmin() && !this.authService.isDecideur()) {
      this.router.navigate(['/dashboard']);
      return;
    }
    this.loadHistory();
  }

  loadHistory(): void {
    this.loading = true;
    this.historyService.getAllHistory().subscribe({
      next: (response) => {
        if (response.success) {
          this.historyEntries = response.data;
          this.groupHistory();
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading history:', error);
        this.loading = false;
      }
    });
  }

  loadStats(): void {
    this.historyService.getHistoryStats().subscribe({
      next: (response) => {
        if (response.success) {
          this.stats = response.data;
          this.showStats = true;
        }
      },
      error: (error) => {
        console.error('Error loading stats:', error);
      }
    });
  }

  applyFilters(): void {
    this.loading = true;
    this.historyService.searchHistory(
      this.entityTypeFilter || undefined,
      this.actionTypeFilter || undefined,
      undefined,
      this.dateFilter || undefined
    ).subscribe({
      next: (response) => {
        if (response.success) {
          this.historyEntries = response.data;
          this.groupHistory();
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error applying filters:', error);
        this.loading = false;
      }
    });
  }

  resetFilters(): void {
    this.entityTypeFilter = '';
    this.actionTypeFilter = '';
    this.dateFilter = '';
    this.loadHistory();
  }

  groupHistory(): void {
    const groups: { [key: string]: HistoryEntry[] } = {};
    
    this.historyEntries.forEach(entry => {
      if (!groups[entry.dateFormatted]) {
        groups[entry.dateFormatted] = [];
      }
      groups[entry.dateFormatted].push(entry);
    });
    
    this.groupedHistory = Object.keys(groups)
      .sort((a, b) => b.localeCompare(a))
      .map(date => ({
        date,
        entries: groups[date].sort((a, b) => b.timestamp.localeCompare(a.timestamp))
      }));
  }

  getActionClass(actionType: string): string {
    switch (actionType) {
      case 'CREATE': return 'bg-green-100 text-green-800';
      case 'UPDATE': return 'bg-blue-100 text-blue-800';
      case 'DELETE': return 'bg-red-100 text-red-800';
      case 'LOGIN': return 'bg-indigo-100 text-indigo-800';
      case 'LOGOUT': return 'bg-gray-100 text-gray-800';
      case 'PASSWORD_CHANGE': return 'bg-yellow-100 text-yellow-800';
      case 'LOCK': return 'bg-red-100 text-red-800';
      case 'UNLOCK': return 'bg-green-100 text-green-800';
      case 'ROLE_CHANGE': return 'bg-purple-100 text-purple-800';
      case 'ASSIGN_CHEF': return 'bg-indigo-100 text-indigo-800';
      case 'STATUS_CHANGE': return 'bg-orange-100 text-orange-800';
      case 'ARCHIVE': return 'bg-gray-100 text-gray-800';
      case 'RESTORE': return 'bg-green-100 text-green-800';
      case 'PAYMENT': return 'bg-emerald-100 text-emerald-800';
      case 'OVERDUE': return 'bg-red-100 text-red-800';
      case 'RENEW': return 'bg-cyan-100 text-cyan-800';
      case 'REASSIGN_CHEF': return 'bg-indigo-100 text-indigo-800';
      case 'REQUEST_PROCESSED': return 'bg-green-100 text-green-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  navigateToEntity(entry: HistoryEntry): void {
    switch (entry.entityType) {
      case 'USER':
        // Navigate to user profile or admin users page
        this.router.navigate(['/admin/users'], { queryParams: { userId: entry.entityId } });
        break;
      case 'APPLICATION':
        this.router.navigate(['/applications', entry.entityId]);
        break;
      case 'CONVENTION':
        this.router.navigate(['/conventions', entry.entityId]);
        break;
      case 'FACTURE':
        this.router.navigate(['/factures', entry.entityId]);
        break;
    }
  }

  closeStats(): void {
    this.showStats = false;
  }

}