//auth-guard.service.ts- TypeScript file which facilitates authentication using route guards in client application             ///
//Angular route guards are interfaces which can tell the router whether or not it should allow navigation to a requested route  //
//Used to protect access to client side routes                                                                                  //
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

//importing required modules and services
import { Injectable } from '@angular/core';
import {
  CanActivate,
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
  Router,
} from '@angular/router';
import {RestApiService} from './rest-api.service';
import {HttpClient} from '@angular/common/http';
import { firstValueFrom } from 'rxjs';


//exporting the auth-guard Service
@Injectable({
  providedIn: 'root'
})
export class AuthGuardService implements CanActivate {
  constructor(
    private router: Router,
    private rest: RestApiService,
    private http: HttpClient
  ) {}

  async canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
    const token = localStorage.getItem('token');

    if (token) {
      const data: any = await firstValueFrom(
        this.http.get('/api/user-info', {
          headers: { Authorization: `Bearer ${token}` },
        })
      );
      if (!data.success) {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        this.router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
        return false;
      }
      // when a logged in user tries to access the login page, redirect to the home page
      if (state.url === '/login') {
        this.router.navigate(['/user-home']);
        return false;
      }
      return true;
    } else {
      // when an unauthenticated user tries to access a protected route, redirect to the login page with the returnUrl
      this.router.navigate(
        ['/login'],
        {queryParams: {returnUrl: state.url}}
      );
      return false;
    }
  }
}
