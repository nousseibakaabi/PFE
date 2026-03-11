// src/app/pipes/translate.pipe.ts
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

  constructor(
    private translationService: TranslationService,
    private cd: ChangeDetectorRef
  ) {
    this.subscription = this.translationService.currentLang$.subscribe(() => {
      this.value = ''; // Trigger update
      this.cd.markForCheck();
    });
  }

  transform(key: string): string {
    if (!key) return '';
    
    // Only update if value has changed
    const translated = this.translationService.translate(key);
    if (translated !== this.value) {
      this.value = translated;
    }
    return this.value;
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }
}