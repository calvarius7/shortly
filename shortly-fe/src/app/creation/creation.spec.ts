import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Creation } from './creation';

describe('Creation', () => {
  let component: Creation;
  let fixture: ComponentFixture<Creation>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Creation]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Creation);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
