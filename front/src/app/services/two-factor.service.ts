import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, catchError, map, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class TwoFactorService {
  private apiUrl = environment.apiUrl;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  private getHeaders(): HttpHeaders {
    const token = this.authService.token;
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : ''
    });
  }

  /**
   * Setup 2FA - get secret and QR code
   */
  setupTwoFactor(): Observable<any> {
    return this.http.post(`${this.apiUrl}/api/2fa/setup`, {}, {
      headers: this.getHeaders()
    });
  }

  /**
   * Verify and enable 2FA
   */
  verifyAndEnable(code: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/api/2fa/verify`, { code }, {
      headers: this.getHeaders()
    }).pipe(
      map((response: any) => {
        // Refresh user data after enabling 2FA
        this.authService.refreshUser().subscribe();
        return response;
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Disable 2FA
   */
  disableTwoFactor(code: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/api/2fa/disable`, { code }, {
      headers: this.getHeaders()
    }).pipe(
      map((response: any) => {
        // Refresh user data after disabling 2FA
        this.authService.refreshUser().subscribe();
        return response;
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Get 2FA status
   */
  getTwoFactorStatus(): Observable<{ enabled: boolean }> {
    return this.http.get<{ enabled: boolean }>(`${this.apiUrl}/api/2fa/status`, {
      headers: this.getHeaders()
    }).pipe(
      catchError(this.handleError)
    );
  }

  private handleError(error: any): Observable<never> {
    let errorMessage = 'An error occurred';
    if (error.error?.message) {
      errorMessage = error.error.message;
    }
    return throwError(() => new Error(errorMessage));
  }
}