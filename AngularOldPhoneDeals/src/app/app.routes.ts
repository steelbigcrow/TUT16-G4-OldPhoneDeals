// app.routes.ts
import { RouterModule, Routes } from '@angular/router';
import { NgModule } from '@angular/core';

import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { CheckoutComponent } from './checkout/checkout.component';
import { AuthGuardService } from './services/auth-guard.service';
import { VerifyEmailComponent } from './verify-email/verify-email.component';
import { UserHomeComponent } from './user-home/user-home.component';
import { UserSearchComponent } from './user-search/user-search.component';
import { UserPhoneDetailComponent } from './user-phone-detail/user-phone-detail.component';
import { UserProfileComponent } from './user-profile/user-profile.component';
import { WishlistComponent } from './wishlist/wishlist.component';
import { EditProfileComponent } from './user-profile/edit-profile/edit-profile.component';
import { ChangePasswordComponent } from './user-profile/change-password/change-password.component';
import { ManageListingsComponent } from './user-profile/manage-listings/manage-listings.component';
import { ViewSellerReviewComponent } from './user-profile/view-seller-review/view-seller-review.component';
import {ResetPasswordComponent} from './reset-password/reset-password.component';


export const routes: Routes = [
  {
    path: '',
    redirectTo: 'user-home',
    pathMatch: 'full'
  },
  {
    path: 'user-home',
    component: UserHomeComponent
  },
  {
    path: 'search',
    component: UserSearchComponent
  },
  {
    path: 'phone/:id',
    component: UserPhoneDetailComponent
  },
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: 'checkout',
    component: CheckoutComponent,
    canActivate: [AuthGuardService]
  },
  {
    path: 'wishlist',
    component: WishlistComponent,
    canActivate: [AuthGuardService]

  },
  {
    path: 'register',
    component: RegisterComponent
  },
  {
    path: 'verify-email',
    component: VerifyEmailComponent
  },
  {
    path: 'reset-password',
    component: ResetPasswordComponent
    // canActivate: [AuthGuardService]
  },
  // Admin routes
  {
    path: 'admin',
    loadChildren: () => import('./admin/admin.module').then(m => m.AdminModule)
  },
  {
    path: 'user-profile',
    component: UserProfileComponent,
    canActivate: [AuthGuardService],
    children: [
      { path: 'edit',
        component: EditProfileComponent,
        canActivate: [AuthGuardService] },
      { path: 'change-password',
        component: ChangePasswordComponent ,
        canActivate: [AuthGuardService]},
      { path: 'manage-listings',
        component: ManageListingsComponent ,
        canActivate: [AuthGuardService]},
      { path: 'reviews',
        component: ViewSellerReviewComponent,
        canActivate: [AuthGuardService] }
    ]
  },
  // {
  //   path: 'user-profile',
  //   component: UserProfileComponent,
  //   children: [
  //     { path: 'edit', component: EditProfileComponent },
  //     { path: 'change-password', component: ChangePasswordComponent },
  //     { path: 'phones', component: ManagePhonesComponent },
  //     { path: 'reviews', component: ViewSellerReviewComponent }
  //   ]
  // },


  {

    path: '**',
    redirectTo: 'user-home'
  }
];
