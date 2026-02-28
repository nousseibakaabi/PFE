import { Router, ActivatedRoute } from '@angular/router';
import { first } from 'rxjs/operators';
import { AuthService } from '../../services/auth.service';
import { gsap } from 'gsap';



import { Component, OnInit, AfterViewInit, ElementRef, ViewChild, ViewEncapsulation, ChangeDetectionStrategy, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';

class MockAuthService {
  currentUserValue = null;
  login() {
    return { pipe: () => ({ subscribe: (callbacks: { next: (val: boolean) => void }) => callbacks.next(true) }) };
  }
  isAdmin() { return false; }
  isCommercial() { return false; }
  isChefProjet() { return false; }
  isDecideur() { return false; }
}
@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
  encapsulation: ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush

})
export class LoginComponent implements OnInit, AfterViewInit {
  loginForm!: FormGroup;
  loading = false;
  submitted = false;
  error = '';
  isDarkMode = false;
  passwordVisible = false;
  isEyeActive = false;
  successMessage = '';



  // Avatar Animation ViewChild References
  @ViewChild('loginEmail') loginEmail!: ElementRef<HTMLInputElement>;
  @ViewChild('loginPassword') loginPassword!: ElementRef<HTMLInputElement>;
  @ViewChild('mySVG') mySVG!: ElementRef<SVGElement>;
  @ViewChild('twoFingers') twoFingers!: ElementRef<SVGGElement>;
  @ViewChild('armL') armL!: ElementRef<SVGGElement>;
  @ViewChild('armR') armR!: ElementRef<SVGGElement>;
  @ViewChild('eyeL') eyeL!: ElementRef<SVGGElement>;
  @ViewChild('eyeR') eyeR!: ElementRef<SVGGElement>;
  @ViewChild('nose') nose!: ElementRef<SVGPathElement>;
  @ViewChild('mouth') mouth!: ElementRef<SVGGElement>;
  @ViewChild('mouthBG') mouthBG!: ElementRef<SVGPathElement>;
  @ViewChild('mouthSmallBG') mouthSmallBG!: ElementRef<SVGPathElement>;
  @ViewChild('mouthMediumBG') mouthMediumBG!: ElementRef<SVGPathElement>;
  @ViewChild('mouthLargeBG') mouthLargeBG!: ElementRef<SVGPathElement>;
  @ViewChild('mouthMaskPath') mouthMaskPath!: ElementRef<SVGPathElement>;
  @ViewChild('mouthOutline') mouthOutline!: ElementRef<SVGPathElement>;
  @ViewChild('chin') chin!: ElementRef<SVGPathElement>;
  @ViewChild('face') face!: ElementRef<SVGPathElement>;
  @ViewChild('eyebrow') eyebrow!: ElementRef<SVGGElement>;
  @ViewChild('outerEarL') outerEarL!: ElementRef<SVGGElement>;
  @ViewChild('outerEarR') outerEarR!: ElementRef<SVGGElement>;
  @ViewChild('earHairL') earHairL!: ElementRef<SVGGElement>;
  @ViewChild('earHairR') earHairR!: ElementRef<SVGGElement>;
  @ViewChild('hair') hair!: ElementRef<SVGPathElement>;
  @ViewChild('bodyBG') bodyBG!: ElementRef<SVGPathElement>;
  @ViewChild('bodyBGchanged') bodyBGchanged!: ElementRef<SVGPathElement>;

  // Avatar Animation Variables
  private activeElement: string | null = null;
  private curEmailIndex = 0;
  private screenCenter = 0;
  private svgCoords: { x: number; y: number } = { x: 0, y: 0 };
  private emailCoords: { x: number; y: number } = { x: 0, y: 0 };
  private emailScrollMax = 0;
  private chinMin = 0.5;
  private dFromC = 0;
  private mouthStatus = "small";
  private blinking: gsap.core.Tween | null = null;
  private eyeScale = 1;
  private eyesCovered = false;
  private showPasswordClicked = false;
  
