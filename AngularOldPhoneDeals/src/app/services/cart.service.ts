import { Injectable } from '@angular/core';
import { RestApiService } from './rest-api.service';
import { DataService } from './data.service';

@Injectable({ providedIn: 'root' })
export class CartService {
  constructor(
    private rest: RestApiService,
    private data: DataService
  ) {}

  /** Get the full cart from server */
  async getCart() {
    try {
      return await this.rest.get('http://localhost:3000/api/cart');
    } catch (error) {
      this.data.error('Failed to load cart');
      return null;
    }
  }

  /** Add or update an item in the cart */
  async addOrUpdateItem(phoneId: string, quantity: number) {
    try {
      return await this.rest.post(
        'http://localhost:3000/api/cart/items',
        { phoneId, quantity }
      );
    } catch (error) {
      this.data.error('Failed to add/update cart item');
      return null;
    }
  }

  /** Update quantity of a cart item */
  async updateItemQuantity(phoneId: string, quantity: number) {
    try {
      return await this.rest.patch(
        `http://localhost:3000/api/cart/items/${phoneId}`,
        { quantity }
      );
    } catch (error) {
      this.data.error('Failed to update cart item quantity');
      return null;
    }
  }

  /** Remove an item from the cart */
  async removeItem(phoneId: string) {
    try {
      return await this.rest.delete(
        `http://localhost:3000/api/cart/items/${phoneId}`
      );
    } catch (error) {
      this.data.error('Failed to remove cart item');
      return null;
    }
  }

  /** Clear the entire cart by removing each item */
  async clearCart() {
    try {
      const cart = await this.getCart();
      if (!cart) return null;
      const removals = cart.items.map((i: any) => this.removeItem(i.phoneId));
      await Promise.all(removals);
      return true;
    } catch (error) {
      this.data.error('Failed to clear cart');
      return null;
    }
  }
}