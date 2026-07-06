import { TestBed } from '@angular/core/testing';
import { CartService } from './cart.service';
import { Product } from '../models/product.models';

function makeProduct(overrides: Partial<Product> = {}): Product {
  return {
    id: 1,
    name: 'Widget',
    description: 'A widget',
    sku: 'W-1',
    price: 10,
    category: 'Tools',
    active: true,
    ...overrides,
  };
}

describe('CartService', () => {
  let service: CartService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CartService);
  });

  it('should be created with an empty cart', () => {
    expect(service).toBeTruthy();
    expect(service.items()).toEqual([]);
    expect(service.itemCount()).toBe(0);
    expect(service.total()).toBe(0);
  });

  describe('add', () => {
    it('appends a new line item on an empty cart', () => {
      const product = makeProduct({ id: 1 });
      service.add(product, 2);

      expect(service.items().length).toBe(1);
      expect(service.items()[0]).toEqual({ product, quantity: 2 });
    });

    it('defaults quantity to 1 when not provided', () => {
      const product = makeProduct({ id: 1 });
      service.add(product);

      expect(service.items()[0].quantity).toBe(1);
    });

    it('increments quantity instead of duplicating when product already in cart', () => {
      const product = makeProduct({ id: 1 });
      service.add(product, 1);
      service.add(product, 3);

      expect(service.items().length).toBe(1);
      expect(service.items()[0].quantity).toBe(4);
    });

    it('adds distinct products as separate line items', () => {
      const p1 = makeProduct({ id: 1 });
      const p2 = makeProduct({ id: 2, name: 'Gadget' });
      service.add(p1, 1);
      service.add(p2, 1);

      expect(service.items().length).toBe(2);
    });
  });

  describe('updateQuantity', () => {
    it('changes the quantity of an existing line', () => {
      const product = makeProduct({ id: 1 });
      service.add(product, 1);
      service.updateQuantity(1, 5);

      expect(service.items()[0].quantity).toBe(5);
    });

    it('removes the line when quantity is 0', () => {
      const product = makeProduct({ id: 1 });
      service.add(product, 1);
      service.updateQuantity(1, 0);

      expect(service.items()).toEqual([]);
    });

    it('removes the line when quantity is negative', () => {
      const product = makeProduct({ id: 1 });
      service.add(product, 1);
      service.updateQuantity(1, -2);

      expect(service.items()).toEqual([]);
    });

    it('does not affect other lines', () => {
      const p1 = makeProduct({ id: 1 });
      const p2 = makeProduct({ id: 2, name: 'Gadget' });
      service.add(p1, 1);
      service.add(p2, 2);
      service.updateQuantity(1, 9);

      expect(service.items().find((i) => i.product.id === 1)?.quantity).toBe(9);
      expect(service.items().find((i) => i.product.id === 2)?.quantity).toBe(2);
    });
  });

  describe('remove', () => {
    it('removes only the targeted product', () => {
      const p1 = makeProduct({ id: 1 });
      const p2 = makeProduct({ id: 2, name: 'Gadget' });
      service.add(p1, 1);
      service.add(p2, 2);

      service.remove(1);

      expect(service.items().length).toBe(1);
      expect(service.items()[0].product.id).toBe(2);
    });
  });

  describe('clear', () => {
    it('empties the cart', () => {
      service.add(makeProduct({ id: 1 }), 1);
      service.add(makeProduct({ id: 2 }), 2);

      service.clear();

      expect(service.items()).toEqual([]);
      expect(service.itemCount()).toBe(0);
      expect(service.total()).toBe(0);
    });
  });

  describe('itemCount', () => {
    it('sums quantities across multiple distinct products', () => {
      service.add(makeProduct({ id: 1 }), 2);
      service.add(makeProduct({ id: 2 }), 3);

      expect(service.itemCount()).toBe(5);
    });
  });

  describe('total', () => {
    it('sums price * quantity across multiple lines', () => {
      service.add(makeProduct({ id: 1, price: 10 }), 2); // 20
      service.add(makeProduct({ id: 2, price: 5 }), 3); // 15

      expect(service.total()).toBe(35);
    });
  });
});
