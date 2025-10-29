import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { RestApiService } from '../services/rest-api.service';
import { DataService } from '../services/data.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';


@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent {
  firstName = '';
  lastName= '';
  email = '';
  password = '';
  confirmPassword = '';
  btnDisabled = false;
  loading = false;
  successMessage = '';
  registeredEmail = '';
  checkInProgress = false;

  constructor(
    private router: Router,
    private rest: RestApiService,
    private data: DataService,
  ) {}

  // reuse the email validation from the login component
  isValidEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  }

  // reuse the password strength validation from the login component
  isValidPassword(password: string): boolean {
    const pwdRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[\W_]).{8,}$/;
    return pwdRegex.test(password);
  }

  // add password matching validation
  passwordsMatch(): boolean {
    return this.password === this.confirmPassword;
  }

  validate(): boolean {
    if (!this.firstName) {
      this.data.error('FirstName is required.');
      return false;
    }
    if (!this.lastName) {
      this.data.error('LastName is required.');
      return false;
    }
    if (!this.email) {
      this.data.error('Email is required.');
      return false;
    }
    if (!this.isValidEmail(this.email)) {
      this.data.error('Invalid email format.');
      return false;
    }
    if (!this.password) {
      this.data.error('Password is required.');
      return false;
    }
    if (!this.isValidPassword(this.password)) {
      this.data.error('Password must contain: 8+ chars, 1 uppercase, 1 lowercase, 1 number, 1 symbol');
      return false;
    }
    if (!this.confirmPassword) {
      this.data.error('Please confirm your password.');
      return false;
    }
    if (!this.passwordsMatch()) {
      this.data.error('Passwords do not match.');
      return false;
    }
    return true;
  }

  async signUp() {

    this.btnDisabled = true;
    this.loading = true;
    this.successMessage = '';  // clear the previous success message
    try {
      if (this.validate()) {
        const data = await this.rest.post(
          'http://localhost:3000/api/register',
          {
            firstName: this.firstName,
            lastName: this.lastName,
            email: this.email,
            password: this.password
          }
        );

        if (data.success) {
          this.successMessage = 'Registration successful! Please check your email to activate your account.';
          this.registeredEmail = this.email; // save the email for later check
        } else {
          this.data.error(data.message || 'Registration failed');
        }


      }

    } catch (error : any) {

      // check the error object and extract the error message
      const errorMessage = error?.error?.message || 'An unknown error occurred during registration';
      // use the extracted error message
      this.data.error(errorMessage);
    }
    this.btnDisabled = false;
    this.loading = false;  // asynchronous operation completed,解除按钮禁用
  }

  // check if the email is activated
  async checkVerified() {
    if (!this.registeredEmail || this.checkInProgress) return;  // prevent duplicate requests
    this.checkInProgress = true;

    try {
      const result = await this.rest.get(`http://localhost:3000/api/check-verified?email=${encodeURIComponent(this.registeredEmail)}`);
      if (result.isVerified) {
        this.successMessage = 'Your account has been activated! Redirecting to home...';
        setTimeout(() => this.router.navigate(['/user-home']), 1500);
      } else {
        this.data.error('Your account is not activated yet. Please check your email.');
      }
    } catch (err) {
      this.data.error('Error checking activation status');
    }

    this.checkInProgress = false;  // after the request is completed, restore the state
  }

  // add navigateToLogin method
  navigateToLogin() {
    this.successMessage = '';  // clear the success message
    this.router.navigate(['/login']);
  }
  // add navigateToHome method
  navigateToHome() {
    this.successMessage = '';  // clear the success message
    this.router.navigate(['/user_home']);
  }
}
