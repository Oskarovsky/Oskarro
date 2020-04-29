import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ToplistEditComponent } from './toplist-edit.component';

describe('ToplistEditComponent', () => {
  let component: ToplistEditComponent;
  let fixture: ComponentFixture<ToplistEditComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ToplistEditComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ToplistEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
