import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'home',
    loadComponent: () => import('./feature/home/home.component').then(m => m.HomeComponent),
  },
  {
    path: 'products',
    loadComponent: () => import('./feature/product/product-list/product-list.component').then(m => m.ProductListComponent),
  },
  {
    path: 'auth',
    loadChildren: () => import('./feature/auth/auth.routes').then(m => m.AUTH_ROUTES),
  },
  { path: '', redirectTo: 'home', pathMatch: 'full' },
];
