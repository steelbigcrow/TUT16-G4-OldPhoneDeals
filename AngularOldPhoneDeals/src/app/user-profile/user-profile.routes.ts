import { Routes } from '@angular/router';
import { EditProfileComponent } from './edit-profile/edit-profile.component';
import { ChangePasswordComponent } from './change-password/change-password.component';
import { ManageListingsComponent } from './manage-listings/manage-listings.component';
import { ViewSellerReviewComponent } from './view-seller-review/view-seller-review.component';


export const USER_PROFILE_ROUTES: Routes = [
  {
    path: 'edit',
    component: EditProfileComponent // 直接引用 Standalone 组件
  },
  {
    path: 'change-password',
    component: ChangePasswordComponent
  },
  { path: 'manage-listings',
    component: ManageListingsComponent
  },
  { path: 'reviews',
    component: ViewSellerReviewComponent
  }
];
