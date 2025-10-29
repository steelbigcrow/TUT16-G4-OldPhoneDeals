import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatToolbarModule } from '@angular/material/toolbar';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatToolbarModule
  ],
  template: `
    <mat-sidenav-container class="admin-container">
      <mat-sidenav #sidenav mode="side" opened class="admin-sidenav">
        <div class="sidenav-header">
          <h2>Admin Panel</h2>
        </div>
        <mat-nav-list>
          <a mat-list-item routerLink="/admin/dashboard" routerLinkActive="active">
            <span>Dashboard</span>
          </a>
          <a mat-list-item routerLink="/admin/users" routerLinkActive="active">
            <span>User Management</span>
          </a>
          <a mat-list-item routerLink="/admin/listings" routerLinkActive="active">
            <span>Listing Management</span>
          </a>
          <a mat-list-item routerLink="/admin/reviews" routerLinkActive="active">
            <span>Review Management</span>
          </a>
          <a mat-list-item routerLink="/admin/sales" routerLinkActive="active">
            <span>Sales Logs</span>
          </a>
        </mat-nav-list>
      </mat-sidenav>

      <mat-sidenav-content class="admin-content">
        <mat-toolbar color="primary">
          <button class="btn btn-light me-2" aria-label="Toggle sidebar" (click)="sidenav.toggle()">
            <i class="bi bi-list"></i>
          </button>
        </mat-toolbar>
        <div class="content-wrapper">
          <router-outlet></router-outlet>
        </div>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: [`
    .admin-container {
      height: 100vh;
    }

    .admin-sidenav {
      width: 250px;
      background-color: #f8f9fa;
    }

    .sidenav-header {
      padding: 16px;
      background-color: #3f51b5;
      color: white;
      text-align: center;
    }
    .sidenav-header h2 {
      margin: 0;
      font-size: 1.2rem;
    }

    mat-nav-list {
      padding-top: 0;
    }
    mat-nav-list a {
      display: flex;
      align-items: center;
      gap: 12px;
      color: #333;
      text-decoration: none;
    }
    mat-nav-list a mat-icon {
      color: #666;
    }
    mat-nav-list a.active {
      background-color: #e8eaf6;
      color: #3f51b5;
    }
    mat-nav-list a.active mat-icon {
      color: #3f51b5;
    }
    mat-nav-list a:hover {
      background-color: #f5f5f5;
    }

    .admin-content {
      background-color: #f5f5f5;
    }

    .content-wrapper {
      padding: 24px;
      height: calc(100vh - 64px);
      overflow-y: auto;
    }
  `]
})
export class AdminLayoutComponent {
  constructor() {}
} 