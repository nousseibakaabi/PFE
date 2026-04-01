import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { Router } from '@angular/router';
import { LayoutService } from '../services/layout.service';
import { AuthService } from '../../../services/auth.service';
import { User } from '../../../models/user';
import { Subscription } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { TranslationService } from '../traduction/translation.service';
import { NotificationService, Notification } from '../../../services/notification.service';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css'],
})
export class HeaderComponent implements OnInit, OnDestroy {
  isMenuOpen: boolean = false;
  isNotificationsOpen: boolean = false;
  isUserDropdownOpen: boolean = false;
  isDarkMode: boolean = false;
  isNotifying: boolean = true;
  isMobile: boolean = false;
  searchQuery: string = '';
  currentUser: User | null = null;
  private userSubscription!: Subscription;

  isSidebarOpen: boolean = false;

  isLanguageDropdownOpen = false;
  currentLanguage = 'en';
  languages: any[] = [];
  translationActive = false;

  // Notifications
  notifications: Notification[] = [];
  unreadCount: number = 0;
  isLoadingNotifications: boolean = false;
  notificationPage: number = 0;
  hasMoreNotifications: boolean = true;
  
  private notificationSubscription!: Subscription;
  private unreadCountSubscription!: Subscription;

  constructor(
    private router: Router,
    private layoutService: LayoutService,
    private authService: AuthService,
    private translationService: TranslationService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.checkScreenSize();
    
    // Check for saved dark mode preference
    const savedDarkMode = localStorage.getItem('darkMode');
    if (savedDarkMode) {
      this.isDarkMode = savedDarkMode === 'true';
      this.applyDarkMode();
    } else {
      this.isDarkMode = window.matchMedia('(prefers-color-scheme: dark)').matches;
      this.applyDarkMode();
    }
    
    // Subscribe to user changes
    this.userSubscription = this.authService.currentUser.subscribe(user => {
      this.currentUser = user;
      
      // Load notifications when user is logged in
      if (user) {
        this.loadNotifications();
        this.loadUnreadCount();
        this.subscribeToNewNotifications();
      }
    });

    // Subscribe to sidebar state
    this.layoutService.sidebarOpen$.subscribe((isOpen) => {
      this.isSidebarOpen = isOpen;
    });
  }

  ngOnDestroy(): void {
    if (this.userSubscription) {
      this.userSubscription.unsubscribe();
    }
    if (this.notificationSubscription) {
      this.notificationSubscription.unsubscribe();
    }
    if (this.unreadCountSubscription) {
      this.unreadCountSubscription.unsubscribe();
    }
    this.notificationService.disconnect();
  }

  // ============= NOTIFICATION METHODS =============

  /**
   * Load notifications
   */
  loadNotifications(reset: boolean = false): void {
    if (reset) {
      this.notificationPage = 0;
      this.notifications = [];
      this.hasMoreNotifications = true;
    }

    if (!this.hasMoreNotifications || this.isLoadingNotifications) {
      return;
    }

    this.isLoadingNotifications = true;
    
    this.notificationService.getNotifications(this.notificationPage, 10).subscribe({
      next: (response) => {
        if (response.data && response.data.length > 0) {
          this.notifications = [...this.notifications, ...response.data];
          this.notificationPage++;
          this.hasMoreNotifications = this.notificationPage < response.totalPages;
        } else {
          this.hasMoreNotifications = false;
        }
        this.isLoadingNotifications = false;
      },
      error: (error) => {
        console.error('Failed to load notifications', error);
        this.isLoadingNotifications = false;
      }
    });
  }

  /**
   * Load unread count
   */
  loadUnreadCount(): void {
    if (this.unreadCountSubscription) {
      this.unreadCountSubscription.unsubscribe();
    }
    
    this.unreadCountSubscription = this.notificationService.onUnreadCountChange().subscribe(
      count => {
        this.unreadCount = count;
        this.isNotifying = count > 0;
      }
    );
  }

  /**
   * Subscribe to new notifications via WebSocket
   */
  subscribeToNewNotifications(): void {
    this.notificationSubscription = this.notificationService.onNewNotification().subscribe(
      notification => {
        // Add new notification to the top of the list
        this.notifications.unshift(notification);
        // Update unread count (will be handled by the service)
      }
    );
  }

  /**
   * Toggle notifications dropdown
   */
  toggleNotifications(): void {
    this.isNotificationsOpen = !this.isNotificationsOpen;
    
    // If opening dropdown, load fresh notifications
    if (this.isNotificationsOpen) {
      this.loadNotifications(true);
      
      // Mark as read when opened
      setTimeout(() => {
        this.markAllAsRead();
      }, 2000);
    }
  }

  /**
   * Mark notification as read
   */
  markAsRead(notification: Notification, event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    
    if (!notification.isRead) {
      this.notificationService.markAsRead(notification.id).subscribe({
        next: () => {
          notification.isRead = true;
          this.isNotifying = this.notifications.some(n => !n.isRead);
        },
        error: (error) => console.error('Failed to mark notification as read', error)
      });
    }
  }

