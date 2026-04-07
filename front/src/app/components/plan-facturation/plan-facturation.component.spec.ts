import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PlanFacturationComponent } from './plan-facturation.component';

describe('PlanFacturationComponent', () => {
  let component: PlanFacturationComponent;
  let fixture: ComponentFixture<PlanFacturationComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [PlanFacturationComponent]
    });
    fixture = TestBed.createComponent(PlanFacturationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
