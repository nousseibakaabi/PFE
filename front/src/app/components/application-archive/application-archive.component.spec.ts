import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ApplicationArchiveComponent } from './application-archive.component';

describe('ApplicationArchiveComponent', () => {
  let component: ApplicationArchiveComponent;
  let fixture: ComponentFixture<ApplicationArchiveComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ApplicationArchiveComponent]
    });
    fixture = TestBed.createComponent(ApplicationArchiveComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
