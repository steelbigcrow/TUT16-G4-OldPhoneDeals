import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

import {RestApiService} from '../../services/rest-api.service';
import {DataService} from '../../services/data.service';


@Component({
  standalone: true,
  selector: 'app-change-password',
  templateUrl: './change-password.component.html',
  styleUrls: ['./change-password.component.scss'],
  imports: [ReactiveFormsModule, CommonModule]
})
export class ChangePasswordComponent {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private rest = inject(RestApiService)
  private data =    inject(DataService)

  // 密码表单模型
  passwordForm = this.fb.group(
    {
      oldPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]]
    },
    { validators: this.passwordMatchValidator } // 自定义跨字段验证
  );

  // 自定义验证：检查新密码和确认密码是否一致
  passwordMatchValidator(form: any) {
    return form.get('newPassword').value === form.get('confirmPassword').value
      ? null
      : { mismatch: true };
  }

  // 提交表单
  async onSubmit() {
    if (this.passwordForm.valid) {
      const user = JSON.parse(localStorage.getItem('user') || '{}');
      const email = user.email;

      const {oldPassword, newPassword} = this.passwordForm.value;
      // this.userService
      //   .changePassword(email, oldPassword!, newPassword!)
      //   .subscribe({
      //     next: () => {
      //       alert('Password change was successful!');
      //       this.router.navigate(['/user-profile']);
      //     },
      //     error: (err) => console.error('Change failed', err)
      //   });
      try {
        // 使用 async/await 调用 changePassword

        const data = await this.rest.post(
          'http://localhost:3000/api/reset-password',
          {
            email: email,
            currentPassword: oldPassword,
            newPassword: newPassword
          }
        );
        if (data.success) {
            alert('Password change was successful!');
            await this.router.navigate(['/user-profile']);
        } else {
          this.data.error(data.message || 'Registration failed');
        }
        // if (!data.success) {
        //   // 如果成功字段为 false，设置错误信息
        //   this.errorMessage = data.message || 'An error occurred while changing the password.';
        // } else {
        //   // 如果成功，执行后续操作
        //   alert('Password change was successful!');
        //   this.router.navigate(['/user-profile']);
        // }
      } catch (error: any) {
        const errorMessage = error?.error?.message || 'An unknown error occurred during reset password';

        this.data.error(errorMessage);
      }
    }
  }


  // 返回按钮
  goBack() {
    this.router.navigate(['/user-profile']);
  }
}
