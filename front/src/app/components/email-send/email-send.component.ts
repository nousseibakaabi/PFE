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

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {
    document.body.classList.add('no-navbar');
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