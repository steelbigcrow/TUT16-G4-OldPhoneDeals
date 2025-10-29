import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, FormsModule, Validators } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { CommonModule } from '@angular/common';
import { PaginationComponent } from '../../shared/pagination/pagination.component';
import { AdminService } from '../../services/admin.service';
import { DataService } from '../../services/data.service';
import { User } from '../../models/user.model';
import { Phone } from '../../models/phone.model';
import { Review } from '../../models/review.model';

@Component({
  selector: 'app-admin-user-management',
  templateUrl: './admin-user-management.component.html',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, PaginationComponent]
})
export class AdminUserManagementComponent implements OnInit {
  users: User[] = [];
  filteredUsers: User[] = [];
  searchForm: FormGroup;
  selectedUser: User | null = null;
  isEditing = false;
  loading = false;
  editForm!: FormGroup;
  editErrorMessage: string = '';

  // Pagination properties
  currentPage = 1;
  itemsPerPage = 10;
  totalPages = 1;
  totalItems = 0;
  paginatedUsers: User[] = [];

  // Modal flags and data
  showPhonesModal = false;
  showReviewsModal = false;
  userPhones: Phone[] = [];
  userReviews: any[] = [];
  // Review pagination properties
  reviewUserId: string = '';
  reviewCurrentPage: number = 1;
  reviewItemsPerPage: number = 4;
  reviewTotalItems: number = 0;
  reviewTotalPages: number = 1;
  // Review sorting and filtering state
  reviewSortOption: string = 'default';
  reviewSortBy: string = 'createdAt';
  reviewSortOrder: string = 'desc';
  reviewFilterBrand: string = 'all';
  reviewBrands: string[] = ['Samsung', 'Apple', 'HTC', 'Huawei', 'Nokia', 'LG', 'Motorola', 'Sony', 'BlackBerry'];
  // Sort and filter state
  sortBy: string = 'createdAt';
  sortOrder: string = 'desc';
  filterStatus: string = 'all'; // 'all', 'disabled', 'active'
  currentSortOption: string = 'default';

  // FIRST_EDIT: add phone listing pagination, sorting, filtering state
  phoneUserId: string = '';
  phoneCurrentPage: number = 1;
  phoneItemsPerPage: number = 10;
  phoneTotalItems: number = 0;
  phoneTotalPages: number = 1;
  phoneSortOption: string = 'default';
  phoneSortBy: string = 'createdAt';
  phoneSortOrder: string = 'desc';
  phoneFilterBrand: string = 'all';
  brands: string[] = ['Samsung', 'Apple', 'HTC', 'Huawei', 'Nokia', 'LG', 'Motorola', 'Sony', 'BlackBerry'];

  constructor(
    private fb: FormBuilder,
    private adminService: AdminService,
    private data: DataService
  ) {
    this.searchForm = this.fb.group({
      searchTerm: ['']
    });
  }

  ngOnInit(): void {
    this.loadUsers();
    this.setupSearch();
  }

