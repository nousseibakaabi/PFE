import { Component, OnInit, AfterViewInit ,HostListener } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { ApiService } from '../../services/api.service';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from 'src/environments/environment';
import { ChartService } from '../partials/services/chart.service';
import { MapService } from '../partials/services/map.service';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

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

  showFilterDropdown = false;
roleFilter: string = '';
statusFilter: string = '';
allUsers: AdminUser[] = []; // Store all users for filtering

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
    private fb: FormBuilder
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

  onRoleEditCheckboxChange(event: any, roleValue: string): void {
    const roles = this.editRoleForm.get('roles')?.value || [];
    if (event.target.checked) {
      // Add role
      this.editRoleForm.patchValue({
        roles: [...roles, roleValue]
      });
    } else {
      // Remove role
      this.editRoleForm.patchValue({
        roles: roles.filter((r: string) => r !== roleValue)
      });
    }
    this.editRoleForm.get('roles')?.updateValueAndValidity();
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
        // Store all users
        this.allUsers = users.map(user => ({
          ...user,
          roles: this.extractRoleNames(user)
        }));
        
        // Apply filters if any
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


// Replace onRoleEditCheckboxChange with this:
onRoleRadioChange(roleValue: string): void {
  // For radio buttons, set the form value to an array with just this role
  this.editRoleForm.patchValue({
    roles: [roleValue] // Single role in array
  });
  this.editRoleForm.get('roles')?.updateValueAndValidity();
}

applyFilters(): void {
  let filteredUsers = [...this.allUsers];
  
  // Apply role filter (direct display name comparison)
  if (this.roleFilter) {
    filteredUsers = filteredUsers.filter(user => 
      user.roles.some(role => {
        // Case-insensitive comparison
        return role.toLowerCase() === this.roleFilter.toLowerCase();
      })
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
          this.successMessage = response.message;
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
          this.error = response.message || 'Failed to lock user';
        }
      },
      error: (error) => {
        console.error('Lock user error:', error);
        this.error = error.error?.message || 'Failed to lock user';
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
          this.successMessage = response.message;
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
        this.error = error.error?.message || 'Failed to unlock user';
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
          this.successMessage = response.message;
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
        this.successMessage = response.message;
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
        this.successMessage = response.message;
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
  
  // Since extractRoleNames already formats them nicely, just join them
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
}