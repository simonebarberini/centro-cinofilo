import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { forkJoin } from 'rxjs';
import { CatalogAdminService } from '../catalog/catalog-admin.service';
import { CatalogModule } from '../catalog/catalog-module.model';
import { SubscriptionAdminService } from '../subscriptions/subscription-admin.service';
import { TenantModuleStatusItem } from '../subscriptions/tenant-module-status.model';
import { TenantModuleViewModel } from './tenant-module-view.model';

@Component({
  selector: 'app-tenant-modules-tab',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './tenant-modules-tab.component.html',
})
export class TenantModulesTabComponent implements OnInit {
  viewModel: TenantModuleViewModel[] = [];
  loading = false;
  error: string | null = null;
  pendingModuleKey: string | null = null;

  private tenantId = '';
  private catalog: CatalogModule[] = [];

  constructor(
    private route: ActivatedRoute,
    private catalogAdmin: CatalogAdminService,
    private subscriptionAdmin: SubscriptionAdminService
  ) {}

  ngOnInit(): void {
    this.tenantId = this.route.parent?.snapshot.paramMap.get('tenantId') ?? '';
    this.loadAll();
  }

  loadAll(): void {
    if (!this.tenantId) {
      this.error = 'Tenant non specificato.';
      return;
    }
    this.loading = true;
    this.error = null;
    forkJoin({
      catalog: this.catalogAdmin.getAll(),
      tenantModules: this.subscriptionAdmin.getTenantModules(this.tenantId),
    }).subscribe({
      next: ({ catalog, tenantModules }) => {
        this.catalog = catalog;
        this.viewModel = this.buildViewModel(catalog, tenantModules);
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Impossibile caricare i moduli. Riprova.';
        this.loading = false;
        console.error('Error loading tenant modules', err);
      },
    });
  }

  grant(moduleKey: string): void {
    this.pendingModuleKey = moduleKey;
    this.subscriptionAdmin.grant(this.tenantId, moduleKey).subscribe({
      next: () => this.reloadTenantModules(),
      error: (err) => {
        this.error = 'Impossibile attivare il modulo. Riprova.';
        this.pendingModuleKey = null;
        console.error('Error granting module', err);
      },
    });
  }

  cancel(moduleKey: string): void {
    this.pendingModuleKey = moduleKey;
    this.subscriptionAdmin.cancel(this.tenantId, moduleKey).subscribe({
      next: () => this.reloadTenantModules(),
      error: (err) => {
        this.error = 'Impossibile disattivare il modulo. Riprova.';
        this.pendingModuleKey = null;
        console.error('Error cancelling module', err);
      },
    });
  }

  private reloadTenantModules(): void {
    this.subscriptionAdmin.getTenantModules(this.tenantId).subscribe({
      next: (tenantModules) => {
        this.viewModel = this.buildViewModel(this.catalog, tenantModules);
        this.pendingModuleKey = null;
      },
      error: (err) => {
        this.error = 'Impossibile aggiornare lo stato dei moduli. Riprova.';
        this.pendingModuleKey = null;
        console.error('Error reloading tenant modules', err);
      },
    });
  }

  private buildViewModel(
    catalog: CatalogModule[],
    tenantModules: TenantModuleStatusItem[]
  ): TenantModuleViewModel[] {
    const statusByKey = new Map(tenantModules.map((m) => [m.moduleKey, m]));
    return catalog.map((module) => {
      const tenantModule = statusByKey.get(module.moduleKey) ?? null;
      return {
        moduleKey: module.moduleKey,
        name: module.name,
        description: module.description,
        type: module.type,
        catalogActivationStatus: module.activationStatus,
        tenantStatus: tenantModule?.status ?? null,
        trialEndsAt: tenantModule?.trialEndsAt ?? null,
      };
    });
  }
}
