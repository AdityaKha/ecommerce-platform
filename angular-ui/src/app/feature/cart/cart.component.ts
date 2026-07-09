import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { CartService } from '../../core/services/cart.service';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule, MatCardModule, MatIconModule],
  template: `
    <div class="page-container cart-container">
      <h2 class="page-title">Your Cart</h2>

      @if (cartService.items().length === 0) {
        <div class="empty-state">
          <mat-icon class="empty-state-icon">remove_shopping_cart</mat-icon>
          <p>Your cart is empty.</p>
          <a mat-flat-button routerLink="/products">Browse products</a>
        </div>
      } @else {
        <mat-card class="cart-card">
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
                  <td>
                    <div class="product-cell">
                      <span class="product-name">{{ item.product.name }}</span>
                      <span class="product-sku">{{ item.product.sku }}</span>
                    </div>
                  </td>
                  <td>{{ item.product.price | currency }}</td>
                  <td>
                    <span class="quantity-stepper">
                      <button mat-icon-button (click)="decrement(item.product.id, item.quantity)">
                        <mat-icon>remove</mat-icon>
                      </button>
                      <span class="quantity-value">{{ item.quantity }}</span>
                      <button mat-icon-button (click)="increment(item.product.id, item.quantity)">
                        <mat-icon>add</mat-icon>
                      </button>
                    </span>
                  </td>
                  <td class="subtotal">{{ item.product.price * item.quantity | currency }}</td>
                  <td>
                    <button mat-button color="warn" (click)="cartService.remove(item.product.id)">
                      Remove
                    </button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </mat-card>

        <div class="cart-footer">
          <div class="cart-total">Total: {{ cartService.total() | currency }}</div>
          <button mat-flat-button routerLink="/checkout">
            <mat-icon>shopping_cart_checkout</mat-icon>
            Proceed to Checkout
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    .cart-container {
      max-width: 860px;
    }
    .cart-card {
      padding: 8px 16px;
      margin-bottom: 16px;
      overflow-x: auto;
    }
    .cart-table {
      width: 100%;
      border-collapse: collapse;
    }
    .cart-table th, .cart-table td {
      text-align: left;
      padding: 12px 8px;
      border-bottom: 1px solid #e6eaf1;
    }
    .cart-table th {
      color: #5a6472;
      font-size: 13px;
      font-weight: 500;
    }
    .cart-table tbody tr:last-child td {
      border-bottom: none;
    }
    .product-cell {
      display: flex;
      flex-direction: column;
    }
    .product-name {
      font-weight: 500;
    }
    .product-sku {
      font-size: 12px;
      color: #8a94a3;
    }
    .quantity-stepper {
      display: inline-flex;
      align-items: center;
      gap: 4px;
    }
    .quantity-value {
      min-width: 24px;
      text-align: center;
      font-weight: 500;
    }
    .subtotal {
      font-weight: 500;
    }
    .cart-footer {
      display: flex;
      justify-content: flex-end;
      align-items: center;
      gap: 24px;
    }
    .cart-total {
      font-size: 20px;
      font-weight: 600;
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
