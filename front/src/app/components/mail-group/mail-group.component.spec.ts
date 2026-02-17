import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MailGroupComponent } from './mail-group.component';

describe('MailGroupComponent', () => {
  let component: MailGroupComponent;
  let fixture: ComponentFixture<MailGroupComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [MailGroupComponent]
    });
    fixture = TestBed.createComponent(MailGroupComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
