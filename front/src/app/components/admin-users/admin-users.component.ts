import { Component, OnInit, AfterViewInit, HostListener } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { ApiService } from '../../services/api.service';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from 'src/environments/environment';
import { ChartService } from '../partials/services/chart.service';
import { MapService } from '../partials/services/map.service';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TranslationService } from '../partials/traduction/translation.service';
import { ProjectService } from 'src/app/services/project.service';

interface AdminUser {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  lockedByAdmin: boolean;
  accountLockedUntil: string | null;
  failedLoginAttempts: number;
  enabled: boolean;
  department: string;
  roles: string[];
  phone?: string;
  profileImage?: string;

}

interface RoleOption {
  value: string;
  label: string;
}

@Component({
  selector: 'app-admin-users',
  templateUrl: './admin-users.component.html',
  styleUrls: ['./admin-users.component.css']
})
export class AdminUsersComponent implements OnInit, AfterViewInit {
  users: AdminUser[] = [];
  loading = false;
  error = '';
  successMessage: string = '';
  baseUrl = environment.baseUrl || 'http://localhost:8081';

  
  // In AdminUsersComponent class
currentPage = 1;
pageSize = 6;
totalPages = 1;
paginatedUsers: AdminUser[] = [];
  showFilterDropdown = false;
  Math = Math; // Add this line

// In AdminUsersComponent class
showSuccessMessage = false;
showErrorMessage = false;
successMessageType: 'success' | 'info' | 'warning' | null = null;
  showConfirmModal = false;
confirmTitle = '';
confirmMessage = '';
confirmAction: 'lock' | 'unlock' | null = null;
userToConfirm: AdminUser | null = null;

roleFilter: string = '';
statusFilter: string = '';
allUsers: AdminUser[] = []; // Store all users for filtering

unassignedProjects: any[] = [];
  selectedUserForAssignment: AdminUser | null = null;
  showAssignmentModal = false;
  projectsToAssign: any[] = []; // For storing selected projects

  addUserError = '';
  editDepartmentError = '';
  editRoleError = '';

  // New properties for modals
  showAddUserModal = false;
  showEditDepartmentModal = false;
  showEditRoleModal = false;
  availableRoles: RoleOption[] = [];
  
  // Forms
  addUserForm: FormGroup;
  editDepartmentForm: FormGroup;
  editRoleForm: FormGroup;
  
  // Selected user for editing
  selectedUser: AdminUser | null = null;
  
  // Password visibility toggle for add user
  passwordVisible = false;

  private apiUrl = environment.apiUrl;
  private chart01: any;
  private chart02: any;
  private chart03: any;
  private map01: any;

  constructor(
    private authService: AuthService,
    private http: HttpClient,
    private apiService: ApiService,
    private chartService: ChartService,
    private mapService: MapService,
    private fb: FormBuilder,
    private trasnlationService: TranslationService,
    private projectService: ProjectService
  ) {
    // Initialize forms
    this.addUserForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      firstName: [''],
      lastName: [''],
      phone: [''],
      department: [''],
      roles: [[], Validators.required]
    });
    
    this.editDepartmentForm = this.fb.group({
      department: ['', Validators.required]
    });
    
    this.editRoleForm = this.fb.group({
      roles: [[], Validators.required]
    });


  }



  ngOnInit(): void {
    this.loadUsers();
    this.loadAvailableRoles();
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      if (this.isAdmin()) {
        this.initCharts();
        this.initMap();
      }
    }, 100);
  }



  loadAvailableRoles(): void {
  const token = this.authService.token;
  const headers = new HttpHeaders({
    'Authorization': `Bearer ${token}`
  });
  
  this.http.get<any>(`${this.apiUrl}/admin/roles/available`, { headers })
    .subscribe({
      next: (response) => {
        console.log('Available roles from backend:', response.roles); // Add this
        if (response.success) {
          this.availableRoles = response.roles;
        }
      },
      error: (error) => {
        console.error('Failed to load roles:', error);
      }
    });
}



