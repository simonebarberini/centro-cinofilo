import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

interface LoginRequest {
  tenantSlug: string;
  username: string;
  password: string;
}

interface RegisterRequest {
  tenantName: string;
  tenantType: string;
  tenantSlug: string;
  username: string;
  password: string;
  email: string;
}

interface LoginResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
}

interface RegisterResponse {
  message: string;
  email: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY = 'accessToken';

  constructor(private http: HttpClient) {}

  login(tenantSlug: string, username: string, password: string): Observable<LoginResponse> {
    const body: LoginRequest = { tenantSlug, username, password };
    return this.http.post<LoginResponse>(`${environment.apiBaseUrl}/auth/login`, body).pipe(
      tap((res) => localStorage.setItem(this.TOKEN_KEY, res.token))
    );
  }

  register(data: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${environment.apiBaseUrl}/auth/register`, data);
  }

  verifyEmail(token: string): Observable<void> {
    return this.http.get<void>(`${environment.apiBaseUrl}/auth/verify-email`, {
      params: { token }
    });
  }

  resendVerification(email: string, tenantSlug: string): Observable<void> {
    return this.http.post<void>(`${environment.apiBaseUrl}/auth/resend-verification`, {
      email,
      tenantSlug
    });
  }

  forgotPassword(email: string, tenantSlug: string): Observable<void> {
    return this.http.post<void>(`${environment.apiBaseUrl}/auth/forgot-password`, {
      email,
      tenantSlug
    });
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${environment.apiBaseUrl}/auth/reset-password`, {
      token,
      newPassword
    });
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }
}
