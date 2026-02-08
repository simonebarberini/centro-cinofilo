import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TenantSettings, UpdateTenantSettingsRequest } from '../models/tenant.model';

@Injectable({ providedIn: 'root' })
export class TenantApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/tenant`;

  constructor(private http: HttpClient) {}

  getMe(): Observable<TenantSettings> {
    return this.http.get<TenantSettings>(`${this.baseUrl}/me`);
  }

  updateMe(request: UpdateTenantSettingsRequest): Observable<TenantSettings> {
    return this.http.put<TenantSettings>(`${this.baseUrl}/me`, request);
  }
}
