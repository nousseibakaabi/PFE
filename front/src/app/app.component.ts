// app.component.ts
import { Component, OnInit } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { LayoutService } from '../app/components/partials/services/layout.service';
import { UtilityService } from '../app/components/partials/services/utility.service';
import { TranslationService } from '../app/components/partials/traduction/translation.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  isSidebarOpen: boolean = false;
  currentRoute: string = '';
  isRtl: boolean = false;

  constructor(
    private layoutService: LayoutService,
    private utilityService: UtilityService,
    private router: Router,
    private translationService: TranslationService
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
    
    // Subscribe to language changes for RTL
    this.translationService.getLanguageChangeObservable().subscribe(() => {
      this.isRtl = this.translationService.isRtl();
    });
    
    // Initial check
    this.isRtl = this.translationService.isRtl();
  }

  toggleSidebar(): void {
    this.layoutService.toggleSidebar();
  }
  
  showLayout(): boolean {
    const layoutRoutes = ['/profile','/admin','/conventions','/factures','/commercial','/decideur','/chef' ,'/calendar' ,'/mailBox','/application','/notifications','/requests','/planFacturation'];
    return layoutRoutes.some(route => this.currentRoute.includes(route));
  }
}