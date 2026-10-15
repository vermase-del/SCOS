import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { User, AuthResponse, ApiResponse } from './user.model';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let routerSpy: jasmine.SpyObj<Router>;

  const mockUser: User = {
    id: '11111111-1111-1111-1111-111111111111',
    username: 'test.user',
    firstName: 'Test',
    lastName: 'User',
    email: 'test@example.com',
    active: true,
    verified: true,
    primaryRole: 'COMMUNITY_ADMIN',
    roles: ['COMMUNITY_ADMIN'],
    permissions: ['SCREEN:DASHBOARD', 'SCREEN:WORK_ORDERS', 'WORKORDER:READ', 'WORKORDER:WRITE'],
    activeCommunityId: '22222222-2222-2222-2222-222222222222'
  };

  beforeEach(() => {
    localStorage.clear();
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AuthService,
        { provide: Router, useValue: routerSpy }
      ]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should report not authenticated when no token in storage', () => {
    expect(service.isAuthenticated()).toBeFalse();
    expect(service.getToken()).toBeNull();
    expect(service.currentUser()).toBeNull();
  });

  it('should successfully login and store session', () => {
    const authData: AuthResponse = {
      token: 'jwt.token.here',
      tokenType: 'Bearer',
      user: mockUser
    };
    const mockApiResponse: ApiResponse<AuthResponse> = {
      success: true,
      message: 'Login successful',
      data: authData,
      timestamp: new Date().toISOString()
    };

    service.login({ username: 'test.user', password: 'Password@123' }).subscribe(res => {
      expect(res.success).toBeTrue();
      expect(res.data?.token).toBe('jwt.token.here');
    });

    const req = httpMock.expectOne('/api/v1/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush(mockApiResponse);

    expect(localStorage.getItem('scos_token')).toBe('jwt.token.here');
    expect(service.isAuthenticated()).toBeTrue();
    expect(service.currentUser()?.username).toBe('test.user');
    expect(service.hasPermission('SCREEN:WORK_ORDERS')).toBeTrue();
    expect(service.hasPermission('NON_EXISTENT_PERMISSION')).toBeFalse();
    expect(service.hasAnyPermission('SCREEN:DASHBOARD', 'OTHER')).toBeTrue();
  });

  it('should grant all permissions to SUPER_ADMIN', () => {
    const superAdmin: User = {
      ...mockUser,
      primaryRole: 'SUPER_ADMIN',
      roles: ['SUPER_ADMIN'],
      permissions: []
    };

    const authData: AuthResponse = {
      token: 'superadmin.jwt.token',
      tokenType: 'Bearer',
      user: superAdmin
    };

    service.login({ username: 'admin', password: 'Password@123' }).subscribe();
    const req = httpMock.expectOne('/api/v1/auth/login');
    req.flush({ success: true, message: 'OK', data: authData, timestamp: '' });

    expect(service.hasPermission('ANY_RANDOM_PERMISSION')).toBeTrue();
    expect(service.hasAnyPermission('SCREEN:NOT_EXIST')).toBeTrue();
  });

  it('should clear session and navigate to login on logout', () => {
    service.logout();
    expect(localStorage.getItem('scos_token')).toBeNull();
    expect(localStorage.getItem('scos_user')).toBeNull();
    expect(service.isAuthenticated()).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
  });
});
