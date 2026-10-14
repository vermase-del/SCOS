import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { AuthResponse, User, ApiResponse } from './user.model';
import { Router } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private currentUserSignal = signal<User | null>(this.getStoredUser());
  private tokenSignal = signal<string | null>(localStorage.getItem('scos_token'));

  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly isAuthenticated = computed(() => !!this.tokenSignal());
  readonly activeCommunityId = computed(() => this.currentUserSignal()?.activeCommunityId || null);

  login(credentials: { username: string; password: string; communityId?: string }): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>('/api/v1/auth/login', credentials).pipe(
      tap(response => {
        if (response.success && response.data) {
          this.setSession(response.data);
        }
      })
    );
  }

  logout(): void {
    localStorage.removeItem('scos_token');
    localStorage.removeItem('scos_user');
    this.tokenSignal.set(null);
    this.currentUserSignal.set(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return this.tokenSignal();
  }

  /**
   * Evaluates if current user holds a specific screen or capability permission.
   * SUPER_ADMIN automatically holds all permissions.
   */
  hasPermission(permissionCode: string): boolean {
    const user = this.currentUserSignal();
    if (!user) return false;
    if (user.roles?.includes('SUPER_ADMIN') || user.primaryRole === 'SUPER_ADMIN') {
      return true;
    }
    return user.permissions?.includes(permissionCode) ?? false;
  }

  hasAnyPermission(...permissionCodes: string[]): boolean {
    return permissionCodes.some(p => this.hasPermission(p));
  }

  hasRole(roleCode: string): boolean {
    const user = this.currentUserSignal();
    if (!user) return false;
    return (user.roles && user.roles.includes(roleCode)) || user.primaryRole === roleCode;
  }

  updateCurrentUserPermissions(permissions: string[]): void {
    const user = this.currentUserSignal();
    if (user) {
      const updatedUser: User = { ...user, permissions };
      localStorage.setItem('scos_user', JSON.stringify(updatedUser));
      this.currentUserSignal.set(updatedUser);
    }
  }

  private setSession(authResult: AuthResponse): void {
    localStorage.setItem('scos_token', authResult.token);
    localStorage.setItem('scos_user', JSON.stringify(authResult.user));
    this.tokenSignal.set(authResult.token);
    this.currentUserSignal.set(authResult.user);
  }

  private getStoredUser(): User | null {
    const userJson = localStorage.getItem('scos_user');
    if (!userJson) return null;
    try {
      return JSON.parse(userJson);
    } catch {
      return null;
    }
  }
}
