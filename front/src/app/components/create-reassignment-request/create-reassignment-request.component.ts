import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { RequestService, CreateReassignmentRequest } from '../../services/request.service';
import { UserService } from '../../services/user.service';
import { WorkloadService } from '../../services/workload.service';
import { AuthService } from '../../services/auth.service';
import { ApplicationService } from '../../services/application.service';
import { environment } from 'src/environments/environment';

@Component({
  selector: 'app-create-reassignment-request',
  templateUrl: './create-reassignment-request.component.html',
  styleUrls: ['./create-reassignment-request.component.css']
})
export class CreateReassignmentRequestComponent implements OnInit {
  @Input() application: any = null;
  @Input() show = false;

  availableChefs: any[] = [];
  selectedChefId: number | null = null;
  reason = '';
  recommendations = '';
  loading = false;
  errorMessage = '';
  successMessage = '';
  chefsWorkload: Map<number, any> = new Map();
  workloadLoading = false;

  baseUrl=environment.apiUrl;
  
  constructor(
    private requestService: RequestService,
    private userService: UserService,
    private workloadService: WorkloadService,
    private authService: AuthService,
    private applicationService: ApplicationService
  ) {}

  ngOnInit(): void {
    this.loadAvailableChefs();
  }

  loadAvailableChefs(): void {
    this.workloadLoading = true;
    
    this.userService.getChefsProjet().subscribe({
      next: (response) => {
        if (response.success) {
          this.availableChefs = response.data;
          
          // Filter out current user
          const currentUser = this.authService.getCurrentUser();
          if (currentUser) {
            this.availableChefs = this.availableChefs.filter(c => c.id !== currentUser.id);
          }
          
          // Load workloads if we have an application
          if (this.application && this.application.id) {
            this.loadChefsWorkload(this.application.id);
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

  getWorkloadText(workload: number): string {
    if (workload > 75) return 'Charge critique';
    if (workload >= 45) return 'Charge moyenne';
    return 'Charge faible';
  }

  submitRequest(): void {
    if (!this.selectedChefId) {
      this.errorMessage = 'Veuillez sélectionner un chef de projet recommandé';
      return;
    }

    if (!this.reason.trim()) {
      this.errorMessage = 'Veuillez expliquer la raison de votre demande';
      return;
    }

    const requestData: CreateReassignmentRequest = {
      applicationId: this.application.id,
      recommendedChefId: this.selectedChefId,
      reason: this.reason,
      recommendations: this.recommendations
    };

    this.loading = true;
    this.errorMessage = '';

    this.requestService.createReassignmentRequest(requestData).subscribe({
      next: (response) => {
        if (response.success) {
          this.successMessage = 'Demande de réassignation créée avec succès';
          setTimeout(() => {
            this.close();
          }, 2000);
        } else {
          this.errorMessage = response.message || 'Erreur lors de la création de la demande';
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error creating request:', error);
        this.errorMessage = error.error?.message || 'Erreur lors de la création de la demande';
        this.loading = false;
      }
    });
  }

  close(): void {
    this.show = false;
    this.selectedChefId = null;
    this.reason = '';
    this.recommendations = '';
    this.errorMessage = '';
    this.successMessage = '';
  }


  // Add to your create-reassignment-request component

getChefAvatarUrl(chef: any): string {
  if (!chef || !chef.profileImage) {
    let initials = '?';
    if (chef?.firstName && chef?.lastName) {
      initials = (chef.firstName[0] + chef.lastName[0]).toUpperCase();
    } else if (chef?.firstName) {
      initials = chef.firstName[0].toUpperCase();
    } else if (chef?.username) {
      initials = chef.username[0].toUpperCase();
    }
    return this.generateDefaultAvatar(initials);
  }
  
  const profileImage = chef.profileImage;
  if (profileImage.startsWith('http')) {
    return profileImage;
  }
  if (profileImage.startsWith('/uploads/')) {
    return this.baseUrl + profileImage;
  }
  if (profileImage.startsWith('data:image')) {
    return profileImage;
  }
  return this.baseUrl + '/uploads/avatars/' + profileImage;
}

generateDefaultAvatar(initials: string): string {
  // Blue color for the ring and text
  const blueColor = '#3b82f6'; // Tailwind blue-500
  
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="100" height="100">
    <!-- Circle with blue ring and no fill -->
    <circle cx="50" cy="50" r="48" fill="none" stroke="${blueColor}" stroke-width="2"/>
    <!-- Text in blue -->
    <text x="50" y="58" text-anchor="middle" font-family="Arial, Helvetica, sans-serif" font-size="38" font-weight="500" fill="${blueColor}" dominant-baseline="middle">${initials}</text>
  </svg>`;
  
  return 'data:image/svg+xml;base64,' + btoa(unescape(encodeURIComponent(svg)));
}

handleChefImageError(event: any, chef: any): void {
  let initials = '?';
  if (chef?.firstName && chef?.lastName) {
    initials = (chef.firstName[0] + chef.lastName[0]).toUpperCase();
  } else if (chef?.firstName) {
    initials = chef.firstName[0].toUpperCase();
  } else if (chef?.username) {
    initials = chef.username[0].toUpperCase();
  }
  event.target.src = this.generateDefaultAvatar(initials);
  event.target.onerror = null;
}
}