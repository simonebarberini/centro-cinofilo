import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subject, Subscription, of } from 'rxjs';
import { debounceTime, switchMap, catchError, map } from 'rxjs/operators';

import { BookingsApiService } from '../../core/api/bookings-api.service';
import { CustomersApiService } from '../../core/api/customers-api.service';
import { DogsApiService } from '../../core/api/dogs-api.service';
import { DailyAvailability } from '../../core/models/booking.model';
import { Customer } from '../../core/models/customer.model';
import { Dog } from '../../core/models/dog.model';

type CustomerMode = 'existing' | 'new';
type DogMode = 'existing' | 'new';

@Component({
  selector: 'app-quick-booking',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './quick-booking.component.html',
})
export class QuickBookingComponent implements OnInit, OnDestroy {
  form!: FormGroup;
  saving = false;
  serverError: string | null = null;
  successMessage: string | null = null;

  customerMode: CustomerMode = 'existing';
  dogMode: DogMode = 'existing';

  customers: Customer[] = [];
  dogs: Dog[] = [];
  loadingCustomers = false;
  loadingDogs = false;

  availability: DailyAvailability[] = [];
  checkingAvailability = false;
  availabilityError: string | null = null;
  hasUnavailableDays = false;

  private dateChange$ = new Subject<void>();
  private subs = new Subscription();

  constructor(
    private fb: FormBuilder,
    private bookingsApi: BookingsApiService,
    private customersApi: CustomersApiService,
    private dogsApi: DogsApiService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      // Existing customer
      customerId: [''],
      // New customer
      customerFirstName: [''],
      customerLastName: [''],
      customerEmail: [''],
      customerPhone: [''],
      // Existing dog
      dogId: [''],
      // New dog
      dogName: [''],
      dogBreed: [''],
      dogBirthDate: [''],
      // Booking
      startDate: ['', Validators.required],
      endDate: ['', Validators.required],
      notes: [''],
    });

    this.applyCustomerValidators();
    this.applyDogValidators();
    this.loadCustomers();

    // Cascading: reload dogs when existing customer changes
    this.subs.add(
      this.form.get('customerId')!.valueChanges.subscribe((id) => {
        this.form.get('dogId')!.setValue('');
        this.dogs = [];
        if (id && this.customerMode === 'existing') {
          this.loadDogsForCustomer(id);
        }
      }),
    );

    // Availability check
    this.subs.add(
      this.dateChange$.pipe(
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
          return this.bookingsApi.availability(start, end).pipe(
            catchError(() => {
              this.availabilityError = 'Impossibile verificare disponibilità.';
              return of(null);
            }),
          );
        }),
      ).subscribe((result) => {
        this.checkingAvailability = false;
        if (result) {
          this.availability = result;
          this.hasUnavailableDays = result.some((d) => d.available <= 0);
        }
      }),
    );

    this.subs.add(this.form.get('startDate')!.valueChanges.subscribe(() => this.dateChange$.next()));
    this.subs.add(this.form.get('endDate')!.valueChanges.subscribe(() => this.dateChange$.next()));
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
  }

  setCustomerMode(mode: CustomerMode): void {
    this.customerMode = mode;
    // New customer can only have a new dog
    if (mode === 'new') {
      this.dogMode = 'new';
      this.form.get('customerId')!.setValue('');
      this.dogs = [];
    }
    this.applyCustomerValidators();
    this.applyDogValidators();
  }

  setDogMode(mode: DogMode): void {
    this.dogMode = mode;
    this.applyDogValidators();
  }

  private applyCustomerValidators(): void {
    const existingFields = ['customerId'];
    const newFields = ['customerFirstName', 'customerLastName'];
    const optionalNewFields = ['customerEmail', 'customerPhone'];

    if (this.customerMode === 'existing') {
      existingFields.forEach((f) => this.form.get(f)!.setValidators(Validators.required));
      newFields.forEach((f) => this.form.get(f)!.clearValidators());
    } else {
      existingFields.forEach((f) => this.form.get(f)!.clearValidators());
      newFields.forEach((f) => this.form.get(f)!.setValidators(Validators.required));
    }

    const emailCtrl = this.form.get('customerEmail')!;
    emailCtrl.setValidators(
      this.customerMode === 'new' ? Validators.email : [],
    );

    [...existingFields, ...newFields, ...optionalNewFields].forEach((f) =>
      this.form.get(f)!.updateValueAndValidity({ emitEvent: false }),
    );
  }

  private applyDogValidators(): void {
    const dogId = this.form.get('dogId')!;
    const dogName = this.form.get('dogName')!;

    if (this.dogMode === 'existing') {
      dogId.setValidators(Validators.required);
      dogName.clearValidators();
    } else {
      dogId.clearValidators();
      dogName.setValidators(Validators.required);
    }

    dogId.updateValueAndValidity({ emitEvent: false });
    dogName.updateValueAndValidity({ emitEvent: false });
  }

  private loadCustomers(): void {
    this.loadingCustomers = true;
    this.customersApi.getAll().subscribe({
      next: (customers) => {
        this.customers = customers;
        this.loadingCustomers = false;
      },
      error: () => {
        this.loadingCustomers = false;
        this.serverError = 'Errore nel caricamento dei clienti.';
      },
    });
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
    return !!(ctrl?.invalid && (ctrl.dirty || ctrl.touched));
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving = true;
    this.serverError = null;
    const val = this.form.value;

    // Step 1: resolve customer
    const customer$ = this.customerMode === 'new'
      ? this.customersApi.create({
          firstName: val.customerFirstName,
          lastName: val.customerLastName,
          email: val.customerEmail || '',
          phone: val.customerPhone || '',
          notes: '',
        })
      : of({ id: val.customerId } as Pick<Customer, 'id'>);

    // Step 2: resolve dog → Step 3: create booking
    this.subs.add(
      customer$.pipe(
        switchMap((customer) => {
          const dog$ = this.dogMode === 'new'
            ? this.dogsApi.create({
                customerId: customer.id,
                name: val.dogName,
                breed: val.dogBreed || '',
                birthDate: val.dogBirthDate || null,
                notes: '',
              })
            : of({ id: val.dogId } as Pick<Dog, 'id'>);

          return dog$.pipe(map((dog) => ({ customerId: customer.id, dogId: dog.id })));
        }),
        switchMap(({ customerId, dogId }) =>
          this.bookingsApi.create({
            customerId,
            dogId,
            startDate: val.startDate,
            endDate: val.endDate,
            notes: val.notes || '',
          }),
        ),
      ).subscribe({
        next: () => {
          this.saving = false;
          this.router.navigate(['/bookings']);
        },
        error: (err) => {
          this.saving = false;
          if (err.status === 409) {
            const body = err.error;
            this.serverError = body?.message
              || `Overbooking: il giorno ${body?.date} è pieno (capacità ${body?.capacity}, prenotati ${body?.booked}).`;
          } else if (err.status === 400) {
            this.serverError = err.error?.message || 'Dati non validi. Controlla i campi.';
          } else {
            this.serverError = 'Errore durante il salvataggio. Riprova.';
          }
        },
      }),
    );
  }
}
