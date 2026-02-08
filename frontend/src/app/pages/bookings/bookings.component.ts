import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { BookingsApiService } from '../../core/api/bookings-api.service';
import { CustomersApiService } from '../../core/api/customers-api.service';
import { DogsApiService } from '../../core/api/dogs-api.service';
import { Booking } from '../../core/models/booking.model';
import { Customer } from '../../core/models/customer.model';
import { Dog } from '../../core/models/dog.model';
import { BookingFormComponent } from './booking-form.component';

@Component({
  selector: 'app-bookings',
  standalone: true,
  imports: [CommonModule, FormsModule, BookingFormComponent],
  templateUrl: './bookings.component.html',
})
export class BookingsComponent implements OnInit {
  bookings: Booking[] = [];
  filteredBookings: Booking[] = [];
  loading = false;
  error: string | null = null;

  customerMap: Record<string, Customer> = {};
  dogMap: Record<string, Dog> = {};

  /** Filtro: mostra solo prenotazioni attive (CONFIRMED) */
  onlyActive = true;

  formOpen = false;
  editingBooking: Booking | null = null;

  cancellingBooking: Booking | null = null;
  cancelling = false;

  @ViewChild('cancelDialog') cancelDialog!: ElementRef<HTMLDialogElement>;

  constructor(
    private api: BookingsApiService,
    private customersApi: CustomersApiService,
    private dogsApi: DogsApiService,
  ) {}

  ngOnInit(): void {
    this.loadBookings();
  }

  loadBookings(): void {
    this.loading = true;
    this.error = null;

    forkJoin({
      bookings: this.api.getAll(),
      customers: this.customersApi.getAll(),
      dogs: this.dogsApi.getAll(),
    }).subscribe({
      next: ({ bookings, customers, dogs }) => {
        this.customerMap = {};
        for (const c of customers) {
          this.customerMap[c.id] = c;
        }
        this.dogMap = {};
        for (const d of dogs) {
          this.dogMap[d.id] = d;
        }
        this.bookings = bookings;
        this.applyFilter();
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Impossibile caricare le prenotazioni. Riprova.';
        this.loading = false;
        console.error('Error loading bookings', err);
      },
    });
  }

  applyFilter(): void {
    if (this.onlyActive) {
      this.filteredBookings = this.bookings.filter(b => b.status === 'CONFIRMED');
    } else {
      this.filteredBookings = [...this.bookings];
    }
    // ordina per data inizio discendente
    this.filteredBookings.sort((a, b) => b.startDate.localeCompare(a.startDate));
  }

  toggleFilter(): void {
    this.onlyActive = !this.onlyActive;
    this.applyFilter();
  }

  customerName(b: Booking): string {
    const c = this.customerMap[b.customerId];
    return c ? `${c.firstName} ${c.lastName}` : '—';
  }

  dogName(b: Booking): string {
    const d = this.dogMap[b.dogId];
    return d ? d.name : '—';
  }

  openCreate(): void {
    this.editingBooking = null;
    this.formOpen = true;
  }

  openEdit(booking: Booking): void {
    this.editingBooking = booking;
    this.formOpen = true;
  }

  closeForm(): void {
    this.formOpen = false;
    this.editingBooking = null;
  }

  onSaved(): void {
    this.closeForm();
    this.loadBookings();
  }

  confirmCancel(booking: Booking): void {
    this.cancellingBooking = booking;
    this.cancelDialog.nativeElement.showModal();
  }

  executeCancel(): void {
    if (!this.cancellingBooking) return;
    this.cancelling = true;
    this.api.cancel(this.cancellingBooking.id).subscribe({
      next: () => {
        this.cancelling = false;
        this.cancelDialog.nativeElement.close();
        this.cancellingBooking = null;
        this.loadBookings();
      },
      error: (err) => {
        this.cancelling = false;
        console.error('Error cancelling booking', err);
      },
    });
  }

  closeCancelDialog(): void {
    this.cancelDialog.nativeElement.close();
    this.cancellingBooking = null;
  }
}
