import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConventionFacturesComponent } from './convention-factures.component';

describe('ConventionFacturesComponent', () => {
  let component: ConventionFacturesComponent;
  let fixture: ComponentFixture<ConventionFacturesComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ConventionFacturesComponent]
    });
    fixture = TestBed.createComponent(ConventionFacturesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
