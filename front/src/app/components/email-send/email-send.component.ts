import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-email-send',
  templateUrl: './email-send.component.html',
  styleUrls: ['./email-send.component.css']
})
export class EmailSendComponent implements OnInit, OnDestroy {
  email: string = '';
  isResending = false;
  resendSuccess = false;
  resendError = '';
  isDarkMode = false;


  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {
    document.body.classList.add('no-navbar');
    this.checkDarkMode(); 

  }


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

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.email = params['email'] || '';
    });
  }

  ngOnDestroy(): void {
    document.body.classList.remove('no-navbar');
  }

  openEmailClient(): void {
    window.open('mailto:', '_blank');
  }

  resendEmail(): void {
    if (!this.email || this.isResending) return;

    this.isResending = true;
    this.resendSuccess = false;
    this.resendError = '';

    this.authService.forgotPassword(this.email).subscribe({
      next: (response) => {
        this.isResending = false;
        this.resendSuccess = true;
        setTimeout(() => {
          this.resendSuccess = false;
        }, 3000);
      },
      error: (error) => {
        this.isResending = false;
        this.resendError = 'Failed to resend email. Please try again.';
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/forgot-password']);
  }
}