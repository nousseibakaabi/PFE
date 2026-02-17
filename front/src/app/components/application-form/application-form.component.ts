import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ApplicationService, Application, ApplicationRequest, ApiResponse } from '../../services/application.service';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { Location } from '@angular/common';

@Component({
  selector: 'app-application-form',
  templateUrl: './application-form.component.html',
  styleUrls: ['./application-form.component.css']
})
export class ApplicationFormComponent implements OnInit {
  applicationId: number | null = null;
  isEditing = false;
  loading = false;
  errorMessage = '';
  successMessage = '';

  // Current user info
  currentUser: any = null;
  isAdmin = false;
  isChefProjet = false;
  chefsProjet: any[] = [];

  // Application form
  applicationForm: ApplicationRequest = {
    code: '',
    name: '',
    description: '',
    chefDeProjetId: 0,
    clientName: '',
    clientEmail: '',
    clientPhone: '',
    clientAddress: '',
    minUser: 0,
    maxUser: 0,
    status: 'PLANIFIE'
  };

  // Status options
  statusOptions = [
    { value: 'PLANIFIE', label: 'Planifié' },
    { value: 'EN_COURS', label: 'En Cours' },
    { value: 'TERMINE', label: 'Terminé' },
    { value: 'SUSPENDU', label: 'Suspendu' },
    { value: 'ANNULE', label: 'Annulé' }
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private location: Location,
    private applicationService: ApplicationService,
    private userService: UserService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadCurrentUser();
    
    // Check if we're editing
    this.route.params.subscribe(params => {
      if (params['id']) {
        this.applicationId = +params['id'];
        this.isEditing = true;
        this.loadApplication(this.applicationId);
      } else {
        // New application - load suggested code
        this.loadSuggestedApplicationCode();
      }
    });

    // Load chefs if admin
    if (this.isAdmin) {
      this.loadChefsProjet();
    }
  }

  loadCurrentUser(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.isAdmin = this.authService.isAdmin();
    this.isChefProjet = this.authService.isChefProjet();
    
    // For chef de projet creating new application, auto-assign themselves
    if (this.isChefProjet && !this.isAdmin && !this.isEditing) {
      this.applicationForm.chefDeProjetId = this.currentUser?.id;
    }
  }

  loadChefsProjet(): void {
    this.userService.getChefsProjet().subscribe({
      next: (response) => {
        if (response.success) {
          this.chefsProjet = response.data;
        }
      },
      error: (error) => {
        console.error('Error loading chefs:', error);
      }
    });
  }

  loadApplication(id: number): void {
    this.loading = true;
    this.applicationService.getApplication(id).subscribe({
      next: (response: ApiResponse) => {
        if (response.success) {
          const app = response.data;
          this.applicationForm = {
            code: app.code,
            name: app.name,
            description: app.description || '',
            chefDeProjetId: app.chefDeProjetId,
            clientName: app.clientName,
            clientEmail: app.clientEmail || '',
            clientPhone: app.clientPhone || '',
            clientAddress: app.clientAddress || '',
            dateDebut: app.dateDebut || '',
            dateFin: app.dateFin || '',
            minUser: app.minUser || 0,
            maxUser: app.maxUser || 0,
            status: app.status || 'PLANIFIE'
          };
          
          // Check if chef de projet can edit this
          if (this.isChefProjet && !this.isAdmin && app.chefDeProjetId !== this.currentUser?.id) {
            this.errorMessage = 'Vous ne pouvez modifier que vos propres applications';
            setTimeout(() => this.goBack(), 2000);
          }
        } else {
          this.errorMessage = 'Application non trouvée';
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading application:', error);
        this.errorMessage = 'Erreur lors du chargement';
        this.loading = false;
      }
    });
  }

  loadSuggestedApplicationCode(): void {
    this.applicationService.getSuggestedApplicationCode().subscribe({
      next: (response: any) => {
        if (response.success && response.suggestedCode) {
          this.applicationForm.code = response.suggestedCode;
        }
      },
      error: (error) => {
        console.error('Failed to load suggested code:', error);
      }
    });
  }

  saveApplication(): void {
    if (!this.validateForm()) return;

    this.loading = true;
    
    if (this.isEditing && this.applicationId) {
      this.applicationService.updateApplication(this.applicationId, this.applicationForm).subscribe({
        next: (response: ApiResponse) => {
          if (response.success) {
            this.successMessage = 'Application mise à jour avec succès';
            setTimeout(() => {
              this.router.navigate(['/applications', this.applicationId]);
            }, 1500);
          } else {
            this.errorMessage = response.message || 'Échec de la mise à jour';
          }
          this.loading = false;
        },
        error: (error) => {
          this.errorMessage = error.error?.message || 'Erreur lors de la mise à jour';
          this.loading = false;
        }
      });
    } else {
      this.applicationService.createApplication(this.applicationForm).subscribe({
        next: (response: ApiResponse) => {
          if (response.success) {
            this.successMessage = 'Application créée avec succès';
            setTimeout(() => {
              this.router.navigate(['/applications', response.data.id]);
            }, 1500);
          } else {
            this.errorMessage = response.message || 'Échec de la création';
          }
          this.loading = false;
        },
        error: (error) => {
          this.errorMessage = error.error?.message || 'Erreur lors de la création';
          this.loading = false;
        }
      });
    }
  }

  validateForm(): boolean {
    if (!this.applicationForm.code?.trim()) {
      this.errorMessage = 'Le code est requis';
      return false;
    }

    if (!this.validateCodeFormat()) {
      return false;
    }

    if (!this.applicationForm.name?.trim()) {
      this.errorMessage = 'Le nom est requis';
      return false;
    }

    if (!this.applicationForm.clientName?.trim()) {
      this.errorMessage = 'Le nom du client est requis';
      return false;
    }

    

    return true;
  }

  validateCodeFormat(): boolean {
    const pattern = /^APP-\d{4}-\d{3}$/;
    if (!pattern.test(this.applicationForm.code)) {
      this.errorMessage = 'Format invalide. Utilisez APP-AAAA-XXX (ex: APP-2024-001)';
      return false;
    }
    return true;
  }

  canReturnToAutoStatus(): boolean {
    if (!this.applicationForm.dateDebut) return false;
    const today = new Date();
    const startDate = new Date(this.applicationForm.dateDebut);
    return today >= startDate;
  }

  goBack(): void {
    this.location.back();
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  getStatusLabel(status: string): string {
    const option = this.statusOptions.find(opt => opt.value === status);
    return option ? option.label : status;
  }

  shouldShowChefField(): boolean {
    return this.isAdmin;
  }

  getCurrentChefName(): string {
    if (!this.currentUser) return '';
    return `${this.currentUser.firstName} ${this.currentUser.lastName}`;
  }
}