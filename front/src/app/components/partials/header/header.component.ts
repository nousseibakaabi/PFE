import { Component, OnInit, HostListener, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { LayoutService } from '../services/layout.service';
import { AuthService } from '../../../services/auth.service'; 
import { User } from '../../../models/user'; 
import { Subscription } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { TranslationService } from '../traduction/translation.service';

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

  // WebSocket properties
 

  isLanguageDropdownOpen = false;
  currentLanguage = 'en';
  languages: any[] = [];
  translationActive = false;

  // UPDATED: Real notifications array
  notifications: any[] = [];

  constructor(
    private router: Router,
    private layoutService: LayoutService,
    private authService: AuthService,
    private translationService: TranslationService,
  ) {}

  ngOnInit(): void {
    this.checkScreenSize();
    
    // Check for saved dark mode preference
    const savedDarkMode = localStorage.getItem('darkMode');
    if (savedDarkMode) {
      this.isDarkMode = savedDarkMode === 'true';
      this.applyDarkMode();
    } else {
      // Check system preference
      this.isDarkMode = window.matchMedia('(prefers-color-scheme: dark)').matches;
      this.applyDarkMode();
    }
    
    // Subscribe to user changes
    this.userSubscription = this.authService.currentUser.subscribe(
      user => {
        this.currentUser = user;
        console.log('Header user updated:', this.currentUser?.profileImage);
      }
    );

    this.languages = this.translationService.languages;
    this.currentLanguage = this.translationService.getCurrentLanguage();
    
    // Test translation server
    this.translationService.testConnection().subscribe(isActive => {
      this.translationActive = isActive;
      if (!isActive) {
        console.warn('Translation server is not running on localhost:5000');
      }
    });  

  }

  



 


 

 

  


  ngOnDestroy(): void {
    // Unsubscribe to prevent memory leaks
    if (this.userSubscription) {
      this.userSubscription.unsubscribe();
    }
    
   
  }


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
    
    // Convert role to a more readable format
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
    this.isMobile = window.innerWidth < 1024; // lg breakpoint
  }


  toggleSidebar() {
    this.layoutService.toggleSidebar();
  }

  toggleMenu() {
    this.isMenuOpen = !this.isMenuOpen;
  }

  toggleNotifications() {
    this.isNotificationsOpen = !this.isNotificationsOpen;
    if (this.isNotificationsOpen && this.isNotifying) {
      this.isNotifying = false;
    }
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
    // Implement search functionality
  }

 onSignOut() {
  this.authService.logout().subscribe({
    next: () => {
      console.log('Successfully logged out');
      this.router.navigate(['/']);
    },
    error: (error) => {
      console.error('Logout error:', error);
      // Even if API call fails, clear local storage
      this.authService.clearLocalStorage();
      this.router.navigate(['/']);
    },
    complete: () => {
      // Force redirect even if subscription completes without navigation
      this.authService.clearLocalStorage();
      this.router.navigate(['/']);
    }
  });
}

getAvatarUrl(): string {
    if (!this.currentUser?.profileImage) {
      return this.generateDefaultAvatar();
    }
    
    const profileImage = this.currentUser.profileImage;
    
    // If it's already a full URL, use it
    if (profileImage.startsWith('http')) {
      return profileImage;
    }
    
    // If it's a relative path starting with /uploads/, prepend base URL
    if (profileImage.startsWith('/uploads/')) {
      const baseUrl = environment.baseUrl || 'http://localhost:8081';
      return baseUrl + profileImage;
    }
    
    // If it's base64, use it directly
    if (profileImage.startsWith('data:image')) {
      return profileImage;
    }
    
    // Default fallback
    return this.generateDefaultAvatar();
  }

  // ADD THIS METHOD: Generate default avatar
  generateDefaultAvatar(): string {
    if (!this.currentUser) {
      return 'assets/images/user/owner.jpg';
    }
    
    // Generate initials
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
    
    // Simple SVG with initials as fallback
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
      <circle cx="50" cy="50" r="48" fill="#4F46E5"/>
      <text x="50" y="58" text-anchor="middle" font-family="Arial" font-size="38" fill="white">${initials}</text>
    </svg>`;
    
    return 'data:image/svg+xml;base64,' + btoa(svg);
  }


   toggleLanguageDropdown(): void {
    this.isLanguageDropdownOpen = !this.isLanguageDropdownOpen;
  }

  changeLanguage(langCode: string): void {
    this.translationService.setLanguage(langCode);
    this.currentLanguage = langCode;
    this.isLanguageDropdownOpen = false;
    
    // Show success message
    const langName = this.translationService.getLanguageName(langCode);
    console.log(`Language changed to ${langName}`);
  }

  getCurrentLanguageFlag(): string {
    return this.translationService.getLanguageFlag(this.currentLanguage);
  }

  getCurrentLanguageName(): string {
    return this.translationService.getLanguageName(this.currentLanguage);
  }


  // Add this method to the HeaderComponent class
openTranslationHelp(): void {
  const helpMessage = `
    Translation server not running!
    
    To enable translations, please run:
    
    1. Open Command Prompt/Terminal
    2. Install: pip install libretranslate
    3. Run: libretranslate --host 127.0.0.1 --port 5000
    4. Keep the window open
    
    OR
    
    1. Run in background:
       nohup libretranslate --host 127.0.0.1 --port 5000 > translate.log 2>&1 &
    
    Then refresh this page.
  `;
  
  alert(helpMessage);
}
}