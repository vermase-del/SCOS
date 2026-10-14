import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'scos-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="dashboard-container">
      <div class="page-header">
        <div>
          <h1 class="page-title">Executive Operations Dashboard</h1>
          <p class="page-subtitle">Real-time status overview for Emerald Grand Heights</p>
        </div>
        <div class="header-actions">
          <a routerLink="/infrastructure/matrix" class="btn-primary">
            <span>🏢</span> Open Tower Matrix
          </a>
        </div>
      </div>

      <!-- KPI Metric Cards Grid -->
      <div class="kpi-grid">
        <div class="kpi-card glass-panel">
          <div class="kpi-header">
            <span class="kpi-title">TOTAL UNITS</span>
            <span class="kpi-icon">🚪</span>
          </div>
          <div class="kpi-value">48</div>
          <div class="kpi-meta text-emerald">Tower A (12 Floors)</div>
        </div>

        <div class="kpi-card glass-panel">
          <div class="kpi-header">
            <span class="kpi-title">OCCUPANCY RATE</span>
            <span class="kpi-icon">📊</span>
          </div>
          <div class="kpi-value">75%</div>
          <div class="kpi-meta">36 Occupied / 12 Vacant</div>
        </div>

        <div class="kpi-card glass-panel">
          <div class="kpi-header">
            <span class="kpi-title">MAINTENANCE TICKETS</span>
            <span class="kpi-icon">🔧</span>
          </div>
          <div class="kpi-value">4</div>
          <div class="kpi-meta text-amber">2 In Progress / 2 Pending</div>
        </div>

        <div class="kpi-card glass-panel">
          <div class="kpi-header">
            <span class="kpi-title">COLLECTION PERFORMANCE</span>
            <span class="kpi-icon">💳</span>
          </div>
          <div class="kpi-value">92.4%</div>
          <div class="kpi-meta text-emerald">₹ 14.8L / ₹ 16.0L Collected</div>
        </div>
      </div>

      <!-- Quick Action Modules -->
      <div class="actions-grid">
        <div class="action-card glass-panel">
          <h3>🏢 Interactive Building Matrix</h3>
          <p>Inspect visual apartment grids floor-by-floor with real-time occupancy status indicators.</p>
          <a routerLink="/infrastructure/matrix" class="btn-secondary">Launch Matrix Visualizer →</a>
        </div>

        <div class="action-card glass-panel">
          <h3>👥 User & Resident Directory</h3>
          <p>Manage community owners, primary tenants, family members, gate guards, and maintenance staff.</p>
          <a routerLink="/users" class="btn-secondary">Manage Directory →</a>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .dashboard-container {
      display: flex;
      flex-direction: column;
      gap: 2rem;
    }
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .page-title {
      font-size: 1.75rem;
      font-weight: 800;
      letter-spacing: -0.02em;
    }
    .page-subtitle {
      color: #94a3b8;
      font-size: 0.875rem;
      margin-top: 0.25rem;
    }
    .kpi-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 1.25rem;
    }
    .kpi-card {
      padding: 1.5rem;
      display: flex;
      flex-direction: column;
    }
    .kpi-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 0.5rem;
    }
    .kpi-title {
      font-size: 0.75rem;
      font-weight: 700;
      letter-spacing: 0.05em;
      color: #94a3b8;
    }
    .kpi-icon {
      font-size: 1.25rem;
    }
    .kpi-value {
      font-size: 2.25rem;
      font-weight: 800;
      color: #ffffff;
    }
    .kpi-meta {
      font-size: 0.75rem;
      color: #64748b;
      margin-top: 0.25rem;
    }
    .text-emerald { color: #34d399; }
    .text-amber { color: #fbbf24; }
    .actions-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(360px, 1fr));
      gap: 1.5rem;
    }
    .action-card {
      padding: 2rem;
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }
    .action-card h3 {
      font-size: 1.25rem;
      font-weight: 700;
    }
    .action-card p {
      color: #94a3b8;
      font-size: 0.875rem;
      line-height: 1.5;
      margin-bottom: 0.5rem;
    }
  `]
})
export class DashboardComponent {}
