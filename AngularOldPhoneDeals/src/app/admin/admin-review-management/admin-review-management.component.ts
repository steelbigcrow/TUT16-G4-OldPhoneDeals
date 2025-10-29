import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { CommonModule } from '@angular/common';
import { AdminService } from '../../services/admin.service';
import { DataService } from '../../services/data.service';
import { PaginationComponent } from '../../shared/pagination/pagination.component';

interface Review {
  id: string;
  phoneId: string;
  content: string;
  rating: number;
  user: string;
  listing: string;
  isVisible: boolean;
  createdAt: Date;
  isExpanded?: boolean;
}

@Component({
  selector: 'app-admin-review-management',
  templateUrl: './admin-review-management.component.html',
  styleUrls: ['./admin-review-management.component.scss'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, PaginationComponent]
})
export class AdminReviewManagementComponent implements OnInit {
  reviews: Review[] = [];
  filteredReviews: Review[] = [];
  searchForm: FormGroup;
  selectedReview: Review | null = null;
  isEditing = false;
  loading = false;

  // Pagination properties
  currentPage = 1;
  itemsPerPage = 10;
  totalPages = 1;
  totalItems = 0;

  // Sorting state
  sortOption: string = 'default';
  sortBy: string = 'createdAt';
  sortOrder: string = 'desc';

  constructor(
    private fb: FormBuilder,
    private adminService: AdminService,
    private data: DataService,
  ) {
    this.searchForm = this.fb.group({
      searchTerm: [''],
      visibility: ['all']
    });
  }

  ngOnInit(): void {
    this.loadReviews();
    this.setupSearch();
  }

  async loadReviews(): Promise<void> {
    this.loading = true;
    try {
      const { searchTerm, visibility } = this.searchForm.value;
      const response = await this.adminService.getReviews(
        this.currentPage,
        this.itemsPerPage,
        this.sortBy,
        this.sortOrder,
        searchTerm,
        visibility
      );
      
      if (response.success) {
        this.reviews = response.reviews;
        this.filteredReviews = [...this.reviews];
        this.totalItems = response.total;
        this.totalPages = Math.ceil(this.totalItems / this.itemsPerPage);
        // initialize expansion flags
        this.filteredReviews.forEach(r => (r as any).isExpanded = false);
      } else {
        this.data.error(response.message || 'Failed to load reviews');
      }
    } catch (error: any) {
      this.data.error(error.message || 'An error occurred while loading reviews');
    } finally {
      this.loading = false;
    }
  }

  setupSearch(): void {
    this.searchForm.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b))
      )
      .subscribe(() => {
        this.currentPage = 1;
        this.loadReviews();
      });
  }

  // Content expand/collapse
  toggleExpand(review: any): void {
    review.isExpanded = !review.isExpanded;
  }

  editReview(review: Review): void {
    this.selectedReview = { ...review };
    this.isEditing = true;
  }

  async saveReview(): Promise<void> {
    if (this.selectedReview) {
      try {
        const response = await this.adminService.updateReview(
          this.selectedReview.id,
          this.selectedReview
        );

        if (response.success) {
          this.data.success('Review updated successfully');
          await this.loadReviews();
          this.isEditing = false;
          this.selectedReview = null;
        } else {
          this.data.error(response.message || 'Failed to update review');
        }
      } catch (error: any) {
        this.data.error(error.message || 'An error occurred while updating review');
      }
    }
  }

  async toggleReviewVisibility(review: Review): Promise<void> {
    const action = review.isVisible ? 'hide' : 'show';
    if (!window.confirm(`Are you sure you want to ${action} this review?`)) {
      return;
    }
    try {
      // Compute isHidden: hide if currently visible, show if currently hidden
      const isHidden = review.isVisible;
      const response = await this.adminService.toggleReviewVisibility(
        review.phoneId,
        review.id,
        isHidden
      );

      if (response.success) {
        this.data.success(`Review ${review.isVisible ? 'hidden' : 'shown'} successfully`);
        await this.loadReviews();
      } else {
        this.data.error(response.message || 'Failed to update review visibility');
      }
    } catch (error: any) {
      this.data.error(error.message || 'An error occurred while updating review visibility');
    }
  }

  async deleteReview(review: Review): Promise<void> {
    if (!window.confirm('Are you sure you want to delete this review?')) {
      return;
    }
    try {
      const response = await this.adminService.deleteReview(review.id);
      if (response.success) {
        this.data.success('Review deleted successfully');
        await this.loadReviews();
      } else {
        this.data.error(response.message || 'Failed to delete review');
      }
    } catch (error: any) {
      this.data.error(error.message || 'An error occurred while deleting review');
    }
  }

  cancelEdit(): void {
    this.isEditing = false;
    this.selectedReview = null;
  }

  // Handle pagination
  async onPageChange(page: number): Promise<void> {
    this.currentPage = page;
    await this.loadReviews();
  }

  // Handle sort selection
  onSortChange(option: string): void {
    this.sortOption = option;
    switch (option) {
      case 'rating_desc':
        this.sortBy = 'rating'; this.sortOrder = 'desc'; break;
      case 'rating_asc':
        this.sortBy = 'rating'; this.sortOrder = 'asc'; break;
      case 'createdAt_asc':
        this.sortBy = 'createdAt'; this.sortOrder = 'asc'; break;
      case 'createdAt_desc':
        this.sortBy = 'createdAt'; this.sortOrder = 'desc'; break;
      default:
        this.sortBy = 'createdAt'; this.sortOrder = 'desc'; break;
    }
    this.currentPage = 1;
    this.loadReviews();
  }
} 