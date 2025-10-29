import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, RouterLink, RouterOutlet, NavigationEnd } from '@angular/router';
import { NgIf } from '@angular/common';
import { MessageComponent } from './message/message.component';
import { UserHeaderComponent } from './user-header/user-header.component';
import { AdminHeaderComponent } from './admin-header/admin-header.component';
import { filter } from 'rxjs/operators';
import { interval, Subscription } from 'rxjs';
import { RestApiService } from './services/rest-api.service';
import { DataService } from './services/data.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet,
    MessageComponent,
    AdminHeaderComponent,
    UserHeaderComponent,
    NgIf
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'AngularOldPhoneDeals';
  searchTerm = '';
  isCollapsed = true;
  isAdmin = false;
  private lastTotalSales = 0;
  private pollingSub?: Subscription;

  constructor(private router: Router,
              private rest: RestApiService,
              private data: DataService) {}

  get token() {
    return localStorage.getItem('token');
  }

  get showNav(): boolean {
    return true;
  }

  setAdminStatus(isAdmin: boolean) {
    this.isAdmin = isAdmin;
  }

  // Start polling admin stats for new orders
  private startPolling(): void {
    // initialize and get current total sales
    this.rest.get('/api/admin/stats').then(res => {
      if (res.success) {
        this.lastTotalSales = res.data.totalSales;
      }
    });
    // polling every 10 seconds
    this.pollingSub = interval(10000).subscribe(() => {
      this.rest.get('/api/admin/stats').then(res => {
        if (res.success) {
          const newTotal = res.data.totalSales;
          if (newTotal > this.lastTotalSales) {
            this.data.success(`new order!`);
            this.lastTotalSales = newTotal;
          }
        }
      }).catch(console.error);
    });
  }

  ngOnInit(): void {
    // Initialize admin status and start/stop polling accordingly
    this.isAdmin = !!localStorage.getItem('adminToken');
    if (this.isAdmin) {
      this.startPolling();
    }
    // Listen to route changes to manage admin polling and redirect
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: NavigationEnd) => {
      const nowAdmin = !!localStorage.getItem('adminToken');
      // when not admin and become admin, start polling
      if (!this.isAdmin && nowAdmin) {
        this.startPolling();
      }
      // when admin and become not admin, stop polling
      if (this.isAdmin && !nowAdmin) {
        this.pollingSub?.unsubscribe();
      }
      this.isAdmin = nowAdmin;
      // when admin login and redirect to user home, redirect to dashboard
      if (this.isAdmin && event.urlAfterRedirects === '/user-home') {
        this.router.navigate(['/admin', 'dashboard']);
      }
    });
  }

  ngOnDestroy(): void {
    // clean up polling subscription
    this.pollingSub?.unsubscribe();
  }
}