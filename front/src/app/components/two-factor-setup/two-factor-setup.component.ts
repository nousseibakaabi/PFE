import { Component, OnInit, EventEmitter, Output, ViewChildren, QueryList, ElementRef  } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TwoFactorService } from '../../services/two-factor.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-two-factor-setup',
  templateUrl: './two-factor-setup.component.html',
  styleUrls: ['./two-factor-setup.component.css']
})
export class TwoFactorSetupComponent implements OnInit {
  @Output() twoFactorStatusChanged = new EventEmitter<boolean>();
  @ViewChildren('codeInput') inputs!: QueryList<ElementRef>;
  @ViewChildren('disableCodeInput') disableInputs!: QueryList<ElementRef>; 
  
  verifyForm!: FormGroup;
  disableForm!: FormGroup;
  

  codeDigits: string[] = ['', '', '', '', '', '']; 
  disableCodeDigits: string[] = ['', '', '', '', '', '']; 
  
  
  loading = false;
  verifying = false;
  disabling = false;
  
  error = '';
  success = '';
  
  isTwoFactorEnabled = false;
  qrCodeUrl = '';
  secret = '';
  backupCodes: string[] = [];
  
  showSetup = false;
  showVerify = false;
  showBackupCodes = false;
  showDisable = false;
  
  
  constructor(
    private formBuilder: FormBuilder,
    private twoFactorService: TwoFactorService,
    private authService: AuthService
  ) {}
  
  ngOnInit(): void {
    this.initForms();
    this.loadStatus();
  }
  
  initForms(): void {
    this.verifyForm = this.formBuilder.group({
      code: ['', [Validators.required, Validators.pattern('^[0-9]{6}$')]]
    });
    this.disableForm = this.formBuilder.group({
      code: ['', [Validators.required, Validators.pattern('^[0-9]{6}$')]]
    });
  }
  
  loadStatus(): void {
    this.twoFactorService.getTwoFactorStatus().subscribe({
      next: (status) => {
        this.isTwoFactorEnabled = status.enabled;
      },
      error: (error) => {
        this.error = 'Failed to load 2FA status';
      }
    });
  }
  
  startSetup(): void {
    this.loading = true;
    this.error = '';
    
    this.twoFactorService.setupTwoFactor().subscribe({
      next: (response) => {
        this.qrCodeUrl = response.qrCodeUrl;
        this.secret = response.secret;
        this.backupCodes = response.backupCodes;
        this.showSetup = true;
        this.showVerify = true;
        this.loading = false;
        // Reset code digits when opening modal
        this.resetCodeInputs();
        // Focus sur le premier champ après un court délai
        setTimeout(() => {
          const firstInput = document.querySelector('#code-input-0') as HTMLInputElement;
          if (firstInput) {
            firstInput.focus();
          }
        }, 100);
      },
      error: (error) => {
        this.error = error.message || 'Failed to setup 2FA';
        this.loading = false;
      }
    });
  }
  
resetCodeInputs(): void {
  this.codeDigits = ['', '', '', '', '', ''];
  this.verifyForm.patchValue({ code: '' });

  this.inputs.forEach(input => input.nativeElement.value = '');
}



trackByIndex(index: number): number {
  return index;
}



onKeyDown(index: number, event: KeyboardEvent): void {
  const inputs = this.inputs.toArray();
  const currentInput = inputs[index].nativeElement;

  if (['Tab', 'Shift', 'ArrowLeft', 'ArrowRight'].includes(event.key)) {
    return;
  }

  event.preventDefault();

  // ✅ numbers
  if (/^\d$/.test(event.key)) {
    this.codeDigits[index] = event.key;
    this.codeDigits = [...this.codeDigits]; // ✅ HERE

    currentInput.value = event.key;

    if (index < 5) {
      inputs[index + 1].nativeElement.focus();
    }

    this.verifyForm.patchValue({ code: this.codeDigits.join('') });
    return;
  }

  // ✅ backspace
  if (event.key === 'Backspace') {
    if (this.codeDigits[index]) {
      this.codeDigits[index] = '';
      currentInput.value = '';
    } else if (index > 0) {
      const prevInput = inputs[index - 1].nativeElement;
      this.codeDigits[index - 1] = '';
      prevInput.value = '';
      prevInput.focus();
    }

    this.codeDigits = [...this.codeDigits]; // ✅ ALSO HERE
    this.verifyForm.patchValue({ code: this.codeDigits.join('') });
  }
}

  
  onPaste(event: ClipboardEvent): void {
    event.preventDefault();
    const pastedData = event.clipboardData?.getData('text');
    if (pastedData && /^\d+$/.test(pastedData)) {
      const digits = pastedData.split('').slice(0, 6);
      // Réinitialiser et remplir les digits
    
      // Mettre à jour le form control
      const fullCode = this.codeDigits.join('');
      this.verifyForm.patchValue({ code: fullCode });
      
      // Focus sur le dernier champ rempli
      let lastFilledIndex = -1;
      for (let i = 0; i < 6; i++) {
        if (this.codeDigits[i]) {
          lastFilledIndex = i;
        }
      }
      
      if (lastFilledIndex < 5) {
        const nextInput = document.querySelector(`#code-input-${lastFilledIndex + 1}`) as HTMLInputElement;
        if (nextInput) {
          nextInput.focus();
        }
      } else if (fullCode.length === 6) {
        setTimeout(() => {
          this.verifyAndEnable();
        }, 100);
      }
    }
  }
  
