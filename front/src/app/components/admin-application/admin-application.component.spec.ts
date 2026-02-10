import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminApplicationComponent } from './admin-application.component';

describe('AdminApplicationComponent', () => {
  let component: AdminApplicationComponent;
  let fixture: ComponentFixture<AdminApplicationComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [AdminApplicationComponent]
    });
    fixture = TestBed.createComponent(AdminApplicationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
