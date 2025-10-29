import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { HttpClient, HttpParams } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Phone } from '../models/phone.model';
import { PaginationComponent } from '../shared/pagination/pagination.component';
import { PhoneCardComponent } from '../shared/phone-card/phone-card.component';

@Component({
  selector: 'app-user-search',
  templateUrl: './user-search.component.html',
  styleUrls: ['./user-search.component.scss'],
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, PaginationComponent, PhoneCardComponent]
})
export class UserSearchComponent implements OnInit {
  // Search parameters
  searchQuery: string | null = null;
  selectedBrand: string | null = null;
  maxPrice: number | null = null;
  // sorting
  sortingOption: string = 'default';
  // Pagination parameters
  totalPages = 1;
  currentPage = 1;
  pageSize = 10;
  totalResults = 0;
  
  // brand options
  brands = [
    'Apple',
    'Samsung',
    'BlackBerry',
    'Sony',
    'Huawei',
    'HTC',
    'Nokia',
    'LG',
    'Motorola'
  ];
  
  // Phone listings
  searchResults: Phone[] = [];
  allSearchResults: Phone[] = [];
  isClientSidePagination = false;
  
  // Loading state
  loadingSearch = false;
  
  // Base URL for backend server to load static images
  public serverBaseUrl = 'http://localhost:3000';

  // Helper to construct full image URL
  getImageUrl(imagePath: string): string {
    if (!imagePath) {
      return '';
    }
    let effectiveImagePath = imagePath;
    if (!imagePath.startsWith('/')) {
      effectiveImagePath = '/' + imagePath;
    }
    return this.serverBaseUrl + effectiveImagePath;
  }

  constructor(
    private route: ActivatedRoute,
    private http: HttpClient,
    private router: Router
  ) {}
  
  ngOnInit(): void {
    this.route.queryParamMap.subscribe(params => {
      this.searchQuery = params.get('q');
      this.selectedBrand = params.get('brand');
      const priceParam = params.get('maxPrice');
      this.maxPrice = priceParam ? parseFloat(priceParam) : null;
      
      const sortParam = params.get('sort');
      this.sortingOption = sortParam || 'default';

      const pageParam = params.get('page');
      this.currentPage = pageParam ? parseInt(pageParam, 10) : 1;
      const limitParam = params.get('limit');
      this.pageSize = limitParam ? parseInt(limitParam, 10) : 10;

      // If sorting is applied (not default), we want to fetch all results
      // and handle sorting and pagination on the client side
      if (this.sortingOption !== 'default') {
        this.fetchAllResults();
      } else {
        this.isClientSidePagination = false;
        this.fetchPagedResults();
      }
    });
  }
  
  fetchPagedResults(): void {
    this.loadingSearch = true;

    let httpParams = new HttpParams()
      .set('page', this.currentPage.toString())
      .set('limit', this.pageSize.toString());
    if (this.searchQuery) {
      httpParams = httpParams.set('search', this.searchQuery);
    }
    if (this.selectedBrand) {
      httpParams = httpParams.set('brand', this.selectedBrand);
    }
    if (this.maxPrice !== null) {
      httpParams = httpParams.set('maxPrice', this.maxPrice.toString());
    }

    // Fetch data from backend with pagination
    this.http.get<{ phones: Phone[]; totalPages: number; currentPage: number; total: number }>(
      '/api/phones',
      { params: httpParams }
    ).subscribe({
      next: (resp) => {
        this.searchResults = resp.phones;
        this.totalPages = resp.totalPages;
        this.currentPage = resp.currentPage;
        this.totalResults = resp.total;
        this.loadingSearch = false;
      },
      error: (error) => {
        console.error('Error searching phones:', error);
        this.loadingSearch = false;
      }
    });
  }
  