// Add this property to your component
private roleMapping: {[key: string]: string} = {
  // Backend value -> Display value
  'ROLE_COMMERCIAL_METIER': 'Commercial Métier',
  'commercial_metier': 'Commercial Métier',
  'COMMERCIAL_METIER': 'Commercial Métier',
  'ROLE_DECIDEUR': 'Décideur',
  'decideur': 'Décideur',
  'DECIDEUR': 'Décideur',
  'ROLE_CHEF_PROJET': 'Chef de Projet',
  'chef_projet': 'Chef de Projet',
  'CHEF_PROJET': 'Chef de Projet',
  'ROLE_ADMIN': 'Admin',
  'admin': 'Admin',
  'ADMIN': 'Admin'
};

// Add this helper method
private mapRoleToDisplay(roleValue: string): string {
  return this.roleMapping[roleValue] || roleValue;
}

private mapDisplayToBackend(displayRole: string): string {
  // Reverse lookup
  const entry = Object.entries(this.roleMapping).find(([key, value]) => value === displayRole);
  return entry ? entry[0] : displayRole;
}



  // Checkbox change handlers
  onRoleCheckboxChange(event: any, roleValue: string): void {
    const roles = this.addUserForm.get('roles')?.value || [];
    if (event.target.checked) {
      // Add role
      this.addUserForm.patchValue({
        roles: [...roles, roleValue]
      });
    } else {
      // Remove role
      this.addUserForm.patchValue({
        roles: roles.filter((r: string) => r !== roleValue)
      });
    }
    this.addUserForm.get('roles')?.updateValueAndValidity();
  }



loadUsers(): void {
  this.loading = true;
  this.error = '';
  const token = this.authService.token;
  const headers = new HttpHeaders({
    'Authorization': `Bearer ${token}`
  });
  
  this.http.get<AdminUser[]>(`${this.apiUrl}/admin/users`, { headers })
    .subscribe({
      next: (users) => {
        this.allUsers = users.map(user => ({
          ...user,
          roles: this.extractRoleNames(user)
        }));
        
        // Reset to first page when loading new users
        this.currentPage = 1;
        this.applyFilters();
        this.loading = false;
      },
      error: (error) => {
        console.error('Load users error:', error);
        this.error = error.error?.message || 'Failed to load users';
        this.loading = false;
      }
    });
}

// Add filter methods:
toggleFilterDropdown(): void {
  this.showFilterDropdown = !this.showFilterDropdown;
}






getPageNumbers(): number[] {
  const pages: number[] = [];
  const maxVisiblePages = 5;
  
  if (this.totalPages <= maxVisiblePages) {
    // Show all pages if total pages are less than or equal to maxVisiblePages
    for (let i = 1; i <= this.totalPages; i++) {
      pages.push(i);
    }
  } else {
    // Always show first page
    pages.push(1);
    
    // Calculate start and end of visible pages
    let start = Math.max(2, this.currentPage - 1);
    let end = Math.min(this.totalPages - 1, this.currentPage + 1);
    
    // Adjust if we're near the beginning
    if (this.currentPage <= 2) {
      end = 4;
    }
    
    // Adjust if we're near the end
    if (this.currentPage >= this.totalPages - 1) {
      start = this.totalPages - 3;
    }
    
    // Add visible pages
    for (let i = start; i <= end; i++) {
      if (!pages.includes(i)) {
        pages.push(i);
      }
    }
    
    // Add last page if not already included
    if (!pages.includes(this.totalPages)) {
      pages.push(this.totalPages);
    }
  }
  
  return pages;
}


applyFilters(): void {
  let filteredUsers = [...this.allUsers];
  
  // Apply role filter (direct display name comparison)
  if (this.roleFilter) {
    filteredUsers = filteredUsers.filter(user => 
      user.roles.some(role => 
        role.toLowerCase() === this.roleFilter.toLowerCase()
      )
    );
  }
  
  // Apply status filter
  if (this.statusFilter) {
    switch(this.statusFilter) {
      case 'active':
        filteredUsers = filteredUsers.filter(user => 
          !user.lockedByAdmin && 
          (!user.accountLockedUntil || new Date(user.accountLockedUntil) <= new Date())
        );
        break;
      case 'temporarily_locked':
        filteredUsers = filteredUsers.filter(user => 
          !user.lockedByAdmin && 
          user.accountLockedUntil && 
          new Date(user.accountLockedUntil) > new Date()
        );
        break;
      case 'admin_locked':
        filteredUsers = filteredUsers.filter(user => user.lockedByAdmin);
        break;
    }
  }
  
  this.users = filteredUsers;
  this.totalPages = Math.ceil(this.users.length / this.pageSize);
  this.updatePaginatedUsers();
}

