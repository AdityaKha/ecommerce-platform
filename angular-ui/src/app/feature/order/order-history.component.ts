import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { OrderResponse } from '../../core/models/order.models';
import { OrderService } from '../../core/services/order.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-order-history',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <div class="page-container order-history-container">
      <h2 class="page-title">Your Orders</h2>

      @if (loading()) {
        <div class="loading-state">
          <mat-spinner diameter="40" />
          <p>Loading orders...</p>
        </div>
      } @else if (errorMessage()) {
        <div class="empty-state">
          <mat-icon class="empty-state-icon">cloud_off</mat-icon>
          <p class="error-message">{{ errorMessage() }}</p>
        </div>
      } @else if (orders().length === 0) {
        <div class="empty-state">
          <mat-icon class="empty-state-icon">receipt_long</mat-icon>
          <p>You haven't placed any orders yet.</p>
          <a mat-flat-button routerLink="/products">Browse products</a>
        </div>
      } @else {
        @for (order of orders(); track order.id) {
          <mat-card class="order-card">
            <div class="order-header">
              <span class="order-id">Order #{{ order.id }}</span>
              <span class="order-status" [class]="'status-' + order.status.toLowerCase()">
                {{ order.status }}
              </span>
            </div>
            <div class="order-meta">Placed {{ order.createdAt | date: 'medium' }}</div>
            <table class="order-items">
              <tbody>
                @for (item of order.items; track item.productId) {
                  <tr>
                    <td>Product #{{ item.productId }}</td>
                    <td class="item-amount">{{ item.quantity }} &times; {{ item.unitPrice | currency }}</td>
                  </tr>
                }
              </tbody>
            </table>
            <div class="order-total">Total: {{ order.totalAmount | currency }}</div>
          </mat-card>
        }
      }
    </div>
  `,
  styles: [`
    .order-history-container {
      max-width: 760px;
    }
    .loading-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
      padding: 64px 0;
      color: #5a6472;
    }
    .order-card {
      padding: 20px;
      margin-bottom: 16px;
    }
    .order-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .order-id {
      font-weight: 600;
      font-size: 16px;
    }
    .order-status {
      font-size: 12px;
      font-weight: 600;
      letter-spacing: 0.5px;
      border-radius: 999px;
      padding: 3px 12px;
      background: #e8edf6;
      color: #3f6bd8;
    }
    .status-created {
      background: #e3ecfb;
      color: #2857b8;
    }
    .status-confirmed, .status-completed {
      background: #e0f2e6;
      color: #23794a;
    }
    .status-cancelled, .status-failed {
      background: #fbe4e4;
      color: #b03030;
    }
    .order-meta {
      color: #8a94a3;
      font-size: 13px;
      margin: 4px 0 12px;
    }
    .order-items {
      width: 100%;
      border-collapse: collapse;
    }
    .order-items td {
      padding: 6px 0;
      border-bottom: 1px solid #eef1f6;
    }
    .order-items tr:last-child td {
      border-bottom: none;
    }
    .item-amount {
      text-align: right;
      color: #5a6472;
    }
    .order-total {
      font-weight: 600;
      text-align: right;
      margin-top: 12px;
    }
    .error-message {
      color: #ba1a1a;
    }
  `],
})
export class OrderHistoryComponent implements OnInit {
  private orderService = inject(OrderService);
  private authService = inject(AuthService);

  orders = signal<OrderResponse[]>([]);
  loading = signal(false);
  errorMessage = signal('');

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
