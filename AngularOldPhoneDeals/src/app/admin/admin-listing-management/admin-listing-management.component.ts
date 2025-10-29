import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { CommonModule } from '@angular/common';
import { PaginationComponent } from '../../shared/pagination/pagination.component';
import { AdminService } from '../../services/admin.service';
import { DataService } from '../../services/data.service';
import {RestApiService} from '../../services/rest-api.service';

interface Listing {
  _id: string;
  title: string;
  brand: string;
  image: string;
  price: number;
  stock: number;
  seller: {
    _id: string;
    firstName: string;
    lastName: string;
    email: string;
  };
  isDisabled: boolean;
  createdAt: Date;
  reviews: number;
}

@Component({
  selector: 'app-admin-listing-management',
  templateUrl: './admin-listing-management.component.html',
  styleUrls: ['./admin-listing-management.component.scss'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, PaginationComponent]
})
export class AdminListingManagementComponent implements OnInit {
  listings: Listing[] = [];
  // filteredListings: Listing[] = [];
  searchForm: FormGroup;
  selectedListing: Listing | null = null;
  isEditing = false;
  brands: string[] = ['Samsung', 'Apple', 'HTC', 'Huawei', 'Nokia', 'LG', 'Motorola', 'Sony', 'BlackBerry'];
  loading = false;

  // Pagination properties
  currentPage = 1;
  itemsPerPage = 10;
  totalPages = 1;
  totalItems = 0;
  paginatedListings: Listing[] = [];
  // upload image
  selectedFile: File | null = null;
  private serverBaseUrl = 'http://localhost:3000';
  // reviews
  selectedReviewListing: any = null; // current clicked listing
  protected readonly Array = Array;
  // omit review
  toggleStates: { [reviewId: string]: boolean } = {};
  constructor(
    private fb: FormBuilder,
    private adminService: AdminService,
    private data: DataService,
    private rest : RestApiService
  ) {
    this.searchForm = this.fb.group({
      searchTerm: [''],
      brand: [''],
      sortBy: ['']
    });
  }

  ngOnInit(): void {
    this.loadListings();
    this.setupSearch();
  }

  async loadListings(): Promise<void> {
    this.loading = true;
    try {
      const { searchTerm, brand , sortBy} = this.searchForm.value;

      let sortField = 'createdAt';
      let sortOrder: 'asc' | 'desc' = 'desc';

      switch (sortBy) {
        case 'priceAsc':
          sortField = 'price';
          sortOrder = 'asc';
          break;
        case 'priceDesc':
          sortField = 'price';
          sortOrder = 'desc';
          break;
        case 'dateAsc':
          sortField = 'createdAt';
          sortOrder = 'asc';
          break;
        case 'dateDesc':
          sortField = 'createdAt';
          sortOrder = 'desc';
          break;
      }

      const response = await this.adminService.getListings(
        this.currentPage,
        this.itemsPerPage,
        sortField,
        sortOrder,
        searchTerm,  // search condition
        brand        // brand filter
      );

      if (response.success) {

        this.paginatedListings = response.phones;

        console.log("backend admin listing  this.paginatedListings",  this.paginatedListings)

        this.totalItems = response.total;
        this.totalPages = Math.ceil(this.totalItems / this.itemsPerPage);

      } else {
        this.data.error(response.message || 'Failed to load listings');
      }
    } catch (error: any) {
      this.data.error(error.message || 'An error occurred while loading listings');
    } finally {
      this.loading = false;
    }
  }


  setupSearch(): void {
    this.searchForm.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(values => {
        this.currentPage = 1;
        this.loadListings();
      });
  }


  async onPageChange(page: number): Promise<void> {
    this.currentPage = page;
    await this.loadListings();
  }

  editListing(listing: Listing): void {
    this.selectedListing = { ...listing };
    console.log('Editing listing ID:', this.selectedListing?._id);
    this.isEditing = true;
  }

  async saveListing(): Promise<void> {
    if (this.selectedListing) {
      try {




        // first step: upload image (if a file is selected)
        if (this.selectedFile) {
          const formData = new FormData();
          formData.append('image', this.selectedFile);

          const uploadResponse = await this.rest.post('http://localhost:3000/api/phones/upload-image', formData);
          console.log("frontend selectedFile check image upload", uploadResponse );
          if (uploadResponse.success && uploadResponse.url) {
            this.selectedListing.image = uploadResponse?.url; // 服务器返回的图片URL
            this.selectedFile = null;
          } else {
            this.data.error(uploadResponse.message || 'Failed to upload image');
            return;
          }
        }

          // second step: request update
        if (this.selectedListing && this.selectedListing._id) {
          const admin = JSON.parse(localStorage.getItem('admin') || '{}');
          const adminId = admin.id;
          const response = await this.adminService.updateListing(
            this.selectedListing._id,
            {...this.selectedListing, adminId: adminId }
          );
          if (response.success) {
            this.data.success('Listing updated successfully');
            await this.loadListings();
            this.isEditing = false;
            this.selectedListing = null;
          } else {
            this.data.error(response.message || 'Failed to update listing');
          }
        }


      } catch (error: any) {
        this.data.error(error.message || 'An error occurred while updating listing');
      }
    }
  }

  async toggleListingStatus(listing: Listing): Promise<void> {
    const newStatus = !listing.isDisabled;

    if (!window.confirm(`Are you sure you want to ${newStatus ? 'disable' : 'enable'} listing "${listing.title}"?`)) {
      return;
    }

    try {
      const admin = JSON.parse(localStorage.getItem("admin") || '{}');


      const response = await this.adminService.updateListing(listing._id,
        { isDisabled: newStatus , adminId : admin.id});




      if (response.success) {
        this.data.success(`Listing ${newStatus ? 'disabled' : 'enabled'} successfully`);
        await this.loadListings();
      } else {
        this.data.error(response.message || 'Failed to update listing status');
      }
    } catch (error: any) {
      this.data.error(error.message || 'An error occurred while updating listing status');
    }
  }


  async deleteListing(listing: Listing): Promise<void> {
    if (!window.confirm(`Are you sure you want to delete listing "${listing.title}"?`)) {
      return;
    }
    try {
      const admin = JSON.parse(localStorage.getItem('admin') || '{}');
      const adminId = admin.id;


      const response = await this.rest.delete(`http://localhost:3000/api/admin/phones/${listing._id}?adminId=${adminId}`)
      if (response.success) {
        this.data.success('Listing deleted successfully');
        await this.loadListings();
      } else {
        this.data.error(response.message || 'Failed to delete listing');
      }
    } catch (error: any) {
      this.data.error(error.message || 'An error occurred while deleting listing');
    }
  }

  cancelEdit(): void {
    this.isEditing = false;
    this.selectedListing = null;
  }

  getImageUrl(imagePath: string): string {
    if (!imagePath) return '';
    return this.serverBaseUrl + (imagePath.startsWith('/') ? imagePath : '/' + imagePath);
  }

  // file selection processing
  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      // do not upload here, delay to createPhone and handle it uniformly
    }
  }



  viewAllReviews(listing: any): void {
    this.selectedReviewListing = listing;
  }
  closeReviewModal(): void {
    this.selectedReviewListing = null;
  }
  toggleReview(reviewId: string): void {
    this.toggleStates[reviewId] = !this.toggleStates[reviewId];
  }

  isExpanded(reviewId: string): boolean {
    return this.toggleStates[reviewId];
  }
}
