import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth.models';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/api/auth`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    localStorage.clear();
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('login', () => {
    it('should POST to /api/auth/login with the request body and return the response', () => {
      const req: LoginRequest = { username: 'jdoe', password: 'secret' };
      const mockResponse: AuthResponse = { token: 'tok123', username: 'jdoe', expiresInMillis: 3600000 };

      let actual: AuthResponse | undefined;
      service.login(req).subscribe((res) => (actual = res));

      const httpReq = httpMock.expectOne(`${baseUrl}/login`);
      expect(httpReq.request.method).toBe('POST');
      expect(httpReq.request.body).toEqual(req);
      httpReq.flush(mockResponse);

      expect(actual).toEqual(mockResponse);
    });
  });

  describe('register', () => {
    it('should POST to /api/auth/register with the request body and return the response', () => {
      const req: RegisterRequest = { username: 'jdoe', email: 'jdoe@example.com', password: 'secret' };
      const mockResponse: AuthResponse = { token: 'tok456', username: 'jdoe', expiresInMillis: 3600000 };

      let actual: AuthResponse | undefined;
      service.register(req).subscribe((res) => (actual = res));

      const httpReq = httpMock.expectOne(`${baseUrl}/register`);
      expect(httpReq.request.method).toBe('POST');
      expect(httpReq.request.body).toEqual(req);
      httpReq.flush(mockResponse);

      expect(actual).toEqual(mockResponse);
    });
  });

  describe('session management', () => {
    it('saveSession should write both localStorage keys', () => {
      const res: AuthResponse = { token: 'tok789', username: 'alice', expiresInMillis: 1000 };
      service.saveSession(res);

      expect(localStorage.getItem('auth_token')).toBe('tok789');
      expect(localStorage.getItem('auth_username')).toBe('alice');
    });

    it('getToken and getUsername should read back saved values', () => {
      service.saveSession({ token: 'abc', username: 'bob', expiresInMillis: 500 });

      expect(service.getToken()).toBe('abc');
      expect(service.getUsername()).toBe('bob');
    });

    it('getToken and getUsername should return null when nothing is saved', () => {
      expect(service.getToken()).toBeNull();
      expect(service.getUsername()).toBeNull();
    });

    it('logout should clear both localStorage keys', () => {
      service.saveSession({ token: 'abc', username: 'bob', expiresInMillis: 500 });
      service.logout();

      expect(localStorage.getItem('auth_token')).toBeNull();
      expect(localStorage.getItem('auth_username')).toBeNull();
    });

    it('isAuthenticated should be false when there is no token', () => {
      expect(service.isAuthenticated()).toBeFalse();
    });

    it('isAuthenticated should be true once a token is set', () => {
      service.saveSession({ token: 'abc', username: 'bob', expiresInMillis: 500 });
      expect(service.isAuthenticated()).toBeTrue();
    });
  });
});