// Add this new method:
updatePaginatedUsers(): void {
  const startIndex = (this.currentPage - 1) * this.pageSize;
  const endIndex = startIndex + this.pageSize;
  this.paginatedUsers = this.users.slice(startIndex, endIndex);
}


goToPage(page: number): void {
  if (page >= 1 && page <= this.totalPages) {
    this.currentPage = page;
    this.updatePaginatedUsers();
  }
}

previousPage(): void {
  if (this.currentPage > 1) {
    this.currentPage--;
    this.updatePaginatedUsers();
  }
}

nextPage(): void {
  if (this.currentPage < this.totalPages) {
    this.currentPage++;
    this.updatePaginatedUsers();
  }
}

clearFilters(): void {
  this.roleFilter = '';
  this.statusFilter = '';
  this.showFilterDropdown = false;
  this.users = [...this.allUsers]; // Reset to all users
}

// Update lock/unlock methods to refresh data without full reload:
lockUser(userId: number): void {
  this.error = '';
  this.successMessage = '';
  const token = this.authService.token;
  const headers = new HttpHeaders({
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  });
  
  this.http.post(`${this.apiUrl}/admin/users/${userId}/lock`, {}, { 
    headers
  })
    .subscribe({
      next: (response: any) => {
        console.log('Lock response:', response);
        if (response.success) {
this.showSuccess(response.message);
          // Update the specific user in allUsers array
          const userIndex = this.allUsers.findIndex(u => u.id === userId);
          if (userIndex !== -1) {
            this.allUsers[userIndex].lockedByAdmin = true;
          }
          // Reapply filters
          this.applyFilters();
          
          setTimeout(() => {
            this.successMessage = '';
          }, 5000);
        } else {
        }
      },
      error: (error) => {
        console.error('Lock user error:', error);
this.showError(error.error?.message || 'Failed to lock user');
      }
    });
}

unlockUser(userId: number): void {
  this.error = '';
  this.successMessage = '';
  const token = this.authService.token;
  const headers = new HttpHeaders({
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  });
  
  this.http.post(`${this.apiUrl}/admin/users/${userId}/unlock`, {}, { 
    headers
  })
    .subscribe({
      next: (response: any) => {
        console.log('Unlock response:', response);
        if (response.success) {
this.showSuccess(response.message);
          // Update the specific user in allUsers array
          const userIndex = this.allUsers.findIndex(u => u.id === userId);
          if (userIndex !== -1) {
            this.allUsers[userIndex].lockedByAdmin = false;
          }
          // Reapply filters
          this.applyFilters();
          
          setTimeout(() => {
            this.successMessage = '';
          }, 5000);
        } else {
          this.error = response.message || 'Failed to unlock user';
        }
      },
      error: (error) => {
        console.error('Unlock user error:', error);
this.showError(error.error?.message || 'Failed to unlock user');
      }
    });
}


// In AdminUsersComponent, add this method:
@HostListener('document:click', ['$event'])
onDocumentClick(event: MouseEvent): void {
  const target = event.target as HTMLElement;
  if (!target.closest('.relative') && this.showFilterDropdown) {
    this.showFilterDropdown = false;
  }
}


private extractRoleNames(user: any): string[] {
  if (!user.roles) return [];
  
  if (Array.isArray(user.roles)) {
    return user.roles.map((role: any) => {
      let roleName = '';
      
      if (typeof role === 'object' && role.name) {
        roleName = role.name;
      } else if (typeof role === 'string') {
        roleName = role;
      }
      
      if (roleName) {
        // Direct mapping based on the role name
        if (roleName === 'ROLE_COMMERCIAL_METIER') {
          return 'Commercial Métier';
        } else if (roleName === 'ROLE_DECIDEUR') {
          return 'Décideur';
        } else if (roleName === 'ROLE_CHEF_PROJET') {
          return 'Chef de Projet';
        } else if (roleName === 'ROLE_ADMIN') {
          return 'Admin';
        } else {
          // Fallback for any other roles
          return roleName.replace('ROLE_', '')
            .toLowerCase()
            .split('_')
            .map(word => word.charAt(0).toUpperCase() + word.slice(1))
            .join(' ');
        }
      }
      
      return '';
    }).filter((role: string) => role !== '');
  }
  
  return [];
}


