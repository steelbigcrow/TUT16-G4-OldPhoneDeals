import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { UserHeaderComponent } from './user-header.component';
import { DataService } from '../services/data.service';

// Mock DataService
class MockDataService {
  user = null;  // default set to null, simulate not logged in state
  cartItems = 0;

  get isLoggedIn(): boolean {
    return !!this.user; // calculate whether logged in based on whether user exists
  }

  setUser(user: any) {
    this.user = user;
  }
}

describe('UserHeaderComponent', () => {
  let component: UserHeaderComponent;
  let fixture: ComponentFixture<UserHeaderComponent>;
  let mockDataService: MockDataService;

  beforeEach(async () => {
    mockDataService = new MockDataService(); // initialize MockDataService

    await TestBed.configureTestingModule({
      imports: [
        CommonModule,
        FormsModule,
        RouterModule,
        UserHeaderComponent
      ],
      providers: [
        { provide: DataService, useValue: mockDataService } // use mockDataService instead of original DataService
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(UserHeaderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display the site name', () => {
    const siteName = fixture.nativeElement.querySelector('.site-name');
    expect(siteName.textContent).toContain(component.siteName);
  });

  it('should display all brand options', () => {
    const options = fixture.nativeElement.querySelectorAll('.brand-select option');
    // one default option + 9 brand options
    expect(options.length).toBe(component.brands.length + 1);
  });

  it('should update searchQuery when input changes', () => {
    const searchInput = fixture.nativeElement.querySelector('.search-input');
    const testQuery = 'iPhone';

    searchInput.value = testQuery;
    searchInput.dispatchEvent(new Event('input'));

    expect(component.searchQuery).toBe(testQuery);
  });

  it('should call performSearch when search button is clicked', () => {
    spyOn(component, 'performSearch');

    const searchButton = fixture.nativeElement.querySelector('.search-button');
    searchButton.click();

    expect(component.performSearch).toHaveBeenCalled();
  });

  it('should show login button when not logged in', () => {
    mockDataService.user = null; // simulate not logged in state
    fixture.detectChanges();

    const loginButton = fixture.nativeElement.querySelector('.login-button');
    expect(loginButton).toBeTruthy();
  });

  it('should show profile menu when logged in', () => {
    mockDataService.user = { id: '1', firstName: 'John', lastName: 'Doe', email: 'john@example.com' }; // simulate logged in state
    fixture.detectChanges();

    const profileMenu = fixture.nativeElement.querySelector('.profile-menu');
    expect(profileMenu).toBeTruthy();
  });

  it('should display cart badge with correct count', () => {
    mockDataService.cartItems = 5;
    fixture.detectChanges();

    const cartBadge = fixture.nativeElement.querySelector('.cart-badge');
    expect(cartBadge.textContent.trim()).toBe('5');
  });

  it('should hide cart badge when count is 0', () => {
    mockDataService.cartItems = 0;
    fixture.detectChanges();

    const cartBadge = fixture.nativeElement.querySelector('.cart-badge');
    expect(cartBadge).toBeFalsy();
  });
});
