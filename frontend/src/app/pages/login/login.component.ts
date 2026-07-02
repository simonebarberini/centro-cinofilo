import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  form: FormGroup;
  errorMessage = '';
  loading = false;
  showResendLink = false;

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router
  ) {
    this.form = this.fb.group({
      tenantSlug: ['', Validators.required],
      username: ['', Validators.required],
      password: ['', Validators.required]
    });
  }

  onSubmit(): void {
    if (this.form.invalid) return;

    this.loading = true;
    this.errorMessage = '';
    this.showResendLink = false;

    const { tenantSlug, username, password } = this.form.value;

    this.auth.login(tenantSlug, username, password).subscribe({
      next: () => {
        this.router.navigate(['/calendar']);
      },
      error: (err) => {
        this.loading = false;
        if (err.status === 401) {
          this.errorMessage = 'Credenziali non valide';
        } else if (err.status === 403) {
          this.errorMessage = 'Email non verificata. Controlla la tua casella di posta.';
          this.showResendLink = true;
        } else {
          this.errorMessage = 'Errore di connessione al server';
        }
      }
    });
  }
}
