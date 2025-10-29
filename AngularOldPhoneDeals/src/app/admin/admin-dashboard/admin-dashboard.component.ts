import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AdminService } from '../../services/admin.service';
import { DataService } from '../../services/data.service';

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss'],
  standalone: true,
  imports: [CommonModule, RouterModule]
})
export class AdminDashboardComponent implements OnInit {
  stats = {
    totalUsers: 0,
    totalListings: 0,
    totalReviews: 0,
    totalSales: 0
  };
  loading = false;

  constructor(
    private router: Router,
    private adminService: AdminService,
    private data: DataService
  ) {}

  ngOnInit(): void {
    this.loadDashboardStats();
  }

  async loadDashboardStats(): Promise<void> {
    this.loading = true;
    try {
      const response = await this.adminService.getDashboardStats();
      if (response.success) {
        this.stats = response.data;
      } else {
        this.data.error(response.message || 'Failed to load dashboard statistics');
      }
    } catch (error: any) {
      this.data.error(error.message || 'An error occurred while loading dashboard statistics');
    } finally {
      this.loading = false;
    }
  }

  logout(): void {
    localStorage.removeItem('adminToken');
    localStorage.removeItem('admin');
    this.router.navigate(['/login']);
  }
} 