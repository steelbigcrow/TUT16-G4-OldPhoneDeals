import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { UserHeaderComponent } from './user-header.component';

describe('UserHeaderComponent', () => {
  let component: UserHeaderComponent;
  let fixture: ComponentFixture<UserHeaderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        CommonModule,
        FormsModule,
        RouterModule,
        UserHeaderComponent
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
    // 一个默认选项 + 9个品牌选项
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
    component.isLoggedIn = false;
    fixture.detectChanges();
    
    const loginButton = fixture.nativeElement.querySelector('.login-button');
    expect(loginButton).toBeTruthy();
  });

  it('should show profile menu when logged in', () => {
    component.isLoggedIn = true;
    fixture.detectChanges();
    
    const profileMenu = fixture.nativeElement.querySelector('.profile-menu');
    expect(profileMenu).toBeTruthy();
  });

  it('should display cart badge with correct count', () => {
    component.cartItemCount = 5;
    fixture.detectChanges();
    
    const cartBadge = fixture.nativeElement.querySelector('.cart-badge');
    expect(cartBadge.textContent.trim()).toBe('5');
  });

  it('should hide cart badge when count is 0', () => {
    component.cartItemCount = 0;
    fixture.detectChanges();
    
    const cartBadge = fixture.nativeElement.querySelector('.cart-badge');
    expect(cartBadge).toBeFalsy();
  });
}); 