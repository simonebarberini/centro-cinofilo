import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { adminGuard } from './admin/guards/admin.guard';
import { MainLayoutComponent } from './layout/main-layout.component';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent) },
  { path: 'register', loadComponent: () => import('./pages/register/register.component').then(m => m.RegisterComponent) },
  { path: 'register-success', loadComponent: () => import('./pages/register-success/register-success.component').then(m => m.RegisterSuccessComponent) },
  { path: 'verify-email', loadComponent: () => import('./pages/verify-email/verify-email.component').then(m => m.VerifyEmailComponent) },
  { path: 'forgot-password', loadComponent: () => import('./pages/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent) },
  { path: 'reset-password', loadComponent: () => import('./pages/reset-password/reset-password.component').then(m => m.ResetPasswordComponent) },
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
      { path: 'quick-booking', loadComponent: () => import('./pages/quick-booking/quick-booking.component').then(m => m.QuickBookingComponent) },
      { path: 'settings', loadComponent: () => import('./pages/settings/settings.component').then(m => m.SettingsComponent) },
      { path: '', redirectTo: 'calendar', pathMatch: 'full' }
    ]
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    loadChildren: () => import('./admin/admin.routes').then(m => m.adminRoutes)
  },
  { path: '**', redirectTo: '' }
];
