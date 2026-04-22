import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError } from 'rxjs';
import { map, catchError, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { LoginRequest , AuthResponse, User, MessageResponse } from '../models/user';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = environment.apiUrl;
  private currentUserSubject: BehaviorSubject<User | null>;
  public currentUser: Observable<User | null>;

  constructor(private http: HttpClient) {
    const storedUser = localStorage.getItem('currentUser');
    this.currentUserSubject = new BehaviorSubject<User | null>(
      storedUser ? JSON.parse(storedUser) : null
    );
    this.currentUser = this.currentUserSubject.asObservable();
  }

  private getHeaders(): HttpHeaders {
    const token = this.token;
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : ''
    });
  }

  public get currentUserValue(): User | null {
    return this.currentUserSubject.value;
  }

  public get token(): string | null {
    return localStorage.getItem('token');
  }

  // FIXED: Pass through the original error instead of creating a new one
  private handleError(error: HttpErrorResponse): Observable<never> {
    console.log('🔴 HANDLE ERROR called with:', error);
    
    // IMPORTANT: Throw the original error object to preserve all backend data
    // This allows the component to access error.error.lockUntil, error.error.minutesRemaining, etc.
    return throwError(() => error);
  }

  login(loginRequest: LoginRequest): Observable<AuthResponse> {
    console.log('🚀 AuthService.login called');
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/login`, loginRequest)
      .pipe(
        map(response => {
          console.log('📦 Login response:', response);
          
          // Vérifier si la 2FA est requise
          if (response.requiresTwoFactor) {
            console.log('🔐 2FA required, tempToken:', response.tempToken);
            // Stocker le token temporaire et attendre la vérification 2FA
            if (response.tempToken) {
              sessionStorage.setItem('2fa_temp_token', response.tempToken);
            }
            sessionStorage.setItem('2fa_required', 'true');
            return response;
          }
          
          // Sinon, procéder normalement
          if (response.token) {
            localStorage.setItem('token', response.token);
          }
          const user: User = {
            id: response.id!,
            username: response.username!,
            email: response.email!,
            firstName: response.firstName!,
            lastName: response.lastName!,
            roles: response.roles!
          };
          localStorage.setItem('currentUser', JSON.stringify(user));
          this.currentUserSubject.next(user);
          return response;
        }),
        catchError((error: HttpErrorResponse) => {
          console.error('❌ Login error:', error);
          // Pass through the original error
          return throwError(() => error);
        })
      );
  }

  verifyTwoFactor(code: string, tempToken: string): Observable<AuthResponse> {
    console.log('🔐 verifyTwoFactor called with code:', code, 'tempToken:', tempToken);
    
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/verify-2fa`, {
      tempToken: tempToken,
      code: code
    }).pipe(
      map(response => {
        console.log('✅ verifyTwoFactor response:', response);
        if (response.token) {
          localStorage.setItem('token', response.token);
        }
        const user: User = {
          id: response.id!,
          username: response.username!,
          email: response.email!,
          firstName: response.firstName!,
          lastName: response.lastName!,
          roles: response.roles!
        };
        localStorage.setItem('currentUser', JSON.stringify(user));
        this.currentUserSubject.next(user);
        
        // Clean up temporary data
        sessionStorage.removeItem('2fa_temp_token');
        sessionStorage.removeItem('2fa_required');
        sessionStorage.removeItem('2fa_code');
        sessionStorage.removeItem('2fa_backup_code');
        
        return response;
      }),
      catchError((error: HttpErrorResponse) => {
        console.log('❌ verifyTwoFactor error caught:', error);
        return throwError(() => error);
      })
    );
  }

  updateCurrentUser(updates: Partial<User>): void {
    const currentUser = this.currentUserValue;
    if (currentUser) {
      const updatedUser = { ...currentUser, ...updates };
      localStorage.setItem('currentUser', JSON.stringify(updatedUser));
      this.currentUserSubject.next(updatedUser);
    }
  }

  getCurrentUser(): User | null {
    return this.currentUserValue;
  }

  refreshUser(): Observable<User> {
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${this.token}`
    });
    
    return this.http.get<User>(`${this.apiUrl}/profile/me`, { headers }).pipe(
      tap(user => {
        const updatedUser = { ...user };
        localStorage.setItem('currentUser', JSON.stringify(updatedUser));
        this.currentUserSubject.next(updatedUser);
      }),
      catchError((error: HttpErrorResponse) => throwError(() => error))
    );
  }

  getProfile(): Observable<User> {
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${this.token}`
    });
    
    return this.http.get<User>(`${this.apiUrl}/profile/me`, { headers }).pipe(
      tap(user => {
        const updatedUser = { ...user };
        localStorage.setItem('currentUser', JSON.stringify(updatedUser));
        this.currentUserSubject.next(updatedUser);
      }),
      catchError((error: HttpErrorResponse) => throwError(() => error))
    );
  }

  changePassword(currentPassword: string, newPassword: string, confirmPassword: string): Observable<any> {
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${this.token}`
    });
    
    return this.http.post(`${this.apiUrl}/auth/change-password`, {
      currentPassword,
      newPassword,
      confirmPassword
    }, { headers }).pipe(
      tap(() => {
        this.refreshUser().subscribe();
      }),
      catchError((error: HttpErrorResponse) => throwError(() => error))
    );
  }

  forgotPassword(email: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/forgot-password`, { email })
      .pipe(
        catchError((error: HttpErrorResponse) => throwError(() => error))
      );
  }

  resetPassword(token: string, newPassword: string, confirmPassword: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/reset-password`, {
      token,
      newPassword,
      confirmPassword
    });
  }

  isLoggedIn(): boolean {
    return !!this.token;
  }

  hasRole(role: string): boolean {
    const user = this.currentUserValue;
    if (!user) return false;
    return user.roles.includes(role);
  }

  isAdmin(): boolean {
    return this.hasRole('ROLE_ADMIN');
  }

  isCommercial(): boolean {
    return this.hasRole('ROLE_COMMERCIAL_METIER');
  }

  isDecideur(): boolean {
    return this.hasRole('ROLE_DECIDEUR');
  }

  isChefProjet(): boolean {
    return this.hasRole('ROLE_CHEF_PROJET');
  }

  logout(): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/logout`, {}, { 
      headers: this.getHeaders() 
    }).pipe(
      map(() => {
        this.clearLocalStorage();
      }),
      catchError((error: HttpErrorResponse) => throwError(() => error))
    );
  }

  public clearLocalStorage(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('currentUser');
    this.currentUserSubject.next(null);
  }
}