  /**
   * Mark all notifications as read
   */
  markAllAsRead(): void {
    const unreadIds = this.notifications
      .filter(n => !n.isRead)
      .map(n => n.id);
    
    if (unreadIds.length === 0) return;

    this.notificationService.markAllAsRead().subscribe({
      next: () => {
        this.notifications.forEach(n => n.isRead = true);
        this.isNotifying = false;
      },
      error: (error) => console.error('Failed to mark all as read', error)
    });
  }

  /**
   * Delete notification
   */
  deleteNotification(notification: Notification, event: Event): void {
    event.stopPropagation();
    
    this.notificationService.deleteNotification(notification.id).subscribe({
      next: () => {
        const index = this.notifications.findIndex(n => n.id === notification.id);
        if (index !== -1) {
          this.notifications.splice(index, 1);
        }
        this.isNotifying = this.notifications.some(n => !n.isRead);
      },
      error: (error) => console.error('Failed to delete notification', error)
    });
  }

  /**
   * Navigate to related entity when notification is clicked
   */
  navigateToNotification(notification: Notification): void {
    // Mark as read first
    this.markAsRead(notification);
    
    // Navigate based on reference type
    switch (notification.referenceType) {
      case 'FACTURE':
        this.router.navigate(['/factures', notification.referenceId]);
        break;
      case 'CONVENTION':
        this.router.navigate(['/conventions', notification.referenceId]);
        break;
      case 'APPLICATION':
        this.router.navigate(['/applications', notification.referenceId]);
        break;
      default:
        // Do nothing or navigate to a general notifications page
        break;
    }
    
    // Close dropdown
    this.isNotificationsOpen = false;
  }

  /**
   * Load more notifications (infinite scroll)
   */
  onScrollNotifications(): void {
    if (this.hasMoreNotifications && !this.isLoadingNotifications) {
      this.loadNotifications();
    }
  }


// Add these methods to your HeaderComponent class

// Get days status text
getDaysStatusText(daysUntilDue?: number): string {
  if (daysUntilDue === undefined || daysUntilDue === null) return '';
  
  if (daysUntilDue > 0) {
    return daysUntilDue === 1 ? 'Tomorrow' : `In ${daysUntilDue} days`;
  } else if (daysUntilDue === 0) {
    return 'Today';
  } else {
    return `${Math.abs(daysUntilDue)} days overdue`;
  }
}

// Get notification icon based on type
getNotificationIcon(type: string): string {
  switch (type) {
    case 'INFO': return 'fa-info-circle';
    case 'WARNING': return 'fa-exclamation-triangle';
    case 'SUCCESS': return 'fa-check-circle';
    case 'DANGER': return 'fa-exclamation-circle';
    default: return 'fa-bell';
  }
}

// Get notification color class based on type
getNotificationColor(type: string): string {
  switch (type) {
    case 'INFO': return 'text-blue-500';
    case 'WARNING': return 'text-yellow-500';
    case 'SUCCESS': return 'text-green-500';
    case 'DANGER': return 'text-red-500';
    default: return 'text-gray-500';
  }
}

// Format notification time
formatTime(createdAt: string): string {
  const date = new Date(createdAt);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHours / 24);

  if (diffMins < 1) return 'Just now';
  if (diffMins < 60) return `${diffMins} min${diffMins === 1 ? '' : 's'} ago`;
  if (diffHours < 24) return `${diffHours} hour${diffHours === 1 ? '' : 's'} ago`;
  if (diffDays < 7) return `${diffDays} day${diffDays === 1 ? '' : 's'} ago`;
  
