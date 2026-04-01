import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { ApiService } from '../../services/api.service';
import { environment } from '../../../environments/environment';
import { TranslationService } from '../partials/traduction/translation.service';
import { HttpClient, HttpHeaders } from '@angular/common/http';

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
  isTwoFactorEnabled = false;

  notificationForm!: FormGroup;
  notificationLoading = false;
  notificationSuccess = false;
  notificationError = '';

  selectedFile: File | null = null;
  baseUrl = environment.baseUrl || 'http://localhost:8084';

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private apiService: ApiService,
    private translationService: TranslationService,
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadUserProfile();
    
    this.personalInfoForm = this.formBuilder.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: [{value: '', disabled: true}],
      phone: [''],
      department: ['']
    });

    this.passwordForm = this.formBuilder.group({
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    });

    this.notificationForm = this.formBuilder.group({
      notifMode: ['email', Validators.required]
    });
  }

  loadUserProfile(): void {
    this.profileLoading = true;
    this.authService.getProfile().subscribe({
      next: (user) => {
        this.user = user;
        this.isTwoFactorEnabled = user.twoFactorEnabled || false;
        
        this.personalInfoForm.patchValue({
          firstName: user.firstName,
          lastName: user.lastName,
          email: user.email,
          phone: user.phone,
          department: user.department
        });

        this.loadNotificationMode();
        this.profileLoading = false;
      },
      error: (error) => {
        this.error = 'Failed to load profile';
        this.profileLoading = false;
      }
    });
  }

  // IMPROVED AVATAR METHODS (from application-form)
  getAvatarUrl(): string {
    if (!this.user) {
      return this.generateDefaultAvatar();
    }
    
    const profileImage = this.user.profileImage;
    
    if (!profileImage) {
      return this.generateDefaultAvatar();
    }
    
    if (profileImage.startsWith('http')) {
      return profileImage;
    }
    
    if (profileImage.startsWith('/uploads/')) {
      return this.baseUrl + profileImage;
    }
    
    if (profileImage.startsWith('data:image')) {
      return profileImage;
    }
    
    return this.generateDefaultAvatar();
  }

generateDefaultAvatar(): string {
  if (!this.user) {
    return 'assets/images/user/owner.jpg';
  }
  
  let initials = 'U';
  if (this.user?.firstName && this.user?.lastName) {
    initials = (this.user.firstName.charAt(0) + this.user.lastName.charAt(0)).toUpperCase();
  } else if (this.user?.firstName) {
    initials = this.user.firstName.charAt(0).toUpperCase();
  } else if (this.user?.lastName) {
    initials = this.user.lastName.charAt(0).toUpperCase();
  } else if (this.user?.username) {
    initials = this.user.username.charAt(0).toUpperCase();
  }
  
  const blueColor = '#2B72DF';
  
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="100" height="100">
    <circle cx="50" cy="50" r="48" fill="white" stroke="${blueColor}" stroke-width="2"/>
    <text x="50" y="58" text-anchor="middle" font-family="Arial, Helvetica, sans-serif" 
          font-size="38" font-weight="500" fill="${blueColor}" dominant-baseline="middle">
      ${initials}
    </text>
  </svg>`;
  
  return 'data:image/svg+xml;base64,' + btoa(svg);
}
  handleImageError(event: any): void {
    event.target.src = this.generateDefaultAvatar();
    event.target.onerror = null;
  }

  getUserInitials(): string {
    if (!this.user) return 'U';
    if (this.user.firstName && this.user.lastName) {
      return (this.user.firstName.charAt(0) + this.user.lastName.charAt(0)).toUpperCase();
    } else if (this.user.firstName) {
      return this.user.firstName.charAt(0).toUpperCase();
    } else if (this.user.lastName) {
      return this.user.lastName.charAt(0).toUpperCase();
    } else if (this.user.username) {
      return this.user.username.charAt(0).toUpperCase();
    }
    return 'U';
  }

  loadNotificationMode(): void {
    if (this.user?.notifMode) {
      this.notificationForm.patchValue({
        notifMode: this.user.notifMode
      });
    }
  }

  updateNotificationMode(): void {
    if (this.notificationForm.invalid) {
      return;
    }

    this.notificationLoading = true;
    this.notificationError = '';

    const notifData = {
      notifMode: this.notificationForm.value.notifMode
    };

    this.apiService.updateNotificationPreferences(notifData).subscribe({
      next: () => {
        this.notificationSuccess = true;
        this.notificationLoading = false;
        
        if (this.user) {
          this.user.notifMode = this.notificationForm.value.notifMode;
        }
        
        setTimeout(() => {
          this.notificationSuccess = false;
        }, 3000);
      },
      error: (error) => {
        this.notificationError = error.error?.message || 'Failed to update notification preferences';
        this.notificationLoading = false;
      }
    });
  }

  getNotificationModeTextKey(): string {
    const mode = this.user?.notifMode;
    switch(mode) {
      case 'email': return 'Email';
      case 'sms': return 'SMS';
      case 'both': return 'Les deux';
      default: return 'Email';
    }
  }

  getNotificationModeDescriptionKey(): string {
    const mode = this.user?.notifMode;
    switch(mode) {
      case 'email': return 'Recevoir les notifications uniquement par email';
      case 'sms': return 'Recevoir les notifications uniquement par SMS';
      case 'both': return 'Recevoir les notifications par email et SMS';
      default: return 'Notifications par email uniquement';
    }
  }

  refreshUserData(): void {
    this.authService.refreshUser().subscribe({
      next: (user) => {
        this.user = user;
        this.isTwoFactorEnabled = user.twoFactorEnabled || false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Failed to refresh user data:', err);
      }
    });
  }

  getAccountStatusKey(): string {
    if (!this.user) return 'Loading...';
    
    if (this.user.lockedByAdmin) {
      return 'Locked by Administrator';
    } else if (this.user.accountLockedUntil) {
      const lockDate = new Date(this.user.accountLockedUntil);
      if (lockDate > new Date()) {
        return 'Temporarily Locked';
      }
    } else if (this.user.failedLoginAttempts && this.user.failedLoginAttempts > 0) {
      return 'Failed attempts';
    }
    
    return this.user.enabled ? 'Active' : 'Disabled';
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
    formData.append('email', this.personalInfoForm.value.email);
    formData.append('phone', this.personalInfoForm.value.phone || '');
    formData.append('department', this.personalInfoForm.value.department || '');
    
    if (this.selectedFile) {
      formData.append('avatar', this.selectedFile);
    }

    this.apiService.updateProfileWithAvatar(formData).subscribe({
      next: () => {
        this.personalInfoSuccess = true;
        this.personalInfoLoading = false;
        this.selectedFile = null;
        this.avatarPreview = null;
        
        this.authService.refreshUser().subscribe({
          next: (updatedUser) => {
            this.user = updatedUser;
            this.isTwoFactorEnabled = updatedUser.twoFactorEnabled || false;
          },
          error: (err) => {
            console.error('Failed to refresh user:', err);
          }
        });
        
        setTimeout(() => {
          this.personalInfoSuccess = false;
          this.isProfileInfoModal = false;
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