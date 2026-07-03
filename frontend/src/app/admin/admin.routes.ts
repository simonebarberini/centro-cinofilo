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
      // P6 — { path: 'tenants', ... }
      // P6 — { path: 'tenants/:tenantId', ... }
    ]
  }
];
