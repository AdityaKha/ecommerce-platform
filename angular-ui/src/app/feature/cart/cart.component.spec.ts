import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { By } from '@angular/platform-browser';
import { CartComponent } from './cart.component';
import { CartService } from '../../core/services/cart.service';
import { Product } from '../../core/models/product.models';

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

describe('CartComponent', () => {
  let cartService: CartService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CartComponent],
      providers: [
        CartService,
        provideRouter([]),
        { provide: ActivatedRoute, useValue: {} },
      ],
    }).compileComponents();

    cartService = TestBed.inject(CartService);
  });

  it('renders the empty-state message and no table when the cart is empty', () => {
    const fixture = TestBed.createComponent(CartComponent);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Your cart is empty.');
    expect(compiled.querySelector('.cart-table')).toBeNull();
  });

  it('renders rows with correct product info and total when the cart has items', () => {
    cartService.add(makeProduct({ id: 1, name: 'Widget', price: 10 }), 2);
    cartService.add(makeProduct({ id: 2, name: 'Gadget', price: 5 }), 3);

    const fixture = TestBed.createComponent(CartComponent);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const rows = compiled.querySelectorAll('.cart-table tbody tr');
    expect(rows.length).toBe(2);

    expect(compiled.textContent).toContain('Widget');
    expect(compiled.textContent).toContain('Gadget');

    const total = compiled.querySelector('.cart-total');
    // 10*2 + 5*3 = 35
    expect(total?.textContent).toContain('35');
  });

  it('clicking increment calls cartService.updateQuantity with quantity + 1', () => {
    cartService.add(makeProduct({ id: 1 }), 2);
    const fixture = TestBed.createComponent(CartComponent);
    fixture.detectChanges();
    spyOn(cartService, 'updateQuantity');

    const incrementBtn = fixture.debugElement.queryAll(By.css('button[mat-icon-button]'))[1];
    incrementBtn.triggerEventHandler('click', null);

    expect(cartService.updateQuantity).toHaveBeenCalledWith(1, 3);
  });

  it('clicking decrement calls cartService.updateQuantity with quantity - 1', () => {
    cartService.add(makeProduct({ id: 1 }), 2);
    const fixture = TestBed.createComponent(CartComponent);
    fixture.detectChanges();
    spyOn(cartService, 'updateQuantity');

    const decrementBtn = fixture.debugElement.queryAll(By.css('button[mat-icon-button]'))[0];
    decrementBtn.triggerEventHandler('click', null);

    expect(cartService.updateQuantity).toHaveBeenCalledWith(1, 1);
  });

  it('clicking Remove calls cartService.remove with the right productId', () => {
    cartService.add(makeProduct({ id: 1 }), 1);
    cartService.add(makeProduct({ id: 2 }), 1);
    const fixture = TestBed.createComponent(CartComponent);
    fixture.detectChanges();
    spyOn(cartService, 'remove');

    const removeButtons = fixture.debugElement.queryAll(By.css('button[color="warn"]'));
    removeButtons[1].triggerEventHandler('click', null);

    expect(cartService.remove).toHaveBeenCalledWith(2);
  });
});
