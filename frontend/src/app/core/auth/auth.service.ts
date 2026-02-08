import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

interface LoginRequest {
  tenantSlug: string;
  username: string;
  password: string;
}

interface LoginResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
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
