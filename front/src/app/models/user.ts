

export interface AuthResponse {
  token: string;
  type: string;
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
}

export interface User {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
  phone?: string;
  department?: string;
  enabled?: boolean;
  createdAt?: string;
  lastLogin?: string;
  profileImage?: string;

  failedLoginAttempts?: number;
  accountLockedUntil?: string;
  lockedByAdmin?: boolean;
}

export interface LoginUser {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
}

export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
  department?: string;
  roles: string[];
}



export interface MessageResponse {
  message: string;
}