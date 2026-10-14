export interface User {
  id: string;
  username: string;
  email: string;
  phoneNumber?: string;
  firstName: string;
  lastName: string;
  avatarUrl?: string;
  active: boolean;
  verified: boolean;
  roles: string[];
  primaryRole?: string;
  permissions?: string[];
  activeCommunityId?: string;
}

export interface RoleDto {
  id: string;
  code: string;
  name: string;
  description: string;
  systemRole: boolean;
  permissions: string[];
}

export interface PermissionDto {
  id: string;
  code: string;
  module: string;
  description: string;
}

export interface CreateUserRequest {
  username: string;
  email: string;
  phoneNumber?: string;
  firstName: string;
  lastName: string;
  password?: string;
  roleCode?: string;
  communityId?: string;
}

export interface UpdateRolePermissionsRequest {
  permissionCodes: string[];
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  user: User;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  errorCode?: string;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
