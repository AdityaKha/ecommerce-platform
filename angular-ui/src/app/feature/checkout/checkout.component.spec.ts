import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { CheckoutComponent } from './checkout.component';
import { CartService } from '../../core/services/cart.service';
import { OrderService } from '../../core/services/order.service';
import { AuthService } from '../../core/services/auth.service';
import { Product } from '../../core/models/product.models';
import { OrderResponse } from '../../core/models/order.models';

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

describe('CheckoutComponent', () => {
  let cartService: CartService;
  let orderServiceSpy: jasmine.SpyObj<OrderService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  const mockOrderResponse: OrderResponse = {
    id: 1,
    customerUsername: 'jdoe',
    customerEmail: 'jdoe@example.com',
    status: 'CREATED',
    totalAmount: 20,
    items: [],
    createdAt: new Date().toISOString(),
  };

  beforeEach(async () => {
    orderServiceSpy = jasmine.createSpyObj('OrderService', ['create']);
    authServiceSpy = jasmine.createSpyObj('AuthService', ['getUsername']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    authServiceSpy.getUsername.and.returnValue('jdoe');
    orderServiceSpy.create.and.returnValue(of(mockOrderResponse));

    await TestBed.configureTestingModule({
      imports: [CheckoutComponent],
      providers: [
        CartService,
        { provide: OrderService, useValue: orderServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: {} },
      ],
    }).compileComponents();

    cartService = TestBed.inject(CartService);
  });

  function createComponent() {
    const fixture = TestBed.createComponent(CheckoutComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('should create', () => {
    const fixture = createComponent();
    expect(fixture.componentInstance).toBeTruthy();
  });

  describe('form validation', () => {
    it('is invalid with an empty email', () => {
      const fixture = createComponent();
      const { form } = fixture.componentInstance;

      form.controls.customerEmail.setValue('');
      expect(form.invalid).toBeTrue();
    });

    it('is invalid with a malformed email', () => {
      const fixture = createComponent();
      const { form } = fixture.componentInstance;

      form.controls.customerEmail.setValue('not-an-email');
      expect(form.invalid).toBeTrue();
    });

    it('is valid with a proper email', () => {
      const fixture = createComponent();
      const { form } = fixture.componentInstance;

      form.controls.customerEmail.setValue('jdoe@example.com');
      expect(form.valid).toBeTrue();
    });
  });

  describe('onSubmit', () => {
    it('does not call orderService.create and marks the form touched when the cart is empty', () => {
      const fixture = createComponent();
      const { componentInstance } = fixture;
      componentInstance.form.controls.customerEmail.setValue('jdoe@example.com');

      componentInstance.onSubmit();

      expect(orderServiceSpy.create).not.toHaveBeenCalled();
      expect(componentInstance.form.controls.customerEmail.touched).toBeTrue();
    });

    it('does not call orderService.create when the form is invalid, even with items in the cart', () => {
      cartService.add(makeProduct({ id: 1 }), 1);
      const fixture = createComponent();
      const { componentInstance } = fixture;
      componentInstance.form.controls.customerEmail.setValue('not-an-email');

      componentInstance.onSubmit();

      expect(orderServiceSpy.create).not.toHaveBeenCalled();
      expect(componentInstance.form.controls.customerEmail.touched).toBeTrue();
    });

    it('calls orderService.create with a payload built from username + cart items when valid', () => {
      cartService.add(makeProduct({ id: 1, price: 10 }), 2);
      cartService.add(makeProduct({ id: 2, price: 5 }), 1);
      const fixture = createComponent();
      const { componentInstance } = fixture;
      componentInstance.form.controls.customerEmail.setValue('jdoe@example.com');

      componentInstance.onSubmit();

      expect(orderServiceSpy.create).toHaveBeenCalledWith({
        customerUsername: 'jdoe',
        customerEmail: 'jdoe@example.com',
        items: [
          { productId: 1, quantity: 2, unitPrice: 10 },
          { productId: 2, quantity: 1, unitPrice: 5 },
        ],
      });
    });

    it('clears the cart and navigates to /orders on success', () => {
      cartService.add(makeProduct({ id: 1 }), 1);
      const fixture = createComponent();
      const { componentInstance } = fixture;
      componentInstance.form.controls.customerEmail.setValue('jdoe@example.com');
      spyOn(cartService, 'clear').and.callThrough();

      componentInstance.onSubmit();

      expect(cartService.clear).toHaveBeenCalled();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/orders']);
    });

    it('resets loading and sets errorMessage from err.error.message on error', () => {
      orderServiceSpy.create.and.returnValue(throwError(() => ({ error: { message: 'Out of stock' } })));
      cartService.add(makeProduct({ id: 1 }), 1);
      const fixture = createComponent();
      const { componentInstance } = fixture;
      componentInstance.form.controls.customerEmail.setValue('jdoe@example.com');

      componentInstance.onSubmit();

      expect(componentInstance.loading).toBeFalse();
      expect(componentInstance.errorMessage).toBe('Out of stock');
    });

    it('falls back to the default error message when err.error.message is absent', () => {
      orderServiceSpy.create.and.returnValue(throwError(() => ({})));
      cartService.add(makeProduct({ id: 1 }), 1);
      const fixture = createComponent();
      const { componentInstance } = fixture;
      componentInstance.form.controls.customerEmail.setValue('jdoe@example.com');

      componentInstance.onSubmit();

      expect(componentInstance.loading).toBeFalse();
      expect(componentInstance.errorMessage).toBe('Failed to place order. Please try again.');
    });

    it('sets loading to true while the request is in flight', () => {
      // Use a non-emitting observable substitute isn't needed since of() is synchronous;
      // instead verify loading was set true by checking it via a delayed-style spy.
      let capturedLoadingDuringCall = false;
      orderServiceSpy.create.and.callFake(() => {
        capturedLoadingDuringCall = componentInstance.loading;
        return of(mockOrderResponse);
      });
      cartService.add(makeProduct({ id: 1 }), 1);
      const fixture = createComponent();
      const componentInstance = fixture.componentInstance;
      componentInstance.form.controls.customerEmail.setValue('jdoe@example.com');

      componentInstance.onSubmit();

      expect(capturedLoadingDuringCall).toBeTrue();
    });
  });
});
