import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth/auth.service';
import { WorkOrderService } from './work-order.service';
import { CreateWorkOrderRequest, WorkOrder, WorkOrderCategory, WorkOrderPriority, WorkOrderScope, WorkOrderStatus, WorkOrderSummary } from './work-order.model';

@Component({
  selector: 'scos-work-order-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="work-orders">
      <header class="page-header">
        <div><p class="eyebrow">COMMUNITY OPERATIONS</p><h1>Complaints & Maintenance</h1><p class="muted">Dispatch, track and resolve community work orders.</p></div>
        <button class="primary" (click)="createOpen = true">＋ New work order</button>
      </header>
      <p class="error" *ngIf="error">{{ error }}</p>
      <div class="kpis" *ngIf="summary as s">
        <article><span>Active work orders</span><strong>{{ s.totalActive }}</strong></article>
        <article><span>Open queue</span><strong>{{ s.openCount }}</strong></article>
        <article><span>In progress</span><strong>{{ s.inProgressCount }}</strong></article>
        <article class="danger"><span>SLA overdue</span><strong>{{ s.slaBreachedCount }}</strong></article>
        <article><span>Resident rating</span><strong>{{ s.averageRating | number:'1.1-1' }} <small>/ 5</small></strong></article>
      </div>
      <div class="toolbar">
        <input class="field search" [(ngModel)]="search" placeholder="Search ticket, title or location" aria-label="Search work orders">
        <select class="field" [(ngModel)]="statusFilter" (change)="loadOrders()"><option value="">All statuses</option><option *ngFor="let s of statuses" [value]="s">{{ label(s) }}</option></select>
        <select class="field" [(ngModel)]="priorityFilter" (change)="loadOrders()"><option value="">All priorities</option><option *ngFor="let p of priorities" [value]="p">{{ p }}</option></select>
        <div class="view-switch"><button [class.selected]="view === 'kanban'" (click)="view='kanban'">Kanban</button><button [class.selected]="view === 'table'" (click)="view='table'">Table</button></div>
        <button class="secondary" (click)="refresh()">↻ Refresh</button>
      </div>
      <div class="loading" *ngIf="loading">Loading work orders…</div>
      <ng-container *ngIf="!loading && view === 'kanban'">
        <div class="kanban">
          <section class="lane" *ngFor="let lane of lanes">
            <header><span>{{ label(lane) }}</span><b>{{ filteredOrders(lane).length }}</b></header>
            <button class="ticket" *ngFor="let order of filteredOrders(lane)" (click)="open(order)">
              <div class="ticket-top"><span class="ticket-id">{{ order.ticketNumber }}</span><span class="priority" [ngClass]="order.priority.toLowerCase()">{{ order.priority }}</span></div>
              <h3>{{ order.title }}</h3><p class="muted">{{ order.locationDetails || (order.scope === 'COMMON_AREA' ? 'Common area' : 'Private unit') }}</p>
              <div class="ticket-foot"><span [class.overdue]="isOverdue(order)">◷ {{ slaLabel(order) }}</span><span>{{ order.currencyCode }} {{ order.totalCost | number:'1.2-2' }}</span></div>
            </button>
            <p class="empty" *ngIf="!filteredOrders(lane).length">No tickets in this lane</p>
          </section>
        </div>
      </ng-container>
      <div class="table-wrap" *ngIf="!loading && view === 'table'">
        <table><thead><tr><th>Ticket</th><th>Issue</th><th>Category</th><th>Priority</th><th>Status</th><th>SLA deadline</th><th>Cost</th><th></th></tr></thead>
        <tbody><tr *ngFor="let order of filteredOrders()"><td class="ticket-id">{{ order.ticketNumber }}</td><td><strong>{{ order.title }}</strong><small>{{ order.locationDetails || order.scope }}</small></td><td>{{ order.category }}</td><td><span class="priority" [ngClass]="order.priority.toLowerCase()">{{ order.priority }}</span></td><td><span class="status">{{ label(order.status) }}</span></td><td [class.overdue]="isOverdue(order)">{{ order.slaDueAt | date:'medium' }}</td><td>{{ order.currencyCode }} {{ order.totalCost | number:'1.2-2' }}</td><td><button class="secondary" (click)="open(order)">Open</button></td></tr></tbody></table>
        <p class="empty" *ngIf="!filteredOrders().length">No work orders match these filters.</p>
      </div>
    </section>

    <div class="overlay" *ngIf="createOpen" (click)="createOpen=false">
      <section class="dialog" role="dialog" aria-modal="true" aria-labelledby="create-title" (click)="$event.stopPropagation()">
        <header><div><p class="eyebrow">NEW REQUEST</p><h2 id="create-title">Create work order</h2></div><button class="icon-btn" (click)="createOpen=false" aria-label="Close">×</button></header>
        <form (ngSubmit)="createOrder()">
          <label>Issue title<input class="field" name="title" [(ngModel)]="draft.title" required maxlength="160"></label>
          <label>Description<textarea class="field" name="description" [(ngModel)]="draft.description" required rows="3"></textarea></label>
          <div class="form-grid"><label>Scope<select class="field" name="scope" [(ngModel)]="draft.scope"><option value="COMMON_AREA">Common area</option><option value="PRIVATE_UNIT">Private unit</option></select></label>
          <label>Category<select class="field" name="category" [(ngModel)]="draft.category"><option *ngFor="let c of categories" [value]="c">{{ c }}</option></select></label>
          <label>Priority<select class="field" name="priority" [(ngModel)]="draft.priority"><option *ngFor="let p of priorities" [value]="p">{{ p }}</option></select></label>
          <label *ngIf="draft.scope === 'PRIVATE_UNIT'">Unit ID<input class="field" name="unitId" [(ngModel)]="draft.unitId" required placeholder="Unit UUID"></label>
          <label>Location<input class="field" name="locationDetails" [(ngModel)]="draft.locationDetails" maxlength="200" placeholder="Tower, floor or amenity"></label></div>
          <p class="error" *ngIf="formError">{{ formError }}</p><footer><button type="button" class="secondary" (click)="createOpen=false">Cancel</button><button type="submit" class="primary" [disabled]="saving">{{ saving ? 'Creating…' : 'Create work order' }}</button></footer>
        </form>
      </section>
    </div>

    <div class="overlay" *ngIf="selected" (click)="selected=null">
      <aside class="drawer" role="dialog" aria-modal="true" (click)="$event.stopPropagation()">
        <header><div><p class="eyebrow">{{ selected.ticketNumber }}</p><h2>{{ selected.title }}</h2><p class="muted">{{ selected.category }} · {{ selected.priority }} · {{ label(selected.status) }}</p></div><button class="icon-btn" (click)="selected=null" aria-label="Close">×</button></header>
        <div class="drawer-body">
          <section class="detail-card"><h3>Issue details</h3><p>{{ selected.description }}</p><dl><dt>Location</dt><dd>{{ selected.locationDetails || selected.scope }}</dd><dt>Reported</dt><dd>{{ selected.createdAt | date:'medium' }}</dd><dt>SLA due</dt><dd [class.overdue]="isOverdue(selected)">{{ selected.slaDueAt | date:'medium' }}</dd><dt>Estimated / recorded cost</dt><dd>{{ selected.currencyCode }} {{ selected.totalCost | number:'1.2-2' }}</dd></dl></section>
          <section class="detail-card"><h3>Work order actions</h3>
            <label>Technician user UUID<input class="field" [(ngModel)]="technicianId" placeholder="Technician UUID"></label><label>Dispatch note<input class="field" [(ngModel)]="actionNotes" placeholder="Instructions or reason"></label>
            <button class="secondary full" (click)="assignTechnician()" [disabled]="!technicianId">Assign technician</button>
            <div class="actions"><button *ngFor="let next of allowedTransitions(selected.status)" class="secondary" (click)="transition(next)">{{ label(next) }}</button></div>
          </section>
          <section class="detail-card"><h3>Itemized costs</h3><div class="cost-line" *ngFor="let c of selected.costs"><span>{{ c.itemDescription }}<small>{{ c.costType }} · {{ c.quantity }} × {{ c.unitPrice }}</small></span><b>{{ c.currencyCode }} {{ c.totalAmount | number:'1.2-2' }}</b></div>
            <div class="form-grid"><label>Type<select class="field" [(ngModel)]="costType"><option *ngFor="let c of costTypes" [value]="c">{{ c }}</option></select></label><label>Item<input class="field" [(ngModel)]="costDescription" placeholder="Part or labour"></label><label>Qty<input class="field" type="number" min="0.01" step="0.01" [(ngModel)]="costQuantity"></label><label>Unit price<input class="field" type="number" min="0" step="0.01" [(ngModel)]="costUnitPrice"></label></div><button class="secondary full" (click)="addCost()">Add cost line</button>
          </section>
          <section class="detail-card"><h3>Activity timeline</h3><div class="activity" *ngFor="let a of selected.activities"><span class="activity-dot"></span><div><strong>{{ a.activityType.replace('_',' ') }}</strong><p>{{ a.notes }}</p><small>{{ a.createdAt | date:'medium' }}</small></div></div><p class="empty" *ngIf="!selected.activities.length">No activity recorded yet.</p></section>
          <section class="detail-card" *ngIf="selected.status === 'RESOLVED'"><h3>Resident verification</h3><label>Rating<select class="field" [(ngModel)]="rating"><option [ngValue]="5">5 — Excellent</option><option [ngValue]="4">4 — Good</option><option [ngValue]="3">3 — Average</option><option [ngValue]="2">2 — Poor</option><option [ngValue]="1">1 — Very poor</option></select></label><label>Feedback<input class="field" [(ngModel)]="feedbackComments"></label><button class="primary full" (click)="submitFeedback()">Confirm resolution</button></section>
        </div>
      </aside>
    </div>
  `,
  styles: [`
    :host{display:block;color:var(--text-main)}.work-orders{max-width:1600px;margin:auto}.page-header{display:flex;justify-content:space-between;align-items:center;gap:1rem;margin-bottom:1.5rem}.page-header h1{font-size:1.7rem;margin:.3rem 0}.eyebrow{font-size:.68rem;letter-spacing:.13em;color:#818cf8;font-weight:800}.muted{color:var(--text-muted);font-size:.85rem}.primary,.secondary,.view-switch button{border-radius:9px;padding:.65rem .9rem;font-weight:650;cursor:pointer;border:1px solid transparent}.primary{background:var(--primary-600);color:white}.primary:disabled{opacity:.55;cursor:wait}.secondary,.view-switch button{background:var(--surface-card);border-color:var(--surface-border);color:var(--text-main)}.kpis{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:.8rem;margin-bottom:1.25rem}.kpis article,.lane,.detail-card{background:var(--surface-card);border:1px solid var(--surface-border);border-radius:13px;padding:1rem}.kpis span{display:block;color:var(--text-muted);font-size:.78rem}.kpis strong{display:block;font-size:1.7rem;margin-top:.45rem}.kpis small{font-size:.8rem;color:var(--text-muted)}.kpis .danger strong,.overdue{color:#fb7185!important}.toolbar{display:flex;gap:.65rem;align-items:center;margin-bottom:1rem;flex-wrap:wrap}.field{background:#0b1120;border:1px solid var(--surface-border);border-radius:8px;padding:.68rem .75rem;color:var(--text-main);min-width:0;width:100%}.toolbar .field{width:auto;max-width:190px}.toolbar .search{flex:1;max-width:none}.view-switch{display:flex;background:#0b1120;border:1px solid var(--surface-border);padding:3px;border-radius:10px}.view-switch button{border:0;padding:.5rem .7rem}.view-switch button.selected{background:var(--primary-600);color:white}.kanban{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:.8rem;align-items:start}.lane{padding:.65rem;min-height:220px;background:rgba(30,41,59,.6)}.lane>header{display:flex;justify-content:space-between;align-items:center;padding:.35rem .4rem .8rem;font-size:.78rem;text-transform:uppercase;letter-spacing:.04em;color:var(--text-muted)}.lane>header b{background:#0b1120;border-radius:7px;padding:.2rem .45rem;color:var(--text-main)}.ticket{display:block;text-align:left;width:100%;border:1px solid var(--surface-border);background:#111c30;border-radius:10px;padding:.85rem;margin-bottom:.6rem;color:var(--text-main);cursor:pointer}.ticket:hover{border-color:#6366f1;transform:translateY(-1px)}.ticket-top,.ticket-foot{display:flex;justify-content:space-between;align-items:center;gap:.5rem}.ticket h3{font-size:.92rem;line-height:1.35;margin:.65rem 0}.ticket-foot{font-size:.72rem;color:var(--text-muted);margin-top:.9rem}.ticket-id{font-size:.75rem;color:#a5b4fc;font-weight:800}.priority{font-size:.68rem;font-weight:800;border-radius:5px;padding:.25rem .4rem;background:#334155;color:#e2e8f0}.priority.critical{background:#7f1d1d;color:#fecaca}.priority.high{background:#78350f;color:#fde68a}.priority.medium{background:#1e3a8a;color:#bfdbfe}.priority.low{background:#14532d;color:#bbf7d0}.empty{font-size:.8rem;color:var(--text-muted);padding:1rem;text-align:center}.table-wrap{overflow:auto;border:1px solid var(--surface-border);border-radius:12px}table{width:100%;border-collapse:collapse;min-width:900px;background:var(--surface-card)}th,td{text-align:left;padding:.85rem;border-bottom:1px solid var(--surface-border);font-size:.8rem}th{color:var(--text-muted);font-size:.7rem;text-transform:uppercase;letter-spacing:.06em}td small{display:block;color:var(--text-muted);margin-top:.25rem}.status{font-size:.72rem;background:#334155;padding:.25rem .45rem;border-radius:5px}.loading{padding:2rem;color:var(--text-muted)}.error{color:#fda4af;margin:.5rem 0 1rem}.overlay{position:fixed;inset:0;z-index:1000;background:rgba(2,6,23,.72);display:flex;justify-content:center;align-items:center;padding:1rem}.dialog,.drawer{background:#101a2d;border:1px solid var(--surface-border);box-shadow:var(--shadow-lg);color:var(--text-main)}.dialog{width:min(700px,100%);max-height:90vh;overflow:auto;border-radius:16px;padding:1.25rem}.dialog header,.drawer>header{display:flex;justify-content:space-between;gap:1rem;align-items:flex-start;padding-bottom:1rem;border-bottom:1px solid var(--surface-border)}h2{font-size:1.25rem;margin:.35rem 0}.icon-btn{background:transparent;color:var(--text-main);border:0;font-size:1.7rem;cursor:pointer}.dialog form{display:grid;gap:1rem;padding-top:1rem}.dialog label,.detail-card label{display:grid;gap:.4rem;font-size:.8rem;color:var(--text-muted)}.form-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:.8rem}.dialog footer{display:flex;justify-content:flex-end;gap:.6rem;margin-top:.3rem}.drawer{width:min(650px,100%);height:100%;max-height:100%;margin-left:auto;border-radius:15px 0 0 15px;display:flex;flex-direction:column;padding:1.2rem}.drawer-body{overflow:auto;padding:1rem .2rem 2rem;display:grid;gap:.8rem}.detail-card h3{font-size:.92rem;margin-bottom:.8rem}.detail-card p{font-size:.84rem;line-height:1.5}.detail-card dl{display:grid;grid-template-columns:1fr 1.3fr;gap:.55rem;margin-top:1rem;font-size:.78rem}.detail-card dt{color:var(--text-muted)}.detail-card dd{overflow-wrap:anywhere}.detail-card label{margin:.65rem 0}.full{width:100%;margin-top:.5rem}.actions{display:flex;flex-wrap:wrap;gap:.5rem;margin-top:.7rem}.cost-line{display:flex;justify-content:space-between;gap:.5rem;border-bottom:1px solid var(--surface-border);padding:.6rem 0;font-size:.8rem}.cost-line small{display:block;color:var(--text-muted);margin-top:.25rem}.activity{display:flex;gap:.7rem;padding:.6rem 0}.activity-dot{width:8px;height:8px;border-radius:50%;background:#818cf8;margin-top:.35rem;flex:none}.activity strong{font-size:.8rem}.activity p{margin:.25rem 0}.activity small{font-size:.7rem;color:var(--text-muted)}@media(max-width:1100px){.kpis{grid-template-columns:repeat(3,1fr)}.kanban{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(max-width:700px){.page-header{align-items:flex-start;flex-direction:column}.kpis{grid-template-columns:repeat(2,1fr)}.kanban{grid-template-columns:1fr}.toolbar .field{max-width:none;flex:1}.form-grid{grid-template-columns:1fr}.drawer{width:100%;border-radius:12px}.main-content{padding:1rem}}
  `]
})
export class WorkOrderListComponent implements OnInit {
  private readonly api=inject(WorkOrderService);
  private readonly auth=inject(AuthService);
  readonly statuses: WorkOrderStatus[]=['OPEN','ASSIGNED','IN_PROGRESS','ON_HOLD','RESOLVED','CLOSED','CANCELLED'];
  readonly lanes: WorkOrderStatus[]=['OPEN','ASSIGNED','IN_PROGRESS','RESOLVED'];
  readonly priorities: WorkOrderPriority[]=['CRITICAL','HIGH','MEDIUM','LOW'];
  readonly categories: WorkOrderCategory[]=['PLUMBING','ELECTRICAL','HVAC','ELEVATOR','CIVIL','SECURITY_DEVICE','GARDENING','OTHER'];
  readonly costTypes=['LABOR','MATERIALS','VENDOR_FEE','EQUIPMENT','OTHER'];
  communityId=this.auth.activeCommunityId() || '';
  orders:WorkOrder[]=[]; summary:WorkOrderSummary|null=null; selected:WorkOrder|null=null;
  loading=false; saving=false; error=''; formError=''; createOpen=false; search=''; statusFilter:WorkOrderStatus|''=''; priorityFilter:WorkOrderPriority|''=''; view:'kanban'|'table'='kanban';
  technicianId=''; actionNotes=''; costType='MATERIALS'; costDescription=''; costQuantity=1; costUnitPrice=0; rating=5; feedbackComments='';
  draft:CreateWorkOrderRequest={communityId:this.communityId,title:'',description:'',scope:'COMMON_AREA',category:'OTHER',priority:'MEDIUM',locationDetails:''};
  ngOnInit():void { this.refresh(); }
  refresh():void { if(!this.communityId){this.error='No active community is selected for this account.';return;} this.loadSummary(); this.loadOrders(); }
  loadSummary():void { this.api.getSummary(this.communityId).subscribe({next:r=>this.summary=r.data,error:()=>this.error='Could not load work-order metrics.'}); }
  loadOrders():void { if(!this.communityId)return;this.loading=true;this.api.getWorkOrders(this.communityId,this.statusFilter,this.priorityFilter).subscribe({next:r=>{this.orders=r.data.content||[];this.loading=false;},error:e=>{this.loading=false;this.error=e?.error?.message||'Could not load work orders. Check your community access and permissions.';}}); }
  filteredOrders(lane?:WorkOrderStatus):WorkOrder[]{const q=this.search.trim().toLowerCase();return this.orders.filter(o=>(!lane||o.status===lane)&&(!q||[o.ticketNumber,o.title,o.locationDetails||''].some(v=>v.toLowerCase().includes(q))));}
  label(value:string):string{return value.replaceAll('_',' ').toLowerCase().replace(/\b\w/g,m=>m.toUpperCase());}
  isOverdue(order:WorkOrder):boolean{return order.status!=='CLOSED'&&order.status!=='CANCELLED'&&new Date(order.slaDueAt).getTime()<Date.now();}
  slaLabel(order:WorkOrder):string{if(this.isOverdue(order))return 'SLA overdue';const hours=Math.max(0,Math.round((new Date(order.slaDueAt).getTime()-Date.now())/3600000));return hours<24?`${hours}h left`:`${Math.floor(hours/24)}d left`;}
  open(order:WorkOrder):void{this.api.getWorkOrder(order.id,this.communityId).subscribe({next:r=>{this.selected=r.data;this.technicianId=r.data.assignedTechnicianId||'';this.actionNotes='';},error:()=>this.error='Could not load work-order details.'});}
  createOrder():void{this.formError='';if(!this.communityId){this.formError='Select an active community first.';return;}if(this.draft.scope==='PRIVATE_UNIT'&&!this.draft.unitId){this.formError='Unit ID is required for private-unit requests.';return;}this.saving=true;this.api.create({...this.draft,communityId:this.communityId}).subscribe({next:r=>{this.saving=false;this.createOpen=false;this.draft={communityId:this.communityId,title:'',description:'',scope:'COMMON_AREA',category:'OTHER',priority:'MEDIUM',locationDetails:''};this.refresh();this.open(r.data);},error:e=>{this.saving=false;this.formError=e?.error?.message||'Could not create work order.';}});}
  assignTechnician():void{if(!this.selected||!this.technicianId.trim())return;this.api.assign(this.selected.id,this.communityId,this.technicianId.trim(),this.actionNotes).subscribe({next:r=>{this.selected=r.data;this.refresh();},error:e=>this.error=e?.error?.message||'Technician assignment failed.'});}
  allowedTransitions(status:WorkOrderStatus):WorkOrderStatus[]{const map:Record<WorkOrderStatus,WorkOrderStatus[]>={OPEN:['ASSIGNED','CANCELLED'],ASSIGNED:['IN_PROGRESS','CANCELLED'],IN_PROGRESS:['ON_HOLD','RESOLVED','ASSIGNED'],ON_HOLD:['IN_PROGRESS','CANCELLED'],RESOLVED:['CLOSED','IN_PROGRESS'],CLOSED:[],CANCELLED:[]};return map[status]||[];}
  transition(status:WorkOrderStatus):void{if(!this.selected)return;this.api.transition(this.selected.id,this.communityId,status,this.actionNotes).subscribe({next:r=>{this.selected=r.data;this.actionNotes='';this.refresh();},error:e=>this.error=e?.error?.message||'Status transition failed.'});}
  addCost():void{if(!this.selected||!this.costDescription.trim()||this.costQuantity<=0||this.costUnitPrice<0){this.error='Enter a valid cost item, quantity and unit price.';return;}this.api.addCost(this.selected.id,this.communityId,this.costType,this.costDescription.trim(),this.costQuantity,this.costUnitPrice).subscribe({next:()=>{if(this.selected)this.open(this.selected);this.refresh();this.costDescription='';},error:e=>this.error=e?.error?.message||'Could not record cost.'});}
  submitFeedback():void{if(!this.selected)return;this.api.submitFeedback(this.selected.id,this.communityId,this.rating,true,this.feedbackComments).subscribe({next:r=>{this.selected=r.data;this.refresh();},error:e=>this.error=e?.error?.message||'Could not submit feedback.'});}
}
