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
    { path: 'conventions/archives', page: 'conventions/archives' }, 
    { path: 'conventions', page: 'conventions' },
    { path: 'factures', page: 'factures' },
    { path: 'commercial', page: 'commercial' },
    { path: 'decideur', page: 'decideur' },
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
}