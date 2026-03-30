import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

function passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
  const password = control.get('password');
  const confirmPassword = control.get('confirmPassword');
  if (!password || !confirmPassword) return null;
  return password.value === confirmPassword.value ? null : { passwordMismatch: true };
}

function slugValidator(control: AbstractControl): ValidationErrors | null {
  const value = control.value as string;
  if (!value) return null;
  return /^[a-z0-9-]+$/.test(value) ? null : { invalidSlug: true };
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html'
})
export class RegisterComponent {
  form: FormGroup;
  errorMessage = '';
  loading = false;

  readonly tenantTypes = [
    'Centro addestramento',
    'Allevamento',
    'Pensione',
    'Veterinario',
    'Altro'
  ];

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router
  ) {
    this.form = this.fb.group({
      tenantName: ['', [Validators.required, Validators.maxLength(255)]],
      tenantType: ['', Validators.required],
      tenantSlug: ['', [Validators.required, Validators.maxLength(100), slugValidator]],
      username: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required]
    }, { validators: passwordMatchValidator });

    // Auto-genera lo slug dal nome del centro
    this.form.get('tenantName')!.valueChanges.subscribe((name: string) => {
      if (this.form.get('tenantSlug')!.pristine) {
        const slug = name.toLowerCase().replace(/\s+/g, '-').replace(/[^a-z0-9-]/g, '');
        this.form.get('tenantSlug')!.setValue(slug, { emitEvent: false });
      }
    });
  }

  get slugField() { return this.form.get('tenantSlug')!; }
  get emailField() { return this.form.get('email')!; }
  get passwordField() { return this.form.get('password')!; }

  onSubmit(): void {
    if (this.form.invalid) return;

    this.loading = true;
    this.errorMessage = '';

    const { tenantName, tenantType, tenantSlug, username, email, password } = this.form.value;

    this.auth.register({ tenantName, tenantType, tenantSlug, username, email, password }).subscribe({
      next: (res) => {
        this.router.navigate(['/register-success'], { state: { email: res.email } });
      },
      error: (err) => {
        this.loading = false;
        if (err.status === 409) {
          this.errorMessage = 'Slug già in uso: scegli un identificativo diverso per il tuo centro';
        } else if (err.status === 400) {
          this.errorMessage = err.error?.message ?? 'Dati non validi';
        } else {
          this.errorMessage = 'Errore di connessione al server';
        }
      }
    });
  }
}
