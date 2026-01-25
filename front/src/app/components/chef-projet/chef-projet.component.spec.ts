import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ChefProjetComponent } from './chef-projet.component';

describe('ChefProjetComponent', () => {
  let component: ChefProjetComponent;
  let fixture: ComponentFixture<ChefProjetComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ChefProjetComponent]
    });
    fixture = TestBed.createComponent(ChefProjetComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
