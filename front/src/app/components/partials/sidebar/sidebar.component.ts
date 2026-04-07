import { Component, OnInit, Input, HostListener } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter } from 'rxjs/operators';
import { LayoutService } from '../services/layout.service';
import { AuthService } from '../../../services/auth.service';
import { TranslationService } from '../traduction/translation.service'; 
import { environment } from '../../../../environments/environment';
import { User } from '../../../models/user';

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css']
})
export class SidebarComponent implements OnInit {
  @Input() isSidebarOpen: boolean = false; 
  selected: string = 'Dashboard';
  currentPage: string = 'ecommerce'; 
  user: any = null;
  baseUrl = environment.baseUrl || 'http://localhost:8084';
  isDesktop: boolean = false;
  showUserDropdown: boolean = false;
  isDarkMode = false;
  isHovering = false;

    isRtl: boolean = false;

  constructor(
    private router: Router,
    public layoutService: LayoutService,
    private authService: AuthService,
    private translationService: TranslationService
  ) {}



// Add this method
  checkRtlState(): void {
    this.isRtl = this.translationService.isRtl();
    // Update sidebar position based on RTL
    if (this.isRtl) {
      document.documentElement.style.setProperty('--sidebar-position', 'right');
    } else {
      document.documentElement.style.setProperty('--sidebar-position', 'left');
    }
  }


  ngOnInit(): void {
    // Check screen size initially
    this.checkScreenSize();
      this.checkRtlState();

    // Get current page from URL
    this.layoutService.sidebarOpen$.subscribe((isOpen) => {
      this.isSidebarOpen = isOpen;
    });
    
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      this.updateCurrentPage();
    });

    this.updateCurrentPage();
    
    // Load selected from localStorage
    const savedSelected = localStorage.getItem('sidebarSelected');
    if (savedSelected) {
      this.selected = savedSelected;
    }

    // Load the current user
    this.loadCurrentUser();

    // Add dark mode check
    this.checkDarkMode();
    
    // Listen for dark mode changes
    this.listenForDarkModeChanges();

    this.checkRtlState();
    
    // Listen for language changes
    this.translationService.getLanguageChangeObservable().subscribe(() => {
      this.checkRtlState();
    });
    
  }

  checkDarkMode(): void {
    this.isDarkMode = document.documentElement.classList.contains('dark');
  }


