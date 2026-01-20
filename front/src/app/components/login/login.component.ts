import { Component, OnInit, AfterViewInit, ElementRef, ViewChild ,ViewEncapsulation ,HostListener } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { first } from 'rxjs/operators';
import { AuthService } from '../../services/auth.service';
import { gsap } from 'gsap';
import { MorphSVGPlugin } from 'gsap/MorphSVGPlugin';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
    encapsulation: ViewEncapsulation.None  // Add this line

})
export class LoginComponent implements OnInit, AfterViewInit {
  loginForm!: FormGroup;
  loading = false;
  submitted = false;
  error = '';
  returnUrl: string = '';
  isDarkMode: boolean = false;


  // Avatar Animation ViewChild References
  @ViewChild('loginEmail') loginEmail!: ElementRef<HTMLInputElement>;
  @ViewChild('loginPassword') loginPassword!: ElementRef<HTMLInputElement>;
  @ViewChild('loginEmailLabel') loginEmailLabel!: ElementRef<HTMLLabelElement>;
  @ViewChild('loginPasswordLabel') loginPasswordLabel!: ElementRef<HTMLLabelElement>;
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
  @ViewChild('tooth') tooth!: ElementRef<SVGPathElement>;
  @ViewChild('tongue') tongue!: ElementRef<SVGGElement>;
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
  private curEmailIndex: number = 0;
  private screenCenter: number = 0;
  private svgCoords: { x: number; y: number } = { x: 0, y: 0 };
  private emailCoords: { x: number; y: number } = { x: 0, y: 0 };
  private emailScrollMax: number = 0;
  private chinMin: number = 0.5;
  private dFromC: number = 0;
  private mouthStatus: string = "small";
  private blinking: any;
  private eyeScale: number = 1;
  private eyesCovered: boolean = false;
  private showPasswordClicked: boolean = false;
  
  private eyeLCoords: { x: number; y: number } = { x: 0, y: 0 };
  private eyeRCoords: { x: number; y: number } = { x: 0, y: 0 };
  private noseCoords: { x: number; y: number } = { x: 0, y: 0 };
  private mouthCoords: { x: number; y: number } = { x: 0, y: 0 };
  
  private eyeLAngle: number = 0;
  private eyeLX: number = 0;
  private eyeLY: number = 0;
  private eyeRAngle: number = 0;
  private eyeRX: number = 0;
  private eyeRY: number = 0;
  private noseAngle: number = 0;
  private noseX: number = 0;
  private noseY: number = 0;
  private mouthAngle: number = 0;
  private mouthX: number = 0;
  private mouthY: number = 0;
  private mouthR: number = 0;
  private chinX: number = 0;
  private chinY: number = 0;
  private chinS: number = 0;
  private faceX: number = 0;
  private faceY: number = 0;
  private faceSkew: number = 0;
  private eyebrowSkew: number = 0;
  private outerEarX: number = 0;
  private outerEarY: number = 0;
  private hairX: number = 0;
  private hairS: number = 0;

  // Public properties
  passwordVisible: boolean = false;
  isEyeActive: boolean = false;

