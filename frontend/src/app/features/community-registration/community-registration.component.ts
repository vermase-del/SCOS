import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { CommunityService, CommunityRegistrationRequest, CommunityRegistrationResponse } from '../../core/community/community.service';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'scos-community-registration',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="registration-page">
      <div class="registration-card glass-panel">
        
        <!-- Header -->
        <div class="card-header">
          <div class="brand-row">
            <span class="logo-icon">🏢</span>
            <span class="brand-title">SCOS Enterprise</span>
          </div>
          <h1 class="page-title">Register Your Society / Community</h1>
          <p class="page-subtitle">Onboard your residential complex to the Smart Community Operating System</p>
        </div>

        <!-- Success Screen -->
        <div *ngIf="registeredResult() as result" class="success-screen">
          <div class="success-icon">🎉</div>
          <h2 class="success-title">Society Registered Successfully!</h2>
          <p class="success-desc">
            <strong>{{ result.communityName }}</strong> has been initialized and provisioned in the SCOS platform.
          </p>

          <div class="details-grid">
            <div class="detail-box">
              <span class="detail-label">Society Code</span>
              <span class="detail-value badge-code">{{ result.communityCode }}</span>
            </div>
            <div class="detail-box">
              <span class="detail-label">Administrator</span>
              <span class="detail-value">{{ result.adminUsername }}</span>
            </div>
            <div class="detail-box">
              <span class="detail-label">Towers Built</span>
              <span class="detail-value">{{ result.towersCreated }}</span>
            </div>
            <div class="detail-box">
              <span class="detail-label">Units Configured</span>
              <span class="detail-value">{{ result.unitsCreated }}</span>
            </div>
          </div>

          <div class="success-actions">
            <button class="btn-primary" (click)="enterNewDashboard(result)">
              🚀 Enter {{ result.communityName }} Dashboard
            </button>
            <a routerLink="/login" class="btn-secondary">
              Back to Sign In
            </a>
          </div>
        </div>

        <!-- Multi-Step Wizard -->
        <div *ngIf="!registeredResult()" class="wizard-container">
          
          <!-- Progress Stepper -->
          <div class="stepper">
            <div class="step-item" [class.active]="currentStep() === 1" [class.completed]="currentStep() > 1" (click)="goToStep(1)">
              <div class="step-circle">{{ currentStep() > 1 ? '✓' : '1' }}</div>
              <span class="step-title">Society Details</span>
            </div>
            <div class="step-line" [class.active]="currentStep() > 1"></div>
            <div class="step-item" [class.active]="currentStep() === 2" [class.completed]="currentStep() > 2" (click)="goToStep(2)">
              <div class="step-circle">{{ currentStep() > 2 ? '✓' : '2' }}</div>
              <span class="step-title">Administrator</span>
            </div>
            <div class="step-line" [class.active]="currentStep() > 2"></div>
            <div class="step-item" [class.active]="currentStep() === 3" [class.completed]="currentStep() > 3" (click)="goToStep(3)">
              <div class="step-circle">{{ currentStep() > 3 ? '✓' : '3' }}</div>
              <span class="step-title">Infrastructure</span>
            </div>
            <div class="step-line" [class.active]="currentStep() > 3"></div>
            <div class="step-item" [class.active]="currentStep() === 4" (click)="goToStep(4)">
              <div class="step-circle">4</div>
              <span class="step-title">Review & Launch</span>
            </div>
          </div>

          <!-- Error Alert -->
          <div *ngIf="errorMessage()" class="alert-error">
            <span>⚠️</span> {{ errorMessage() }}
          </div>

          <!-- STEP 1: Society Info -->
          <div *ngIf="currentStep() === 1" class="step-content">
            <h3 class="step-heading">Step 1: Society & Association Profile</h3>
            
            <div class="form-grid">
              <div class="form-group full-width">
                <label>Residential Society / Complex Name *</label>
                <input type="text" class="form-control" [(ngModel)]="form.name" placeholder="e.g. Prestige Willow Woods" required />
              </div>

              <div class="form-group">
                <label>Custom Society Code (Optional)</label>
                <input type="text" class="form-control" [(ngModel)]="form.code" placeholder="Auto-generated if blank (e.g. WILLOW_WOODS)" />
              </div>

              <div class="form-group">
                <label>Legal Registered Name (RWA / Association)</label>
                <input type="text" class="form-control" [(ngModel)]="form.legalName" placeholder="e.g. Prestige Willow Woods Residents Welfare Assoc." />
              </div>

              <div class="form-group">
                <label>Society Registration Number</label>
                <input type="text" class="form-control" [(ngModel)]="form.registrationNumber" placeholder="e.g. SOR/BLR/2026/0148" />
              </div>

              <div class="form-group">
                <label>Tax / GST Identifier</label>
                <input type="text" class="form-control" [(ngModel)]="form.taxIdentifier" placeholder="e.g. 29AAACP9988D1Z2" />
              </div>

              <div class="form-group full-width">
                <label>Street Address Line 1 *</label>
                <input type="text" class="form-control" [(ngModel)]="form.addressLine1" placeholder="Plot / Survey No., Road, Area" required />
              </div>

              <div class="form-group">
                <label>City *</label>
                <input type="text" class="form-control" [(ngModel)]="form.city" placeholder="e.g. Bangalore" required />
              </div>

              <div class="form-group">
                <label>State / Province *</label>
                <input type="text" class="form-control" [(ngModel)]="form.stateProvince" placeholder="e.g. Karnataka" required />
              </div>

              <div class="form-group">
                <label>Postal / PIN Code *</label>
                <input type="text" class="form-control" [(ngModel)]="form.postalCode" placeholder="e.g. 560100" required />
              </div>

              <div class="form-group">
                <label>Official Contact Email *</label>
                <input type="email" class="form-control" [(ngModel)]="form.email" placeholder="manager&#64;willowwoods.org" required />
              </div>

              <div class="form-group">
                <label>Society Office Phone</label>
                <input type="tel" class="form-control" [(ngModel)]="form.phone" placeholder="+91 80 2345 6789" />
              </div>

              <div class="form-group">
                <label>Billing Currency</label>
                <select class="form-control" [(ngModel)]="form.currencyCode">
                  <option value="INR">INR (₹ - Indian Rupee)</option>
                  <option value="USD">USD ($ - US Dollar)</option>
                  <option value="AED">AED (د.إ - UAE Dirham)</option>
                  <option value="EUR">EUR (€ - Euro)</option>
                </select>
              </div>
            </div>

            <div class="wizard-buttons">
              <a routerLink="/login" class="btn-secondary">Cancel</a>
              <button class="btn-primary" (click)="nextStep()">Continue to Administrator &rarr;</button>
            </div>
          </div>

          <!-- STEP 2: Administrator Info -->
          <div *ngIf="currentStep() === 2" class="step-content">
            <h3 class="step-heading">Step 2: Primary Community Manager / Society President</h3>
            <p class="step-subheading">This user will receive full administrative control over this community.</p>

            <div class="form-grid">
              <div class="form-group">
                <label>First Name *</label>
                <input type="text" class="form-control" [(ngModel)]="form.adminFirstName" placeholder="e.g. Rajesh" required />
              </div>

              <div class="form-group">
                <label>Last Name *</label>
                <input type="text" class="form-control" [(ngModel)]="form.adminLastName" placeholder="e.g. Verma" required />
              </div>

              <div class="form-group">
                <label>Admin Work Email *</label>
                <input type="email" class="form-control" [(ngModel)]="form.adminEmail" placeholder="rajesh.verma&#64;willowwoods.org" required />
              </div>

              <div class="form-group">
                <label>Mobile / Phone Number *</label>
                <input type="tel" class="form-control" [(ngModel)]="form.adminPhone" placeholder="+91 98888 77665" required />
              </div>

              <div class="form-group">
                <label>Login Username *</label>
                <input type="text" class="form-control" [(ngModel)]="form.adminUsername" placeholder="e.g. rajesh.admin" required />
              </div>

              <div class="form-group">
                <label>Login Password *</label>
                <input type="password" class="form-control" [(ngModel)]="form.adminPassword" placeholder="Minimum 6 characters" required />
              </div>
            </div>

            <div class="wizard-buttons">
              <button class="btn-secondary" (click)="prevStep()">&larr; Back to Society</button>
              <button class="btn-primary" (click)="nextStep()">Continue to Infrastructure &rarr;</button>
            </div>
          </div>

          <!-- STEP 3: Infrastructure Setup -->
          <div *ngIf="currentStep() === 3" class="step-content">
            <h3 class="step-heading">Step 3: Rapid Infrastructure Provisioning (Optional)</h3>
            <p class="step-subheading">Auto-generate towers, floors, and units so your 2D Floor Matrix is ready immediately.</p>

            <div class="form-grid">
              <div class="form-group">
                <label>Number of Towers / Blocks</label>
                <input type="number" min="0" max="20" class="form-control" [(ngModel)]="form.numberOfTowers" placeholder="e.g. 2" />
                <span class="help-text">Leave 0 if you prefer to build towers manually later</span>
              </div>

              <div class="form-group">
                <label>Floors per Tower</label>
                <input type="number" min="0" max="50" class="form-control" [(ngModel)]="form.floorsPerTower" placeholder="e.g. 10" [disabled]="!form.numberOfTowers" />
              </div>

              <div class="form-group">
                <label>Units per Floor</label>
                <input type="number" min="0" max="20" class="form-control" [(ngModel)]="form.unitsPerFloor" placeholder="e.g. 4" [disabled]="!form.numberOfTowers" />
              </div>

              <div class="form-group">
                <label>Tower Naming Prefix</label>
                <select class="form-control" [(ngModel)]="form.towerPrefix" [disabled]="!form.numberOfTowers">
                  <option value="Tower ">Tower (e.g. Tower A, Tower B)</option>
                  <option value="Block ">Block (e.g. Block A, Block B)</option>
                  <option value="Wing ">Wing (e.g. Wing A, Wing B)</option>
                </select>
              </div>
            </div>

            <div *ngIf="form.numberOfTowers && form.floorsPerTower && form.unitsPerFloor" class="provisioning-preview glass-card">
              <div class="preview-metric">
                <span class="metric-num">{{ form.numberOfTowers }}</span>
                <span class="metric-txt">Towers</span>
              </div>
              <div class="preview-metric">
                <span class="metric-num">{{ form.floorsPerTower }}</span>
                <span class="metric-txt">Floors Each</span>
              </div>
              <div class="preview-metric">
                <span class="metric-num highlight">{{ totalUnitsPreview() }}</span>
                <span class="metric-txt">Total Units Auto-Provisioned</span>
              </div>
            </div>

            <div class="wizard-buttons">
              <button class="btn-secondary" (click)="prevStep()">&larr; Back to Admin</button>
              <button class="btn-primary" (click)="nextStep()">Review & Launch &rarr;</button>
            </div>
          </div>

          <!-- STEP 4: Review & Launch -->
          <div *ngIf="currentStep() === 4" class="step-content">
            <h3 class="step-heading">Step 4: Review & Launch Society</h3>
            <p class="step-subheading">Verify details before provisioning the multi-tenant database records.</p>

            <div class="review-grid">
              <div class="review-card glass-card">
                <h4>🏛️ Society Profile</h4>
                <div class="review-item"><span class="k">Name:</span> <span class="v">{{ form.name }}</span></div>
                <div class="review-item"><span class="k">Location:</span> <span class="v">{{ form.city }}, {{ form.stateProvince }} ({{ form.postalCode }})</span></div>
                <div class="review-item"><span class="k">Email:</span> <span class="v">{{ form.email }}</span></div>
                <div class="review-item"><span class="k">Currency:</span> <span class="v">{{ form.currencyCode }}</span></div>
              </div>

              <div class="review-card glass-card">
                <h4>👤 Primary Administrator</h4>
                <div class="review-item"><span class="k">Name:</span> <span class="v">{{ form.adminFirstName }} {{ form.adminLastName }}</span></div>
                <div class="review-item"><span class="k">Username:</span> <span class="v">{{ form.adminUsername }}</span></div>
                <div class="review-item"><span class="k">Email:</span> <span class="v">{{ form.adminEmail }}</span></div>
                <div class="review-item"><span class="k">Phone:</span> <span class="v">{{ form.adminPhone }}</span></div>
                <div class="review-item"><span class="k">Role:</span> <span class="v badge-role">COMMUNITY_MANAGER</span></div>
              </div>

              <div class="review-card glass-card">
                <h4>🏢 Initial Provisioning</h4>
                <div *ngIf="form.numberOfTowers && form.floorsPerTower && form.unitsPerFloor; else noAutoInfra">
                  <div class="review-item"><span class="k">Towers:</span> <span class="v">{{ form.numberOfTowers }} ({{ form.towerPrefix }}A to {{ form.towerPrefix }}{{ getEndChar() }})</span></div>
                  <div class="review-item"><span class="k">Floors:</span> <span class="v">{{ form.floorsPerTower }} floors per tower</span></div>
                  <div class="review-item"><span class="k">Total Units:</span> <span class="v highlight">{{ totalUnitsPreview() }} units</span></div>
                </div>
                <ng-template #noAutoInfra>
                  <p class="text-muted">No initial towers configured. You can add them later via Building Matrix.</p>
                </ng-template>
              </div>
            </div>

            <div class="wizard-buttons">
              <button class="btn-secondary" (click)="prevStep()" [disabled]="loading()">&larr; Back to Infrastructure</button>
              <button class="btn-primary btn-submit" (click)="submitRegistration()" [disabled]="loading()">
                {{ loading() ? 'Provisioning Society...' : '🚀 Launch Society Operating System' }}
              </button>
            </div>
          </div>

        </div>

      </div>
    </div>
  `,
  styles: [`
    .registration-page {
      min-height: 100vh;
      display: flex;
      justify-content: center;
      align-items: center;
      background: radial-gradient(circle at 10% 20%, #1e1b4b 0%, #0f172a 90%);
      padding: 2.5rem 1.5rem;
    }
    .registration-card {
      width: 100%;
      max-width: 900px;
      padding: 2.5rem;
      border-radius: 16px;
      background: rgba(15, 23, 42, 0.85);
      border: 1px solid rgba(255, 255, 255, 0.1);
      box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
    }
    .card-header {
      margin-bottom: 2rem;
      text-align: center;
    }
    .brand-row {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.5rem;
      margin-bottom: 0.5rem;
    }
    .logo-icon {
      font-size: 1.8rem;
    }
    .brand-title {
      font-weight: 800;
      font-size: 1.1rem;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      color: #818cf8;
    }
    .page-title {
      font-size: 1.75rem;
      font-weight: 800;
      color: #f8fafc;
      margin-bottom: 0.35rem;
    }
    .page-subtitle {
      color: #94a3b8;
      font-size: 0.95rem;
    }

    /* Stepper */
    .stepper {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 2.5rem;
      padding: 0 1rem;
    }
    .step-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.5rem;
      cursor: pointer;
    }
    .step-circle {
      width: 38px;
      height: 38px;
      border-radius: 50%;
      background: #1e293b;
      border: 2px solid #334155;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 700;
      font-size: 0.9rem;
      color: #64748b;
      transition: all 0.2s ease;
    }
    .step-item.active .step-circle {
      border-color: #6366f1;
      background: #4f46e5;
      color: #ffffff;
      box-shadow: 0 0 15px rgba(99, 102, 241, 0.5);
    }
    .step-item.completed .step-circle {
      border-color: #10b981;
      background: #059669;
      color: #ffffff;
    }
    .step-title {
      font-size: 0.8rem;
      font-weight: 600;
      color: #64748b;
    }
    .step-item.active .step-title {
      color: #818cf8;
    }
    .step-line {
      flex: 1;
      height: 2px;
      background: #334155;
      margin: 0 0.75rem 1.25rem;
    }
    .step-line.active {
      background: #10b981;
    }

    /* Form Styles */
    .step-heading {
      font-size: 1.25rem;
      font-weight: 700;
      color: #e2e8f0;
      margin-bottom: 0.25rem;
    }
    .step-subheading {
      font-size: 0.875rem;
      color: #94a3b8;
      margin-bottom: 1.5rem;
    }
    .form-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 1.25rem;
      margin-bottom: 2rem;
    }
    .full-width {
      grid-column: span 2;
    }
    .form-group label {
      display: block;
      font-size: 0.8125rem;
      font-weight: 600;
      color: #cbd5e1;
      margin-bottom: 0.35rem;
    }
    .form-control {
      width: 100%;
      padding: 0.65rem 0.85rem;
      background: rgba(15, 23, 42, 0.6);
      border: 1px solid rgba(255, 255, 255, 0.15);
      border-radius: 8px;
      color: #ffffff;
      font-size: 0.9rem;
      box-sizing: border-box;
      outline: none;
      transition: border-color 0.2s;
    }
    .form-control:focus {
      border-color: #818cf8;
      box-shadow: 0 0 0 2px rgba(129, 140, 248, 0.2);
    }
    .form-control:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
    .help-text {
      display: block;
      font-size: 0.75rem;
      color: #64748b;
      margin-top: 0.25rem;
    }

    /* Provisioning Preview */
    .provisioning-preview {
      display: flex;
      justify-content: space-around;
      padding: 1.25rem;
      margin-bottom: 2rem;
      background: rgba(30, 41, 59, 0.6);
      border: 1px solid rgba(99, 102, 241, 0.3);
      border-radius: 12px;
    }
    .preview-metric {
      display: flex;
      flex-direction: column;
      align-items: center;
    }
    .metric-num {
      font-size: 1.75rem;
      font-weight: 800;
      color: #e2e8f0;
    }
    .metric-num.highlight {
      color: #38bdf8;
    }
    .metric-txt {
      font-size: 0.75rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: #94a3b8;
    }

    /* Review Grid */
    .review-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 1.25rem;
      margin-bottom: 2rem;
    }
    .review-card {
      padding: 1.25rem;
      border-radius: 12px;
      background: rgba(30, 41, 59, 0.5);
      border: 1px solid rgba(255, 255, 255, 0.08);
    }
    .review-card h4 {
      font-size: 0.95rem;
      font-weight: 700;
      color: #818cf8;
      margin-bottom: 0.85rem;
    }
    .review-item {
      display: flex;
      justify-content: space-between;
      margin-bottom: 0.45rem;
      font-size: 0.8125rem;
    }
    .review-item .k {
      color: #94a3b8;
    }
    .review-item .v {
      color: #f1f5f9;
      font-weight: 600;
      text-align: right;
    }
    .badge-role {
      background: rgba(99, 102, 241, 0.2);
      color: #818cf8;
      padding: 0.15rem 0.45rem;
      border-radius: 4px;
      font-size: 0.75rem;
    }

    /* Buttons */
    .wizard-buttons {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: 1.5rem;
      border-top: 1px solid rgba(255, 255, 255, 0.1);
      padding-top: 1.5rem;
    }
    .btn-primary {
      padding: 0.75rem 1.5rem;
      background: linear-gradient(135deg, #4f46e5, #6366f1);
      color: #ffffff;
      border: none;
      border-radius: 8px;
      font-size: 0.95rem;
      font-weight: 700;
      cursor: pointer;
      transition: all 0.2s;
    }
    .btn-primary:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 10px 15px -3px rgba(79, 70, 229, 0.4);
    }
    .btn-secondary {
      padding: 0.75rem 1.25rem;
      background: rgba(30, 41, 59, 0.6);
      color: #cbd5e1;
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 8px;
      font-size: 0.9rem;
      font-weight: 600;
      text-decoration: none;
      cursor: pointer;
    }
    .btn-secondary:hover:not(:disabled) {
      background: rgba(51, 65, 85, 0.8);
      color: #ffffff;
    }
    .btn-submit {
      background: linear-gradient(135deg, #059669, #10b981);
    }
    .alert-error {
      background: rgba(239, 68, 68, 0.15);
      border: 1px solid rgba(239, 68, 68, 0.4);
      color: #fca5a5;
      padding: 0.75rem 1rem;
      border-radius: 8px;
      margin-bottom: 1.5rem;
      font-size: 0.875rem;
    }

    /* Success Screen */
    .success-screen {
      text-align: center;
      padding: 2rem 1rem;
    }
    .success-icon {
      font-size: 3.5rem;
      margin-bottom: 1rem;
    }
    .success-title {
      font-size: 1.85rem;
      font-weight: 800;
      color: #f8fafc;
      margin-bottom: 0.5rem;
    }
    .success-desc {
      color: #94a3b8;
      font-size: 1rem;
      margin-bottom: 2rem;
    }
    .details-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 1rem;
      max-width: 700px;
      margin: 0 auto 2.5rem;
    }
    .detail-box {
      background: rgba(30, 41, 59, 0.6);
      border: 1px solid rgba(255, 255, 255, 0.08);
      padding: 1rem;
      border-radius: 10px;
      display: flex;
      flex-direction: column;
      gap: 0.35rem;
    }
    .detail-label {
      font-size: 0.75rem;
      color: #64748b;
      text-transform: uppercase;
      font-weight: 600;
    }
    .detail-value {
      font-size: 1.1rem;
      font-weight: 700;
      color: #f1f5f9;
    }
    .badge-code {
      color: #38bdf8;
    }
    .success-actions {
      display: flex;
      justify-content: center;
      gap: 1.25rem;
    }
  `]
})
export class CommunityRegistrationComponent {
  private communityService = inject(CommunityService);
  private authService = inject(AuthService);
  private router = inject(Router);

  currentStep = signal<number>(1);
  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  registeredResult = signal<CommunityRegistrationResponse | null>(null);

  form: CommunityRegistrationRequest = {
    name: '',
    code: '',
    legalName: '',
    registrationNumber: '',
    taxIdentifier: '',
    addressLine1: '',
    addressLine2: '',
    city: '',
    stateProvince: '',
    postalCode: '',
    countryCode: 'IND',
    phone: '',
    email: '',
    currencyCode: 'INR',
    timezone: 'Asia/Kolkata',
    adminFirstName: '',
    adminLastName: '',
    adminEmail: '',
    adminPhone: '',
    adminUsername: '',
    adminPassword: '',
    numberOfTowers: 2,
    floorsPerTower: 8,
    unitsPerFloor: 4,
    towerPrefix: 'Tower '
  };

  totalUnitsPreview(): number {
    return (this.form.numberOfTowers || 0) * (this.form.floorsPerTower || 0) * (this.form.unitsPerFloor || 0);
  }

  getEndChar(): string {
    const count = this.form.numberOfTowers || 1;
    return String.fromCharCode('A'.charCodeAt(0) + count - 1);
  }

  goToStep(step: number): void {
    if (step < this.currentStep()) {
      this.currentStep.set(step);
    } else if (this.validateCurrentStep()) {
      this.currentStep.set(step);
    }
  }

  nextStep(): void {
    if (this.validateCurrentStep()) {
      this.errorMessage.set(null);
      this.currentStep.update(s => s + 1);
    }
  }

  prevStep(): void {
    this.errorMessage.set(null);
    this.currentStep.update(s => s - 1);
  }

  validateCurrentStep(): boolean {
    if (this.currentStep() === 1) {
      if (!this.form.name.trim()) {
        this.errorMessage.set('Please enter the residential complex / society name.');
        return false;
      }
      if (!this.form.addressLine1.trim() || !this.form.city.trim() || !this.form.stateProvince.trim() || !this.form.postalCode.trim()) {
        this.errorMessage.set('Please complete the society address fields (Line 1, City, State, Postal code).');
        return false;
      }
      if (!this.form.email.trim() || !this.form.email.includes('@')) {
        this.errorMessage.set('Please provide a valid official society email address.');
        return false;
      }
    } else if (this.currentStep() === 2) {
      if (!this.form.adminFirstName.trim() || !this.form.adminLastName.trim()) {
        this.errorMessage.set('Please enter the administrator first and last name.');
        return false;
      }
      if (!this.form.adminEmail.trim() || !this.form.adminEmail.includes('@')) {
        this.errorMessage.set('Please provide a valid administrator email address.');
        return false;
      }
      if (!this.form.adminPhone.trim()) {
        this.errorMessage.set('Please provide the administrator contact phone number.');
        return false;
      }
      if (!this.form.adminUsername.trim() || this.form.adminUsername.length < 3) {
        this.errorMessage.set('Username must be at least 3 characters long.');
        return false;
      }
      if (!this.form.adminPassword || this.form.adminPassword.length < 6) {
        this.errorMessage.set('Password must be at least 6 characters long.');
        return false;
      }
    }
    return true;
  }

  submitRegistration(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.communityService.registerCommunity(this.form).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.success && res.data) {
          this.registeredResult.set(res.data);
        } else {
          this.errorMessage.set(res.message || 'Registration failed');
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to register society. Please check details and try again.');
      }
    });
  }

  enterNewDashboard(result: CommunityRegistrationResponse): void {
    // If token returned, log in automatically as the newly registered community manager
    if (result.token) {
      localStorage.setItem('scos_auth_token', result.token);
      const user = {
        id: result.adminUserId,
        username: result.adminUsername,
        email: this.form.adminEmail,
        firstName: this.form.adminFirstName,
        lastName: this.form.adminLastName,
        roles: ['COMMUNITY_MANAGER'],
        active: true,
        verified: true,
        activeCommunityId: result.communityId
      };
      localStorage.setItem('scos_current_user', JSON.stringify(user));
      localStorage.setItem('scos_active_community_id', result.communityId);
      
      this.communityService.setActiveCommunity({
        id: result.communityId,
        code: result.communityCode,
        name: result.communityName,
        city: this.form.city,
        stateProvince: this.form.stateProvince,
        countryCode: this.form.countryCode || 'IND',
        email: this.form.email,
        currencyCode: this.form.currencyCode || 'INR',
        status: 'ACTIVE'
      });

      this.router.navigate(['/dashboard']);
    } else {
      this.router.navigate(['/login']);
    }
  }
}
