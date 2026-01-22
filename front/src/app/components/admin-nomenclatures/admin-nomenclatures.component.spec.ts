import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminNomenclaturesComponent } from './admin-nomenclatures.component';

describe('AdminNomenclaturesComponent', () => {
  let component: AdminNomenclaturesComponent;
  let fixture: ComponentFixture<AdminNomenclaturesComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [AdminNomenclaturesComponent]
    });
    fixture = TestBed.createComponent(AdminNomenclaturesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
