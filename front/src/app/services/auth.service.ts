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

private handleError(error: HttpErrorResponse): Observable<never> {
  console.log('🔴 HANDLE ERROR called with:', error);
  
  let errorMessage = 'An error occurred';
  let remainingAttempts: number | undefined;
  let lockType: string | undefined;
  let lockUntil: string | undefined;
  let isLastAttempt: boolean = false;
  
  if (error.error) {
    console.log('error.error:', error.error);
    const backendError = error.error;
    
    // Try to get the error message from different places
    if (backendError.message) {
      errorMessage = backendError.message;
      console.log('Found message in backendError.message:', errorMessage);
    } else if (backendError.error && typeof backendError.error === 'string') {
      errorMessage = backendError.error;
      console.log('Found message in backendError.error (string):', errorMessage);
    } else if (typeof backendError === 'string') {
      errorMessage = backendError;
      console.log('Backend error is string:', errorMessage);
    }
    
    // Get other properties
    if (backendError.remainingAttempts !== undefined) {
      remainingAttempts = backendError.remainingAttempts;
    }
    if (backendError.lockType) {
      lockType = backendError.lockType;
    }
    if (backendError.lockUntil) {
      lockUntil = backendError.lockUntil;
    }
    if (backendError.isLastAttempt !== undefined || backendError.error === 'LastAttemptWarning') {
      isLastAttempt = true;
    }
  } else if (error.message) {
    errorMessage = error.message;
    console.log('Found message in error.message:', errorMessage);
  }
  
  console.log('Final error message:', errorMessage);
  
  // Create a custom error object with all the information
  const customError = new Error(errorMessage);
  (customError as any).remainingAttempts = remainingAttempts;
  (customError as any).lockType = lockType;
  (customError as any).lockUntil = lockUntil;
  (customError as any).isLastAttempt = isLastAttempt;
  (customError as any).errorCode = error.error?.error;
  (customError as any).status = error.status;
  (customError as any).statusText = error.statusText;
  
  console.log('Throwing custom error:', customError);
  return throwError(() => customError);
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
        return this.handleError(error);
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
      return this.handleError(error);
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
        // Update current user with full profile data including profileImage and notifMode
        const updatedUser = { ...user };
        localStorage.setItem('currentUser', JSON.stringify(updatedUser));
        this.currentUserSubject.next(updatedUser);
      }),
      catchError(this.handleError.bind(this))
    );
  }

  getProfile(): Observable<User> {
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${this.token}`
    });
    
    return this.http.get<User>(`${this.apiUrl}/profile/me`, { headers }).pipe(
      tap(user => {
        // Update current user when profile is fetched
        const updatedUser = { ...user };
        localStorage.setItem('currentUser', JSON.stringify(updatedUser));
        this.currentUserSubject.next(updatedUser);
      }),
      catchError(this.handleError.bind(this))
    );
  }

  // UPDATE THIS METHOD to handle avatar updates
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
        // Optionally refresh user data after password change
        this.refreshUser().subscribe();
      }),
      catchError(this.handleError.bind(this))
    );
  }

  forgotPassword(email: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/forgot-password`, { email })
      .pipe(
        catchError((error: HttpErrorResponse) => {
          let errorMessage = 'Failed to send reset link. Please try again.';
          if (error.error && error.error.message) {
            errorMessage = error.error.message;
          } else if (error.status === 400) {
            errorMessage = 'Invalid email address.';
          } else if (error.status === 404) {
            errorMessage = 'No account found with this email address.';
          }
          return throwError(() => new Error(errorMessage));
        })
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
      catchError(this.handleError.bind(this))
    );
  }

  public clearLocalStorage(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('currentUser');
    this.currentUserSubject.next(null);
  }
}