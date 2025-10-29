import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { UserPhoneDetailComponent } from './user-phone-detail.component';

describe('UserPhoneDetailComponent', () => {
  let component: UserPhoneDetailComponent;
  let fixture: ComponentFixture<UserPhoneDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
        RouterTestingModule,
        FormsModule,
        UserPhoneDetailComponent
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(UserPhoneDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
}); 