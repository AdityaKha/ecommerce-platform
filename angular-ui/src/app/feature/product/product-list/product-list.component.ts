import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { Product } from '../../../core/models/product.models';
import { ProductService } from '../../../core/services/product.service';
import { CartService } from '../../../core/services/cart.service';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule],
  template: `
    <div class="product-list-container">
      <div class="product-list-header">
        <h2>Products</h2>
        <a routerLink="/cart">Cart ({{ cartService.itemCount() }})</a>
      </div>

      @if (loading()) {
        <p>Loading products...</p>
      } @else if (errorMessage()) {
        <p class="error-message">{{ errorMessage() }}</p>
      } @else if (products().length === 0) {
        <p>No products found.</p>
      } @else {
        <table class="product-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>SKU</th>
              <th>Category</th>
              <th>Price</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (product of products(); track product.id) {
              <tr>
                <td>{{ product.name }}</td>
                <td>{{ product.sku }}</td>
                <td>{{ product.category }}</td>
                <td>{{ product.price | currency }}</td>
                <td>
                  <button mat-stroked-button color="primary" (click)="cartService.add(product)">
                    Add to Cart
                  </button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      }
    </div>
  `,
  styles: [`
    .product-list-container {
      max-width: 720px;
      margin: 32px auto;
      padding: 0 16px;
    }
    .product-list-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .product-table {
      width: 100%;
      border-collapse: collapse;
    }
    .product-table th, .product-table td {
      text-align: left;
      padding: 8px;
      border-bottom: 1px solid #ddd;
    }
    .error-message {
      color: #f44336;
    }
  `],
})
export class ProductListComponent implements OnInit {
  products = signal<Product[]>([]);
  loading = signal(false);
  errorMessage = signal('');

  constructor(
    private productService: ProductService,
    protected cartService: CartService,
  ) {}

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
}
