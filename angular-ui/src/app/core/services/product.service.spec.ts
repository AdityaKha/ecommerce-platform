import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ProductService } from './product.service';
import { environment } from '../../../environments/environment';
import { Product } from '../models/product.models';

describe('ProductService', () => {
  let service: ProductService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/api/products`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(ProductService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('findAll', () => {
    it('should GET /api/products and return the mocked array', () => {
      const mockProducts: Product[] = [
        { id: 1, name: 'Widget', description: 'A widget', sku: 'W-1', price: 9.99, category: 'Tools', active: true },
        { id: 2, name: 'Gadget', description: 'A gadget', sku: 'G-1', price: 19.99, category: 'Tools', active: true },
      ];

      let actual: Product[] | undefined;
      service.findAll().subscribe((res) => (actual = res));

      const httpReq = httpMock.expectOne(baseUrl);
      expect(httpReq.request.method).toBe('GET');
      httpReq.flush(mockProducts);

      expect(actual).toEqual(mockProducts);
    });
  });
});
