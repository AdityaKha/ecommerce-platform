import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CartService } from '../../core/services/cart.service';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule, MatIconModule],
  template: `
    <div class="cart-container">
      <h2>Your Cart</h2>

      @if (cartService.items().length === 0) {
        <p>Your cart is empty.</p>
        <a routerLink="/products">Browse products</a>
      } @else {
        <table class="cart-table">
          <thead>
            <tr>
              <th>Product</th>
              <th>Price</th>
              <th>Quantity</th>
              <th>Subtotal</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (item of cartService.items(); track item.product.id) {
              <tr>
                <td>{{ item.product.name }}</td>
                <td>{{ item.product.price | currency }}</td>
                <td>
                  <button mat-icon-button (click)="decrement(item.product.id, item.quantity)">
                    <mat-icon>remove</mat-icon>
                  </button>
                  {{ item.quantity }}
                  <button mat-icon-button (click)="increment(item.product.id, item.quantity)">
                    <mat-icon>add</mat-icon>
                  </button>
                </td>
                <td>{{ item.product.price * item.quantity | currency }}</td>
                <td>
                  <button mat-button color="warn" (click)="cartService.remove(item.product.id)">
                    Remove
                  </button>
                </td>
              </tr>
            }
          </tbody>
        </table>

        <div class="cart-total">Total: {{ cartService.total() | currency }}</div>

        <button mat-flat-button color="primary" routerLink="/checkout">Proceed to Checkout</button>
      }
    </div>
  `,
  styles: [`
    .cart-container {
      max-width: 720px;
      margin: 32px auto;
      padding: 0 16px;
    }
    .cart-table {
      width: 100%;
      border-collapse: collapse;
      margin-bottom: 16px;
    }
    .cart-table th, .cart-table td {
      text-align: left;
      padding: 8px;
      border-bottom: 1px solid #ddd;
    }
    .cart-total {
      font-size: 18px;
      font-weight: 600;
      margin-bottom: 16px;
      text-align: right;
    }
  `],
})
export class CartComponent {
  protected cartService = inject(CartService);


  increment(productId: number, quantity: number): void {
    this.cartService.updateQuantity(productId, quantity + 1);
  }

  decrement(productId: number, quantity: number): void {
    this.cartService.updateQuantity(productId, quantity - 1);
  }
}
