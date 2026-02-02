import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { CalendarEvent } from '../../../models/calendar';
import { environment } from '../../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class CalendarService {
  
 private apiUrl = `${environment.apiUrl}/calendar`;
  
  constructor(private http: HttpClient) {
    console.log('📅 CalendarService initialized with API URL:', this.apiUrl);
  }

  // Get all invoice events
  getInvoiceEvents(start?: Date, end?: Date): Observable<CalendarEvent[]> {
    let params = new HttpParams();
    
    if (start) {
      params = params.set('start', start.toISOString().split('T')[0]);
    }
    
    if (end) {
      params = params.set('end', end.toISOString().split('T')[0]);
    }
    
    return this.http.get<any>(`${this.apiUrl}/invoices`, { params }).pipe(
      map(response => {
        if (response.success) {
          return this.mapToCalendarEvents(response.data);
        }
        return [];
      }),
      catchError(error => {
        console.error('Error fetching invoice events:', error);
        return of([]);
      })
    );
  }

  // Get upcoming invoices
  getUpcomingInvoices(): Observable<CalendarEvent[]> {
    return this.http.get<any>(`${this.apiUrl}/upcoming-invoices`).pipe(
      map(response => {
        if (response.success) {
          return this.mapToCalendarEvents(response.data);
        }
        return [];
      }),
      catchError(error => {
        console.error('Error fetching upcoming invoices:', error);
        return of([]);
      })
    );
  }

  // Get overdue invoices
  getOverdueInvoices(): Observable<CalendarEvent[]> {
    return this.http.get<any>(`${this.apiUrl}/overdue-invoices`).pipe(
      map(response => {
        if (response.success) {
          return this.mapToCalendarEvents(response.data);
        }
        return [];
      }),
      catchError(error => {
        console.error('Error fetching overdue invoices:', error);
        return of([]);
      })
    );
  }

  
 

  // Get all events (invoices only for now)
  getAllEvents(start?: Date, end?: Date): Observable<CalendarEvent[]> {
    let params = new HttpParams();
    
    if (start) {
      const startDate = start.toISOString().split('T')[0];
      params = params.set('start', startDate);
      console.log('📅 Start date:', startDate);
    }
    
    if (end) {
      const endDate = end.toISOString().split('T')[0];
      params = params.set('end', endDate);
      console.log('📅 End date:', endDate);
    }
    
    const url = `${this.apiUrl}/events`;
    console.log('📅 Fetching from URL:', url);
    console.log('📅 Params:', params.toString());
    
    return this.http.get<any>(url, { params }).pipe(
      map(response => {
        console.log('📅 API Response:', response);
        if (response && response.success) {
          return this.mapToCalendarEvents(response.data);
        } else {
          console.error('📅 API Response error:', response);
          return [];
        }
      }),
      catchError(error => {
        console.error('❌ Error fetching calendar events:', error);
        if (error.error) {
          console.error('❌ Error details:', error.error);
        }
        return of([]);
      })
    );
  }

  // Get calendar statistics
  getCalendarStats(): Observable<any> {
    const url = `${this.apiUrl}/stats`;
    console.log('📊 Fetching stats from:', url);
    
    return this.http.get<any>(url).pipe(
      map(response => {
        console.log('📊 Stats response:', response);
        if (response && response.success) {
          return response.data;
        } else {
          console.error('📊 Stats response error:', response);
          return {};
        }
      }),
      catchError(error => {
        console.error('❌ Error fetching calendar stats:', error);
        if (error.error) {
          console.error('❌ Error details:', error.error);
        }
        return of({});
      })
    );
  }

  // Helper method to map DTO to CalendarEvent
  private mapToCalendarEvents(data: any[]): CalendarEvent[] {
    if (!data || !Array.isArray(data)) {
      console.warn('⚠️ No data or invalid data format received');
      return [];
    }
    
    console.log(`📅 Mapping ${data.length} events`);
    
    return data.map((item, index) => {
      console.log(`📅 Event ${index + 1}:`, item);
      return {
        id: item.id,
        title: item.title || 'Untitled Event',
        start: item.start ? new Date(item.start) : new Date(),
        end: item.end ? new Date(item.end) : undefined,
        allDay: item.allDay || false,
        type: item.type || 'INVOICE',
        color: item.color || '#3788d8',
        extendedProps: item.extendedProps || {}
      };
    });
  }
}


