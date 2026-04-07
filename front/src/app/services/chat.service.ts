import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private apiUrl = `${environment.apiUrl}/api/chat`;

  constructor(private http: HttpClient) {}

  askQuestion(question: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/ask`, { question });
  }

  getExampleQuestions(): Observable<any> {
    return this.http.get(`${this.apiUrl}/examples`);
  }
}