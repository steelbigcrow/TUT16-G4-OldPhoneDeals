import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RestApiService } from '../services/rest-api.service';
import { DataService } from '../services/data.service';
import { Location } from '@angular/common';

@Component({
  selector: 'app-wishlist',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './wishlist.component.html'
})
export class WishlistComponent implements OnInit {
  wishlist: any[] = [];
  isLoading = false;

  // Base URL for static assets
  serverBaseUrl: string = 'http://localhost:3000';

  constructor(
    private rest: RestApiService, 
    private data: DataService,
    private location: Location
  ) {}

  async ngOnInit() {
    this.isLoading = true;
    try {
      // Fetch the wishlist items for the authenticated user
      this.wishlist = await this.rest.get('http://localhost:3000/api/profile/wishlist');
    } catch (err) {
      this.data.error('Error loading wishlist');
    } finally {
      this.isLoading = false;
    }
  }

  /**
   * Remove an item from the wishlist
   */
  async removeItem(phoneId: string) {
    this.isLoading = true;
    try {
      // Call delete on the wishlist endpoint
      await this.rest.delete(
        `http://localhost:3000/api/profile/wishlist/${phoneId}`
      );
      
      // manually remove the deleted item from the local array
      this.wishlist = this.wishlist.filter(item => item._id !== phoneId);
      
      this.data.success('Item removed from wishlist');
    } catch (err) {
      this.data.error('Error removing item from wishlist');
    } finally {
      this.isLoading = false;
    }
  }

  /**
   * Build full image URL for display
   */
  getImageUrl(imagePath: string | undefined): string {
    if (!imagePath) {
      return '';
    }
    const path = imagePath.startsWith('/') ? imagePath : '/' + imagePath;
    return `${this.serverBaseUrl}${path}`;
  }

  /**
   * Generate boolean array for star icons based on average rating
   */
  getStars(rating: number | undefined): boolean[] {
    const fullStars = rating ? Math.round(rating) : 0;
    return Array(5).fill(false).map((_, i) => i < fullStars);
  }

  /**
   * Navigate back to the previous page
   */
  goBack(): void {
    this.location.back();
  }
} 