// Helper method for title case (preserves accents)
private toTitleCase(str: string): string {
  return str
    .toLowerCase()
    .split('_')
    .map(word => {
      // Preserve the first character as-is (to keep accents)
      return word.charAt(0).toUpperCase() + word.slice(1);
    })
    .join(' ');
}

  // Modal management methods
  openAddUserModal(): void {
    this.addUserForm.reset();
    this.showAddUserModal = true;
  }



  openEditDepartmentModal(user: AdminUser): void {
    this.selectedUser = user;
    this.editDepartmentForm.patchValue({
      department: user.department || ''
    });
    this.showEditDepartmentModal = true;
  }



// openEditRoleModal(user: AdminUser): void {
//   this.selectedUser = user;
  
//   // Debug: See what we're working with
//   console.log('User display roles:', user.roles);
//   console.log('Available roles:', this.availableRoles);
  
//   // Map user's display roles to available role values
//   const formRoleValues = user.roles.map(displayRole => {
//     // Find which available role has this display name
//     const availableRole = this.availableRoles.find(ar => ar.label === displayRole);
//     return availableRole ? availableRole.value : displayRole;
//   });
  
//   console.log('Form role values to set:', formRoleValues);
  
//   this.editRoleForm.patchValue({
//     roles: formRoleValues
//   });
//   this.showEditRoleModal = true;
// }

openEditRoleModal(user: AdminUser): void {
  this.selectedUser = user;
  
  console.log('User display roles:', user.roles);
  console.log('Available roles:', this.availableRoles);
  
  // Map user's display roles to available role values
  const formRoleValues = user.roles.map(displayRole => {
    const availableRole = this.availableRoles.find(ar => ar.label === displayRole);
    return availableRole ? availableRole.value : displayRole;
  });
  
  // For single selection, take only the first role if multiple exist
  const singleRoleValue = formRoleValues.length > 0 ? [formRoleValues[0]] : [];
  
  console.log('Form role value to set (single):', singleRoleValue);
  
  this.editRoleForm.patchValue({
    roles: singleRoleValue
  });
  this.showEditRoleModal = true;
}


isRoleChecked(roleValue: string): boolean {
  if (!this.selectedUser || !this.editRoleForm.get('roles')?.value) {
    return false;
  }
  
  const currentRoles = this.editRoleForm.get('roles')?.value || [];
  return currentRoles.includes(roleValue);
}
 


  onSubmitAddUser(): void {
  if (this.addUserForm.invalid) {
    this.markFormGroupTouched(this.addUserForm);
    return;
  }

  const token = this.authService.token;
  const headers = new HttpHeaders({
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  });

  const formData = this.addUserForm.value;
  
  this.http.post(`${this.apiUrl}/admin/users/add`, formData, { headers })
    .subscribe({
      next: (response: any) => {
        if (response.success) {
this.showSuccess(response.message);
          this.loadUsers();
          this.closeAddUserModal();
          
          setTimeout(() => {
            this.successMessage = '';
          }, 5000);
        } else {
          this.addUserError = response.message; // Change to modal-specific error
        }
      },
      error: (error) => {
        console.error('Add user error:', error);
        this.addUserError = error.error?.message || 'Failed to add user'; // Change to modal-specific error
      }
    });
}

  onSubmitEditDepartment(): void {
    if (this.editDepartmentForm.invalid || !this.selectedUser) {
      this.markFormGroupTouched(this.editDepartmentForm);
      return;
    }

    const token = this.authService.token;
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });

    const department = this.editDepartmentForm.value.department;
    
    this.http.put(`${this.apiUrl}/admin/users/${this.selectedUser.id}/department`, 
      { department }, { headers })
        .subscribe({
    next: (response: any) => {
      if (response.success) {
this.showSuccess(response.message);
        this.loadUsers();
        this.closeEditDepartmentModal();
        
        setTimeout(() => {
          this.successMessage = '';
        }, 5000);
      } else {
        this.editDepartmentError = response.message; // Change to modal-specific error
      }
    },
    error: (error) => {
      console.error('Update department error:', error);
      this.editDepartmentError = error.error?.message || 'Failed to update department'; // Change to modal-specific error
    }
  });
}

  onSubmitEditRole(): void {
    if (this.editRoleForm.invalid || !this.selectedUser) {
      this.markFormGroupTouched(this.editRoleForm);
      return;
    }

    const token = this.authService.token;
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });

    const roles = this.editRoleForm.value.roles;
    
    this.http.put(`${this.apiUrl}/admin/users/${this.selectedUser.id}/roles`, 
      { roles }, { headers })
      .subscribe({
    next: (response: any) => {
      if (response.success) {
this.showSuccess(response.message);
        this.loadUsers();
        this.closeEditRoleModal();
        
        setTimeout(() => {
          this.successMessage = '';
        }, 5000);
      } else {
        this.editRoleError = response.message; // Change to modal-specific error
      }
    },
    error: (error) => {
      console.error('Update role error:', error);
      this.editRoleError = error.error?.message || 'Failed to update roles'; // Change to modal-specific error
    }
  });
}

