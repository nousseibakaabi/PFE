import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConventionHistoryComponent } from './convention-history.component';

describe('ConventionHistoryComponent', () => {
  let component: ConventionHistoryComponent;
  let fixture: ComponentFixture<ConventionHistoryComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ConventionHistoryComponent]
    });
    fixture = TestBed.createComponent(ConventionHistoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
