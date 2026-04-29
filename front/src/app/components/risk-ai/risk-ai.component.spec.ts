import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RiskAIComponent } from './risk-ai.component';

describe('RiskAIComponent', () => {
  let component: RiskAIComponent;
  let fixture: ComponentFixture<RiskAIComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [RiskAIComponent]
    });
    fixture = TestBed.createComponent(RiskAIComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
