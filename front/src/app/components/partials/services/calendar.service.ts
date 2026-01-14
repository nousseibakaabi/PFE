import { Injectable } from '@angular/core';
import { Calendar } from '@fullcalendar/core';
import dayGridPlugin from '@fullcalendar/daygrid';
import listPlugin from '@fullcalendar/list';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';

@Injectable({
  providedIn: 'root'
})
export class CalendarService {
  
  initCalendar(containerId: string = 'calendar') {
    const calendarEl = document.getElementById(containerId);
    if (!calendarEl) return;

    const newDate = new Date();
    const getDynamicMonth = () => {
      const month = newDate.getMonth() + 1;
      return month < 10 ? `0${month}` : `${month}`;
    };

    const calendarEventsList = [
      {
        id: 1,
        title: "Event Conf.",
        start: `${newDate.getFullYear()}-${getDynamicMonth()}-01`,
        extendedProps: { calendar: "Danger" },
      },
      {
        id: 2,
        title: "Seminar #4",
        start: `${newDate.getFullYear()}-${getDynamicMonth()}-07`,
        end: `${newDate.getFullYear()}-${getDynamicMonth()}-10`,
        extendedProps: { calendar: "Success" },
      },
      // ... rest of events
    ];

    const calendar = new Calendar(calendarEl, {
      plugins: [dayGridPlugin, timeGridPlugin, listPlugin, interactionPlugin],
      selectable: true,
      initialView: "dayGridMonth",
      initialDate: `${newDate.getFullYear()}-${getDynamicMonth()}-07`,
      headerToolbar: {
        left: "prev,next addEventButton",
        center: "title",
        right: "dayGridMonth,timeGridWeek,timeGridDay",
      },
      events: calendarEventsList,
      customButtons: {
        addEventButton: {
          text: "Add Event +",
          click: () => this.openModal()
        }
      },
      eventClassNames({ event }: any) {
        const getColorValue = event._def.extendedProps.calendar;
        return [`event-fc-color`, `fc-bg-${getColorValue}`];
      },
    });

    calendar.render();
    return calendar;
  }

  private openModal() {
    // Implement modal opening logic
    console.log('Open calendar modal');
  }
}