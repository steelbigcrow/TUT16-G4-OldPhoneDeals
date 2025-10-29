import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Location } from '@angular/common';
import { RestApiService } from '../services/rest-api.service';
import { DataService } from '../services/data.service';
import { CartService } from '../services/cart.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CartItem } from '../models/cart.model';
import { Address } from '../models/user.model';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './checkout.component.html',
  styleUrls: ['./checkout.component.scss']
})
export class CheckoutComponent implements OnInit {
  // cart items
  cartItems: CartItem[] = [];
  // loading state
  isLoading = false;
  // shipping address
  address: Address = { street: '', city: '', state: '', zip: '', country: '' };
  // server base URL
  serverBaseUrl: string = 'http://localhost:3000';

  constructor(
    private location: Location,
    private router: Router,
    private rest: RestApiService,
    private data: DataService,
    private cartService: CartService,
  ) {}

  /* Load cart items when initialized */
  ngOnInit() {
    this.loadCartItems();
  }

  /* Load cart items from backend */
  async loadCartItems() {
    this.isLoading = true;
    try {
      const cart = await this.cartService.getCart();
      if (cart && cart.items) {
        this.cartItems = cart.items;
      } else {
        this.cartItems = [];
      }
    } catch (error) {
      this.data.error('Failed to load cart items');
    } finally {
      this.isLoading = false;
    }
  }

  /* Calculate total price */
  get totalPrice(): number {
    return this.cartItems.reduce(
      (sum, item) => sum + (item.price * item.quantity), 0
    );
  }

  /* Update quantity for a specific item in backend cart */
  updateQuantity(index: number, newQuantity: number) {
    newQuantity = Math.max(0, newQuantity);
    const item = this.cartItems[index];
    // Remove item if zero
    if (newQuantity === 0) {
      this.rest.delete(`http://localhost:3000/api/cart/items/${item.phoneId}`)
        .then(() => {
          this.data.message = '';
          this.loadCartItems();
        })
        .catch(err => {
          const msg = err.error?.message || 'Failed to remove item';
          this.data.error(msg);
          this.loadCartItems();
        });
    } else {
      // Update quantity
      this.rest.patch(
        `http://localhost:3000/api/cart/items/${item.phoneId}`,
        { quantity: newQuantity }
      )
        .then((res: any) => {
          // If backend returned a message without items, show it as error
          if (res.message && !res.items) {
            this.data.error(res.message);
            this.loadCartItems();
          } else {
            // Success: clear any existing message and reload cart
            this.data.message = '';
            this.loadCartItems();
          }
        })
        .catch(err => {
          const msg = err.error?.message || 'Cannot exceed available stock';
          this.data.error(msg);
          this.loadCartItems();
        });
    }
  }

  /* Remove item from backend cart */
  removeItem(index: number) {
    const item = this.cartItems[index];
    this.rest.delete(`http://localhost:3000/api/cart/items/${item.phoneId}`)
      .then(() => {
        this.data.message = '';
        this.loadCartItems();
      })
      .catch(err => {
        const msg = err.error?.message || 'Failed to remove item';
        this.data.error(msg);
        this.loadCartItems();
      });
  }

  /* Generate boolean array for star icons based on average rating */
  getStars(rating: number | undefined): boolean[] {
    const fullStars = rating ? Math.round(rating) : 0;
    return Array(5).fill(false).map((_, i) => i < fullStars);
  }

  /* Confirm transaction */
  async confirmTransaction() {
    if (!confirm('Are you sure you want to place the order?')) {
      return;
    }
    try {
      if (this.cartItems.length === 0) {
        this.data.error('Cart is empty');
        return;
      }
      /** Validate address */
      if (!this.address.street || !this.address.city || !this.address.state || !this.address.zip || !this.address.country) {
        this.data.error('Please provide complete shipping address');
        return;
      }

      const orderPayload = { address: this.address };

      // submit order to backend
      const createdOrder = await this.rest.post(
        'http://localhost:3000/api/orders',
        orderPayload
      );
      // clear the cart and redirect to user home page
      await this.cartService.clearCart();
      await this.router.navigate(['/user-home']);
      this.data.success('Your order has been successfully delivered！');
    } catch (error: any) {
      console.error('Checkout error:', error);
      const errMsg = error.error?.message || error.message || 'Error processing order';
      this.data.error(errMsg);
    }
  }

  /* Go back to previous page */
  goBack(): void {
    this.location.back();
  }
}
