import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { RestApiService } from '../services/rest-api.service';
import { DataService } from '../services/data.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.scss']
})
export class ResetPasswordComponent   {
  email = '';  // email address
  currentPassword = '';
  newPassword = '';
  confirmNewPassword = '';
  btnDisabled = false;
  loading = false;
  successMessage = '';
  errorMessage = '';
  isPasswordChanged = false;


  constructor(
    private router: Router,
    private rest: RestApiService,
    private data: DataService,
  ) {}

  // ngOnInit(): void {
  //   this.token = localStorage.getItem('token');
  //   if (!this.token) {
  //     this.router.navigate(['/login']);
  //   }
  // }

  isValidPassword(password: string): boolean {
    const pwdRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[\W_]).{8,}$/;
    return pwdRegex.test(password);
  }

  validate(): boolean {
    if (!this.email) {
      this.data.error('Email is required.');
      return false;
    }
    // if (!this.currentPassword) {
    //   this.data.error('Current password is required.');
    //   return false;
    // }
    if (!this.newPassword) {
      this.data.error('New password is required.');
      return false;
    }
    if (this.newPassword !== this.confirmNewPassword) {
      this.data.error('New password and confirmation do not match.');
      return false;
    }
    if (!this.isValidPassword(this.newPassword)) {
      this.data.error('New password must contain: 8+ chars, 1 uppercase, 1 lowercase, 1 number, 1 symbol');
      return false;
    }
    return true;
  }

  async resetPassword() {
    this.btnDisabled = true;
    this.loading = true;
    this.successMessage = '';
    this.errorMessage = '';
    this.isPasswordChanged = false;
    console.log("frontend reset-password starts working")
    try {
      if (this.validate()) {
        const data = await this.rest.post('http://localhost:3000/api/reset-password', {
          email: this.email,  // pass the email to the backend
          // currentPassword: this.currentPassword,
          newPassword: this.newPassword
        });

        if (data.success) {
          this.successMessage = 'Password changed successfully! A confirmation email has been sent.';
          this.isPasswordChanged = true;
        } else {
          console.log("frontend reset-password data not success")
          this.errorMessage = data.message || 'Password change failed';
        }
      }
    } catch (error) {
      console.error('Password change error:', error);
      this.errorMessage = 'An error occurred while changing the password';
    }

    this.btnDisabled = false;
    this.loading = false;
  }
  navigateToLogin() {
    this.successMessage = '';  // clear the success message
    this.router.navigate(['/login']);
  }
}
