import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth/auth.service';
import { WorkOrderService } from './work-order.service';
import {
  Asset,
  CreateWorkOrderRequest,
  WorkOrder,
  WorkOrderCategory,
  WorkOrderPriority,
  WorkOrderScope,
  WorkOrderStatus,
  WorkOrderSummary,
  WorkOrderType
} from './work-order.model';

@Component({
  selector: 'scos-work-order-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="work-orders">
      <header class="page-header">
        <div>
          <p class="eyebrow">COMMUNITY OPERATIONS</p>
          <h1>Complaints & Maintenance Desk</h1>
          <p class="muted">Dispatch, track and resolve community work orders with SLA governance.</p>
        </div>
        <div class="header-actions">
          <button class="primary" (click)="openCreateModal()">＋ New work order</button>
        </div>
      </header>

      <p class="error" *ngIf="error">{{ error }}</p>

      <div class="kpis" *ngIf="summary as s">
        <article><span>Active tickets</span><strong>{{ s.totalActive }}</strong></article>
        <article><span>Open queue</span><strong>{{ s.openCount }}</strong></article>
        <article><span>In progress</span><strong>{{ s.inProgressCount }}</strong></article>
        <article class="danger"><span>SLA overdue</span><strong>{{ s.slaBreachedCount }}</strong></article>
        <article><span>Resident rating</span><strong>{{ s.averageRating | number:'1.1-1' }} <small>/ 5</small></strong></article>
      </div>

      <div class="toolbar">
        <input class="field search" [(ngModel)]="search" placeholder="Search ticket #, title, location or asset" aria-label="Search work orders">
        <select class="field" [(ngModel)]="statusFilter" (change)="loadOrders()">
          <option value="">All statuses</option>
          <option *ngFor="let s of statuses" [value]="s">{{ label(s) }}</option>
        </select>
        <select class="field" [(ngModel)]="priorityFilter" (change)="loadOrders()">
          <option value="">All priorities</option>
          <option *ngFor="let p of priorities" [value]="p">{{ p }}</option>
        </select>
        <select class="field" [(ngModel)]="typeFilter" (change)="loadOrders()">
          <option value="">All types</option>
          <option *ngFor="let t of workOrderTypes" [value]="t">{{ label(t) }}</option>
        </select>
        <div class="view-switch">
          <button [class.selected]="view === 'kanban'" (click)="view='kanban'">Kanban</button>
          <button [class.selected]="view === 'table'" (click)="view='table'">Table</button>
        </div>
        <button class="secondary" (click)="refresh()">↻ Refresh</button>
      </div>

      <div class="loading" *ngIf="loading">Loading work orders…</div>

      <ng-container *ngIf="!loading && view === 'kanban'">
        <div class="kanban">
          <section class="lane" *ngFor="let lane of lanes">
            <header>
              <span>{{ label(lane) }}</span>
              <b>{{ filteredOrders(lane).length }}</b>
            </header>
            <button class="ticket" *ngFor="let order of filteredOrders(lane)" (click)="open(order)">
              <div class="ticket-top">
                <span class="ticket-id">{{ order.ticketNumber }}</span>
                <div class="tags">
                  <span class="type-tag" [ngClass]="order.workOrderType.toLowerCase()">{{ order.workOrderType }}</span>
                  <span class="priority" [ngClass]="order.priority.toLowerCase()">{{ order.priority }}</span>
                </div>
              </div>
              <h3>{{ order.title }}</h3>
              <p class="location-label">
                📍 {{ order.locationDetails || (order.scope === 'COMMON_AREA' ? 'Common area' : 'Private unit') }}
              </p>
              <div class="asset-chip" *ngIf="order.assetName || order.assetCode">
                ⚙️ {{ order.assetName || order.assetCode }}
                <span class="oos-badge" *ngIf="order.outOfService">🚨 Out of Service</span>
              </div>
              <div class="ticket-foot">
                <span [class.overdue]="isOverdue(order)">◷ {{ slaLabel(order) }}</span>
                <span>{{ order.currencyCode }} {{ order.totalCost | number:'1.2-2' }}</span>
              </div>
            </button>
            <p class="empty" *ngIf="!filteredOrders(lane).length">No tickets in this lane</p>
          </section>
        </div>
      </ng-container>

      <div class="table-wrap" *ngIf="!loading && view === 'table'">
        <table>
          <thead>
            <tr>
              <th>Ticket</th>
              <th>Issue & Location</th>
              <th>Type</th>
              <th>Asset</th>
              <th>Priority</th>
              <th>Status</th>
              <th>SLA deadline</th>
              <th>Cost</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let order of filteredOrders()">
              <td class="ticket-id">{{ order.ticketNumber }}</td>
              <td>
                <strong>{{ order.title }}</strong>
                <small>{{ order.locationDetails || order.scope }}</small>
              </td>
              <td><span class="type-tag" [ngClass]="order.workOrderType.toLowerCase()">{{ order.workOrderType }}</span></td>
              <td>
                <span *ngIf="order.assetName" class="asset-name">⚙️ {{ order.assetName }}</span>
                <span *ngIf="!order.assetName" class="muted">—</span>
                <small class="danger-text" *ngIf="order.outOfService">Out of service</small>
              </td>
              <td><span class="priority" [ngClass]="order.priority.toLowerCase()">{{ order.priority }}</span></td>
              <td><span class="status">{{ label(order.status) }}</span></td>
              <td [class.overdue]="isOverdue(order)">{{ order.slaDueAt | date:'medium' }}</td>
              <td>{{ order.currencyCode }} {{ order.totalCost | number:'1.2-2' }}</td>
              <td><button class="secondary" (click)="open(order)">Open</button></td>
            </tr>
          </tbody>
        </table>
        <p class="empty" *ngIf="!filteredOrders().length">No work orders match these filters.</p>
      </div>
    </section>

    <!-- Create Work Order Modal -->
    <div class="overlay" *ngIf="createOpen" (click)="createOpen=false">
      <section class="dialog" role="dialog" aria-modal="true" aria-labelledby="create-title" (click)="$event.stopPropagation()">
        <header>
          <div>
            <p class="eyebrow">NEW MAINTENANCE REQUEST</p>
            <h2 id="create-title">Create work order</h2>
          </div>
          <button class="icon-btn" (click)="createOpen=false" aria-label="Close">×</button>
        </header>
        <form (ngSubmit)="createOrder()">
          <label>Issue title
            <input class="field" name="title" [(ngModel)]="draft.title" required maxlength="160" placeholder="E.g., Booster pump vibration, corridor lighting, AC repair">
          </label>
          <label>Description
            <textarea class="field" name="description" [(ngModel)]="draft.description" required rows="3" placeholder="Provide full context, symptoms, or maintenance steps"></textarea>
          </label>

          <div class="form-grid">
            <label>Work order type
              <select class="field" name="workOrderType" [(ngModel)]="draft.workOrderType">
                <option *ngFor="let t of workOrderTypes" [value]="t">{{ label(t) }}</option>
              </select>
            </label>
            <label>Scope
              <select class="field" name="scope" [(ngModel)]="draft.scope">
                <option value="COMMON_AREA">Common area infrastructure</option>
                <option value="PRIVATE_UNIT">Private residential unit</option>
              </select>
            </label>
            <label>Category
              <select class="field" name="category" [(ngModel)]="draft.category">
                <option *ngFor="let c of categories" [value]="c">{{ c }}</option>
              </select>
            </label>
            <label>Priority
              <select class="field" name="priority" [(ngModel)]="draft.priority">
                <option *ngFor="let p of priorities" [value]="p">{{ p }}</option>
              </select>
            </label>
            <label>Linked equipment / asset (optional)
              <select class="field" name="assetId" [(ngModel)]="draft.assetId" (change)="onAssetSelected()">
                <option [ngValue]="undefined">None (general area)</option>
                <option *ngFor="let a of availableAssets" [value]="a.id">{{ a.assetCode }} — {{ a.name }}</option>
              </select>
            </label>
            <label *ngIf="draft.scope === 'PRIVATE_UNIT'">Unit ID
              <input class="field" name="unitId" [(ngModel)]="draft.unitId" required placeholder="Unit UUID (e.g. A-1201)">
            </label>
            <label>Location details
              <input class="field" name="locationDetails" [(ngModel)]="draft.locationDetails" maxlength="200" placeholder="Tower, floor, basement or utility area">
            </label>
          </div>

          <div class="form-check" *ngIf="draft.assetId">
            <label class="checkbox-label">
              <input type="checkbox" name="outOfService" [(ngModel)]="draft.outOfService">
              <span>Flag linked equipment as <strong>Out of Service</strong> during repairs</span>
            </label>
          </div>

          <div class="form-grid" *ngIf="draft.workOrderType === 'PREVENTIVE' || draft.workOrderType === 'INSPECTION'">
            <label>Planned start
              <input class="field" type="datetime-local" name="plannedStart" [(ngModel)]="plannedStartStr">
            </label>
            <label>Planned completion
              <input class="field" type="datetime-local" name="plannedEnd" [(ngModel)]="plannedEndStr">
            </label>
          </div>

          <p class="error" *ngIf="formError">{{ formError }}</p>
          <footer>
            <button type="button" class="secondary" (click)="createOpen=false">Cancel</button>
            <button type="submit" class="primary" [disabled]="saving">{{ saving ? 'Creating…' : 'Create work order' }}</button>
          </footer>
        </form>
      </section>
    </div>

    <!-- Slide-Over Workbench Drawer -->
    <div class="overlay" *ngIf="selected" (click)="selected=null">
      <aside class="drawer" role="dialog" aria-modal="true" (click)="$event.stopPropagation()">
        <header>
          <div>
            <div class="drawer-eyebrow">
              <span class="ticket-id">{{ selected.ticketNumber }}</span>
              <span class="type-tag" [ngClass]="selected.workOrderType.toLowerCase()">{{ selected.workOrderType }}</span>
              <span class="priority" [ngClass]="selected.priority.toLowerCase()">{{ selected.priority }}</span>
            </div>
            <h2>{{ selected.title }}</h2>
            <p class="muted">{{ selected.category }} · Status: <strong>{{ label(selected.status) }}</strong></p>
          </div>
          <button class="icon-btn" (click)="selected=null" aria-label="Close">×</button>
        </header>

        <div class="drawer-body">
          <!-- Overview Card -->
          <section class="detail-card">
            <h3>Issue Details</h3>
            <p>{{ selected.description }}</p>
            <dl>
              <dt>Location</dt>
              <dd>{{ selected.locationDetails || selected.scope }}</dd>
              <dt *ngIf="selected.assetName || selected.assetCode">Equipment</dt>
              <dd *ngIf="selected.assetName || selected.assetCode">
                ⚙️ {{ selected.assetName || selected.assetCode }}
                <span class="danger-text" *ngIf="selected.outOfService"> (Out of Service)</span>
              </dd>
              <dt>Reported</dt>
              <dd>{{ selected.createdAt | date:'medium' }}</dd>
              <dt>SLA due</dt>
              <dd [class.overdue]="isOverdue(selected)">{{ selected.slaDueAt | date:'medium' }}</dd>
              <dt *ngIf="selected.actualStartAt">Actual started</dt>
              <dd *ngIf="selected.actualStartAt">{{ selected.actualStartAt | date:'medium' }}</dd>
              <dt *ngIf="selected.resolvedAt">Resolved at</dt>
              <dd *ngIf="selected.resolvedAt">{{ selected.resolvedAt | date:'medium' }}</dd>
              <dt>Total recorded expense</dt>
              <dd><strong>{{ selected.currencyCode }} {{ selected.totalCost | number:'1.2-2' }}</strong></dd>
            </dl>
          </section>

          <!-- Action & Dispatch Card -->
          <section class="detail-card" *ngIf="selected.status !== 'CLOSED' && selected.status !== 'CANCELLED'">
            <h3>Work Order Actions & Dispatch</h3>
            <label>Technician user UUID
              <input class="field" [(ngModel)]="technicianId" placeholder="Technician UUID (e.g. Ramesh)">
            </label>
            <label>Dispatch instructions / Action note
              <input class="field" [(ngModel)]="actionNotes" placeholder="Reason, instructions or hold justification">
            </label>
            <button class="secondary full" (click)="assignTechnician()" [disabled]="!technicianId">Assign / Reassign technician</button>
            <div class="actions">
              <button *ngFor="let next of allowedTransitions(selected.status)" class="primary-btn-action" (click)="transition(next)">
                Move to {{ label(next) }}
              </button>
            </div>
          </section>

          <!-- Cost Ledger Card -->
          <section class="detail-card">
            <h3>Itemized Cost Breakdown</h3>
            <div class="cost-line" *ngFor="let c of selected.costs">
              <span>
                {{ c.itemDescription }}
                <small>{{ c.costType }} · {{ c.quantity }} × {{ c.unitPrice }}</small>
              </span>
              <b>{{ c.currencyCode }} {{ c.totalAmount | number:'1.2-2' }}</b>
            </div>
            <p class="empty" *ngIf="!selected.costs.length">No costs recorded yet.</p>
            <div class="form-grid" *ngIf="selected.status !== 'CLOSED' && selected.status !== 'CANCELLED'">
              <label>Cost category
                <select class="field" [(ngModel)]="costType">
                  <option *ngFor="let c of costTypes" [value]="c">{{ c }}</option>
                </select>
              </label>
              <label>Item description
                <input class="field" [(ngModel)]="costDescription" placeholder="Replacement valve, cable, labour">
              </label>
              <label>Quantity
                <input class="field" type="number" min="0.01" step="0.01" [(ngModel)]="costQuantity">
              </label>
              <label>Unit price (₹)
                <input class="field" type="number" min="0" step="0.01" [(ngModel)]="costUnitPrice">
              </label>
            </div>
            <button *ngIf="selected.status !== 'CLOSED' && selected.status !== 'CANCELLED'" class="secondary full" (click)="addCost()">＋ Record cost item</button>
          </section>

          <!-- Activity Timeline Card -->
          <section class="detail-card">
            <h3>Append-Only Activity Timeline</h3>
            <div class="activity" *ngFor="let a of selected.activities">
              <span class="activity-dot"></span>
              <div>
                <strong>{{ a.activityType.replace('_',' ') }}</strong>
                <p>{{ a.notes }}</p>
                <small>{{ a.createdAt | date:'medium' }}</small>
              </div>
            </div>
            <p class="empty" *ngIf="!selected.activities.length">No activity recorded yet.</p>
          </section>

          <!-- Resident Sign-off Card -->
          <section class="detail-card" *ngIf="selected.status === 'RESOLVED'">
            <h3>Resident Resolution Verification</h3>
            <p class="muted">Confirm that the issue has been resolved to satisfaction.</p>
            <label>Rating
              <select class="field" [(ngModel)]="rating">
                <option [ngValue]="5">⭐⭐⭐⭐⭐ 5 — Excellent</option>
                <option [ngValue]="4">⭐⭐⭐⭐ 4 — Good</option>
                <option [ngValue]="3">⭐⭐⭐ 3 — Average</option>
                <option [ngValue]="2">⭐⭐ 2 — Needs Improvement</option>
                <option [ngValue]="1">⭐ 1 — Unsatisfactory</option>
              </select>
            </label>
            <label>Feedback notes
              <input class="field" [(ngModel)]="feedbackComments" placeholder="Workmanship, cleanup, promptness">
            </label>
            <button class="primary full" (click)="submitFeedback()">✓ Verify & Close Ticket</button>
          </section>
        </div>
      </aside>
    </div>
  `,
  styles: [`
    :host{display:block;color:var(--text-main)}.work-orders{max-width:1600px;margin:auto}.page-header{display:flex;justify-content:space-between;align-items:center;gap:1rem;margin-bottom:1.5rem}.page-header h1{font-size:1.7rem;margin:.3rem 0}.eyebrow{font-size:.68rem;letter-spacing:.13em;color:#818cf8;font-weight:800}.muted{color:var(--text-muted);font-size:.85rem}.primary,.secondary,.view-switch button,.primary-btn-action{border-radius:9px;padding:.65rem .9rem;font-weight:650;cursor:pointer;border:1px solid transparent}.primary,.primary-btn-action{background:var(--primary-600);color:white}.primary-btn-action{background:#4338ca;padding:.45rem .8rem;font-size:.8rem}.primary:disabled{opacity:.55;cursor:wait}.secondary,.view-switch button{background:var(--surface-card);border-color:var(--surface-border);color:var(--text-main)}.kpis{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:.8rem;margin-bottom:1.25rem}.kpis article,.lane,.detail-card{background:var(--surface-card);border:1px solid var(--surface-border);border-radius:13px;padding:1rem}.kpis span{display:block;color:var(--text-muted);font-size:.78rem}.kpis strong{display:block;font-size:1.7rem;margin-top:.45rem}.kpis small{font-size:.8rem;color:var(--text-muted)}.kpis .danger strong,.overdue{color:#fb7185!important}.toolbar{display:flex;gap:.65rem;align-items:center;margin-bottom:1rem;flex-wrap:wrap}.field{background:#0b1120;border:1px solid var(--surface-border);border-radius:8px;padding:.68rem .75rem;color:var(--text-main);min-width:0;width:100%}.toolbar .field{width:auto;max-width:190px}.toolbar .search{flex:1;max-width:none}.view-switch{display:flex;background:#0b1120;border:1px solid var(--surface-border);padding:3px;border-radius:10px}.view-switch button{border:0;padding:.5rem .7rem}.view-switch button.selected{background:var(--primary-600);color:white}.kanban{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:.8rem;align-items:start}.lane{padding:.65rem;min-height:220px;background:rgba(30,41,59,.6)}.lane>header{display:flex;justify-content:space-between;align-items:center;padding:.35rem .4rem .8rem;font-size:.78rem;text-transform:uppercase;letter-spacing:.04em;color:var(--text-muted)}.lane>header b{background:#0b1120;border-radius:7px;padding:.2rem .45rem;color:var(--text-main)}.ticket{display:block;text-align:left;width:100%;border:1px solid var(--surface-border);background:#111c30;border-radius:10px;padding:.85rem;margin-bottom:.6rem;color:var(--text-main);cursor:pointer}.ticket:hover{border-color:#6366f1;transform:translateY(-1px)}.ticket-top,.ticket-foot{display:flex;justify-content:space-between;align-items:center;gap:.5rem}.ticket h3{font-size:.92rem;line-height:1.35;margin:.65rem 0}.tags{display:flex;gap:.35rem;align-items:center}.location-label{font-size:.78rem;color:var(--text-muted);margin:.2rem 0 .4rem}.asset-chip{display:inline-flex;align-items:center;gap:.35rem;background:#1e293b;border-radius:6px;padding:.2rem .4rem;font-size:.72rem;color:#93c5fd;margin-bottom:.5rem}.oos-badge{background:#7f1d1d;color:#fecaca;padding:.15rem .35rem;border-radius:4px;font-size:.65rem;font-weight:700}.danger-text{color:#f87171;font-weight:600}.ticket-foot{font-size:.72rem;color:var(--text-muted);margin-top:.6rem}.ticket-id{font-size:.75rem;color:#a5b4fc;font-weight:800}.type-tag{font-size:.65rem;font-weight:700;border-radius:4px;padding:.2rem .4rem;background:#1e293b;color:#cbd5e1}.type-tag.preventive{background:#065f46;color:#a7f3d0}.type-tag.emergency{background:#991b1b;color:#fecaca}.type-tag.inspection{background:#075985;color:#bae6fd}.type-tag.corrective{background:#854d0e;color:#fef08a}.priority{font-size:.68rem;font-weight:800;border-radius:5px;padding:.25rem .4rem;background:#334155;color:#e2e8f0}.priority.critical{background:#7f1d1d;color:#fecaca}.priority.high{background:#78350f;color:#fde68a}.priority.medium{background:#1e3a8a;color:#bfdbfe}.priority.low{background:#14532d;color:#bbf7d0}.empty{font-size:.8rem;color:var(--text-muted);padding:1rem;text-align:center}.table-wrap{overflow:auto;border:1px solid var(--surface-border);border-radius:12px}table{width:100%;border-collapse:collapse;min-width:900px;background:var(--surface-card)}th,td{text-align:left;padding:.85rem;border-bottom:1px solid var(--surface-border);font-size:.8rem}th{color:var(--text-muted);font-size:.7rem;text-transform:uppercase;letter-spacing:.06em}td small{display:block;color:var(--text-muted);margin-top:.25rem}.status{font-size:.72rem;background:#334155;padding:.25rem .45rem;border-radius:5px}.loading{padding:2rem;color:var(--text-muted)}.error{color:#fda4af;margin:.5rem 0 1rem}.overlay{position:fixed;inset:0;z-index:1000;background:rgba(2,6,23,.72);display:flex;justify-content:center;align-items:center;padding:1rem}.dialog,.drawer{background:#101a2d;border:1px solid var(--surface-border);box-shadow:var(--shadow-lg);color:var(--text-main)}.dialog{width:min(700px,100%);max-height:90vh;overflow:auto;border-radius:16px;padding:1.25rem}.dialog header,.drawer>header{display:flex;justify-content:space-between;gap:1rem;align-items:flex-start;padding-bottom:1rem;border-bottom:1px solid var(--surface-border)}h2{font-size:1.25rem;margin:.35rem 0}.icon-btn{background:transparent;color:var(--text-main);border:0;font-size:1.7rem;cursor:pointer}.dialog form{display:grid;gap:1rem;padding-top:1rem}.dialog label,.detail-card label{display:grid;gap:.4rem;font-size:.8rem;color:var(--text-muted)}.form-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:.8rem}.form-check{margin-top:.2rem}.checkbox-label{display:flex;align-items:center;gap:.6rem;cursor:pointer;color:var(--text-main);font-size:.82rem}.checkbox-label input{accent-color:#6366f1;width:1.1rem;height:1.1rem}.dialog footer{display:flex;justify-content:flex-end;gap:.6rem;margin-top:.3rem}.drawer{width:min(650px,100%);height:100%;max-height:100%;margin-left:auto;border-radius:15px 0 0 15px;display:flex;flex-direction:column;padding:1.2rem}.drawer-eyebrow{display:flex;align-items:center;gap:.6rem}.drawer-body{overflow:auto;padding:1rem .2rem 2rem;display:grid;gap:.8rem}.detail-card h3{font-size:.92rem;margin-bottom:.8rem}.detail-card p{font-size:.84rem;line-height:1.5}.detail-card dl{display:grid;grid-template-columns:1fr 1.3fr;gap:.55rem;margin-top:1rem;font-size:.78rem}.detail-card dt{color:var(--text-muted)}.detail-card dd{overflow-wrap:anywhere}.detail-card label{margin:.65rem 0}.full{width:100%;margin-top:.5rem}.actions{display:flex;flex-wrap:wrap;gap:.5rem;margin-top:.7rem}.cost-line{display:flex;justify-content:space-between;gap:.5rem;border-bottom:1px solid var(--surface-border);padding:.6rem 0;font-size:.8rem}.cost-line small{display:block;color:var(--text-muted);margin-top:.25rem}.activity{display:flex;gap:.7rem;padding:.6rem 0}.activity-dot{width:8px;height:8px;border-radius:50%;background:#818cf8;margin-top:.35rem;flex:none}.activity strong{font-size:.8rem}.activity p{margin:.25rem 0}.activity small{font-size:.7rem;color:var(--text-muted)}@media(max-width:1100px){.kpis{grid-template-columns:repeat(3,1fr)}.kanban{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(max-width:700px){.page-header{align-items:flex-start;flex-direction:column}.kpis{grid-template-columns:repeat(2,1fr)}.kanban{grid-template-columns:1fr}.toolbar .field{max-width:none;flex:1}.form-grid{grid-template-columns:1fr}.drawer{width:100%;border-radius:12px}}
  `]
})
export class WorkOrderListComponent implements OnInit {
  private readonly api = inject(WorkOrderService);
  private readonly auth = inject(AuthService);

  readonly statuses: WorkOrderStatus[] = ['OPEN', 'ASSIGNED', 'IN_PROGRESS', 'ON_HOLD', 'RESOLVED', 'CLOSED', 'CANCELLED'];
  readonly lanes: WorkOrderStatus[] = ['OPEN', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED'];
  readonly priorities: WorkOrderPriority[] = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];
  readonly categories: WorkOrderCategory[] = ['PLUMBING', 'ELECTRICAL', 'HVAC', 'ELEVATOR', 'CIVIL', 'SECURITY_DEVICE', 'GARDENING', 'OTHER'];
  readonly workOrderTypes: WorkOrderType[] = ['REACTIVE', 'PREVENTIVE', 'INSPECTION', 'EMERGENCY', 'CORRECTIVE'];
  readonly costTypes = ['LABOR', 'MATERIALS', 'VENDOR_FEE', 'EQUIPMENT', 'OTHER'];

  communityId = this.auth.activeCommunityId() || '';
  orders: WorkOrder[] = [];
  summary: WorkOrderSummary | null = null;
  selected: WorkOrder | null = null;
  availableAssets: Asset[] = [];

  loading = false;
  saving = false;
  error = '';
  formError = '';
  createOpen = false;
  search = '';
  statusFilter: WorkOrderStatus | '' = '';
  priorityFilter: WorkOrderPriority | '' = '';
  typeFilter: WorkOrderType | '' = '';
  view: 'kanban' | 'table' = 'kanban';

  technicianId = '';
  actionNotes = '';
  costType = 'MATERIALS';
  costDescription = '';
  costQuantity = 1;
  costUnitPrice = 0;
  rating = 5;
  feedbackComments = '';
  plannedStartStr = '';
  plannedEndStr = '';

  draft: CreateWorkOrderRequest = {
    communityId: this.communityId,
    title: '',
    description: '',
    scope: 'COMMON_AREA',
    category: 'OTHER',
    priority: 'MEDIUM',
    workOrderType: 'REACTIVE',
    locationDetails: '',
    outOfService: false
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
    this.loadOrders();
    this.loadAssets();
  }

  loadSummary(): void {
    this.api.getSummary(this.communityId).subscribe({
      next: r => this.summary = r.data,
      error: () => this.error = 'Could not load work-order metrics.'
    });
  }

  loadOrders(): void {
    if (!this.communityId) return;
    this.loading = true;
    this.api.getWorkOrders(this.communityId, this.statusFilter, this.priorityFilter, this.typeFilter).subscribe({
      next: r => {
        this.orders = r.data.content || [];
        this.loading = false;
      },
      error: e => {
        this.loading = false;
        this.error = e?.error?.message || 'Could not load work orders. Check your community access and permissions.';
      }
    });
  }

  loadAssets(): void {
    if (!this.communityId) return;
    this.api.getAssets(this.communityId).subscribe({
      next: r => this.availableAssets = r.data.content || [],
      error: () => {}
    });
  }

  openCreateModal(): void {
    this.createOpen = true;
    this.formError = '';
    this.draft = {
      communityId: this.communityId,
      title: '',
      description: '',
      scope: 'COMMON_AREA',
      category: 'OTHER',
      priority: 'MEDIUM',
      workOrderType: 'REACTIVE',
      locationDetails: '',
      outOfService: false
    };
    this.plannedStartStr = '';
    this.plannedEndStr = '';
    this.loadAssets();
  }

  onAssetSelected(): void {
    if (!this.draft.assetId) return;
    const matched = this.availableAssets.find(a => a.id === this.draft.assetId);
    if (matched) {
      if (matched.locationDetails) {
        this.draft.locationDetails = matched.locationDetails;
      }
      if (matched.buildingId) {
        this.draft.buildingId = matched.buildingId;
      }
      if (matched.unitId) {
        this.draft.unitId = matched.unitId;
        this.draft.scope = 'PRIVATE_UNIT';
      }
    }
  }

  filteredOrders(lane?: WorkOrderStatus): WorkOrder[] {
    const q = this.search.trim().toLowerCase();
    return this.orders.filter(o =>
      (!lane || o.status === lane) &&
      (!q || [o.ticketNumber, o.title, o.locationDetails || '', o.assetName || '', o.assetCode || ''].some(v => v.toLowerCase().includes(q)))
    );
  }

  label(value: string): string {
    return value.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, m => m.toUpperCase());
  }

  isOverdue(order: WorkOrder): boolean {
    return order.status !== 'CLOSED' && order.status !== 'CANCELLED' && new Date(order.slaDueAt).getTime() < Date.now();
  }

  slaLabel(order: WorkOrder): string {
    if (this.isOverdue(order)) return 'SLA overdue';
    const hours = Math.max(0, Math.round((new Date(order.slaDueAt).getTime() - Date.now()) / 3600000));
    return hours < 24 ? `${hours}h left` : `${Math.floor(hours / 24)}d left`;
  }

  open(order: WorkOrder): void {
    this.api.getWorkOrder(order.id, this.communityId).subscribe({
      next: r => {
        this.selected = r.data;
        this.technicianId = r.data.assignedTechnicianId || '';
        this.actionNotes = '';
      },
      error: () => this.error = 'Could not load work-order details.'
    });
  }

  createOrder(): void {
    this.formError = '';
    if (!this.communityId) {
      this.formError = 'Select an active community first.';
      return;
    }
    if (this.draft.scope === 'PRIVATE_UNIT' && !this.draft.unitId) {
      this.formError = 'Unit ID is required for private-unit requests.';
      return;
    }

    if (this.plannedStartStr && this.plannedEndStr) {
      const s = new Date(this.plannedStartStr).getTime();
      const e = new Date(this.plannedEndStr).getTime();
      if (e < s) {
        this.formError = 'Planned completion date cannot be earlier than planned start date.';
        return;
      }
      this.draft.plannedStartAt = new Date(this.plannedStartStr).toISOString();
      this.draft.plannedEndAt = new Date(this.plannedEndStr).toISOString();
    }

    this.saving = true;
    this.api.create({ ...this.draft, communityId: this.communityId }).subscribe({
      next: r => {
        this.saving = false;
        this.createOpen = false;
        this.refresh();
        this.open(r.data);
      },
      error: e => {
        this.saving = false;
        this.formError = e?.error?.message || 'Could not create work order.';
      }
    });
  }

  assignTechnician(): void {
    if (!this.selected || !this.technicianId.trim()) return;
    this.api.assign(this.selected.id, this.communityId, this.technicianId.trim(), this.actionNotes).subscribe({
      next: r => {
        this.selected = r.data;
        this.refresh();
      },
      error: e => this.error = e?.error?.message || 'Technician assignment failed.'
    });
  }

  allowedTransitions(status: WorkOrderStatus): WorkOrderStatus[] {
    const map: Record<WorkOrderStatus, WorkOrderStatus[]> = {
      OPEN: ['ASSIGNED', 'CANCELLED'],
      ASSIGNED: ['IN_PROGRESS', 'CANCELLED'],
      IN_PROGRESS: ['ON_HOLD', 'RESOLVED', 'ASSIGNED'],
      ON_HOLD: ['IN_PROGRESS', 'CANCELLED'],
      RESOLVED: ['CLOSED', 'IN_PROGRESS'],
      CLOSED: [],
      CANCELLED: []
    };
    return map[status] || [];
  }

  transition(status: WorkOrderStatus): void {
    if (!this.selected) return;
    this.api.transition(this.selected.id, this.communityId, status, this.actionNotes).subscribe({
      next: r => {
        this.selected = r.data;
        this.actionNotes = '';
        this.refresh();
      },
      error: e => this.error = e?.error?.message || 'Status transition failed.'
    });
  }

  addCost(): void {
    if (!this.selected || !this.costDescription.trim() || this.costQuantity <= 0 || this.costUnitPrice < 0) {
      this.error = 'Enter a valid cost item, quantity and unit price.';
      return;
    }
    this.api.addCost(this.selected.id, this.communityId, this.costType, this.costDescription.trim(), this.costQuantity, this.costUnitPrice).subscribe({
      next: () => {
        if (this.selected) this.open(this.selected);
        this.refresh();
        this.costDescription = '';
      },
      error: e => this.error = e?.error?.message || 'Could not record cost.'
    });
  }

  submitFeedback(): void {
    if (!this.selected) return;
    this.api.submitFeedback(this.selected.id, this.communityId, this.rating, true, this.feedbackComments).subscribe({
      next: r => {
        this.selected = r.data;
        this.refresh();
      },
      error: e => this.error = e?.error?.message || 'Could not submit feedback.'
    });
  }
}
