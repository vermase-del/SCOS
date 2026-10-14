import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../auth/auth.service';
import { CommunityService } from '../community/community.service';

@Component({
  selector: 'scos-header',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <header class="header glass-panel">
      <div class="brand">
        <div class="logo-icon">🏢</div>
        <div class="brand-text">
          <span class="brand-title">SCOS</span>
          <div class="community-selector" *ngIf="communityService.communities().length > 1; else singleComm">
            <select class="comm-select" [ngModel]="communityService.activeCommunity()?.id" (ngModelChange)="onCommunityChange($event)">
              <option *ngFor="let c of communityService.communities()" [value]="c.id">
                📍 {{ c.name }}
              </option>
            </select>
          </div>
          <ng-template #singleComm>
            <span class="community-name">{{ communityService.activeCommunity()?.name || 'Emerald Grand Heights' }}</span>
          </ng-template>
        </div>
      </div>

      <div class="user-menu" *ngIf="authService.currentUser() as user">
        <div class="user-avatar">{{ user.firstName[0] }}{{ user.lastName[0] }}</div>
        <div class="user-info">
          <span class="user-name">{{ user.firstName }} {{ user.lastName }}</span>
          <span class="user-role">{{ user.roles[0] || 'Resident' }}</span>
        </div>
        <button class="btn-logout" (click)="authService.logout()" title="Logout">
          Exit
        </button>
      </div>
    </header>
  `,
  styles: [`
    .header {
      height: 70px;
      padding: 0 1.5rem;
      display: flex;
      align-items: center;
      justify-content: space-between;
      border-radius: 0;
      border-left: none;
      border-right: none;
      border-top: none;
      position: sticky;
      top: 0;
      z-index: 50;
    }
    .brand {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }
    .logo-icon {
      font-size: 1.75rem;
    }
    .brand-title {
      font-weight: 800;
      font-size: 1.25rem;
      letter-spacing: -0.02em;
      color: #818cf8;
      display: block;
    }
    .community-name {
      font-size: 0.8125rem;
      color: #94a3b8;
      font-weight: 500;
    }
    .comm-select {
      background: rgba(30, 41, 59, 0.7);
      border: 1px solid rgba(99, 102, 241, 0.35);
      color: #e2e8f0;
      padding: 0.25rem 0.5rem;
      border-radius: 6px;
      font-size: 0.8125rem;
      font-weight: 600;
      outline: none;
      cursor: pointer;
    }
    .comm-select:focus {
      border-color: #818cf8;
    }
    .comm-select option {
      background: #0f172a;
      color: #ffffff;
    }
    .user-menu {
      display: flex;
      align-items: center;
      gap: 0.875rem;
    }
    .user-avatar {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      background: linear-gradient(135deg, #6366f1, #a855f7);
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 700;
      font-size: 0.875rem;
    }
    .user-info {
      display: flex;
      flex-direction: column;
    }
    .user-name {
      font-weight: 600;
      font-size: 0.875rem;
    }
    .user-role {
      font-size: 0.75rem;
      color: #818cf8;
    }
    .btn-logout {
      background: rgba(239, 68, 68, 0.15);
      color: #f87171;
      border: 1px solid rgba(239, 68, 68, 0.3);
      padding: 0.35rem 0.85rem;
      font-size: 0.75rem;
      font-weight: 600;
      border-radius: 6px;
      cursor: pointer;
      transition: all 0.2s ease;
    }
    .btn-logout:hover {
      background: rgba(239, 68, 68, 0.3);
    }
  `]
})
export class HeaderComponent implements OnInit {
  authService = inject(AuthService);
  communityService = inject(CommunityService);

  ngOnInit(): void {
    this.communityService.loadCommunities().subscribe();
  }

  onCommunityChange(communityId: string): void {
    const found = this.communityService.communities().find(c => c.id === communityId);
    if (found) {
      this.communityService.setActiveCommunity(found);
      window.location.reload();
    }
  }
}
