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
  isLoading: boolean = false;
  isDarkMode = false;

  constructor(private formBuilder: FormBuilder, private authService: AuthService, private router: Router) {
    this.forgetPasswordForm = this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]]
    });
    document.body.classList.add('no-navbar');
      this.checkDarkMode(); 
  }




// Add these methods to your component class
toggleDarkMode(): void {
  this.isDarkMode = !this.isDarkMode;
  this.applyDarkMode();
  localStorage.setItem('darkMode', this.isDarkMode.toString());
}

private checkDarkMode(): void {
  const savedDarkMode = localStorage.getItem('darkMode');
  if (savedDarkMode) {
    this.isDarkMode = savedDarkMode === 'true';
  } else {
    this.isDarkMode = window.matchMedia('(prefers-color-scheme: dark)').matches;
  }
  this.applyDarkMode();
}

private applyDarkMode(): void {
  if (this.isDarkMode) {
    document.documentElement.classList.add('dark');
  } else {
    document.documentElement.classList.remove('dark');
  }
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
