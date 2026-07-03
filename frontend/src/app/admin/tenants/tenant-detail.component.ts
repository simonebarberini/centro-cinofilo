import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TenantAdminService } from './tenant-admin.service';
import { TenantDetailState } from './tenant-detail-state';
import { TenantDetail } from './tenant-detail.model';

@Component({
  selector: 'app-tenant-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './tenant-detail.component.html',
})
export class TenantDetailComponent implements OnInit {
  tenant: TenantDetail | null = null;
  loading = false;
  error: string | null = null;

  private tenantId = '';

  constructor(
    private route: ActivatedRoute,
    private tenantAdmin: TenantAdminService,
    private tenantDetailState: TenantDetailState
  ) {}

  ngOnInit(): void {
    this.tenantId = this.route.snapshot.paramMap.get('tenantId') ?? '';
    this.loadTenant();
  }

  loadTenant(): void {
    if (!this.tenantId) {
      this.error = 'Tenant non specificato.';
      return;
    }
    this.loading = true;
    this.error = null;
    this.tenantAdmin.getById(this.tenantId).subscribe({
      next: (data) => {
        this.tenant = data;
        this.tenantDetailState.setTenant(data);
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Impossibile caricare il tenant. Riprova.';
        this.loading = false;
        console.error('Error loading tenant', err);
      },
    });
  }
}
