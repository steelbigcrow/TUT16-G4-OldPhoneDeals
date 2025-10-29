import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { UserProfileService } from '../services/user-profile.service';
import { RestApiService } from '../../services/rest-api.service';
import {DataService} from '../../services/data.service';
import { PaginationComponent } from '../../shared/pagination/pagination.component';


@Component({
  standalone: true,
  selector: 'app-view-seller-review',
  templateUrl: './view-seller-review.component.html',
  styleUrls: ['./view-seller-review.component.scss'],
  imports: [CommonModule, PaginationComponent]
})
export class ViewSellerReviewComponent implements OnInit {
  private router = inject(Router);
  private userService = inject(UserProfileService);
  private rest = inject(RestApiService); // 注入 RestApiService
  private data = inject(DataService)

  // 评价列表数据
  reviews: any[] = [];
  // Pagination parameters
  allReviews: any[] = [];
  currentPage = 1;
  pageSize = 10;
  totalPages = 1;
  isLoading = true;
  user: any;
  ngOnInit() {
    this.loadReviews();
  }

  // 加载评价数据
  async loadReviews() {
    try {
      // 从本地存储获取 token
      this.user = JSON.parse(localStorage.getItem('user') || '{}');
      if (this.user) {
        // 解码 token 获取 user_id
        const sellerId = this.user.id; // 假设 token 中存储了 user_id

        // 使用 REST API 获取评论数据
        const data = await this.rest.get(`http://localhost:3000/api/phones/get-reviews-by-id/${sellerId}`);

        if (data.success) {

          // 确保从后端返回的数据是数组
          if (Array.isArray(data.reviews)) {
            this.allReviews = data.reviews;
            this.applyPagination();
          } else {
            this.reviews = [];  // 如果不是数组，赋值为空数组
            this.data.error('Reviews data is not an array:');
          }
          this.isLoading = false;

        } else {
          this.isLoading = false;
          this.data.error(data.message || 'load Reviews failed');
        }



      }
    } catch (error : any) {

      this.isLoading = false;
      // 检查错误对象并提取错误消息
      const errorMessage = error?.error?.message || 'An unknown error occurred during registration';
      // 使用提取的错误消息
      this.data.error(errorMessage);
    }
  }

  async toggleVisibility(review: any): Promise<void> {
    const updatedHiddenStatus = !review.isHidden;

    try {
      const url = `http://localhost:3000/api/phones/${review.phoneId}/reviews/${review._id}/visibility`;
      const data = await this.rest.patch(url,
        { isHidden: updatedHiddenStatus });
      console.log('qianduan  ',data.success)
      if (data.success) {
        console.log('切换成功，后端返回:', data);
        review.isHidden = updatedHiddenStatus; // 立即更新本地状态
      } else {
        this.data.error(data.message || 'toggle Visibility request failed');
      }

    } catch (error: any) {
      const errorMessage = error?.error?.message || 'An unknown error occurred during toggle Visibility';
      // 使用提取的错误消息
      this.data.error(errorMessage);
    }
  }

  // 返回按钮
  goBack() {
    this.router.navigate(['/user-profile']);
  }

  // Apply client-side pagination
  applyPagination(): void {
    this.totalPages = Math.ceil(this.allReviews.length / this.pageSize) || 1;
    if (this.currentPage > this.totalPages) {
      this.currentPage = 1;
    }
    const startIndex = (this.currentPage - 1) * this.pageSize;
    this.reviews = this.allReviews.slice(startIndex, startIndex + this.pageSize);
  }

  // Handle page change from pagination component
  onPageChange(page: number): void {
    this.currentPage = page;
    this.applyPagination();
  }
}
