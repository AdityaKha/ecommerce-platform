import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { OrderHistoryComponent } from './order-history.component';
import { OrderService } from '../../core/services/order.service';
import { AuthService } from '../../core/services/auth.service';
import { OrderResponse } from '../../core/models/order.models';

function makeOrder(overrides: Partial<OrderResponse> = {}): OrderResponse {
  return {
    id: 1,
    customerUsername: 'jdoe',
    customerEmail: 'jdoe@example.com',
    status: 'CREATED',
    totalAmount: 20,
    items: [{ productId: 1, quantity: 2, unitPrice: 10 }],
    createdAt: new Date().toISOString(),
    ...overrides,
  };
}

describe('OrderHistoryComponent', () => {
  let orderServiceSpy: jasmine.SpyObj<OrderService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    orderServiceSpy = jasmine.createSpyObj('OrderService', ['findAll']);
    authServiceSpy = jasmine.createSpyObj('AuthService', ['getUsername']);

    await TestBed.configureTestingModule({
      imports: [OrderHistoryComponent],
      providers: [
        { provide: OrderService, useValue: orderServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: ActivatedRoute, useValue: {} },
      ],
    }).compileComponents();
  });

  function createComponent() {
    return TestBed.createComponent(OrderHistoryComponent);
  }

  it('should create', () => {
    authServiceSpy.getUsername.and.returnValue('jdoe');
    orderServiceSpy.findAll.and.returnValue(of([]));
    const fixture = createComponent();
    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('fetches orders on init', () => {
    authServiceSpy.getUsername.and.returnValue('jdoe');
    orderServiceSpy.findAll.and.returnValue(of([]));
    const fixture = createComponent();
    fixture.detectChanges();

    expect(orderServiceSpy.findAll).toHaveBeenCalled();
  });

  it('filters orders by the signed-in username client-side', () => {
    authServiceSpy.getUsername.and.returnValue('jdoe');
    const orders = [
      makeOrder({ id: 1, customerUsername: 'jdoe' }),
      makeOrder({ id: 2, customerUsername: 'alice' }),
      makeOrder({ id: 3, customerUsername: 'jdoe' }),
    ];
    orderServiceSpy.findAll.and.returnValue(of(orders));

    const fixture = createComponent();
    fixture.detectChanges();

    const result = fixture.componentInstance.orders();
    expect(result.length).toBe(2);
    expect(result.every((o) => o.customerUsername === 'jdoe')).toBeTrue();
  });

  it('renders the orders after loading', () => {
    authServiceSpy.getUsername.and.returnValue('jdoe');
    orderServiceSpy.findAll.and.returnValue(of([makeOrder({ id: 42, customerUsername: 'jdoe' })]));

    const fixture = createComponent();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(fixture.componentInstance.loading()).toBeFalse();
    expect(compiled.textContent).toContain('Order #42');
  });

  it('renders the empty-state message when there are no orders for this user', () => {
    authServiceSpy.getUsername.and.returnValue('jdoe');
    orderServiceSpy.findAll.and.returnValue(of([makeOrder({ id: 1, customerUsername: 'someone-else' })]));

    const fixture = createComponent();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain("You haven't placed any orders yet.");
  });

  it('sets an error message and stops loading when the request fails', () => {
    authServiceSpy.getUsername.and.returnValue('jdoe');
    orderServiceSpy.findAll.and.returnValue(throwError(() => new Error('network error')));

    const fixture = createComponent();
    fixture.detectChanges();

    expect(fixture.componentInstance.loading()).toBeFalse();
    expect(fixture.componentInstance.errorMessage()).toBe('Failed to load orders.');

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Failed to load orders.');
  });
});
