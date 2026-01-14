import { Component, OnInit } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { LayoutService } from '../app/components/partials/services/layout.service';
import { UtilityService } from '../app/components/partials/services/utility.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  isSidebarOpen: boolean = false;
  currentRoute: string = '';
  
  constructor(
    private layoutService: LayoutService,
    private utilityService: UtilityService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Subscribe to sidebar state
    this.layoutService.sidebarOpen$.subscribe((isOpen) => {
      this.isSidebarOpen = isOpen;
    });
    
    // Track current route
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: any) => {
      this.currentRoute = event.url;
    });
  }

  toggleSidebar(): void {
    this.layoutService.toggleSidebar();
  }
  
  // Show layout (header/sidebar) only on these routes
  showLayout(): boolean {
    const layoutRoutes = ['/dashboard', '/profile'];
    return layoutRoutes.some(route => this.currentRoute.includes(route));
  }
}