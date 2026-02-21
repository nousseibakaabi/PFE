import { Component, Input, Output, EventEmitter, OnInit, HostListener, ElementRef } from '@angular/core';

@Component({
  selector: 'app-date-picker',
  templateUrl: './date-picker.component.html',
  styleUrls: ['./date-picker.component.css']
})
export class DatePickerComponent implements OnInit {
  @Input() date: string = '';
  @Input() placeholder: string = 'Sélectionner une date';
  @Input() minDate: string = '';
  @Output() dateChange = new EventEmitter<string>();

  showDatePicker = false;
  currentDate = new Date();
  currentMonth = new Date().getMonth();
  currentYear = new Date().getFullYear();
  
  weekDays = ['L', 'M', 'M', 'J', 'V', 'S', 'D'];
  monthNames = ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin', 
                'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'];

  // Add ElementRef to constructor
  constructor(private elementRef: ElementRef) {}

  // Add HostListener for click outside
  @HostListener('document:click', ['$event.target'])
  onClick(target: any) {
    const clickedInside = this.elementRef.nativeElement.contains(target);
    if (!clickedInside && this.showDatePicker) {
      this.showDatePicker = false;
    }
  }

  get currentMonthName(): string {
    return this.monthNames[this.currentMonth];
  }

  get formattedDate(): string {
    if (!this.date) return '';
    const d = new Date(this.date);
    return `${d.getDate().toString().padStart(2, '0')}/${(d.getMonth() + 1).toString().padStart(2, '0')}/${d.getFullYear()}`;
  }

  get daysInMonth(): any[] {
    const days = [];
    const firstDay = new Date(this.currentYear, this.currentMonth, 1);
    const lastDay = new Date(this.currentYear, this.currentMonth + 1, 0);
    
    // Adjust for Monday as first day (European format)
    let firstDayIndex = firstDay.getDay() - 1;
    if (firstDayIndex < 0) firstDayIndex = 6;
    
    // Previous month days
    const prevMonthLastDay = new Date(this.currentYear, this.currentMonth, 0);
    for (let i = firstDayIndex; i > 0; i--) {
      const date = new Date(this.currentYear, this.currentMonth - 1, prevMonthLastDay.getDate() - i + 1);
      days.push({
        day: prevMonthLastDay.getDate() - i + 1,
        date: date,
        isCurrentMonth: false
      });
    }
    
    // Current month days
    for (let i = 1; i <= lastDay.getDate(); i++) {
      const date = new Date(this.currentYear, this.currentMonth, i);
      days.push({
        day: i,
        date: date,
        isCurrentMonth: true,
        isSelectable: this.isDateSelectable(date)
      });
    }
    
    // Next month days
    const remainingDays = 42 - days.length;
    for (let i = 1; i <= remainingDays; i++) {
      const date = new Date(this.currentYear, this.currentMonth + 1, i);
      days.push({
        day: i,
        date: date,
        isCurrentMonth: false
      });
    }
    
    return days;
  }

  ngOnInit() {
    if (this.date) {
      const d = new Date(this.date);
      this.currentYear = d.getFullYear();
      this.currentMonth = d.getMonth();
    }
  }

  toggleDatePicker(): void {
    this.showDatePicker = !this.showDatePicker;
    if (this.showDatePicker && this.date) {
      const d = new Date(this.date);
      this.currentYear = d.getFullYear();
      this.currentMonth = d.getMonth();
    }
  }

  closeDatePicker(): void {
    this.showDatePicker = false;
  }

  previousMonth(): void {
    if (this.currentMonth === 0) {
      this.currentMonth = 11;
      this.currentYear--;
    } else {
      this.currentMonth--;
    }
  }

  nextMonth(): void {
    if (this.currentMonth === 11) {
      this.currentMonth = 0;
      this.currentYear++;
    } else {
      this.currentMonth++;
    }
  }

  selectDate(date: Date): void {
    if (!this.isDateSelectable(date)) return;
    
    const year = date.getFullYear();
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const day = date.getDate().toString().padStart(2, '0');
    this.date = `${year}-${month}-${day}`;
    this.dateChange.emit(this.date);
    this.showDatePicker = false;
  }

  selectToday(): void {
    const today = new Date();
    this.selectDate(today);
  }

  isSelected(date: Date): boolean {
    if (!this.date) return false;
    const selected = new Date(this.date);
    return date.toDateString() === selected.toDateString();
  }

  isDateSelectable(date: Date): boolean {
    if (!this.minDate) return true;
    const min = new Date(this.minDate);
    return date >= min;
  }
}