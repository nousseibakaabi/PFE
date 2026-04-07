import { Component, OnInit, OnDestroy, ElementRef, ViewChild, AfterViewChecked } from '@angular/core';
import { ChatService } from '../../services/chat.service';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';

export interface ChatMessage {
  id: string;
  text: string;
  isUser: boolean;
  timestamp: Date;
  isLoading?: boolean;
  isFirstInGroup?: boolean;
  isLastInGroup?: boolean;
  isSent?: boolean;
}

@Component({
  selector: 'app-chat-ai',
  templateUrl: './chat-ai.component.html',
  styleUrls: ['./chat-ai.component.css']
})
export class ChatAiComponent implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('chatMessages') private chatMessagesContainer!: ElementRef;
  @ViewChild('messageInput') private messageInput!: ElementRef;
  
  isOpen: boolean = false;
  isMinimized: boolean = false;
  isFullPage: boolean = false;
  currentMessage: string = '';
  messages: ChatMessage[] = [];
  isLoading: boolean = false;
  exampleQuestions: any[] = [];
  unreadCount: number = 0;
  showTypingHint: boolean = true;
  
  private welcomeMessageSent: boolean = false;
  private shouldScrollToBottom: boolean = false;

  constructor(
    private chatService: ChatService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadExampleQuestions();
    this.loadUnreadCount();
    
    // Vérifier si on est en mode page entière
    if (this.router.url === '/chat-full') {
      this.isOpen = true;
      this.isFullPage = true;
    }
    
    setTimeout(() => {
      if (!this.welcomeMessageSent && !this.isOpen) {
        this.unreadCount++;
      }
    }, 2000);
  }

  ngOnDestroy(): void {}

  ngAfterViewChecked(): void {
    if (this.shouldScrollToBottom) {
      this.scrollToBottom();
      this.shouldScrollToBottom = false;
    }
  }

  loadUnreadCount(): void {
    const savedMessages = localStorage.getItem('chat_messages_read');
    if (savedMessages) {
      const readIds = JSON.parse(savedMessages);
      this.unreadCount = this.messages.filter(m => !m.isUser && !readIds.includes(m.id)).length;
    }
  }

  loadExampleQuestions(): void {
    this.chatService.getExampleQuestions().subscribe({
      next: (response) => {
        this.exampleQuestions = response.questions || [];
      },
      error: () => {
        this.exampleQuestions = [
          { question: "Quelle est la date d'échéance de la facture FACT-2026-CONV-2026-005-001 ?", icon: "📅" },
          { question: "Combien de factures sont impayées ?", icon: "💰" },
          { question: "Quel est le montant total des conventions en cours ?", icon: "📊" },
          { question: "Quelles sont les conventions qui expirent ce mois-ci ?", icon: "⚠️" },
          { question: "Montrer les applications avec leurs chefs de projet", icon: "👥" }
        ];
      }
    });
  }

  addWelcomeMessage(): void {
    const user = this.authService.currentUserValue;
    const userName = user?.firstName || user?.username || 'utilisateur';
    
    this.messages.push({
      id: Date.now().toString(),
      text: `Bonjour ${userName} ! 👋\n\nJe suis votre assistant IA. Je peux répondre à vos questions sur:\n\n• 📅 **Factures** (dates d'échéance, statuts)\n• 💰 **Paiements** (impayés, montants)\n• 📊 **Conventions** (expirations, totaux)\n• 👥 **Applications** (chefs de projet, clients)\n\nComment puis-je vous aider aujourd'hui ?`,
      isUser: false,
      timestamp: new Date(),
      isFirstInGroup: true,
      isLastInGroup: true,
      isSent: true
    });
    this.welcomeMessageSent = true;
    this.shouldScrollToBottom = true;
    this.markMessagesAsRead();
  }

  toggleChat(): void {
    this.isOpen = !this.isOpen;
    if (this.isOpen) {
      this.isMinimized = false;
      setTimeout(() => {
        if (this.messageInput) {
          this.messageInput.nativeElement.focus();
        }
        this.scrollToBottom();
      }, 300);
      this.markMessagesAsRead();
    }
  }

  openFullPage(): void {
    this.router.navigate(['/chat-full']);
  }

  closeFullPage(): void {
    this.router.navigate(['/']);
  }

  toggleMinimize(): void {
    this.isMinimized = !this.isMinimized;
  }

  openAttachMenu(): void {
    console.log('Menu attaché - à implémenter plus tard');
  }

  autoResize(event: any): void {
    const textarea = event.target;
    textarea.style.height = 'auto';
    textarea.style.height = Math.min(textarea.scrollHeight, 100) + 'px';
  }

  markMessagesAsRead(): void {
    const readIds = this.messages.filter(m => !m.isUser).map(m => m.id);
    localStorage.setItem('chat_messages_read', JSON.stringify(readIds));
    this.unreadCount = 0;
  }

  sendMessage(): void {
    const messageText = this.currentMessage?.trim();
    if (!messageText || this.isLoading) return;
    
    // Marquer le message précédent comme dernier du groupe
    if (this.messages.length > 0) {
      const lastMsg = this.messages[this.messages.length - 1];
      if (lastMsg.isUser === true) {
        lastMsg.isLastInGroup = false;
      }
    }
    
    // Ajouter le message de l'utilisateur
    const userMessage: ChatMessage = {
      id: Date.now().toString(),
      text: messageText,
      isUser: true,
      timestamp: new Date(),
      isFirstInGroup: this.messages.length === 0 || !this.messages[this.messages.length - 1]?.isUser,
      isLastInGroup: true,
      isSent: false
    };
    this.messages.push(userMessage);
    
    this.currentMessage = '';
    this.shouldScrollToBottom = true;
    this.isLoading = true;
    this.showTypingHint = false;
    
    // Simuler l'envoi
    setTimeout(() => {
      userMessage.isSent = true;
    }, 500);
    
    // Appel API
    this.chatService.askQuestion(messageText).subscribe({
      next: (response) => {
        userMessage.isSent = true;
        
        const answer = response.answer || response.data?.answer || "Désolé, je n'ai pas pu traiter votre question.";
        
        const botMessage: ChatMessage = {
          id: Date.now().toString(),
          text: answer,
          isUser: false,
          timestamp: new Date(),
          isFirstInGroup: true,
          isLastInGroup: true,
          isSent: true
        };
        this.messages.push(botMessage);
        this.isLoading = false;
        this.shouldScrollToBottom = true;
        
        if (!this.isOpen) {
          this.unreadCount++;
        }
      },
      error: (error) => {
        userMessage.isSent = true;
        const errorMessage: ChatMessage = {
          id: Date.now().toString(),
          text: "❌ Désolé, une erreur s'est produite. Veuillez réessayer.",
          isUser: false,
          timestamp: new Date(),
          isFirstInGroup: true,
          isLastInGroup: true,
          isSent: true
        };
        this.messages.push(errorMessage);
        this.isLoading = false;
        this.shouldScrollToBottom = true;
      }
    });
  }

  useExampleQuestion(question: string): void {
    this.currentMessage = question;
    this.sendMessage();
  }

  scrollToBottom(): void {
    setTimeout(() => {
      if (this.chatMessagesContainer) {
        this.chatMessagesContainer.nativeElement.scrollTop = 
          this.chatMessagesContainer.nativeElement.scrollHeight;
      }
    }, 100);
  }

  formatTime(date: Date): string {
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    
    if (diffMins < 1) return "À l'instant";
    if (diffMins < 60) return `Il y a ${diffMins} min`;
    if (diffHours < 24) return `Il y a ${diffHours} h`;
    
    return date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  }
}