  return date.toLocaleDateString('en-US', { 
    month: 'short', 
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
}

  // ============= EXISTING METHODS =============

  loadCurrentUser(): void {
    this.currentUser = this.authService.currentUserValue;
  }

  getUserFullName(): string {
    if (!this.currentUser) return 'User';
    
    if (this.currentUser.firstName && this.currentUser.lastName) {
      return `${this.currentUser.firstName} ${this.currentUser.lastName}`;
    } else if (this.currentUser.firstName) {
      return this.currentUser.firstName;
    } else {
      return this.currentUser.username;
    }
  }

  getUserRole(): string {
    if (!this.currentUser || !this.currentUser.roles || this.currentUser.roles.length === 0) {
      return 'User';
    }
    
    const role = this.currentUser.roles[0];
    return role
      .replace('ROLE_', '')
      .replace('_', ' ')
      .toLowerCase()
      .replace(/\b\w/g, l => l.toUpperCase());
  }

  @HostListener('window:resize', ['$event'])
  onResize() {
    this.checkScreenSize();
  }

  checkScreenSize() {
    this.isMobile = window.innerWidth < 1024;
  }

  toggleSidebar() {
    this.layoutService.toggleSidebar();
  }

  toggleMenu() {
    this.isMenuOpen = !this.isMenuOpen;
  }

  toggleUserDropdown(event: Event) {
    event.preventDefault();
    this.isUserDropdownOpen = !this.isUserDropdownOpen;
  }

  toggleDarkMode() {
    this.isDarkMode = !this.isDarkMode;
    this.applyDarkMode();
    localStorage.setItem('darkMode', this.isDarkMode.toString());
  }

  applyDarkMode() {
    if (this.isDarkMode) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }

  onSearch(event: Event) {
    event.preventDefault();
    console.log('Search query:', this.searchQuery);
  }

  onSignOut() {
    this.authService.logout().subscribe({
      next: () => {
        console.log('Successfully logged out');
        this.router.navigate(['/']);
        // Disconnect WebSocket
        this.notificationService.disconnect();
      },
      error: (error) => {
        console.error('Logout error:', error);
        this.authService.clearLocalStorage();
        this.router.navigate(['/']);
        this.notificationService.disconnect();
      }
    });
  }

  getAvatarUrl(): string {
    if (!this.currentUser?.profileImage) {
      return this.generateDefaultAvatar();
    }
    
    const profileImage = this.currentUser.profileImage;
    
    if (profileImage.startsWith('http')) {
      return profileImage;
    }
    
    if (profileImage.startsWith('/uploads/')) {
      const baseUrl = environment.baseUrl || 'http://localhost:8084';
      return baseUrl + profileImage;
    }
    
    if (profileImage.startsWith('data:image')) {
      return profileImage;
    }
    
    return this.generateDefaultAvatar();
  }

  generateDefaultAvatar(): string {
    if (!this.currentUser) {
      return 'assets/images/user/owner.jpg';
    }
    
    let initials = 'U';
    if (this.currentUser?.firstName && this.currentUser?.lastName) {
      initials = this.currentUser.firstName.charAt(0).toUpperCase() + 
                 this.currentUser.lastName.charAt(0).toUpperCase();
    } else if (this.currentUser?.firstName) {
      initials = this.currentUser.firstName.charAt(0).toUpperCase();
    } else if (this.currentUser?.lastName) {
      initials = this.currentUser.lastName.charAt(0).toUpperCase();
    } else if (this.currentUser?.username) {
      initials = this.currentUser.username.charAt(0).toUpperCase();
    }
    
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
      <circle cx="50" cy="50" r="48" fill="#FAB40C"/>
      <text x="50" y="58" text-anchor="middle" font-family="Arial" font-size="38" fill="white">${initials}</text>
    </svg>`;
    
    return 'data:image/svg+xml;base64,' + btoa(svg);
  }

  

  navigateToMailBox(): void {
    this.router.navigate(['/mailBox']);
  }


  // Add this method to handle image errors
handleImageError(event: any): void {
  event.target.src = this.generateDefaultAvatar();
  event.target.onerror = null;
}

// Add this method to get user initials for avatar fallback
getUserInitials(): string {
  if (!this.currentUser) return 'U';
  
  if (this.currentUser.firstName && this.currentUser.lastName) {
    return (this.currentUser.firstName.charAt(0) + this.currentUser.lastName.charAt(0)).toUpperCase();
  } else if (this.currentUser.firstName) {
    return this.currentUser.firstName.charAt(0).toUpperCase();
  } else if (this.currentUser.lastName) {
    return this.currentUser.lastName.charAt(0).toUpperCase();
  } else if (this.currentUser.username) {
    return this.currentUser.username.charAt(0).toUpperCase();
  }
  return 'U';
}

// Add this method to get user email
getUserEmail(): string {
  return this.currentUser?.email || 'No email';
}


// Add these methods to your HeaderComponent class

/**
 * Get days status text in French
 */
getDaysStatusTextFr(daysUntilDue?: number): string {
  if (daysUntilDue === undefined || daysUntilDue === null) return '';
  
  if (daysUntilDue > 0) {
    return daysUntilDue === 1 ? 'Demain' : `Dans ${daysUntilDue} jours`;
  } else if (daysUntilDue === 0) {
    return "Aujourd'hui";
  } else {
    return `${Math.abs(daysUntilDue)} jours de retard`;
  }
}

/**
 * Format notification time in French
 */
formatTimeFr(createdAt: string): string {
  const date = new Date(createdAt);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHours / 24);

  if (diffMins < 1) return "À l'instant";
  if (diffMins < 60) return `Il y a ${diffMins} minute${diffMins === 1 ? '' : 's'}`;
  if (diffHours < 24) return `Il y a ${diffHours} heure${diffHours === 1 ? '' : 's'}`;
  if (diffDays < 7) return `Il y a ${diffDays} jour${diffDays === 1 ? '' : 's'}`;
  
  return date.toLocaleDateString('fr-FR', { 
    month: 'short', 
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
}
}