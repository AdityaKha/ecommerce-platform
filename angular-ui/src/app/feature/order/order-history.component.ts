import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { OrderResponse } from '../../core/models/order.models';
import { OrderService } from '../../core/services/order.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-order-history',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="order-history-container">
      <h2>Your Orders</h2>

      @if (loading()) {
        <p>Loading orders...</p>
      } @else if (errorMessage()) {
        <p class="error-message">{{ errorMessage() }}</p>
      } @else if (orders().length === 0) {
        <p>You haven't placed any orders yet.</p>
        <a routerLink="/products">Browse products</a>
      } @else {
        @for (order of orders(); track order.id) {
          <div class="order-card">
            <div class="order-header">
              <span>Order #{{ order.id }}</span>
              <span class="order-status">{{ order.status }}</span>
            </div>
            <div class="order-meta">{{ order.createdAt | date: 'medium' }}</div>
            <table class="order-items">
              <tbody>
                @for (item of order.items; track item.productId) {
                  <tr>
                    <td>Product #{{ item.productId }}</td>
                    <td>{{ item.quantity }} &times; {{ item.unitPrice | currency }}</td>
                  </tr>
                }
              </tbody>
            </table>
            <div class="order-total">Total: {{ order.totalAmount | currency }}</div>
          </div>
        }
      }
    </div>
  `,
  styles: [`
    .order-history-container {
      max-width: 720px;
      margin: 32px auto;
      padding: 0 16px;
    }
    .order-card {
      border: 1px solid #ddd;
      border-radius: 4px;
      padding: 16px;
      margin-bottom: 16px;
    }
    .order-header {
      display: flex;
      justify-content: space-between;
      font-weight: 600;
    }
    .order-status {
      color: #1976d2;
    }
    .order-meta {
      color: #666;
      font-size: 13px;
      margin-bottom: 8px;
    }
    .order-items {
      width: 100%;
      border-collapse: collapse;
    }
    .order-items td {
      padding: 4px 0;
    }
    .order-total {
      font-weight: 600;
      text-align: right;
      margin-top: 8px;
    }
    .error-message {
      color: #f44336;
    }
  `],
})
export class OrderHistoryComponent implements OnInit {
  orders = signal<OrderResponse[]>([]);
  loading = signal(false);
  errorMessage = signal('');

  constructor(
    private orderService: OrderService,
    private authService: AuthService,
  ) {}

  ngOnInit(): void {
    this.loading.set(true);
    const username = this.authService.getUsername();
    this.orderService.findAll().subscribe({
      next: (orders) => {
        this.orders.set(orders.filter((order) => order.customerUsername === username));
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Failed to load orders.');
        this.loading.set(false);
      },
    });
  }
}
