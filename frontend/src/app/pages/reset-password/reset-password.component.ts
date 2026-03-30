import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

function passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
  const pw = control.get('newPassword');
  const confirm = control.get('confirmPassword');
  if (!pw || !confirm) return null;
  return pw.value === confirm.value ? null : { passwordMismatch: true };
}

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.component.html'
})
export class ResetPasswordComponent implements OnInit {
  form: FormGroup;
  loading = false;
  success = false;
  errorMessage = '';
  token = '';
  invalidToken = false;

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.form = this.fb.group({
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required]
    }, { validators: passwordMatchValidator });
  }

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParams['token'] ?? '';
    if (!this.token) {
      this.invalidToken = true;
    }
  }

  get passwordField() { return this.form.get('newPassword')!; }

  onSubmit(): void {
    if (this.form.invalid || !this.token) return;

    this.loading = true;
    this.errorMessage = '';

    this.auth.resetPassword(this.token, this.form.value.newPassword).subscribe({
      next: () => {
        this.loading = false;
        this.success = true;
        setTimeout(() => this.router.navigate(['/login']), 2500);
      },
      error: (err) => {
        this.loading = false;
        if (err.status === 401) {
          this.errorMessage = 'Il link è scaduto o non valido. Richiedine uno nuovo.';
          this.invalidToken = true;
        } else {
          this.errorMessage = 'Errore durante il reset. Riprova.';
        }
      }
    });
  }
}