// In modal close methods, reset the specific errors:
closeAddUserModal(): void {
  this.showAddUserModal = false;
  this.addUserForm.reset();
  this.addUserError = ''; // Reset modal-specific error
}

closeEditDepartmentModal(): void {
  this.showEditDepartmentModal = false;
  this.selectedUser = null;
  this.editDepartmentForm.reset();
  this.editDepartmentError = ''; // Reset modal-specific error
}

closeEditRoleModal(): void {
  this.showEditRoleModal = false;
  this.selectedUser = null;
  this.editRoleForm.reset();
  this.editRoleError = ''; // Reset modal-specific error
}

private markFormGroupTouched(formGroup: FormGroup): void {
    Object.values(formGroup.controls).forEach(control => {
      control.markAsTouched();
      if (control instanceof FormGroup) {
        this.markFormGroupTouched(control);
      }
    });
  }

getUserRolesDisplay(user: AdminUser): string {
  if (!user.roles || user.roles.length === 0) return 'No role';
  
  // Return the role(s) - they will be translated in the template
  return user.roles.join(', ');
}




  // Password visibility toggle
  togglePasswordVisibility(): void {
    this.passwordVisible = !this.passwordVisible;
  }



  getLockedUsers(): AdminUser[] {
    return this.users.filter(user => 
      user.lockedByAdmin || 
      (user.accountLockedUntil && new Date(user.accountLockedUntil) > new Date())
    );
  }

  getLockStatus(user: AdminUser): string {
  if (user.lockedByAdmin) {
    return 'Locked by Admin';
  } else if (user.accountLockedUntil && new Date(user.accountLockedUntil) > new Date()) {
    return 'Temporarily Locked';
  } else if (user.failedLoginAttempts > 0) {
    return `${user.failedLoginAttempts} failed attempts`;
  }
  return 'Active';
}

getTranslatedLockStatus(user: AdminUser): string {
  const status = this.getLockStatus(user);
  
  // If it's a failed attempts status, we need special handling
  if (status.includes('failed attempts')) {
    const attempts = user.failedLoginAttempts || 0;
    // Use the translation service directly
    let translatedText = 'failed attempts';
    
    // Check if we're in Arabic and use manual translation
    if (this.trasnlationService.getCurrentLanguage() === 'ar') {
      translatedText = 'محاولات فاشلة';
    }
    
    return `${attempts} ${translatedText}`;
  }
  
  // For other statuses, return as is - they will be translated in the template
  return status;
}

onRoleRadioChange(event: any, roleValue: string): void {
  // For radio buttons, set the form value to an array with just this role
  this.addUserForm.patchValue({
    roles: [roleValue] // Single role in array
  });
  this.addUserForm.get('roles')?.updateValueAndValidity();
}


// onRoleRadio(roleValue: string): void {
//   // For radio buttons, set the form value to an array with just this role
//   this.editRoleForm.patchValue({
//     roles: [roleValue] // Single role in array
//   });
//   this.editRoleForm.get('roles')?.updateValueAndValidity();
// }


