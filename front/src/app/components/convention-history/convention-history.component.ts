import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ConventionService } from '../../services/convention.service';
import { HistoryService, HistoryEntry } from '../../services/history.service';
import { Location } from '@angular/common';

@Component({
  selector: 'app-convention-history',
  templateUrl: './convention-history.component.html',
  styleUrls: ['./convention-history.component.css'],
  standalone: false
})
export class ConventionHistoryComponent implements OnInit {
  conventionId!: number;
  conventionReference: string = '';
  conventionHistory: HistoryEntry[] = [];
  groupedHistory: { date: string, entries: HistoryEntry[] }[] = [];
  loading = true;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private conventionService: ConventionService,
    public historyService: HistoryService,
    private location: Location,

  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.conventionId = +params['id'];
      this.loadData();
    });
  }

  loadData(): void {
    this.loading = true;
    
    // Get convention info
    this.conventionService.getConvention(this.conventionId).subscribe({
      next: (response) => {
        if (response.success) {
          this.conventionReference = response.data.referenceConvention;
        }
      }
    });
    
    // Get history
    this.historyService.getHistoryByEntity('CONVENTION', this.conventionId).subscribe({
      next: (response) => {
        if (response.success) {
          this.conventionHistory = response.data;
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

groupHistory(): void {
  const groups: { [key: string]: HistoryEntry[] } = {};
  
  this.conventionHistory.forEach(entry => {
    // Format the date as JJ/MM/YY
    const date = new Date(entry.timestamp);
    const day = date.getDate().toString().padStart(2, '0');
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const year = date.getFullYear().toString().slice(-2);
    const formattedDate = `${day}/${month}/${year}`; // JJ/MM/YY format
    
    if (!groups[formattedDate]) {
      groups[formattedDate] = [];
    }
    groups[formattedDate].push(entry);
  });
  
  this.groupedHistory = Object.keys(groups)
    .sort((a, b) => {
      // Parse JJ/MM/YY for sorting
      const [aDay, aMonth, aYear] = a.split('/');
      const [bDay, bMonth, bYear] = b.split('/');
      const dateA = new Date(`20${aYear}-${aMonth}-${aDay}`);
      const dateB = new Date(`20${bYear}-${bMonth}-${bDay}`);
      return dateB.getTime() - dateA.getTime(); // Descending (newest first)
    })
    .map(date => ({
      date, // This will now be in JJ/MM/YY format
      entries: groups[date].sort((a, b) => 
        new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
      )
    }));
}

  goBack(): void {
    this.location.back();
  }

  getConventionHistoryActionClass(actionType: string): string {
    switch (actionType) {
      case 'CREATE': return 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300';
      case 'UPDATE': return 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300';
      case 'DELETE': return 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300';
      case 'ARCHIVE': return 'bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-300';
      case 'RESTORE': return 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300';
      default: return 'bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-300';
    }
  }

  getChangedFieldsArray(entry: HistoryEntry): { field: string, old: any, new: any }[] {
    const changes = this.historyService.getChangedFields(entry.oldValues, entry.newValues);
    return Object.keys(changes).map(key => ({
      field: key,
      old: changes[key].old,
      new: changes[key].new
    }));
  }

  formatFieldName(field: string): string {
    const fieldMap: { [key: string]: string } = {
      'referenceConvention': 'Référence',
      'libelle': 'Libellé',
      'dateDebut': 'Date début',
      'dateFin': 'Date fin',
      'montantHT': 'Montant HT',
      'montantTTC': 'Montant TTC',
      'tva': 'TVA',
      'nbUsers': 'Utilisateurs',
      'etat': 'Statut',
      'periodicite': 'Périodicité'
    };
    return fieldMap[field] || field;
  }

  formatChangeValue(value: any): string {
    if (value === null || value === undefined) return '-';
    if (typeof value === 'object') return JSON.stringify(value);
    if (typeof value === 'number') return value.toString();
    if (value instanceof Date) return value.toLocaleDateString();
    return value.toString();
  }

  objectKeys(obj: any): string[] {
    return obj ? Object.keys(obj) : [];
  }


  getChangedFieldsOnly(entry: HistoryEntry): string[] {
    if (!entry.oldValues || !entry.newValues) return [];
    
    return Object.keys(entry.newValues).filter(key => {
      const oldVal = entry.oldValues?.[key];
      const newVal = entry.newValues[key];
      
      // Compare values properly (handle dates, numbers, strings)
      if (oldVal === null || oldVal === undefined) return newVal !== null && newVal !== undefined;
      if (newVal === null || newVal === undefined) return true;
      
      // Convert to string for comparison to handle date objects
      return String(oldVal) !== String(newVal);
    });
  }
}