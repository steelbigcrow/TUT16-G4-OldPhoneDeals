import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map } from 'rxjs/operators';

@Injectable({ providedIn: 'root' }) // 全局单例服务
export class UserProfileService {
  private apiUrl = 'http://localhost:3000/api';

  constructor(private http: HttpClient) {}

  getProfile() {
    return this.http.get<any>(`${this.apiUrl}/profile`).pipe(
      map(response => ({
        ...response,
        createdAt: new Date(response.createdAt) // 转换日期类型
      }))
    );
  }

  updateProfile(data: any) {
    return this.http.post(`${this.apiUrl}/update-profile`, data);
  }

  changePassword(email: string, currentPassword: string, newPassword: string) {
    return this.http.post(`${this.apiUrl}/reset-password`, {
      email,
      currentPassword,
      newPassword
    });
  }

  getPhones() {
    return this.http.get<any[]>(`${this.apiUrl}/phones`);
  }

  // 添加手机号
  addPhone(number: string) {
    return this.http.post(`${this.apiUrl}/phones`, { number });
  }

  // 删除手机号
  deletePhone(phoneId: string) {
    return this.http.delete(`${this.apiUrl}/phones/${phoneId}`);
  }

  getSellerReviews() {
    return this.http.get<any[]>(`${this.apiUrl}/reviews`);
  }
}
