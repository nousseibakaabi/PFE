import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { ApiService } from '../../services/api.service';
import { environment } from '../../../environments/environment';
import { TranslationService } from '../partials/traduction/translation.service';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ProfileComponent implements OnInit {
  profileForm!: FormGroup;
  personalInfoForm!: FormGroup;
  passwordForm!: FormGroup;
  
  loading = false;
  profileLoading = false;
  passwordLoading = false;
  personalInfoLoading = false;
  
  profileSuccess = false;
  passwordSuccess = false;
  personalInfoSuccess = false;
  
  error = '';
  user: any = null;
  
  isProfileInfoModal = false;
  isPasswordModal = false;

  avatarPreview: string | null = null;


  selectedFile: File | null = null;
  baseUrl = environment.baseUrl || 'http://localhost:8081';

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private apiService: ApiService,
    private translationService: TranslationService
  ) {}

  ngOnInit(): void {
    this.loadUserProfile();
    
    this.personalInfoForm = this.formBuilder.group({
     firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: [{value: '', disabled: true}], // ADD THIS LINE
    phone: [''],
    department: [''],
    avatar: [null]
    });

    this.passwordForm = this.formBuilder.group({
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    });
  }

loadUserProfile(): void {
  this.profileLoading = true;
  this.authService.getProfile().subscribe({
    next: (user) => {
      this.user = user;
      this.personalInfoForm.patchValue({
        firstName: user.firstName,
        lastName: user.lastName,
        email: user.email, // ADD THIS LINE
        phone: user.phone,
        department: user.department
      });
      this.profileLoading = false;
    },
    error: (error) => {
      this.error = 'Failed to load profile';
      this.profileLoading = false;
    }
  });
}
  // ADD THIS METHOD - Gets the correct avatar URL
  getAvatarUrl(): string {
    if (!this.user?.profileImage) {
      return this.generateAvatarUrl();
    }
    
    const profileImage = this.user.profileImage;
    
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
    return this.generateAvatarUrl();
  }

  // ADD THIS METHOD - Generates avatar from initials (fallback)
  generateAvatarUrl(): string {
    if (!this.user) {
      return 'assets/images/user/owner.jpg'; // Default image
    }
    
    // Generate initials for fallback avatar
    let initials = 'U';
    if (this.user?.firstName && this.user?.lastName) {
      initials = this.user.firstName.charAt(0).toUpperCase() + this.user.lastName.charAt(0).toUpperCase();
    } else if (this.user?.firstName) {
      initials = this.user.firstName.charAt(0).toUpperCase();
    } else if (this.user?.lastName) {
      initials = this.user.lastName.charAt(0).toUpperCase();
    } else if (this.user?.username) {
      initials = this.user.username.charAt(0).toUpperCase();
    }
    
    // Simple SVG with initials as fallback
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
      <circle cx="50" cy="50" r="48" fill="#4F46E5"/>
      <text x="50" y="58" text-anchor="middle" font-family="Arial" font-size="38" fill="white">${initials}</text>
    </svg>`;
    
    return 'data:image/svg+xml;base64,' + btoa(svg);
  }

handleImageError(event: any) {
  console.error('Image failed to load:', this.user?.profileImage);
  // Set to default avatar
  event.target.src = this.generateAvatarUrl();
  event.target.onerror = null; // Prevent infinite loop
}




updatePersonalInfo(): void {
  if (this.personalInfoForm.invalid) {
    this.error = 'Please fill in all required fields';
    return;
  }

  this.personalInfoLoading = true;
  this.error = '';

  const formData = new FormData();
  formData.append('firstName', this.personalInfoForm.value.firstName);
  formData.append('lastName', this.personalInfoForm.value.lastName);
  formData.append('email', this.personalInfoForm.value.email); // ADD THIS LINE
  formData.append('phone', this.personalInfoForm.value.phone || '');
  formData.append('department', this.personalInfoForm.value.department || '');
  
  // Add avatar file if selected
  if (this.selectedFile) {
    formData.append('avatar', this.selectedFile);
  }

  this.apiService.updateProfileWithAvatar(formData).subscribe({
    next: () => {
      this.personalInfoSuccess = true;
      this.personalInfoLoading = false;
      this.selectedFile = null;
      this.avatarPreview = null; // Clear preview
      
      // Refresh user data immediately
      this.authService.refreshUser().subscribe({
        next: (updatedUser) => {
          // Update local user object
          this.user = updatedUser;
          console.log('User updated in modal:', this.user);
        },
        error: (err) => {
          console.error('Failed to refresh user:', err);
        }
      });
      
      setTimeout(() => {
        this.personalInfoSuccess = false;
        this.isProfileInfoModal = false;
        // Reload full profile data
        this.loadUserProfile();
      }, 3000);
    },
    error: (error) => {
      this.error = error.error?.message || 'Failed to update profile';
      this.personalInfoLoading = false;
    }
  });
}

  changePassword(): void {
    if (this.passwordForm.value.newPassword !== this.passwordForm.value.confirmPassword) {
      this.error = 'Passwords do not match';
      return;
    }

    this.passwordLoading = true;
    this.authService.changePassword(
      this.passwordForm.value.currentPassword,
      this.passwordForm.value.newPassword,
      this.passwordForm.value.confirmPassword
    ).subscribe({
      next: () => {
        this.passwordSuccess = true;
        this.passwordLoading = false;
        this.passwordForm.reset();
        setTimeout(() => {
          this.passwordSuccess = false;
          this.isPasswordModal = false;
        }, 3000);
      },
      error: (error) => {
        this.error = error.error?.message || 'Failed to change password';
        this.passwordLoading = false;
      }
    });
  }

onFileSelected(event: any): void {
  const file = event.target.files[0];
  if (file) {
    this.selectedFile = file;
    
    // Create preview
    const reader = new FileReader();
    reader.onload = (e: any) => {
      this.avatarPreview = e.target.result;
    };
    reader.readAsDataURL(file);
  }
}

  getUserFullName(): string {
    if (!this.user) return 'User';
    if (this.user.firstName && this.user.lastName) {
      return `${this.user.firstName} ${this.user.lastName}`;
    } else if (this.user.firstName) {
      return this.user.firstName;
    } else {
      return this.user.username;
    }
  }

  getUserRole(): string {
    if (!this.user || !this.user.roles || this.user.roles.length === 0) {
      return 'User';
    }
    
    const role = this.user.roles[0];
    return role
      .replace('ROLE_', '')
      .replace('_', ' ')
      .toLowerCase()
      .replace(/\b\w/g, (l: string) => l.toUpperCase());
  }

  get userRoles(): string {
    if (!this.user) return '';
    return this.user.roles.join(', ');
  }

closeProfileInfoModal(): void {
  this.isProfileInfoModal = false;
  this.error = '';
  this.selectedFile = null;
  this.avatarPreview = null; 
}


  closePasswordModal(): void {
    this.isPasswordModal = false;
    this.error = '';
    this.passwordForm.reset();
  }

  debugAvatar(): void {
  console.log('User object:', this.user);
  console.log('User profileImage:', this.user?.profileImage);
  console.log('getAvatarUrl():', this.getAvatarUrl());
  console.log('avatarPreview:', this.avatarPreview);
}

// In profile.component.ts, add these methods:
getAccountStatus(): string {
  if (!this.user) return 'Loading...';
  
  if (this.user.lockedByAdmin) {
    return 'Locked by Administrator';
  } else if (this.user.accountLockedUntil) {
    const lockDate = new Date(this.user.accountLockedUntil);
    if (lockDate > new Date()) {
      return `Temporarily Locked until ${lockDate.toLocaleString()}`;
    }
  } else if (this.user.failedLoginAttempts && this.user.failedLoginAttempts > 0) {
    return `${this.user.failedLoginAttempts} failed login attempt(s)`;
  }
  
  return this.user.enabled ? 'Active' : 'Disabled';
}

getStatusClass(): string {
  if (!this.user) return 'bg-gray-100 text-gray-700';
  
  if (this.user.lockedByAdmin) {
    return 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400';
  } else if (this.user.accountLockedUntil && new Date(this.user.accountLockedUntil) > new Date()) {
    return 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400';
  } else if (this.user.failedLoginAttempts && this.user.failedLoginAttempts > 0) {
    return 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400';
  }
  
  return this.user.enabled 
    ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400' 
    : 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400';
}

}