// Add this method to your AdminUsersComponent class
getTotalSelectedBudget(): number {
  return this.projectsToAssign.reduce((total, project) => {
    return total + (project.budget || 0);
  }, 0);
}
  isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  // Dashboard methods
  initCharts() {
    this.chart01 = this.chartService.initChart01();
    if (this.chart01) this.chart01.render();

    this.chart02 = this.chartService.initChart02();
    if (this.chart02) this.chart02.render();

    this.chart03 = this.chartService.initChart03();
    if (this.chart03) this.chart03.render();
  }

  initMap() {
    this.map01 = this.mapService.initMap01();
  }

  getTotalFailedAttempts(): number {
    return this.users.reduce((total, user) => total + (user.failedLoginAttempts || 0), 0);
  }

  getActiveUsersCount(): number {
    return this.users.filter(user => 
      !user.lockedByAdmin && 
      (!user.accountLockedUntil || new Date(user.accountLockedUntil) <= new Date())
    ).length;
  }

  getLockStatusClass(status: string): string {
    switch(status) {
      case 'Active':
        return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300';
      case 'Locked by Admin':
        return 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-300';
      case 'Temporarily Locked':
        return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-300';
      default:
        return 'bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-300';
    }
  }

  ngOnDestroy(): void {
    if (this.chart01) this.chart01.destroy();
    if (this.chart02) this.chart02.destroy();
    if (this.chart03) this.chart03.destroy();
  }

  getAvatarUrl(user: AdminUser): string {
  if (!user?.profileImage) {
    return this.generateAvatarUrl(user);
  }
  
  const profileImage = user.profileImage;
  
  // If it's already a full URL, use it
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
  
  // Default fallback
  return this.generateAvatarUrl(user);
}

