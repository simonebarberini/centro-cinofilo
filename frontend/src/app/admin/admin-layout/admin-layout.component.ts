import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './admin-layout.component.html'
})
export class AdminLayoutComponent {
  sidebarOpen = false;

  readonly navItems = [
    { path: '/admin/dashboard', label: 'Dashboard', icon: '🏠' },
    { path: '/admin/tenants',   label: 'Tenant',    icon: '👥' },
    { path: '/admin/catalog',   label: 'Catalog',   icon: '📦' }
  ];

  constructor(
    private auth: AuthService,
    private router: Router
  ) {}

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
  }

  closeSidebar(): void {
    this.sidebarOpen = false;
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
