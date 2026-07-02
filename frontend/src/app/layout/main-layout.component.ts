import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './main-layout.component.html'
})
export class MainLayoutComponent {
  sidebarOpen = false;

  readonly navItems = [
    { path: '/calendar', label: 'Calendario', icon: '📅' },
    { path: '/quick-booking', label: 'Prenotazione rapida', icon: '⚡' },
    { path: '/customers', label: 'Clienti', icon: '👥' },
    { path: '/dogs', label: 'Cani', icon: '🐾' },
    { path: '/bookings', label: 'Prenotazioni', icon: '📋' },
    { path: '/settings', label: 'Impostazioni', icon: '⚙️' }
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
