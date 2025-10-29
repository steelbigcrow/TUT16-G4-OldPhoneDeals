import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { UserHomeComponent } from './user-home.component';
import { Router } from '@angular/router';

describe('UserHomeComponent', () => {
  let component: UserHomeComponent;
  let fixture: ComponentFixture<UserHomeComponent>;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
        RouterTestingModule,
        FormsModule,
        UserHomeComponent
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(UserHomeComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should navigate to search page when searching', () => {
    const navigateSpy = spyOn(router, 'navigate');
    component.searchQuery = 'test';
    component.searchPhones();
    expect(navigateSpy).toHaveBeenCalledWith(['/search'], { queryParams: { q: 'test' } });
  });

  it('should navigate to phone detail page when viewing phone details', () => {
    const navigateSpy = spyOn(router, 'navigate');
    const mockPhone = {
      _id: '1',
      title: 'Test Phone',
      brand: 'Test Brand',
      image: 'test.jpg',
      stock: 10,
      seller: 'Test Seller',
      price: 100,
      reviews: []
    };
    
    component.viewPhoneDetails(mockPhone);
    expect(navigateSpy).toHaveBeenCalledWith(['/phone', '1']);
  });
}); 