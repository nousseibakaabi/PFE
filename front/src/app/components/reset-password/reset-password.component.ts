import { Component, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-reset-password',
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.css']
})
export class ResetPasswordComponent implements OnDestroy {
  resetPasswordForm: FormGroup;
  message: string = '';
  error: string = '';
  token: string = '';

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.resetPasswordForm = this.formBuilder.group({
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]]
    });

    this.route.queryParams.subscribe(params => {
      this.token = params['token'] || '';
    });

    document.body.classList.add('no-navbar');
  }

  ngOnDestroy(): void {
    document.body.classList.remove('no-navbar');
  }

  showPasswordMismatchError(): boolean {
    const password = this.resetPasswordForm.get('password')?.value;
    const confirmPassword = this.resetPasswordForm.get('confirmPassword')?.value;
    const confirmPasswordTouched = this.resetPasswordForm.get('confirmPassword')?.touched;

    // Check if all values are defined
    if (password === undefined || confirmPassword === undefined || confirmPasswordTouched === undefined) {
      return false;
    }

    return password !== confirmPassword && confirmPasswordTouched && confirmPassword !== '';
  }

  onSubmit(): void {
    // Check if form is invalid
    if (this.resetPasswordForm.invalid) {
      // Mark all fields as touched to show validation errors
      this.resetPasswordForm.markAllAsTouched();
      return;
    }

    // Check if passwords match
    if (this.showPasswordMismatchError()) {
      this.error = 'Passwords do not match.';
      this.message = '';
      return;
    }

    const password = this.resetPasswordForm.get('password')?.value;
    const confirmPassword = this.resetPasswordForm.get('confirmPassword')?.value;

    // Validate passwords match one more time
    if (password !== confirmPassword) {
      this.error = 'Passwords do not match.';
      this.message = '';
      return;
    }

    // Clear previous messages
    this.error = '';
    this.message = '';

    this.authService.resetPassword(this.token, password, confirmPassword).subscribe({
      next: (response) => {
        this.message = 'Password has been reset successfully.';
        this.error = '';
        setTimeout(() => {
          this.router.navigate(['/']);
        }, 2000);
      },
      error: (err) => {
        this.error = 'Failed to reset password. Please try again.';
        this.message = '';
      }
    });
  }
}