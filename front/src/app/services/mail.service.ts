import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Mail {
  id: number;
  subject: string;
  content: string;
  senderEmail: string;
  senderName: string;
  senderId: number;
  sentAt: string;
  readAt?: string;
  isRead: boolean;
  isStarred: boolean;
  isArchived: boolean;
  hasAttachments: boolean;
  importance: 'HIGH' | 'NORMAL' | 'LOW';
  recipients: MailRecipient[];
  attachments: MailAttachment[];
  parentMailId?: number;
  replyCount: number;
}

export interface MailRecipient {
  email: string;
  name?: string;
  type: 'TO' | 'CC' | 'BCC';
  isRead: boolean;
  readAt?: string;
}

export interface MailAttachment {
  id: number;
  fileName: string;
  fileType: string;
  fileSize: number;
  downloadUrl: string;
}

export interface MailDraft {
  id: number;
  subject: string;
  content: string;
  to: string[];
  cc: string[];
  bcc: string[];
  lastSavedAt: string;
  isSending: boolean;
  attachments: DraftAttachment[];
}

export interface DraftAttachment {
  id: number;
  fileName: string;
  fileType: string;
  fileSize: number;
}


// In mail.service.ts, update the MailStats interface
export interface MailStats {
  inboxCount: number;
  unreadCount: number;
  sentCount: number;
  draftCount: number;
  starredCount: number;
  archivedCount: number;
  trashCount: number;
  
  // Group counts
  systemGroupsCount: number;
  customGroupsCount: number;
  totalGroupsCount: number;
  
  // Group mail statistics - NEW
  groupMailsCount: number;
  unreadGroupMailsCount: number;
  groupStats?: GroupMailStat[]; // Optional detailed stats
}

export interface GroupMailStat {
  groupId: number;
  groupName: string;
  isSystem: boolean;
  totalMails: number;
  unreadMails: number;
  membersCount: number;
}

export interface MailFolder {
  id: number;
  name: string;
  description?: string;
  color?: string;
  isSystem: boolean;
  mailCount: number;
  unreadCount: number;
}

export interface MailSignature {
  id: number;
  name: string;
  content: string;
  isDefault: boolean;
  isHtml: boolean;
}

export interface MailRequest {
  subject: string;
  content: string;
  to: string[];
  cc?: string[];
  bcc?: string[];
  importance?: 'HIGH' | 'NORMAL' | 'LOW';
  parentMailId?: number;
  draftId?: number;
}

export interface MailActionRequest {
  mailIds: number[];
  action: 'READ' | 'UNREAD' | 'STAR' | 'UNSTAR' | 'ARCHIVE' | 'UNARCHIVE' | 'DELETE' | 'RESTORE';
  folderId?: number;
  folderName?: string;
}

export interface PageResponse<T> {
  success: boolean;
  data: T[];
  totalPages: number;
  totalElements: number;
  currentPage: number;
}

export interface MailGroup {
  id: number;
  name: string;
  description?: string;
  isSystem: boolean;
  ownerId: number;
  ownerName: string;
  members: GroupMember[];
  createdAt: string;
  updatedAt: string;
  unreadCount:number;
  mailCount: number;        
}

export interface GroupMember {
  id: number;
  email: string;
  name: string;
  role: string;
}

export interface EmailSuggestion {
  email: string;
  name: string;
  type: 'USER' | 'GROUP';
  id: number;
  groupName?: string;
  role?: string;
}

export interface GroupRequest {
  name: string;
  description?: string;
  memberIds: number[];
}

