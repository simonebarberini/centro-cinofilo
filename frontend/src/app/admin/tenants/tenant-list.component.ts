import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, Subscription, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, switchMap, tap } from 'rxjs/operators';
import { TenantAdminService } from './tenant-admin.service';
import { TenantSummary } from './tenant-summary.model';

@Component({
  selector: 'app-tenant-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tenant-list.component.html',
})
export class TenantListComponent implements OnInit, OnDestroy {
  tenants: TenantSummary[] = [];
  loading = false;
  error: string | null = null;
  searchTerm = '';

  private readonly searchTerm$ = new Subject<string>();
  private subscription?: Subscription;

  constructor(private tenantAdmin: TenantAdminService, private router: Router) {}

  ngOnInit(): void {
    this.subscription = this.searchTerm$
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        tap(() => {
          this.loading = true;
          this.error = null;
        }),
        switchMap((term) =>
          this.tenantAdmin.search(term).pipe(
            catchError((err) => {
              console.error('Error loading tenants', err);
              this.error = 'Impossibile caricare i tenant. Riprova.';
              return of<TenantSummary[]>([]);
            })
          )
        )
      )
      .subscribe((data) => {
        this.tenants = data;
        this.loading = false;
      });

    this.searchTerm$.next('');
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  onSearchChange(): void {
    this.searchTerm$.next(this.searchTerm.trim());
  }

  openTenant(tenant: TenantSummary): void {
    this.router.navigate(['/admin/tenants', tenant.id]);
  }
}
