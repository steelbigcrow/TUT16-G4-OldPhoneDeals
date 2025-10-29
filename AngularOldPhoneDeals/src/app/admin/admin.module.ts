import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { SharedModule } from '../components/shared.module';
import { AdminLayoutComponent } from './admin-layout/admin-layout.component';
import { AdminDashboardComponent } from './admin-dashboard/admin-dashboard.component';
import { AdminUserManagementComponent } from './admin-user-management/admin-user-management.component';
import { AdminListingManagementComponent } from './admin-listing-management/admin-listing-management.component';
import { AdminReviewManagementComponent } from './admin-review-management/admin-review-management.component';
import { AdminSalesLogsComponent } from './admin-sales-logs/admin-sales-logs.component';
import { AdminGuardService } from '../services/admin-guard.service';

const routes: Routes = [
  {
    path: '',
    component: AdminLayoutComponent,
    canActivate: [AdminGuardService],
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },
      {
        path: 'dashboard',
        component: AdminDashboardComponent
      },
      {
        path: 'users',
        component: AdminUserManagementComponent
      },
      {
        path: 'listings',
        component: AdminListingManagementComponent
      },
      {
        path: 'reviews',
        component: AdminReviewManagementComponent
      },
      {
        path: 'sales',
        component: AdminSalesLogsComponent
      }
    ]
  }
];

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule.forChild(routes),
    SharedModule,
    AdminLayoutComponent,
    AdminDashboardComponent,
    AdminUserManagementComponent,
    AdminListingManagementComponent,
    AdminReviewManagementComponent,
    AdminSalesLogsComponent
  ]
})
export class AdminModule { } 