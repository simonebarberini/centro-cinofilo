import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { MainLayoutComponent } from './layout/main-layout.component';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent) },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'calendar', loadComponent: () => import('./pages/calendar/calendar.component').then(m => m.CalendarComponent) },
      { path: 'customers', loadComponent: () => import('./pages/customers/customers.component').then(m => m.CustomersComponent) },
      { path: 'customers/:customerId/dogs', loadComponent: () => import('./pages/dogs/dogs.component').then(m => m.DogsComponent) },
      { path: 'dogs', loadComponent: () => import('./pages/dogs/dogs.component').then(m => m.DogsComponent) },
      { path: 'bookings', loadComponent: () => import('./pages/bookings/bookings.component').then(m => m.BookingsComponent) },
      { path: 'settings', loadComponent: () => import('./pages/settings/settings.component').then(m => m.SettingsComponent) },
      { path: '', redirectTo: 'calendar', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: '' }
];
