import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BilanOneComponent } from './bilan-one.component';

describe('BilanOneComponent', () => {
  let component: BilanOneComponent;
  let fixture: ComponentFixture<BilanOneComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [BilanOneComponent]
    });
    fixture = TestBed.createComponent(BilanOneComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
