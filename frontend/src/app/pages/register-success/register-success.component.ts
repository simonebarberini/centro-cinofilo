import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-register-success',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register-success.component.html'
})
export class RegisterSuccessComponent implements OnInit {
  email = '';
  resendMode = false;
  resendForm: FormGroup;
  resendLoading = false;
  resendSuccess = false;
  resendError = '';

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.resendForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      tenantSlug: ['', Validators.required]
    });

    // Riceve l'email dallo state del router (dopo la registrazione)
    const nav = this.router.getCurrentNavigation();
    if (nav?.extras?.state?.['email']) {
      this.email = nav.extras.state['email'];
    }
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['resend'] === 'true') {
        this.resendMode = true;
      }
    });

    if (this.email) {
      this.resendForm.patchValue({ email: this.email });
    }
  }

  onResend(): void {
    if (this.resendForm.invalid) return;

    this.resendLoading = true;
    this.resendError = '';

    const { email, tenantSlug } = this.resendForm.value;

    this.auth.resendVerification(email, tenantSlug).subscribe({
      next: () => {
        this.resendLoading = false;
        this.resendSuccess = true;
      },
      error: () => {
        this.resendLoading = false;
        this.resendError = 'Errore durante il reinvio. Riprova tra poco.';
      }
    });
  }
}
