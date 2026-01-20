import { Component, OnInit, Input } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter } from 'rxjs/operators';
import { LayoutService } from '../services/layout.service';
import { AuthService } from '../../../services/auth.service'; // Add this import

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css']
})
export class SidebarComponent implements OnInit {
   @Input() isSidebarOpen: boolean = false; 
  selected: string = 'Dashboard';
  currentPage: string = 'ecommerce'; 

  constructor(
    private router: Router,
    private layoutService: LayoutService,
    private authService: AuthService // Inject AuthService
  ) {}

  ngOnInit(): void {
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
  }

  updateCurrentPage(): void {
    const url = this.router.url;
    if (url.includes('admin')) {
      this.currentPage = 'adminUsers';
    } else if (url.includes('calendar')) {
      this.currentPage = 'calendar';
    } else if (url.includes('profile')) {
      this.currentPage = 'profile';
    } else if (url.includes('form-elements')) {
      this.currentPage = 'formElements';
    } else if (url.includes('basic-tables')) {
      this.currentPage = 'basicTables';
    } else if (url.includes('blank')) {
      this.currentPage = 'blank';
    } else if (url.includes('404')) {
      this.currentPage = 'page404';
    } else if (url.includes('line-chart')) {
      this.currentPage = 'lineChart';
    } else if (url.includes('bar-chart')) {
      this.currentPage = 'barChart';
    } else if (url.includes('alerts')) {
      this.currentPage = 'alerts';
    } else if (url.includes('avatars')) {
      this.currentPage = 'avatars';
    } else if (url.includes('badge')) {
      this.currentPage = 'badge';
    } else if (url.includes('buttons')) {
      this.currentPage = 'buttons';
    } else if (url.includes('images')) {
      this.currentPage = 'images';
    } else if (url.includes('videos')) {
      this.currentPage = 'videos';
    } else if (url.includes('signin')) {
      this.currentPage = 'signin';
    } else if (url.includes('signup')) {
      this.currentPage = 'signup';
    } else {
      this.currentPage = 'ecommerce';
    }
  }

  // Add this method to check if user is admin
  isAdminUser(): boolean {
    return this.authService.isAdmin();
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
}