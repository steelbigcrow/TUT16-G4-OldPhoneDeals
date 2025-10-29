import { Injectable } from '@angular/core';
import { NavigationStart, Router } from '@angular/router';
import { RestApiService } from './rest-api.service';
import {BehaviorSubject} from 'rxjs';

export interface User {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
}

@Injectable({
  providedIn: 'root'
})
export class DataService {

  message = '';
  messageType = 'danger';

  // user: User | null = null;
  cartItems = 0;
  // Observable for current logged-in user
  currentUser$ = new BehaviorSubject<User|null>(null);

  constructor(private router: Router, private rest: RestApiService) {
    this.router.events.subscribe(event => {
      if (event instanceof NavigationStart) {
        this.message = '';
      }
    });
    // Load current user on service init
    this.loadUser();

    // Listen to storage events to sync logout across tabs
    window.addEventListener('storage', event => {
      if (event.key === 'token' && event.newValue === null) {
        // Token removed in another tab: clear user and navigate home
        this.currentUser$.next(null);
        this.router.navigate(['/user-home']);
      }
    });
  }

  error(message: string) {
    this.messageType = 'danger';
    this.message = message;
  }

  success(message: string) {
    this.messageType = 'success';
    this.message = message;
  }

  warning(message: string) {
    this.messageType = 'warning';
    this.message = message;
  }

  // Load user info from API if token exists
  private loadUser() {
    const token = localStorage.getItem('token');
    if (token) {
      this.rest.getUserInfo().subscribe({
        next: res => {
          if (res.success) {
            this.currentUser$.next(res.user);
            localStorage.setItem('user', JSON.stringify(res.user)); // 同步localStorage user
          } else {
            // this.currentUser$.next(null);
            // 用户不存在或token无效，强制登出
            this.logoutUser();
          }
        },
        error: () => {
          this.currentUser$.next(null);
        }
      });
    } else {
      this.currentUser$.next(null);
    }
  }

  // Refresh the current user info
  refreshUser() {
    this.loadUser();
  }

  // Logout and clear current user
  logoutUser() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    this.currentUser$.next(null);
    this.router.navigate(['/user-home']);  // 登出后导航到首页
  }
}
