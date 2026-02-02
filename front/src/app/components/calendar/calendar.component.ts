import { Component, OnInit, ViewChild } from '@angular/core';
import { CalendarService } from '../partials/services/calendar.service';
import { CalendarEvent } from '../../models/calendar';
import { FullCalendarComponent } from '@fullcalendar/angular';
import { CalendarOptions, EventClickArg } from '@fullcalendar/core';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import listPlugin from '@fullcalendar/list';
import interactionPlugin from '@fullcalendar/interaction';
import { Router } from '@angular/router';

@Component({
  selector: 'app-calendar',
  templateUrl: './calendar.component.html',
  styleUrls: ['./calendar.component.css']
})

export class CalendarComponent implements OnInit {
  @ViewChild('calendar') calendarComponent!: FullCalendarComponent;

  showInvoiceCalendar = true;
  showProjectCalendar = false;
  calendarEvents: CalendarEvent[] = [];
  calendarStats: any = {};
  isLoading = false;
  selectedInvoice: CalendarEvent | null = null;

  // Remove the static calendarOptions declaration and initialize it in constructor or ngOnInit
  calendarOptions!: CalendarOptions;

  constructor(
    private calendarService: CalendarService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initCalendarOptions();
    this.loadCalendarEvents();
    this.loadCalendarStats();
  }

  private initCalendarOptions(): void {
    this.calendarOptions = {
      plugins: [dayGridPlugin, timeGridPlugin, listPlugin, interactionPlugin],
      initialView: 'dayGridMonth',
      selectable: true,
      editable: false,
      droppable: false,
      eventClick: this.handleEventClick.bind(this),
      events: [],
      headerToolbar: {
        left: 'prev,next today',
        center: 'title',
        right: 'dayGridMonth,timeGridWeek,timeGridDay,listWeek'
      },
      // REMOVE THESE - they interfere with our custom styling
      // eventColor: '#3788d8',
      // eventTextColor: '#ffffff',
      weekends: true,
      dayMaxEvents: 3,
      height: '500px',
      contentHeight: 'auto',
      aspectRatio: 1.5,
      eventDidMount: this.handleEventMount.bind(this),
      datesSet: this.handleDatesChange.bind(this),
      dayMaxEventRows: 3,
      eventMaxStack: 3,
      dayCellContent: (arg) => {
        return { html: `<span class="fc-daygrid-day-number">${arg.dayNumberText}</span>` };
      }
    };
  }

  navigateTo(route: string[]): void {
  this.router.navigate(route);
}


  navigateToInvoice(invoiceId: number): void {
    this.router.navigate(['/factures', invoiceId]);
    this.selectedInvoice = null; // Close modal
  }
  
 get overdueEvents(): CalendarEvent[] {
    return this.calendarEvents.filter(event => event.extendedProps?.isOverdue);
  }

  get recentOverdueEvents(): CalendarEvent[] {
    return this.overdueEvents.slice(0, 5);
  }


  loadCalendarEvents(): void {
    this.isLoading = true;
    this.calendarService.getAllEvents().subscribe(
      events => {
        this.calendarEvents = events;
        this.updateCalendarEvents();
        this.isLoading = false;
      },
      error => {
        console.error('Error loading calendar events:', error);
        this.isLoading = false;
      }
    );
  }

  loadCalendarStats(): void {
    this.calendarService.getCalendarStats().subscribe(
      stats => {
        this.calendarStats = stats;
      },
      error => {
        console.error('Error loading calendar stats:', error);
      }
    );
  }


  updateCalendarEvents(): void {
  const filteredEvents = this.calendarEvents.filter(event => {
    if (event.type === 'INVOICE' && !this.showInvoiceCalendar) return false;
    return true;
  });

  if (this.calendarComponent && this.calendarComponent.getApi()) {
    const calendarApi = this.calendarComponent.getApi();
    calendarApi.removeAllEvents();
    
    filteredEvents.forEach(event => {
      // Determine CSS class based on status
      let className = '';
      const status = event.extendedProps?.status;
      const isOverdue = event.extendedProps?.isOverdue;
      
      if (status === 'PAYE') {
        className = 'fc-event-paid';
      } else if (isOverdue || status === 'EN_RETARD') {
        className = 'fc-event-overdue';
      } else if (status === 'NON_PAYE') {
        className = 'fc-event-unpaid';
      } else {
        className = 'fc-event-default';
      }
      
      calendarApi.addEvent({
        id: event.id?.toString(),
        title: event.title,
        start: event.start,
        end: event.end,
        allDay: event.allDay || false,
        className: className, // Add the class here
        extendedProps: {
          type: event.type,
          ...event.extendedProps
        }
      });
    });
  }
}



  handleDatesChange(info: any): void {
    // Load events for visible date range
    this.calendarService.getAllEvents(info.start, info.end).subscribe(
      events => {
        this.calendarEvents = events;
        this.updateCalendarEvents();
      }
    );
  }



  toggleInvoiceCalendar(): void {
    this.showInvoiceCalendar = !this.showInvoiceCalendar;
    this.updateCalendarEvents();
  }

