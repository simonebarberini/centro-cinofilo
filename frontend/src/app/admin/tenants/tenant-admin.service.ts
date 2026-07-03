import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TenantSummary } from './tenant-summary.model';
import { TenantDetail } from './tenant-detail.model';

@Injectable({ providedIn: 'root' })
export class TenantAdminService {
  private readonly baseUrl = `${environment.apiBaseUrl}/admin/tenants`;

  constructor(private http: HttpClient) {}

  search(q: string): Observable<TenantSummary[]> {
    let params = new HttpParams();
    if (q) {
      params = params.set('q', q);
    }
    return this.http.get<TenantSummary[]>(this.baseUrl, { params });
  }

  getById(tenantId: string): Observable<TenantDetail> {
    return this.http.get<TenantDetail>(`${this.baseUrl}/${tenantId}`);
  }
}
