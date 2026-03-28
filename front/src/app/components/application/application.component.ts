import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ApplicationService, Application, ApiResponse } from '../../services/application.service';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { WorkloadService, WorkloadDTO } from '../../services/workload.service'; // ADD THIS
import { DatePipe } from '@angular/common';
import { environment } from 'src/environments/environment';

@Component({
  selector: 'app-application',
  templateUrl: './application.component.html',
  styleUrls: ['./application.component.css'],
  providers: [DatePipe]
})
export class ApplicationComponent implements OnInit {
  applications: Application[] = [];
  filteredApplications: Application[] = [];
  chefsProjet: any[] = []; // Only for admin view
  
  loading = false;
  workloadLoading = false;
  errorMessage = '';
  successMessage = '';
  searchTerm = '';
  filterStatus = '';
  filterCategory = '';
  filterClient = '';

  categoryOptions: string[] = ['All', 'UI Designer', 'UX Designer', 'Developer', 'QA'];
  customerOptions: string[] = [];

  // Current user info
  currentUser: any = null;
  isAdmin = false;
  isChefProjet = false;

  showAssignModal = false;
  selectedAppForAssign: Application | null = null;
  availableChefs: any[] = [];
  selectedChefId: number | null = null;
  assigning = false;
  
  // NEW: Workload check properties
  workloadCheck: any = null;
  showWorkloadWarning = false;
  alternativeChefs: any[] = [];
  workloadLoadingForChef = false;
  forceAssignMode = false;
  chefsWorkload: Map<number, any> = new Map();


  selectedApplication: Application | null = null;

  baseUrl = environment.baseUrl;

  Math = Math; 

  // Status options
  statusOptions = [
    { value: 'PLANIFIE', label: 'Planifié' },
    { value: 'EN_COURS', label: 'En Cours' },
    { value: 'TERMINE', label: 'Terminé' },
  ];