  private eyeLCoords: { x: number; y: number } = { x: 0, y: 0 };
  private eyeRCoords: { x: number; y: number } = { x: 0, y: 0 };
  private noseCoords: { x: number; y: number } = { x: 0, y: 0 };
  private mouthCoords: { x: number; y: number } = { x: 0, y: 0 };
  
  private eyeLAngle = 0;
  private eyeLX = 0;
  private eyeLY = 0;
  private eyeRAngle = 0;
  private eyeRX = 0;
  private eyeRY = 0;
  private noseAngle = 0;
  private noseX = 0;
  private noseY = 0;
  private mouthAngle = 0;
  private mouthX = 0;
  private mouthY = 0;
  private mouthR = 0;
  private chinX = 0;
  private chinY = 0;
  private chinS = 0;
  private faceX = 0;
  private faceY = 0;
  private faceSkew = 0;
  private eyebrowSkew = 0;
  private outerEarX = 0;
  private outerEarY = 0;
  private hairX = 0;
  private hairS = 0;


  
  returnUrl: string = '/';


  // Properly inject dependencies
  private authService = inject(AuthService); // Use real AuthService, not mock
  private formBuilder = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  // Add getter for form controls
  get f() { return this.loginForm.controls; }

  ngOnInit(): void {
    this.loginForm = this.formBuilder.group({
      usernameOrEmail: ['', Validators.required],
      password: ['', Validators.required]
        });
    
    // Get return url from route parameters or default to '/'
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/';
    
    this.checkDarkMode();
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.initAvatarAnimations();
    }, 100);
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
    this.cdr.markForCheck();
  }

  toggleDarkMode(): void {
    this.isDarkMode = !this.isDarkMode;
    this.applyDarkMode();
    localStorage.setItem('darkMode', this.isDarkMode.toString());
  }

  // ===================== PASSWORD TOGGLE =====================
  onPasswordToggleClick(_event: Event): void {
    _event.preventDefault();
    _event.stopPropagation();
    this.passwordVisible = !this.passwordVisible;
    this.loginPassword.nativeElement.type = this.passwordVisible ? 'text' : 'password';
    this.isEyeActive = this.passwordVisible;
    
    if (this.passwordVisible) {
      this.spreadFingersInstantly();
      this.makeAvatarPeekInstantly();
    } else {
      this.closeFingersInstantly();
      this.stopAvatarPeekInstantly();
    }
    this.loginPassword.nativeElement.focus();
    this.cdr.markForCheck();
  }

  onPasswordToggleMouseDown(): void {
    this.showPasswordClicked = true;
    this.isEyeActive = true;
    this.cdr.markForCheck();
  }

  onPasswordToggleMouseUp(): void {
    this.showPasswordClicked = false;
    this.cdr.markForCheck();
  }

  private spreadFingersInstantly(): void {
    gsap.to(this.twoFingers.nativeElement, {
      duration: 0.1,
      transformOrigin: "bottom left",
      rotation: 30,
      x: -9,
      y: -2,
      ease: "none"
    });
  }

  private closeFingersInstantly(): void {
    gsap.to(this.twoFingers.nativeElement, {
      duration: 0.1,
      transformOrigin: "bottom left",
      rotation: 0,
      x: 0,
      y: 0,
      ease: "none"
    });
  }

  private makeAvatarPeekInstantly(): void {
    gsap.killTweensOf([this.eyeL.nativeElement, this.eyeR.nativeElement, this.mouth.nativeElement]);
    gsap.to([this.eyeL.nativeElement, this.eyeR.nativeElement], {
      duration: 0.05,
      scaleY: 0.6,
      y: -4,
      ease: "none",
      transformOrigin: "center center"
    });
    gsap.to(this.mouth.nativeElement, { duration: 0.05, y: -6, ease: "none" });
    gsap.to(this.eyebrow.nativeElement, { duration: 0.05, y: -2, ease: "none" });
  }

  private stopAvatarPeekInstantly(): void {
    gsap.killTweensOf([this.eyeL.nativeElement, this.eyeR.nativeElement, this.mouth.nativeElement, this.eyebrow.nativeElement]);
    gsap.to([this.eyeL.nativeElement, this.eyeR.nativeElement], {
      duration: 0.05,
      scaleY: this.eyeScale,
      y: 0,
      ease: "none"
    });
    gsap.to(this.mouth.nativeElement, { duration: 0.05, y: 0, ease: "none" });
    gsap.to(this.eyebrow.nativeElement, { duration: 0.05, y: 0, ease: "none" });
  }

  onPasswordFocus(): void {
    this.activeElement = "password";
    if (!this.eyesCovered) {
      this.coverEyes();
    }
  }

  onPasswordBlur(): void {
    this.activeElement = null;
    if (this.activeElement !== "toggle" && this.activeElement !== "password") {
      this.uncoverEyes();
      if (!this.passwordVisible) {
        this.stopAvatarPeekInstantly();
      }
    }
  }

  onPasswordInput(): void {
    const value = this.loginPassword.nativeElement.value;
    this.loginForm.patchValue({ password: value });
  }

  onEmailInput(): void {
    const email = this.loginEmail.nativeElement;
    const value = email.value;
    this.curEmailIndex = value.length;
    this.calculateFaceMove();
    this.loginForm.patchValue({ usernameOrEmail: value });
    
    if (this.curEmailIndex > 0) {
      if (this.mouthStatus === "small") {
        this.mouthStatus = "medium";
        this.eyeScale = 0.85;
        gsap.to([this.eyeL.nativeElement, this.eyeR.nativeElement], { duration: 0.3, scaleX: 0.85, scaleY: 0.85, ease: "power2.out" });
      }
      if (value.includes("@")) {
        this.mouthStatus = "large";
        this.eyeScale = 0.65;
        gsap.to([this.eyeL.nativeElement, this.eyeR.nativeElement], { duration: 0.3, scaleX: 0.65, scaleY: 0.65, ease: "power2.out", transformOrigin: "center center" });
      } else {
        this.mouthStatus = "medium";
        this.eyeScale = 0.85;
        gsap.to([this.eyeL.nativeElement, this.eyeR.nativeElement], { duration: 0.3, scaleX: 0.85, scaleY: 0.85, ease: "power2.out" });
      }
    } else {
      this.mouthStatus = "small";
      this.eyeScale = 1;
      gsap.to([this.eyeL.nativeElement, this.eyeR.nativeElement], { duration: 0.3, scaleX: 1, scaleY: 1, ease: "power2.out" });
    }
  }

  onEmailFocus(): void {
    this.activeElement = "email";
    this.stopBlinking();
    this.onEmailInput();
  }

  onEmailBlur(): void {
    this.activeElement = null;
    if (this.activeElement !== "email") {
      this.startBlinking();
      this.resetFace();
    }
  }

  private getPosition(el: HTMLElement | SVGElement | null): { x: number; y: number } {
    if (!el) return { x: 0, y: 0 };
    const rect = el.getBoundingClientRect();
    return { x: rect.left + window.scrollX, y: rect.top + window.scrollY };
  }

  private calculateFaceMove(): void {
    const email = this.loginEmail.nativeElement;
    const caretPos = email.selectionEnd || email.value.length;
    const caretX = (caretPos / 20) * 100; // Simplified caret position estimation
    
    this.dFromC = this.screenCenter - (this.emailCoords.x + caretX);
    this.eyeLAngle = this.getAngle(this.eyeLCoords.x, this.eyeLCoords.y, this.emailCoords.x + caretX, this.emailCoords.y + 25);
    this.eyeRAngle = this.getAngle(this.eyeRCoords.x, this.eyeRCoords.y, this.emailCoords.x + caretX, this.emailCoords.y + 25);
    this.noseAngle = this.getAngle(this.noseCoords.x, this.noseCoords.y, this.emailCoords.x + caretX, this.emailCoords.y + 25);
    this.mouthAngle = this.getAngle(this.mouthCoords.x, this.mouthCoords.y, this.emailCoords.x + caretX, this.emailCoords.y + 25);
    
    this.eyeLX = Math.cos(this.eyeLAngle) * 20;
    this.eyeLY = Math.sin(this.eyeLAngle) * 10;
    this.eyeRX = Math.cos(this.eyeRAngle) * 20;
    this.eyeRY = Math.sin(this.eyeRAngle) * 10;
    this.noseX = Math.cos(this.noseAngle) * 23;
    this.noseY = Math.sin(this.noseAngle) * 10;
    this.mouthX = Math.cos(this.mouthAngle) * 23;
    this.mouthY = Math.sin(this.mouthAngle) * 10;
    this.mouthR = Math.cos(this.mouthAngle) * 6;
    this.chinX = this.mouthX * 0.8;
    this.chinY = this.mouthY * 0.5;
    this.chinS = 1 - ((this.dFromC * 0.15) / 100);
    
    if (this.chinS > 1) {
      this.chinS = 1 - (this.chinS - 1);
      if (this.chinS < this.chinMin) this.chinS = this.chinMin;
    }
    
    this.faceX = this.mouthX * 0.3;
    this.faceY = this.mouthY * 0.4;
    this.faceSkew = Math.cos(this.mouthAngle) * 5;
    this.eyebrowSkew = Math.cos(this.mouthAngle) * 25;
    this.outerEarX = Math.cos(this.mouthAngle) * 4;
    this.outerEarY = Math.cos(this.mouthAngle) * 5;
    this.hairX = Math.cos(this.mouthAngle) * 6;
    this.hairS = 1.2;
    
    gsap.to(this.eyeL.nativeElement, { duration: 0.3, x: -this.eyeLX, y: -this.eyeLY, ease: "power2.out" });
    gsap.to(this.eyeR.nativeElement, { duration: 0.3, x: -this.eyeRX, y: -this.eyeRY, ease: "power2.out" });
    gsap.to(this.nose.nativeElement, { duration: 0.3, x: -this.noseX, y: -this.noseY, rotation: this.mouthR, transformOrigin: "center center", ease: "power2.out" });
    gsap.to(this.mouth.nativeElement, { duration: 0.3, x: -this.mouthX, y: -this.mouthY, rotation: this.mouthR, transformOrigin: "center center", ease: "power2.out" });
    gsap.to(this.chin.nativeElement, { duration: 0.3, x: -this.chinX, y: -this.chinY, scaleY: this.chinS, ease: "power2.out" });
    gsap.to(this.face.nativeElement, { duration: 0.3, x: -this.faceX, y: -this.faceY, skewX: -this.faceSkew, transformOrigin: "center top", ease: "power2.out" });
    gsap.to(this.eyebrow.nativeElement, { duration: 0.3, x: -this.faceX, y: -this.faceY, skewX: -this.eyebrowSkew, transformOrigin: "center top", ease: "power2.out" });
    gsap.to(this.outerEarL.nativeElement, { duration: 0.3, x: this.outerEarX, y: -this.outerEarY, ease: "power2.out" });
    gsap.to(this.outerEarR.nativeElement, { duration: 0.3, x: this.outerEarX, y: this.outerEarY, ease: "power2.out" });
    gsap.to(this.earHairL.nativeElement, { duration: 0.3, x: -this.outerEarX, y: -this.outerEarY, ease: "power2.out" });
    gsap.to(this.earHairR.nativeElement, { duration: 0.3, x: -this.outerEarX, y: this.outerEarY, ease: "power2.out" });
    gsap.to(this.hair.nativeElement, { duration: 0.3, x: this.hairX, scaleY: this.hairS, transformOrigin: "center bottom", ease: "power2.out" });
  }

  private getAngle(x1: number, y1: number, x2: number, y2: number): number {
    return Math.atan2(y1 - y2, x1 - x2);
  }

  private coverEyes(): void {
    gsap.killTweensOf([this.armL.nativeElement, this.armR.nativeElement]);
    gsap.set([this.armL.nativeElement, this.armR.nativeElement], { visibility: "visible" });
    gsap.to(this.armL.nativeElement, { duration: 0.2, x: -93, y: 10, rotation: 0, ease: "power2.out" });
    gsap.to(this.armR.nativeElement, { duration: 0.2, x: -93, y: 10, rotation: 0, ease: "power2.out", delay: 0.05 });
    this.eyesCovered = true;
  }

  private uncoverEyes(): void {
    gsap.killTweensOf([this.armL.nativeElement, this.armR.nativeElement]);
    gsap.to(this.armL.nativeElement, { duration: 0.5, y: 220, ease: "power2.out" });
    gsap.to(this.armL.nativeElement, { duration: 0.5, rotation: 105, ease: "power2.out", delay: 0.05 });
    gsap.to(this.armR.nativeElement, { duration: 0.5, y: 220, ease: "power2.out" });
    gsap.to(this.armR.nativeElement, {
      duration: 0.5,
      rotation: -105,
      ease: "power2.out",
      delay: 0.05,
      onComplete: () => {
        gsap.set([this.armL.nativeElement, this.armR.nativeElement], { visibility: "hidden" });
      }
    });
    this.eyesCovered = false;
  }

  private resetFace(): void {
    gsap.to([this.eyeL.nativeElement, this.eyeR.nativeElement], { duration: 0.3, x: 0, y: 0, ease: "power2.out" });
    gsap.to(this.nose.nativeElement, { duration: 0.3, x: 0, y: 0, scaleX: 1, scaleY: 1, ease: "power2.out" });
    gsap.to(this.mouth.nativeElement, { duration: 0.3, x: 0, y: 0, rotation: 0, ease: "power2.out" });
    gsap.to(this.chin.nativeElement, { duration: 0.3, x: 0, y: 0, scaleY: 1, ease: "power2.out" });
    gsap.to([this.face.nativeElement, this.eyebrow.nativeElement], { duration: 0.3, x: 0, y: 0, skewX: 0, ease: "power2.out" });
    gsap.to([this.outerEarL.nativeElement, this.outerEarR.nativeElement, this.earHairL.nativeElement, this.earHairR.nativeElement, this.hair.nativeElement], { duration: 0.3, x: 0, y: 0, scaleY: 1, ease: "power2.out" });
  }

  private startBlinking(delay?: number): void {
    if (!delay) delay = Math.floor(Math.random() * 12);
    this.blinking = gsap.to([this.eyeL.nativeElement, this.eyeR.nativeElement], {
      duration: 0.05,
      delay: delay,
      scaleY: 0,
      yoyo: true,
      repeat: 1,
      transformOrigin: "center center",
      onComplete: () => this.startBlinking(12)
    });
  }

  private stopBlinking(): void {
    if (this.blinking) {
      this.blinking.kill();
      this.blinking = null;
    }
    gsap.set([this.eyeL.nativeElement, this.eyeR.nativeElement], { scaleY: this.eyeScale });
  }

  private initAvatarAnimations(): void {
    this.svgCoords = this.getPosition(this.mySVG.nativeElement);
    this.emailCoords = this.getPosition(this.loginEmail.nativeElement);
    this.screenCenter = this.svgCoords.x + (this.mySVG.nativeElement.getBoundingClientRect().width / 2);
    
    this.eyeLCoords = { x: this.svgCoords.x + 84, y: this.svgCoords.y + 76 };
    this.eyeRCoords = { x: this.svgCoords.x + 113, y: this.svgCoords.y + 76 };
    this.noseCoords = { x: this.svgCoords.x + 97, y: this.svgCoords.y + 81 };
    this.mouthCoords = { x: this.svgCoords.x + 100, y: this.svgCoords.y + 100 };
    
    gsap.set(this.armL.nativeElement, { x: -93, y: 220, rotation: 105, transformOrigin: "top left" });
    gsap.set(this.armR.nativeElement, { x: -93, y: 220, rotation: -105, transformOrigin: "top right" });
    gsap.set(this.mouth.nativeElement, { transformOrigin: "center center" });
    
    this.startBlinking(5);
    this.emailScrollMax = this.loginEmail.nativeElement.scrollWidth;
  }

