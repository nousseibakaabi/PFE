import { Component, forwardRef, Input, OnInit, ElementRef, ViewChild, HostListener } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MailService, EmailSuggestion } from '../../services/mail.service';
import { debounceTime, distinctUntilChanged, switchMap, catchError } from 'rxjs/operators';
import { Subject, of } from 'rxjs';

@Component({
  selector: 'app-email-input',
  templateUrl: './email-input.component.html',
  styleUrls: ['./email-input.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EmailInputComponent),
      multi: true
    }
  ]
})
export class EmailInputComponent implements ControlValueAccessor, OnInit {
  @Input() placeholder: string = 'To';
  @Input() label: string = 'To';
  @Input() disabled: boolean = false;
  
  @ViewChild('inputElement') inputElement!: ElementRef;

  // The actual value - array of email strings
  value: string[] = [];
  
  // For the input field
  inputValue: string = '';
  
  // Suggestions dropdown
  suggestions: EmailSuggestion[] = [];
  showSuggestions = false;
  selectedSuggestionIndex = -1;
  
  // For tag display
  tags: Array<{ email: string; name?: string; type?: string; id?: number }> = [];
  
  // Search subject
  private searchSubject = new Subject<string>();
  
  // ControlValueAccessor callbacks
  onChange: any = () => {};
  onTouched: any = () => {};

  constructor(private mailService: MailService) {}

  ngOnInit(): void {
    this.setupSearch();
  }

  setupSearch(): void {
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(query => {
        if (query.length < 2) {
          return of({ success: true, data: [] });
        }
        return this.mailService.getEmailSuggestions(query).pipe(
          catchError(() => of({ success: true, data: [] }))
        );
      })
    ).subscribe(response => {
      if (response.success) {
        this.suggestions = response.data;
        this.showSuggestions = this.suggestions.length > 0;
        this.selectedSuggestionIndex = -1;
      }
    });
  }

  // ControlValueAccessor implementation
  writeValue(value: any): void {
    if (value !== undefined) {
      this.value = Array.isArray(value) ? value : (value ? [value] : []);
      this.updateTagsFromValue();
    }
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  updateTagsFromValue(): void {
    this.tags = this.value.map(email => ({ email }));
  }

  onInputChange(): void {
    this.searchSubject.next(this.inputValue);
  }

  onInputFocus(): void {
    if (this.inputValue.length >= 2) {
      this.showSuggestions = true;
    }
  }

  @HostListener('document:click', ['$event'])
  onClickOutside(event: Event): void {
    if (!this.inputElement?.nativeElement.contains(event.target)) {
      this.showSuggestions = false;
    }
  }

  onInputKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault();
      this.addCurrentInput();
    } else if (event.key === 'Backspace' && this.inputValue === '' && this.tags.length > 0) {
      this.removeTag(this.tags.length - 1);
    } else if (event.key === 'ArrowDown' && this.showSuggestions) {
      event.preventDefault();
      this.selectedSuggestionIndex = Math.min(this.selectedSuggestionIndex + 1, this.suggestions.length - 1);
    } else if (event.key === 'ArrowUp' && this.showSuggestions) {
      event.preventDefault();
      this.selectedSuggestionIndex = Math.max(this.selectedSuggestionIndex - 1, -1);
    } else if (event.key === 'Tab' && this.showSuggestions && this.selectedSuggestionIndex >= 0) {
      event.preventDefault();
      this.selectSuggestion(this.suggestions[this.selectedSuggestionIndex]);
    } else if (event.key === 'Escape') {
      this.showSuggestions = false;
    }
  }

  addCurrentInput(): void {
    const email = this.inputValue.trim();
    if (email && this.isValidEmail(email) && !this.value.includes(email)) {
      this.value.push(email);
      this.tags.push({ email });
      this.onChange(this.value);
      this.inputValue = '';
      this.showSuggestions = false;
    }
  }

  addSuggestionAsTag(suggestion: EmailSuggestion): void {
    let email = suggestion.email;
    
    // If it's a group, we store the group identifier
    if (suggestion.type === 'GROUP') {
      email = `GROUP:${suggestion.name}`;
    }
    
    if (!this.value.includes(email)) {
      this.value.push(email);
      this.tags.push({ 
        email: email, 
        name: suggestion.name, 
        type: suggestion.type,
        id: suggestion.id 
      });
      this.onChange(this.value);
    }
    this.inputValue = '';
    this.showSuggestions = false;
  }

  selectSuggestion(suggestion: EmailSuggestion): void {
    this.addSuggestionAsTag(suggestion);
    this.inputElement.nativeElement.focus();
  }

  removeTag(index: number): void {
    this.tags.splice(index, 1);
    this.value.splice(index, 1);
    this.onChange(this.value);
  }

  isValidEmail(email: string): boolean {
    if (email.startsWith('GROUP:')) return true; // Group identifiers are valid
    const re = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    return re.test(email);
  }

  getTagDisplay(tag: any): string {
    if (tag.type === 'GROUP') {
      return `👥 ${tag.name || tag.email.replace('GROUP:', '')}`;
    }
    return tag.name || tag.email;
  }

  getTagClass(tag: any): string {
    return tag.type === 'GROUP' ? 'bg-purple-100 text-purple-800' : 'bg-blue-100 text-blue-800';
  }
}