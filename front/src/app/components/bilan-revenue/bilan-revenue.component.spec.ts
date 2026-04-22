import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BilanRevenueComponent } from './bilan-revenue.component';

describe('BilanRevenueComponent', () => {
  let component: BilanRevenueComponent;
  let fixture: ComponentFixture<BilanRevenueComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [BilanRevenueComponent]
    });
    fixture = TestBed.createComponent(BilanRevenueComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
