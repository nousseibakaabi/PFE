import { Component, OnInit } from '@angular/core';
import { RequestService, Request, RequestAction, AvailableChef } from '../../services/request.service';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { WorkloadService } from '../../services/workload.service';
import { Router } from '@angular/router';
import { ApplicationService } from '../../services/application.service';
import { ConventionService } from '../../services/convention.service';

@Component({
  selector: 'app-requests',
  templateUrl: './requests.component.html',
  styleUrls: ['./requests.component.css']
})
export class RequestsComponent implements OnInit {
  requests: Request[] = [];
  filteredRequests: Request[] = [];
  loading = false;
  errorMessage = '';
  successMessage = '';
  
  currentUser: any = null;
  isAdmin = false;
  isChefProjet = false;
  
  filterStatus = 'ALL'; // ALL, PENDING, APPROVED, DENIED
  
  // For response modals
  showResponseModal = false;
  selectedRequest: Request | null = null;
  responseAction: 'APPROVE' | 'DENY' = 'APPROVE';
  responseReason = '';
  recommendations = '';
  
  // For chef recommendation (when declining)
  availableChefs: AvailableChef[] = [];
  selectedChefId: number | null = null;
  chefsWorkload: Map<number, any> = new Map();
  workloadLoading = false;
  
  // For detailed view
  showDetailModal = false;

  // Track request types for UI
  requestTypes = {
    RENEWAL_ACCEPTANCE: 'Acceptation de renouvellement',
    REASSIGNMENT_SUGGESTION: 'Suggestion de réassignation'
  };

  constructor(
    private requestService: RequestService,
    private userService: UserService,
    private authService: AuthService,
    private workloadService: WorkloadService,
    private applicationService: ApplicationService,
    private conventionService: ConventionService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadCurrentUser();
    this.loadRequests();
  }

  loadCurrentUser(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.isAdmin = this.authService.isAdmin();
    this.isChefProjet = this.authService.isChefProjet();
    
    console.log('Current user:', this.currentUser);
    console.log('Is admin:', this.isAdmin);
    console.log('Is chef projet:', this.isChefProjet);
  }

  loadRequests(): void {
    this.loading = true;
    this.requestService.getUserRequests().subscribe({
      next: (response) => {
        console.log('Requests response:', response);
        if (response.success) {
          this.requests = response.data;
          this.applyFilter();
        } else {
          this.errorMessage = response.message || 'Failed to load requests';
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading requests:', error);
        this.errorMessage = 'Failed to load requests';
        this.loading = false;
      }
    });
  }

  applyFilter(): void {
    if (this.filterStatus === 'ALL') {
      this.filteredRequests = [...this.requests];
    } else {
      this.filteredRequests = this.requests.filter(r => r.status === this.filterStatus);
    }
    console.log('Filtered requests:', this.filteredRequests);
  }

  openResponseModal(request: Request, action: 'APPROVE' | 'DENY'): void {
    console.log('Opening response modal:', request, action);
    this.selectedRequest = request;
    this.responseAction = action;
    this.responseReason = '';
    this.recommendations = '';
    this.selectedChefId = null;
    
    if (action === 'DENY') {
      // For denial, load available chefs based on request type
      this.loadAvailableChefs(request);
    }
    
    this.showResponseModal = true;
  }

  closeResponseModal(): void {
    this.showResponseModal = false;
    this.selectedRequest = null;
    this.responseReason = '';
    this.recommendations = '';
    this.selectedChefId = null;
  }

  openDetailModal(request: Request): void {
    this.selectedRequest = request;
    this.showDetailModal = true;
  }

  closeDetailModal(): void {
    this.showDetailModal = false;
    this.selectedRequest = null;
  }

  loadAvailableChefs(request: Request): void {
    this.workloadLoading = true;
    
    // Get all chefs with ROLE_CHEF_PROJET
    this.userService.getChefsProjet().subscribe({
      next: (response) => {
        if (response.success) {
          this.availableChefs = response.data;
          // Filter out current user if they are a chef
          if (this.currentUser) {
            this.availableChefs = this.availableChefs.filter(c => c.id !== this.currentUser.id);
          }
          
          // If there's an application, check workloads
          if (request.applicationId) {
            this.loadChefsWorkload(request.applicationId);
          } else {
            this.workloadLoading = false;
          }
        } else {
          this.workloadLoading = false;
        }
      },
      error: (error) => {
        console.error('Error loading chefs:', error);
        this.workloadLoading = false;
      }
    });
  }

  loadChefsWorkload(applicationId: number): void {
    this.workloadService.getWorkloadDashboard().subscribe({
      next: (response) => {
        if (response.success && response.data?.workloads) {
          response.data.workloads.forEach((w: any) => {
            this.chefsWorkload.set(w.chefId, w);
          });
        }
        this.workloadLoading = false;
      },
      error: (error) => {
        console.error('Error loading workload:', error);
        this.workloadLoading = false;
      }
    });
  }

  getChefWorkload(chefId: number): any {
    return this.chefsWorkload.get(chefId);
  }

  getWorkloadColor(workload: number): string {
    if (workload > 75) return '#ef4444';
    if (workload >= 45) return '#f59e0b';
    return '#10b981';
  }

  submitResponse(): void {
    if (!this.selectedRequest) return;
    
    const action: RequestAction = {
      requestId: this.selectedRequest.id,
      action: this.responseAction
    };
    
    if (this.responseAction === 'DENY') {
      if (!this.responseReason.trim()) {
        this.errorMessage = 'La raison est requise';
        return;
      }
      action.reason = this.responseReason;
      
      if (this.recommendations.trim()) {
        action.recommendations = this.recommendations;
      }
      
      if (this.selectedChefId) {
        action.recommendedChefId = this.selectedChefId;
      }
    }
    
    console.log('Submitting action:', action);
    
    this.loading = true;
    this.requestService.processRequest(action).subscribe({
      next: (response) => {
        console.log('Process response:', response);
        if (response.success) {
          this.successMessage = this.responseAction === 'APPROVE' 
            ? 'Demande approuvée avec succès' 
            : 'Demande refusée avec succès';
          this.loadRequests();
          this.closeResponseModal();
        } else {
          this.errorMessage = response.message || 'Failed to process request';
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error processing request:', error);
        this.errorMessage = error.error?.message || 'Failed to process request';
        this.loading = false;
      }
    });
  }

  viewApplication(id: number): void {
    this.router.navigate(['/applications', id]);
    this.closeDetailModal();
  }

  viewConvention(id: number): void {
    this.router.navigate(['/conventions', id]);
    this.closeDetailModal();
  }

  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'PENDING': return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-300';
      case 'APPROVED': return 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300';
      case 'DENIED': return 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  getRequestTypeLabel(type: string): string {
    switch (type) {
      case 'RENEWAL_ACCEPTANCE': return 'Acceptation de renouvellement';
      case 'REASSIGNMENT_SUGGESTION': return 'Suggestion de réassignation';
      case 'REASSIGNMENT_REQUEST_FROM_CHEF': return 'Demande de réassignation';
      default: return type;
    }
  }

  getStatusIcon(status: string): string {
    switch (status) {
      case 'PENDING': return '⏳';
      case 'APPROVED': return '✅';
      case 'DENIED': return '❌';
      default: return '📋';
    }
  }
  
  formatDate(dateString: string): string {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }
}