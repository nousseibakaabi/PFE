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
import { TranslationService } from '../partials/traduction/translation.service';

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

  calendarOptions!: CalendarOptions;

  constructor(
    private calendarService: CalendarService,
    private router: Router,
    private translationService: TranslationService
  ) {}

  ngOnInit(): void {
    this.initCalendarOptions();
    this.loadCalendarEvents();
    this.loadCalendarStats();
  }

  private initCalendarOptions(): void {
    const translate = (key: string) => this.translationService.translate(key);
    
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
        right: 'dayGridMonth,timeGridWeek,timeGridDay'
      },
      buttonText: {
        today: translate("Aujourd'hui"),
        month: translate('Mois'),
        week: translate('Semaine'),
        day: translate('Jour')
      },
      locale: this.translationService.getCurrentLanguage(),
      allDayText: translate('Toute la journée'),
      dayHeaderFormat: { 
        weekday: 'short'
      },
      weekends: true,
      dayMaxEvents: 2,
      height: 'auto',
      contentHeight: 320,
      aspectRatio: 1.3,
      eventDidMount: this.handleEventMount.bind(this),
      datesSet: this.handleDatesChange.bind(this),
      dayMaxEventRows: 2,
      eventMaxStack: 2,
      eventTimeFormat: {
        hour: '2-digit',
        minute: '2-digit',
        hour12: false
      },
      displayEventTime: false,
      displayEventEnd: false,
      dayCellContent: (arg) => {
        return { html: `<span class="fc-daygrid-day-number text-[11px]">${arg.dayNumberText}</span>` };
      }
    };
  }

  navigateTo(route: string[]): void {
    this.router.navigate(route);
  }

  navigateToInvoice(invoiceId: number): void {
    this.router.navigate(['/factures', invoiceId]);
    this.selectedInvoice = null;
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
        console.error('Erreur lors du chargement des événements du calendrier', error);
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
        console.error('Erreur lors du chargement des statistiques du calendrier', error);
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
          className: className,
          extendedProps: {
            type: event.type,
            ...event.extendedProps
          }
        });
      });
    }
  }

  handleDatesChange(info: any): void {
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

  loadUpcomingInvoices(): void {
    this.isLoading = true;
    this.calendarService.getUpcomingInvoices().subscribe(
      events => {
        this.calendarEvents = events;
        this.updateCalendarEvents();
        this.isLoading = false;
      },
      error => {
        console.error('Erreur lors du chargement des factures à venir', error);
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
        console.error('Erreur lors du chargement des factures en retard', error);
        this.isLoading = false;
      }
    );
  }

  refreshCalendar(): void {
    this.loadCalendarEvents();
    this.loadCalendarStats();
  }

  getStatusColor(event: CalendarEvent): string {
    const status = event.extendedProps?.status;
    const isOverdue = event.extendedProps?.isOverdue;
    
    if (status === 'PAYE') return 'green';
    if (status === 'EN_RETARD' || isOverdue) return 'red';
    if (status === 'NON_PAYE') return 'yellow';
    return 'gray';
  }

  getStatusBadgeClass(event: CalendarEvent): string {
    const status = event.extendedProps?.status;
    const isOverdue = event.extendedProps?.isOverdue;
    
    if (status === 'PAYE') return 'status-paid';
    if (status === 'EN_RETARD' || isOverdue) return 'status-overdue';
    if (status === 'NON_PAYE') return 'status-unpaid';
    return '';
  }

  getCountByStatus(status: string): number {
    return this.calendarEvents.filter(event => 
      event.extendedProps?.status === status
    ).length;
  }

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
      const clickedEvent = this.calendarEvents.find(e => 
        e.id?.toString() === eventId
      );
      
      if (clickedEvent) {
        this.openInvoiceModal(clickedEvent);
      }
    }
  }

  openInvoiceModal(event: CalendarEvent): void {
    this.selectedInvoice = event;
  }

  getStatusText(event: CalendarEvent): string {
    const translate = (key: string) => this.translationService.translate(key);
    const status = event.extendedProps?.status;
    const isOverdue = event.extendedProps?.isOverdue;
    
    if (status === 'PAYE') return translate('Payé');
    if (status === 'EN_RETARD') return translate('En retard');
    if (isOverdue) return translate('En retard');
    if (status === 'NON_PAYE') return translate('Impayé');
    return translate('Inconnu');
  }

  handleEventMount(info: any): void {
    const translate = (key: string) => this.translationService.translate(key);
    const event = info.event;
    const extendedProps = event.extendedProps;
    
    if (extendedProps) {
      const status = extendedProps.status;
      const isOverdue = extendedProps.isOverdue;
      
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
        info.el.classList.add('fc-event-default');
      }
      
      const titleEl = info.el.querySelector('.fc-event-title');
      if (titleEl) {
        const fullTitle = event.title;
        const match = fullTitle.match(/#[A-Z0-9-]+/);
        if (match) {
          titleEl.textContent = match[0];
        } else {
          titleEl.textContent = fullTitle.length > 12 ? fullTitle.substring(0, 7) + '..' : fullTitle;
        }
      }
      
      info.el.title = `${event.title}\n${translate('Statut')}: ${this.getStatusText({
        extendedProps,
        id: event.id
      } as CalendarEvent)}\n${translate('Montant')}: ${extendedProps.amount || '0'} TND`;
      
      info.el.addEventListener('click', (e: MouseEvent) => {
        e.stopPropagation();
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