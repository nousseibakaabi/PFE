import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TranslationService } from './translation.service';

@Injectable()
export class LanguageInterceptor implements HttpInterceptor {
  constructor(private translationService: TranslationService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Add language header to all API calls
    const lang = this.translationService.getCurrentLanguage();
    
    const modifiedReq = req.clone({
      setHeaders: {
        'Accept-Language': lang
      }
    });
    
    return next.handle(modifiedReq);
  }
}