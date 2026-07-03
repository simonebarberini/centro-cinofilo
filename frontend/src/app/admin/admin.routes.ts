import { Routes } from '@angular/router';
import { AdminLayoutComponent } from './admin-layout/admin-layout.component';

export const adminRoutes: Routes = [
  {
    path: '',
    component: AdminLayoutComponent,
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./dashboard/dashboard.component').then(m => m.DashboardComponent) },
      { path: 'catalog', loadComponent: () => import('./catalog/catalog-list.component').then(m => m.CatalogListComponent) },
      { path: 'tenants', loadComponent: () => import('./tenants/tenant-list.component').then(m => m.TenantListComponent) },
      // P7 — { path: 'tenants/:tenantId', ... }
    ]
  }
];