navigateWithLang(route: string): void {
  const currentLang = localStorage.getItem('appLanguage') || 'fr';
  this.router.navigate([`/${route}`]);
}

  listenForDarkModeChanges(): void {
    const observer = new MutationObserver((mutations) => {
      mutations.forEach((mutation) => {
        if (mutation.attributeName === 'class') {
          this.checkDarkMode();
        }
      });
    });

    observer.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ['class']
    });
  }

  onSidebarHover(event: MouseEvent): void {
    if (!this.isSidebarOpen) {
      this.isHovering = event.type === 'mouseenter';
    }
  }

  @HostListener('window:resize', ['$event'])
  onResize(event: any): void {
    this.checkScreenSize();
  }

  checkScreenSize(): void {
    this.isDesktop = window.innerWidth >= 1024;
  }

  loadCurrentUser(): void {
    this.user = this.authService.currentUserValue;
    console.log('Current user loaded:', this.user);
  }


  
  updateCurrentPage(): void {
    const url = this.router.url;
    
    const routeMapping = [
      { path: 'admin/nomenclatures', page: 'Nomenclatures' },
      { path: 'admin/users', page: 'Users' },
      { path: 'admin/calendar', page: 'admin/calendar' },
      { path: 'admin', page: 'admin' },
      { path: 'conventions/archives', page: 'conventions/archives' }, 
      { path: 'conventions', page: 'conventions' },
      { path: 'factures', page: 'factures' },
      { path: 'commercial/calendar', page: 'commercial/calendar' },
      { path: 'commercial', page: 'commercial' },
      { path: 'decideur', page: 'decideur' },
      {path :'archives/applications', page:'archives/applications'},
      { path: 'applications', page: 'applications'},
      { path: 'chef/calendar', page: 'chef/calendar' },
      { path: 'chef', page: 'chef' },
      { path: 'requests', page: 'requests' },
      { path: 'profile', page: 'profile' },
      { path: 'planFacturation', page: 'planFacturation' }

    ];
    
    const matchedRoute = routeMapping.find(route => url.includes(route.path));
    this.currentPage = matchedRoute ? matchedRoute.page : 'ecommerce';
  }

  isAdminUser(): boolean {
    return this.authService.isAdmin();
  }

  isCommercialUser(): boolean {
    return this.authService.isCommercial() ;
  }

  isDecideurUser(): boolean {
    return this.authService.isDecideur() ;
  }

  isChefProjetUser(): boolean {
    return this.authService.isChefProjet() ;
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => {
        this.router.navigate(['/']);
      },
      error: (error) => {
        console.error('Logout error:', error);
        this.authService.clearLocalStorage();
        this.router.navigate(['/']);
      }
    });
  }



  isAdminOrChefProjet(): boolean {
    return this.isAdminUser() || this.isChefProjetUser();
  }

  getCurrentUser(): User | null {
    return this.user || this.authService.currentUserValue;
  }

  getUserFullName(): string {
    const user = this.getCurrentUser();
    if (!user) return 'User';
    
    if (user.firstName && user.lastName) {
      return `${user.firstName} ${user.lastName}`;
    } else if (user.firstName) {
      return user.firstName;
    } else {
      return user.username || 'User';
    }
  }

  getUserEmail(): string {
    const user = this.getCurrentUser();
    return user?.email || 'No email';
  }

  // FIXED: Properly handle avatar URL from backend
  getAvatarUrl(): string {
    // If no user or no profile image, return empty string
    if (!this.user || !this.user.profileImage) {
      console.log('No profile image for user');
      return '';
    }
    
    const profileImage = this.user.profileImage;
    console.log('Profile image from user:', profileImage);
    
    // If it's already a full URL, use it
    if (profileImage.startsWith('http')) {
      return profileImage;
    }
    
    // If it's a relative path starting with /uploads/, prepend base URL
    if (profileImage.startsWith('/uploads/')) {
      const fullUrl = this.baseUrl + profileImage;
      console.log('Constructed full URL:', fullUrl);
      return fullUrl;
    }
    
    // If it's base64, use it directly
    if (profileImage.startsWith('data:image')) {
      return profileImage;
    }
    
    console.log('Unknown image format:', profileImage);
    return '';
  }

  // ADD THIS: Handle image loading errors
  handleImageError(event: any): void {
    console.error('Failed to load image:', event.target.src);
    // Hide the image and show initials instead
    event.target.style.display = 'none';
    event.target.parentElement.classList.add('show-initials');
  }

  closeUserDropdown(): void {
    this.showUserDropdown = false;
  }

  toggleUserDropdown(event?: MouseEvent): void {
    if (event) {
      event.stopPropagation();
    }
    this.showUserDropdown = !this.showUserDropdown;
  }

  getUserInitials(): string {
    const user = this.getCurrentUser();
    if (!user) return 'U';
    
    if (user.firstName && user.lastName) {
      return (user.firstName.charAt(0) + user.lastName.charAt(0)).toUpperCase();
    } else if (user.firstName) {
      return user.firstName.charAt(0).toUpperCase();
    } else if (user.lastName) {
      return user.lastName.charAt(0).toUpperCase();
    } else if (user.username) {
      return user.username.charAt(0).toUpperCase();
    }
    return 'U';
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    
    if (this.showUserDropdown && 
        !target.closest('.user-dropdown-container') && 
        !target.closest('.user-menu-button')) {
      this.showUserDropdown = false;
    }
  }
}