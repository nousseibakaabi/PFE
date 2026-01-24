import { Injectable } from '@angular/core';
import { HttpRequest, HttpHandler, HttpEvent, HttpInterceptor, HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(private authService: AuthService, private router: Router) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // DEBUG: Afficher toutes les informations
    console.log('🚀 AuthInterceptor intercepting request to:', request.url);
    console.log('📝 Request method:', request.method);
    
    // Vérifier le token de différentes manières
    const tokenFromAuthService = this.authService.token;
    const tokenFromLocalStorage = localStorage.getItem('token');
    const token = tokenFromAuthService || tokenFromLocalStorage;
    
    console.log('🔑 Token from authService:', tokenFromAuthService ? 'YES' : 'NO');
    console.log('🔑 Token from localStorage:', tokenFromLocalStorage ? 'YES' : 'NO');
    console.log('🔑 Token to use:', token ? token.substring(0, 30) + '...' : 'NO TOKEN');
    
    let authRequest = request;
    
    // Ajouter le header Authorization
    if (token) {
      console.log('✅ Adding Authorization header to request');
      authRequest = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
      
      // Afficher les headers de la requête
      console.log('📋 Request headers after clone:');
      authRequest.headers.keys().forEach(key => {
        console.log(`  ${key}: ${authRequest.headers.get(key)?.substring(0, 50)}...`);
      });
    } else {
      console.warn('⚠️ No token found, sending request without Authorization header');
    }

    // Envoyer la requête
    return next.handle(authRequest).pipe(
      tap(event => {
        if (event instanceof HttpResponse) {
          console.log('✅ Response received for:', request.url);
          console.log('📊 Response status:', event.status);
        }
      }),
      catchError((error: HttpErrorResponse) => {
        console.error('❌ HTTP Error in interceptor:');
        console.error('  URL:', error.url);
        console.error('  Status:', error.status, error.statusText);
        console.error('  Error message:', error.message);
        
        // Vérifier si le header Authorization était présent
        const hadAuthHeader = authRequest.headers.has('Authorization');
        console.error('  Had Authorization header:', hadAuthHeader);
        if (hadAuthHeader) {
          const authHeader = authRequest.headers.get('Authorization');
          console.error('  Authorization header value:', authHeader?.substring(0, 50) + '...');
        }
        
        // If 401 Unauthorized, logout and redirect to login
        if (error.status === 401) {
          console.log('🔒 401 Unauthorized detected');
          console.log('💡 Possible causes:');
          console.log('  1. Token expired');
          console.log('  2. Token invalid');
          console.log('  3. Backend not accepting token');
          
          // Afficher l'heure actuelle pour vérifier l'expiration
          console.log('🕐 Current time:', new Date().toISOString());
          
          this.authService.clearLocalStorage();
          this.router.navigate(['/']);
        }
        
        // If 403 Forbidden, redirect to home
        if (error.status === 403) {
          console.log('🚫 403 Forbidden detected');
          this.router.navigate(['/']);
        }
        
        return throwError(() => error);
      })
    );
  }
}