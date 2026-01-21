import { Pipe, PipeTransform } from '@angular/core';
import { TranslationService } from './translation.service';
import { Observable, of } from 'rxjs';

@Pipe({
  name: 'translate',
  pure: false
})
export class TranslatePipe implements PipeTransform {
  private lastValue: string = '';
  private lastResult: string = '';
  
  constructor(private translationService: TranslationService) {}

  transform(value: string): string {
    if (!value || value === this.lastValue) {
      return this.lastResult;
    }
    
    this.lastValue = value;
    this.lastResult = ''; // Clear while loading
    
    this.translationService.translate(value).subscribe(
      (translated) => {
        this.lastResult = translated;
      },
      (error) => {
        console.error('Translation error in pipe:', error);
        this.lastResult = value; // Fallback to original
      }
    );
    
    return this.lastResult || value;
  }
}