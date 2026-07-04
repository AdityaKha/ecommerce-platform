import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'home',
    canActivate: [authGuard],
    loadComponent: () => import('./feature/home/home.component').then(m => m.HomeComponent),
  },
  {
    path: 'products',
    canActivate: [authGuard],
    loadComponent: () => import('./feature/product/product-list/product-list.component').then(m => m.ProductListComponent),
  },
  {
    path: 'cart',
    canActivate: [authGuard],
    loadComponent: () => import('./feature/cart/cart.component').then(m => m.CartComponent),
  },
  {
    path: 'checkout',
    canActivate: [authGuard],
    loadComponent: () => import('./feature/checkout/checkout.component').then(m => m.CheckoutComponent),
  },
  {
    path: 'orders',
    canActivate: [authGuard],
    loadComponent: () => import('./feature/order/order-history.component').then(m => m.OrderHistoryComponent),
  },
  {
    path: 'auth',
    loadChildren: () => import('./feature/auth/auth.routes').then(m => m.AUTH_ROUTES),
  },
  { path: '', redirectTo: 'home', pathMatch: 'full' },
];
