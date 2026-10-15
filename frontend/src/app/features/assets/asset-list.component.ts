import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { WorkOrderService } from '../work-orders/work-order.service';
import {
  Asset,
  AssetCriticality,
  AssetStatus,
  AssetStatusHistory,
  AssetSummary,
  CreateAssetRequest
} from '../work-orders/work-order.model';

@Component({
  selector: 'scos-asset-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="assets-console">
      <header class="page-header">
        <div>
          <p class="eyebrow">ESTATE INFRASTRUCTURE & CAPITAL ASSETS</p>
          <h1>Asset Register & Preventive Maintenance</h1>
          <p class="muted">Monitor equipment lifecycle, warranty status, maintenance schedules, and out-of-service alerts.</p>
        </div>
        <div class="header-actions">
          <button class="primary" (click)="openCreateModal()">＋ Register asset</button>
        </div>
      </header>

      <p class="error" *ngIf="error">{{ error }}</p>

      <div class="kpis" *ngIf="summary as s">
        <article><span>Total assets</span><strong>{{ s.totalAssets }}</strong></article>
        <article class="success"><span>Operational</span><strong>{{ s.operationalCount }}</strong></article>
        <article class="warning"><span>Under maintenance</span><strong>{{ s.underMaintenanceCount }}</strong></article>
        <article class="danger"><span>Out of service</span><strong>{{ s.outOfServiceCount }}</strong></article>
        <article class="critical"><span>Critical assets</span><strong>{{ s.criticalCount }}</strong></article>
        <article class="alert"><span>Due for PM (14d)</span><strong>{{ s.upcomingMaintenanceCount }}</strong></article>
      </div>

      <div class="toolbar">
        <input class="field search" [(ngModel)]="search" placeholder="Search code, equipment name, manufacturer, model or location" aria-label="Search assets">
        <select class="field" [(ngModel)]="statusFilter" (change)="loadAssets()">
          <option value="">All statuses</option>
          <option *ngFor="let s of statuses" [value]="s">{{ label(s) }}</option>
        </select>
        <select class="field" [(ngModel)]="criticalityFilter" (change)="loadAssets()">
          <option value="">All criticalities</option>
          <option *ngFor="let c of criticalities" [value]="c">{{ c }}</option>
        </select>
        <button class="secondary" [class.active-pill]="showDueOnly" (click)="toggleDueFilter()">
          📅 {{ showDueOnly ? 'Showing PM Due' : 'Filter PM Due' }}
        </button>
        <button class="secondary" (click)="refresh()">↻ Refresh</button>
      </div>

      <div class="loading" *ngIf="loading">Loading asset inventory…</div>

      <div class="table-wrap" *ngIf="!loading">
        <table>
          <thead>
            <tr>
              <th>Asset Code</th>
              <th>Equipment Name & Specs</th>
              <th>Type</th>
              <th>Location</th>
              <th>Criticality</th>
              <th>Status</th>
              <th>Next Maintenance</th>
              <th>Warranty</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let asset of filteredAssets()" (click)="selectAsset(asset)" class="clickable-row">
              <td class="asset-code"><strong>{{ asset.assetCode }}</strong></td>
              <td>
                <div class="asset-title-block">
                  <span class="asset-name">{{ asset.name }}</span>
                  <small class="muted" *ngIf="asset.manufacturer || asset.modelNumber">
                    {{ asset.manufacturer }} {{ asset.modelNumber ? '· ' + asset.modelNumber : '' }}
                  </small>
                </div>
              </td>
              <td><span class="type-pill">{{ asset.assetType }}</span></td>
              <td><small>{{ asset.locationDetails || 'Common Area' }}</small></td>
              <td><span class="criticality" [ngClass]="asset.criticality.toLowerCase()">{{ asset.criticality }}</span></td>
              <td>
                <span class="status-badge" [ngClass]="asset.status.toLowerCase()">
                  {{ label(asset.status) }}
                </span>
              </td>
              <td>
                <span [class.due-alert]="isMaintenanceDue(asset)">
                  {{ asset.nextMaintenanceAt ? (asset.nextMaintenanceAt | date:'mediumDate') : 'Not scheduled' }}
                </span>
                <small class="due-tag" *ngIf="isMaintenanceDue(asset)">Due soon</small>
              </td>
              <td>
                <small *ngIf="asset.warrantyExpiresOn">{{ asset.warrantyExpiresOn | date:'mediumDate' }}</small>
                <small class="muted" *ngIf="!asset.warrantyExpiresOn">—</small>
              </td>
              <td>
                <button class="secondary" (click)="$event.stopPropagation(); selectAsset(asset)">Manage</button>
              </td>
            </tr>
          </tbody>
        </table>
        <p class="empty" *ngIf="!filteredAssets().length">No assets match your search or filter criteria.</p>
      </div>
    </section>

    <!-- Register Asset Modal Dialog -->
    <div class="overlay" *ngIf="createOpen" (click)="createOpen=false">
      <section class="dialog" role="dialog" aria-modal="true" aria-labelledby="asset-modal-title" (click)="$event.stopPropagation()">
        <header>
          <div>
            <p class="eyebrow">NEW EQUIPMENT</p>
            <h2 id="asset-modal-title">Register Estate Asset</h2>
          </div>
          <button class="icon-btn" (click)="createOpen=false" aria-label="Close">×</button>
        </header>

        <form (ngSubmit)="createAsset()">
          <div class="form-grid">
            <label>Asset code *
              <input class="field" name="assetCode" [(ngModel)]="draft.assetCode" required maxlength="40" placeholder="E.g. PUMP-002, LIFT-003, GEN-002">
            </label>
            <label>Asset classification *
              <select class="field" name="assetType" [(ngModel)]="draft.assetType" required>
                <option value="PLUMBING">PLUMBING</option>
                <option value="ELECTRICAL">ELECTRICAL</option>
                <option value="HVAC">HVAC</option>
                <option value="ELEVATOR">ELEVATOR</option>
                <option value="SECURITY_DEVICE">SECURITY_DEVICE</option>
                <option value="CIVIL">CIVIL</option>
                <option value="OTHER">OTHER</option>
              </select>
            </label>
          </div>

          <label>Equipment name *
            <input class="field" name="name" [(ngModel)]="draft.name" required maxlength="160" placeholder="E.g. Secondary Hydro-pneumatic Booster Pump 15HP">
          </label>

          <label>Description / Technical remarks
            <textarea class="field" name="description" [(ngModel)]="draft.description" rows="2" placeholder="Operating parameters, rated capacity, maintenance manual notes"></textarea>
          </label>

          <div class="form-grid">
            <label>Criticality
              <select class="field" name="criticality" [(ngModel)]="draft.criticality">
                <option *ngFor="let c of criticalities" [value]="c">{{ c }}</option>
              </select>
            </label>
            <label>Location details
              <input class="field" name="locationDetails" [(ngModel)]="draft.locationDetails" maxlength="200" placeholder="Tower B Basement, Substation, Rooftop">
            </label>
            <label>Manufacturer
              <input class="field" name="manufacturer" [(ngModel)]="draft.manufacturer" maxlength="100" placeholder="E.g. Grundfos, Otis, Cummins">
            </label>
            <label>Model number
              <input class="field" name="modelNumber" [(ngModel)]="draft.modelNumber" maxlength="100" placeholder="E.g. CR-15-04">
            </label>
            <label>Serial number
              <input class="field" name="serialNumber" [(ngModel)]="draft.serialNumber" maxlength="120" placeholder="Factory serial barcode">
            </label>
            <label>Installation date
              <input class="field" type="date" name="installedOn" [(ngModel)]="draft.installedOn">
            </label>
            <label>Warranty expiration date
              <input class="field" type="date" name="warrantyExpiresOn" [(ngModel)]="draft.warrantyExpiresOn">
            </label>
            <label>Next scheduled maintenance
              <input class="field" type="datetime-local" name="nextMaintenanceAt" [(ngModel)]="nextMaintenanceStr">
            </label>
          </div>

          <p class="error" *ngIf="formError">{{ formError }}</p>
          <footer>
            <button type="button" class="secondary" (click)="createOpen=false">Cancel</button>
            <button type="submit" class="primary" [disabled]="saving">{{ saving ? 'Registering…' : 'Register Asset' }}</button>
          </footer>
        </form>
      </section>
    </div>

    <!-- Asset Workbench Slide-Over Drawer -->
    <div class="overlay" *ngIf="selected" (click)="selected=null">
      <aside class="drawer" role="dialog" aria-modal="true" (click)="$event.stopPropagation()">
        <header>
          <div>
            <div class="drawer-eyebrow">
              <span class="asset-code">{{ selected.assetCode }}</span>
              <span class="criticality" [ngClass]="selected.criticality.toLowerCase()">{{ selected.criticality }}</span>
              <span class="status-badge" [ngClass]="selected.status.toLowerCase()">{{ label(selected.status) }}</span>
            </div>
            <h2>{{ selected.name }}</h2>
            <p class="muted">{{ selected.assetType }} · {{ selected.manufacturer }} {{ selected.modelNumber }}</p>
          </div>
          <button class="icon-btn" (click)="selected=null" aria-label="Close">×</button>
        </header>

        <div class="drawer-body">
          <!-- Quick Action Bar -->
          <section class="action-card">
            <h3>Preventive Maintenance & Lifecycle Actions</h3>
            <div class="btn-cluster">
              <button class="primary" (click)="schedulePreventiveWorkOrder(selected)">
                ⚙️ File Preventive Work Order
              </button>
            </div>

            <div class="status-transition-box">
              <h4>Update Operational Status</h4>
              <div class="status-form">
                <select class="field" [(ngModel)]="targetStatus">
                  <option *ngFor="let s of statuses" [value]="s">{{ label(s) }}</option>
                </select>
                <input class="field" [(ngModel)]="statusReason" placeholder="Status change reason / maintenance note">
                <button class="secondary" (click)="updateStatus()" [disabled]="targetStatus === selected.status">Update Status</button>
              </div>
            </div>
          </section>

          <!-- Specifications Card -->
          <section class="detail-card">
            <h3>Technical Specifications</h3>
            <p *ngIf="selected.description">{{ selected.description }}</p>
            <dl>
              <dt>Location</dt>
              <dd>{{ selected.locationDetails || 'Common Area' }}</dd>
              <dt>Manufacturer / Brand</dt>
              <dd>{{ selected.manufacturer || '—' }}</dd>
              <dt>Model Number</dt>
              <dd>{{ selected.modelNumber || '—' }}</dd>
              <dt>Serial Number</dt>
              <dd>{{ selected.serialNumber || '—' }}</dd>
              <dt>Installed Date</dt>
              <dd>{{ selected.installedOn ? (selected.installedOn | date:'mediumDate') : '—' }}</dd>
              <dt>Warranty Expiry</dt>
              <dd>{{ selected.warrantyExpiresOn ? (selected.warrantyExpiresOn | date:'mediumDate') : '—' }}</dd>
              <dt>Last Maintenance</dt>
              <dd>{{ selected.lastMaintenanceAt ? (selected.lastMaintenanceAt | date:'medium') : 'None recorded' }}</dd>
              <dt>Next PM Target</dt>
              <dd [class.due-alert]="isMaintenanceDue(selected)">
                {{ selected.nextMaintenanceAt ? (selected.nextMaintenanceAt | date:'medium') : 'Not scheduled' }}
              </dd>
            </dl>
          </section>

          <!-- Append-Only Status History Card -->
          <section class="detail-card">
            <h3>Append-Only Status History Log</h3>
            <div class="history-item" *ngFor="let h of statusHistory">
              <span class="history-dot"></span>
              <div class="history-content">
                <div class="history-top">
                  <strong>{{ h.fromStatus ? label(h.fromStatus) + ' → ' + label(h.toStatus) : label(h.toStatus) }}</strong>
                  <small>{{ h.changedAt | date:'medium' }}</small>
                </div>
                <p>{{ h.reason || 'Operational transition' }}</p>
              </div>
            </div>
            <p class="empty" *ngIf="!statusHistory.length">No status history recorded yet.</p>
          </section>
        </div>
      </aside>
    </div>
  `,
  styles: [`
    :host{display:block;color:var(--text-main)}.assets-console{max-width:1600px;margin:auto}.page-header{display:flex;justify-content:space-between;align-items:center;gap:1rem;margin-bottom:1.5rem}.page-header h1{font-size:1.7rem;margin:.3rem 0}.eyebrow{font-size:.68rem;letter-spacing:.13em;color:#818cf8;font-weight:800}.muted{color:var(--text-muted);font-size:.85rem}.primary,.secondary{border-radius:9px;padding:.65rem .9rem;font-weight:650;cursor:pointer;border:1px solid transparent}.primary{background:var(--primary-600);color:white}.primary:disabled{opacity:.55;cursor:wait}.secondary{background:var(--surface-card);border-color:var(--surface-border);color:var(--text-main)}.active-pill{background:#1e3a8a!important;border-color:#3b82f6!important;color:#bfdbfe!important}.kpis{display:grid;grid-template-columns:repeat(6,minmax(0,1fr));gap:.8rem;margin-bottom:1.25rem}.kpis article,.detail-card,.action-card{background:var(--surface-card);border:1px solid var(--surface-border);border-radius:13px;padding:1rem}.kpis span{display:block;color:var(--text-muted);font-size:.78rem}.kpis strong{display:block;font-size:1.7rem;margin-top:.45rem}.kpis.success strong{color:#4ade80}.kpis.warning strong{color:#facc15}.kpis .danger strong{color:#fb7185}.kpis.critical strong{color:#c084fc}.kpis .alert strong{color:#38bdf8}.toolbar{display:flex;gap:.65rem;align-items:center;margin-bottom:1rem;flex-wrap:wrap}.field{background:#0b1120;border:1px solid var(--surface-border);border-radius:8px;padding:.68rem .75rem;color:var(--text-main);min-width:0;width:100%}.toolbar .field{width:auto;max-width:190px}.toolbar .search{flex:1;max-width:none}.table-wrap{overflow:auto;border:1px solid var(--surface-border);border-radius:12px}table{width:100%;border-collapse:collapse;min-width:980px;background:var(--surface-card)}th,td{text-align:left;padding:.85rem;border-bottom:1px solid var(--surface-border);font-size:.8rem}th{color:var(--text-muted);font-size:.7rem;text-transform:uppercase;letter-spacing:.06em}.clickable-row{cursor:pointer;transition:background .15s}.clickable-row:hover{background:rgba(30,41,59,.6)}.asset-code{font-size:.78rem;color:#a5b4fc;font-weight:800}.asset-title-block{display:grid;gap:.2rem}.asset-name{font-weight:600}.type-pill{font-size:.7rem;background:#1e293b;border-radius:5px;padding:.2rem .4rem;color:#cbd5e1}.criticality{font-size:.68rem;font-weight:800;border-radius:5px;padding:.25rem .4rem;background:#334155;color:#e2e8f0}.criticality.critical{background:#7f1d1d;color:#fecaca}.criticality.high{background:#78350f;color:#fde68a}.criticality.medium{background:#1e3a8a;color:#bfdbfe}.criticality.low{background:#14532d;color:#bbf7d0}.status-badge{font-size:.7rem;font-weight:700;border-radius:5px;padding:.25rem .45rem}.status-badge.operational{background:#065f46;color:#a7f3d0}.status-badge.under_maintenance{background:#854d0e;color:#fef08a}.status-badge.out_of_service{background:#991b1b;color:#fecaca}.status-badge.retired{background:#334155;color:#94a3b8}.due-alert{color:#f87171;font-weight:700}.due-tag{display:inline-block;background:#7f1d1d;color:#fecaca;padding:.1rem .3rem;border-radius:3px;font-size:.65rem;margin-left:.3rem}.loading{padding:2rem;color:var(--text-muted)}.error{color:#fda4af;margin:.5rem 0 1rem}.empty{font-size:.8rem;color:var(--text-muted);padding:1.5rem;text-align:center}.overlay{position:fixed;inset:0;z-index:1000;background:rgba(2,6,23,.72);display:flex;justify-content:center;align-items:center;padding:1rem}.dialog,.drawer{background:#101a2d;border:1px solid var(--surface-border);box-shadow:var(--shadow-lg);color:var(--text-main)}.dialog{width:min(720px,100%);max-height:90vh;overflow:auto;border-radius:16px;padding:1.25rem}.dialog header,.drawer>header{display:flex;justify-content:space-between;gap:1rem;align-items:flex-start;padding-bottom:1rem;border-bottom:1px solid var(--surface-border)}h2{font-size:1.25rem;margin:.35rem 0}.icon-btn{background:transparent;color:var(--text-main);border:0;font-size:1.7rem;cursor:pointer}.dialog form{display:grid;gap:1rem;padding-top:1rem}.dialog label,.detail-card label{display:grid;gap:.4rem;font-size:.8rem;color:var(--text-muted)}.form-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:.8rem}.dialog footer{display:flex;justify-content:flex-end;gap:.6rem;margin-top:.3rem}.drawer{width:min(680px,100%);height:100%;max-height:100%;margin-left:auto;border-radius:15px 0 0 15px;display:flex;flex-direction:column;padding:1.2rem}.drawer-eyebrow{display:flex;align-items:center;gap:.6rem}.drawer-body{overflow:auto;padding:1rem .2rem 2rem;display:grid;gap:.8rem}.detail-card h3,.action-card h3{font-size:.92rem;margin-bottom:.8rem}.detail-card dl{display:grid;grid-template-columns:1fr 1.3fr;gap:.55rem;margin-top:.8rem;font-size:.78rem}.detail-card dt{color:var(--text-muted)}.detail-card dd{overflow-wrap:anywhere}.action-card{background:#131d33}.btn-cluster{margin-bottom:.9rem}.status-transition-box h4{font-size:.8rem;margin-bottom:.4rem;color:var(--text-muted)}.status-form{display:grid;grid-template-columns:1fr 1.5fr auto;gap:.5rem;align-items:center}.history-item{display:flex;gap:.7rem;padding:.6rem 0;border-bottom:1px solid var(--surface-border)}.history-dot{width:8px;height:8px;border-radius:50%;background:#38bdf8;margin-top:.35rem;flex:none}.history-content{flex:1}.history-top{display:flex;justify-content:space-between;font-size:.8rem}.history-content p{margin:.25rem 0;font-size:.78rem;color:var(--text-muted)}.history-top small{color:var(--text-muted);font-size:.7rem}@media(max-width:1100px){.kpis{grid-template-columns:repeat(3,1fr)}}@media(max-width:700px){.page-header{flex-direction:column;align-items:flex-start}.kpis{grid-template-columns:repeat(2,1fr)}.form-grid,.status-form{grid-template-columns:1fr}.drawer{width:100%;border-radius:12px}}
  `]
})
export class AssetListComponent implements OnInit {
  private readonly api = inject(WorkOrderService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly statuses: AssetStatus[] = ['OPERATIONAL', 'UNDER_MAINTENANCE', 'OUT_OF_SERVICE', 'RETIRED'];
  readonly criticalities: AssetCriticality[] = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];

  communityId = this.auth.activeCommunityId() || '';
  assets: Asset[] = [];
  summary: AssetSummary | null = null;
  selected: Asset | null = null;
  statusHistory: AssetStatusHistory[] = [];

  loading = false;
  saving = false;
  error = '';
  formError = '';
  createOpen = false;
  search = '';
  statusFilter: AssetStatus | '' = '';
  criticalityFilter: AssetCriticality | '' = '';
  showDueOnly = false;

  targetStatus: AssetStatus = 'OPERATIONAL';
  statusReason = '';
  nextMaintenanceStr = '';

  draft: CreateAssetRequest = {
    assetCode: '',
    name: '',
    description: '',
    assetType: 'PLUMBING',
    criticality: 'MEDIUM',
    locationDetails: '',
    manufacturer: '',
    modelNumber: '',
    serialNumber: ''
  };

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    if (!this.communityId) {
      this.error = 'No active community is selected for this account.';
      return;
    }
    this.loadSummary();
    this.loadAssets();
  }

  loadSummary(): void {
    this.api.getAssetSummary(this.communityId).subscribe({
      next: r => this.summary = r.data,
      error: () => this.error = 'Could not load asset summary metrics.'
    });
  }

  loadAssets(): void {
    if (!this.communityId) return;
    this.loading = true;
    if (this.showDueOnly) {
      this.api.getDueForMaintenance(this.communityId).subscribe({
        next: r => {
          this.assets = r.data.content || [];
          this.loading = false;
        },
        error: e => {
          this.loading = false;
          this.error = e?.error?.message || 'Could not load due maintenance assets.';
        }
      });
    } else {
      this.api.getAssets(this.communityId, this.statusFilter).subscribe({
        next: r => {
          this.assets = r.data.content || [];
          this.loading = false;
        },
        error: e => {
          this.loading = false;
          this.error = e?.error?.message || 'Could not load asset list.';
        }
      });
    }
  }

  toggleDueFilter(): void {
    this.showDueOnly = !this.showDueOnly;
    this.loadAssets();
  }

  filteredAssets(): Asset[] {
    const q = this.search.trim().toLowerCase();
    return this.assets.filter(a =>
      (!this.criticalityFilter || a.criticality === this.criticalityFilter) &&
      (!q || [a.assetCode, a.name, a.manufacturer || '', a.modelNumber || '', a.locationDetails || '', a.assetType].some(v => v.toLowerCase().includes(q)))
    );
  }

  label(val: string): string {
    return val.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, m => m.toUpperCase());
  }

  isMaintenanceDue(asset: Asset): boolean {
    if (!asset.nextMaintenanceAt) return false;
    const dueTime = new Date(asset.nextMaintenanceAt).getTime();
    const twoWeeks = 14 * 24 * 3600 * 1000;
    return dueTime <= (Date.now() + twoWeeks);
  }

  openCreateModal(): void {
    this.createOpen = true;
    this.formError = '';
    this.draft = {
      assetCode: '',
      name: '',
      description: '',
      assetType: 'PLUMBING',
      criticality: 'MEDIUM',
      locationDetails: '',
      manufacturer: '',
      modelNumber: '',
      serialNumber: ''
    };
    this.nextMaintenanceStr = '';
  }

  createAsset(): void {
    this.formError = '';
    if (!this.draft.assetCode.trim() || !this.draft.name.trim()) {
      this.formError = 'Asset code and equipment name are required.';
      return;
    }
    if (this.nextMaintenanceStr) {
      this.draft.nextMaintenanceAt = new Date(this.nextMaintenanceStr).toISOString();
    }
    this.saving = true;
    this.api.createAsset(this.communityId, this.draft).subscribe({
      next: r => {
        this.saving = false;
        this.createOpen = false;
        this.refresh();
        this.selectAsset(r.data);
      },
      error: e => {
        this.saving = false;
        this.formError = e?.error?.message || 'Could not register asset.';
      }
    });
  }

  selectAsset(asset: Asset): void {
    this.selected = asset;
    this.targetStatus = asset.status;
    this.statusReason = '';
    this.loadHistory(asset.id);
  }

  loadHistory(assetId: string): void {
    this.api.getAssetStatusHistory(assetId, this.communityId).subscribe({
      next: r => this.statusHistory = r.data || [],
      error: () => {}
    });
  }

  updateStatus(): void {
    if (!this.selected) return;
    this.api.updateAssetStatus(this.selected.id, this.communityId, this.targetStatus, this.statusReason).subscribe({
      next: r => {
        this.selected = r.data;
        this.statusReason = '';
        this.loadHistory(r.data.id);
        this.loadSummary();
        this.loadAssets();
      },
      error: e => this.error = e?.error?.message || 'Could not update asset status.'
    });
  }

  schedulePreventiveWorkOrder(_asset: Asset): void {
    this.router.navigate(['/work-orders']);
  }
}
