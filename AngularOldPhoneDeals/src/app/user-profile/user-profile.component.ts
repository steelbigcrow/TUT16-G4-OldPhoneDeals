// user-profile.component.ts
import { Component, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { UserProfileService } from './services/user-profile.service';
import { Router, ActivatedRoute, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import {DataService} from '../services/data.service';
import {User} from '../models/user.model';
import {Observable} from 'rxjs';
import {AuthGuardService} from '../services/auth-guard.service';
import {AuthService} from '../services/auth.service';

@Component({
  standalone: true,
  selector: 'app-user-profile',
  templateUrl: './user-profile.component.html',
  styleUrls: ['./user-profile.component.scss'],
  imports: [CommonModule, RouterModule ]
})
export class UserProfileComponent implements OnInit {
  user: any;
  currentRoute = '';

  constructor(
    private userService: UserProfileService,
    private router: Router,
    private route: ActivatedRoute,
    private auth: AuthService,
  public data: DataService
  ) {
    // 监听路由变化
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => {
        this.currentRoute = this.route.firstChild?.snapshot.routeConfig?.path || '';
      });
  }

  ngOnInit() {
    // this.loadUserProfile();
    // 取得登入后的本地用户信息
    this.user = JSON.parse(localStorage.getItem('user') || '{}');
  }
  /**
   * Expose currentUser$ from DataService via a getter to avoid using this.data in initializer
   */
  get currentUser$() {
    return this.data.currentUser$;
  }

  // loadUserProfile() {
  //   this.userService.getProfile().subscribe({
  //     next: (user) => this.user = user,
  //     error: (err) => console.error('Fail to load user data:', err)
  //   });
  // }

  // 判断是否显示简介视图
  isProfileViewActive(): boolean {
    return this.currentRoute === '';
  }
  logout(): void {
    const confirmed = window.confirm('Are you sure you want to log out?'); // 弹出确认提示

    if (confirmed) {
      this.auth.logout();  // 清除登录信息
      this.router.navigate(['/user-home']);  // 重定向到登录页
    }
  }
}