  setupSearch(): void {
    this.searchForm.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b))
      )
      .subscribe(() => {
        this.currentPage = 1;
        this.loadUsers();
      });
  }

  async loadUsers(): Promise<void> {
    this.loading = true;
    try {
      const { searchTerm } = this.searchForm.value;
      // fetch with server-side filtering
      const response = await this.adminService.getUsers(
        this.currentPage,
        this.itemsPerPage,
        this.sortBy,
        this.sortOrder,
        searchTerm,
        undefined,
        this.filterStatus === 'disabled' ? true : this.filterStatus === 'active' ? false : undefined
      );
      
      if (response.success) {
        const rawItems = response.data?.items ?? response.users ?? [];
        const mapped: User[] = rawItems.map((u: any) => ({
          id: u._id,
          firstName: u.firstName,
          lastName: u.lastName,
          email: u.email,
          isVerified: u.isVerified,
          isAdmin: u.isAdmin,
          isDisabled: u.isDisabled,
          isBan: u.isBan,
          wishlist: u.wishlist ?? [],
          address: u.address,
          lastLogin: u.lastLogin,
          createdAt: u.createdAt,
          updatedAt: u.updatedAt,
        }));
        // server-side filtering applied, just assign results
        const total = response.data?.total ?? response.total ?? mapped.length;
        this.users = mapped;
        this.filteredUsers = mapped;
        this.totalItems = total;
        this.totalPages = Math.ceil(this.totalItems / this.itemsPerPage);
        this.paginatedUsers = mapped;
      } else {
        this.data.error(response.message || 'Failed to load users');
      }
    } catch (error: any) {
      this.data.error(error.message || 'An error occurred while loading users');
    } finally {
      this.loading = false;
    }
  }

  async onPageChange(page: number): Promise<void> {
    this.currentPage = page;
    await this.loadUsers();
  }

  editUser(user: User): void {
    this.selectedUser = { ...user };
    this.editErrorMessage = '';
    this.editForm = this.fb.group({
      firstName: [user.firstName, [Validators.required]],
      lastName: [user.lastName, [Validators.required]],
      email: [user.email, [Validators.required, Validators.pattern(/^[^\s@]+@[^\s@]+\.com$/i)]]
    });
    this.isEditing = true;
  }

  async saveUser(): Promise<void> {
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }
    if (this.selectedUser) {
      this.selectedUser.firstName = this.editForm.value.firstName;
      this.selectedUser.lastName = this.editForm.value.lastName;
      this.selectedUser.email = this.editForm.value.email;
      try {
        const response = await this.adminService.updateUser(
          this.selectedUser.id,
          this.selectedUser
        );
        if (response.success) {
          this.data.success('User updated successfully');
          await this.loadUsers();
          this.isEditing = false;
          this.selectedUser = null;
          this.editErrorMessage = '';
        } else {
          this.editErrorMessage = response.message || 'Failed to update user';
        }
      } catch (error: any) {
        this.editErrorMessage = error.message || 'An error occurred while updating user';
      }
    }
  }

  async updateUserStatus(userId: string, isDisabled: boolean): Promise<void> {
    try {
      const response = await this.adminService.updateUserStatus(userId, isDisabled);

      if (response.success) {
        this.data.success(`User ${isDisabled ? 'disabled' : 'activated'} successfully`);
        await this.loadUsers();
      } else {
        this.data.error(response.message || 'Failed to update user status');
      }
    } catch (error: any) {
      this.data.error(error.message || 'An error occurred while updating user status');
    }
  }

  // Provide a confirmation prompt before disabling or activating a user
  async toggleUserStatus(user: User): Promise<void> {
    const action = user.isDisabled ? 'activate' : 'disable';
    const confirmMessage = `Are you sure you want to ${action} this user?`;
    if (!window.confirm(confirmMessage)) {
      return;
    }
    await this.updateUserStatus(user.id, !user.isDisabled);
  }

  async deleteUser(userId: string): Promise<void> {
    if (!window.confirm('Are you sure you want to delete this user?')) {
      return;
    }
    try {
      const response = await this.adminService.deleteUser(userId);
      if (response.success) {
        this.data.success('User deleted successfully');
        await this.loadUsers();
      } else {
        this.data.error(response.message || 'Failed to delete user');
      }
    } catch (error: any) {
      this.data.error(error.message || 'An error occurred while deleting user');
    }
  }

  cancelEdit(): void {
    this.isEditing = false;
    this.selectedUser = null;
    this.editErrorMessage = '';
  }

  // SECOND_EDIT: update openSellingPhones to initialize and load with pagination, sorting, filtering
  async openSellingPhones(user: User): Promise<void> {
    this.phoneUserId = user.id;
    this.phoneCurrentPage = 1;
    this.phoneSortOption = 'default';
    this.phoneSortBy = 'createdAt';
    this.phoneSortOrder = 'desc';
    this.phoneFilterBrand = 'all';
    await this.loadUserPhones(1);
    this.showPhonesModal = true;
  }

  async openReviews(user: User): Promise<void> {
    // Initialize pagination, sorting, filtering and load first page of reviews
    this.reviewUserId = user.id;
    this.reviewCurrentPage = 1;
    this.reviewSortOption = 'default';
    this.reviewSortBy = 'createdAt';
    this.reviewSortOrder = 'desc';
    this.reviewFilterBrand = 'all';
    await this.loadUserReviews(1);
    this.showReviewsModal = true;
  }

  private async loadUserReviews(page: number): Promise<void> {
    try {
      const response = await this.adminService.getUserReviews(
        this.reviewUserId,
        page,
        this.reviewItemsPerPage,
        this.reviewSortBy,
        this.reviewSortOrder,
        this.reviewFilterBrand === 'all' ? undefined : this.reviewFilterBrand
      );
      if (response.success) {
        // set up expansion flags for each review
        this.userReviews = response.reviews.map((r: any) => ({ ...r, isExpanded: false }));
        this.reviewTotalItems = response.total;
        this.reviewTotalPages = Math.ceil(this.reviewTotalItems / this.reviewItemsPerPage);
      } else {
        this.data.error(response.message || 'Failed to load user reviews');
      }
    } catch (error: any) {
      this.data.error(error.message || 'An error occurred while loading user reviews');
    }
  }

  async onReviewPageChange(page: number): Promise<void> {
    this.reviewCurrentPage = page;
    await this.loadUserReviews(page);
  }

  // Toggle expanded state of a review comment
  toggleReviewExpand(review: any): void {
    review.isExpanded = !review.isExpanded;
  }

  // Handle sort changes
  onSortChange(option: string): void {
    this.currentSortOption = option;
    switch (option) {
      case 'createdAt_asc':
        this.sortBy = 'createdAt'; this.sortOrder = 'asc'; break;
      case 'lastLogin_desc':
        this.sortBy = 'lastLogin'; this.sortOrder = 'desc'; break;
      case 'lastLogin_asc':
        this.sortBy = 'lastLogin'; this.sortOrder = 'asc'; break;
      default:
        // 'default' or 'createdAt_desc'
        this.sortBy = 'createdAt'; this.sortOrder = 'desc'; break;
    }
    this.currentPage = 1;
    this.loadUsers();
  }

  // Handle filter changes
  onFilterChange(status: string): void {
    this.filterStatus = status;
    this.currentPage = 1;
    this.loadUsers();
  }

  // THIRD_EDIT: add methods to handle loading, paging, sorting, filtering for user phones
  private async loadUserPhones(page: number): Promise<void> {
    try {
      const response = await this.adminService.getUserPhones(
        this.phoneUserId,
        page,
        this.phoneItemsPerPage,
        this.phoneSortBy,
        this.phoneSortOrder,
        this.phoneFilterBrand === 'all' ? undefined : this.phoneFilterBrand
      );
      if (response.success) {
        this.userPhones = response.phones;
        this.phoneTotalItems = response.total;
        this.phoneTotalPages = Math.ceil(this.phoneTotalItems / this.phoneItemsPerPage);
      } else {
        this.data.error(response.message || 'Failed to load user phones');
      }
    } catch (error: any) {
      this.data.error(error.message || 'An error occurred while loading user phones');
    }
  }

  async onPhonePageChange(page: number): Promise<void> {
    this.phoneCurrentPage = page;
    await this.loadUserPhones(page);
  }

  onPhoneSortChange(option: string): void {
    this.phoneSortOption = option;
    switch (option) {
      case 'price_desc':
        this.phoneSortBy = 'price'; this.phoneSortOrder = 'desc'; break;
      case 'price_asc':
        this.phoneSortBy = 'price'; this.phoneSortOrder = 'asc'; break;
      case 'stock_desc':
        this.phoneSortBy = 'stock'; this.phoneSortOrder = 'desc'; break;
      case 'stock_asc':
        this.phoneSortBy = 'stock'; this.phoneSortOrder = 'asc'; break;
      default:
        this.phoneSortBy = 'createdAt'; this.phoneSortOrder = 'desc'; break;
    }
    this.phoneCurrentPage = 1;
    this.loadUserPhones(1);
  }

  onPhoneFilterBrandChange(brand: string): void {
    this.phoneFilterBrand = brand;
    this.phoneCurrentPage = 1;
    this.loadUserPhones(1);
  }

  // Handle review sorting
  onReviewSortChange(option: string): void {
    this.reviewSortOption = option;
    switch (option) {
      case 'rating_desc': this.reviewSortBy = 'rating'; this.reviewSortOrder = 'desc'; break;
      case 'rating_asc': this.reviewSortBy = 'rating'; this.reviewSortOrder = 'asc'; break;
      case 'createdAt_desc': this.reviewSortBy = 'createdAt'; this.reviewSortOrder = 'desc'; break;
      case 'createdAt_asc': this.reviewSortBy = 'createdAt'; this.reviewSortOrder = 'asc'; break;
      default: this.reviewSortBy = 'createdAt'; this.reviewSortOrder = 'desc'; break;
    }
    this.reviewCurrentPage = 1;
    this.loadUserReviews(1);
  }

  // Handle review brand filtering
  onReviewFilterBrandChange(brand: string): void {
    this.reviewFilterBrand = brand;
    this.reviewCurrentPage = 1;
    this.loadUserReviews(1);
  }
} 