import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConventionArchiveComponent } from './convention-archive.component';

describe('ConventionArchiveComponent', () => {
  let component: ConventionArchiveComponent;
  let fixture: ComponentFixture<ConventionArchiveComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ConventionArchiveComponent]
    });
    fixture = TestBed.createComponent(ConventionArchiveComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
