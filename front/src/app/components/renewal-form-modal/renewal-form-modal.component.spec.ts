import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RenewalFormModalComponent } from './renewal-form-modal.component';

describe('RenewalFormModalComponent', () => {
  let component: RenewalFormModalComponent;
  let fixture: ComponentFixture<RenewalFormModalComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [RenewalFormModalComponent]
    });
    fixture = TestBed.createComponent(RenewalFormModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
