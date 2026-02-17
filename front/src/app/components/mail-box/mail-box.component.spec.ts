import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MailBoxComponent } from './mail-box.component';

describe('MailBoxComponent', () => {
  let component: MailBoxComponent;
  let fixture: ComponentFixture<MailBoxComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [MailBoxComponent]
    });
    fixture = TestBed.createComponent(MailBoxComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