  toggleProjectCalendar(): void {
    this.showProjectCalendar = !this.showProjectCalendar;
    this.updateCalendarEvents();
  }

  viewInvoiceDetails(invoiceId: number, extendedProps: any): void {
    // Navigate to invoice details page
    console.log('View invoice:', invoiceId, extendedProps);
    
    // You can show a modal or navigate to invoice page
    const confirmation = confirm(
      `Invoice Details:\n
ID: ${invoiceId}
Status: ${extendedProps.status || 'N/A'}
Amount: ${extendedProps.amount || 'N/A'}
Client: ${extendedProps.clientName || 'N/A'}
Convention: ${extendedProps.conventionReference || 'N/A'}
\nDo you want to view this invoice?`
    );
    
    if (confirmation) {
      this.router.navigate(['/factures']);
      // Or navigate directly to invoice: this.router.navigate(['/factures', invoiceId]);
    }
  }

  // Load specific data
  loadUpcomingInvoices(): void {
    this.isLoading = true;
    this.calendarService.getUpcomingInvoices().subscribe(
      events => {
        this.calendarEvents = events;
        this.updateCalendarEvents();
        this.isLoading = false;
      },
      error => {
        console.error('Error loading upcoming invoices:', error);
        this.isLoading = false;
      }
    );
  }

  loadOverdueInvoices(): void {
    this.isLoading = true;
    this.calendarService.getOverdueInvoices().subscribe(
      events => {
        this.calendarEvents = events;
        this.updateCalendarEvents();
        this.isLoading = false;
      },
      error => {
        console.error('Error loading overdue invoices:', error);
        this.isLoading = false;
      }
    );
  }

  // Refresh calendar
  refreshCalendar(): void {
    this.loadCalendarEvents();
    this.loadCalendarStats();
  }




// Get status color for display
getStatusColor(event: CalendarEvent): string {
  const status = event.extendedProps?.status;
  const isOverdue = event.extendedProps?.isOverdue;
  
  if (status === 'PAYE') return 'green';
  if (status === 'EN_RETARD' || isOverdue) return 'red';
  if (status === 'NON_PAYE') return 'yellow';
  return 'gray';
}

// Get status badge class
getStatusBadgeClass(event: CalendarEvent): string {
  const status = event.extendedProps?.status;
  const isOverdue = event.extendedProps?.isOverdue;
  
  if (status === 'PAYE') return 'status-paid';
  if (status === 'EN_RETARD' || isOverdue) return 'status-overdue';
  if (status === 'NON_PAYE') return 'status-unpaid';
  return '';
}


// Add this method to calendar.component.ts
getCountByStatus(status: string): number {
  return this.calendarEvents.filter(event => 
    event.extendedProps?.status === status
  ).length;
}

// Add computed property for overdue count
get overdueCount(): number {
  return this.calendarEvents.filter(event => 
    event.extendedProps?.isOverdue || event.extendedProps?.status === 'EN_RETARD'
  ).length;
}


 handleEventClick(info: EventClickArg): void {
    const eventType = info.event.extendedProps['type'];
    const eventId = info.event.id;
    const extendedProps = info.event.extendedProps;
    
    if (eventType === 'INVOICE') {
      // Find the clicked event from our events array
      const clickedEvent = this.calendarEvents.find(e => 
        e.id?.toString() === eventId
      );
      
      if (clickedEvent) {
        this.openInvoiceModal(clickedEvent);
      }
    }
  }

  // New method to open invoice modal
  openInvoiceModal(event: CalendarEvent): void {
    this.selectedInvoice = event;
  }

  // Method to get status text
  getStatusText(event: CalendarEvent): string {
    const status = event.extendedProps?.status;
    const isOverdue = event.extendedProps?.isOverdue;
    
    if (status === 'PAYE') return 'Paid';
    if (status === 'EN_RETARD') return 'Late';
    if (isOverdue) return 'Overdue';
    if (status === 'NON_PAYE') return 'Unpaid';
    return 'Unknown';
  }



  handleEventMount(info: any): void {
  const event = info.event;
  const extendedProps = event.extendedProps;
  
  if (extendedProps) {
    const status = extendedProps.status;
    const isOverdue = extendedProps.isOverdue;
    
    // ONLY add CSS classes - NO inline styles!
    if (status === 'PAYE') {
      info.el.classList.add('fc-event-paid');
    } 
    else if (isOverdue || status === 'EN_RETARD') {
      info.el.classList.add('fc-event-overdue');
    }
    else if (status === 'NON_PAYE') {
      info.el.classList.add('fc-event-unpaid');
    }
    else {
      // Default fallback
      info.el.classList.add('fc-event-default');
    }
    
    // Add tooltip
    info.el.title = `${event.title}\nStatus: ${this.getStatusText({
      extendedProps,
      id: event.id
    } as CalendarEvent)}\nAmount: ${extendedProps.amount || '0'} TND`;
    
    // Add click handler
    info.el.addEventListener('click', () => {
      const clickedEvent = this.calendarEvents.find(e => 
        e.id?.toString() === event.id
      );
      if (clickedEvent) {
        this.openInvoiceModal(clickedEvent);
      }
    });
  }
}
}