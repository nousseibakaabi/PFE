import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CreateReassignmentRequestComponent } from './create-reassignment-request.component';

describe('CreateReassignmentRequestComponent', () => {
  let component: CreateReassignmentRequestComponent;
  let fixture: ComponentFixture<CreateReassignmentRequestComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [CreateReassignmentRequestComponent]
    });
    fixture = TestBed.createComponent(CreateReassignmentRequestComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
