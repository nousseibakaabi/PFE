import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ApplicationService, Application ,ApplicationRequest, ApiResponse } from '../../services/application.service';
import { ConventionService } from '../../services/convention.service';
import { AuthService } from '../../services/auth.service';
import { environment } from '../../../environments/environment';
import { UserService } from 'src/app/services/user.service';
import { HistoryService, HistoryEntry } from '../../services/history.service';
import { WorkloadService, WorkloadCheck, AlternativeChef, WorkloadDTO } from '../../services/workload.service';
import { Location } from '@angular/common';

@Component({
  selector: 'app-application-detail',
  templateUrl: './application-detail.component.html',
  styleUrls: ['./application-detail.component.css']
})
export class ApplicationDetailComponent implements OnInit {
  application: Application | null = null;
  conventions: any[] = [];
  dateSummary: any = null;
  loading = false;
  syncing = false;
  errorMessage = '';
  successMessage = '';
  structureResponsableName: string = '';
  structureResponsableEmail: string = '';
  structureResponsablePhone: string = '';
  currentUser: any = null;
  isChefProjet = false;
  isAdmin = false;     
  showAssignChefModal = false;
  availableChefs: any[] = [];
  selectedChefId: number | null = null;
  assigning = false;


  showTerminateModal = false;
terminating = false;
terminationReason = '';

  applicationHistory: HistoryEntry[] = [];
  loadingHistory = false;
  Object = Object;
  showAllHistoryModal = false;

  chefWorkload: any = null;

  showReassignmentModal = false;



  Workload: WorkloadDTO | null = null;



  groupedApplicationHistory: { date: string, entries: HistoryEntry[] }[] = [];

  activeTab: 'details' | 'conventions' | 'history' | 'chef' = 'details';


  showWorkloadWarning = false;
  workloadCheck: WorkloadCheck | null = null;
  workloadLoading = false;
  alternativeChefs: AlternativeChef[] = [];

  Math = Math;




  baseUrl = environment.apiUrl;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private applicationService: ApplicationService,
    private conventionService: ConventionService,
    private authService: AuthService,
    private userService: UserService,
    public historyService: HistoryService,
    private workloadService: WorkloadService,
    private location: Location
    ) {}

  ngOnInit(): void {

        this.loadCurrentUser();


    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadApplicationDetails(+id);
      this.loadApplicationConventions(+id);
      this.loadDateSummary(+id);
      this.loadApplicationHistory(); 
    }
  }






loadChefWorkload(): void {
  if (!this.application || !this.application.chefDeProjetId) {
    this.chefWorkload = null;
    return;
  }

  this.workloadLoading = true;
  
  // CORRECTION: Utiliser l'ID du chef de projet de l'application, pas le currentUser
  const chefId = this.application.chefDeProjetId;
  
  this.workloadService.getWorkloadDashboard().subscribe({
    next: (response) => {
      if (response.success && response.data?.workloads) {
        // Find the workload for the application's chef
        this.chefWorkload = response.data.workloads.find(
          (w: WorkloadDTO) => w.chefId === chefId
        ) || null;
        
        console.log('Chef workload loaded:', this.chefWorkload);
      }
      this.workloadLoading = false;
    },
    error: (error) => {
      console.error('Error loading chef workload:', error);
      this.workloadLoading = false;
      this.chefWorkload = null;
    }
  });
}
  

    loadCurrentUser(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.isAdmin = this.authService.isAdmin();
    this.isChefProjet = this.authService.isChefProjet();
  }


  loadStructureInfo(): void {
  if (this.conventions && this.conventions.length > 0) {
    this.structureResponsableName = this.conventions[0].structureResponsableName;
    this.structureResponsableEmail = this.conventions[0].structureResponsableEmail;
    this.structureResponsablePhone = this.conventions[0].structureResponsablePhone;
  }
  }

