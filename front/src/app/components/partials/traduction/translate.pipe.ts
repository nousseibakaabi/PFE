// translate.pipe.ts
import { Pipe, PipeTransform, ChangeDetectorRef, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { TranslationService } from './translation.service';

@Pipe({
  name: 'translate',
  pure: false // This makes the pipe update when language changes
})
export class TranslatePipe implements PipeTransform, OnDestroy {
  private value: string = '';
  private subscription: Subscription;
  private currentLang: string = '';

  constructor(
    private translationService: TranslationService,
    private cd: ChangeDetectorRef
  ) {
    // Subscribe to language changes
    this.subscription = this.translationService.currentLang$.subscribe((lang) => {
      this.currentLang = lang;
      this.value = ''; // Trigger update
      this.cd.markForCheck();
    });
  }

  transform(key: string): string {
    if (!key) return '';
    
    // Get the translated value
    const translated = this.translationService.translate(key);
    
    // Update if changed
    if (translated !== this.value) {
      this.value = translated;
    }
    return this.value;
  }

  ngOnDestroy() {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }
}