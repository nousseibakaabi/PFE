import { Pipe, PipeTransform } from '@angular/core';
import { TranslationService } from './translation.service';
import { Observable, of } from 'rxjs';

@Pipe({
  name: 'asyncTranslate',
  pure: false
})
export class AsyncTranslatePipe implements PipeTransform {
  private cache = new Map<string, Observable<string>>();

  constructor(private translationService: TranslationService) {}

  transform(value: string): Observable<string> {
    if (!value) return of('');
    
    const lang = this.translationService.getCurrentLanguage();
    const cacheKey = `${value}_${lang}`;
    
    if (this.cache.has(cacheKey)) {
      return this.cache.get(cacheKey)!;
    }
    
    const translation$ = this.translationService.translate(value);
    this.cache.set(cacheKey, translation$);
    
    return translation$;
  }
}