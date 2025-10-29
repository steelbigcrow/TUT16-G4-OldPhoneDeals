import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { UserProfileService } from '../services/user-profile.service';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import {DataService} from '../../services/data.service';

@Component({
  selector: 'app-edit-profile',
  standalone: true,
  templateUrl: './edit-profile.component.html',
  styleUrls: ['./edit-profile.component.scss'],
  imports: [ReactiveFormsModule, CommonModule]
})
export class EditProfileComponent implements OnInit {
  private fb = inject(FormBuilder);
  private userService = inject(UserProfileService);
  private dataService = inject(DataService)
  private router = inject(Router);

  profileForm = this.fb.group({
    firstName: ['', [Validators.required, Validators.maxLength(20)]],
    lastName: ['', [Validators.required, Validators.maxLength(20)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
    id: ['']  // Add a field for the user's ID
  });

  ngOnInit() {
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    this.profileForm.patchValue({
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      id: user.id  // Set the user ID in the form
    });
  }

  async onSubmit() {
    if (this.profileForm.valid) {
      const { firstName, lastName, email, password, id } = this.profileForm.value;

      try {
        const response: any = await this.userService.updateProfile({ firstName, lastName, email, password, id }).toPromise();
        if (response.success){
          alert('Profile updated!');
          this.dataService.refreshUser();
          localStorage.setItem('user', JSON.stringify(response.user));
          this.router.navigateByUrl('/', { skipLocationChange: true }).then(() => {
            this.router.navigate(['/user-profile']);
          });
        } else {
          this.dataService.error(response.message );
        }


      } catch (error) {
        alert('Failed to update profile. Check password.');
      }
    }
  }

  goBack() {
    this.router.navigate(['/user-profile']);
  }
}
