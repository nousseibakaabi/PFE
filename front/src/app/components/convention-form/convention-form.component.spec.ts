import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConventionFormComponent } from './convention-form.component';

describe('ConventionFormComponent', () => {
  let component: ConventionFormComponent;
  let fixture: ComponentFixture<ConventionFormComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ConventionFormComponent]
    });
    fixture = TestBed.createComponent(ConventionFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