  fetchAllResults(): void {
    this.loadingSearch = true;

    // Create a large limit to fetch all results at once
    // (backend should have a max limit to prevent overloading)
    let httpParams = new HttpParams()
      .set('page', '1')
      .set('limit', '1000'); // Use a large number to get all results
      
    if (this.searchQuery) {
      httpParams = httpParams.set('search', this.searchQuery);
    }
    if (this.selectedBrand) {
      httpParams = httpParams.set('brand', this.selectedBrand);
    }
    if (this.maxPrice !== null) {
      httpParams = httpParams.set('maxPrice', this.maxPrice.toString());
    }

    // Fetch all data from backend
    this.http.get<{ phones: Phone[]; totalPages: number; currentPage: number; total: number }>(
      '/api/phones',
      { params: httpParams }
    ).subscribe({
      next: (resp) => {
        this.allSearchResults = resp.phones;
        this.totalResults = resp.total;
        
        // Enable client-side pagination
        this.isClientSidePagination = true;
        
        // Apply sorting to all results
        this.applySorting();
        
        // Set up client-side pagination
        this.applyClientSidePagination();
        
        this.loadingSearch = false;
      },
      error: (error) => {
        console.error('Error fetching all phones:', error);
        this.loadingSearch = false;
        // Fallback to regular paginated fetch
        this.isClientSidePagination = false;
        this.fetchPagedResults();
      }
    });
  }
  
  applySorting(): void {
    if (this.sortingOption && this.sortingOption !== 'default') {
      const results = this.isClientSidePagination ? this.allSearchResults : this.searchResults;
      
      switch (this.sortingOption) {
        case 'price_desc':
          results.sort((a, b) => b.price - a.price);
          break;
        case 'price_asc':
          results.sort((a, b) => a.price - b.price);
          break;
        case 'stock_desc':
          results.sort((a, b) => b.stock - a.stock);
          break;
        case 'stock_asc':
          results.sort((a, b) => a.stock - b.stock);
          break;
      }
      
      // If client-side pagination, we need to update the page slice
      if (this.isClientSidePagination) {
        this.applyClientSidePagination();
      }
    }
  }
  
  applyClientSidePagination(): void {
    // Calculate total pages
    this.totalPages = Math.ceil(this.allSearchResults.length / this.pageSize);
    
    // Ensure current page is valid
    if (this.currentPage > this.totalPages) {
      this.currentPage = 1;
    }
    
    // Get the slice of results for the current page
    const startIndex = (this.currentPage - 1) * this.pageSize;
    const endIndex = Math.min(startIndex + this.pageSize, this.allSearchResults.length);
    this.searchResults = this.allSearchResults.slice(startIndex, endIndex);
  }
  
  // Update search parameters and execute search
  updateSearch(): void {
    const queryParams: any = {
      q: this.searchQuery,
      brand: this.selectedBrand,
      maxPrice: this.maxPrice,
      sort: this.sortingOption,
      page: 1, // Reset to first page when filters change
      limit: this.pageSize
    };
    
    // Navigate with updated parameters
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
      queryParamsHandling: 'merge' // keep other parameters not in queryParams
    });
  }
  
  // Update sorting and maintain current search
  updateSorting(): void {
    if (this.sortingOption !== 'default' && !this.isClientSidePagination) {
      // If switching to a sorted view, fetch all results first
      this.fetchAllResults();
    } else if (this.isClientSidePagination) {
      // If already in client-side pagination mode, just apply the new sorting
      this.applySorting();
    }
    
    // Update URL to include sort parameter
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { sort: this.sortingOption },
      queryParamsHandling: 'merge'
    });
  }
  
  // Handle page change from pagination component
  onPageChange(page: number): void {
    this.currentPage = page;
    
    if (this.isClientSidePagination) {
      // If client-side pagination, just update the page slice
      this.applyClientSidePagination();
    } else {
      // Otherwise navigate to update the URL and fetch new page
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { page },
        queryParamsHandling: 'merge'
      });
    }
  }
  
  // Clear all search conditions
  clearSearchFilters(): void {
    this.searchQuery = null;
    this.selectedBrand = null;
    this.maxPrice = null;
    this.sortingOption = 'default';
    this.isClientSidePagination = false;
    this.updateSearch();
  }
  
  // View phone details - redirects to phone detail component
  viewPhoneDetails(phone: Phone): void {
    this.router.navigate(['/phone', phone.id]);
  }
  
  // Go back to home
  goToHome(): void {
    this.router.navigate(['/user-home']);
  }
} 