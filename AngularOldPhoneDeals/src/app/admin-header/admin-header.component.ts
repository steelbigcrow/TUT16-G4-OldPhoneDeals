import { Component, AfterViewInit, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { DataService } from '../services/data.service';

declare var bootstrap: any;

interface SearchConfig {
  placeholder: string;
  searchFields: string[];
}

@Component({
  selector: 'app-admin-header',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule
  ],
  templateUrl: './admin-header.component.html',
  styleUrls: ['./admin-header.component.scss']
})

export class AdminHeaderComponent implements AfterViewInit, OnInit {
  siteName = 'OldPhoneDeals';
  
  // Tabs configuration
  tabs = [
    { id: 'users', label: 'Users' },
    { id: 'phones', label: 'Phones' },
    { id: 'reviews', label: 'Reviews' },
    { id: 'orders', label: 'Orders' },
    { id: 'log', label: 'Log' }
  ];
  activeTab = 'users';

  // Search related
  searchQuery = '';

  // Admin info
  adminName = 'Admin User';

  constructor(private router: Router, private data: DataService) {}

  ngAfterViewInit() {
    this.initializeBootstrapComponents();
  }

  ngOnInit(): void {
    // Load admin name from localStorage
    const adminJson = localStorage.getItem('admin');
    if (adminJson) {
      try {
        const admin = JSON.parse(adminJson);
        this.adminName = `${admin.firstName} ${admin.lastName}`;
      } catch (e) {
        console.error('Failed to parse admin info from storage', e);
      }
    }
  }

  private initializeBootstrapComponents() {
    const dropdowns = document.querySelectorAll('.dropdown-toggle');
    if (dropdowns.length > 0 && typeof bootstrap !== 'undefined') {
      dropdowns.forEach(dropdown => {
        new bootstrap.Dropdown(dropdown);
      });
    }
  }

  get activeTabLabel(): string {
    return this.tabs.find(t => t.id === this.activeTab)?.label || '';
  }

  switchTab(tabId: string) {
    this.activeTab = tabId;
    this.router.navigate([`/admin/${tabId}`]);
  }

  private searchConfigs: { [key: string]: SearchConfig } = {
    users: {
      placeholder: 'Search by name or email',
      searchFields: ['name', 'email']
    },
    phones: {
      placeholder: 'Search by title or brand',
      searchFields: ['title', 'brand']
    },
    reviews: {
      placeholder: 'Search by user, content or listing',
      searchFields: ['user', 'content', 'listingId']
    }
  };

  get searchPlaceholder(): string {
    return this.searchConfigs[this.activeTab]?.placeholder || 'Search';
  }

  performSearch() {
    if (this.activeTab === 'orders' || this.activeTab === 'log') return;

    const searchParams = {
      q: this.searchQuery.trim(),
      fields: this.searchConfigs[this.activeTab].searchFields.join(',')
    };

    this.router.navigate([`/admin/${this.activeTab}`], {
      queryParams: searchParams,
      queryParamsHandling: 'merge'
    });

    this.collapseMobileMenu();
  }

  logout() {
    if (confirm('Are you sure to logout?')) {
      // Clear admin credentials
      localStorage.removeItem('adminToken');
      localStorage.removeItem('admin');
      // Clear user credentials
      this.data.logoutUser();
      // Navigate back to user home
      this.router.navigate(['/user-home']);
    }
  }

  private collapseMobileMenu() {
    const navbar = document.getElementById('adminNavContent');
    if (navbar?.classList.contains('show')) {
      const bsCollapse = bootstrap.Collapse.getInstance(navbar);
      bsCollapse?.hide();
    }
  }
}