generateAvatarUrl(user: AdminUser): string {
  if (!user) {
    return 'assets/images/user/owner.jpg'; // Default image
  }
  
  // Generate initials for fallback avatar
  let initials = 'U';
  if (user?.firstName && user?.lastName) {
    initials = user.firstName.charAt(0).toUpperCase() + user.lastName.charAt(0).toUpperCase();
  } else if (user?.firstName) {
    initials = user.firstName.charAt(0).toUpperCase();
  } else if (user?.lastName) {
    initials = user.lastName.charAt(0).toUpperCase();
  } else if (user?.username) {
    initials = user.username.charAt(0).toUpperCase();
  }
  
  // Simple SVG with initials as fallback
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
    <circle cx="50" cy="50" r="48" fill="#4F46E5"/>
    <text x="50" y="58" text-anchor="middle" font-family="Arial" font-size="38" fill="white">${initials}</text>
  </svg>`;
  
  return 'data:image/svg+xml;base64,' + btoa(svg);
}

handleImageError(event: any, user: AdminUser) {
  console.error('Image failed to load for user:', user.username, user.profileImage);
  // Set to default avatar
  event.target.src = this.generateAvatarUrl(user);
  event.target.onerror = null; // Prevent infinite loop
}


// In AdminUsersComponent class, add these methods:

// Open confirmation modal
openConfirmModal(user: AdminUser, action: 'lock' | 'unlock'): void {
  this.userToConfirm = user;
  this.confirmAction = action;
  
  if (action === 'lock') {
    this.confirmTitle = 'Confirm Lock User';
    this.confirmMessage = `Are you sure you want to lock ${user.firstName} ${user.lastName}? They will not be able to access their account until unlocked.`;
  } else {
    this.confirmTitle = 'Confirm Unlock User';
    this.confirmMessage = `Are you sure you want to unlock ${user.firstName} ${user.lastName}? They will regain access to their account.`;
  }
  
  this.showConfirmModal = true;
}

// Close confirmation modal
closeConfirmModal(): void {
  this.showConfirmModal = false;
  this.userToConfirm = null;
  this.confirmAction = null;
  this.confirmTitle = '';
  this.confirmMessage = '';
}

// Execute the confirmed action
executeConfirmedAction(): void {
  if (!this.userToConfirm || !this.confirmAction) return;
  
  if (this.confirmAction === 'lock') {
    this.lockUser(this.userToConfirm.id);
  } else {
    this.unlockUser(this.userToConfirm.id);
  }
  
  this.closeConfirmModal();
}

// Replace your existing success/error message setting with these methods

showSuccess(msg: string, type: 'success' | 'info' | 'warning' = 'success'): void {
  this.successMessage = msg;
  this.successMessageType = type;
  this.showSuccessMessage = true;
  this.showErrorMessage = false;
  
  // Auto-hide after 5 seconds
  setTimeout(() => {
    this.hideSuccessMessage();
  }, 5000);
}

showError(msg: string): void {
  this.error = msg;
  this.showErrorMessage = true;
  this.showSuccessMessage = false;
  
  // Auto-hide after 5 seconds
  setTimeout(() => {
    this.hideErrorMessage();
  }, 5000);
}

hideSuccessMessage(): void {
  this.showSuccessMessage = false;
  setTimeout(() => {
    this.successMessage = '';
    this.successMessageType = null;
  }, 300);
}

hideErrorMessage(): void {
  this.showErrorMessage = false;
  setTimeout(() => {
    this.error = '';
  }, 300);
}


showAssignProjectsModal(user: AdminUser): void {
    this.selectedUserForAssignment = user;
    
    // Fetch unassigned projects
    this.projectService.getUnassignedProjects().subscribe({
      next: (response: any) => {
        if (response.success) {
          this.unassignedProjects = response.data;
          this.projectsToAssign = []; // Reset selections
          this.showAssignmentModal = true;
        }
      },
      error: (error) => {
        console.error('Error fetching unassigned projects:', error);
        this.showError('Failed to load unassigned projects');
      }
    });
  }

  // Handle project selection in modal
  toggleProjectSelection(project: any): void {
    const index = this.projectsToAssign.findIndex(p => p.id === project.id);
    if (index === -1) {
      this.projectsToAssign.push(project);
    } else {
      this.projectsToAssign.splice(index, 1);
    }
  }

  // Check if project is selected
  isProjectSelected(project: any): boolean {
    return this.projectsToAssign.some(p => p.id === project.id);
  }

  // Assign selected projects to chef de projet
  assignSelectedProjects(): void {
    if (!this.selectedUserForAssignment || this.projectsToAssign.length === 0) {
      return;
    }

    const chefId = this.selectedUserForAssignment.id;
    let completedAssignments = 0;
    let failedAssignments = 0;

    // Show loading
    this.loading = true;

    // Assign each selected project
    this.projectsToAssign.forEach((project, index) => {
      this.projectService.assignChefDeProjet(project.id, chefId).subscribe({
        next: () => {
          completedAssignments++;
          console.log(`Project ${project.code} assigned successfully`);
          
          // Remove from unassigned projects list
          this.unassignedProjects = this.unassignedProjects.filter(p => p.id !== project.id);
          
          if (completedAssignments + failedAssignments === this.projectsToAssign.length) {
            this.loading = false;
            this.showAssignmentModal = false;
            this.projectsToAssign = [];
            
            if (failedAssignments === 0) {
              this.showSuccess(`Successfully assigned ${completedAssignments} project(s) to ${this.selectedUserForAssignment?.firstName} ${this.selectedUserForAssignment?.lastName}`);
            } else {
              this.showError(`Assigned ${completedAssignments} project(s), failed to assign ${failedAssignments} project(s)`);
            }
          }
        },
        error: (error) => {
          console.error('Error assigning project:', error);
          failedAssignments++;
          
          if (completedAssignments + failedAssignments === this.projectsToAssign.length) {
            this.loading = false;
            if (completedAssignments > 0) {
              this.showSuccess(`Partially assigned ${completedAssignments} project(s), ${failedAssignments} failed`);
            } else {
              this.showError('Failed to assign projects');
            }
          }
        }
      });
    });
  }

  // Close assignment modal
  closeAssignmentModal(): void {
    this.showAssignmentModal = false;
    this.selectedUserForAssignment = null;
    this.unassignedProjects = [];
    this.projectsToAssign = [];
  }

  // Update the onRoleRadio method to trigger project assignment when role changes
  onRoleRadio(roleValue: string): void {
    // For radio buttons, set the form value to an array with just this role
    this.editRoleForm.patchValue({
      roles: [roleValue]
    });
    this.editRoleForm.get('roles')?.updateValueAndValidity();
    
    // If changing to Chef de Projet, offer to assign projects
    if (roleValue === 'chef_projet' && this.selectedUser) {
      // You could optionally call showAssignProjectsModal here
      // or add a button in the UI to assign projects
    }
  }
}