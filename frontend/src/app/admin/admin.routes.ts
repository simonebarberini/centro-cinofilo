import { Routes } from '@angular/router';
import { AdminLayoutComponent } from './admin-layout/admin-layout.component';
import { TenantDetailState } from './tenants/tenant-detail-state';

export const adminRoutes: Routes = [
  {
    path: '',
    component: AdminLayoutComponent,
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./dashboard/dashboard.component').then(m => m.DashboardComponent) },
      { path: 'catalog', loadComponent: () => import('./catalog/catalog-list.component').then(m => m.CatalogListComponent) },
      { path: 'tenants', loadComponent: () => import('./tenants/tenant-list.component').then(m => m.TenantListComponent) },
      {
        path: 'tenants/:tenantId',
        loadComponent: () => import('./tenants/tenant-detail.component').then(m => m.TenantDetailComponent),
        providers: [TenantDetailState],
        children: [
          { path: '', redirectTo: 'info', pathMatch: 'full' },
          { path: 'info', loadComponent: () => import('./tenants/tenant-info-tab.component').then(m => m.TenantInfoTabComponent) },
          { path: 'modules', loadComponent: () => import('./tenants/tenant-modules-tab.component').then(m => m.TenantModulesTabComponent) }
        ]
      }
    ]
  }
];