@Injectable({
  providedIn: 'root'
})
export class MailService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  // ============= SEND MAIL =============
  sendMail(request: MailRequest, attachments?: File[]): Observable<any> {
    const formData = new FormData();
    formData.append('request', new Blob([JSON.stringify(request)], { type: 'application/json' }));
    
    if (attachments && attachments.length > 0) {
      attachments.forEach(file => {
        formData.append('attachments', file);
      });
    }

    return this.http.post(`${this.apiUrl}/api/mails/send`, formData);
  }

  // ============= DRAFTS =============
  saveDraft(request: Partial<MailRequest>, attachments?: File[]): Observable<any> {
    const formData = new FormData();
    
    const draftRequest = {
      subject: request.subject || '',
      content: request.content || '',
      to: request.to || [],
      cc: request.cc || [],
      bcc: request.bcc || []
    };
    
    formData.append('request', new Blob([JSON.stringify(draftRequest)], { type: 'application/json' }));
    
    if (attachments && attachments.length > 0) {
      attachments.forEach(file => {
        formData.append('attachments', file);
      });
    }

    return this.http.post(`${this.apiUrl}/api/mails/drafts`, formData);
  }

  getDraftById(id: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/mails/drafts/${id}`);
  }

  getDrafts(): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/mails/drafts`);
  }

  updateDraft(id: number, request: Partial<MailRequest>, attachments?: File[]): Observable<any> {
    const formData = new FormData();
    
    const draftRequest = {
      subject: request.subject || '',
      content: request.content || '',
      to: request.to || [],
      cc: request.cc || [],
      bcc: request.bcc || []
    };
    
    formData.append('request', new Blob([JSON.stringify(draftRequest)], { type: 'application/json' }));
    
    if (attachments && attachments.length > 0) {
      attachments.forEach(file => {
        formData.append('attachments', file);
      });
    }

    return this.http.put(`${this.apiUrl}/api/mails/drafts/${id}`, formData);
  }

  deleteDraft(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/api/mails/drafts/${id}`);
  }

  sendFromDraft(id: number, request: MailRequest, attachments?: File[]): Observable<any> {
    const formData = new FormData();
    request.draftId = id;
    formData.append('request', new Blob([JSON.stringify(request)], { type: 'application/json' }));
    
    if (attachments && attachments.length > 0) {
      attachments.forEach(file => {
        formData.append('attachments', file);
      });
    }

    return this.http.post(`${this.apiUrl}/api/mails/send`, formData);
  }

  // ============= GET MAILS =============
  getInbox(page: number = 0, size: number = 20): Observable<PageResponse<Mail>> {
    return this.http.get<PageResponse<Mail>>(`${this.apiUrl}/api/mails/inbox?page=${page}&size=${size}`);
  }

  getSent(page: number = 0, size: number = 20): Observable<PageResponse<Mail>> {
    return this.http.get<PageResponse<Mail>>(`${this.apiUrl}/api/mails/sent?page=${page}&size=${size}`);
  }

  getStarred(page: number = 0, size: number = 20): Observable<PageResponse<Mail>> {
    return this.http.get<PageResponse<Mail>>(`${this.apiUrl}/api/mails/starred?page=${page}&size=${size}`);
  }

  getArchived(page: number = 0, size: number = 20): Observable<PageResponse<Mail>> {
    return this.http.get<PageResponse<Mail>>(`${this.apiUrl}/api/mails/archived?page=${page}&size=${size}`);
  }

  getTrash(page: number = 0, size: number = 20): Observable<PageResponse<Mail>> {
    return this.http.get<PageResponse<Mail>>(`${this.apiUrl}/api/mails/trash?page=${page}&size=${size}`);
  }

  getMailById(id: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/mails/${id}`);
  }

  // ============= SEARCH =============
  searchMails(query: string, page: number = 0, size: number = 20): Observable<PageResponse<Mail>> {
    return this.http.get<PageResponse<Mail>>(`${this.apiUrl}/api/mails/search?q=${query}&page=${page}&size=${size}`);
  }

  // ============= ACTIONS =============
  performBatchAction(request: MailActionRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/api/mails/actions`, request);
  }

  markAsRead(id: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/api/mails/${id}/read`, {});
  }

  markAsUnread(id: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/api/mails/${id}/unread`, {});
  }

  toggleStar(id: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/api/mails/${id}/star`, {});
  }

  toggleArchive(id: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/api/mails/${id}/archive`, {});
  }

  deleteMail(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/api/mails/${id}`);
  }

  restoreMail(id: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/api/mails/${id}/restore`, {});
  }

  toggleTrash(id:number):  Observable<any> {
    return this.http.post(`${this.apiUrl}/api/mails/${id}/trash`, {});
  }

  // ============= STATISTICS =============
  getStats(): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/mails/stats`);
  }

  // ============= FOLDERS =============
  getFolders(): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/mails/folders`);
  }

  createFolder(name: string, color?: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/api/mails/folders`, { name, color });
  }

  // ============= SIGNATURES =============
  getSignatures(): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/mails/signatures`);
  }

  createSignature(signature: Partial<MailSignature>): Observable<any> {
    return this.http.post(`${this.apiUrl}/api/mails/signatures`, signature);
  }

  setDefaultSignature(id: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/api/mails/signatures/${id}/default`, {});
  }

  downloadAttachment(attachmentId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/api/mails/attachments/${attachmentId}`, {
      responseType: 'blob'
    });
  }

  // ============= GROUPS =============
  getGroups(): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/mails/groups`);
  }

  getGroup(id: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/mails/groups/${id}`);
  }

  createGroup(request: GroupRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/api/mails/groups`, request);
  }

  updateGroup(id: number, request: GroupRequest): Observable<any> {
    return this.http.put(`${this.apiUrl}/api/mails/groups/${id}`, request);
  }

  deleteGroup(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/api/mails/groups/${id}`);
  }

  addGroupMember(groupId: number, memberId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/api/mails/groups/${groupId}/members/${memberId}`, {});
  }

  removeGroupMember(groupId: number, memberId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/api/mails/groups/${groupId}/members/${memberId}`);
  }

  getEmailSuggestions(query: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/mails/groups/suggestions?q=${query}`);
  }


  getGroupMails(groupId: number, page: number = 0, size: number = 20): Observable<PageResponse<Mail>> {
  return this.http.get<PageResponse<Mail>>(`${this.apiUrl}/api/mails/groups/${groupId}/mails?page=${page}&size=${size}`);
}

  getGroupsWithUnread(): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/mails/with-unread`); 
  }
}