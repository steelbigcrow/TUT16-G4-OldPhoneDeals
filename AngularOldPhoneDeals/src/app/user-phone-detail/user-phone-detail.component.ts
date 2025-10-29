import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Phone } from '../models/phone.model';
import { Review } from '../models/review.model';
import { User } from '../models/user.model';
import { Observable, Subject, takeUntil, tap } from 'rxjs'; // Removed switchMap if not used elsewhere
import { ReviewItemComponent } from '../shared/review-item/review-item.component';
import { RestApiService } from '../services/rest-api.service';
import { CartService } from '../services/cart.service';
import { DataService } from '../services/data.service';

@Component({
  selector: 'app-user-phone-detail',
  templateUrl: './user-phone-detail.component.html',
  styleUrls: ['./user-phone-detail.component.scss'],
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ReviewItemComponent]
})
export class UserPhoneDetailComponent implements OnInit, OnDestroy {
  phone$: Observable<Phone | null> | undefined;
  phone: Phone | null = null;
  reviews: Review[] = [];
  
  // For reviews pagination
  currentPage = 1;
  reviewsPerPage = 3;
  totalReviews = 0;
  totalPages = 0;
  
  // For adding a new review
  newReviewRating = 5;
  newReviewComment = '';
  
  // Error and success messages
  errorMessage: string | null = null;
  successMessage: string | null = null;
  
  // Loading states
  isLoadingPhone = false;
  isLoadingReviews = false;
  isSubmittingReview = false;
  
  // currentUser: User | null = null; // Commented out
  private destroy$ = new Subject<void>();
  currentUserId: string = '';
  cartQuantity: number = 0; // Track the current quantity in cart

  public serverBaseUrl = 'http://localhost:3000';
  showQuantityModal: boolean = false;
  selectedQuantity: number = 1;
  maxQuantity: number = 0;

  constructor(
    private http: HttpClient,
    private router: Router,
    private location: Location,
    private route: ActivatedRoute,
    private restApiService: RestApiService,
    private cartService: CartService,
    private data: DataService
    // private authService: AuthService // Commented out
  ) {}
  
