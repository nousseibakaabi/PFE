import { Component } from '@angular/core';

@Component({
  selector: 'app-chat-full-page',
  template: '<app-chat-ai></app-chat-ai>',
  styles: [':host { display: block; height: 100vh; width: 100vw; }']
})
export class ChatFullPageComponent {}