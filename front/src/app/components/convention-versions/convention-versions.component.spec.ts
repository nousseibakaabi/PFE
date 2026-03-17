import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConventionVersionsComponent } from './convention-versions.component';

describe('ConventionVersionsComponent', () => {
  let component: ConventionVersionsComponent;
  let fixture: ComponentFixture<ConventionVersionsComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ConventionVersionsComponent]
    });
    fixture = TestBed.createComponent(ConventionVersionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