  ngOnInit(): void {
    this.isLoadingPhone = true;
    const phoneId = this.route.snapshot.paramMap.get('id');
    if (phoneId) {
      this.http.get<Phone>(
        `/api/phones/${phoneId}`,
        { headers: this.restApiService.getHeaders() }
      ).pipe(
        takeUntil(this.destroy$)
      ).subscribe({
        next: (phoneData) => {
          // Map server data and extract sellerId from populated seller object
          const mappedPhone: any = {
            ...phoneData,
            sellerId: (phoneData.seller as any)?._id || ''
          };
          this.phone = mappedPhone as Phone;
          // Map server 'reviewer' field to frontend 'reviewerName'
          this.reviews = (mappedPhone.reviews || []).map((r: any) => ({
            ...r,
            reviewerName: r.reviewer,
            isExpanded: false
          }));
          this.isLoadingPhone = false;

          // Check if seller details need to be fetched
          if (this.phone && !this.phone.seller && this.phone.sellerId) {
            this.fetchSellerDetails(this.phone.sellerId);
          }
          
          this.loadTotalReviewCount(phoneId);
          
          // Load cart to check if this phone is in it
          this.fetchCartQuantity(phoneId);
        },
        error: (err) => {
          console.error('Error fetching phone details:', err);
          this.errorMessage = 'Failed to load phone details.';
          this.isLoadingPhone = false;
        }
      });
    }

    // get current user information, extract id
    this.restApiService.getUserInfo()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (user: any) => {
          // backend return format: { success: true, user: { id: '...' , ... } }
          // also compatible with previous return { id: '...' }
          this.currentUserId = user?.user?.id || user?.id || '';
        },
        error: () => {
          this.currentUserId = '';
        }
      });

    // Get current user - Commented out
    // this.authService.currentUser$.pipe(takeUntil(this.destroy$)).subscribe((user: User | null) => {
    //   this.currentUser = user;
    // });
  }
  
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
  
  // Fetch seller details if not populated with the phone
  fetchSellerDetails(sellerId: string): void {
    this.http.get<User>(`/api/users/${sellerId}`).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (sellerData) => {
        if (this.phone) {
          this.phone.seller = sellerData;
        }
      },
      error: (err) => {
        console.error(`Error fetching seller details for ID ${sellerId}:`, err);
        // Optionally, handle the error more visibly in the UI
        // For example: this.errorMessage = 'Could not load seller information.';
      }
    });
  }

  // Fetch the total count of reviews to correctly set pagination
  loadTotalReviewCount(phoneId: string): void {
    // Assuming an endpoint to get just review metadata or count might not exist.
    // We'll fetch the first page of reviews again to get the total count.
    // This isn't ideal but works if the backend /api/phones/:phoneId/reviews returns total.
    this.http.get<{reviews: Review[], totalReviews: number}>(
      `/api/phones/${phoneId}/reviews?page=1&limit=${this.reviewsPerPage}`,
      { headers: this.restApiService.getHeaders() }
    )
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.totalReviews = response.totalReviews;
          this.totalPages = Math.ceil(this.totalReviews / this.reviewsPerPage);
        },
        error: (err) => {
          console.error('Error fetching total review count:', err);
        }
      });
  }

  loadMoreReviews(): void {
    if (!this.phone || this.isLoadingReviews || this.reviews.length >= this.totalReviews) {
      return;
    }
    this.isLoadingReviews = true;
    // Fetch next batch of up to 10 reviews
    const limit = 10;
    const page = Math.floor(this.reviews.length / limit) + 1;
    this.http.get<{reviews: Review[], totalReviews: number}>(
      `/api/phones/${this.phone.id}/reviews?page=${page}&limit=${limit}`,
      { headers: this.restApiService.getHeaders() }
    )
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          // check the review data structure returned by the server
          console.log('Server response reviews:', response.reviews);
          // Map server 'reviewer' field to frontend 'reviewerName'
          const mappedReviews = (response.reviews || []).map((r: any) => ({
            ...r,
            reviewerName: r.reviewer,
            reviewerId: r.reviewerId || '',
            isExpanded: false
          }));
          // Filter out already displayed reviews
          const newReviews = mappedReviews.filter(newReview =>
            !this.reviews.some(existingReview => existingReview._id === newReview._id)
          );
          this.reviews = [...this.reviews, ...newReviews];
          // Update total count in case it changed
          this.totalReviews = response.totalReviews;
          this.isLoadingReviews = false;
        },
        error: (err) => {
          console.error('Error loading more reviews:', err);
          this.isLoadingReviews = false;
          this.errorMessage = 'Failed to load more reviews.';
        }
      });
  }

  submitReview(): void {
    const token = localStorage.getItem('token');
    if (!token) {
      // If the user is not logged in, redirect to login page
      this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }
    if (!this.phone || !this.newReviewComment || this.isSubmittingReview) {
      return;
    }
    this.isSubmittingReview = true;
    this.errorMessage = null;
    this.successMessage = null;
    
    const reviewData = {
      rating: this.newReviewRating,
      comment: this.newReviewComment
    };
    
    this.http.post<Review>(
      `/api/phones/${this.phone.id}/reviews`,
      reviewData,
      { headers: this.restApiService.getHeaders() }
    )
    .pipe(takeUntil(this.destroy$))
    .subscribe({
      next: (newReview) => {
        // Map server 'reviewer' to 'reviewerName' and add to the top of the list
        const reviewWithName: any = { ...newReview, reviewerName: (newReview as any).reviewer, isExpanded: false };
        this.reviews = [reviewWithName, ...this.reviews];
        this.totalReviews++;
        this.totalPages = Math.ceil(this.totalReviews / this.reviewsPerPage);
        
        // Reset form
        this.newReviewRating = 5;
        this.newReviewComment = '';
        this.successMessage = 'Review submitted successfully!';
        this.isSubmittingReview = false;

        // Potentially reload phone data if average rating is displayed and calculated server-side and not updated by this POST
      },
      error: (err) => {
        console.error('Error submitting review:', err);
        this.errorMessage = err.error?.message || 'Failed to submit review. You might have already reviewed this item or you are the seller.';
        this.isSubmittingReview = false;
      }
    });
  }

  // Helper to get star rating array
  getStars(rating: number | undefined): boolean[] {
    if (typeof rating === 'undefined') {
      return Array(5).fill(false);
    }
    return Array(5).fill(false).map((_, i) => i < rating);
  }

  // Toggle full/short comment display
  toggleExpand(review: any): void {
    review.isExpanded = !review.isExpanded;
  }

  // Toggle review visibility (for seller or reviewer)
  toggleReviewVisibility(reviewId: string, hidden: boolean): void {
    if (!this.phone) return;
    const review = this.reviews.find(r => r._id === reviewId);
    if (!review) return;

    this.http.patch(
      `/api/phones/${this.phone.id}/reviews/${reviewId}/visibility`, 
      { isHidden: hidden },
      { headers: this.restApiService.getHeaders() }
    )
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          review.isHidden = hidden; // Update local state
          this.successMessage = `Review visibility updated. It is now ${hidden ? 'hidden' : 'visible'}.`;
        },
        error: (err) => {
          console.error('Error toggling review visibility:', err);
          this.errorMessage = err.error?.message || 'Failed to update review visibility.';
        }
      });
  }

  // Helper method to construct the full image URL
  getImageUrl(imagePath: string | undefined): string {
    if (!imagePath) {
      return ''; // Return empty string or a path to a default placeholder image
    }
    // Ensure there's exactly one slash between the base URL and the image path
    let effectiveImagePath = imagePath;
    if (!imagePath.startsWith('/')) {
      effectiveImagePath = '/' + imagePath;
    }
    return this.serverBaseUrl + effectiveImagePath;
  }

  // Go back (preserve history) instead of hard navigating home
  goToHome(): void {
    this.location.back();
  }
  
  // Add phone to cart (will require authentication)
  addToCart(phone: Phone): void {
    const token = localStorage.getItem('token');
    if (!token) {
      this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }
    this.maxQuantity = phone.stock;
    this.selectedQuantity = this.cartQuantity || 0; // Start with current cart quantity
    this.showQuantityModal = true;
  }
  
  // Add phone to wishlist (will require authentication)
  addToWishlist(phone: Phone): void {
    const token = localStorage.getItem('token');
    if (!token) {
      // If the user is not logged in, redirect to login page
      this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }
    // If the user is logged in, add the phone to the wishlist
    this.restApiService.post('http://localhost:3000/api/profile/wishlist', { phoneId: phone.id })
      .then(() => {
        this.data.success('Item added to wishlist');
      })
      .catch((error) => {
        this.data.error(error.message || 'Failed to add item to wishlist');
      });
  }

  // Determine if the current user is the seller of this phone
  get isSeller(): boolean {
    return !!this.phone && this.phone.sellerId === this.currentUserId;
  }

  // Determine if the current user has already submitted a review for this phone
  get hasReviewed(): boolean {
    return !!this.currentUserId && this.reviews.some(review => review.reviewerId === this.currentUserId);
  }

  // Quantity selection modal handlers
  increaseQuantity(): void {
    if (this.selectedQuantity < this.maxQuantity) {
      this.selectedQuantity++;
    }
  }

  decreaseQuantity(): void {
    if (this.selectedQuantity > 0) {
      this.selectedQuantity--;
    }
  }

  confirmAddToCart(): void {
    if (!this.phone) return;
    
    if (this.selectedQuantity === 0) {
      // If quantity is 0, remove the item from cart
      this.cartService.removeItem(this.phone.id)
        .then(response => {
          if (response) {
            this.data.success('Item removed from cart');
            this.cartQuantity = 0;
          } else {
            this.data.error('Failed to remove item from cart');
          }
        })
        .catch(() => {
          this.data.error('Failed to remove item from cart');
        })
        .finally(() => {
          this.showQuantityModal = false;
        });
    } else {
      // If quantity > 0, update or add item
      this.cartService.addOrUpdateItem(this.phone.id, this.selectedQuantity)
        .then(response => {
          if (response) {
            this.data.success(`Updated cart quantity to ${this.selectedQuantity}`);
            this.cartQuantity = this.selectedQuantity;
          } else {
            this.data.error('Failed to update cart');
          }
        })
        .catch(() => {
          this.data.error('Failed to update cart');
        })
        .finally(() => {
          this.showQuantityModal = false;
        });
    }
  }

  cancelAddToCart(): void {
    this.showQuantityModal = false;
  }

  // Fetch the current quantity of this phone in the cart
  fetchCartQuantity(phoneId: string): void {
    this.cartService.getCart()
      .then(cart => {
        if (cart && cart.items) {
          const cartItem = cart.items.find((item: any) => item.phoneId === phoneId);
          this.cartQuantity = cartItem ? cartItem.quantity : 0;
        } else {
          this.cartQuantity = 0;
        }
      })
      .catch(() => {
        this.cartQuantity = 0;
      });
  }
} 