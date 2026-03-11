import { Component , HostListener } from '@angular/core';
import { TranslationService } from '../partials/traduction/translation.service';

@Component({
  selector: 'app-translate',
  templateUrl: './translate.component.html',
styles: [`
    :host { display: inline-block; }
  `]
  })
export class TranslateComponent {

    isOpen = false;
  languages: any[];
  currentLang: string;

  constructor(private translationService: TranslationService) {
    this.languages = this.translationService.getLanguages();
    this.currentLang = this.translationService.getCurrentLanguage();
  }

  toggleDropdown(): void {
    this.isOpen = !this.isOpen;
  }

  changeLanguage(langCode: string): void {
    this.translationService.setLanguage(langCode);
    this.currentLang = langCode;
    this.isOpen = false;
  }

  getCurrentLanguageFlag(): string {
    const lang = this.languages.find(l => l.code === this.currentLang);
    return lang ? lang.flag : '🇫🇷';
  }

  getCurrentLanguageName(): string {
    const lang = this.languages.find(l => l.code === this.currentLang);
    return lang ? lang.name : 'Français';
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!event.target || !(event.target as HTMLElement).closest('.relative')) {
      this.isOpen = false;
    }
  }

}
