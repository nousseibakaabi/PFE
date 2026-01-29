// src/app/services/user.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface User {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  department?: string;
  enabled: boolean;
  roles: string[];
  profileImage?: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  // Get all users
  getAllUsers(): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/users`);
  }

  // Get users by role
  getUsersByRole(role: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/users/role/${role}`);
  }

  // Get users with ROLE_CHEF_PROJET
  getChefsProjet(): Observable<any> {
    return this.getUsersByRole('CHEF_PROJET');
  }

  // Get user by ID
  getUser(id: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/users/${id}`);
  }
}