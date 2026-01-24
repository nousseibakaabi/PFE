import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConventionComponent } from './convention.component';

describe('ConventionComponent', () => {
  let component: ConventionComponent;
  let fixture: ComponentFixture<ConventionComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ConventionComponent]
    });
    fixture = TestBed.createComponent(ConventionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
