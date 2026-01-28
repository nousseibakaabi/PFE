// notification.service.ts
import { Injectable } from '@angular/core';
import { Subject, Observable } from 'rxjs';

export interface Notification {
  id: number;
  type: 'success' | 'error' | 'info' | 'warning';
  title: string;
  message: string;
  duration?: number; // Auto-dismiss after milliseconds
  timestamp: Date;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private notifications: Notification[] = [];
  private notificationSubject = new Subject<Notification[]>();
  private nextId = 1;

  constructor() {}

  show(notification: Omit<Notification, 'id' | 'timestamp'>): void {
    const newNotification: Notification = {
      ...notification,
      id: this.nextId++,
      timestamp: new Date()
    };

    this.notifications.unshift(newNotification); // Add to beginning
    this.notificationSubject.next([...this.notifications]);

    // Auto-dismiss if duration is set
    if (notification.duration) {
      setTimeout(() => {
        this.dismiss(newNotification.id);
      }, notification.duration);
    }

    // Keep only last 10 notifications
    if (this.notifications.length > 10) {
      this.notifications = this.notifications.slice(0, 10);
    }
  }

  success(message: string, title: string = 'Success'): void {
    this.show({
      type: 'success',
      title,
      message,
      duration: 5000
    });
  }

  error(message: string, title: string = 'Error'): void {
    this.show({
      type: 'error',
      title,
      message,
      duration: 10000
    });
  }

  info(message: string, title: string = 'Info'): void {
    this.show({
      type: 'info',
      title,
      message,
      duration: 5000
    });
  }

  warning(message: string, title: string = 'Warning'): void {
    this.show({
      type: 'warning',
      title,
      message,
      duration: 7000
    });
  }

  dismiss(id: number): void {
    this.notifications = this.notifications.filter(n => n.id !== id);
    this.notificationSubject.next([...this.notifications]);
  }

  clearAll(): void {
    this.notifications = [];
    this.notificationSubject.next([]);
  }

  getNotifications(): Observable<Notification[]> {
    return this.notificationSubject.asObservable();
  }

  getNotificationsArray(): Notification[] {
    return [...this.notifications];
  }
}