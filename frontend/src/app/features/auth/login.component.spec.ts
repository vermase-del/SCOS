import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LoginComponent } from './login.component';
import { AuthService } from '../../core/auth/auth.service';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['login']);

    await TestBed.configureTestingModule({
      imports: [LoginComponent, CommonModule, FormsModule],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create login component', () => {
    expect(component).toBeTruthy();
  });

  it('should show error when submitting empty fields', () => {
    component.username = '';
    component.password = '';
    component.onLogin();

    expect(component.errorMessage()).toBe('Please enter both username and password');
    expect(authServiceSpy.login).not.toHaveBeenCalled();
  });

  it('should navigate to dashboard upon successful login', () => {
    component.username = 'admin';
    component.password = 'Password@123';

    authServiceSpy.login.and.returnValue(of({
      success: true,
      message: 'OK',
      data: {
        token: 'mock.token',
        tokenType: 'Bearer',
        user: {
          id: '1',
          username: 'admin',
          firstName: 'Admin',
          lastName: 'User',
          email: 'admin@scos.local',
          active: true,
          verified: true,
          primaryRole: 'SUPER_ADMIN',
          roles: ['SUPER_ADMIN'],
          permissions: [],
          activeCommunityId: 'comm-1'
        }
      },
      timestamp: ''
    }));

    component.onLogin();

    expect(component.loading()).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should display error message on failed login', () => {
    component.username = 'invalid';
    component.password = 'wrong';

    authServiceSpy.login.and.returnValue(throwError(() => ({
      error: { message: 'Invalid username or password' }
    })));

    component.onLogin();

    expect(component.loading()).toBeFalse();
    expect(component.errorMessage()).toBe('Invalid username or password');
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
