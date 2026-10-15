import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth/auth.service';
import {
  BookingSlot,
  BookingStatus,
  CancelBookingRequest,
  CreateBookingRequest,
  CreateFacilityRequest,
  Facility,
  FacilityBooking,
  FacilityCategory,
  FacilitySummary,
  ReviewBookingRequest
} from '../../core/models/facility.model';
import { FacilityService } from './facility.service';

@Component({
  selector: 'scos-facility-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="facility-console">
      <header class="page-header">
        <div>
          <p class="eyebrow">COMMUNITY AMENITIES & RECREATIONAL INFRASTRUCTURE</p>
          <h1>Facility Bookings & Amenity Reservations</h1>
          <p class="muted">
            Reserve clubhouses, swimming pools, sports courts, and coworking spaces with real-time schedule conflict protection.
          </p>
        </div>
        <div class="header-actions">
          <button
            *ngIf="isManagerOrAdmin()"
            class="primary"
            (click)="openCreateFacilityModal()"
          >
            ＋ Add New Facility
          </button>
        </div>
      </header>

      <div class="alert alert-error" *ngIf="errorMessage">
        <span>⚠️ {{ errorMessage }}</span>
        <button class="close-btn" (click)="errorMessage = ''">✕</button>
      </div>

      <div class="alert alert-success" *ngIf="successMessage">
        <span>✅ {{ successMessage }}</span>
        <button class="close-btn" (click)="successMessage = ''">✕</button>
      </div>

      <!-- KPI Summary Cards -->
      <div class="kpis" *ngIf="summary as s">
        <article class="kpi-card">
          <span class="kpi-label">Total Facilities</span>
          <strong class="kpi-value">{{ s.totalFacilities }}</strong>
        </article>
        <article class="kpi-card success">
          <span class="kpi-label">Active Amenities</span>
          <strong class="kpi-value">{{ s.activeFacilities }}</strong>
        </article>
        <article class="kpi-card accent">
          <span class="kpi-label">Confirmed Bookings</span>
          <strong class="kpi-value">{{ s.totalBookings }}</strong>
        </article>
        <article class="kpi-card info">
          <span class="kpi-label">Reservations Today</span>
          <strong class="kpi-value">{{ s.bookingsToday }}</strong>
        </article>
        <article class="kpi-card warning" [class.highlight]="s.pendingApprovals > 0">
          <span class="kpi-label">Pending Approval</span>
          <strong class="kpi-value">{{ s.pendingApprovals }}</strong>
        </article>
      </div>

      <!-- Navigation Tabs -->
      <div class="tabs-nav">
        <button
          class="tab-btn"
          [class.active]="activeTab === 'catalog'"
          (click)="switchTab('catalog')"
        >
          🏊 Explore & Book Facilities
        </button>
        <button
          class="tab-btn"
          [class.active]="activeTab === 'my-bookings'"
          (click)="switchTab('my-bookings')"
        >
          📋 My Reservations
          <span class="badge" *ngIf="myBookings.length > 0">{{ myBookings.length }}</span>
        </button>
        <button
          *ngIf="isManagerOrAdmin()"
          class="tab-btn"
          [class.active]="activeTab === 'approvals'"
          (click)="switchTab('approvals')"
        >
          🛡️ Approval Desk
          <span class="badge alert-badge" *ngIf="pendingApprovals.length > 0">{{ pendingApprovals.length }}</span>
        </button>
      </div>

      <!-- TAB 1: Facilities Catalog -->
      <div *ngIf="activeTab === 'catalog'" class="tab-content">
        <div class="toolbar">
          <input
            class="field search"
            [(ngModel)]="searchQuery"
            placeholder="Search amenity by name, category, or location..."
            aria-label="Search facilities"
          />
          <select class="field" [(ngModel)]="categoryFilter" (change)="loadFacilities()">
            <option value="">All Categories</option>
            <option *ngFor="let cat of categories" [value]="cat">
              {{ categoryIcon(cat) }} {{ formatCategory(cat) }}
            </option>
          </select>
          <button class="secondary" (click)="refreshAll()">↻ Refresh</button>
        </div>

        <div class="loading" *ngIf="loading">Loading community facilities…</div>

        <div class="facility-grid" *ngIf="!loading">
          <div
            *ngFor="let facility of filteredFacilities()"
            class="facility-card glass-panel"
          >
            <div class="card-header">
              <span class="category-tag">
                {{ categoryIcon(facility.category) }} {{ formatCategory(facility.category) }}
              </span>
              <span class="approval-tag" *ngIf="facility.requiresApproval">Approval Req.</span>
              <span class="instant-tag" *ngIf="!facility.requiresApproval">Instant Book</span>
            </div>

            <div class="card-body">
              <div class="code-badge">{{ facility.facilityCode }}</div>
              <h3 class="facility-name">{{ facility.name }}</h3>
              <p class="facility-desc">{{ facility.description || 'Community recreational space.' }}</p>

              <div class="meta-row">
                <span class="meta-item">
                  <span class="meta-icon">📍</span>
                  {{ facility.locationDetails || 'Common Area' }}
                </span>
                <span class="meta-item">
                  <span class="meta-icon">👥</span>
                  Max {{ facility.capacity }} Persons
                </span>
              </div>

              <div class="meta-row">
                <span class="meta-item">
                  <span class="meta-icon">⏰</span>
                  {{ facility.operatingHoursStart }} - {{ facility.operatingHoursEnd }}
                </span>
                <span class="meta-item">
                  <span class="meta-icon">⏱️</span>
                  {{ facility.minBookingHours }}-{{ facility.maxBookingHours }} hrs slot
                </span>
              </div>

              <div class="price-container">
                <div class="hourly-rate">
                  <span class="rate-amount">{{ facility.hourlyFee > 0 ? ('₹' + facility.hourlyFee) : 'Free' }}</span>
                  <span class="rate-unit" *ngIf="facility.hourlyFee > 0">/ hour</span>
                </div>
                <div class="deposit-tag" *ngIf="facility.securityDeposit > 0">
                  Deposit: ₹{{ facility.securityDeposit }}
                </div>
              </div>

              <p class="rules-snippet" *ngIf="facility.rulesAndRegulations">
                <small><strong>Rules:</strong> {{ facility.rulesAndRegulations }}</small>
              </p>
            </div>

            <div class="card-footer">
              <button
                class="primary book-btn"
                [disabled]="!facility.isActive"
                (click)="openBookingModal(facility)"
              >
                {{ facility.isActive ? '📅 Book Time Slot' : 'Temporarily Closed' }}
              </button>
            </div>
          </div>
        </div>

        <div class="empty-state" *ngIf="!loading && filteredFacilities().length === 0">
          <p>No facilities match your search criteria.</p>
        </div>
      </div>

      <!-- TAB 2: My Bookings -->
      <div *ngIf="activeTab === 'my-bookings'" class="tab-content">
        <div class="toolbar">
          <span class="table-title">Your Active and Past Amenity Bookings</span>
          <button class="secondary" (click)="loadMyBookings()">↻ Refresh</button>
        </div>

        <div class="table-wrap" *ngIf="myBookings.length > 0">
          <table>
            <thead>
              <tr>
                <th>Booking Ref</th>
                <th>Facility</th>
                <th>Date & Time Slot</th>
                <th>Attendees</th>
                <th>Purpose</th>
                <th>Total Fee</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let booking of myBookings">
                <td><strong>{{ booking.bookingReference }}</strong></td>
                <td>
                  <span class="table-facility-name">{{ booking.facilityName }}</span>
                  <small class="muted d-block">{{ formatCategory(booking.facilityCategory) }}</small>
                </td>
                <td>
                  <div class="slot-time-block">
                    <span>📅 {{ booking.bookingDate }}</span>
                    <small class="muted">{{ formatTime(booking.startTime) }} - {{ formatTime(booking.endTime) }}</small>
                  </div>
                </td>
                <td>{{ booking.attendeesCount }} guests</td>
                <td><small>{{ booking.purpose || 'Personal recreation' }}</small></td>
                <td>
                  <span class="fee-pill">₹{{ booking.totalFee }}</span>
                  <small class="muted d-block" *ngIf="booking.depositAmount > 0">
                    Dep: ₹{{ booking.depositAmount }}
                  </small>
                </td>
                <td>
                  <span class="status-badge" [ngClass]="booking.status.toLowerCase()">
                    {{ formatStatus(booking.status) }}
                  </span>
                </td>
                <td>
                  <button
                    *ngIf="canCancel(booking)"
                    class="secondary danger-btn"
                    (click)="openCancelModal(booking)"
                  >
                    Cancel
                  </button>
                  <span *ngIf="!canCancel(booking)" class="muted text-sm">—</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="empty-state" *ngIf="myBookings.length === 0 && !loading">
          <span class="empty-icon">🏖️</span>
          <h3>No Reservations Found</h3>
          <p>You have not booked any amenities yet. Check the catalog to reserve a slot!</p>
          <button class="primary" (click)="switchTab('catalog')">Browse Amenities</button>
        </div>
      </div>

      <!-- TAB 3: Approval Desk (Manager/Admin Only) -->
      <div *ngIf="activeTab === 'approvals'" class="tab-content">
        <div class="toolbar">
          <span class="table-title">Pending Facility Reservations Awaiting Society Approval</span>
          <button class="secondary" (click)="loadAllBookings()">↻ Refresh</button>
        </div>

        <div class="table-wrap" *ngIf="pendingApprovals.length > 0">
          <table>
            <thead>
              <tr>
                <th>Reference</th>
                <th>Facility & Category</th>
                <th>Slot Time</th>
                <th>Guest Count</th>
                <th>Event Purpose</th>
                <th>Fee & Deposit</th>
                <th>Review Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let booking of pendingApprovals">
                <td><strong>{{ booking.bookingReference }}</strong></td>
                <td>
                  <strong>{{ booking.facilityName }}</strong>
                  <small class="muted d-block">{{ formatCategory(booking.facilityCategory) }}</small>
                </td>
                <td>
                  <span>📅 {{ booking.bookingDate }}</span>
                  <small class="muted d-block">{{ formatTime(booking.startTime) }} - {{ formatTime(booking.endTime) }}</small>
                </td>
                <td>{{ booking.attendeesCount }} persons</td>
                <td>{{ booking.purpose || 'Not specified' }}</td>
                <td>
                  <strong>₹{{ booking.totalFee }}</strong>
                  <small class="muted d-block" *ngIf="booking.depositAmount > 0">
                    Deposit: ₹{{ booking.depositAmount }}
                  </small>
                </td>
                <td>
                  <div class="action-btn-group">
                    <button class="action-btn approve-btn" (click)="reviewBooking(booking, true)">
                      ✓ Approve
                    </button>
                    <button class="action-btn reject-btn" (click)="openRejectModal(booking)">
                      ✕ Reject
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="empty-state" *ngIf="pendingApprovals.length === 0">
          <span class="empty-icon">🎉</span>
          <h3>All Clear!</h3>
          <p>There are no reservations currently waiting for approval.</p>
        </div>
      </div>

      <!-- MODAL: Book Facility Slot -->
      <div class="modal-backdrop" *ngIf="bookingModalFacility" (click)="closeBookingModal()">
        <div class="modal-card glass-panel" (click)="$event.stopPropagation()">
          <header class="modal-header">
            <div>
              <p class="eyebrow">RESERVE AMENITY</p>
              <h2>Book {{ bookingModalFacility.name }}</h2>
            </div>
            <button class="close-x" (click)="closeBookingModal()">✕</button>
          </header>

          <form (ngSubmit)="submitBooking()" class="modal-form">
            <div class="form-row">
              <label>
                Reservation Date *
                <input
                  type="date"
                  class="field"
                  [(ngModel)]="bookingDate"
                  name="bookingDate"
                  [min]="minDateString"
                  (change)="onDateChange()"
                  required
                />
              </label>
              <label>
                Expected Attendees *
                <input
                  type="number"
                  class="field"
                  [(ngModel)]="attendeesCount"
                  name="attendeesCount"
                  [max]="bookingModalFacility.capacity"
                  min="1"
                  required
                />
              </label>
            </div>

            <!-- Existing Bookings for Selected Date -->
            <div class="slots-overview" *ngIf="bookedSlots.length > 0">
              <p class="slots-title">⚠️ Existing Reservations on {{ bookingDate }}:</p>
              <div class="slots-list">
                <span class="slot-pill booked" *ngFor="let s of bookedSlots">
                  🔒 {{ formatTime(s.startTime) }} - {{ formatTime(s.endTime) }} (Booked)
                </span>
              </div>
            </div>

            <div class="form-row">
              <label>
                Start Time *
                <input
                  type="time"
                  class="field"
                  [(ngModel)]="startTimeStr"
                  name="startTime"
                  (change)="calculateFee()"
                  required
                />
              </label>
              <label>
                End Time *
                <input
                  type="time"
                  class="field"
                  [(ngModel)]="endTimeStr"
                  name="endTime"
                  (change)="calculateFee()"
                  required
                />
              </label>
            </div>

            <label>
              Event Purpose / Description
              <textarea
                class="field"
                [(ngModel)]="bookingPurpose"
                name="bookingPurpose"
                rows="2"
                placeholder="e.g. Birthday celebration, morning training, client meeting..."
              ></textarea>
            </label>

            <!-- Price Breakdown Box -->
            <div class="price-breakdown glass-panel">
              <div class="breakdown-row">
                <span>Calculated Duration:</span>
                <strong>{{ estimatedDurationHours | number:'1.1-1' }} hour(s)</strong>
              </div>
              <div class="breakdown-row">
                <span>Amenity Usage Fee:</span>
                <strong>₹{{ calculatedFee | number:'1.2-2' }}</strong>
              </div>
              <div class="breakdown-row" *ngIf="bookingModalFacility.securityDeposit > 0">
                <span>Refundable Deposit:</span>
                <strong>₹{{ bookingModalFacility.securityDeposit | number:'1.2-2' }}</strong>
              </div>
              <div class="breakdown-row total-row">
                <span>Estimated Total Payable:</span>
                <span class="total-amount">
                  ₹{{ (calculatedFee + (bookingModalFacility.securityDeposit || 0)) | number:'1.2-2' }}
                </span>
              </div>
            </div>

            <footer class="modal-footer">
              <button type="button" class="secondary" (click)="closeBookingModal()">Cancel</button>
              <button type="submit" class="primary" [disabled]="submittingBooking">
                {{ submittingBooking ? 'Submitting Reservation…' : (bookingModalFacility.requiresApproval ? 'Submit for Approval' : 'Confirm & Reserve Slot') }}
              </button>
            </footer>
          </form>
        </div>
      </div>

      <!-- MODAL: Reject Booking Reason -->
      <div class="modal-backdrop" *ngIf="rejectModalBooking" (click)="closeRejectModal()">
        <div class="modal-card modal-sm glass-panel" (click)="$event.stopPropagation()">
          <header class="modal-header">
            <h3>Reject Booking {{ rejectModalBooking.bookingReference }}</h3>
            <button class="close-x" (click)="closeRejectModal()">✕</button>
          </header>
          <div class="modal-form">
            <label>
              Reason for Rejection *
              <textarea
                class="field"
                [(ngModel)]="rejectReason"
                rows="3"
                placeholder="e.g. Facility under scheduled maintenance, exceeds quiet hour limit..."
              ></textarea>
            </label>
            <footer class="modal-footer">
              <button class="secondary" (click)="closeRejectModal()">Cancel</button>
              <button class="primary danger-btn" (click)="confirmReject()">Reject Booking</button>
            </footer>
          </div>
        </div>
      </div>

      <!-- MODAL: Cancel Booking Reason -->
      <div class="modal-backdrop" *ngIf="cancelModalBooking" (click)="closeCancelModal()">
        <div class="modal-card modal-sm glass-panel" (click)="$event.stopPropagation()">
          <header class="modal-header">
            <h3>Cancel Reservation {{ cancelModalBooking.bookingReference }}</h3>
            <button class="close-x" (click)="closeCancelModal()">✕</button>
          </header>
          <div class="modal-form">
            <p class="muted">
              Cancellation is subject to the facility policy (cutoff: {{ cancelModalBooking.facilityName }}).
            </p>
            <label>
              Reason for Cancellation *
              <textarea
                class="field"
                [(ngModel)]="cancelReason"
                rows="3"
                placeholder="e.g. Schedule conflict, family plans rescheduled..."
              ></textarea>
            </label>
            <footer class="modal-footer">
              <button class="secondary" (click)="closeCancelModal()">Keep Booking</button>
              <button class="primary danger-btn" (click)="confirmCancel()">Confirm Cancellation</button>
            </footer>
          </div>
        </div>
      </div>

      <!-- MODAL: Add New Facility (Manager Only) -->
      <div class="modal-backdrop" *ngIf="showCreateFacilityModal" (click)="closeCreateFacilityModal()">
        <div class="modal-card glass-panel" (click)="$event.stopPropagation()">
          <header class="modal-header">
            <div>
              <p class="eyebrow">ESTATE CATALOG</p>
              <h2>Add New Facility / Amenity</h2>
            </div>
            <button class="close-x" (click)="closeCreateFacilityModal()">✕</button>
          </header>
          <form (ngSubmit)="submitCreateFacility()" class="modal-form">
            <div class="form-row">
              <label>
                Facility Code *
                <input class="field" [(ngModel)]="newFac.facilityCode" name="facCode" placeholder="FAC-GYM-01" required />
              </label>
              <label>
                Amenity Name *
                <input class="field" [(ngModel)]="newFac.name" name="facName" placeholder="High-Intensity Gym" required />
              </label>
            </div>
            <div class="form-row">
              <label>
                Category *
                <select class="field" [(ngModel)]="newFac.category" name="facCat" required>
                  <option *ngFor="let c of categories" [value]="c">{{ formatCategory(c) }}</option>
                </select>
              </label>
              <label>
                Location *
                <input class="field" [(ngModel)]="newFac.locationDetails" name="facLoc" placeholder="Clubhouse 2nd Floor" />
              </label>
            </div>
            <div class="form-row">
              <label>
                Capacity (Persons) *
                <input type="number" class="field" [(ngModel)]="newFac.capacity" name="facCap" min="1" required />
              </label>
              <label>
                Hourly Fee (₹) *
                <input type="number" class="field" [(ngModel)]="newFac.hourlyFee" name="facFee" min="0" required />
              </label>
              <label>
                Security Deposit (₹)
                <input type="number" class="field" [(ngModel)]="newFac.securityDeposit" name="facDep" min="0" />
              </label>
            </div>
            <div class="form-row">
              <label>
                Opens At
                <input type="time" class="field" [(ngModel)]="newFac.operatingHoursStart" name="openTime" required />
              </label>
              <label>
                Closes At
                <input type="time" class="field" [(ngModel)]="newFac.operatingHoursEnd" name="closeTime" required />
              </label>
              <label>
                Cancellation Cutoff (Hrs)
                <input type="number" class="field" [(ngModel)]="newFac.cancellationCutoffHours" name="cutoff" min="0" />
              </label>
            </div>
            <div class="checkbox-row">
              <label class="checkbox-label">
                <input type="checkbox" [(ngModel)]="newFac.requiresApproval" name="reqAppr" />
                Require Community Management Approval for Reservations
              </label>
            </div>
            <label>
              Description & Amenities
              <textarea class="field" [(ngModel)]="newFac.description" name="facDesc" rows="2" placeholder="Features, equipment, audio setup..."></textarea>
            </label>
            <label>
              Rules & Regulations
              <textarea class="field" [(ngModel)]="newFac.rulesAndRegulations" name="facRules" rows="2" placeholder="Shoes policy, music cutoff, cleanup rules..."></textarea>
            </label>
            <footer class="modal-footer">
              <button type="button" class="secondary" (click)="closeCreateFacilityModal()">Cancel</button>
              <button type="submit" class="primary">Register Facility</button>
            </footer>
          </form>
        </div>
      </div>
    </section>
  `,
  styles: [`
    :host {
      display: block;
      padding: 1.5rem;
      color: #f1f5f9;
      font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
    }

    .facility-console {
      max-width: 1400px;
      margin: 0 auto;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 1.5rem;
      gap: 1rem;
    }

    .eyebrow {
      font-size: 0.75rem;
      letter-spacing: 0.1em;
      text-transform: uppercase;
      color: #38bdf8;
      font-weight: 700;
      margin: 0 0 0.25rem 0;
    }

    h1 {
      font-size: 1.85rem;
      font-weight: 800;
      margin: 0 0 0.5rem 0;
      background: linear-gradient(135deg, #ffffff 0%, #cbd5e1 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }

    .muted {
      color: #94a3b8;
      font-size: 0.9rem;
      margin: 0;
    }

    .primary {
      background: linear-gradient(135deg, #2563eb, #1d4ed8);
      color: #ffffff;
      border: 1px solid rgba(255, 255, 255, 0.15);
      border-radius: 8px;
      padding: 0.65rem 1.25rem;
      font-weight: 600;
      cursor: pointer;
      box-shadow: 0 4px 14px rgba(37, 99, 235, 0.35);
      transition: all 0.2s ease;
    }

    .primary:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 6px 20px rgba(37, 99, 235, 0.5);
    }

    .primary:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .secondary {
      background: rgba(30, 41, 59, 0.7);
      color: #cbd5e1;
      border: 1px solid rgba(148, 163, 184, 0.2);
      border-radius: 8px;
      padding: 0.55rem 1rem;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.15s ease;
    }

    .secondary:hover {
      background: rgba(51, 65, 85, 0.8);
      color: #ffffff;
    }

    .danger-btn {
      color: #f87171 !important;
      border-color: rgba(239, 68, 68, 0.3) !important;
    }

    .danger-btn:hover {
      background: rgba(239, 68, 68, 0.2) !important;
    }

    .alert {
      padding: 0.75rem 1rem;
      border-radius: 8px;
      margin-bottom: 1.25rem;
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 0.9rem;
    }

    .alert-error {
      background: rgba(239, 68, 68, 0.15);
      border: 1px solid rgba(239, 68, 68, 0.4);
      color: #fca5a5;
    }

    .alert-success {
      background: rgba(34, 197, 94, 0.15);
      border: 1px solid rgba(34, 197, 94, 0.4);
      color: #86efac;
    }

    .close-btn {
      background: none;
      border: none;
      color: inherit;
      cursor: pointer;
      font-size: 1rem;
    }

    /* KPIs */
    .kpis {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 1rem;
      margin-bottom: 1.5rem;
    }

    .kpi-card {
      background: rgba(30, 41, 59, 0.6);
      backdrop-filter: blur(12px);
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 12px;
      padding: 1rem 1.25rem;
      display: flex;
      flex-direction: column;
      gap: 0.35rem;
      transition: transform 0.2s ease;
    }

    .kpi-card:hover {
      transform: translateY(-2px);
    }

    .kpi-label {
      font-size: 0.75rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: #94a3b8;
      font-weight: 600;
    }

    .kpi-value {
      font-size: 1.75rem;
      font-weight: 800;
      color: #ffffff;
    }

    .kpi-card.success .kpi-value { color: #4ade80; }
    .kpi-card.accent .kpi-value { color: #38bdf8; }
    .kpi-card.info .kpi-value { color: #a78bfa; }
    .kpi-card.warning .kpi-value { color: #fbbf24; }
    .kpi-card.highlight {
      border-color: rgba(251, 191, 36, 0.4);
      background: rgba(251, 191, 36, 0.1);
    }

    /* Tabs */
    .tabs-nav {
      display: flex;
      gap: 0.5rem;
      border-bottom: 1px solid rgba(148, 163, 184, 0.15);
      margin-bottom: 1.5rem;
    }

    .tab-btn {
      background: none;
      border: none;
      color: #94a3b8;
      font-size: 0.95rem;
      font-weight: 600;
      padding: 0.75rem 1.25rem;
      cursor: pointer;
      border-bottom: 2px solid transparent;
      transition: all 0.15s ease;
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .tab-btn:hover {
      color: #f1f5f9;
    }

    .tab-btn.active {
      color: #38bdf8;
      border-bottom-color: #38bdf8;
    }

    .badge {
      background: rgba(56, 189, 248, 0.2);
      color: #38bdf8;
      padding: 0.15rem 0.45rem;
      border-radius: 9999px;
      font-size: 0.75rem;
      font-weight: 700;
    }

    .alert-badge {
      background: rgba(239, 68, 68, 0.25);
      color: #f87171;
    }

    /* Toolbar */
    .toolbar {
      display: flex;
      flex-wrap: wrap;
      gap: 0.75rem;
      align-items: center;
      margin-bottom: 1.5rem;
    }

    .table-title {
      font-weight: 600;
      color: #cbd5e1;
      font-size: 1rem;
      flex-grow: 1;
    }

    .field {
      background: rgba(15, 23, 42, 0.7);
      border: 1px solid rgba(148, 163, 184, 0.2);
      border-radius: 8px;
      padding: 0.55rem 0.85rem;
      color: #f1f5f9;
      font-size: 0.9rem;
      outline: none;
      transition: border-color 0.15s ease;
    }

    .field:focus {
      border-color: #38bdf8;
      box-shadow: 0 0 0 2px rgba(56, 189, 248, 0.2);
    }

    .field.search {
      flex: 1;
      min-width: 250px;
    }

    /* Facility Grid */
    .facility-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
      gap: 1.5rem;
    }

    .facility-card {
      background: rgba(30, 41, 59, 0.65);
      backdrop-filter: blur(14px);
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 16px;
      padding: 1.25rem;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      transition: all 0.25s ease;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.25);
    }

    .facility-card:hover {
      transform: translateY(-4px);
      border-color: rgba(56, 189, 248, 0.3);
      box-shadow: 0 10px 30px rgba(0, 0, 0, 0.4);
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 0.75rem;
    }

    .category-tag {
      background: rgba(56, 189, 248, 0.15);
      color: #7dd3fc;
      border: 1px solid rgba(56, 189, 248, 0.3);
      padding: 0.25rem 0.6rem;
      border-radius: 6px;
      font-size: 0.75rem;
      font-weight: 600;
    }

    .approval-tag {
      background: rgba(251, 191, 36, 0.15);
      color: #fde047;
      border: 1px solid rgba(251, 191, 36, 0.3);
      padding: 0.2rem 0.5rem;
      border-radius: 6px;
      font-size: 0.7rem;
      font-weight: 600;
    }

    .instant-tag {
      background: rgba(34, 197, 94, 0.15);
      color: #86efac;
      border: 1px solid rgba(34, 197, 94, 0.3);
      padding: 0.2rem 0.5rem;
      border-radius: 6px;
      font-size: 0.7rem;
      font-weight: 600;
    }

    .code-badge {
      font-size: 0.75rem;
      color: #94a3b8;
      font-family: monospace;
      font-weight: 700;
      margin-bottom: 0.25rem;
    }

    .facility-name {
      font-size: 1.2rem;
      font-weight: 700;
      color: #ffffff;
      margin: 0 0 0.5rem 0;
    }

    .facility-desc {
      font-size: 0.85rem;
      color: #94a3b8;
      line-height: 1.4;
      margin-bottom: 1rem;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .meta-row {
      display: flex;
      justify-content: space-between;
      font-size: 0.8rem;
      color: #cbd5e1;
      margin-bottom: 0.4rem;
    }

    .meta-item {
      display: flex;
      align-items: center;
      gap: 0.35rem;
    }

    .price-container {
      display: flex;
      justify-content: space-between;
      align-items: baseline;
      background: rgba(15, 23, 42, 0.5);
      border-radius: 8px;
      padding: 0.65rem 0.85rem;
      margin: 0.85rem 0;
    }

    .hourly-rate .rate-amount {
      font-size: 1.35rem;
      font-weight: 800;
      color: #4ade80;
    }

    .hourly-rate .rate-unit {
      font-size: 0.75rem;
      color: #94a3b8;
      margin-left: 0.25rem;
    }

    .deposit-tag {
      font-size: 0.75rem;
      color: #fbbf24;
      font-weight: 500;
    }

    .rules-snippet {
      font-size: 0.75rem;
      color: #94a3b8;
      background: rgba(0, 0, 0, 0.2);
      padding: 0.5rem;
      border-radius: 6px;
      margin-bottom: 0.75rem;
    }

    .card-footer {
      margin-top: 0.75rem;
    }

    .book-btn {
      width: 100%;
      padding: 0.75rem;
      font-size: 0.95rem;
      border-radius: 10px;
    }

    /* Tables */
    .table-wrap {
      background: rgba(30, 41, 59, 0.65);
      backdrop-filter: blur(12px);
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 12px;
      overflow-x: auto;
    }

    table {
      width: 100%;
      border-collapse: collapse;
      text-align: left;
      font-size: 0.875rem;
    }

    th {
      background: rgba(15, 23, 42, 0.6);
      padding: 0.85rem 1rem;
      color: #94a3b8;
      font-weight: 600;
      border-bottom: 1px solid rgba(255, 255, 255, 0.08);
      text-transform: uppercase;
      font-size: 0.75rem;
      letter-spacing: 0.05em;
    }

    td {
      padding: 0.85rem 1rem;
      border-bottom: 1px solid rgba(255, 255, 255, 0.05);
      vertical-align: middle;
    }

    tr:hover td {
      background: rgba(255, 255, 255, 0.02);
    }

    .table-facility-name {
      font-weight: 600;
      color: #ffffff;
    }

    .d-block { display: block; }

    .slot-time-block {
      display: flex;
      flex-direction: column;
      gap: 0.2rem;
    }

    .fee-pill {
      font-weight: 700;
      color: #4ade80;
    }

    .status-badge {
      display: inline-block;
      padding: 0.25rem 0.65rem;
      border-radius: 9999px;
      font-size: 0.75rem;
      font-weight: 700;
      text-transform: uppercase;
    }

    .status-badge.confirmed {
      background: rgba(34, 197, 94, 0.15);
      color: #4ade80;
      border: 1px solid rgba(34, 197, 94, 0.3);
    }

    .status-badge.pending_approval {
      background: rgba(251, 191, 36, 0.15);
      color: #fde047;
      border: 1px solid rgba(251, 191, 36, 0.3);
    }

    .status-badge.cancelled {
      background: rgba(148, 163, 184, 0.15);
      color: #cbd5e1;
      border: 1px solid rgba(148, 163, 184, 0.3);
    }

    .status-badge.rejected {
      background: rgba(239, 68, 68, 0.15);
      color: #f87171;
      border: 1px solid rgba(239, 68, 68, 0.3);
    }

    .action-btn-group {
      display: flex;
      gap: 0.4rem;
    }

    .action-btn {
      padding: 0.35rem 0.65rem;
      border-radius: 6px;
      font-size: 0.8rem;
      font-weight: 600;
      cursor: pointer;
      border: 1px solid transparent;
      transition: all 0.15s ease;
    }

    .approve-btn {
      background: rgba(34, 197, 94, 0.2);
      color: #86efac;
      border-color: rgba(34, 197, 94, 0.4);
    }

    .approve-btn:hover {
      background: rgba(34, 197, 94, 0.35);
    }

    .reject-btn {
      background: rgba(239, 68, 68, 0.2);
      color: #fca5a5;
      border-color: rgba(239, 68, 68, 0.4);
    }

    .reject-btn:hover {
      background: rgba(239, 68, 68, 0.35);
    }

    /* Modals */
    .modal-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.75);
      backdrop-filter: blur(8px);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      padding: 1rem;
    }

    .modal-card {
      background: #1e293b;
      border: 1px solid rgba(255, 255, 255, 0.12);
      border-radius: 16px;
      width: 100%;
      max-width: 620px;
      max-height: 90vh;
      overflow-y: auto;
      padding: 1.75rem;
      box-shadow: 0 20px 50px rgba(0, 0, 0, 0.6);
    }

    .modal-card.modal-sm {
      max-width: 440px;
    }

    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 1.25rem;
    }

    .modal-header h2 {
      margin: 0;
      font-size: 1.4rem;
      color: #ffffff;
    }

    .close-x {
      background: none;
      border: none;
      color: #94a3b8;
      font-size: 1.25rem;
      cursor: pointer;
    }

    .close-x:hover { color: #ffffff; }

    .modal-form {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .form-row {
      display: flex;
      gap: 1rem;
    }

    .form-row > label {
      flex: 1;
    }

    label {
      display: flex;
      flex-direction: column;
      gap: 0.35rem;
      font-size: 0.85rem;
      color: #cbd5e1;
      font-weight: 500;
    }

    .checkbox-row {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .checkbox-label {
      flex-direction: row;
      align-items: center;
      cursor: pointer;
    }

    .slots-overview {
      background: rgba(251, 191, 36, 0.1);
      border: 1px solid rgba(251, 191, 36, 0.25);
      border-radius: 8px;
      padding: 0.65rem 0.85rem;
    }

    .slots-title {
      font-size: 0.8rem;
      color: #fde047;
      font-weight: 600;
      margin: 0 0 0.35rem 0;
    }

    .slots-list {
      display: flex;
      flex-wrap: wrap;
      gap: 0.4rem;
    }

    .slot-pill {
      font-size: 0.75rem;
      padding: 0.2rem 0.5rem;
      border-radius: 4px;
    }

    .slot-pill.booked {
      background: rgba(239, 68, 68, 0.2);
      color: #fca5a5;
      border: 1px solid rgba(239, 68, 68, 0.3);
    }

    .price-breakdown {
      background: rgba(15, 23, 42, 0.6);
      border-radius: 10px;
      padding: 1rem;
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      margin-top: 0.5rem;
    }

    .breakdown-row {
      display: flex;
      justify-content: space-between;
      font-size: 0.85rem;
      color: #cbd5e1;
    }

    .breakdown-row.total-row {
      border-top: 1px solid rgba(255, 255, 255, 0.1);
      padding-top: 0.6rem;
      margin-top: 0.25rem;
      font-size: 1rem;
      font-weight: 700;
      color: #ffffff;
    }

    .total-amount {
      color: #4ade80;
      font-size: 1.25rem;
    }

    .modal-footer {
      display: flex;
      justify-content: flex-end;
      gap: 0.75rem;
      margin-top: 1rem;
    }

    .empty-state {
      text-align: center;
      padding: 3.5rem 1rem;
      color: #94a3b8;
    }

    .empty-icon {
      font-size: 3rem;
      display: block;
      margin-bottom: 0.75rem;
    }

    .loading {
      text-align: center;
      padding: 2.5rem;
      color: #38bdf8;
      font-weight: 600;
    }
  `]
})
export class FacilityListComponent implements OnInit {
  private readonly facilityService = inject(FacilityService);
  private readonly authService = inject(AuthService);

  // State
  facilities: Facility[] = [];
  myBookings: FacilityBooking[] = [];
  allBookings: FacilityBooking[] = [];
  pendingApprovals: FacilityBooking[] = [];
  summary: FacilitySummary | null = null;

  activeTab: 'catalog' | 'my-bookings' | 'approvals' = 'catalog';
  loading = false;
  errorMessage = '';
  successMessage = '';

  // Filters
  searchQuery = '';
  categoryFilter: FacilityCategory | '' = '';
  categories: FacilityCategory[] = [
    'CLUBHOUSE',
    'SWIMMING_POOL',
    'SPORTS_COURT',
    'PARTY_HALL',
    'GYM',
    'GUEST_ROOM',
    'COWORKING',
    'OTHER'
  ];

  // Booking Modal
  bookingModalFacility: Facility | null = null;
  bookingDate = '';
  minDateString = '';
  startTimeStr = '10:00';
  endTimeStr = '12:00';
  attendeesCount = 1;
  bookingPurpose = '';
  estimatedDurationHours = 2;
  calculatedFee = 0;
  bookedSlots: BookingSlot[] = [];
  submittingBooking = false;

  // Review Modals
  rejectModalBooking: FacilityBooking | null = null;
  rejectReason = '';

  cancelModalBooking: FacilityBooking | null = null;
  cancelReason = '';

  // Create Facility Modal
  showCreateFacilityModal = false;
  newFac: CreateFacilityRequest = this.emptyFacility();

  get communityId(): string {
    return this.authService.activeCommunityId() || '11111111-1111-1111-1111-111111111111';
  }

  ngOnInit(): void {
    const today = new Date();
    this.minDateString = today.toISOString().split('T')[0];
    this.bookingDate = this.minDateString;
    this.refreshAll();
  }

  refreshAll(): void {
    this.loadFacilities();
    this.loadSummary();
    this.loadMyBookings();
    if (this.isManagerOrAdmin()) {
      this.loadAllBookings();
    }
  }

  switchTab(tab: 'catalog' | 'my-bookings' | 'approvals'): void {
    this.activeTab = tab;
    if (tab === 'my-bookings') this.loadMyBookings();
    if (tab === 'approvals') this.loadAllBookings();
  }

  loadFacilities(): void {
    this.loading = true;
    this.facilityService.getFacilities(this.communityId, this.categoryFilter).subscribe({
      next: (res) => {
        this.facilities = res.data || [];
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Failed to load community facilities';
        this.loading = false;
      }
    });
  }

  loadSummary(): void {
    this.facilityService.getSummary(this.communityId).subscribe({
      next: (res) => {
        this.summary = res.data;
      },
      error: () => {}
    });
  }

  loadMyBookings(): void {
    this.facilityService.listMyBookings(this.communityId).subscribe({
      next: (res) => {
        this.myBookings = res.data || [];
      },
      error: () => {}
    });
  }

  loadAllBookings(): void {
    this.facilityService.listBookings(this.communityId).subscribe({
      next: (res) => {
        this.allBookings = res.data || [];
        this.pendingApprovals = this.allBookings.filter(b => b.status === 'PENDING_APPROVAL');
      },
      error: () => {}
    });
  }

  filteredFacilities(): Facility[] {
    if (!this.searchQuery) return this.facilities;
    const q = this.searchQuery.toLowerCase();
    return this.facilities.filter(f =>
      f.name.toLowerCase().includes(q) ||
      f.facilityCode.toLowerCase().includes(q) ||
      (f.locationDetails && f.locationDetails.toLowerCase().includes(q)) ||
      f.category.toLowerCase().includes(q)
    );
  }

  // Booking Flow
  openBookingModal(facility: Facility): void {
    this.bookingModalFacility = facility;
    this.attendeesCount = 1;
    this.bookingPurpose = '';
    this.startTimeStr = facility.operatingHoursStart.substring(0, 5) || '10:00';
    // set end time + minBookingHours
    const [h, m] = this.startTimeStr.split(':').map(Number);
    const endH = Math.min(23, h + facility.minBookingHours);
    this.endTimeStr = `${String(endH).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
    this.calculateFee();
    this.onDateChange();
  }

  closeBookingModal(): void {
    this.bookingModalFacility = null;
    this.bookedSlots = [];
  }

  onDateChange(): void {
    if (!this.bookingModalFacility || !this.bookingDate) return;
    this.facilityService.getAvailability(this.communityId, this.bookingModalFacility.id, this.bookingDate).subscribe({
      next: (res) => {
        this.bookedSlots = res.data || [];
      },
      error: () => {
        this.bookedSlots = [];
      }
    });
  }

  calculateFee(): void {
    if (!this.bookingModalFacility || !this.startTimeStr || !this.endTimeStr) return;
    const [sh, sm] = this.startTimeStr.split(':').map(Number);
    const [eh, em] = this.endTimeStr.split(':').map(Number);
    const durationMinutes = (eh * 60 + em) - (sh * 60 + sm);
    this.estimatedDurationHours = Math.max(0, durationMinutes / 60.0);
    this.calculatedFee = (this.bookingModalFacility.hourlyFee || 0) * this.estimatedDurationHours;
  }

  submitBooking(): void {
    if (!this.bookingModalFacility) return;

    if (this.estimatedDurationHours <= 0) {
      this.errorMessage = 'Reservation end time must be strictly after start time';
      return;
    }

    if (this.estimatedDurationHours < this.bookingModalFacility.minBookingHours) {
      this.errorMessage = `Minimum duration for this facility is ${this.bookingModalFacility.minBookingHours} hour(s)`;
      return;
    }

    const startIso = `${this.bookingDate}T${this.startTimeStr}:00Z`;
    const endIso = `${this.bookingDate}T${this.endTimeStr}:00Z`;

    const req: CreateBookingRequest = {
      facilityId: this.bookingModalFacility.id,
      bookingDate: this.bookingDate,
      startTime: startIso,
      endTime: endIso,
      attendeesCount: this.attendeesCount,
      purpose: this.bookingPurpose
    };

    this.submittingBooking = true;
    this.facilityService.createBooking(this.communityId, req).subscribe({
      next: (res) => {
        this.submittingBooking = false;
        this.closeBookingModal();
        this.successMessage = `Reservation created successfully! Reference: ${res.data.bookingReference}`;
        this.refreshAll();
        this.switchTab('my-bookings');
      },
      error: (err) => {
        this.submittingBooking = false;
        this.errorMessage = err?.error?.message || 'Failed to submit reservation. The slot may conflict with an existing booking.';
      }
    });
  }

  // Manager Approval Review
  reviewBooking(booking: FacilityBooking, approved: boolean): void {
    const req: ReviewBookingRequest = { approved };
    this.facilityService.reviewBooking(this.communityId, booking.id, req).subscribe({
      next: () => {
        this.successMessage = `Booking ${booking.bookingReference} approved successfully!`;
        this.refreshAll();
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Failed to approve booking';
      }
    });
  }

  openRejectModal(booking: FacilityBooking): void {
    this.rejectModalBooking = booking;
    this.rejectReason = '';
  }

  closeRejectModal(): void {
    this.rejectModalBooking = null;
  }

  confirmReject(): void {
    if (!this.rejectModalBooking) return;
    const req: ReviewBookingRequest = {
      approved: false,
      rejectionReason: this.rejectReason || 'Unavailable'
    };
    this.facilityService.reviewBooking(this.communityId, this.rejectModalBooking.id, req).subscribe({
      next: () => {
        this.successMessage = `Booking ${this.rejectModalBooking?.bookingReference} rejected.`;
        this.closeRejectModal();
        this.refreshAll();
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Failed to reject booking';
      }
    });
  }

  // Resident Cancellation
  canCancel(booking: FacilityBooking): boolean {
    return booking.status === 'CONFIRMED' || booking.status === 'PENDING_APPROVAL';
  }

  openCancelModal(booking: FacilityBooking): void {
    this.cancelModalBooking = booking;
    this.cancelReason = '';
  }

  closeCancelModal(): void {
    this.cancelModalBooking = null;
  }

  confirmCancel(): void {
    if (!this.cancelModalBooking) return;
    const req: CancelBookingRequest = { reason: this.cancelReason || 'Cancelled by resident' };
    this.facilityService.cancelBooking(this.communityId, this.cancelModalBooking.id, req).subscribe({
      next: () => {
        this.successMessage = `Booking ${this.cancelModalBooking?.bookingReference} cancelled.`;
        this.closeCancelModal();
        this.refreshAll();
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Failed to cancel reservation';
      }
    });
  }

  // Create Facility (Manager only)
  openCreateFacilityModal(): void {
    this.newFac = this.emptyFacility();
    this.showCreateFacilityModal = true;
  }

  closeCreateFacilityModal(): void {
    this.showCreateFacilityModal = false;
  }

  submitCreateFacility(): void {
    this.facilityService.createFacility(this.communityId, this.newFac).subscribe({
      next: (res) => {
        this.successMessage = `Facility ${res.data.name} (${res.data.facilityCode}) registered successfully!`;
        this.closeCreateFacilityModal();
        this.refreshAll();
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Failed to create facility';
      }
    });
  }

  // Format Helpers
  categoryIcon(cat: FacilityCategory): string {
    switch (cat) {
      case 'CLUBHOUSE': return '🏛️';
      case 'SWIMMING_POOL': return '🏊';
      case 'SPORTS_COURT': return '🏸';
      case 'PARTY_HALL': return '🎉';
      case 'GYM': return '🏋️';
      case 'GUEST_ROOM': return '🛏️';
      case 'COWORKING': return '💻';
      default: return '📍';
    }
  }

  formatCategory(cat: FacilityCategory): string {
    return cat.replace('_', ' ');
  }

  formatStatus(status: BookingStatus): string {
    return status.replace('_', ' ');
  }

  formatTime(isoString: string): string {
    if (!isoString) return '';
    try {
      const d = new Date(isoString);
      return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch {
      return isoString;
    }
  }

  isManagerOrAdmin(): boolean {
    return this.authService.hasRole('SUPER_ADMIN') ||
           this.authService.hasRole('COMMUNITY_MANAGER') ||
           this.authService.hasRole('HOA_OFFICER');
  }

  private emptyFacility(): CreateFacilityRequest {
    return {
      facilityCode: '',
      name: '',
      description: '',
      category: 'CLUBHOUSE',
      capacity: 20,
      hourlyFee: 0,
      securityDeposit: 0,
      requiresApproval: false,
      operatingHoursStart: '08:00',
      operatingHoursEnd: '22:00',
      minBookingHours: 1,
      maxBookingHours: 4,
      cancellationCutoffHours: 2,
      rulesAndRegulations: ''
    };
  }
}
