import {
  Component,
  EventEmitter,
  Input,
  OnInit,
  OnDestroy,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Subject, Subscription, of, forkJoin } from 'rxjs';
import { debounceTime, switchMap, catchError } from 'rxjs/operators';

import { BookingsApiService } from '../../core/api/bookings-api.service';
import { CustomersApiService } from '../../core/api/customers-api.service';
import { DogsApiService } from '../../core/api/dogs-api.service';
import {
  Booking,
  CreateBookingRequest,
  UpdateBookingRequest,
  DailyAvailability,
} from '../../core/models/booking.model';
import { Customer } from '../../core/models/customer.model';
import { Dog } from '../../core/models/dog.model';

@Component({
  selector: 'app-booking-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './booking-form.component.html',
})
export class BookingFormComponent implements OnInit, OnDestroy {
  @Input() booking: Booking | null = null;
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  form!: FormGroup;
  saving = false;
  serverError: string | null = null;

  customers: Customer[] = [];
  dogs: Dog[] = [];
  loadingDogs = false;

  /** Availability */
  availability: DailyAvailability[] = [];
  checkingAvailability = false;
  availabilityError: string | null = null;
  /** True se almeno un giorno nel range non è disponibile */
  hasUnavailableDays = false;

  private dateChange$ = new Subject<void>();
  private subs = new Subscription();

  constructor(
    private fb: FormBuilder,
    private api: BookingsApiService,
    private customersApi: CustomersApiService,
    private dogsApi: DogsApiService,
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      customerId: [this.booking?.customerId ?? '', Validators.required],
      dogId: [this.booking?.dogId ?? '', Validators.required],
      startDate: [this.booking?.startDate ?? '', Validators.required],
      endDate: [this.booking?.endDate ?? '', Validators.required],
      notes: [this.booking?.notes ?? ''],
    });

    // Carica clienti (e cani se in modifica)
    this.loadInitialData();

    // Cascading: quando cambia il cliente, ricarica cani
    this.subs.add(
      this.form.get('customerId')!.valueChanges.subscribe((customerId) => {
        this.form.get('dogId')!.setValue('');
        this.dogs = [];
        if (customerId) {
          this.loadDogsForCustomer(customerId);
        }
      }),
    );

    // Debounce availability check sulle date
    this.subs.add(
      this.dateChange$
        .pipe(
          debounceTime(400),
          switchMap(() => {
            const start = this.form.get('startDate')!.value;
            const end = this.form.get('endDate')!.value;
            if (!start || !end || start >= end) {
              this.availability = [];
              this.hasUnavailableDays = false;
              this.availabilityError = null;
              return of(null);
            }
            this.checkingAvailability = true;
            this.availabilityError = null;
            return this.api.availability(start, end).pipe(
              catchError(() => {
                this.availabilityError = 'Impossibile verificare disponibilità.';
                return of(null);
              }),
            );
          }),
        )
        .subscribe((result) => {
          this.checkingAvailability = false;
          if (result) {
            this.availability = result;
            this.hasUnavailableDays = result.some((d) => d.available <= 0);
          }
        }),
    );

    // Trigger availability check quando le date cambiano
    this.subs.add(
      this.form.get('startDate')!.valueChanges.subscribe(() => this.dateChange$.next()),
    );
    this.subs.add(
      this.form.get('endDate')!.valueChanges.subscribe(() => this.dateChange$.next()),
    );
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
  }

  private loadInitialData(): void {
    if (this.booking) {
      // In modifica: carica clienti e cani del cliente già selezionato
      forkJoin({
        customers: this.customersApi.getAll(),
        dogs: this.dogsApi.getByCustomer(this.booking.customerId),
      }).subscribe({
        next: ({ customers, dogs }) => {
          this.customers = customers;
          this.dogs = dogs;
          // Trigger availability check con le date attuali
          this.dateChange$.next();
        },
        error: () => {
          this.serverError = 'Errore nel caricamento dei dati.';
        },
      });
    } else {
      this.customersApi.getAll().subscribe({
        next: (customers) => (this.customers = customers),
        error: () => {
          this.serverError = 'Errore nel caricamento dei clienti.';
        },
      });
    }
  }

  private loadDogsForCustomer(customerId: string): void {
    this.loadingDogs = true;
    this.dogsApi.getByCustomer(customerId).subscribe({
      next: (dogs) => {
        this.dogs = dogs;
        this.loadingDogs = false;
      },
      error: () => {
        this.loadingDogs = false;
      },
    });
  }

  isInvalid(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl && ctrl.invalid && (ctrl.dirty || ctrl.touched));
  }

  get isEditMode(): boolean {
    return !!this.booking;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.serverError = null;

    const val = this.form.value;

    if (this.booking) {
      const body: UpdateBookingRequest = {
        startDate: val.startDate,
        endDate: val.endDate,
        notes: val.notes,
      };
      this.api.update(this.booking.id, body).subscribe({
        next: () => {
          this.saving = false;
          this.saved.emit();
        },
        error: (err) => this.handleError(err),
      });
    } else {
      const body: CreateBookingRequest = {
        customerId: val.customerId,
        dogId: val.dogId,
        startDate: val.startDate,
        endDate: val.endDate,
        notes: val.notes,
      };
      this.api.create(body).subscribe({
        next: () => {
          this.saving = false;
          this.saved.emit();
        },
        error: (err) => this.handleError(err),
      });
    }
  }

  cancel(): void {
    this.cancelled.emit();
  }

  private handleError(err: any): void {
    this.saving = false;
    if (err.status === 409) {
      // Overbooking
      const body = err.error;
      this.serverError = body?.message
        || `Overbooking: il giorno ${body?.date} è pieno (capacità ${body?.capacity}, prenotati ${body?.booked}).`;
    } else if (err.status === 400) {
      this.serverError = err.error?.message || 'Dati non validi. Controlla i campi.';
    } else {
      this.serverError = 'Errore durante il salvataggio. Riprova.';
    }
    console.error('Error saving booking', err);
  }
}
