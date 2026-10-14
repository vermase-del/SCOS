import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from './user.service';
import { AuthService } from '../../core/auth/auth.service';
import { User, RoleDto, PermissionDto, CreateUserRequest } from '../../core/auth/user.model';

@Component({
  selector: 'scos-user-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="user-management-page">
      <!-- Page Header & Top Tabs -->
      <div class="header-card glass-panel">
        <div class="header-content">
          <div>
            <div class="breadcrumb">IDENTITY & ACCESS CONTROL</div>
            <h1 class="page-title">User Management & Permissions</h1>
            <p class="page-subtitle">Configure multi-tenant resident accounts, assign security roles, and enforce fine-grained screen permissions.</p>
          </div>
          <div class="header-actions" *ngIf="activeTab() === 'directory'">
            <button
              *ngIf="authService.hasPermission('SCREEN:USER_CREATE')"
              class="btn-primary"
              (click)="openCreateUserModal()">
              <span class="btn-icon">+</span>
              <span>Invite New User</span>
            </button>
          </div>
          <div class="header-actions" *ngIf="activeTab() === 'matrix'">
            <button
              class="btn-primary"
              [disabled]="!isMatrixDirty() || isSavingMatrix()"
              (click)="savePermissionMatrix()">
              <span class="btn-icon">💾</span>
              <span>{{ isSavingMatrix() ? 'Saving...' : 'Save Matrix Changes' }}</span>
            </button>
          </div>
        </div>

        <!-- Navigation Tabs -->
        <div class="tab-bar">
          <button
            class="tab-btn"
            [class.active]="activeTab() === 'directory'"
            (click)="setActiveTab('directory')">
            <span class="tab-icon">👥</span>
            <span>User Directory ({{ users().length }})</span>
          </button>
          <button
            class="tab-btn"
            [class.active]="activeTab() === 'matrix'"
            (click)="setActiveTab('matrix')">
            <span class="tab-icon">🛡️</span>
            <span>Screen Permission Matrix</span>
            <span class="badge-modified" *ngIf="isMatrixDirty()">Modified</span>
          </button>
        </div>
      </div>

      <!-- Success / Status Toast Banner -->
      <div class="alert-banner glass-panel" *ngIf="alertMessage()">
        <span class="alert-icon">✨</span>
        <span class="alert-text">{{ alertMessage() }}</span>
        <button class="alert-dismiss" (click)="clearAlert()">×</button>
      </div>

      <!-- ========================================================= -->
      <!-- TAB 1: USER DIRECTORY                                     -->
      <!-- ========================================================= -->
      <div *ngIf="activeTab() === 'directory'" class="tab-content">
        <!-- Filter Toolbar -->
        <div class="toolbar-panel glass-panel">
          <div class="search-box">
            <span class="search-icon">🔍</span>
            <input
              type="text"
              placeholder="Search by name, email, or username..."
              [ngModel]="searchTerm()"
              (ngModelChange)="searchTerm.set($event)"
            />
          </div>

          <div class="filter-controls">
            <div class="select-wrapper">
              <label>Role:</label>
              <select [ngModel]="selectedRoleFilter()" (ngModelChange)="selectedRoleFilter.set($event)">
                <option value="ALL">All Roles</option>
                <option *ngFor="let role of roles()" [value]="role.code">{{ role.name }}</option>
              </select>
            </div>

            <div class="select-wrapper">
              <label>Status:</label>
              <select [ngModel]="selectedStatusFilter()" (ngModelChange)="selectedStatusFilter.set($event)">
                <option value="ALL">All Status</option>
                <option value="ACTIVE">Active Only</option>
                <option value="INACTIVE">Inactive Only</option>
              </select>
            </div>
          </div>
        </div>

        <!-- Users Table -->
        <div class="table-container glass-panel">
          <table class="data-table">
            <thead>
              <tr>
                <th>USER / PROFILE</th>
                <th>CONTACT DETAILS</th>
                <th>ROLE & TENANCY</th>
                <th>SCREEN PERMISSIONS</th>
                <th>ACCOUNT STATUS</th>
                <th class="actions-header">ACTIONS</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let user of filteredUsers()">
                <td>
                  <div class="user-cell">
                    <div class="avatar-circle" [style.background]="getAvatarGradient(user.firstName)">
                      {{ getInitials(user) }}
                    </div>
                    <div class="user-info">
                      <div class="user-fullname">{{ user.firstName }} {{ user.lastName }}</div>
                      <div class="user-username">&#64;{{ user.username }}</div>
                    </div>
                  </div>
                </td>
                <td>
                  <div class="contact-email">{{ user.email }}</div>
                  <div class="contact-phone">{{ user.phoneNumber || '—' }}</div>
                </td>
                <td>
                  <span class="role-chip" [ngClass]="getRoleBadgeClass(user.primaryRole || user.roles[0])">
                    {{ formatRoleName(user.primaryRole || user.roles[0]) }}
                  </span>
                </td>
                <td>
                  <div class="permissions-pill-group">
                    <span class="perm-count-badge" (click)="togglePermissionsPopover(user.id)">
                      {{ (user.permissions && user.permissions.length > 0) ? user.permissions.length : getRolePermCount(user) }} Screens
                    </span>
                    <div class="perm-popover glass-panel" *ngIf="expandedUserId() === user.id">
                      <div class="popover-title">Authorized Screens:</div>
                      <div class="popover-tags">
                        <span *ngFor="let perm of getUserPermissionsList(user)" class="popover-tag">
                          {{ perm.replace('SCREEN:', '') }}
                        </span>
                      </div>
                    </div>
                  </div>
                </td>
                <td>
                  <span class="status-indicator" [class.status-active]="user.active" [class.status-inactive]="!user.active">
                    <span class="pulse-dot"></span>
                    {{ user.active ? 'ACTIVE' : 'DEACTIVATED' }}
                  </span>
                </td>
                <td class="actions-cell">
                  <button
                    class="btn-status-toggle"
                    [class.btn-deactivate]="user.active"
                    [class.btn-activate]="!user.active"
                    (click)="toggleUserStatus(user)">
                    {{ user.active ? 'Deactivate' : 'Activate' }}
                  </button>
                </td>
              </tr>
              <tr *ngIf="filteredUsers().length === 0">
                <td colspan="6" class="empty-state">
                  <div class="empty-icon">📂</div>
                  <div class="empty-title">No matching users found</div>
                  <div class="empty-sub">Try modifying your search or filter criteria</div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- ========================================================= -->
      <!-- TAB 2: ROLE & SCREEN PERMISSION MATRIX                    -->
      <!-- ========================================================= -->
      <div *ngIf="activeTab() === 'matrix'" class="tab-content">
        <!-- Matrix Guidance Notice -->
        <div class="matrix-notice glass-panel">
          <div class="notice-left">
            <span class="notice-icon">🔐</span>
            <div>
              <div class="notice-title">Real-Time Screen Permission Mechanism</div>
              <div class="notice-desc">
                Toggle access rights for each screen view. Permissions granted here directly control sidebar visibility, route guards, and UI capabilities across all users belonging to that role.
              </div>
            </div>
          </div>
          <div class="notice-right">
            <button
              class="btn-secondary"
              [disabled]="!isMatrixDirty()"
              (click)="resetMatrixChanges()">
              Reset
            </button>
            <button
              class="btn-primary"
              [disabled]="!isMatrixDirty() || isSavingMatrix()"
              (click)="savePermissionMatrix()">
              💾 Save Permissions
            </button>
          </div>
        </div>

        <!-- 2D RBAC Grid -->
        <div class="matrix-container glass-panel">
          <table class="matrix-table">
            <thead>
              <tr>
                <th class="matrix-feature-header">SCREEN / CAPABILITY</th>
                <th *ngFor="let role of roles()" class="matrix-role-header">
                  <div class="role-header-cell">
                    <span class="role-header-title">{{ role.name }}</span>
                    <span class="role-header-code">{{ role.code }}</span>
                  </div>
                </th>
              </tr>
            </thead>
            <tbody>
              <ng-container *ngFor="let group of groupedPermissions()">
                <!-- Group Header Row -->
                <tr class="group-header-row">
                  <td [attr.colspan]="roles().length + 1">
                    <span class="group-title">{{ group.category }}</span>
                  </td>
                </tr>

                <!-- Permission Row -->
                <tr *ngFor="let perm of group.items" class="matrix-row">
                  <td class="perm-desc-cell">
                    <div class="perm-name">{{ perm.description }}</div>
                    <div class="perm-code">{{ perm.code }}</div>
                  </td>

                  <td *ngFor="let role of roles()" class="matrix-cell">
                    <!-- Locked SUPER_ADMIN Cell -->
                    <div *ngIf="role.code === 'SUPER_ADMIN'" class="locked-access">
                      <span class="lock-icon">🔒</span>
                      <span class="full-access-text">Always Granted</span>
                    </div>

                    <!-- Interactive Checkbox for Other Roles -->
                    <label *ngIf="role.code !== 'SUPER_ADMIN'" class="matrix-checkbox-label">
                      <input
                        type="checkbox"
                        [checked]="hasMatrixPermission(role.code, perm.code)"
                        (change)="toggleMatrixPermission(role.code, perm.code)"
                      />
                      <span class="checkbox-custom"></span>
                    </label>
                  </td>
                </tr>
              </ng-container>
            </tbody>
          </table>
        </div>
      </div>

      <!-- ========================================================= -->
      <!-- MODAL: INVITE / CREATE USER                               -->
      <!-- ========================================================= -->
      <div class="modal-backdrop" *ngIf="showCreateModal()" (click)="closeCreateUserModal()">
        <div class="modal-dialog glass-panel" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <div>
              <h3 class="modal-title">Invite New Resident or Staff</h3>
              <p class="modal-subtitle">Provision account credentials and assign society role permissions</p>
            </div>
            <button class="modal-close-btn" (click)="closeCreateUserModal()">✕</button>
          </div>

          <form (ngSubmit)="submitCreateUser()" class="modal-form">
            <div class="form-row">
              <div class="form-group">
                <label>First Name *</label>
                <input type="text" [(ngModel)]="newUser.firstName" name="firstName" required placeholder="e.g. Rahul" />
              </div>
              <div class="form-group">
                <label>Last Name *</label>
                <input type="text" [(ngModel)]="newUser.lastName" name="lastName" required placeholder="e.g. Sharma" />
              </div>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label>Username *</label>
                <input type="text" [(ngModel)]="newUser.username" name="username" required placeholder="e.g. rahul.sharma" />
              </div>
              <div class="form-group">
                <label>Email Address *</label>
                <input type="email" [(ngModel)]="newUser.email" name="email" required placeholder="e.g. rahul@example.com" />
              </div>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label>Phone Number</label>
                <input type="tel" [(ngModel)]="newUser.phoneNumber" name="phoneNumber" placeholder="e.g. +91 9876543210" />
              </div>
              <div class="form-group">
                <label>Assigned Role *</label>
                <select [(ngModel)]="newUser.roleCode" name="roleCode" required>
                  <option *ngFor="let role of roles()" [value]="role.code">
                    {{ role.name }} ({{ role.code }})
                  </option>
                </select>
              </div>
            </div>

            <div class="form-group">
              <label>Initial Temporary Password *</label>
              <input type="password" [(ngModel)]="newUser.password" name="password" required placeholder="Min 8 characters with numbers" />
            </div>

            <div class="modal-actions">
              <button type="button" class="btn-secondary" (click)="closeCreateUserModal()">Cancel</button>
              <button type="submit" class="btn-primary" [disabled]="isSubmittingUser()">
                {{ isSubmittingUser() ? 'Creating User...' : 'Provision User Account' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .user-management-page {
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
      padding-bottom: 3rem;
    }

    .header-card {
      padding: 1.75rem 2rem 0.5rem;
      display: flex;
      flex-direction: column;
      gap: 1.25rem;
    }
    .header-content {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
    }
    .breadcrumb {
      font-size: 0.75rem;
      font-weight: 700;
      letter-spacing: 0.1em;
      color: #818cf8;
      margin-bottom: 0.25rem;
    }
    .page-title {
      font-size: 1.75rem;
      font-weight: 800;
      letter-spacing: -0.02em;
      margin: 0;
      color: #ffffff;
    }
    .page-subtitle {
      color: #94a3b8;
      font-size: 0.875rem;
      margin-top: 0.35rem;
    }

    .tab-bar {
      display: flex;
      gap: 1rem;
      border-bottom: 1px solid rgba(255, 255, 255, 0.08);
      padding-top: 0.5rem;
    }
    .tab-btn {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      background: none;
      border: none;
      color: #94a3b8;
      font-size: 0.9375rem;
      font-weight: 600;
      padding: 0.75rem 1rem;
      cursor: pointer;
      position: relative;
      transition: all 0.2s ease;
    }
    .tab-btn:hover {
      color: #ffffff;
    }
    .tab-btn.active {
      color: #818cf8;
    }
    .tab-btn.active::after {
      content: '';
      position: absolute;
      bottom: -1px;
      left: 0;
      right: 0;
      height: 3px;
      background: linear-gradient(90deg, #6366f1, #a855f7);
      border-radius: 3px 3px 0 0;
    }
    .badge-modified {
      background: #f59e0b;
      color: #000;
      font-size: 0.6875rem;
      font-weight: 700;
      padding: 0.15rem 0.45rem;
      border-radius: 12px;
      margin-left: 0.35rem;
    }

    .alert-banner {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 1rem 1.5rem;
      background: rgba(16, 185, 129, 0.15);
      border: 1px solid rgba(16, 185, 129, 0.3);
      color: #34d399;
      border-radius: 10px;
    }
    .alert-text {
      flex: 1;
      font-weight: 500;
      font-size: 0.875rem;
    }
    .alert-dismiss {
      background: none;
      border: none;
      color: #34d399;
      font-size: 1.25rem;
      cursor: pointer;
    }

    /* Toolbar */
    .toolbar-panel {
      padding: 1rem 1.5rem;
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 1.5rem;
      flex-wrap: wrap;
    }
    .search-box {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      background: rgba(15, 23, 42, 0.6);
      border: 1px solid rgba(255, 255, 255, 0.1);
      padding: 0.6rem 1rem;
      border-radius: 8px;
      flex: 1;
      min-width: 280px;
    }
    .search-box input {
      background: transparent;
      border: none;
      outline: none;
      color: #ffffff;
      font-size: 0.875rem;
      width: 100%;
    }
    .filter-controls {
      display: flex;
      gap: 1rem;
    }
    .select-wrapper {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 0.8125rem;
      color: #94a3b8;
    }
    .select-wrapper select {
      background: rgba(15, 23, 42, 0.8);
      border: 1px solid rgba(255, 255, 255, 0.1);
      color: #f8fafc;
      padding: 0.5rem 0.85rem;
      border-radius: 6px;
      font-size: 0.8125rem;
      outline: none;
      cursor: pointer;
    }

    /* Data Table */
    .table-container {
      padding: 1.25rem;
      overflow-x: auto;
    }
    .data-table {
      width: 100%;
      border-collapse: separate;
      border-spacing: 0;
    }
    .data-table th {
      text-align: left;
      font-size: 0.6875rem;
      font-weight: 700;
      letter-spacing: 0.08em;
      color: #64748b;
      padding: 0.75rem 1rem;
      border-bottom: 1px solid rgba(255, 255, 255, 0.08);
    }
    .data-table td {
      padding: 1rem;
      font-size: 0.875rem;
      border-bottom: 1px solid rgba(255, 255, 255, 0.04);
      vertical-align: middle;
    }
    .data-table tr:hover td {
      background: rgba(255, 255, 255, 0.02);
    }

    .user-cell {
      display: flex;
      align-items: center;
      gap: 0.875rem;
    }
    .avatar-circle {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 700;
      color: #ffffff;
      font-size: 0.875rem;
      box-shadow: 0 4px 10px rgba(0, 0, 0, 0.3);
      flex-shrink: 0;
    }
    .user-fullname {
      font-weight: 600;
      color: #f1f5f9;
    }
    .user-username {
      font-size: 0.75rem;
      color: #64748b;
      margin-top: 0.15rem;
    }

    .contact-email {
      color: #cbd5e1;
      font-weight: 500;
    }
    .contact-phone {
      color: #64748b;
      font-size: 0.75rem;
      margin-top: 0.2rem;
    }

    .role-chip {
      display: inline-block;
      padding: 0.25rem 0.65rem;
      border-radius: 6px;
      font-size: 0.75rem;
      font-weight: 600;
      letter-spacing: 0.02em;
    }
    .role-super-admin {
      background: rgba(245, 158, 11, 0.15);
      color: #fbbf24;
      border: 1px solid rgba(245, 158, 11, 0.3);
    }
    .role-community-manager {
      background: rgba(168, 85, 247, 0.15);
      color: #c084fc;
      border: 1px solid rgba(168, 85, 247, 0.3);
    }
    .role-hoa-officer {
      background: rgba(59, 130, 246, 0.15);
      color: #60a5fa;
      border: 1px solid rgba(59, 130, 246, 0.3);
    }
    .role-owner {
      background: rgba(16, 185, 129, 0.15);
      color: #34d399;
      border: 1px solid rgba(16, 185, 129, 0.3);
    }
    .role-tenant {
      background: rgba(20, 184, 166, 0.15);
      color: #2dd4bf;
      border: 1px solid rgba(20, 184, 166, 0.3);
    }
    .role-security-guard {
      background: rgba(249, 115, 22, 0.15);
      color: #fb923c;
      border: 1px solid rgba(249, 115, 22, 0.3);
    }
    .role-technician {
      background: rgba(148, 163, 184, 0.15);
      color: #cbd5e1;
      border: 1px solid rgba(148, 163, 184, 0.3);
    }

    .permissions-pill-group {
      position: relative;
    }
    .perm-count-badge {
      display: inline-block;
      padding: 0.25rem 0.65rem;
      background: rgba(99, 102, 241, 0.15);
      border: 1px solid rgba(99, 102, 241, 0.3);
      color: #a5b4fc;
      border-radius: 12px;
      font-size: 0.75rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
    }
    .perm-count-badge:hover {
      background: rgba(99, 102, 241, 0.3);
      color: #ffffff;
    }
    .perm-popover {
      position: absolute;
      top: 100%;
      left: 0;
      z-index: 50;
      margin-top: 0.5rem;
      padding: 0.75rem;
      min-width: 220px;
      border-radius: 8px;
      box-shadow: 0 10px 25px rgba(0, 0, 0, 0.5);
    }
    .popover-title {
      font-size: 0.6875rem;
      font-weight: 700;
      color: #94a3b8;
      margin-bottom: 0.5rem;
    }
    .popover-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 0.35rem;
    }
    .popover-tag {
      background: rgba(255, 255, 255, 0.08);
      font-size: 0.6875rem;
      padding: 0.15rem 0.45rem;
      border-radius: 4px;
      color: #e2e8f0;
    }

    .status-indicator {
      display: inline-flex;
      align-items: center;
      gap: 0.45rem;
      font-size: 0.75rem;
      font-weight: 700;
      letter-spacing: 0.05em;
    }
    .status-indicator.status-active {
      color: #10b981;
    }
    .status-indicator.status-inactive {
      color: #ef4444;
    }
    .pulse-dot {
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: currentColor;
    }
    .status-active .pulse-dot {
      box-shadow: 0 0 8px #10b981;
    }

    .actions-header, .actions-cell {
      text-align: right;
    }
    .btn-status-toggle {
      padding: 0.45rem 0.85rem;
      border-radius: 6px;
      font-size: 0.75rem;
      font-weight: 600;
      cursor: pointer;
      border: none;
      transition: all 0.2s ease;
    }
    .btn-deactivate {
      background: rgba(239, 68, 68, 0.15);
      border: 1px solid rgba(239, 68, 68, 0.3);
      color: #f87171;
    }
    .btn-deactivate:hover {
      background: rgba(239, 68, 68, 0.25);
      color: #ffffff;
    }
    .btn-activate {
      background: rgba(16, 185, 129, 0.15);
      border: 1px solid rgba(16, 185, 129, 0.3);
      color: #34d399;
    }
    .btn-activate:hover {
      background: rgba(16, 185, 129, 0.25);
      color: #ffffff;
    }

    /* Matrix Styles */
    .matrix-notice {
      padding: 1.25rem 1.75rem;
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 1.5rem;
      margin-bottom: 1.5rem;
    }
    .notice-left {
      display: flex;
      align-items: center;
      gap: 1rem;
    }
    .notice-icon {
      font-size: 1.75rem;
    }
    .notice-title {
      font-size: 1rem;
      font-weight: 700;
      color: #ffffff;
    }
    .notice-desc {
      font-size: 0.8125rem;
      color: #94a3b8;
      margin-top: 0.2rem;
      max-width: 750px;
    }
    .notice-right {
      display: flex;
      gap: 0.75rem;
      flex-shrink: 0;
    }

    .matrix-container {
      padding: 1.5rem;
      overflow-x: auto;
    }
    .matrix-table {
      width: 100%;
      border-collapse: separate;
      border-spacing: 0;
    }
    .matrix-feature-header {
      width: 320px;
      text-align: left;
      font-size: 0.75rem;
      font-weight: 700;
      color: #64748b;
      padding: 1rem 1.25rem;
      border-bottom: 2px solid rgba(255, 255, 255, 0.1);
    }
    .matrix-role-header {
      text-align: center;
      padding: 1rem 0.75rem;
      border-bottom: 2px solid rgba(255, 255, 255, 0.1);
      min-width: 130px;
    }
    .role-header-cell {
      display: flex;
      flex-direction: column;
      gap: 0.2rem;
      align-items: center;
    }
    .role-header-title {
      font-size: 0.8125rem;
      font-weight: 700;
      color: #f1f5f9;
    }
    .role-header-code {
      font-size: 0.6875rem;
      color: #64748b;
    }

    .group-header-row td {
      background: rgba(15, 23, 42, 0.4);
      padding: 0.75rem 1.25rem;
      border-top: 1px solid rgba(255, 255, 255, 0.08);
      border-bottom: 1px solid rgba(255, 255, 255, 0.08);
    }
    .group-title {
      font-size: 0.75rem;
      font-weight: 800;
      letter-spacing: 0.08em;
      color: #818cf8;
      text-transform: uppercase;
    }

    .matrix-row td {
      padding: 1rem 1.25rem;
      border-bottom: 1px solid rgba(255, 255, 255, 0.04);
      vertical-align: middle;
    }
    .perm-desc-cell {
      text-align: left;
    }
    .perm-name {
      font-weight: 600;
      color: #f1f5f9;
      font-size: 0.875rem;
    }
    .perm-code {
      font-size: 0.75rem;
      color: #64748b;
      margin-top: 0.15rem;
      font-family: monospace;
    }
    .matrix-cell {
      text-align: center;
    }

    .locked-access {
      display: inline-flex;
      align-items: center;
      gap: 0.35rem;
      font-size: 0.6875rem;
      color: #fbbf24;
      background: rgba(245, 158, 11, 0.1);
      padding: 0.3rem 0.5rem;
      border-radius: 4px;
      font-weight: 600;
    }

    .matrix-checkbox-label {
      position: relative;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
    }
    .matrix-checkbox-label input {
      position: absolute;
      opacity: 0;
      cursor: pointer;
      height: 0;
      width: 0;
    }
    .checkbox-custom {
      height: 22px;
      width: 22px;
      background-color: rgba(15, 23, 42, 0.7);
      border: 2px solid rgba(255, 255, 255, 0.2);
      border-radius: 6px;
      transition: all 0.2s ease;
      position: relative;
    }
    .matrix-checkbox-label:hover input ~ .checkbox-custom {
      border-color: #818cf8;
    }
    .matrix-checkbox-label input:checked ~ .checkbox-custom {
      background: linear-gradient(135deg, #6366f1 0%, #a855f7 100%);
      border-color: #818cf8;
      box-shadow: 0 0 10px rgba(99, 102, 241, 0.5);
    }
    .matrix-checkbox-label input:checked ~ .checkbox-custom::after {
      content: '';
      position: absolute;
      left: 6px;
      top: 2px;
      width: 6px;
      height: 11px;
      border: solid white;
      border-width: 0 2px 2px 0;
      transform: rotate(45deg);
    }

    /* Modal Form */
    .modal-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.75);
      backdrop-filter: blur(8px);
      z-index: 1000;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 1.5rem;
    }
    .modal-dialog {
      width: 100%;
      max-width: 580px;
      border-radius: 12px;
      padding: 2rem;
      box-shadow: 0 20px 50px rgba(0, 0, 0, 0.8);
      border: 1px solid rgba(255, 255, 255, 0.15);
    }
    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 1.75rem;
    }
    .modal-title {
      font-size: 1.25rem;
      font-weight: 700;
      color: #ffffff;
      margin: 0;
    }
    .modal-subtitle {
      font-size: 0.8125rem;
      color: #94a3b8;
      margin-top: 0.25rem;
    }
    .modal-close-btn {
      background: none;
      border: none;
      color: #94a3b8;
      font-size: 1.25rem;
      cursor: pointer;
    }
    .modal-close-btn:hover {
      color: #ffffff;
    }
    .modal-form {
      display: flex;
      flex-direction: column;
      gap: 1.25rem;
    }
    .form-row {
      display: flex;
      gap: 1rem;
    }
    .form-group {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
    }
    .form-group label {
      font-size: 0.75rem;
      font-weight: 600;
      color: #cbd5e1;
    }
    .form-group input, .form-group select {
      background: rgba(15, 23, 42, 0.8);
      border: 1px solid rgba(255, 255, 255, 0.15);
      border-radius: 6px;
      padding: 0.65rem 0.85rem;
      color: #ffffff;
      font-size: 0.875rem;
      outline: none;
      transition: border-color 0.2s;
    }
    .form-group input:focus, .form-group select:focus {
      border-color: #818cf8;
      box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.2);
    }
    .modal-actions {
      display: flex;
      justify-content: flex-end;
      gap: 1rem;
      margin-top: 1rem;
    }

    /* Common Buttons */
    .btn-primary {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      background: linear-gradient(135deg, #6366f1 0%, #a855f7 100%);
      color: #ffffff;
      border: none;
      padding: 0.65rem 1.25rem;
      border-radius: 8px;
      font-size: 0.875rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s ease;
      box-shadow: 0 4px 15px rgba(99, 102, 241, 0.35);
    }
    .btn-primary:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 6px 20px rgba(99, 102, 241, 0.5);
    }
    .btn-primary:disabled {
      opacity: 0.5;
      cursor: not-allowed;
      transform: none;
    }
    .btn-secondary {
      background: rgba(255, 255, 255, 0.06);
      border: 1px solid rgba(255, 255, 255, 0.12);
      color: #e2e8f0;
      padding: 0.65rem 1.25rem;
      border-radius: 8px;
      font-size: 0.875rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s ease;
    }
    .btn-secondary:hover:not(:disabled) {
      background: rgba(255, 255, 255, 0.12);
      color: #ffffff;
    }
    .btn-secondary:disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }

    .empty-state {
      text-align: center;
      padding: 3rem !important;
    }
    .empty-icon {
      font-size: 2.5rem;
      margin-bottom: 0.5rem;
    }
    .empty-title {
      font-weight: 700;
      font-size: 1.125rem;
      color: #cbd5e1;
    }
    .empty-sub {
      font-size: 0.8125rem;
      color: #64748b;
      margin-top: 0.25rem;
    }
  `]
})
export class UserListComponent implements OnInit {
  private userService = inject(UserService);
  readonly authService = inject(AuthService);

  // Active Tab: 'directory' | 'matrix'
  activeTab = signal<'directory' | 'matrix'>('directory');

  // State Signals
  users = signal<User[]>([]);
  roles = signal<RoleDto[]>([]);
  permissions = signal<PermissionDto[]>([]);

  // Filtering Signals
  searchTerm = signal<string>('');
  selectedRoleFilter = signal<string>('ALL');
  selectedStatusFilter = signal<string>('ALL');
  expandedUserId = signal<string | null>(null);

  // Matrix State: Map of roleCode -> Set of permissionCodes
  rolePermissionsMap = signal<Map<string, Set<string>>>(new Map());
  initialRolePermissionsMap = signal<Map<string, Set<string>>>(new Map());
  dirtyRoles = signal<Set<string>>(new Set());
  isSavingMatrix = signal<boolean>(false);

  // Modal State
  showCreateModal = signal<boolean>(false);
  isSubmittingUser = signal<boolean>(false);
  alertMessage = signal<string | null>(null);

  newUser: CreateUserRequest = {
    username: '',
    email: '',
    phoneNumber: '',
    firstName: '',
    lastName: '',
    password: '',
    roleCode: 'TENANT'
  };

  // Grouped Permissions for 2D Matrix view
  readonly groupedPermissions = computed(() => {
    const perms = this.permissions();
    const groups: { category: string; items: PermissionDto[] }[] = [
      {
        category: 'Core Operations & Dashboards',
        items: perms.filter(p => ['SCREEN:DASHBOARD', 'SCREEN:BUILDING_MATRIX'].includes(p.code))
      },
      {
        category: 'Identity, Users & Governance',
        items: perms.filter(p => ['SCREEN:USER_DIRECTORY', 'SCREEN:USER_CREATE', 'SCREEN:ROLE_MANAGEMENT', 'SCREEN:COMMUNITY_REGISTER'].includes(p.code))
      },
      {
        category: 'Security, Facilities & Work Orders',
        items: perms.filter(p => ['SCREEN:GATE_SECURITY', 'SCREEN:WORK_ORDERS'].includes(p.code))
      }
    ];
    return groups;
  });

  // Filtered Users Computed
  readonly filteredUsers = computed(() => {
    const list = this.users();
    const query = this.searchTerm().trim().toLowerCase();
    const roleFilter = this.selectedRoleFilter();
    const statusFilter = this.selectedStatusFilter();

    return list.filter(u => {
      // Search Match
      const matchesSearch = !query ||
        u.firstName?.toLowerCase().includes(query) ||
        u.lastName?.toLowerCase().includes(query) ||
        u.username?.toLowerCase().includes(query) ||
        u.email?.toLowerCase().includes(query);

      // Role Match
      const primary = u.primaryRole || (u.roles && u.roles[0]);
      const matchesRole = roleFilter === 'ALL' || primary === roleFilter || (u.roles && u.roles.includes(roleFilter));

      // Status Match
      const matchesStatus = statusFilter === 'ALL' ||
        (statusFilter === 'ACTIVE' && u.active) ||
        (statusFilter === 'INACTIVE' && !u.active);

      return matchesSearch && matchesRole && matchesStatus;
    });
  });

  readonly isMatrixDirty = computed(() => this.dirtyRoles().size > 0);

  ngOnInit(): void {
    this.loadUsers();
    this.loadRolesAndPermissions();
  }

  setActiveTab(tab: 'directory' | 'matrix'): void {
    this.activeTab.set(tab);
  }

  loadUsers(): void {
    this.userService.getUsers(0, 100).subscribe({
      next: res => {
        if (res.success && res.data) {
          this.users.set(res.data.content);
        }
      },
      error: err => console.error('Failed to load users:', err)
    });
  }

  loadRolesAndPermissions(): void {
    this.userService.getPermissions().subscribe({
      next: pRes => {
        if (pRes.success) {
          this.permissions.set(pRes.data);
        }
      }
    });

    this.userService.getRoles().subscribe({
      next: rRes => {
        if (rRes.success) {
          this.roles.set(rRes.data);

          const map = new Map<string, Set<string>>();
          const initialMap = new Map<string, Set<string>>();

          rRes.data.forEach(role => {
            map.set(role.code, new Set(role.permissions || []));
            initialMap.set(role.code, new Set(role.permissions || []));
          });

          this.rolePermissionsMap.set(map);
          this.initialRolePermissionsMap.set(initialMap);
          this.dirtyRoles.set(new Set());
        }
      }
    });
  }

  hasMatrixPermission(roleCode: string, permCode: string): boolean {
    const set = this.rolePermissionsMap().get(roleCode);
    return set ? set.has(permCode) : false;
  }

  toggleMatrixPermission(roleCode: string, permCode: string): void {
    if (roleCode === 'SUPER_ADMIN') return; // Cannot edit super admin

    const currentMap = this.rolePermissionsMap();
    const rolePerms = new Set(currentMap.get(roleCode) || []);

    if (rolePerms.has(permCode)) {
      rolePerms.delete(permCode);
    } else {
      rolePerms.add(permCode);
    }

    const newMap = new Map(currentMap);
    newMap.set(roleCode, rolePerms);
    this.rolePermissionsMap.set(newMap);

    // Track dirty roles
    const dirty = new Set(this.dirtyRoles());
    dirty.add(roleCode);
    this.dirtyRoles.set(dirty);
  }

  resetMatrixChanges(): void {
    const resetMap = new Map<string, Set<string>>();
    this.initialRolePermissionsMap().forEach((set, key) => {
      resetMap.set(key, new Set(set));
    });
    this.rolePermissionsMap.set(resetMap);
    this.dirtyRoles.set(new Set());
  }

  savePermissionMatrix(): void {
    const dirty = Array.from(this.dirtyRoles());
    if (dirty.length === 0) return;

    this.isSavingMatrix.set(true);
    let completedCount = 0;

    dirty.forEach(roleCode => {
      const perms = Array.from(this.rolePermissionsMap().get(roleCode) || []);
      this.userService.updateRolePermissions(roleCode, perms).subscribe({
        next: () => {
          completedCount++;
          if (completedCount === dirty.length) {
            this.isSavingMatrix.set(false);
            this.dirtyRoles.set(new Set());
            this.showAlert('Screen permission matrix saved and updated successfully across all roles!');
            this.loadRolesAndPermissions();
            this.loadUsers();
          }
        },
        error: err => {
          console.error(`Failed to update permissions for role ${roleCode}:`, err);
          this.isSavingMatrix.set(false);
        }
      });
    });
  }

  toggleUserStatus(user: User): void {
    const nextStatus = !user.active;
    this.userService.toggleStatus(user.id, nextStatus).subscribe({
      next: () => {
        user.active = nextStatus;
        this.users.set([...this.users()]);
        this.showAlert(`User ${user.username} has been ${nextStatus ? 'activated' : 'deactivated'}.`);
      },
      error: err => console.error('Failed to update status:', err)
    });
  }

  openCreateUserModal(): void {
    this.newUser = {
      username: '',
      email: '',
      phoneNumber: '',
      firstName: '',
      lastName: '',
      password: '',
      roleCode: 'TENANT'
    };
    this.showCreateModal.set(true);
  }

  closeCreateUserModal(): void {
    this.showCreateModal.set(false);
  }

  submitCreateUser(): void {
    if (!this.newUser.username || !this.newUser.email || !this.newUser.firstName || !this.newUser.lastName || !this.newUser.password) {
      alert('Please fill out all mandatory fields.');
      return;
    }

    this.isSubmittingUser.set(true);
    this.userService.createUser(this.newUser).subscribe({
      next: res => {
        this.isSubmittingUser.set(false);
        this.showCreateModal.set(false);
        this.showAlert(`Successfully provisioned user @${res.data.username} with role ${res.data.primaryRole}!`);
        this.loadUsers();
      },
      error: err => {
        this.isSubmittingUser.set(false);
        const msg = err.error?.message || 'Failed to create user.';
        alert(msg);
      }
    });
  }

  togglePermissionsPopover(userId: string): void {
    if (this.expandedUserId() === userId) {
      this.expandedUserId.set(null);
    } else {
      this.expandedUserId.set(userId);
    }
  }

  getUserPermissionsList(user: User): string[] {
    if (user.permissions && user.permissions.length > 0) {
      return user.permissions;
    }
    const roleCode = user.primaryRole || (user.roles && user.roles[0]);
    if (!roleCode) return [];
    const set = this.rolePermissionsMap().get(roleCode);
    return set ? Array.from(set) : [];
  }

  getRolePermCount(user: User): number {
    return this.getUserPermissionsList(user).length;
  }

  getInitials(user: User): string {
    const f = user.firstName ? user.firstName[0].toUpperCase() : '';
    const l = user.lastName ? user.lastName[0].toUpperCase() : '';
    return f + l || 'U';
  }

  getAvatarGradient(name: string): string {
    const gradients = [
      'linear-gradient(135deg, #6366f1 0%, #a855f7 100%)',
      'linear-gradient(135deg, #3b82f6 0%, #2dd4bf 100%)',
      'linear-gradient(135deg, #ec4899 0%, #f43f5e 100%)',
      'linear-gradient(135deg, #f59e0b 0%, #ef4444 100%)',
      'linear-gradient(135deg, #10b981 0%, #059669 100%)'
    ];
    const code = (name || 'A').charCodeAt(0);
    return gradients[code % gradients.length];
  }

  formatRoleName(roleCode: string | undefined): string {
    if (!roleCode) return 'Resident';
    return roleCode.replace(/_/g, ' ');
  }

  getRoleBadgeClass(roleCode: string | undefined): string {
    if (!roleCode) return 'role-tenant';
    return 'role-' + roleCode.toLowerCase().replace(/_/g, '-');
  }

  showAlert(msg: string): void {
    this.alertMessage.set(msg);
    setTimeout(() => {
      if (this.alertMessage() === msg) {
        this.alertMessage.set(null);
      }
    }, 5000);
  }

  clearAlert(): void {
    this.alertMessage.set(null);
  }
}
