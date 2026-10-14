import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { User, RoleDto, PermissionDto, CreateUserRequest, ApiResponse, PageResponse } from '../../core/auth/user.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);
  private apiUrl = '/api/v1/users';

  getUsers(page = 0, size = 50): Observable<ApiResponse<PageResponse<User>>> {
    return this.http.get<ApiResponse<PageResponse<User>>>(this.apiUrl, {
      params: { page: page.toString(), size: size.toString() }
    });
  }

  createUser(payload: CreateUserRequest): Observable<ApiResponse<User>> {
    return this.http.post<ApiResponse<User>>(this.apiUrl, payload);
  }

  toggleStatus(userId: string, active: boolean): Observable<ApiResponse<void>> {
    return this.http.patch<ApiResponse<void>>(`${this.apiUrl}/${userId}/status`, null, {
      params: { active: active.toString() }
    });
  }

  assignRole(userId: string, communityId: string, roleCode: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/${userId}/roles`, null, {
      params: { communityId, roleCode }
    });
  }

  getRoles(): Observable<ApiResponse<RoleDto[]>> {
    return this.http.get<ApiResponse<RoleDto[]>>(`${this.apiUrl}/roles`);
  }

  getPermissions(): Observable<ApiResponse<PermissionDto[]>> {
    return this.http.get<ApiResponse<PermissionDto[]>>(`${this.apiUrl}/permissions`);
  }

  updateRolePermissions(roleCode: string, permissionCodes: string[]): Observable<ApiResponse<RoleDto>> {
    return this.http.put<ApiResponse<RoleDto>>(`${this.apiUrl}/roles/${roleCode}/permissions`, {
      permissionCodes
    });
  }

  getMyPermissions(): Observable<ApiResponse<string[]>> {
    return this.http.get<ApiResponse<string[]>>(`${this.apiUrl}/me/permissions`);
  }
}
