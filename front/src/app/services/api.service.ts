import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient, private authService: AuthService) {}

  // UPDATE THIS METHOD to refresh user after update
  updateProfile(profileData: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/profile/update`, profileData, { 
      headers: this.getHeaders() 
    }).pipe(
      tap(() => {
        // Refresh user data after profile update
        this.authService.refreshUser().subscribe();
      })
    );
  }

  // UPDATE THIS METHOD to refresh user after avatar update
  uploadAvatar(formData: FormData): Observable<any> {
    const token = this.authService.token;
    const headers = new HttpHeaders({
      'Authorization': token ? `Bearer ${token}` : ''
    });
    
    return this.http.post(`${this.apiUrl}/profile/upload-avatar`, formData, { headers }).pipe(
      tap((response: any) => {
        // Update current user with new profile image
        if (response.profileImage) {
          this.authService.updateCurrentUser({ profileImage: response.profileImage });
        }
        // Also refresh full user data
        this.authService.refreshUser().subscribe();
      })
    );
  }

  // UPDATE THIS METHOD to refresh user after update
  updateProfileWithAvatar(formData: FormData): Observable<any> {
    const token = this.authService.token;
    const headers = new HttpHeaders({
      'Authorization': token ? `Bearer ${token}` : ''
    });
    
    return this.http.post(`${this.apiUrl}/profile/update-with-avatar`, formData, { headers }).pipe(
      tap(() => {
        // Refresh user data after profile update with avatar
        this.authService.refreshUser().subscribe();
      })
    );
  }

  // Other existing methods...
  getUserContent(): Observable<any> {
    return this.http.get(`${this.apiUrl}/test/user`, { headers: this.getHeaders() });
  }

  getAdminContent(): Observable<any> {
    return this.http.get(`${this.apiUrl}/test/admin`, { headers: this.getHeaders() });
  }

  getCommercialContent(): Observable<any> {
    return this.http.get(`${this.apiUrl}/test/commercial`, { headers: this.getHeaders() });
  }

  getDecideurContent(): Observable<any> {
    return this.http.get(`${this.apiUrl}/test/decideur`, { headers: this.getHeaders() });
  }

  getChefProjetContent(): Observable<any> {
    return this.http.get(`${this.apiUrl}/test/chef-projet`, { headers: this.getHeaders() });
  }

  healthCheck(): Observable<any> {
    return this.http.get(`${this.apiUrl}/test/all`);
  }

  getPublicContent(): Observable<any> {
    return this.http.get(`${this.apiUrl}/test/all`);
  }

  private getHeaders(): HttpHeaders {
    const token = this.authService.token;
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : ''
    });
  }
}