  constructor(
    private formBuilder: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {
    gsap.registerPlugin(MorphSVGPlugin);
    
    if (this.authService.currentUserValue) {
      this.router.navigate(['/dashboard']);
    }
  }

  private checkDarkMode(): void {
    // Check localStorage first
    const savedDarkMode = localStorage.getItem('darkMode');
    if (savedDarkMode) {
      this.isDarkMode = savedDarkMode === 'true';
    } else {
      // Check system preference
      this.isDarkMode = window.matchMedia('(prefers-color-scheme: dark)').matches;
    }
    this.applyDarkMode();
  }
  
  // Add this method to apply dark mode
  private applyDarkMode(): void {
    if (this.isDarkMode) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }

  @HostListener('window:resize', ['$event'])
  @HostListener('window:storage', ['$event'])
  onDarkModeChange(event?: Event): void {
    // Recheck dark mode on storage changes (when changed from other components)
    if (event?.type === 'storage') {
      this.checkDarkMode();
    }
  }

  ngOnInit(): void {
    this.loginForm = this.formBuilder.group({
      usernameOrEmail: ['', Validators.required],
      password: ['', Validators.required]
    });

    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/dashboard';
    
    // Check dark mode on initialization
    this.checkDarkMode();
  }

  // Add this method to your LoginComponent class
toggleDarkMode(): void {
  this.isDarkMode = !this.isDarkMode;
  this.applyDarkMode();
  localStorage.setItem('darkMode', this.isDarkMode.toString());
}

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.initAvatarAnimations();
    }, 0);
  }

  get f() { return this.loginForm.controls; }

  // ===================== PASSWORD TOGGLE - INSTANT RESPONSE =====================
  onPasswordToggleClick(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    
    // INSTANT toggle - no delays
    this.passwordVisible = !this.passwordVisible;
    
    // INSTANT update input type
    this.loginPassword.nativeElement.type = this.passwordVisible ? 'text' : 'password';
    
    // INSTANT eye activation
    this.isEyeActive = this.passwordVisible;
    
    // INSTANT avatar response
    if (this.passwordVisible) {
      this.spreadFingersInstantly();
      this.makeAvatarPeekInstantly();
    } else {
      this.closeFingersInstantly();
      this.stopAvatarPeekInstantly();
    }
    
    // INSTANT focus back - NO DELAY
    this.loginPassword.nativeElement.focus();
  }

  onPasswordToggleMouseDown(): void {
    // INSTANT response on mouse down
    this.showPasswordClicked = true;
    this.isEyeActive = true;
  }

  onPasswordToggleMouseUp(): void {
    this.showPasswordClicked = false;
  }

  // ===================== INSTANT AVATAR ANIMATION METHODS =====================
  private spreadFingersInstantly(): void {
    // INSTANT finger spread - duration 0.1s
    gsap.to(this.twoFingers.nativeElement, {
      duration: 0.1,
      transformOrigin: "bottom left",
      rotation: 30,
      x: -9,
      y: -2,
      ease: "none" // Instant response
    });
  }

  private closeFingersInstantly(): void {
    // INSTANT finger close - duration 0.1s
    gsap.to(this.twoFingers.nativeElement, {
      duration: 0.1,
      transformOrigin: "bottom left",
      rotation: 0,
      x: 0,
      y: 0,
      ease: "none" // Instant response
    });
  }

  private makeAvatarPeekInstantly(): void {
    // INSTANT peek - duration 0.05s (almost instant)
    gsap.killTweensOf([this.eyeL.nativeElement, this.eyeR.nativeElement, this.mouth.nativeElement]);
    
    gsap.to([this.eyeL.nativeElement, this.eyeR.nativeElement], {
      duration: 0.05,
      scaleY: 0.6,
      y: -4,
      ease: "none",
      transformOrigin: "center center"
    });
    
    gsap.to(this.mouth.nativeElement, {
      duration: 0.05,
      y: -6,
      ease: "none"
    });
    
    gsap.to(this.eyebrow.nativeElement, {
      duration: 0.05,
      y: -2,
      ease: "none"
    });
  }

  private stopAvatarPeekInstantly(): void {
    // INSTANT stop peek - duration 0.05s (almost instant)
    gsap.killTweensOf([this.eyeL.nativeElement, this.eyeR.nativeElement, this.mouth.nativeElement, this.eyebrow.nativeElement]);
    
    gsap.to([this.eyeL.nativeElement, this.eyeR.nativeElement], {
      duration: 0.05,
      scaleY: this.eyeScale,
      y: 0,
      ease: "none"
    });
    
    gsap.to(this.mouth.nativeElement, {
      duration: 0.05,
      y: 0,
      ease: "none"
    });
    
    gsap.to(this.eyebrow.nativeElement, {
      duration: 0.05,
      y: 0,
      ease: "none"
    });
  }

  // ===================== PASSWORD FIELD HANDLERS =====================
  onPasswordFocus(): void {
    this.activeElement = "password";
    if (!this.eyesCovered) {
      this.coverEyes();
    }
  }

  onPasswordBlur(): void {
    this.activeElement = null;
    // INSTANT uncover - no setTimeout
    if (this.activeElement !== "toggle" && this.activeElement !== "password") {
      this.uncoverEyes();
      
      // INSTANT stop peek if password not visible
      if (!this.passwordVisible) {
        this.stopAvatarPeekInstantly();
      }
    }
  }

  onPasswordInput(event: Event): void {
    const password = this.loginPassword.nativeElement;
    const value = password.value;
    this.loginForm.patchValue({ password: value });
  }

  // ===================== EMAIL FIELD HANDLERS =====================
  onEmailInput(event: Event): void {
    const email = this.loginEmail.nativeElement;
    const value = email.value;
    this.curEmailIndex = value.length;
    this.calculateFaceMove();
    
    this.loginForm.patchValue({ usernameOrEmail: value });
    
    if (this.curEmailIndex > 0) {
      if (this.mouthStatus === "small") {
        this.mouthStatus = "medium";
        gsap.to([this.mouthBG.nativeElement, this.mouthOutline.nativeElement, this.mouthMaskPath.nativeElement], {
          duration: 0.3,
          morphSVG: this.mouthMediumBG.nativeElement,
          ease: "power2.out"
        });
        gsap.to([this.eyeL.nativeElement, this.eyeR.nativeElement], { 
          duration: 0.3, 
          scaleX: 0.85, 
          scaleY: 0.85, 
          ease: "power2.out" 
        });
        this.eyeScale = 0.85;
      }
      
      if (value.includes("@")) {
        this.mouthStatus = "large";
        gsap.to([this.mouthBG.nativeElement, this.mouthOutline.nativeElement, this.mouthMaskPath.nativeElement], {
          duration: 0.3,
          morphSVG: this.mouthLargeBG.nativeElement,
          ease: "power2.out"
        });
        gsap.to([this.eyeL.nativeElement, this.eyeR.nativeElement], {
          duration: 0.3,
          scaleX: 0.65,
          scaleY: 0.65,
          ease: "power2.out",
          transformOrigin: "center center"
        });
        this.eyeScale = 0.65;
      } else {
        this.mouthStatus = "medium";
        gsap.to([this.mouthBG.nativeElement, this.mouthOutline.nativeElement, this.mouthMaskPath.nativeElement], {
          duration: 0.3,
          morphSVG: this.mouthMediumBG.nativeElement,
          ease: "power2.out"
        });
        gsap.to([this.eyeL.nativeElement, this.eyeR.nativeElement], { 
          duration: 0.3, 
          scaleX: 0.85, 
          scaleY: 0.85, 
          ease: "power2.out" 
        });
        this.eyeScale = 0.85;
      }
    } else {
      this.mouthStatus = "small";
      gsap.to([this.mouthBG.nativeElement, this.mouthOutline.nativeElement, this.mouthMaskPath.nativeElement], {
        duration: 0.3,
        morphSVG: this.mouthSmallBG.nativeElement,
        ease: "power2.out"
      });
      gsap.to([this.eyeL.nativeElement, this.eyeR.nativeElement], { 
        duration: 0.3, 
        scaleX: 1, 
        scaleY: 1, 
        ease: "power2.out" 
      });
      this.eyeScale = 1;
    }
  }

  onEmailFocus(event: Event): void {
    this.activeElement = "email";
    const target = event.target as HTMLElement;
    target.parentElement?.classList.add("focusWithText");
    this.stopBlinking();
    this.onEmailInput(event);
  }

  onEmailBlur(event: Event): void {
    this.activeElement = null;
    // INSTANT response - no setTimeout
    const target = event.target as HTMLInputElement;
    if (this.activeElement !== "email") {
      if (target.value === "") {
        target.parentElement?.classList.remove("focusWithText");
      }
      this.startBlinking();
      this.resetFace();
    }
  }

  onEmailLabelClick(): void {
    this.activeElement = "email";
    this.loginEmail.nativeElement.focus();
  }

  // ===================== AVATAR ANIMATION CORE METHODS =====================
  private getPosition(el: HTMLElement | SVGElement | null): { x: number; y: number } {
    if (!el) return { x: 0, y: 0 };
    
    let xPos = 0;
    let yPos = 0;
    let currentEl: any = el;

    while (currentEl) {
      if (currentEl.tagName === "BODY") {
        const xScroll = currentEl.scrollLeft || document.documentElement.scrollLeft;
        const yScroll = currentEl.scrollTop || document.documentElement.scrollTop;
        xPos += (currentEl.offsetLeft - xScroll + currentEl.clientLeft);
        yPos += (currentEl.offsetTop - yScroll + currentEl.clientTop);
      } else {
        xPos += (currentEl.offsetLeft - currentEl.scrollLeft + currentEl.clientLeft);
        yPos += (currentEl.offsetTop - currentEl.scrollTop + currentEl.clientTop);
      }
      currentEl = currentEl.offsetParent;
    }
    
    return { x: xPos, y: yPos };
  }

  private calculateFaceMove(): void {
    const email = this.loginEmail.nativeElement;
    const caretPos = email.selectionEnd || email.value.length;
    
    const div = document.createElement('div');
    const span = document.createElement('span');
    const copyStyle = getComputedStyle(email);
    
    Array.from(copyStyle).forEach((prop: any) => {
      (div.style as any)[prop] = (copyStyle as any)[prop];
    });
    
    div.style.position = 'absolute';
    document.body.appendChild(div);
    div.textContent = email.value.substr(0, caretPos);
    span.textContent = email.value.substr(caretPos) || '.';
    div.appendChild(span);
    
    const caretCoords = this.getPosition(span);
    
    if (email.scrollWidth <= this.emailScrollMax) {
      this.dFromC = this.screenCenter - (caretCoords.x + this.emailCoords.x);
      this.eyeLAngle = this.getAngle(this.eyeLCoords.x, this.eyeLCoords.y, this.emailCoords.x + caretCoords.x, this.emailCoords.y + 25);
      this.eyeRAngle = this.getAngle(this.eyeRCoords.x, this.eyeRCoords.y, this.emailCoords.x + caretCoords.x, this.emailCoords.y + 25);
      this.noseAngle = this.getAngle(this.noseCoords.x, this.noseCoords.y, this.emailCoords.x + caretCoords.x, this.emailCoords.y + 25);
      this.mouthAngle = this.getAngle(this.mouthCoords.x, this.mouthCoords.y, this.emailCoords.x + caretCoords.x, this.emailCoords.y + 25);
    } else {
      this.eyeLAngle = this.getAngle(this.eyeLCoords.x, this.eyeLCoords.y, this.emailCoords.x + this.emailScrollMax, this.emailCoords.y + 25);
      this.eyeRAngle = this.getAngle(this.eyeRCoords.x, this.eyeRCoords.y, this.emailCoords.x + this.emailScrollMax, this.emailCoords.y + 25);
      this.noseAngle = this.getAngle(this.noseCoords.x, this.noseCoords.y, this.emailCoords.x + this.emailScrollMax, this.emailCoords.y + 25);
      this.mouthAngle = this.getAngle(this.mouthCoords.x, this.mouthCoords.y, this.emailCoords.x + this.emailScrollMax, this.emailCoords.y + 25);
    }
    
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
      if (this.chinS < this.chinMin) {
        this.chinS = this.chinMin;
      }
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
    
    document.body.removeChild(div);
  }

  private getAngle(x1: number, y1: number, x2: number, y2: number): number {
    return Math.atan2(y1 - y2, x1 - x2);
  }

  private coverEyes(): void {
    gsap.killTweensOf([this.armL.nativeElement, this.armR.nativeElement]);
    gsap.set([this.armL.nativeElement, this.armR.nativeElement], { visibility: "visible" });
    gsap.to(this.armL.nativeElement, { duration: 0.2, x: -93, y: 10, rotation: 0, ease: "power2.out" });
    gsap.to(this.armR.nativeElement, { duration: 0.2, x: -93, y: 10, rotation: 0, ease: "power2.out", delay: 0.05 });
    gsap.to(this.bodyBG.nativeElement, { duration: 0.2, morphSVG: this.bodyBGchanged.nativeElement, ease: "power2.out" });
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
    gsap.to(this.bodyBG.nativeElement, { duration: 0.2, morphSVG: this.bodyBG.nativeElement, ease: "power2.out" });
    this.eyesCovered = false;
  }

  private resetFace(): void {
    gsap.to([this.eyeL.nativeElement, this.eyeR.nativeElement], { duration: 0.3, x: 0, y: 0, ease: "power2.out" });
    gsap.to(this.nose.nativeElement, { duration: 0.3, x: 0, y: 0, scaleX: 1, scaleY: 1, ease: "power2.out" });
    gsap.to(this.mouth.nativeElement, { duration: 0.3, x: 0, y: 0, rotation: 0, ease: "power2.out" });
    gsap.to(this.chin.nativeElement, { duration: 0.3, x: 0, y: 0, scaleY: 1, ease: "power2.out" });
    gsap.to([this.face.nativeElement, this.eyebrow.nativeElement], { duration: 0.3, x: 0, y: 0, skewX: 0, ease: "power2.out" });
    gsap.to([
      this.outerEarL.nativeElement,
      this.outerEarR.nativeElement,
      this.earHairL.nativeElement,
      this.earHairR.nativeElement,
      this.hair.nativeElement
    ], { duration: 0.3, x: 0, y: 0, scaleY: 1, ease: "power2.out" });
  }

  private startBlinking(delay?: number): void {
    if (!delay) {
      delay = this.getRandomInt(12);
    }
    
    this.blinking = gsap.to([this.eyeL.nativeElement, this.eyeR.nativeElement], {
      duration: 0.05,
      delay: delay,
      scaleY: 0,
      yoyo: true,
      repeat: 1,
      transformOrigin: "center center",
      onComplete: () => {
        this.startBlinking(12);
      }
    });
  }

  private stopBlinking(): void {
    if (this.blinking) {
      this.blinking.kill();
      this.blinking = null;
    }
    gsap.set([this.eyeL.nativeElement, this.eyeR.nativeElement], { scaleY: this.eyeScale });
  }

  private getRandomInt(max: number): number {
    return Math.floor(Math.random() * Math.floor(max));
  }

  private isMobileDevice(): boolean {
    const userAgent = navigator.userAgent || navigator.vendor || (window as any).opera;
    return /android|iphone|ipad|ipod|blackberry|iemobile|opera mini/i.test(userAgent.toLowerCase());
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
    
    if (this.isMobileDevice()) {
      this.passwordVisible = true;
      this.isEyeActive = true;
      gsap.set(this.twoFingers.nativeElement, {
        transformOrigin: "bottom left",
        rotation: 30,
        x: -9,
        y: -2,
        ease: "none"
      });
      this.makeAvatarPeekInstantly();
    }
  }

  // ===================== LOGIN FORM METHODS =====================


  onSubmit(): void {
  this.submitted = true;

  if (this.loginForm.invalid) {
    return;
  }

  this.loading = true;
  this.authService.login({
    usernameOrEmail: this.f['usernameOrEmail'].value,
    password: this.f['password'].value
  })
  .pipe(first())
  .subscribe({
    next: (response) => {
      this.celebrateLogin();
      
      const user = this.authService.currentUserValue;
      if (user && this.authService.isAdmin()) {
        this.router.navigate(['/admin']);
      } else {
        this.router.navigate([this.returnUrl]);
      }
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
        this.error = '⚠️ WARNING: One more failed attempt will lock your account for 15 minutes!';
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
      this.showLoginError();
    }
  });
}

  forgotPassword(): void {
    this.router.navigate(['/forget-password']);
  }

  private celebrateLogin(): void {
    gsap.to(this.mouth.nativeElement, { 
      duration: 0.2, 
      y: -10, 
      ease: "power2.out" 
    });
    
    gsap.to([this.eyeL.nativeElement, this.eyeR.nativeElement], { 
      duration: 0.2, 
      scaleY: 0.7,
      ease: "power2.out" 
    });
    
    gsap.to(this.armL.nativeElement, {
      duration: 0.15,
      rotation: 130,
      yoyo: true,
      repeat: 3,
      ease: "power2.inOut"
    });
    
    gsap.to(this.armR.nativeElement, {
      duration: 0.15,
      rotation: -130,
      yoyo: true,
      repeat: 3,
      delay: 0.05,
      ease: "power2.inOut"
    });
  }

  private showLoginError(): void {
    gsap.to(this.mouth.nativeElement, { 
      duration: 0.2, 
      y: 5, 
      rotation: -5,
      ease: "power2.out" 
    });
    
    gsap.to([this.eyeL.nativeElement, this.eyeR.nativeElement], { 
      duration: 0.2, 
      y: 5,
      ease: "power2.out" 
    });
    
    // INSTANT reset - no delay
    this.resetFace();
  }
}