import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TwoFactorSetupComponent } from './two-factor-setup.component';

describe('TwoFactorSetupComponent', () => {
  let component: TwoFactorSetupComponent;
  let fixture: ComponentFixture<TwoFactorSetupComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [TwoFactorSetupComponent]
    });
    fixture = TestBed.createComponent(TwoFactorSetupComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
