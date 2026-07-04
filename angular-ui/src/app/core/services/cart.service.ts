import { Injectable, computed, signal } from '@angular/core';
import { Product } from '../models/product.models';
import { CartItem } from '../models/cart.models';

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly itemsSignal = signal<CartItem[]>([]);

  readonly items = this.itemsSignal.asReadonly();
  readonly itemCount = computed(() =>
    this.itemsSignal().reduce((sum, item) => sum + item.quantity, 0),
  );
  readonly total = computed(() =>
    this.itemsSignal().reduce((sum, item) => sum + item.product.price * item.quantity, 0),
  );

  add(product: Product, quantity = 1): void {
    this.itemsSignal.update((items) => {
      const existing = items.find((item) => item.product.id === product.id);
      if (existing) {
        return items.map((item) =>
          item.product.id === product.id
            ? { ...item, quantity: item.quantity + quantity }
            : item,
        );
      }
      return [...items, { product, quantity }];
    });
  }

  updateQuantity(productId: number, quantity: number): void {
    if (quantity <= 0) {
      this.remove(productId);
      return;
    }
    this.itemsSignal.update((items) =>
      items.map((item) => (item.product.id === productId ? { ...item, quantity } : item)),
    );
  }

  remove(productId: number): void {
    this.itemsSignal.update((items) => items.filter((item) => item.product.id !== productId));
  }

  clear(): void {
    this.itemsSignal.set([]);
  }
}
