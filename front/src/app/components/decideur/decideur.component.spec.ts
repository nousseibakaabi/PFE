import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DecideurComponent } from './decideur.component';

describe('DecideurComponent', () => {
  let component: DecideurComponent;
  let fixture: ComponentFixture<DecideurComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [DecideurComponent]
    });
    fixture = TestBed.createComponent(DecideurComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
