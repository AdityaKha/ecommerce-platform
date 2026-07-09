import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Product } from '../../../core/models/product.models';
import { ProductService } from '../../../core/services/product.service';
import { CartService } from '../../../core/services/cart.service';

const CATEGORY_STYLES: Record<string, { icon: string; gradient: string }> = {
  'Audio': { icon: 'headphones', gradient: 'linear-gradient(135deg, #5b7fd9, #7b5cd6)' },
  'Electronics': { icon: 'devices', gradient: 'linear-gradient(135deg, #3f6bd8, #38a1c4)' },
  'Accessories': { icon: 'watch', gradient: 'linear-gradient(135deg, #c66a9a, #8a5cd6)' },
  'Home & Kitchen': { icon: 'home', gradient: 'linear-gradient(135deg, #3aa17e, #6ac46a)' },
  'Fitness': { icon: 'fitness_center', gradient: 'linear-gradient(135deg, #d98a3f, #d95c5c)' },
};

const DEFAULT_STYLE = { icon: 'sell', gradient: 'linear-gradient(135deg, #6b7a90, #9aa7b8)' };

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  template: `
    <div class="page-container">
      <h2 class="page-title">Products</h2>

      @if (loading()) {
        <div class="loading-state">
          <mat-spinner diameter="40" />
          <p>Loading products...</p>
        </div>
      } @else if (errorMessage()) {
        <div class="empty-state">
          <mat-icon class="empty-state-icon">cloud_off</mat-icon>
          <p class="error-message">{{ errorMessage() }}</p>
        </div>
      } @else if (products().length === 0) {
        <div class="empty-state">
          <mat-icon class="empty-state-icon">inventory_2</mat-icon>
          <p>No products found.</p>
        </div>
      } @else {
        <div class="product-grid">
          @for (product of products(); track product.id) {
            <mat-card class="product-card">
              <div class="product-banner" [style.background]="styleFor(product.category).gradient">
                <mat-icon>{{ styleFor(product.category).icon }}</mat-icon>
              </div>
              <mat-card-content>
                <span class="category-chip">{{ product.category }}</span>
                <h3 class="product-name">{{ product.name }}</h3>
                <p class="product-description">{{ product.description }}</p>
              </mat-card-content>
              <mat-card-actions class="product-actions">
                <span class="product-price">{{ product.price | currency }}</span>
                <button mat-flat-button (click)="addToCart(product)">
                  <mat-icon>add_shopping_cart</mat-icon>
                  Add
                </button>
              </mat-card-actions>
            </mat-card>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .loading-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
      padding: 64px 0;
      color: #5a6472;
    }
    .product-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
      gap: 16px;
    }
    .product-card {
      display: flex;
      flex-direction: column;
      overflow: hidden;
      transition: transform 0.15s ease, box-shadow 0.15s ease;
    }
    .product-card:hover {
      transform: translateY(-3px);
      box-shadow: 0 8px 20px rgba(31, 36, 48, 0.14);
    }
    .product-banner {
      height: 110px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .product-banner mat-icon {
      font-size: 44px;
      width: 44px;
      height: 44px;
      color: rgba(255, 255, 255, 0.92);
    }
    .category-chip {
      display: inline-block;
      font-size: 12px;
      font-weight: 500;
      color: var(--mat-sys-primary);
      background: color-mix(in srgb, var(--mat-sys-primary) 10%, transparent);
      border-radius: 999px;
      padding: 2px 10px;
      margin-top: 12px;
    }
    .product-name {
      margin: 8px 0 4px;
      font-size: 16px;
    }
    .product-description {
      margin: 0;
      color: #5a6472;
      font-size: 13px;
      line-height: 1.45;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
    .product-actions {
      margin-top: auto;
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px 16px 16px;
    }
    .product-price {
      font-size: 18px;
      font-weight: 600;
    }
    .error-message {
      color: #ba1a1a;
    }
  `],
})
export class ProductListComponent implements OnInit {
  private productService = inject(ProductService);
  private snackBar = inject(MatSnackBar);
  private router = inject(Router);
  protected cartService = inject(CartService);

  products = signal<Product[]>([]);
  loading = signal(false);
  errorMessage = signal('');

  ngOnInit(): void {
    this.loading.set(true);
    this.productService.findAll().subscribe({
      next: (products) => {
        this.products.set(products);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Failed to load products.');
        this.loading.set(false);
      },
    });
  }

  styleFor(category: string): { icon: string; gradient: string } {
    return CATEGORY_STYLES[category] ?? DEFAULT_STYLE;
  }

  addToCart(product: Product): void {
    this.cartService.add(product);
    this.snackBar
      .open(`${product.name} added to cart`, 'View Cart', { duration: 2500 })
      .onAction()
      .subscribe(() => this.router.navigate(['/cart']));
  }
}