// Update your existing getChefInitials method to accept the chef parameter
getChefInitials(chef: any): string {
  if (!chef) return '?';
  
  let initials = '';
  if (chef.firstName && chef.firstName.length > 0) {
    initials += chef.firstName.charAt(0).toUpperCase();
  }
  if (chef.lastName && chef.lastName.length > 0) {
    initials += chef.lastName.charAt(0).toUpperCase();
  }
  
  if (initials.length === 0 && chef.username && chef.username.length > 0) {
    initials = chef.username.charAt(0).toUpperCase();
  }
  
  return initials || '?';
}



getChefAvatarUrl(): string {
  // If no chef assigned, return default avatar
  if (!this.application || !this.application.chefDeProjetFullName) {
    return this.generateDefaultChefAvatar('?');
  }
  
  // If the chef has a profile image from the backend, use it!
  if (this.application.chefDeProjetProfileImage) {
    const profileImage = this.application.chefDeProjetProfileImage;
    
    // If it's already a full URL, use it directly
    if (profileImage.startsWith('http')) {
      return profileImage;
    }
    
    // If it's a relative path starting with /uploads/, prepend base URL
    if (profileImage.startsWith('/uploads/')) {
      return this.baseUrl + profileImage;
    }
    
    // If it's base64, use it directly
    if (profileImage.startsWith('data:image')) {
      return profileImage;
    }
    
    // If it's just a filename, assume it's in uploads
    return this.baseUrl + '/uploads/avatars/' + profileImage;
  }
  
  // Otherwise generate avatar with initials
  let initials = '?';
  const names = this.application.chefDeProjetFullName.split(' ');
  if (names.length >= 2) {
    initials = (names[0][0] + names[1][0]).toUpperCase();
  } else if (names.length === 1 && names[0]) {
    initials = names[0][0].toUpperCase();
  }
  
  return this.generateDefaultChefAvatar(initials);
}



