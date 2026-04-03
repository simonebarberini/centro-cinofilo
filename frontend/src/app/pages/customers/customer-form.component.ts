import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CustomersApiService } from '../../core/api/customers-api.service';
import { Customer, CreateCustomerRequest } from '../../core/models/customer.model';

@Component({
  selector: 'app-customer-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <dialog class="modal modal-open">
      <div class="modal-box w-11/12 max-w-lg">
        <h3 class="font-bold text-lg mb-4">{{ customer ? 'Modifica cliente' : 'Nuovo cliente' }}</h3>

        <div *ngIf="serverError" class="alert alert-error mb-4 text-sm">
          <span>{{ serverError }}</span>
        </div>

        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <!-- First Name -->
            <div class="form-control">
              <label class="label"><span class="label-text">Nome *</span></label>
              <input type="text" formControlName="firstName" class="input input-bordered"
                     [class.input-error]="isInvalid('firstName')" placeholder="Mario" />
              <label class="label" *ngIf="isInvalid('firstName')">
                <span class="label-text-alt text-error">Il nome è obbligatorio</span>
              </label>
            </div>

            <!-- Last Name -->
            <div class="form-control">
              <label class="label"><span class="label-text">Cognome</span></label>
              <input type="text" formControlName="lastName" class="input input-bordered"
                     placeholder="Rossi" />
            </div>
          </div>

          <!-- Email -->
          <div class="form-control mt-4">
            <label class="label"><span class="label-text">Email</span></label>
            <input type="email" formControlName="email" class="input input-bordered"
                   [class.input-error]="isInvalid('email')" placeholder="mario.rossi@email.com" />
            <label class="label" *ngIf="isInvalid('email')">
              <span class="label-text-alt text-error">Inserisci un'email valida</span>
            </label>
          </div>

          <!-- Phone -->
          <div class="form-control mt-4">
            <label class="label"><span class="label-text">Telefono</span></label>
            <input type="tel" formControlName="phone" class="input input-bordered"
                   placeholder="+39 333 1234567" />
          </div>

          <!-- Notes -->
          <div class="form-control mt-4">
            <label class="label"><span class="label-text">Note</span></label>
            <textarea formControlName="notes" class="textarea textarea-bordered" rows="3"
                      placeholder="Note aggiuntive..."></textarea>
          </div>

          <!-- Actions -->
          <div class="modal-action">
            <button type="button" class="btn" (click)="cancel()">Annulla</button>
            <button type="submit" class="btn btn-primary" [disabled]="saving">
              <span *ngIf="saving" class="loading loading-spinner loading-sm"></span>
              {{ customer ? 'Salva' : 'Crea' }}
            </button>
          </div>
        </form>
      </div>
      <form method="dialog" class="modal-backdrop" (click)="cancel()">
        <button>chiudi</button>
      </form>
    </dialog>
  `,
})
export class CustomerFormComponent implements OnInit {
  @Input() customer: Customer | null = null;
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  form!: FormGroup;
  saving = false;
  serverError: string | null = null;

  constructor(
    private fb: FormBuilder,
    private api: CustomersApiService,
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      firstName: [this.customer?.firstName ?? '', Validators.required],
      lastName: [this.customer?.lastName ?? ''],
      email: [this.customer?.email ?? '', Validators.email],
      phone: [this.customer?.phone ?? ''],
      notes: [this.customer?.notes ?? ''],
    });
  }

  isInvalid(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl && ctrl.invalid && (ctrl.dirty || ctrl.touched));
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.serverError = null;

    const body: CreateCustomerRequest = this.form.value;

    const req$ = this.customer
      ? this.api.update(this.customer.id, body)
      : this.api.create(body);

    req$.subscribe({
      next: () => {
        this.saving = false;
        this.saved.emit();
      },
      error: (err) => {
        this.saving = false;
        this.serverError = err?.error?.message || 'Errore durante il salvataggio. Riprova.';
        console.error('Error saving customer', err);
      },
    });
  }

  cancel(): void {
    this.cancelled.emit();
  }
}
