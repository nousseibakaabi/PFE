import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError } from 'rxjs';
import { map, catchError, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { LoginRequest, RegisterRequest, AuthResponse, User, MessageResponse } from '../models/user';

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

 
login(loginRequest: LoginRequest): Observable<AuthResponse> {
  return this.http.post<AuthResponse>(`${this.apiUrl}/auth/login`, loginRequest)
    .pipe(
      map(response => {
        // Store user details and jwt token in local storage
        localStorage.setItem('token', response.token);
        const user: User = {
          id: response.id,
          username: response.username,
          email: response.email,
          firstName: response.firstName,
          lastName: response.lastName,
          roles: response.roles
        };
        localStorage.setItem('currentUser', JSON.stringify(user));
        this.currentUserSubject.next(user);
        return response;
      }),
      catchError((error: HttpErrorResponse) => {
        console.log('Login error details:', error);
        
        // Extract error message from backend response
        let errorMessage = 'Login failed';
        let remainingAttempts: number | undefined;
        let lockType: string | undefined;
        let lockUntil: string | undefined;
        let isLastAttempt: boolean = false;
        
        if (error.error) {
          const backendError = error.error;
          
          if (backendError.message) {
            errorMessage = backendError.message;
          }
          
          if (backendError.remainingAttempts !== undefined) {
            remainingAttempts = backendError.remainingAttempts;
          }
          
          if (backendError.lockType) {
            lockType = backendError.lockType;
          }
          
          if (backendError.lockUntil) {
            lockUntil = backendError.lockUntil;
          }
          
          if (backendError.isLastAttempt !== undefined) {
            isLastAttempt = backendError.isLastAttempt;
          }
          
          if (backendError.error === 'LastAttemptWarning') {
            isLastAttempt = true;
          }
        }
        
        // Create a custom error with all info
        const customError = new Error(errorMessage);
        (customError as any).remainingAttempts = remainingAttempts;
        (customError as any).lockType = lockType;
        (customError as any).lockUntil = lockUntil;
        (customError as any).isLastAttempt = isLastAttempt;
        (customError as any).errorCode = error.error?.error;
        
        return throwError(() => customError);
      })
    );
}

  register(registerRequest: RegisterRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.apiUrl}/auth/register`, registerRequest);
  }

  // ADD THIS METHOD: Update user in localStorage and BehaviorSubject
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

  // ADD THIS METHOD: Refresh user data from backend
  refreshUser(): Observable<User> {
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${this.token}`
    });
    
    return this.http.get<User>(`${this.apiUrl}/profile/me`, { headers }).pipe(
      tap(user => {
        // Update current user with full profile data including profileImage
        const updatedUser = { ...user };
        localStorage.setItem('currentUser', JSON.stringify(updatedUser));
        this.currentUserSubject.next(updatedUser);
      })
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
      })
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
      })
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
      })
    );
  }

  public clearLocalStorage(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('currentUser');
    this.currentUserSubject.next(null);
  }
}