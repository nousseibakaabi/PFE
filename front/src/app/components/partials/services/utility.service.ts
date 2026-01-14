import { Injectable, HostListener } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class UtilityService {
  
  constructor() {
    this.updateYear();
    this.initCopyFunctionality();
    this.initSearchShortcuts();
  }

  // Get current year for footer
  updateYear() {
    const yearElement = document.getElementById('year');
    if (yearElement) {
      yearElement.textContent = new Date().getFullYear().toString();
    }
  }

  // Copy functionality
  initCopyFunctionality() {
    const copyButton = document.getElementById('copy-button');
    const copyText = document.getElementById('copy-text');
    const websiteInput = document.getElementById('website-input') as HTMLInputElement;

    if (copyButton && copyText && websiteInput) {
      copyButton.addEventListener('click', () => {
        navigator.clipboard.writeText(websiteInput.value).then(() => {
          copyText.textContent = 'Copied';
          setTimeout(() => {
            copyText.textContent = 'Copy';
          }, 2000);
        });
      });
    }
  }

  // Search shortcuts (Cmd+K or Ctrl+K)
  @HostListener('document:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent) {
    const searchInput = document.getElementById('search-input') as HTMLInputElement;
    
    if ((event.metaKey || event.ctrlKey) && event.key === 'k') {
      event.preventDefault();
      if (searchInput) searchInput.focus();
    }
    
    if (event.key === '/' && document.activeElement !== searchInput) {
      event.preventDefault();
      if (searchInput) searchInput.focus();
    }
  }

  initSearchShortcuts() {
    const searchInput = document.getElementById('search-input') as HTMLInputElement;
    const searchButton = document.getElementById('search-button');
    
    if (searchButton && searchInput) {
      searchButton.addEventListener('click', () => {
        searchInput.focus();
      });
    }
  }
}