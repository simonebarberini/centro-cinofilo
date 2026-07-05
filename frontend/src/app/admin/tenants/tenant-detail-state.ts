import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { TenantDetail } from './tenant-detail.model';

@Injectable()
export class TenantDetailState {
  private readonly tenantSubject = new BehaviorSubject<TenantDetail | null>(null);

  readonly tenant$: Observable<TenantDetail | null> = this.tenantSubject.asObservable();

  setTenant(tenant: TenantDetail): void {
    this.tenantSubject.next(tenant);
  }

  get tenant(): TenantDetail | null {
    return this.tenantSubject.value;
  }
}
