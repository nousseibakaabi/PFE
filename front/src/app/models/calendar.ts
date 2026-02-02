// calendar.ts
export interface CalendarEvent {
  id?: number;
  title: string;
  start: Date;
  end?: Date;
  allDay: boolean;
  type: 'INVOICE' | 'PROJECT';
  color?: string;
  extendedProps?: {
    status?: string;
    amount?: number;
    clientName?: string;
    conventionId?: number;
    conventionReference?: string;
    isOverdue?: boolean;
    notes?: string;
    [key: string]: any;
  };
}

export interface InvoiceEvent extends CalendarEvent {
  dueDate: Date;
  amount: number;
  status: 'PAID' | 'UNPAID' | 'OVERDUE';
  clientName: string;
}

export interface ProjectEvent extends CalendarEvent {
  deadline: Date;
  projectStatus: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'DELAYED';
  progress: number;
}