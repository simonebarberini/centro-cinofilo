import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DogsApiService } from '../../core/api/dogs-api.service';
import { Dog, CreateDogRequest, UpdateDogRequest } from '../../core/models/dog.model';

@Component({
  selector: 'app-dog-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <dialog class="modal modal-open">
      <div class="modal-box w-11/12 max-w-lg">
        <h3 class="font-bold text-lg mb-4">{{ dog ? 'Modifica cane' : 'Nuovo cane' }}</h3>

        <div *ngIf="serverError" class="alert alert-error mb-4 text-sm">
          <span>{{ serverError }}</span>
        </div>

        <form [formGroup]="form" (ngSubmit)="submit()">
          <!-- Name -->
          <div class="form-control">
            <label class="label"><span class="label-text">Nome *</span></label>
            <input type="text" formControlName="name" class="input input-bordered"
                   [class.input-error]="isInvalid('name')" placeholder="Fido" />
            <label class="label" *ngIf="isInvalid('name')">
              <span class="label-text-alt text-error">Il nome è obbligatorio</span>
            </label>
          </div>

          <!-- Breed -->
          <div class="form-control mt-4">
            <label class="label"><span class="label-text">Razza</span></label>
            <input type="text" formControlName="breed" class="input input-bordered"
                   placeholder="Labrador Retriever" />
          </div>

          <!-- Birth Date -->
          <div class="form-control mt-4">
            <label class="label"><span class="label-text">Data di nascita</span></label>
            <input type="date" formControlName="birthDate" class="input input-bordered" />
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
              {{ dog ? 'Salva' : 'Crea' }}
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
export class DogFormComponent implements OnInit {
  @Input() dog: Dog | null = null;
  @Input() customerId = '';
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  form!: FormGroup;
  saving = false;
  serverError: string | null = null;

  constructor(
    private fb: FormBuilder,
    private api: DogsApiService,
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      name: [this.dog?.name ?? '', Validators.required],
      breed: [this.dog?.breed ?? ''],
      birthDate: [this.dog?.birthDate ?? ''],
      notes: [this.dog?.notes ?? ''],
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

    const val = this.form.value;
    const birthDate = val.birthDate || null;

    if (this.dog) {
      const body: UpdateDogRequest = {
        name: val.name,
        breed: val.breed,
        birthDate,
        notes: val.notes,
      };
      this.api.update(this.dog.id, body).subscribe({
        next: () => { this.saving = false; this.saved.emit(); },
        error: (err) => this.handleError(err),
      });
    } else {
      const body: CreateDogRequest = {
        customerId: this.customerId,
        name: val.name,
        breed: val.breed,
        birthDate,
        notes: val.notes,
      };
      this.api.create(body).subscribe({
        next: () => { this.saving = false; this.saved.emit(); },
        error: (err) => this.handleError(err),
      });
    }
  }

  cancel(): void {
    this.cancelled.emit();
  }

  private handleError(err: any): void {
    this.saving = false;
    this.serverError = err?.error?.message || 'Errore durante il salvataggio. Riprova.';
    console.error('Error saving dog', err);
  }
}
