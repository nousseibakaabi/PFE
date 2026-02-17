import { MailService, Mail, MailStats, MailFolder, MailRequest, MailActionRequest, MailDraft , MailGroup } from '../../services/mail.service';
import { AuthService } from '../../services/auth.service';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { Subject } from 'rxjs';
import { Component, OnInit, ViewChild, ElementRef, HostListener } from '@angular/core';
@Component({
  selector: 'app-mail-box',
  templateUrl: './mail-box.component.html',
  styleUrls: ['./mail-box.component.css']
})
export class MailBoxComponent implements OnInit {
  @ViewChild('fileInput') fileInput!: ElementRef;

  // Current view - Added 'draft-view' to the union type
  currentView: 'inbox' | 'sent' | 'group-mails' |'groups'| 'folder'|'drafts' | 'starred' | 'archived' | 'trash' | 'compose' | 'view' | 'draft-view' = 'inbox';
  
  selectedDraft: MailDraft | null = null;
  isEditingDraft = false;

  // Data
  mails: Mail[] = [];
  selectedMail: Mail | null = null;
  stats: MailStats | null = null;
  folders: MailFolder[] = [];

  currentFolder: string = 'inbox';
  systemGroups: MailGroup[] = [];
  customGroups: MailGroup[] = [];


  
  // Pagination
  currentPage = 0;
  pageSize = 20;
  totalPages = 0;
  totalElements = 0;
  
  // Compose form
  composeForm: FormGroup;
  attachments: File[] = [];
  showCc = false;
  showBcc = false;
  
  // Selection
  selectedMails: Set<number> = new Set();
  selectAll = false;
  
  // Search
  searchQuery = '';
  searchResults: Mail[] = [];
  isSearching = false;
  
  // Loading states
  loading = false;
  sending = false;
  errorMessage = '';
  successMessage = '';

  // Editor content
  editorContent: string = '';
  previewHtml: SafeHtml = '';
  currentGroup: MailGroup | null = null;


  private unreadUpdateSubject = new Subject<number>();
  unreadUpdate$ = this.unreadUpdateSubject.asObservable();

showGroupsDropdown: boolean = false;

  showAttachmentModal: boolean = false;
selectedAttachment: {
  id: number;
  fileName: string;
  fileType: string;
  fileSize: number;
  downloadUrl: string;
  blob?: Blob; // For storing the actual file data
} | null = null;
attachmentLoading: boolean = false;

URL = window.URL;
textContentCache = new Map<number, string>();

  constructor(
    public mailService: MailService,
    public authService: AuthService,
    private fb: FormBuilder,
    private sanitizer: DomSanitizer
  ) {
    this.composeForm = this.fb.group({
      to: ['', [Validators.required]],
      cc: [''],
      bcc: [''],
      subject: ['', Validators.required],
      content: ['', Validators.required],
      importance: ['NORMAL']
    });
  }

  ngOnInit(): void {
    this.loadStats();
    this.loadInbox();
    this.loadFolders();
  this.loadGroupsWithStats();
  }

  // ============= LOADING DATA =============
  loadStats(): void {
    this.mailService.getStats().subscribe({
      next: (response) => {
        if (response.success) {
          this.stats = response.data;
        }
      },
      error: (error) => console.error('Error loading stats:', error)
    });
  }

