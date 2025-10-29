import { Injectable } from '@angular/core';
import {
  CanActivate,
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
  Router,
} from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AdminGuardService implements CanActivate {
  constructor(private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    const adminToken = localStorage.getItem('adminToken');
    console.log('Checking admin access:', { adminToken, url: state.url });

    if (adminToken) {
      return true;
    } else {
      // when an unauthenticated admin tries to access a protected route, redirect to the login page with the returnUrl
      this.router.navigate(
        ['/login'],
        { 
          queryParams: { 
            returnUrl: state.url,
            isAdmin: true 
          } 
        }
      );
      return false;
    }
  }
}
