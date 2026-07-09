import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../core/services/auth.service';
import { CartService } from '../../core/services/cart.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, MatIconModule, RouterLink],
  template: `
    <div class="page-container">
      <section class="hero">
        <h1>Welcome back, {{ username }} 👋</h1>
        <p>Discover our latest gear — audio, electronics, home and fitness essentials.</p>
        <a mat-flat-button routerLink="/products">
          <mat-icon>storefront</mat-icon>
          Start Shopping
        </a>
      </section>

      <section class="quick-links">
        <mat-card class="quick-card" routerLink="/products">
          <mat-icon class="quick-icon">category</mat-icon>
          <h3>Browse Products</h3>
          <p>Explore the full catalog and add items to your cart.</p>
        </mat-card>

        <mat-card class="quick-card" routerLink="/cart">
          <mat-icon class="quick-icon">shopping_cart</mat-icon>
          <h3>Your Cart</h3>
          <p>
            @if (cartService.itemCount() > 0) {
              {{ cartService.itemCount() }} item(s) waiting for checkout.
            } @else {
              Your cart is empty — go grab something nice.
            }
          </p>
        </mat-card>

        <mat-card class="quick-card" routerLink="/orders">
          <mat-icon class="quick-icon">receipt_long</mat-icon>
          <h3>Order History</h3>
          <p>Track the status of your past orders.</p>
        </mat-card>
      </section>
    </div>
  `,
  styles: [`
    .hero {
      background: linear-gradient(120deg, var(--mat-sys-primary), #3f6bd8 60%, #7b5cd6);
      color: #fff;
      border-radius: 16px;
      padding: 48px 32px;
      margin-bottom: 24px;
    }
    .hero h1 {
      margin: 0 0 8px;
      font-size: 32px;
    }
    .hero p {
      margin: 0 0 24px;
      font-size: 16px;
      opacity: 0.9;
      max-width: 480px;
    }
    .hero a {
      background: #fff;
      color: #1f2430;
    }
    .quick-links {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      gap: 16px;
    }
    .quick-card {
      padding: 24px;
      cursor: pointer;
      transition: transform 0.15s ease, box-shadow 0.15s ease;
    }
    .quick-card:hover {
      transform: translateY(-3px);
      box-shadow: 0 8px 20px rgba(31, 36, 48, 0.12);
    }
    .quick-icon {
      font-size: 32px;
      width: 32px;
      height: 32px;
      color: var(--mat-sys-primary);
    }
    .quick-card h3 {
      margin: 12px 0 4px;
    }
    .quick-card p {
      margin: 0;
      color: #5a6472;
      font-size: 14px;
    }
  `],
})
export class HomeComponent {
  private authService = inject(AuthService);
  protected cartService = inject(CartService);

  username = this.authService.getUsername() ?? 'User';
}
