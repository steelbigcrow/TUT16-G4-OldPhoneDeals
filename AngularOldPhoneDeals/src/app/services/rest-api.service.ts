import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import {throwError, lastValueFrom, of} from 'rxjs';
import { catchError } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class RestApiService {

  constructor(private http: HttpClient) { }

  private handleError(error: HttpErrorResponse) {
    let errorMessage = 'An unknown error occurred!';
    if (error.error instanceof ErrorEvent) {
      errorMessage = `Error: ${error.error.message}`;
    } else {
      errorMessage = `Error Code: ${error.status}\nMessage: ${error.message}`;
    }
    return throwError(() => new Error(errorMessage));
  }

  getHeaders(): HttpHeaders {
    const token = localStorage.getItem('adminToken') || localStorage.getItem('token');
    return new HttpHeaders(token ? { 'Authorization': `Bearer ${token}` } : {});
  }

  async get(link: string): Promise<any> {
    return lastValueFrom(
      this.http.get(link, { headers: this.getHeaders() })
        .pipe(catchError(this.handleError))
    );
  }

  async getBlob(link: string): Promise<Blob> {
    return lastValueFrom(
      this.http.get(link, { headers: this.getHeaders(), responseType: 'blob' })
        .pipe(catchError(this.handleError))
    );
  }

  async post(link: string, body: any): Promise<any> {
    return lastValueFrom(
      this.http.post(link, body, { headers: this.getHeaders() })
        .pipe(catchError(this.handleError))
    );
  }

  async put(link: string, body: any): Promise<any> {
    return lastValueFrom(
      this.http.put(link, body, { headers: this.getHeaders() })
        .pipe(catchError(this.handleError))
    );
  }

  async delete(link: string): Promise<any> {
    return lastValueFrom(
      this.http.delete(link, { headers: this.getHeaders() })
        .pipe(catchError(this.handleError))
    );
  }

  /**
   * Send HTTP PATCH request
   */
  async patch(link: string, body: any): Promise<any> {
    return lastValueFrom(
      this.http.patch(link, body, { headers: this.getHeaders() })
        .pipe(catchError(this.handleError))
    );
  }

  verifyEmail(token: string, email: string) {
    return this.http.get(`/api/verify-email?token=${token}&email=${encodeURIComponent(email)}`);
  }

  getUserInfo() {
    const token = localStorage.getItem('token');

    return this.http.get<any>('/api/user-info', {
        headers: { Authorization: `Bearer ${token}` }
      });

  }

  resetPassword(payload: { currentPassword: string; newPassword: string }) {
    return this.http.post('http://localhost:3000/api/reset-password', payload);
  }


}
