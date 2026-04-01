import { Component, OnInit } from '@angular/core';
import { NotificationService, Notification } from '../../services/notification.service';

@Component({
  selector: 'app-notifications',
  templateUrl: './notifications.component.html'
})
export class NotificationsComponent implements OnInit {
  notifications: Notification[] = [];
  isLoading = false;
  page = 0;
  hasMore = true;

  viewMode: 'list' | 'card' = 'card';
  showDetailModal = false;
  selectedNotification: Notification | null = null;

  constructor(private notificationService: NotificationService) {}

  ngOnInit(): void {
    this.loadNotifications();
  }

  loadNotifications(): void {
    if (!this.hasMore || this.isLoading) return;

    this.isLoading = true;
    this.notificationService.getNotifications(this.page, 20).subscribe({
      next: (response) => {
        this.notifications = [...this.notifications, ...response.data];
        this.page++;
        this.hasMore = this.page < response.totalPages;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Failed to load notifications', error);
        this.isLoading = false;
      }
    });
  }

  loadMore(): void {
    this.loadNotifications();
  }

  refreshNotifications(): void {
    this.page = 0;
    this.notifications = [];
    this.hasMore = true;
    this.loadNotifications();
  }

  hasUnreadNotifications(): boolean {
    return this.notifications.some(n => !n.isRead);
  }

  onNotificationClick(notification: Notification): void {
    this.markAsRead(notification);
    // Add navigation logic here if needed
  }

  markAsRead(notification: Notification): void {
    if (!notification.isRead) {
      this.notificationService.markAsRead(notification.id).subscribe();
    }
  }

  markAllAsRead(): void {
    this.notificationService.markAllAsRead().subscribe({
      next: () => {
        this.notifications.forEach(n => n.isRead = true);
      }
    });
  }

  onDeleteClick(id: number, event: Event): void {
    event.stopPropagation();
    this.notificationService.deleteNotification(id).subscribe({
      next: () => {
        this.notifications = this.notifications.filter(n => n.id !== id);
      }
    });
  }

  // Helper methods for template
  getNotificationRowClass(notification: Notification): any {
    return {
      'bg-blue-50/50 dark:bg-blue-900/10': !notification.isRead
    };
  }

  getTitleClass(notification: Notification): any {
    return {
      'font-semibold': !notification.isRead
    };
  }

  getIconBackgroundClass(type: string): any {
    return {
      'bg-blue-100 dark:bg-blue-900/20': type === 'INFO',
      'bg-yellow-100 dark:bg-yellow-900/20': type === 'WARNING',
      'bg-green-100 dark:bg-green-900/20': type === 'SUCCESS',
      'bg-red-100 dark:bg-red-900/20': type === 'DANGER'
    };
  }

  getNotificationIcon(type: string): string {
    switch (type) {
      case 'INFO': return 'fa-info-circle';
      case 'WARNING': return 'fa-exclamation-triangle';
      case 'SUCCESS': return 'fa-check-circle';
      case 'DANGER': return 'fa-exclamation-circle';
      default: return 'fa-bell';
    }
  }

  getDaysBadgeClass(daysUntilDue: number): any {
    return {
      'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/20 dark:text-yellow-300': daysUntilDue > 0,
      'bg-orange-100 text-orange-800 dark:bg-orange-900/20 dark:text-orange-300': daysUntilDue === 0,
      'bg-red-100 text-red-800 dark:bg-red-900/20 dark:text-red-300': daysUntilDue < 0
    };
  }

  getDaysIconClass(daysUntilDue: number): string {
    if (daysUntilDue > 0) return 'fa-clock';
    if (daysUntilDue === 0) return 'fa-calendar-day';
    return 'fa-exclamation-circle';
  }

  getReferenceTypeIcon(referenceType: string): string {
    switch (referenceType) {
      case 'FACTURE': return 'fa-file-invoice';
      case 'CONVENTION': return 'fa-file-contract';
      case 'APPLICATION': return 'fa-project-diagram';
      default: return 'fa-file';
    }
  }

  getDaysStatusText(daysUntilDue?: number): string {
    if (daysUntilDue === undefined || daysUntilDue === null) return '';
    
    if (daysUntilDue > 0) {
      return daysUntilDue === 1 ? 'Demain' : `Dans ${daysUntilDue} jours`;
    } else if (daysUntilDue === 0) {
      return 'Aujourd\'hui';
    } else {
      return `${Math.abs(daysUntilDue)} jours en retard`;
    }
  }

 formatTime(createdAt: string): string {
  const date = new Date(createdAt);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHours / 24);

  if (diffMins < 1) return 'À l\'instant';
  if (diffMins < 60) return `Il y a ${diffMins} minute${diffMins === 1 ? '' : 's'}`;
  if (diffHours < 24) return `Il y a ${diffHours} heure${diffHours === 1 ? '' : 's'}`;
  if (diffDays < 7) return `Il y a ${diffDays} jour${diffDays === 1 ? '' : 's'}`;
  
  return date.toLocaleDateString('fr-FR', { 
    month: 'short', 
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
}


  openDetailModal(notification: Notification): void {
  this.selectedNotification = notification;
  this.showDetailModal = true;
  if (!notification.isRead) {
    this.markAsRead(notification);
  }
}

closeDetailModal(): void {
  this.showDetailModal = false;
  this.selectedNotification = null;
}

markAsReadAndClose(notification: Notification): void {
  this.markAsRead(notification);
  this.closeDetailModal();
}

getModalHeaderClass(type: string): string {
  switch (type) {
    case 'INFO': return 'bg-gradient-to-r from-blue-500 to-blue-600';
    case 'WARNING': return 'bg-gradient-to-r from-amber-500 to-orange-500';
    case 'SUCCESS': return 'bg-gradient-to-r from-emerald-500 to-green-600';
    case 'DANGER': return 'bg-gradient-to-r from-red-500 to-rose-600';
    default: return 'bg-gradient-to-r from-indigo-500 to-indigo-600';
  }
}

formatDate(dateString: string): string {
  const date = new Date(dateString);
  return date.toLocaleDateString('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
}
}