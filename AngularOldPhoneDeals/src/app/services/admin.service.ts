import { Injectable } from '@angular/core';
import { RestApiService } from './rest-api.service';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private baseUrl = 'http://localhost:3000/api/admin';

  constructor(private rest: RestApiService) {}

  // Dashboard Statistics
  async getDashboardStats() {
    return this.rest.get(`${this.baseUrl}/stats`);
  }

  // User Management
  async getUsers(
    page: number = 1,
    limit: number = 10,
    sortBy: string = 'createdAt',
    sortOrder: string = 'desc',
    search?: string,
    isAdmin?: boolean,
    isDisabled?: boolean
  ) {
    let url = `${this.baseUrl}/users?page=${page}&limit=${limit}&sortBy=${sortBy}&sortOrder=${sortOrder}`;
    if (search) {
      url += `&search=${encodeURIComponent(search)}`;
    }
    if (isAdmin !== undefined) {
      url += `&isAdmin=${isAdmin}`;
    }
    if (isDisabled !== undefined) {
      url += `&isDisabled=${isDisabled}`;
    }
    return this.rest.get(url);
  }

  async updateUser(id: string, data: any) {
    return this.rest.patch(`${this.baseUrl}/users/${id}`, data);
  }

  async deleteUser(id: string) {
    return this.rest.delete(`${this.baseUrl}/users/${id}`);
  }

  async updateUserStatus(id: string, isDisabled: boolean) {
    return this.rest.patch(`${this.baseUrl}/users/${id}/status`, { isDisabled });
  }

  // Phone Listings Management
  async getListings(
    page: number = 1,
    limit: number = 10,
    sortBy: string | null = null,
    sortOrder: string | null = null,
    searchTerm: string | null = null,
    brand: string | null = null
  ) {
    const params = new URLSearchParams();
    params.set('page', String(page));
    params.set('limit', String(limit));
    params.set('sortBy', String(sortBy));
    params.set('sortOrder', String(sortOrder));
    if (searchTerm) params.set('searchTerm', searchTerm);
    if (brand) params.set('brand', brand);

    return this.rest.get(`${this.baseUrl}/phones?${params.toString()}`);
  }


  async updateListing(id: string, data: any) {
    return this.rest.patch(`${this.baseUrl}/phones/${id}`, data);
  }

  async deleteListing(id: string) {
    return this.rest.delete(`${this.baseUrl}/phones/${id}`);
  }

  async toggleListingStatus(id: string, isActive: boolean) {
    return this.rest.patch(`${this.baseUrl}/phones/${id}`, { isActive });
  }

  // Review Management
  async getReviews(
    page: number = 1,
    limit: number = 10,
    sortBy: string = 'createdAt',
    sortOrder: string = 'desc',
    searchTerm?: string,
    visibility?: string
  ) {
    const params = new URLSearchParams();
    params.set('page', String(page));
    params.set('limit', String(limit));
    params.set('sortBy', sortBy);
    params.set('sortOrder', sortOrder);
    if (searchTerm) {
      params.set('search', searchTerm);
    }
    if (visibility) {
      params.set('visibility', visibility);
    }
    return this.rest.get(`${this.baseUrl}/reviews?${params.toString()}`);
  }

  async updateReview(id: string, data: any) {
    return this.rest.patch(`${this.baseUrl}/reviews/${id}`, data);
  }

  async deleteReview(id: string) {
    return this.rest.delete(`${this.baseUrl}/reviews/${id}`);
  }

  // Toggle review visibility for a specific phone and review
  async toggleReviewVisibility(
    phoneId: string,
    reviewId: string,
    isHidden: boolean
  ) {
    return this.rest.patch(
      `${this.baseUrl}/phones/${phoneId}/reviews/${reviewId}/visibility`,
      { isHidden }
    );
  }

  // Sales Logs (Orders)
  async getSalesLogs(
    page: number = 1,
    limit: number = 10,
    searchTerm?: string,
    brandFilter?: string,
    sortOption: string = 'default'
  ) {
    const params = new URLSearchParams();
    params.set('page', String(page));
    params.set('limit', String(limit));
    if (searchTerm) {
      params.set('searchTerm', searchTerm);
    }
    if (brandFilter && brandFilter !== 'All Brands') {
      params.set('brandFilter', brandFilter);
    }
    let sortBy = 'createdAt';
    let sortOrder = 'desc';
    switch (sortOption) {
      case 'timestampAsc':
        sortOrder = 'asc';
        break;
      case 'timestampDesc':
        sortOrder = 'desc';
        break;
      case 'amountAsc':
        sortBy = 'totalAmount';
        sortOrder = 'asc';
        break;
      case 'amountDesc':
        sortBy = 'totalAmount';
        sortOrder = 'desc';
        break;
      default:
        break;
    }
    params.set('sortBy', sortBy);
    params.set('sortOrder', sortOrder);

    return this.rest.get(`${this.baseUrl}/orders?${params.toString()}`);
  }

  async getSalesStats(startDate?: string, endDate?: string) {
    let url = `${this.baseUrl}/orders/stats`;
    if (startDate) url += `?startDate=${startDate}`;
    if (endDate) url += `${startDate ? '&' : '?'}endDate=${endDate}`;
    return this.rest.get(url);
  }

  // Fetch all phones a user is selling (Admin)
  async getUserPhones(
    userId: string,
    page: number = 1,
    limit: number = 10,
    sortBy?: string,
    sortOrder?: string,
    brand?: string
  ) {
    let url = `${this.baseUrl}/users/${userId}/phones?page=${page}&limit=${limit}`;
    if (sortBy) {
      url += `&sortBy=${sortBy}`;
    }
    if (sortOrder) {
      url += `&sortOrder=${sortOrder}`;
    }
    if (brand) {
      url += `&brand=${encodeURIComponent(brand)}`;
    }
    return this.rest.get(url);
  }

  // Fetch all reviews a user has submitted (Admin)
  async getUserReviews(
    userId: string,
    page: number = 1,
    limit: number = 10,
    sortBy?: string,
    sortOrder?: string,
    brand?: string
  ) {
    let url = `${this.baseUrl}/users/${userId}/reviews?page=${page}&limit=${limit}`;
    if (sortBy) {
      url += `&sortBy=${sortBy}`;
    }
    if (sortOrder) {
      url += `&sortOrder=${sortOrder}`;
    }
    if (brand) {
      url += `&brand=${encodeURIComponent(brand)}`;
    }
    return this.rest.get(url);
  }

  // I add this method to export sales logs as CSV or JSON via backend
  async exportSalesLogs(
    format: 'csv' | 'json',
    searchTerm?: string,
    brandFilter?: string,
    sortOption: string = 'default'
  ): Promise<Blob> {
    const params = new URLSearchParams();
    params.set('format', format);
    if (searchTerm) {
      params.set('searchTerm', searchTerm);
    }
    if (brandFilter && brandFilter !== 'All Brands') {
      params.set('brandFilter', brandFilter);
    }
    let sortBy = 'createdAt';
    let sortOrder = 'desc';
    switch (sortOption) {
      case 'timestampAsc':
        sortOrder = 'asc';
        break;
      case 'timestampDesc':
        sortOrder = 'desc';
        break;
      case 'amountAsc':
        sortBy = 'totalAmount';
        sortOrder = 'asc';
        break;
      case 'amountDesc':
        sortBy = 'totalAmount';
        sortOrder = 'desc';
        break;
      default:
        break;
    }
    params.set('sortBy', sortBy);
    params.set('sortOrder', sortOrder);
    const url = `${this.baseUrl}/orders/export?${params.toString()}`;
    return this.rest.getBlob(url);
  }
}
