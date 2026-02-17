import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConventionDetailComponent } from './convention-detail.component';

describe('ConventionDetailComponent', () => {
  let component: ConventionDetailComponent;
  let fixture: ComponentFixture<ConventionDetailComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ConventionDetailComponent]
    });
    fixture = TestBed.createComponent(ConventionDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
