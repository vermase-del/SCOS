export type FacilityCategory =
  | 'CLUBHOUSE'
  | 'SWIMMING_POOL'
  | 'SPORTS_COURT'
  | 'PARTY_HALL'
  | 'GYM'
  | 'GUEST_ROOM'
  | 'COWORKING'
  | 'OTHER';

export type BookingStatus =
  | 'PENDING_APPROVAL'
  | 'CONFIRMED'
  | 'CHECKED_IN'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'REJECTED';

export interface Facility {
  id: string;
  communityId: string;
  facilityCode: string;
  name: string;
  description?: string;
  category: FacilityCategory;
  buildingId?: string;
  locationDetails?: string;
  capacity: number;
  hourlyFee: number;
  securityDeposit: number;
  currencyCode: string;
  requiresApproval: boolean;
  operatingHoursStart: string;
  operatingHoursEnd: string;
  minBookingHours: number;
  maxBookingHours: number;
  cancellationCutoffHours: number;
  rulesAndRegulations?: string;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface FacilityBooking {
  id: string;
  communityId: string;
  bookingReference: string;
  facilityId: string;
  facilityCode: string;
  facilityName: string;
  facilityCategory: FacilityCategory;
  residentUserId: string;
  unitId?: string;
  bookingDate: string;
  startTime: string;
  endTime: string;
  attendeesCount: number;
  purpose?: string;
  status: BookingStatus;
  totalFee: number;
  depositAmount: number;
  currencyCode: string;
  approvedByUserId?: string;
  approvedAt?: string;
  rejectionReason?: string;
  cancelledByUserId?: string;
  cancelledAt?: string;
  cancellationReason?: string;
  createdAt?: string;
}

export interface FacilitySummary {
  totalFacilities: number;
  activeFacilities: number;
  totalBookings: number;
  bookingsToday: number;
  pendingApprovals: number;
}

export interface BookingSlot {
  startTime: string;
  endTime: string;
  isAvailable: boolean;
  bookingReference?: string;
}

export interface CreateBookingRequest {
  facilityId: string;
  unitId?: string;
  bookingDate: string;
  startTime: string;
  endTime: string;
  attendeesCount?: number;
  purpose?: string;
}

export interface CreateFacilityRequest {
  facilityCode: string;
  name: string;
  description?: string;
  category: FacilityCategory;
  buildingId?: string;
  locationDetails?: string;
  capacity: number;
  hourlyFee: number;
  securityDeposit: number;
  currencyCode?: string;
  requiresApproval: boolean;
  operatingHoursStart: string;
  operatingHoursEnd: string;
  minBookingHours: number;
  maxBookingHours: number;
  cancellationCutoffHours: number;
  rulesAndRegulations?: string;
}

export interface ReviewBookingRequest {
  approved: boolean;
  rejectionReason?: string;
}

export interface CancelBookingRequest {
  reason: string;
}

export interface ApiEnvelope<T> {
  success: boolean;
  message: string;
  data: T;
}
