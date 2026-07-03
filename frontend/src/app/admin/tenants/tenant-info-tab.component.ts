import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TenantDetailState } from './tenant-detail-state';

@Component({
  selector: 'app-tenant-info-tab',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './tenant-info-tab.component.html',
})
export class TenantInfoTabComponent {
  readonly tenant$ = this.tenantDetailState.tenant$;

  constructor(private tenantDetailState: TenantDetailState) {}
}
