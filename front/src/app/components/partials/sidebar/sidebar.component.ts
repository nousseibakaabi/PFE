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

  constructor(
    private router: Router,
    public layoutService: LayoutService,
    private authService: AuthService,
    private translationService: TranslationService // Inject TranslationService
  ) {}





  ngOnInit(): void {
    // Check screen size initially
    this.checkScreenSize();
    
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
  }

  // Add this decorator
  @HostListener('window:resize', ['$event'])
  onResize(event: any): void {
    this.checkScreenSize();
  }

  checkScreenSize(): void {
    this.isDesktop = window.innerWidth >= 1024; // lg breakpoint
  }

// ADD THIS METHOD: Load current user
loadCurrentUser(): void {
  // Get user from auth service
  this.user = this.authService.currentUserValue;
  
  
}


navigateAndClose(route: string, pageName: string): void {
  this.toggleSelected(pageName);
  
  // Navigate
  this.router.navigate([route]);
  
  // Close sidebar on mobile (when not desktop)
  if (!this.isDesktop) {
    this.layoutService.setSidebarOpen(false);
  }
}

updateCurrentPage(): void {
  const url = this.router.url;
  
  const routeMapping = [
    { path: 'admin/nomenclatures', page: 'Nomenclatures' },
    { path: 'admin/users', page: 'Users' },
    {path:'admin/projects', page:'admin/projects'},
    { path: 'admin/calendar', page: 'adminCalendar' },
    { path: 'admin', page: 'admin' },
    { path: 'conventions/archives', page: 'conventions/archives' }, 
    { path: 'conventions', page: 'conventions' },
    { path: 'factures', page: 'factures' },
    { path: 'commercial/calendar', page: 'commercialCalendar' },
    { path: 'commercial', page: 'commercial' },
    { path: 'decideur', page: 'decideur' },
    { path: 'chef/projet', page: 'projet' },
    { path: 'chef/calendar', page: 'chefCalendar' },
    { path: 'chef', page: 'chefProjet' },
    { path: 'profile', page: 'profile' },
  ];
  
  // Cherche le premier match (exact or partial)
  const matchedRoute = routeMapping.find(route => url.includes(route.path));
  this.currentPage = matchedRoute ? matchedRoute.page : 'ecommerce';
  
  console.log('Current URL:', url, 'Current Page:', this.currentPage); // Debug log
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

  // Add logout method
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

  toggleSelected(item: string): void {
    if (this.selected === item) {
      this.selected = '';
    } else {
      this.selected = item;
    }
    localStorage.setItem('sidebarSelected', this.selected);
  }


isAdminOrChefProjet(): boolean {
  return this.isAdminUser() || this.isChefProjetUser();
}


getCurrentUser(): User | null {
  // Return this.user directly
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

// Add this method to sidebar.component.ts
handleImageError(event: any): void {
  console.error('Image failed to load:', this.user?.profileImage);
  // Set to default avatar
  event.target.src = this.generateDefaultAvatar();
  event.target.onerror = null; // Prevent infinite loop
}

getAvatarUrl(): string {
  // Check if user exists
  if (!this.user || !this.user.profileImage) {
    return this.generateDefaultAvatar();
  }
  
  const profileImage = this.user.profileImage;
  
  // If it's already a full URL, use it
  if (profileImage.startsWith('http')) {
    return profileImage;
  }
  
  // If it's a relative path starting with /uploads/, prepend base URL
  if (profileImage.startsWith('/uploads/')) {
    const baseUrl = environment.baseUrl || 'http://localhost:8084';
    return baseUrl + profileImage;
  }
  
  // If it's base64, use it directly
  if (profileImage.startsWith('data:image')) {
    return profileImage;
  }
  
  // Default fallback
  return this.generateDefaultAvatar();
}

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

generateDefaultAvatar(): string {
  const user = this.getCurrentUser();
  if (!user) {
    return 'assets/images/user/owner.jpg';
  }
  
  // Generate initials
  let initials = 'U';
  if (user?.firstName && user?.lastName) {
    initials = user.firstName.charAt(0).toUpperCase() + 
               user.lastName.charAt(0).toUpperCase();
  } else if (user?.firstName) {
    initials = user.firstName.charAt(0).toUpperCase();
  } else if (user?.lastName) {
    initials = user.lastName.charAt(0).toUpperCase();
  } else if (user?.username) {
    initials = user.username.charAt(0).toUpperCase();
  }
  
  // Colors for different initials
  const colors = [
    '#e9d709'
    
  ];
  
  // Pick a color based on the first letter
  const colorIndex = initials.charCodeAt(0) % colors.length;
  const color = colors[colorIndex];
  
  // CREATE A RECTANGLE WITH ROUNDED CORNERS instead of circle
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="100" height="100">
    <!-- Rectangle with rounded corners (rx=15 for rounded effect) -->
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



// Add this method
toggleUserDropdown(): void {
  this.showUserDropdown = !this.showUserDropdown;
}


}