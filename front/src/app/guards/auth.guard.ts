import { Injectable } from '@angular/core';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard {
  
  constructor(private authService: AuthService, private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    const currentUser = this.authService.currentUserValue;
    
    if (currentUser) {
      // Check if route is restricted by role
      const { roles } = route.data;
      
      if (roles && !roles.some((role: string) => this.authService.hasRole(role))) {
        // Role not authorized, redirect to home
        this.router.navigate(['/']);
        return false;
      }
      
      // Authorized
      return true;
    }
    
    // Not logged in, redirect to login page
    this.router.navigate(['/'], { queryParams: { returnUrl: state.url } });
    return false;
  }
}