import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject, BehaviorSubject } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { Client, IMessage, Stomp } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { AuthService } from './auth.service';

export interface Notification {
  id: number;
  title: string;
  message: string;
  type: 'INFO' | 'WARNING' | 'SUCCESS' | 'DANGER';
  notificationType: string;
  referenceId: number;
  referenceCode: string;
  referenceType: string;
  daysUntilDue?: number;
  createdAt: string;
  isRead: boolean;
  readAt?: string;
  emailSent?: boolean;
  smsSent?: boolean;
  emailSentAt?: string;
  smsSentAt?: string;
  isSent?: boolean;
  sentAt?: string;
}

export interface NotificationResponse {
  success: boolean;
  data: Notification[];
  totalPages: number;
  totalElements: number;
  currentPage: number;
  unreadCount: number;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private apiUrl = environment.apiUrl;
  private unreadCountSubject = new BehaviorSubject<number>(0);
  private newNotificationSubject = new Subject<Notification>();
  private stompClient: Client | null = null;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {
    this.initializeWebSocketConnection();
    this.loadUnreadCount();
  }

  // ============= REST API METHODS =============

  /**
   * Get user notifications
   */
  getNotifications(page: number = 0, size: number = 20): Observable<NotificationResponse> {
    return this.http.get<NotificationResponse>(
      `${this.apiUrl}/api/notifications?page=${page}&size=${size}`
    ).pipe(
      tap(response => {
        if (response.unreadCount !== undefined) {
          this.unreadCountSubject.next(response.unreadCount);
        }
      })
    );
  }

  /**
   * Get unread count
   */
  getUnreadCount(): Observable<number> {
    return this.http.get<any>(`${this.apiUrl}/api/notifications/unread/count`).pipe(
      map(response => response.count),
      tap(count => this.unreadCountSubject.next(count))
    );
  }

  /**
   * Load unread count
   */
  loadUnreadCount(): void {
    if (this.authService.isLoggedIn()) {
      this.getUnreadCount().subscribe({
        error: (error) => console.error('Failed to load unread count', error)
      });
    }
  }

  /**
   * Mark notification as read
   */
  markAsRead(id: number): Observable<any> {
    return this.http.put(`${this.apiUrl}/api/notifications/${id}/read`, {}).pipe(
      tap(() => {
        this.loadUnreadCount();
      })
    );
  }

  /**
   * Mark all notifications as read
   */
  markAllAsRead(): Observable<any> {
    return this.http.put(`${this.apiUrl}/api/notifications/read-all`, {}).pipe(
      tap(() => {
        this.loadUnreadCount();
      })
    );
  }

  /**
   * Delete notification
   */
  deleteNotification(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/api/notifications/${id}`).pipe(
      tap(() => {
        this.loadUnreadCount();
      })
    );
  }

  // ============= WEBSOCKET METHODS =============

  /**
   * Initialize WebSocket connection for real-time notifications
   */
  private initializeWebSocketConnection(): void {
    if (!this.authService.isLoggedIn()) {
      return;
    }

    const token = this.authService.token;
    
    try {
      const socket = new SockJS(`${this.apiUrl}/ws`);
      
      this.stompClient = new Client({
        webSocketFactory: () => socket,
        connectHeaders: {
          Authorization: `Bearer ${token}`
        },
        debug: (str: string) => {
          console.log(str);
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000
      });

      this.stompClient.onConnect = (frame: any) => {
        console.log('Connected to WebSocket for notifications');
        
        this.stompClient?.subscribe('/user/queue/notifications', (message: IMessage) => {
          if (message.body) {
            try {
              const notification: Notification = JSON.parse(message.body);
              this.newNotificationSubject.next(notification);
              this.loadUnreadCount();
            } catch (e) {
              console.error('Error parsing notification:', e);
            }
          }
        });
      };

      this.stompClient.onStompError = (frame: any) => {
        console.error('Broker reported error: ' + frame.headers['message']);
        console.error('Additional details: ' + frame.body);
      };

      this.stompClient.activate();
    } catch (error) {
      console.error('WebSocket connection error:', error);
    }
  }

  /**
   * Get observable for new notifications
   */
  onNewNotification(): Observable<Notification> {
    return this.newNotificationSubject.asObservable();
  }

  /**
   * Get observable for unread count changes
   */
  onUnreadCountChange(): Observable<number> {
    return this.unreadCountSubject.asObservable();
  }

  /**
   * Get current unread count
   */
  get currentUnreadCount(): number {
    return this.unreadCountSubject.value;
  }

  // ============= HELPER METHODS =============

  /**
   * Get notification icon based on type
   */
  getNotificationIcon(type: string): string {
    switch (type) {
      case 'INFO': return 'fa-info-circle';
      case 'WARNING': return 'fa-exclamation-triangle';
      case 'SUCCESS': return 'fa-check-circle';
      case 'DANGER': return 'fa-exclamation-circle';
      default: return 'fa-bell';
    }
  }

  /**
   * Get notification color class based on type
   */
  getNotificationColor(type: string): string {
    switch (type) {
      case 'INFO': return 'text-blue-500';
      case 'WARNING': return 'text-yellow-500';
      case 'SUCCESS': return 'text-green-500';
      case 'DANGER': return 'text-red-500';
      default: return 'text-gray-500';
    }
  }

  /**
   * Get days status text
   */
  getDaysStatusText(daysUntilDue?: number): string {
    if (daysUntilDue === undefined || daysUntilDue === null) return '';
    
    if (daysUntilDue > 0) {
      return daysUntilDue === 1 ? 'Tomorrow' : `In ${daysUntilDue} days`;
    } else if (daysUntilDue === 0) {
      return 'Today';
    } else {
      return `${Math.abs(daysUntilDue)} days overdue`;
    }
  }

  /**
   * Format notification time
   */
  formatTime(createdAt: string): string {
    const date = new Date(createdAt);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins} min${diffMins === 1 ? '' : 's'} ago`;
    if (diffHours < 24) return `${diffHours} hour${diffHours === 1 ? '' : 's'} ago`;
    if (diffDays < 7) return `${diffDays} day${diffDays === 1 ? '' : 's'} ago`;
    
    return date.toLocaleDateString('en-US', { 
      month: 'short', 
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  /**
   * Disconnect WebSocket
   */
  disconnect(): void {
    if (this.stompClient) {
      this.stompClient.deactivate();
      console.log('Disconnected from WebSocket');
    }
  }
}