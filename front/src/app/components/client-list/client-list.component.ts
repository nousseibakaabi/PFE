import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { ClientBilanService, ClientBilan, Structure } from '../../services/client-bilan.service';
import { FormControl } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-client-list',
  templateUrl: './client-list.component.html',
  styleUrls: ['./client-list.component.css']
})
export class ClientListComponent implements OnInit, OnDestroy {
  clients: Structure[] = [];
  filteredClients: Structure[] = [];
  clientBilans: Map<number, ClientBilan> = new Map();
  loading = false;
  searchControl = new FormControl('');
  private searchSubscription: Subscription = new Subscription();

  // Filters
  weakPaymentFilter = false;
  selectedScoreRange: number = 0;
  maxScore: number = 100;

  // Summary stats
  summaryStats = {
    totalClients: 0,
    averageScore: 0,
    excellentCount: 0,
    goodCount: 0,
    averageCount: 0,
    poorCount: 0,
    criticalCount: 0,
    totalOverdue: 0,
    totalContractValue: 0
  };

  // Pagination
  currentPage: number = 1;
  itemsPerPage: number = 8;

  constructor(
    private clientBilanService: ClientBilanService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadClients();
    
    this.searchSubscription = this.searchControl.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => {
        this.applyFilters();
      });
  }


  getPageNumbers(): number[] {
  return Array.from({ length: this.totalPages }, (_, i) => i + 1);
}

  ngOnDestroy(): void {
    if (this.searchSubscription) {
      this.searchSubscription.unsubscribe();
    }
  }

  loadClients(): void {
    this.loading = true;
    this.clientBilanService.getStructuresBeneficiel().subscribe({
      next: (clients) => {
        console.log('Loaded clients:', clients);
        this.clients = clients || [];
        if (this.clients.length > 0) {
          this.loadAllClientBilans();
        } else {
          this.loading = false;
        }
      },
      error: (error) => {
        console.error('Error loading clients:', error);
        this.loading = false;
      }
    });
  }

  loadAllClientBilans(): void {
    if (!this.clients || this.clients.length === 0) {
      this.loading = false;
      return;
    }
    
    let completed = 0;
    this.clients.forEach(client => {
      if (client && client.id) {
        this.clientBilanService.getClientBilan(client.id).subscribe({
          next: (bilan) => {
            this.clientBilans.set(client.id, bilan);
            completed++;
            if (completed === this.clients.length) {
              this.applyFilters();
              this.calculateSummaryStats();
              this.loading = false;
            }
          },
          error: (error) => {
            console.error(`Error loading bilan for client ${client.id}:`, error);
            completed++;
            if (completed === this.clients.length) {
              this.applyFilters();
              this.calculateSummaryStats();
              this.loading = false;
            }
          }
        });
      } else {
        completed++;
        if (completed === this.clients.length) {
          this.applyFilters();
          this.calculateSummaryStats();
          this.loading = false;
        }
      }
    });
  }

  applyFilters(): void {
    if (!this.clients || this.clients.length === 0) {
      this.filteredClients = [];
      return;
    }
    
    const searchTerm = this.searchControl.value?.toLowerCase() || '';
    
    this.filteredClients = this.clients.filter(client => {
      const bilan = this.clientBilans.get(client.id);
      if (!bilan) return false;
      
      // Search filter
      const matchesSearch = (client.name && client.name.toLowerCase().includes(searchTerm)) ||
                           (client.code && client.code.toLowerCase().includes(searchTerm));
      
      // Weak payment filter
      let matchesWeakPayment = true;
      if (this.weakPaymentFilter && bilan && bilan.paymentStats) {
        matchesWeakPayment = bilan.paymentStats.paymentBehavior === 'VERY_POOR' ||
                           bilan.paymentStats.paymentBehavior === 'POOR';
      }
      
      // Score range filter
      const matchesScoreRange = bilan.rating && bilan.rating.overallScore >= this.selectedScoreRange;
      
      return matchesSearch && matchesWeakPayment && matchesScoreRange;
    });
  }

  calculateSummaryStats(): void {
    let totalScore = 0;
    let totalOverdue = 0;
    let totalContractValue = 0;
    let count = 0;
    
    // Reset counts
    this.summaryStats.excellentCount = 0;
    this.summaryStats.goodCount = 0;
    this.summaryStats.averageCount = 0;
    this.summaryStats.poorCount = 0;
    this.summaryStats.criticalCount = 0;
    
    this.clientBilans.forEach(bilan => {
      if (!bilan || !bilan.rating || !bilan.financialSummary) return;
      
      totalScore += bilan.rating.overallScore || 0;
      totalOverdue += bilan.financialSummary.totalOverdue || 0;
      totalContractValue += bilan.financialSummary.totalContractValue || 0;
      count++;
      
      const rating = bilan.rating.rating;
      if (rating === 'A') this.summaryStats.excellentCount++;
      else if (rating === 'B') this.summaryStats.goodCount++;
      else if (rating === 'C') this.summaryStats.averageCount++;
      else if (rating === 'D') this.summaryStats.poorCount++;
      else if (rating === 'F') this.summaryStats.criticalCount++;
    });
    
    this.summaryStats.totalClients = count;
    this.summaryStats.averageScore = count > 0 ? Math.round(totalScore / count) : 0;
    this.summaryStats.totalOverdue = totalOverdue;
    this.summaryStats.totalContractValue = totalContractValue;
  }

  onScoreRangeChange(event: any): void {
    this.selectedScoreRange = event.target ? Number(event.target.value) : event;
    this.applyFilters();
  }

  getPaymentBehaviorClass(behavior: string): string {
    switch (behavior) {
      case 'EXCELLENT': return 'bg-success';
      case 'GOOD': return 'bg-info';
      case 'AVERAGE': return 'bg-warning';
      case 'POOR': return 'bg-danger';
      case 'VERY_POOR': return 'bg-danger';
      default: return 'bg-secondary';
    }
  }

  viewClientDetails(clientId: number): void {
    this.router.navigate(['/client-bilan', clientId]);
  }

  resetFilters(): void {
    this.searchControl.setValue('');
    this.weakPaymentFilter = false;
    this.selectedScoreRange = 0;
    this.currentPage = 1;
    this.applyFilters();
  }

  get paginatedClients(): Structure[] {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    return this.filteredClients.slice(startIndex, endIndex);
  }

  get totalPages(): number {
    return Math.ceil(this.filteredClients.length / this.itemsPerPage);
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }
}