generateDefaultChefAvatar(initials: string): string {
  // Colors for different initials - use MULTIPLE colors like in your sidebar!
  const colors = [
    '#e9d709'
  ];
  
  // Pick a color based on the first character
  const colorIndex = initials.charCodeAt(0) % colors.length;
  const color = colors[colorIndex];
  
  // Create SVG with rounded square
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="100" height="100">
    <!-- Rectangle with rounded corners -->
    <rect width="100" height="100" rx="15" fill="${color}"/>
    
    <!-- Center the text -->
    <text x="50" y="58" 
          text-anchor="middle" 
          font-family="Arial, Helvetica, sans-serif" 
          font-size="38" 
          font-weight="bold" 
          fill="white"
          dominant-baseline="middle">
      ${initials}
    </text>
  </svg>`;
  
  return 'data:image/svg+xml;base64,' + btoa(svg);
}

// Add error handler for avatar images
handleChefImageError(event: any): void {
  console.error('Chef avatar failed to load');
  // Generate new avatar with initials
  if (this.application?.chefDeProjetFullName) {
    const names = this.application.chefDeProjetFullName.split(' ');
    let initials = '?';
    if (names.length >= 2) {
      initials = (names[0][0] + names[1][0]).toUpperCase();
    } else if (names.length === 1) {
      initials = names[0][0].toUpperCase();
    }
    event.target.src = this.generateDefaultChefAvatar(initials);
  } else {
    event.target.src = this.generateDefaultChefAvatar('?');
  }
  event.target.onerror = null; // Prevent infinite loop
}


loadApplicationDetails(id: number): void {
  this.loading = true;
  this.applicationService.getApplication(id).subscribe({
    next: (response) => {
      if (response.success) {
        this.application = response.data;
        console.log('Application data:', this.application);
        console.log('Chef profile image:', this.application?.chefDeProjetProfileImage);
        console.log('Chef full name:', this.application?.chefDeProjetFullName);
        
        // Load chef workload if chef is assigned
        if (this.application?.chefDeProjetId) {
          this.loadChefWorkload();
        }
      } else {
        this.errorMessage = 'Failed to load application details';
      }
      this.loading = false;
    },
    error: (error) => {
      console.error('Error loading application:', error);
      this.errorMessage = 'Failed to load application details';
      this.loading = false;
    }
  });
}


  
  loadApplicationConventions(id: number): void {
    this.conventionService.getConventionsByApplication(id).subscribe({
      next: (response) => {
        if (response.success) {
          this.conventions = response.data;
          this.loadStructureInfo(); // Add this line
        }
      },
      error: (error) => {
        console.error('Error loading conventions:', error);
      }
    });
  }


  loadDateSummary(id: number): void {
    this.applicationService.getApplicationDateSummary(id).subscribe({
      next: (response) => {
        if (response.success) {
          this.dateSummary = response.data;
        }
      },
      error: (error) => {
        console.error('Error loading date summary:', error);
      }
    });
  }

  syncDates(): void {
    if (!this.application) return;

    this.syncing = true;
    this.conventionService.syncApplicationDates(this.application.id).subscribe({
      next: (response) => {
        if (response.success) {
          this.successMessage = 'Dates synchronisées avec succès';
          // Reload data
          this.loadApplicationDetails(this.application!.id);
          this.loadDateSummary(this.application!.id);
        } else {
          this.errorMessage = response.message || 'Failed to sync dates';
        }
        this.syncing = false;
      },
      error: (error) => {
        console.error('Error syncing dates:', error);
        this.errorMessage = 'Failed to sync application dates';
        this.syncing = false;
      }
    });
  }

  goBack(): void {
    this.location.back();
  }

  editApplication(): void {
    if (this.application) {
      this.router.navigate(['/applications/edit', this.application.id]);
    }
  }

  createConvention(): void {
    if (this.application) {
      this.router.navigate(['/conventions/new'], {
        queryParams: { applicationId: this.application.id }
      });
    }
  }

  viewConvention(id: number): void {
    this.router.navigate(['/conventions', id]);
  }


formatDate(dateString: string | null | undefined): string {
  if (!dateString) return '-';
  const date = new Date(dateString);
  const day = date.getDate().toString().padStart(2, '0');
  const month = (date.getMonth() + 1).toString().padStart(2, '0');
  const year = date.getFullYear();
  return `${day}/${month}/${year}`;
}

getStatusClass(status: string | null | undefined): string {
  switch (status) {
    case 'PLANIFIE': return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-300';
    case 'EN_COURS': return 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300';
    case 'TERMINE': return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300';
    default: return 'bg-gray-100 text-gray-800';
  }
}

getStatusLabel(status: string | null | undefined): string {
  if (status === null || status === undefined) return '-';
  
  switch (status) {
    case 'PLANIFIE': return 'Planifié';
    case 'EN_COURS': return 'En Cours';
    case 'TERMINE': return 'Terminé';
    default: return status;
  }
}

  getConventionStatusClass(etat: string): string {
    switch (etat) {
      case 'PLANIFIE': return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-300';
      case 'EN COURS': return 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300';
      case 'TERMINE': return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300';
      case 'ARCHIVE': return 'bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-300';
      default: return 'bg-gray-100 text-gray-800';
    }
  }


  getConventionStatusLabel(etat: string | null): string {
  if (etat === null) return '-';
  
  switch (etat) {
    case 'PLANIFIE': return 'Planifié';
    case 'EN COURS': return 'En Cours';
    case 'TERMINE': return 'Terminé';
    case 'ARCHIVE': return 'Archivé';
    default: return etat;
  }
}

  
  canEdit(): boolean {
    return this.authService.isAdmin() || this.authService.isChefProjet();
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }



  loadAvailableChefs(): void {
  this.userService.getChefsProjet().subscribe({
    next: (response) => {
      if (response.success) {
        this.availableChefs = response.data;
      }
    },
    error: (error) => {
      console.error('Error loading chefs:', error);
    }
  });
}

// Open assign modal
openAssignChefModal(): void {
  this.loadAvailableChefs();
  this.selectedChefId = null;
  this.showAssignChefModal = true;
}

// Close assign modal
closeAssignChefModal(): void {
  this.showAssignChefModal = false;
  this.selectedChefId = null;
  this.availableChefs = [];
}

// Select a chef
selectChef(chef: any): void {
  this.selectedChefId = chef.id;
  
  // Check workload when chef is selected
  if (this.application && chef.id) {
    this.checkWorkload(chef.id);
  }
}


checkWorkload(chefId: number): void {
  if (!this.application) return;
  
  this.workloadLoading = true;
  this.workloadService.checkAssignment(chefId, this.application.id).subscribe({
    next: (response) => {
      if (response.success && response.data) {
        this.workloadCheck = response.data;
        this.showWorkloadWarning = !response.data.canAssign && !response.data.assignWithCaution;
        this.alternativeChefs = response.data.alternativeChefs || [];
      }
      this.workloadLoading = false;
    },
    error: (error) => {
      console.error('Error checking workload:', error);
      this.workloadLoading = false;
    }
  });
}

assignChef(): void {
  if (!this.selectedChefId || !this.application) return;
  
  this.assigning = true;
  
  this.workloadService.assignWithWorkloadCheck(
    this.selectedChefId, 
    this.application.id, 
    false
  ).subscribe({
    next: (response) => {
      if (response.success) {
        this.successMessage = response.warning 
          ? 'Chef de projet assigné avec avertissement (charge élevée)'
          : 'Chef de projet assigné avec succès';
        this.loadApplicationDetails(this.application!.id);
        
        // IMPORTANT: Load the workload after assignment
        setTimeout(() => {
          this.loadChefWorkload();
        }, 500);
        
        this.closeAssignChefModal();
      } else {
        this.errorMessage = response.message || 'Échec de l\'assignation';
      }
      this.assigning = false;
    },
    error: (error) => {
      console.error('Error assigning chef:', error);
      this.errorMessage = error.error?.message || 'Échec de l\'assignation';
      this.assigning = false;
    }
  });
}

// Add method to force assign despite workload warning
forceAssignChef(): void {
  if (!this.selectedChefId || !this.application) return;
  
  if (confirm('Attention: Ce chef de projet a une charge de travail élevée. Voulez-vous vraiment l\'assigner?')) {
    this.assigning = true;
    
    this.workloadService.assignWithWorkloadCheck(
      this.selectedChefId, 
      this.application.id, 
      true // force = true
    ).subscribe({
      next: (response) => {
        if (response.success) {
          this.successMessage = 'Chef de projet assigné (forcé)';
          this.loadApplicationDetails(this.application!.id);
          this.closeAssignChefModal();
        } else {
          this.errorMessage = response.message || 'Échec de l\'assignation';
        }
        this.assigning = false;
      },
      error: (error) => {
        console.error('Error assigning chef:', error);
        this.errorMessage = error.error?.message || 'Échec de l\'assignation';
        this.assigning = false;
      }
    });
  }
}

// Helper method to get chef avatar URL for modal
getChefAvatarUrlForUser(chef: any): string {
  if (!chef || !chef.profileImage) {
    // Generate avatar with initials
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

// Error handler for chef images in modal
handleChefImageErrorForUser(event: any, chef: any): void {
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



loadApplicationHistory(): void {
  if (!this.application) return;
  
  this.loadingHistory = true;
  this.historyService.getHistoryByEntity('APPLICATION', this.application.id).subscribe({
    next: (response) => {
      if (response.success) {
        this.applicationHistory = response.data;
        this.groupApplicationHistory();
      }
      this.loadingHistory = false;
    },
    error: (error) => {
      console.error('Error loading application history:', error);
      this.loadingHistory = false;
    }
  });
}

// Add this new method to group history by date
groupApplicationHistory(): void {
  const groups: { [key: string]: HistoryEntry[] } = {};
  
  this.applicationHistory.forEach(entry => {
    if (!groups[entry.dateFormatted]) {
      groups[entry.dateFormatted] = [];
    }
    groups[entry.dateFormatted].push(entry);
  });
  
  this.groupedApplicationHistory = Object.keys(groups)
    .sort((a, b) => {
      // Parse dates for sorting (DD/MM/YYYY format)
      const [aDay, aMonth, aYear] = a.split('/');
      const [bDay, bMonth, bYear] = b.split('/');
      const dateA = new Date(`${aYear}-${aMonth}-${aDay}`);
      const dateB = new Date(`${bYear}-${bMonth}-${bDay}`);
      return dateB.getTime() - dateA.getTime(); // Descending (newest first)
    })
    .map(date => ({
      date,
      entries: groups[date].sort((a, b) => 
        new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
      )
    }));
}


// Add this method to ApplicationDetailComponent
getChangedFieldsArray(entry: HistoryEntry): { field: string, old: any, new: any }[] {
  const changes = this.historyService.getChangedFields(entry.oldValues, entry.newValues);
  return Object.keys(changes).map(key => ({
    field: this.formatFieldName(key),
    old: changes[key].old,
    new: changes[key].new
  }));
}

formatFieldName(field: string): string {
  const fieldMap: { [key: string]: string } = {
    'code': 'Code',
    'name': 'Nom',
    'description': 'Description',
    'clientName': 'Nom client',
    'clientEmail': 'Email client',
    'clientPhone': 'Téléphone client',
    'dateDebut': 'Date début',
    'dateFin': 'Date fin',
    'status': 'Statut',
    'minUser': 'Utilisateurs min',
    'maxUser': 'Utilisateurs max',
    'chefDeProjet': 'Chef de projet'
  };
  return fieldMap[field] || field;
}

getHistoryActionClass(actionType: string): string {
  switch (actionType) {
    case 'CREATE': return 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300';
    case 'UPDATE': return 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300';
    case 'DELETE': return 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300';
    case 'ASSIGN_CHEF': return 'bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-300';
    case 'STATUS_CHANGE': return 'bg-orange-100 text-orange-800 dark:bg-orange-900/30 dark:text-orange-300';
    case 'DATES_SYNC': return 'bg-cyan-100 text-cyan-800 dark:bg-cyan-900/30 dark:text-cyan-300';
    
    default: return 'bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-300';
  }
}

getHistoryIcon(actionType: string): string {
  return this.historyService.getActionIcon(actionType);
}

formatChangeValue(value: any): string {
  if (value === null || value === undefined) return '-';
  if (typeof value === 'object') return JSON.stringify(value);
  if (typeof value === 'number') return value.toString();
  if (value instanceof Date) return value.toLocaleDateString();
  return value.toString();
}

openAllHistoryModal(): void {
  this.showAllHistoryModal = true;
}

closeAllHistoryModal(): void {
  this.showAllHistoryModal = false;
}

// Add to component class
objectKeys(obj: any): string[] {
  return obj ? Object.keys(obj) : [];
}

groupHistoryByDate(history: HistoryEntry[]): { date: string, entries: HistoryEntry[] }[] {
  const groups: { [key: string]: HistoryEntry[] } = {};
  
  history.forEach(entry => {
    if (!groups[entry.dateFormatted]) {
      groups[entry.dateFormatted] = [];
    }
    groups[entry.dateFormatted].push(entry);
  });
  
  return Object.keys(groups)
    .sort((a, b) => b.localeCompare(a)) // Sort descending (newest first)
    .map(date => ({
      date,
      entries: groups[date].sort((a, b) => b.timestamp.localeCompare(a.timestamp))
    }));
}


/**
 * Check if application can be terminated
 */
checkCanTerminate(application: Application | null): boolean {
  return !!(application && (application.status === 'PLANIFIE' || application.status === 'EN_COURS'));
}




/**
 * Get termination badge class
 */
getTerminationBadgeClass(application: Application): string {
  if (application.terminatedEarly) {
    return 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300';
  } else if (application.terminatedOnTime) {
    return 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300';
  } else if (application.terminatedLate) {
    return 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300';
  }
  return 'bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-300';
}

/**
 * Get termination status text
 */
getTerminationStatusText(application: Application): string {
  if (application.terminatedEarly) {
    return 'Terminée en avance';
  } else if (application.terminatedOnTime) {
    return 'Terminée à temps';
  } else if (application.terminatedLate) {
    return 'Terminée en retard';
  }
  return '';
}

/**
 * Format days remaining at termination
 */
formatDaysRemainingAtTermination(application: Application): string {
  if (application.daysRemainingAtTermination === undefined || application.daysRemainingAtTermination === null) {
    return 'Non disponible';
  }
  
  const days = application.daysRemainingAtTermination;
  
  if (days > 0) {
    return `${days} jour${days > 1 ? 's' : ''} avant échéance`;
  } else if (days < 0) {
    const absDays = Math.abs(days);
    return `${absDays} jour${absDays > 1 ? 's' : ''} après échéance`;
  } else {
    return 'Le jour même de l\'échéance';
  }
}



isAppCreator(): boolean {
  const currentUser = this.authService.getCurrentUser();
  return this.application?.createdById === currentUser?.id;
}

openReassignmentRequestModal(): void {
  this.showReassignmentModal = true;
}

closeReassignmentRequestModal(): void {
  this.showReassignmentModal = false;
}


// Add this method to your ApplicationDetailComponent class
getChefInitialsFromName(fullName: string): string {
  if (!fullName) return '?';
  const parts = fullName.split(' ');
  if (parts.length >= 2) {
    return (parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
  }
  return fullName.charAt(0).toUpperCase();
}


openTerminateModal(): void {
  this.terminationReason = '';
  this.showTerminateModal = true;
}

/**
 * Close terminate modal
 */
closeTerminateModal(): void {
  this.showTerminateModal = false;
  this.terminationReason = '';
}

/**
 * Confirm and execute termination
 */
confirmTerminate(): void {
  if (!this.application) return;
  
  this.terminating = true;
  
  this.applicationService.manuallyTerminateApplication(
    this.application.id, 
    this.terminationReason || undefined
  ).subscribe({
    next: (response: any) => {
      if (response.success) {
        this.successMessage = 'Application marquée comme terminée avec succès';
        
        // Show termination details
        if (response.terminationInfo) {
          const info = response.terminationInfo;
          let terminationDetail = '';
          
          if (info.daysRemaining > 0) {
            terminationDetail = `Terminée ${info.daysRemaining} jours avant l'échéance`;
          } else if (info.daysRemaining < 0) {
            terminationDetail = `Terminée ${Math.abs(info.daysRemaining)} jours après l'échéance`;
          } else if (info.daysRemaining === 0) {
            terminationDetail = 'Terminée le jour de l\'échéance';
          }
          
          if (terminationDetail) {
            this.successMessage += ` - ${terminationDetail}`;
          }
        }
        
        // Reload application data
        this.loadApplicationDetails(this.application!.id);
        this.closeTerminateModal();
      } else {
        this.errorMessage = response.message || 'Erreur lors de la termination';
      }
      this.terminating = false;
    },
    error: (error) => {
      console.error('Error terminating application:', error);
      this.errorMessage = error.error?.message || 'Erreur lors de la termination';
      this.terminating = false;
    }
  });
}

/**
 * Update the terminate button to open modal instead of direct termination
 */
// Replace your existing terminateApplication() method with this:
terminateApplication(): void {
  if (!this.application || !this.checkCanTerminate(this.application)) {
    this.errorMessage = 'Cette application ne peut pas être terminée';
    return;
  }
  
  // Open modal instead of prompt
  this.openTerminateModal();
}

}