import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TenantModuleStatusItem } from './tenant-module-status.model';
import { GrantResponse } from './grant.model';

@Injectable({ providedIn: 'root' })
export class SubscriptionAdminService {
  private readonly tenantsUrl = `${environment.apiBaseUrl}/admin/tenants`;
  private readonly grantsUrl = `${environment.apiBaseUrl}/admin/grants`;
  private readonly subscriptionsUrl = `${environment.apiBaseUrl}/admin/subscriptions`;

  constructor(private http: HttpClient) {}

  getTenantModules(tenantId: string): Observable<TenantModuleStatusItem[]> {
    return this.http.get<TenantModuleStatusItem[]>(`${this.tenantsUrl}/${tenantId}/modules`);
  }

  grant(tenantId: string, moduleKey: string, note?: string): Observable<GrantResponse> {
    return this.http.post<GrantResponse>(this.grantsUrl, {
      tenantId,
      moduleKey,
      note: note ?? null,
    });
  }

  cancel(tenantId: string, moduleKey: string): Observable<void> {
    return this.http.post<void>(`${this.subscriptionsUrl}/${tenantId}/${moduleKey}/cancel`, {});
  }
}
