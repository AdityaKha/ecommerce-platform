import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatBadgeModule } from '@angular/material/badge';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from './core/services/auth.service';
import { CartService } from './core/services/cart.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatBadgeModule,
    MatTooltipModule,
  ],
  template: `
    @if (authService.isAuthenticated()) {
      <mat-toolbar class="app-toolbar">
        <a routerLink="/home" class="brand">
          <mat-icon>storefront</mat-icon>
          <span>Emporia</span>
        </a>

        <nav class="nav-links">
          <a mat-button routerLink="/products" routerLinkActive="active-link">Products</a>
          <a mat-button routerLink="/orders" routerLinkActive="active-link">Orders</a>
        </nav>

        <span class="spacer"></span>

        <a
          mat-icon-button
          routerLink="/cart"
          matTooltip="Cart"
          aria-label="Cart"
          [matBadge]="cartService.itemCount() || null"
          matBadgeColor="warn"
        >
          <mat-icon>shopping_cart</mat-icon>
        </a>

        <span class="username">
          <mat-icon>account_circle</mat-icon>
          {{ authService.getUsername() }}
        </span>

        <button mat-icon-button matTooltip="Sign out" aria-label="Sign out" (click)="logout()">
          <mat-icon>logout</mat-icon>
        </button>
      </mat-toolbar>
    }

    <main class="app-content">
      <router-outlet />
    </main>
  `,
  styles: [`
    :host {
      display: flex;
      flex-direction: column;
      min-height: 100vh;
    }
    .app-toolbar {
      position: sticky;
      top: 0;
      z-index: 10;
      gap: 8px;
      background: var(--mat-sys-primary);
      color: var(--mat-sys-on-primary);
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.18);
    }
    .app-toolbar a,
    .app-toolbar button {
      color: inherit;
    }
    .brand {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      font-size: 20px;
      font-weight: 600;
      letter-spacing: 0.4px;
      text-decoration: none;
      margin-right: 16px;
    }
    .nav-links {
      display: flex;
      gap: 4px;
    }
    .active-link {
      background: rgba(255, 255, 255, 0.16);
      border-radius: 8px;
    }
    .username {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      margin: 0 8px;
      font-size: 14px;
      opacity: 0.9;
    }
    .app-content {
      flex: 1;
      display: flex;
      flex-direction: column;
    }
    @media (max-width: 600px) {
      .username span,
      .username {
        display: none;
      }
    }
  `],
})
export class AppComponent {
  protected authService = inject(AuthService);
  protected cartService = inject(CartService);
  private router = inject(Router);

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}
