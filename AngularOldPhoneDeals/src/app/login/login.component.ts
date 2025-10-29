import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { RestApiService } from '../services/rest-api.service';
import { DataService } from '../services/data.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  email = '';
  password = '';
  btnDisabled = false;
  returnUrl = '/user-home';
  isAdminLogin = false;
  showForgotModal = false;

  // reset email
  resetEmail = '';
  resetEmailSentSuccessMessage = ''; // 成功提示信息

  constructor(
    private router: Router,
    private rest: RestApiService,
    private data: DataService,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    const params = this.route.snapshot.queryParams;
    if (params['returnUrl']) {
      this.returnUrl = params['returnUrl'];
    }
    if (params['isAdmin']) {
      this.isAdminLogin = true;
    }
  }

  // 邮箱格式校验，简单正则
  isValidEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  }

  // 密码强度校验
  isValidPassword(password: string): boolean {
    // 至少8字符，包含大写、小写、数字、符号
    const pwdRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[\W_]).{8,}$/;
    return pwdRegex.test(password);
  }

  validate() {
    if (!this.email) {
      this.data.error('Please enter your email address.');
      return false;
    }
    if (!this.password) {
      this.data.error('Please enter your password.');
      return false;
    }
    return true;
  }

  async login() {
    this.btnDisabled = true;
    try {
      if (this.validate()) {
        const data = await this.rest.post('http://localhost:3000/api/login', {
          email: this.email,
          password: this.password,
        });

        if (data.success) {
          localStorage.setItem('token', data.token);
          localStorage.setItem('user', JSON.stringify(data.user));
          this.data.currentUser$.next(data.user);
          await this.router.navigate([this.returnUrl]);
        } else {
          this.data.error(data.message);
        }
      }
    } catch (error: any) {
      this.data.error(error.message || 'An error occurred during login');
    }
    this.btnDisabled = false;
  }

  async adminLogin() {
    console.log('Admin login attempt');
    this.btnDisabled = true;
    try {
      if (this.validate()) {
        const data = await this.rest.post('http://localhost:3000/api/admin/auth/login', {
          email: this.email,
          password: this.password,
        });

        if (data.success) {
          localStorage.setItem('adminToken', data.token);
          localStorage.setItem('admin', JSON.stringify(data.admin));
          await this.router.navigate(['/admin', 'dashboard']);
        } else {
          this.data.error(data.message || 'Admin login failed');
        }
      }
    } catch (error: any) {
      console.error('Admin login error:', error);
      this.data.error(error.message || 'An error occurred during admin login');
    }
    this.btnDisabled = false;
  }

  toggleLoginMode() {
    this.isAdminLogin = !this.isAdminLogin;
    this.email = '';
    this.password = '';
  }

  closeForgotModal() {
    this.showForgotModal = false;
    this.resetEmail = '';
  }

  async verifyEmailAndRedirect() {
    if (!this.resetEmail || !this.isValidEmail(this.resetEmail)) {
      this.data.error('Please enter a valid email');
      return;
    }

    try {
      const res = await this.rest.post('http://localhost:3000/api/send-reset-password-email', {
        email: this.resetEmail
      });

      if (res.success) {
        this.resetEmailSentSuccessMessage = 'Reset email sent successfully. Please check your inbox. ' +
          'This page will automatically redirect to the home page shortly.';

        this.showForgotModal = true;
        // 仍然跳转（如果你希望延迟几秒再跳转可以用 setTimeout）
        setTimeout(() => {
          this.router.navigate(['/user-home']);}, 3500); // 延迟 3.5 秒
      } else {
        this.data.error(res.message || 'Email not found');
      }
    } catch (err) {
      console.error(err);
      this.data.error('Something went wrong');
    }
  }

  // 跳转到注册页面
  navigateToRegister() {
    this.router.navigate(['/register']);
  }

  // 跳转到改密码页面
  navigateToResetPassword() {
    this.router.navigate(['/reset-password']);
  }
}