  verifyAndEnable(): void {
    const fullCode = this.codeDigits.join('');
    if (fullCode.length !== 6) {
      this.error = 'Veuillez entrer les 6 chiffres';
      return;
    }
    
    this.verifying = true;
    this.error = '';
    
    this.twoFactorService.verifyAndEnable(fullCode).subscribe({
      next: (response) => {
        this.isTwoFactorEnabled = true;
        this.showVerify = false;
        this.showBackupCodes = true;
        this.success = response.message || '2FA activée avec succès !';
        this.verifying = false;
        
        this.twoFactorStatusChanged.emit(true);
        this.authService.refreshUser().subscribe();
      },
      error: (error) => {
        this.error = error.message || 'Code de vérification invalide';
        this.verifying = false;
        // Réinitialiser les champs en cas d'erreur
        this.resetCodeInputs();
        // Focus sur le premier champ
        setTimeout(() => {
          const firstInput = document.querySelector('#code-input-0') as HTMLInputElement;
          if (firstInput) {
            firstInput.focus();
          }
        }, 100);
      }
    });
  }
  
disableTwoFactor(): void {
  const fullCode = this.disableCodeDigits.join('');
  if (fullCode.length !== 6) {
    this.error = 'Veuillez entrer les 6 chiffres';
    return;
  }
  
  this.disabling = true;
  this.error = '';
  
  this.twoFactorService.disableTwoFactor(fullCode).subscribe({
    next: (response) => {
      this.isTwoFactorEnabled = false;
      this.showDisable = false;
      this.success = response.message || '2FA désactivée avec succès !';
      this.disabling = false;
      this.disableCodeDigits = ['', '', '', '', '', '']; // Reset digits
      
      this.twoFactorStatusChanged.emit(false);
      this.authService.refreshUser().subscribe();
    },
    error: (error) => {
      this.error = error.message || 'Code de vérification invalide';
      this.disabling = false;
      // Reset digits on error
      this.disableCodeDigits = ['', '', '', '', '', ''];
      setTimeout(() => {
        const firstInput = document.querySelector('#disable-code-input-0') as HTMLInputElement;
        if (firstInput) {
          firstInput.focus();
        }
      }, 100);
    }
  });
}


  
  copyBackupCodes(): void {
    const codesText = this.backupCodes.join('\n');
    navigator.clipboard.writeText(codesText).then(() => {
      this.success = 'Backup codes copied to clipboard!';
      setTimeout(() => {
        this.success = '';
      }, 3000);
    });
  }
  
  downloadBackupCodes(): void {
    const codesText = this.backupCodes.join('\n');
    const blob = new Blob([codesText], { type: 'text/plain' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'backup-codes.txt';
    a.click();
    window.URL.revokeObjectURL(url);
  }
  
  closeSetup(): void {
    this.showSetup = false;
    this.showVerify = false;
    this.showBackupCodes = false;
    this.resetCodeInputs();
  }
  
closeDisable(): void {
  this.showDisable = false;
  this.disableForm.reset();
  this.disableCodeDigits = ['', '', '', '', '', '']; 
  this.error = ''; 
  this.disabling = false;
}


  onDisableKeyDown(index: number, event: KeyboardEvent): void {
  const inputs = this.disableInputs.toArray();
  const currentInput = inputs[index].nativeElement;

  if (['Tab', 'Shift', 'ArrowLeft', 'ArrowRight'].includes(event.key)) {
    return;
  }

  event.preventDefault();

  // numbers
  if (/^\d$/.test(event.key)) {
    this.disableCodeDigits[index] = event.key;
    this.disableCodeDigits = [...this.disableCodeDigits];

    currentInput.value = event.key;

    if (index < 5) {
      inputs[index + 1].nativeElement.focus();
    }

    const fullCode = this.disableCodeDigits.join('');
    this.disableForm.patchValue({ code: fullCode });
    return;
  }

  // backspace
  if (event.key === 'Backspace') {
    if (this.disableCodeDigits[index]) {
      this.disableCodeDigits[index] = '';
      currentInput.value = '';
    } else if (index > 0) {
      const prevInput = inputs[index - 1].nativeElement;
      this.disableCodeDigits[index - 1] = '';
      prevInput.value = '';
      prevInput.focus();
    }

    this.disableCodeDigits = [...this.disableCodeDigits];
    const fullCode = this.disableCodeDigits.join('');
    this.disableForm.patchValue({ code: fullCode });
  }
}
}