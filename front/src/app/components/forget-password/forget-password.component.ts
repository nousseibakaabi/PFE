import { Component, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-forget-password',
  templateUrl: './forget-password.component.html',
  styleUrls: ['./forget-password.component.css']
})
export class ForgetPasswordComponent implements OnDestroy {
  forgetPasswordForm: FormGroup;
  message: string = '';
  error: string = '';

  constructor(private formBuilder: FormBuilder, private authService: AuthService, private router: Router) {
    this.forgetPasswordForm = this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]]
    });
    document.body.classList.add('no-navbar');
  }

  ngOnDestroy(): void {
    document.body.classList.remove('no-navbar');
  }

  onSubmit(): void {
    if (this.forgetPasswordForm.invalid) {
      return;
    }

    const email = this.forgetPasswordForm.get('email')?.value;
    this.authService.forgotPassword(email).subscribe({
      next: (response) => {
        // Navigate to success page with email as parameter
        this.router.navigate(['/email-sent'], { 
          queryParams: { email: email } 
        });
      },
      error: (err) => {
        // Display the specific error message from backend
        this.error = err.message || 'Failed to send password reset link. Please try again.';
        this.message = '';
      }
    });
  }
}