  // Pagination controls
  itemsPerPage = 6;
  currentPage = 1;

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredApplications.length / this.itemsPerPage));
  }

  get pagedApplications(): Application[] {
    const start = (this.currentPage - 1) * this.itemsPerPage;
    return this.filteredApplications.slice(start, start + this.itemsPerPage);
  }

  constructor(
    private applicationService: ApplicationService,
    private userService: UserService,
    private authService: AuthService,
    private router: Router,
    private datePipe: DatePipe,
    private workloadService: WorkloadService // ADD THIS
  ) {}

  ngOnInit(): void {
    this.loadCurrentUser();
    this.loadApplications();
    
    if (this.isAdmin) {
      this.loadChefsProjet();
    }
  }

  loadCurrentUser(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.isAdmin = this.authService.isAdmin();
    this.isChefProjet = this.authService.isChefProjet();
  }

  loadApplications(): void {
    this.loading = true;
    this.applicationService.getAllApplications().subscribe({
      next: (response: ApiResponse) => {
        if (response.success) {
          let allApps = response.data || response.applications || [];
          
          // Filter for chef de projet
          if (this.isChefProjet && !this.isAdmin && this.currentUser) {
            this.applications = allApps.filter((app: Application) => 
              app.chefDeProjetId === this.currentUser.id
            );
          } else {
            this.applications = allApps;
          }
          
          this.customerOptions = [...new Set(this.applications.map(app => app.clientName || 'Unknown'))].filter(name => !!name);
          this.filteredApplications = [...this.applications];
          this.currentPage = 1;
        } else {
          this.errorMessage = response.message || 'Erreur lors du chargement';
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading applications:', error);
        this.errorMessage = 'Erreur de connexion au serveur';
        this.loading = false;
      }
    });
  }

  loadChefsProjet(): void {
    if (!this.isAdmin) return;
    
    this.userService.getChefsProjet().subscribe({
      next: (response) => {
        if (response.success) {
          this.chefsProjet = response.data;
          // Load workload data for all chefs
          this.loadChefsWorkload();
        }
      },
      error: (error) => {
        console.error('Error loading chefs:', error);
      }
    });
  }



  // Add this method to calculate progress based on status and dates
calculateProgress(app: Application): number {
  // If status is TERMINE, return 100%
  if (app.status === 'TERMINE') {
    return 100;
  }
  
  // If status is PLANIFIE, return 0%
  if (app.status === 'PLANIFIE') {
    return 0;
  }
  
  // For EN_COURS, calculate based on dates
  if (app.status === 'EN_COURS' && app.dateDebut && app.dateFin) {
    const start = new Date(app.dateDebut);
    const end = new Date(app.dateFin);
    const now = new Date();
    
    // If end date is in the past, return 100%
    if (now > end) {
      return 100;
    }
    
    // If start date is in the future, return 0%
    if (now < start) {
      return 0;
    }
    
    const totalDuration = end.getTime() - start.getTime();
    const elapsedDuration = now.getTime() - start.getTime();
    const progress = (elapsedDuration / totalDuration) * 100;
    
    return Math.min(100, Math.max(0, Math.round(progress)));
  }
  
  return 0;
}

// Get progress color
getProgressColor(progress: number): string {
  if (progress >= 90) return '#10B981';
  if (progress >= 70) return '#3B82F6';
  if (progress >= 40) return '#F59E0B';
  if (progress >= 20) return '#F97316';
  return '#EF4444';
}

// Simplified version with only essential fields - Each attribute in its own column
exportToCSV(): void {
  const csvData = this.filteredApplications.map(app => ({
    'Code': app.code,
    'Nom': app.name || '',
    'Statut': this.getStatusLabel(app.status),
    'Client': app.clientName || '',
    'Chef de Projet': app.chefDeProjetFullName || 'Non assigné',
    'Progression': `${this.calculateProgress(app)}%`,
    'Date Début': this.formatDate(app.dateDebut),
    'Date Fin': this.formatDate(app.dateFin),
    'Conventions': app.conventionsCount || 0
  }));

  if (csvData.length === 0) {
    this.errorMessage = 'Aucune donnée à exporter';
    setTimeout(() => this.clearMessages(), 3000);
    return;
  }

  // Get headers
  const headers = Object.keys(csvData[0]);
  
  // Escape function for CSV values
  const escapeCSV = (value: any): string => {
    if (value === null || value === undefined) return '';
    const stringValue = String(value);
    // Wrap in quotes if contains comma, quote, or newline
    if (stringValue.includes(',') || stringValue.includes('"') || stringValue.includes('\n')) {
      return `"${stringValue.replace(/"/g, '""')}"`;
    }
    return stringValue;
  };

  // Create CSV rows with proper column separation
  const csvRows = [
    headers.join(','), // Header row
    ...csvData.map(row => 
      headers.map(header => escapeCSV(row[header as keyof typeof row])).join(',')
    )
  ];
  
  const csvString = csvRows.join('\n');
  
  // Create blob with UTF-8 BOM for proper Excel encoding
  const blob = new Blob(['\uFEFF' + csvString], { type: 'text/csv;charset=utf-8;' });
  const link = document.createElement('a');
  const url = URL.createObjectURL(blob);
  
  link.setAttribute('href', url);
  link.setAttribute('download', `applications_${new Date().toISOString().split('T')[0]}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
  
  this.successMessage = `${csvData.length} application(s) exportée(s)`;
  setTimeout(() => this.clearMessages(), 3000);
}

  // NEW: Load workload for all chefs
  loadChefsWorkload(): void {
    this.workloadLoading = true;
    this.workloadService.getWorkloadDashboard().subscribe({
      next: (response) => {
        if (response.success && response.data?.workloads) {
          response.data.workloads.forEach((w: WorkloadDTO) => {
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


  
  getProgressClass(progress: number): string {
    if (progress >= 90) return 'bg-green-500';
    if (progress >= 70) return 'bg-blue-500';
    if (progress >= 50) return 'bg-yellow-500';
    if (progress >= 30) return 'bg-orange-500';
    return 'bg-red-500';
  }

  // NEW: Get workload for a specific chef
  getChefWorkload(chefId: number): WorkloadDTO | undefined {
    return this.chefsWorkload.get(chefId);
  }

  // NEW: Get workload color
  getWorkloadColor(workload: number): string {
    return this.workloadService.getWorkloadClass(workload);
  }

  // NEW: Get progress bar class
  getWorkloadBarClass(workload: number): string {
    return this.workloadService.getProgressBarClass(workload);
  }

  // NEW: Get workload status
  getWorkloadStatus(workload: number): string {
    return this.workloadService.getWorkloadStatus(workload);
  }

  searchApplications(): void {
    const baseList = this.filterByStatus(this.applications);

    if (!this.searchTerm.trim()) {
      this.filteredApplications = baseList;
      this.ensureValidPage();
      return;
    }

    const term = this.searchTerm.toLowerCase();
    this.filteredApplications = baseList.filter((app: Application) =>
      (app.code || '').toLowerCase().includes(term) ||
      (app.name || '').toLowerCase().includes(term) ||
      (app.clientName || '').toLowerCase().includes(term) ||
      (app.description || '').toLowerCase().includes(term)
    );
    this.ensureValidPage();
  }

  filterByStatus(applications: Application[]): Application[] {
    let result = [...applications];

    if (this.filterStatus) {
      result = result.filter((app: Application) => app.status === this.filterStatus);
    }

    if (this.filterCategory && this.filterCategory !== 'All') {
      result = result.filter((app: Application) => {
        const category = (app as any).category || '';
        return category.toLowerCase().includes(this.filterCategory.toLowerCase());
      });
    }

    if (this.filterClient) {
      result = result.filter((app: Application) => (app.clientName || '').toLowerCase().includes(this.filterClient.toLowerCase()));
    }

    return result;
  }

  applyFilters(): void {
    this.filteredApplications = this.filterByStatus(this.applications);
    if (this.searchTerm.trim()) {
      this.searchApplications();
      return;
    }
    this.ensureValidPage();
  }

  onItemsPerPageChange(): void {
    this.currentPage = 1;
  }

  goToPage(page: number): void {
    this.currentPage = Math.min(this.totalPages, Math.max(1, page));
  }

  prevPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
    }
  }

  ensureValidPage(): void {
    if (this.currentPage > this.totalPages) {
      this.currentPage = this.totalPages;
    }
    if (this.currentPage < 1) {
      this.currentPage = 1;
    }
  }

  // Navigation methods
  viewApplication(id: number): void {
    this.router.navigate(['/applications', id]);
  }

  createApplication(): void {
    this.router.navigate(['/applications/new']);
  }

  editApplication(application: Application): void {
    // For chef de projet, check ownership
    if (this.isChefProjet && !this.isAdmin && application.chefDeProjetId !== this.currentUser?.id) {
      this.errorMessage = 'Vous ne pouvez modifier que vos propres applications';
      return;
    }
    this.router.navigate(['/applications/edit', application.id]);
  }

  downloadApplication(application: Application): void {
    console.log('Download app', application);
    // TODO: implement actual download (PDF/CSV) logic
  }

  // Utility methods
  getStatusClass(status: string): string {
    switch (status) {
      case 'PLANIFIE': return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-300';
      case 'EN_COURS': return 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300';
      case 'TERMINE': return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  getStatusLabel(status: string): string {
    const option = this.statusOptions.find(opt => opt.value === status);
    return option ? option.label : status;
  }

  formatDate(dateString: string): string {
    if (!dateString) return '-';
    return this.datePipe.transform(dateString, 'dd/MM/yyyy') || '-';
  }

  // Statistics methods
  getTotalApplicationsCount(): number {
    return this.applications.length;
  }

  getEnCoursCount(): number {
    return this.applications.filter(a => a.status === 'EN_COURS').length;
  }

  getPlanifieCount(): number {
    return this.applications.filter(a => a.status === 'PLANIFIE').length;
  }

  getTerminesCount(): number {
    return this.applications.filter(a => a.status === 'TERMINE').length;
  }

  getDelayedCount(): number {
    return this.applications.filter(a => a.isDelayed).length;
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  isApplicationDelayed(application: Application): boolean {
    if (!application.dateFin) return false;
    const today = new Date();
    const endDate = new Date(application.dateFin);
    return today > endDate && application.status === 'EN_COURS';
  }

  loadAvailableChefs(): void {
    this.userService.getChefsProjet().subscribe({
      next: (response) => {
        if (response.success) {
          this.availableChefs = response.data;
          // Load workload data for available chefs
          if (!this.chefsWorkload.size) {
            this.loadChefsWorkload();
          }
        }
      },
      error: (error) => {
        console.error('Error loading chefs:', error);
        this.errorMessage = 'Failed to load chefs';
      }
    });
  }

// Update openAssignModal method
openAssignModal(application: Application): void {
  this.selectedAppForAssign = application;
  this.loadAvailableChefs();
  
  // Also load workload dashboard to get workload data for chefs
  this.workloadService.getWorkloadDashboard().subscribe({
    next: (response) => {
      if (response.success && response.data?.workloads) {
        response.data.workloads.forEach((w: any) => {
          this.chefsWorkload.set(w.chefId, w);
        });
      }
    },
    error: (error) => {
      console.error('Error loading workload:', error);
    }
  });
  
  this.selectedChefId = null;
  this.workloadCheck = null;
  this.showWorkloadWarning = false;
  this.alternativeChefs = [];
  this.forceAssignMode = false;
  this.showAssignModal = true;
}

  // Close assign modal
  closeAssignModal(): void {
    this.showAssignModal = false;
    this.selectedAppForAssign = null;
    this.availableChefs = [];
    this.selectedChefId = null;
    this.assigning = false;
    this.workloadCheck = null;
    this.showWorkloadWarning = false;
    this.alternativeChefs = [];
    this.forceAssignMode = false;
  }

  // Select a chef and check workload
  selectChef(chef: any): void {
    this.selectedChefId = chef.id;
    this.forceAssignMode = false;
    
    if (this.selectedAppForAssign) {
      this.checkWorkload(chef.id);
    }
  }

  // NEW: Check workload for selected chef
  checkWorkload(chefId: number): void {
    if (!this.selectedAppForAssign) return;
    
    this.workloadLoadingForChef = true;
    this.workloadService.checkAssignment(chefId, this.selectedAppForAssign.id).subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.workloadCheck = response.data;
          this.showWorkloadWarning = !response.data.canAssign && !response.data.assignWithCaution;
          this.alternativeChefs = response.data.alternativeChefs || [];
        }
        this.workloadLoadingForChef = false;
      },
      error: (error) => {
        console.error('Error checking workload:', error);
        this.workloadLoadingForChef = false;
      }
    });
  }

  // NEW: Assign chef with workload check
  assignChef(): void {
    if (!this.selectedChefId || !this.selectedAppForAssign) return;
    
    this.assigning = true;
    
    this.workloadService.assignWithWorkloadCheck(
      this.selectedChefId,
      this.selectedAppForAssign.id,
      this.forceAssignMode
    ).subscribe({
      next: (response) => {
        if (response.success) {
          this.successMessage = response.warning 
            ? 'Chef de projet assigné (charge élevée)'
            : 'Chef de projet assigné avec succès';
          this.loadApplications(); // Reload the list
          this.closeAssignModal();
          
          // Refresh workload data
          this.loadChefsWorkload();
        } else {
          this.errorMessage = response.message || 'Failed to assign chef';
        }
        this.assigning = false;
      },
      error: (error) => {
        console.error('Error assigning chef:', error);
        this.errorMessage = error.error?.message || 'Failed to assign chef';
        this.assigning = false;
      }
    });
  }

  // NEW: Toggle force assign mode
  toggleForceAssign(): void {
    this.forceAssignMode = !this.forceAssignMode;
  }

  // Get chef avatar URL for the table
  getChefAvatarUrlForApp(app: Application): string {
    if (!app.chefDeProjetFullName) {
      return this.generateDefaultChefAvatar('?');
    }
    
    if (app.chefDeProjetProfileImage) {
      const profileImage = app.chefDeProjetProfileImage;
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
    
    let initials = '?';
    const names = app.chefDeProjetFullName.split(' ');
    if (names.length >= 2) {
      initials = (names[0][0] + names[1][0]).toUpperCase();
    } else if (names.length === 1 && names[0]) {
      initials = names[0][0].toUpperCase();
    }
    
    return this.generateDefaultChefAvatar(initials);
  }

  // Get chef avatar URL for modal
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
      return this.generateDefaultChefAvatar(initials);
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

  // Generate default chef avatar with initials
  generateDefaultChefAvatar(initials: string): string {
    const colors = ['#e9d709'];
    const colorIndex = initials.charCodeAt(0) % colors.length;
    const color = colors[colorIndex];
    
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="100" height="100">
      <rect width="100" height="100" rx="15" fill="${color}"/>
      <text x="50" y="58" text-anchor="middle" font-family="Arial, Helvetica, sans-serif" 
            font-size="38" font-weight="bold" fill="white" dominant-baseline="middle">
        ${initials}
      </text>
    </svg>`;
    
    return 'data:image/svg+xml;base64,' + btoa(svg);
  }

  // Error handler for chef images in table
  handleChefImageErrorForApp(event: any, app: Application): void {
    let initials = '?';
    if (app.chefDeProjetFullName) {
      const names = app.chefDeProjetFullName.split(' ');
      if (names.length >= 2) {
        initials = (names[0][0] + names[1][0]).toUpperCase();
      } else if (names.length === 1) {
        initials = names[0][0].toUpperCase();
      }
    }
    event.target.src = this.generateDefaultChefAvatar(initials);
    event.target.onerror = null;
  }

  // Error handler for chef images in modal
  handleChefImageError(event: any, chef: any): void {
    let initials = '?';
    if (chef?.firstName && chef?.lastName) {
      initials = (chef.firstName[0] + chef.lastName[0]).toUpperCase();
    } else if (chef?.firstName) {
      initials = chef.firstName[0].toUpperCase();
    } else if (chef?.username) {
      initials = chef.username[0].toUpperCase();
    }
    event.target.src = this.generateDefaultChefAvatar(initials);
    event.target.onerror = null;
  }

  getWorkloadClass(workload: number): string {
  if (workload >= 90) return 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300';
  if (workload >= 70) return 'bg-orange-100 text-orange-800 dark:bg-orange-900/30 dark:text-orange-300';
  if (workload >= 40) return 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300';
  return 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300';
}

goToArchivePage(): void {
  this.router.navigate(['/archives/applications']);
}
}