onSubmit(): void {
    this.submitted = true;
    this.error = ''; // Clear previous errors

    // Stop if form is invalid
    if (this.loginForm.invalid) {
      return;
    }

    this.loading = true;
    this.cdr.markForCheck();

    this.authService.login({
      usernameOrEmail: this.f['usernameOrEmail'].value,
      password: this.f['password'].value
        })
    .pipe(first())
    .subscribe({
      next: (response) => {
        this.celebrateLogin();
        
        // Navigate based on user role
        if (this.authService.isAdmin()) {
          this.router.navigate(['/admin']);
        } else if (this.authService.isCommercial()) {
          this.router.navigate(['/commercial']);
        } else if (this.authService.isChefProjet()) {
          this.router.navigate(['/chef']);
        } else if (this.authService.isDecideur()) {
          this.router.navigate(['/decideur']);
        } else {
          this.router.navigate([this.returnUrl]);
        }
        
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (error: any) => {
        console.log('Login error details:', error);
        
        const backendError = error.error || error;
        const remainingAttempts = backendError.remainingAttempts;
        const errorType = backendError.error;
        
        // Clear any previous error
        this.error = '';
        
        // 1. Account temporarily locked
        if (errorType === 'AccountTemporarilyLocked') {
          if (backendError.lockUntil) {
            const lockUntilDate = new Date(backendError.lockUntil);
            const now = new Date();
            const minutesRemaining = Math.ceil((lockUntilDate.getTime() - now.getTime()) / (1000 * 60));
            
            if (minutesRemaining > 0) {
              this.error = `Account locked for ${minutesRemaining} minute(s) due to too many failed attempts`;
            } else {
              this.error = 'Account was temporarily locked. Please try again.';
            }
          } else {
            this.error = 'Account locked for 15 minutes due to too many failed attempts';
          }
        }
        // 2. Last attempt warning
        else if (errorType === 'LastAttemptWarning' || backendError.isLastAttempt === true) {
          this.error = 'WARNING: One more failed attempt will lock your account for 15 minutes!';
        }
        // 3. Normal failed attempts
        else if (errorType === 'BadCredentials' || errorType === 'InvalidCredentials') {
          if (remainingAttempts === 2) {
            this.error = 'Invalid credentials. 2 attempts remaining';
          } else if (remainingAttempts === 1) {
            // Should show warning instead
            this.error = 'Invalid credentials. 1 attempt remaining';
          } else {
            this.error = 'Invalid credentials';
          }
        }
        // 4. Account locked by admin
        else if (errorType === 'AccountLocked' && backendError.lockType === 'ADMIN') {
          this.error = 'Account locked by administrator. Contact admin to unlock.';
        }
        // 5. Use userMessage if available
        else if (backendError.userMessage) {
          this.error = backendError.userMessage;
        }
        // 6. Any other error
        else {
          this.error = backendError.message || 'Login failed. Please try again.';
        }
        
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }




  private celebrateLogin(): void {
    gsap.to(this.mouth.nativeElement, { duration: 0.2, y: -10, ease: "power2.out" });
    gsap.to([this.eyeL.nativeElement, this.eyeR.nativeElement], { duration: 0.2, scaleY: 0.7, ease: "power2.out" });
    gsap.to(this.armL.nativeElement, { duration: 0.15, rotation: 130, yoyo: true, repeat: 3, ease: "power2.inOut" });
    gsap.to(this.armR.nativeElement, { duration: 0.15, rotation: -130, yoyo: true, repeat: 3, delay: 0.05, ease: "power2.inOut" });
  }
}