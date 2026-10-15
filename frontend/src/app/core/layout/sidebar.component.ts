import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'scos-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <aside class="sidebar glass-panel">
      <nav class="nav-menu">
        <div class="nav-section-title">CORE OPERATIONS</div>

        <a *ngIf="authService.hasPermission('SCREEN:DASHBOARD')"
           routerLink="/dashboard"
           routerLinkActive="active"
           class="nav-item">
          <span class="icon">📊</span>
          <span class="label">Executive Dashboard</span>
        </a>

        <a *ngIf="authService.hasPermission('SCREEN:BUILDING_MATRIX')"
           routerLink="/infrastructure/matrix"
           routerLinkActive="active"
           class="nav-item">
          <span class="icon">🏢</span>
          <span class="label">Building Floor Matrix</span>
        </a>

        <div class="nav-section-title" *ngIf="authService.hasAnyPermission('SCREEN:WORK_ORDERS', 'SCREEN:FACILITY_BOOKINGS')">
          COMMUNITY OPERATIONS
        </div>

        <a *ngIf="authService.hasPermission('SCREEN:WORK_ORDERS')"
           routerLink="/work-orders"
           routerLinkActive="active"
           class="nav-item">
          <span class="icon">🛠️</span>
          <span class="label">Complaints & Maintenance</span>
        </a>

        <a *ngIf="authService.hasPermission('SCREEN:WORK_ORDERS')"
           routerLink="/assets"
           routerLinkActive="active"
           class="nav-item">
          <span class="icon">⚙️</span>
          <span class="label">Asset Register & PM</span>
        </a>

        <a *ngIf="authService.hasPermission('SCREEN:FACILITY_BOOKINGS')"
           routerLink="/facilities"
           routerLinkActive="active"
           class="nav-item">
          <span class="icon">🏊‍♂️</span>
          <span class="label">Amenities & Bookings</span>
        </a>

        <div class="nav-section-title" *ngIf="authService.hasAnyPermission('SCREEN:USER_DIRECTORY', 'SCREEN:ROLE_MANAGEMENT')">
          IDENTITY & ACCESS
        </div>

        <a *ngIf="authService.hasPermission('SCREEN:USER_DIRECTORY')"
           routerLink="/users"
           routerLinkActive="active"
           class="nav-item">
          <span class="icon">👥</span>
          <span class="label">Users & Permissions</span>
        </a>

        <div class="nav-section-title" *ngIf="authService.hasPermission('SCREEN:COMMUNITY_REGISTER')">
          ONBOARDING
        </div>

        <a *ngIf="authService.hasPermission('SCREEN:COMMUNITY_REGISTER')"
           routerLink="/communities/register"
           routerLinkActive="active"
           class="nav-item">
          <span class="icon">🏛️</span>
          <span class="label">Register New Society</span>
        </a>
      </nav>

      <div class="sidebar-footer">
        <div class="role-badge-pill">
          <span class="dot"></span>
          <span class="role-text">{{ authService.currentUser()?.primaryRole || 'USER' }}</span>
        </div>
      </div>
    </aside>
  `,
  styles: [`
    .sidebar {
      width: 260px;
      height: calc(100vh - 70px);
      padding: 1.5rem 1rem;
      border-radius: 0;
      border-top: none;
      border-bottom: none;
      border-left: none;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
    }
    .nav-section-title {
      font-size: 0.6875rem;
      font-weight: 700;
      letter-spacing: 0.08em;
      color: #64748b;
      margin: 1.25rem 0.75rem 0.5rem;
    }
    .nav-item {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.75rem 0.875rem;
      border-radius: 8px;
      color: #94a3b8;
      text-decoration: none;
      font-size: 0.875rem;
      font-weight: 500;
      transition: all 0.2s ease;
      margin-bottom: 0.25rem;
    }
    .nav-item:hover {
      background: rgba(255, 255, 255, 0.05);
      color: #ffffff;
    }
    .nav-item.active {
      background: linear-gradient(135deg, rgba(99, 102, 241, 0.2) 0%, rgba(168, 85, 247, 0.2) 100%);
      color: #818cf8;
      border-left: 3px solid #6366f1;
      font-weight: 600;
    }
    .nav-item .icon {
      font-size: 1.125rem;
    }
    .sidebar-footer {
      padding-top: 1rem;
      border-top: 1px solid rgba(255, 255, 255, 0.05);
    }
    .role-badge-pill {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.5rem 0.75rem;
      background: rgba(99, 102, 241, 0.1);
      border: 1px solid rgba(99, 102, 241, 0.25);
      border-radius: 20px;
      font-size: 0.75rem;
      color: #a5b4fc;
      font-weight: 600;
    }
    .role-badge-pill .dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: #10b981;
      box-shadow: 0 0 8px #10b981;
    }
  `]
})
export class SidebarComponent {
  readonly authService = inject(AuthService);
}
