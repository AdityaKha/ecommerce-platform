import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CartService } from '../../core/services/cart.service';
import { OrderService } from '../../core/services/order.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <div class="checkout-container">
      <h2>Checkout</h2>

      @if (cartService.items().length === 0) {
        <p>Your cart is empty.</p>
        <a routerLink="/products">Browse products</a>
      } @else {
        <mat-card class="summary-card">
          <mat-card-content>
            @for (item of cartService.items(); track item.product.id) {
              <div class="summary-row">
                <span>{{ item.product.name }} &times; {{ item.quantity }}</span>
                <span>{{ item.product.price * item.quantity | currency }}</span>
              </div>
            }
            <div class="summary-row summary-total">
              <span>Total</span>
              <span>{{ cartService.total() | currency }}</span>
            </div>
          </mat-card-content>
        </mat-card>

        <form [formGroup]="form" (ngSubmit)="onSubmit()">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Contact Email</mat-label>
            <input matInput type="email" formControlName="customerEmail" autocomplete="email" />
            @if (form.get('customerEmail')?.hasError('required') && form.get('customerEmail')?.touched) {
              <mat-error>Email is required</mat-error>
            }
            @if (form.get('customerEmail')?.hasError('email') && form.get('customerEmail')?.touched) {
              <mat-error>Enter a valid email</mat-error>
            }
          </mat-form-field>

          @if (errorMessage) {
            <p class="error-message">{{ errorMessage }}</p>
          }

          <button
            mat-flat-button
            color="primary"
            type="submit"
            class="full-width submit-btn"
            [disabled]="loading"
          >
            @if (loading) {
              <mat-spinner diameter="20" />
            } @else {
              Place Order
            }
          </button>
        </form>
      }
    </div>
  `,
  styles: [`
    .checkout-container {
      max-width: 480px;
      margin: 32px auto;
      padding: 0 16px;
    }
    .summary-card {
      margin-bottom: 16px;
    }
    .summary-row {
      display: flex;
      justify-content: space-between;
      padding: 4px 0;
    }
    .summary-total {
      font-weight: 600;
      border-top: 1px solid #ddd;
      margin-top: 8px;
      padding-top: 8px;
    }
    .full-width {
      width: 100%;
    }
    .submit-btn {
      margin-top: 8px;
    }
    .error-message {
      color: #f44336;
      font-size: 14px;
      margin: 4px 0 8px;
    }
    form {
      display: flex;
      flex-direction: column;
    }
  `],
})
export class CheckoutComponent {
  private fb = inject(FormBuilder);
  protected cartService = inject(CartService);
  private orderService = inject(OrderService);
  private authService = inject(AuthService);
  private router = inject(Router);

  form = this.fb.nonNullable.group({
    customerEmail: ['', [Validators.required, Validators.email]],
  });

  loading = false;
  errorMessage = '';

  onSubmit(): void {
    if (this.form.invalid || this.cartService.items().length === 0) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    const { customerEmail } = this.form.getRawValue();
    this.orderService
      .create({
        customerUsername: this.authService.getUsername() ?? '',
        customerEmail,
        items: this.cartService.items().map((item) => ({
          productId: item.product.id,
          quantity: item.quantity,
          unitPrice: item.product.price,
        })),
      })
      .subscribe({
        next: () => {
          this.cartService.clear();
          this.router.navigate(['/orders']);
        },
        error: (err) => {
          this.loading = false;
          this.errorMessage = err.error?.message ?? 'Failed to place order. Please try again.';
        },
      });
  }
}