  loadInbox(): void {
    this.loading = true;
    this.currentView = 'inbox';
    this.mailService.getInbox(this.currentPage, this.pageSize).subscribe({
      next: (response) => {
        if (response.success) {
          this.mails = response.data;
          this.totalPages = response.totalPages;
          this.totalElements = response.totalElements;
          this.clearSelection();
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading inbox:', error);
        this.loading = false;
        this.errorMessage = 'Failed to load inbox';
      }
    });
  }

  loadSent(): void {
    this.loading = true;
    this.currentView = 'sent';
    this.mailService.getSent(this.currentPage, this.pageSize).subscribe({
      next: (response) => {
        if (response.success) {
          this.mails = response.data;
          this.totalPages = response.totalPages;
          this.totalElements = response.totalElements;
          this.clearSelection();
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading sent:', error);
        this.loading = false;
      }
    });
  }

  loadStarred(): void {
    this.loading = true;
    this.currentView = 'starred';
    this.mailService.getStarred(this.currentPage, this.pageSize).subscribe({
      next: (response) => {
        if (response.success) {
          this.mails = response.data;
          this.totalPages = response.totalPages;
          this.totalElements = response.totalElements;
          this.clearSelection();
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading starred:', error);
        this.loading = false;
      }
    });
  }

  loadArchived(): void {
    this.loading = true;
    this.currentView = 'archived';
    this.mailService.getArchived(this.currentPage, this.pageSize).subscribe({
      next: (response) => {
        if (response.success) {
          this.mails = response.data;
          this.totalPages = response.totalPages;
          this.totalElements = response.totalElements;
          this.clearSelection();
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading archived:', error);
        this.loading = false;
      }
    });
  }

  loadDeleted(): void {
    this.loading = true;
    this.currentView = 'trash';
    this.currentPage = 0;
    this.mailService.getTrash(this.currentPage, this.pageSize).subscribe({
      next: (response) => {
        if (response.success) {
          this.mails = response.data;
          this.totalPages = response.totalPages;
          this.totalElements = response.totalElements;
          this.clearSelection();
          console.log('Trash mails loaded:', this.mails);
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading trash:', error);
        this.loading = false;
        this.errorMessage = 'Failed to load trash';
      }
    });
  }

  loadFolders(): void {
    this.mailService.getFolders().subscribe({
      next: (response) => {
        if (response.success) {
          this.folders = response.data;
        }
      },
      error: (error) => console.error('Error loading folders:', error)
    });
  }









  // ============= SEARCH =============
  onSearch(): void {
    if (!this.searchQuery.trim()) {
      this.isSearching = false;
      this.refreshCurrentView();
      return;
    }

    this.isSearching = true;
    this.loading = true;
    this.mailService.searchMails(this.searchQuery, this.currentPage, this.pageSize).subscribe({
      next: (response) => {
        if (response.success) {
          this.mails = response.data;
          this.totalPages = response.totalPages;
          this.totalElements = response.totalElements;
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error searching:', error);
        this.loading = false;
      }
    });
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.isSearching = false;
    this.refreshCurrentView();
  }

  // ============= COMPOSE =============
  openCompose(): void {
    this.currentView = 'compose';
    this.composeForm.reset({
      importance: 'NORMAL'
    });
    this.attachments = [];
    this.showCc = false;
    this.showBcc = false;
    this.isEditingDraft = false;
    this.selectedDraft = null;
    this.errorMessage = '';
  }

  openReply(mail: Mail): void {
    this.currentView = 'compose';
    this.composeForm.reset({
      to: mail.senderEmail,
      subject: `Re: ${mail.subject}`,
      importance: 'NORMAL'
    });
    this.attachments = [];
    this.showCc = false;
    this.showBcc = false;
    this.isEditingDraft = false;
    this.selectedDraft = null;
  }

  openReplyAll(mail: Mail): void {
    const allRecipients = [
      mail.senderEmail,
      ...mail.recipients.filter(r => r.type === 'TO').map(r => r.email)
    ].filter((v, i, a) => a.indexOf(v) === i);

    this.currentView = 'compose';
    this.composeForm.reset({
      to: allRecipients.join(', '),
      subject: `Re: ${mail.subject}`,
      importance: 'NORMAL'
    });
    this.attachments = [];
    this.showCc = false;
    this.showBcc = false;
    this.isEditingDraft = false;
    this.selectedDraft = null;
  }

  openForward(mail: Mail): void {
    this.currentView = 'compose';
    this.composeForm.reset({
      subject: `Fwd: ${mail.subject}`,
      content: `\n\n--- Forwarded message ---\nFrom: ${mail.senderName} <${mail.senderEmail}>\nDate: ${new Date(mail.sentAt).toLocaleString()}\nSubject: ${mail.subject}\n\n${mail.content}`,
      importance: 'NORMAL'
    });
    this.attachments = [];
    this.showCc = false;
    this.showBcc = false;
    this.isEditingDraft = false;
    this.selectedDraft = null;
  }

  onFileSelected(event: any): void {
    const files: FileList = event.target.files;
    for (let i = 0; i < files.length; i++) {
      this.attachments.push(files[i]);
    }
  }

  removeAttachment(index: number): void {
    this.attachments.splice(index, 1);
  }

sendMail(): void {
  if (this.composeForm.invalid) {
    this.errorMessage = 'Please fill all required fields';
    return;
  }

  this.sending = true;
  this.errorMessage = '';

  const formValue = this.composeForm.value;
  
  // Handle 'to' - could be string or array
  let toEmails: string[] = [];
  if (formValue.to) {
    if (Array.isArray(formValue.to)) {
      // It's already an array from the email-input component
      toEmails = formValue.to;
    } else if (typeof formValue.to === 'string') {
      // It's a string (comma-separated)
      toEmails = formValue.to.split(',').map((email: string) => email.trim());
    }
  }

  const request: MailRequest = {
    subject: formValue.subject,
    content: formValue.content,
    to: toEmails,
    importance: formValue.importance
  };

  // Handle CC
  if (formValue.cc && this.showCc) {
    if (Array.isArray(formValue.cc)) {
      request.cc = formValue.cc;
    } else if (typeof formValue.cc === 'string') {
      request.cc = formValue.cc.split(',').map((email: string) => email.trim());
    }
  }

  // Handle BCC
  if (formValue.bcc && this.showBcc) {
    if (Array.isArray(formValue.bcc)) {
      request.bcc = formValue.bcc;
    } else if (typeof formValue.bcc === 'string') {
      request.bcc = formValue.bcc.split(',').map((email: string) => email.trim());
    }
  }

  this.mailService.sendMail(request, this.attachments).subscribe({
    next: (response) => {
      if (response.success) {
        this.successMessage = 'Email sent successfully';
        setTimeout(() => {
          this.currentView = 'sent';
          this.loadSent();
          this.loadStats();
        }, 1500);
      }
      this.sending = false;
    },
    error: (error) => {
      console.error('Error sending mail:', error);
      this.errorMessage = error.error?.message || 'Failed to send email';
      this.sending = false;
    }
  });
}

  // ============= MAIL ACTIONS =============
  toggleSelectAll(): void {
    this.selectAll = !this.selectAll;
    if (this.selectAll) {
      this.mails.forEach(mail => this.selectedMails.add(mail.id));
    } else {
      this.selectedMails.clear();
    }
  }

  toggleSelect(mailId: number): void {
    if (this.selectedMails.has(mailId)) {
      this.selectedMails.delete(mailId);
      this.selectAll = false;
    } else {
      this.selectedMails.add(mailId);
      if (this.selectedMails.size === this.mails.length) {
        this.selectAll = true;
      }
    }
  }

  clearSelection(): void {
    this.selectedMails.clear();
    this.selectAll = false;
  }

  private getActionMessage(action: string): string {
    const messages: { [key: string]: string } = {
      'READ': 'marked as read',
      'UNREAD': 'marked as unread',
      'STAR': 'starred',
      'UNSTAR': 'unstarred',
      'ARCHIVE': 'archived',
      'UNARCHIVE': 'unarchived',
      'DELETE': 'moved to trash',
      'RESTORE': 'restored'
    };
    return messages[action] || action.toLowerCase() + 'ed';
  }


  deleteMail(id: number): void {
    this.mailService.deleteMail(id).subscribe({
      next: (response) => {
        if (response.success) {
          this.successMessage = 'Mail moved to trash';
          this.refreshCurrentView();
          this.loadStats();
        }
      },
      error: (error) => {
        console.error('Error deleting mail:', error);
        this.errorMessage = error.error?.message || 'Failed to delete mail';
      }
    });
  }

  performAction(action: 'READ' | 'UNREAD' | 'STAR' | 'UNSTAR' | 'ARCHIVE' | 'UNARCHIVE' | 'DELETE' | 'RESTORE'): void {
    if (this.selectedMails.size === 0) return;

    const request: MailActionRequest = {
      mailIds: Array.from(this.selectedMails),
      action: action
    };

    this.mailService.performBatchAction(request).subscribe({
      next: (response) => {
        if (response.success) {
          this.successMessage = `Mails ${this.getActionMessage(action)}`;
          this.refreshCurrentView();
          this.clearSelection();
          this.loadStats();
        }
      },
      error: (error) => {
        console.error('Error performing action:', error);
        this.errorMessage = error.error?.message || 'Action failed';
      }
    });
  }





  onPageChange(page: number): void {
    this.currentPage = page;
    this.refreshCurrentView();
  }





  // In mail-box.component.ts, update the loadGroupsWithStats method:

loadGroupsWithStats(): void {
  this.mailService.getGroupsWithUnread().subscribe({
    next: (response) => {
      if (response.success) {
        const groups = response.data;
        this.systemGroups = groups.filter((g: MailGroup) => g.isSystem);
        this.customGroups = groups.filter((g: MailGroup) => !g.isSystem);
        
        // Log for debugging
        console.log('System groups with stats:', this.systemGroups);
        console.log('Custom groups with stats:', this.customGroups);
      }
    },
    error: (error) => {
      console.error('Error loading groups with stats:', error);
      // Fallback to regular groups if with-unread fails
      this.loadGroups();
    }
  });
}

// Keep the original loadGroups as fallback
loadGroups(): void {
  this.mailService.getGroups().subscribe({
    next: (response) => {
      if (response.success) {
        const groups = response.data;
        this.systemGroups = groups.filter((g: MailGroup) => g.isSystem);
        this.customGroups = groups.filter((g: MailGroup) => !g.isSystem);
      }
    },
    error: (error) => console.error('Error loading groups:', error)
  });
}






  // ============= UTILITIES =============
  formatDate(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diffTime = Math.abs(now.getTime() - date.getTime());
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    if (diffDays === 0) {
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } else if (diffDays === 1) {
      return 'Yesterday';
    } else if (diffDays < 7) {
      return date.toLocaleDateString([], { weekday: 'short' });
    } else {
      return date.toLocaleDateString([], { day: '2-digit', month: '2-digit', year: 'numeric' });
    }
  }

  getRecipientsSummary(mail: Mail): string {
    const to = mail.recipients.filter(r => r.type === 'TO').map(r => r.name || r.email);
    if (to.length === 1) return to[0];
    if (to.length === 2) return to.join(' & ');
    return `${to[0]} and ${to.length - 1} others`;
  }

  getInitials(name: string): string {
    if (!name) return '?';
    return name.split(' ')
      .map(n => n[0])
      .join('')
      .toUpperCase()
      .substring(0, 2);
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  getSenderNameOrEmail(mail: Mail): string {
    if (this.currentView === 'sent') {
      return this.getRecipientsSummary(mail);
    }
    return mail.senderName || mail.senderEmail;
  }

  getRecipientName(recipient: any): string {
    return recipient.name || recipient.email;
  }

  getToRecipients(mail: Mail): string {
    if (!mail || !mail.recipients) return '';
    return mail.recipients
      .filter(r => r.type === 'TO')
      .map(r => r.name || r.email)
      .join(', ');
  }

  getCcRecipients(mail: Mail): string {
    if (!mail || !mail.recipients) return '';
    return mail.recipients
      .filter(r => r.type === 'CC')
      .map(r => r.name || r.email)
      .join(', ');
  }

  hasToRecipients(mail: Mail): boolean {
    return mail && mail.recipients ? mail.recipients.filter(r => r.type === 'TO').length > 0 : false;
  }

  hasCcRecipients(mail: Mail): boolean {
    return mail && mail.recipients ? mail.recipients.filter(r => r.type === 'CC').length > 0 : false;
  }

  // ============= DRAFT METHODS =============
  loadDraftDetail(id: number): void {
    this.loading = true;
    this.mailService.getDraftById(id).subscribe({
      next: (response) => {
        if (response.success) {
          this.selectedDraft = response.data;
          this.currentView = 'draft-view';
          console.log('Draft loaded:', this.selectedDraft);
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading draft:', error);
        this.loading = false;
        this.errorMessage = 'Failed to load draft';
      }
    });
  }

  editDraft(draft: MailDraft): void {
    this.currentView = 'compose';
    this.isEditingDraft = true;
    
    this.composeForm.patchValue({
      to: draft.to ? draft.to.join(', ') : '',
      cc: draft.cc ? draft.cc.join(', ') : '',
      bcc: draft.bcc ? draft.bcc.join(', ') : '',
      subject: draft.subject || '',
      content: draft.content || '',
      importance: 'NORMAL'
    });
    
    this.showCc = draft.cc && draft.cc.length > 0;
    this.showBcc = draft.bcc && draft.bcc.length > 0;
    
    this.selectedDraft = draft;
  }

  deleteDraft(id: number): void {
    if (confirm('Are you sure you want to delete this draft?')) {
      this.loading = true;
      this.mailService.deleteDraft(id).subscribe({
        next: (response) => {
          if (response.success) {
            this.successMessage = 'Draft deleted successfully';
            this.loadDrafts();
            this.goBack();
          }
          this.loading = false;
        },
        error: (error) => {
          console.error('Error deleting draft:', error);
          this.errorMessage = error.error?.message || 'Failed to delete draft';
          this.loading = false;
        }
      });
    }
  }

saveDraft(): void {
  if (this.composeForm.invalid) {
    this.errorMessage = 'Please fill required fields';
    return;
  }

  this.sending = true;
  this.errorMessage = '';

  const formValue = this.composeForm.value;
  
  // Handle 'to' - could be string or array
  let toEmails: string[] = [];
  if (formValue.to) {
    if (Array.isArray(formValue.to)) {
      toEmails = formValue.to;
    } else if (typeof formValue.to === 'string') {
      toEmails = formValue.to.split(',').map((email: string) => email.trim());
    }
  }

  // Handle CC
  let ccEmails: string[] = [];
  if (formValue.cc && this.showCc) {
    if (Array.isArray(formValue.cc)) {
      ccEmails = formValue.cc;
    } else if (typeof formValue.cc === 'string') {
      ccEmails = formValue.cc.split(',').map((email: string) => email.trim());
    }
  }

  // Handle BCC
  let bccEmails: string[] = [];
  if (formValue.bcc && this.showBcc) {
    if (Array.isArray(formValue.bcc)) {
      bccEmails = formValue.bcc;
    } else if (typeof formValue.bcc === 'string') {
      bccEmails = formValue.bcc.split(',').map((email: string) => email.trim());
    }
  }

  const draftRequest: Partial<MailRequest> = {
    subject: formValue.subject,
    content: formValue.content,
    to: toEmails,
    cc: ccEmails,
    bcc: bccEmails,
    importance: formValue.importance
  };

  console.log('Saving draft:', draftRequest);
  console.log('Attachments:', this.attachments);

  const saveObservable = this.isEditingDraft && this.selectedDraft
    ? this.mailService.updateDraft(this.selectedDraft.id, draftRequest, this.attachments)
    : this.mailService.saveDraft(draftRequest, this.attachments);

  saveObservable.subscribe({
    next: (response) => {
      console.log('Draft saved response:', response);
      if (response.success) {
        this.successMessage = this.isEditingDraft ? 'Draft updated successfully' : 'Draft saved successfully';
        this.sending = false;
        this.isEditingDraft = false;
        this.selectedDraft = null;
        
        setTimeout(() => {
          this.goBack();
          this.loadStats();
        }, 1500);
      }
    },
    error: (error) => {
      console.error('Error saving draft:', error);
      this.errorMessage = error.error?.message || 'Failed to save draft';
      this.sending = false;
    }
  });
}


  loadDrafts(): void {
    this.loading = true;
    this.currentView = 'drafts';
    this.currentPage = 0;
    console.log('Loading drafts...');
    this.mailService.getDrafts().subscribe({
      next: (response) => {
        console.log('Drafts response:', response);
        if (response.success) {
          this.mails = response.data;
          console.log('Drafts loaded:', this.mails);
          this.clearSelection();
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading drafts:', error);
        this.loading = false;
        this.errorMessage = 'Failed to load drafts';
      }
    });
  }

  sendFromDraft(): void {
  if (!this.selectedDraft) return;
  
  if (this.composeForm.invalid) {
    this.errorMessage = 'Please fill all required fields';
    return;
  }

  this.sending = true;
  this.errorMessage = '';

  const formValue = this.composeForm.value;
  
  // Handle 'to' - could be string or array
  let toEmails: string[] = [];
  if (formValue.to) {
    if (Array.isArray(formValue.to)) {
      toEmails = formValue.to;
    } else if (typeof formValue.to === 'string') {
      toEmails = formValue.to.split(',').map((email: string) => email.trim());
    }
  }

  const request: MailRequest = {
    subject: formValue.subject,
    content: formValue.content,
    to: toEmails,
    importance: formValue.importance,
    draftId: this.selectedDraft.id
  };

  // Handle CC
  if (formValue.cc && this.showCc) {
    if (Array.isArray(formValue.cc)) {
      request.cc = formValue.cc;
    } else if (typeof formValue.cc === 'string') {
      request.cc = formValue.cc.split(',').map((email: string) => email.trim());
    }
  }

  // Handle BCC
  if (formValue.bcc && this.showBcc) {
    if (Array.isArray(formValue.bcc)) {
      request.bcc = formValue.bcc;
    } else if (typeof formValue.bcc === 'string') {
      request.bcc = formValue.bcc.split(',').map((email: string) => email.trim());
    }
  }

  this.mailService.sendFromDraft(this.selectedDraft.id, request, this.attachments).subscribe({
    next: (response) => {
      if (response.success) {
        this.successMessage = 'Email sent successfully';
        this.isEditingDraft = false;
        this.selectedDraft = null;
        setTimeout(() => {
          this.currentView = 'sent';
          this.loadSent();
          this.loadStats();
        }, 1500);
      }
      this.sending = false;
    },
    error: (error) => {
      console.error('Error sending mail:', error);
      this.errorMessage = error.error?.message || 'Failed to send email';
      this.sending = false;
    }
  });
}


  // Add these helper methods for draft handling
getDraftRecipientsText(item: any): string {
  // Check if it's a draft (has 'to' property)
  if (item && 'to' in item) {
    const draft = item as MailDraft;
    if (draft.to && draft.to.length > 0) {
      if (draft.to.length > 2) {
        return draft.to[0] + ' and ' + (draft.to.length - 1) + ' others';
      } else {
        return draft.to.join(', ');
      }
    }
  }
  return 'No recipients';
}

getDraftDate(item: any): string {
  if (item && 'lastSavedAt' in item) {
    const draft = item as MailDraft;
    return this.formatDate(draft.lastSavedAt);
  }
  return '';
}

hasDraftAttachments(item: any): boolean {
  if (item && 'attachments' in item) {
    const draft = item as MailDraft;
    return draft.attachments && draft.attachments.length > 0;
  }
  return false;
}

getDraftAttachmentsCount(item: any): number {
  if (item && 'attachments' in item) {
    const draft = item as MailDraft;
    return draft.attachments ? draft.attachments.length : 0;
  }
  return 0;
}


// Add this method to open attachment in modal
openAttachment(attachment: any, event?: Event): void {
  if (event) {
    event.stopPropagation(); // Prevent triggering the mail click
  }
  
  this.attachmentLoading = true;
  this.showAttachmentModal = true;
  this.selectedAttachment = attachment;
  
  // If it's a draft attachment (no downloadUrl), we need to handle differently
  if (!attachment.downloadUrl && attachment.id) {
    // For draft attachments, you might need to fetch the file
    this.loadAttachmentContent(attachment.id);
  } else {
    // For regular mail attachments, we can fetch the content
    this.fetchAttachmentContent(attachment.id, attachment.fileName, attachment.fileType);
  }
}

// Fetch attachment content from server
fetchAttachmentContent(attachmentId: number, fileName: string, fileType: string): void {
  this.mailService.downloadAttachment(attachmentId).subscribe({
    next: (blob: Blob) => {
      if (this.selectedAttachment) {
        this.selectedAttachment.blob = blob;
      }
      this.attachmentLoading = false;
    },
    error: (error) => {
      console.error('Error loading attachment:', error);
      this.errorMessage = 'Failed to load attachment';
      this.attachmentLoading = false;
      this.closeAttachmentModal();
    }
  });
}

// For draft attachments (if needed)
loadAttachmentContent(attachmentId: number): void {
  // Implement this if you have a separate endpoint for draft attachments
  // this.mailService.getDraftAttachment(attachmentId).subscribe...
  this.attachmentLoading = false;
}

// Close modal
closeAttachmentModal(): void {
  this.showAttachmentModal = false;
  this.selectedAttachment = null;
}

// Download the attachment
downloadAttachment(attachment: any, event?: Event): void {
  if (event) {
    event.stopPropagation();
  }
  
  if (attachment.blob) {
    // If we already have the blob, use it
    const url = window.URL.createObjectURL(attachment.blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = attachment.fileName;
    link.click();
    window.URL.revokeObjectURL(url);
  } else if (attachment.downloadUrl) {
    // If we have a downloadUrl, use it
    window.open(attachment.downloadUrl, '_blank');
  }
}


getTextContent(blob: Blob): string {
  if (!blob) return '';
  
  // Use a unique identifier (you might want to use attachment id)
  const key = this.selectedAttachment?.id || 0;
  
  // Return cached content if available
  if (this.textContentCache.has(key)) {
    return this.textContentCache.get(key) || '';
  }
  
  // Read the blob and cache it
  const reader = new FileReader();
  reader.onload = (e) => {
    const content = e.target?.result as string;
    this.textContentCache.set(key, content);
    // Trigger change detection
    this.textContentCache = new Map(this.textContentCache);
  };
  reader.readAsText(blob);
  
  return 'Loading text content...';
}


openFolder(folder: string): void {
  this.currentFolder = folder;
  this.currentView = 'folder';
  // Load mails for this folder - implement based on your folder logic
  this.loadFolderMails(folder);
}



loadFolderMails(folder: string): void {
  this.loading = true;
  // Implement folder-specific mail loading
  // This depends on how you've implemented folders in your backend
  this.loading = false;
}


markMailAsRead(mail: Mail): void {
  if (!mail.isRead) {
    this.mailService.markAsRead(mail.id).subscribe({
      next: () => {
        mail.isRead = true;
        
        // Update the mail in the list
        const index = this.mails.findIndex(m => m.id === mail.id);
        if (index !== -1) {
          this.mails[index].isRead = true;
        }
        
        // Update stats in real-time
        if (this.stats) {
          this.stats.unreadCount = Math.max(0, this.stats.unreadCount - 1);
        }
        
        // Update group unread counts if in group view
        if (this.currentGroup) {
          this.currentGroup.unreadCount = this.mails.filter(m => !m.isRead).length;
          
          // Also update the group in the sidebar lists
          const groupInSystem = this.systemGroups.find(g => g.id === this.currentGroup?.id);
          const groupInCustom = this.customGroups.find(g => g.id === this.currentGroup?.id);
          
          if (groupInSystem) {
            groupInSystem.unreadCount = this.currentGroup.unreadCount;
          }
          if (groupInCustom) {
            groupInCustom.unreadCount = this.currentGroup.unreadCount;
          }
        }
      }
    });
  }
}




// Add this method to toggle dropdown
toggleGroupsDropdown(): void {
  this.showGroupsDropdown = !this.showGroupsDropdown;
}

// Add this method to handle group selection from dropdown
selectGroupFromDropdown(group: MailGroup): void {
  this.showGroupsDropdown = false; // Close dropdown
  this.loadGroupMails(group); // Load group mails
}

// Close dropdown when clicking outside (optional)
@HostListener('document:click', ['$event'])
onDocumentClick(event: MouseEvent): void {
  const target = event.target as HTMLElement;
  if (!target.closest('.relative')) {
    this.showGroupsDropdown = false;
  }
}


// Add method to update group unread count
updateGroupUnreadCount(): void {
  if (this.currentGroup && this.stats) {
    // You might want to call an API to get updated group unread count
    // For now, we'll just update locally
    this.currentGroup.unreadCount = this.mails.filter(m => !m.isRead).length;
  }
}

// Update loadMailDetail to use the real-time update
loadMailDetail(id: number): void {
  this.loading = true;
  this.mailService.getMailById(id).subscribe({
    next: (response) => {
      if (response.success) {
        this.selectedMail = response.data;
        this.currentView = 'view';
        if (this.selectedMail) {
          this.previewHtml = this.sanitizer.bypassSecurityTrustHtml(this.selectedMail.content);
          
          // Mark as read if it was unread
          if (!this.selectedMail.isRead) {
            this.markMailAsRead(this.selectedMail);
          }
        }
      }
      this.loading = false;
    },
    error: (error) => {
      console.error('Error loading mail:', error);
      this.loading = false;
    }
  });
}

// Also update toggleStar to update UI immediately
toggleMailStar(mail: Mail, event: Event): void {
  event.stopPropagation();
  const previousState = mail.isStarred;
  mail.isStarred = !mail.isStarred; // Optimistic update
  
  this.mailService.toggleStar(mail.id).subscribe({
    next: () => {
      // Update stats if needed
      if (this.stats) {
        if (mail.isStarred) {
          this.stats.starredCount++;
        } else {
          this.stats.starredCount = Math.max(0, this.stats.starredCount - 1);
        }
      }
    },
    error: (error) => {
      console.error('Error toggling star:', error);
      mail.isStarred = previousState; // Revert on error
    }
  });
}



// Update loadGroupMails method
loadGroupMails(group: MailGroup): void {
  console.log('Loading group mails for:', group);
  this.currentGroup = group;
  this.currentView = 'group-mails';
  this.currentPage = 0;
  this.loading = true;
  
  this.mailService.getGroupMails(group.id, this.currentPage, this.pageSize).subscribe({
    next: (response) => {
      console.log('Group mails response:', response);
      if (response.success) {
        this.mails = response.data;
        this.totalPages = response.totalPages;
        this.totalElements = response.totalElements;
        this.clearSelection();
        
        // Update unread count for this group
        if (this.currentGroup) {
          this.currentGroup.unreadCount = this.mails.filter(m => !m.isRead).length;
        }
      }
      this.loading = false;
    },
    error: (error) => {
      console.error('Error loading group mails:', error);
      this.loading = false;
      this.errorMessage = 'Failed to load group mails';
    }
  });
}

// Also update the refreshCurrentView method to reload groups when needed
refreshCurrentView(): void {
  this.currentPage = 0;
  switch (this.currentView) {
    case 'inbox':
      this.loadInbox();
      break;
    case 'sent':
      this.loadSent();
      break;
    case 'starred':
      this.loadStarred();
      break;
    case 'archived':
      this.loadArchived();
      break;
    case 'trash':
      this.loadDeleted();  
      break;
    case 'drafts':
      this.loadDrafts();
      break;
    case 'groups':
      this.loadGroupsWithStats();
      break;
    default:
      this.loadInbox();
      break;
  }
}

// Update goBack method to handle groups view
goBack(): void {
  if (this.currentView === 'view' || this.currentView === 'draft-view') {
    this.selectedMail = null;
    this.selectedDraft = null;
    this.refreshCurrentView();
  } else if (this.currentView === 'compose') {
    this.isEditingDraft = false;
    this.selectedDraft = null;
    this.refreshCurrentView();
  } else if (this.currentView === 'group-mails') {
    this.currentGroup = null;
    this.currentView = 'inbox';
    this.loadInbox();
  } else if (this.currentView === 'groups') {
    this.currentView = 'inbox';
    this.loadInbox();
  }
}

// Add method to open groups manager
openGroupsManager(): void {
  this.currentView = 'groups';
  this.loadGroupsWithStats();
}




}