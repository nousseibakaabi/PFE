import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ChatFullPageComponent } from './chat-full-page.component';

describe('ChatFullPageComponent', () => {
  let component: ChatFullPageComponent;
  let fixture: ComponentFixture<ChatFullPageComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ChatFullPageComponent]
    });
    fixture = TestBed.createComponent(ChatFullPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
