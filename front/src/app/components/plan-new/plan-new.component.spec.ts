import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PlanNewComponent } from './plan-new.component';

describe('PlanNewComponent', () => {
  let component: PlanNewComponent;
  let fixture: ComponentFixture<PlanNewComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [PlanNewComponent]
    });
    fixture = TestBed.createComponent(PlanNewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
