import { Component, OnInit, Input } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter } from 'rxjs/operators';
import { LayoutService } from '../services/layout.service';
import { AuthService } from '../../../services/auth.service';
import { TranslationService } from '../traduction/translation.service'; 

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
    private authService: AuthService,
    private translationService: TranslationService // Inject TranslationService
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
  
  // Définit un mapping URL -> currentPage
  const routeMapping = [
    { path: 'admin/nomenclatures', page: 'Nomenclatures' },
    { path: 'admin/users', page: 'Users' },
    { path: 'admin', page: 'admin' },
     { path: 'conventions', page: 'conventions' },
  { path: 'factures', page: 'factures' },
    { path: 'calendar', page: 'calendar' },
    { path: 'profile', page: 'profile' },
    { path: 'form-elements', page: 'formElements' },
    { path: 'basic-tables', page: 'basicTables' },
    { path: 'blank', page: 'blank' },
    { path: '404', page: 'page404' },
    { path: 'line-chart', page: 'lineChart' },
    { path: 'bar-chart', page: 'barChart' },
    { path: 'alerts', page: 'alerts' },
    { path: 'avatars', page: 'avatars' },
    { path: 'badge', page: 'badge' },
    { path: 'buttons', page: 'buttons' },
    { path: 'images', page: 'images' },
    { path: 'videos', page: 'videos' },
    { path: 'signin', page: 'signin' },
    { path: 'signup', page: 'signup' }
  ];
  
  // Cherche le premier match
  const matchedRoute = routeMapping.find(route => url.includes(route.path));
  this.currentPage = matchedRoute ? matchedRoute.page : 'ecommerce';
}

  // Add this method to check if user is admin
  isAdminUser(): boolean {
    return this.authService.isAdmin();
  }

isCommercialUser(): boolean {
  return this.authService.isCommercial() || this.authService.isAdmin();
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