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
  isLoading: boolean = false;
  isDarkMode = false;


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
        this.message = 'Mot de passe réinitialisé avec succès ! Redirection vers la page de connexion...';
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


  // Add these to your ResetPasswordComponent class

getPasswordStrength(): number {
  const password = this.resetPasswordForm.get('password')?.value || '';
  if (!password) return 0;
  
  let strength = 0;
  if (password.length >= 6) strength += 25;
  if (password.length >= 8) strength += 15;
  if (/[A-Z]/.test(password)) strength += 20;
  if (/[a-z]/.test(password)) strength += 20;
  if (/[0-9]/.test(password)) strength += 10;
  if (/[^A-Za-z0-9]/.test(password)) strength += 10;
  
  return Math.min(100, strength);
}

getPasswordStrengthText(): string {
  const strength = this.getPasswordStrength();
  if (strength < 30) return 'Faible';
  if (strength < 60) return 'Moyen';
  if (strength < 80) return 'Bon';
  return 'Fort';
}

getPasswordStrengthColor(): string {
  const strength = this.getPasswordStrength();
  if (strength < 30) return 'text-red-500';
  if (strength < 60) return 'text-orange-500';
  if (strength < 80) return 'text-blue-500';
  return 'text-green-500';
}

getPasswordStrengthBarClass(): string {
  const strength = this.getPasswordStrength();
  if (strength < 30) return 'bg-red-500';
  if (strength < 60) return 'bg-orange-500';
  if (strength < 80) return 'bg-blue-500';
  return 'bg-green-500';
}


}