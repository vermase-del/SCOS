import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ApiEnvelope,
  BookingSlot,
  CancelBookingRequest,
  CreateBookingRequest,
  CreateFacilityRequest,
  Facility,
  FacilityBooking,
  FacilityCategory,
  FacilitySummary,
  ReviewBookingRequest
} from '../../core/models/facility.model';

@Injectable({ providedIn: 'root' })
export class FacilityService {
  private readonly http = inject(HttpClient);

  getFacilities(
    communityId: string,
    category?: FacilityCategory | '',
    active?: boolean
  ): Observable<ApiEnvelope<Facility[]>> {
    let params = new HttpParams().set('communityId', communityId);
    if (category) params = params.set('category', category);
    if (active !== undefined) params = params.set('active', active.toString());
    return this.http.get<ApiEnvelope<Facility[]>>('/api/v1/facilities', { params });
  }

  getFacility(communityId: string, id: string): Observable<ApiEnvelope<Facility>> {
    return this.http.get<ApiEnvelope<Facility>>(`/api/v1/facilities/${id}`, { params: { communityId } });
  }

  createFacility(communityId: string, request: CreateFacilityRequest): Observable<ApiEnvelope<Facility>> {
    return this.http.post<ApiEnvelope<Facility>>('/api/v1/facilities', request, { params: { communityId } });
  }

  getSummary(communityId: string): Observable<ApiEnvelope<FacilitySummary>> {
    return this.http.get<ApiEnvelope<FacilitySummary>>('/api/v1/facilities/summary', { params: { communityId } });
  }

  listBookings(communityId: string, residentUserId?: string): Observable<ApiEnvelope<FacilityBooking[]>> {
    let params = new HttpParams().set('communityId', communityId);
    if (residentUserId) params = params.set('residentUserId', residentUserId);
    return this.http.get<ApiEnvelope<FacilityBooking[]>>('/api/v1/facility-bookings', { params });
  }

  listMyBookings(communityId: string): Observable<ApiEnvelope<FacilityBooking[]>> {
    return this.http.get<ApiEnvelope<FacilityBooking[]>>('/api/v1/facility-bookings/my', { params: { communityId } });
  }

  getBooking(communityId: string, id: string): Observable<ApiEnvelope<FacilityBooking>> {
    return this.http.get<ApiEnvelope<FacilityBooking>>(`/api/v1/facility-bookings/${id}`, { params: { communityId } });
  }

  createBooking(communityId: string, request: CreateBookingRequest): Observable<ApiEnvelope<FacilityBooking>> {
    return this.http.post<ApiEnvelope<FacilityBooking>>('/api/v1/facility-bookings', request, { params: { communityId } });
  }

  reviewBooking(communityId: string, id: string, request: ReviewBookingRequest): Observable<ApiEnvelope<FacilityBooking>> {
    return this.http.post<ApiEnvelope<FacilityBooking>>(`/api/v1/facility-bookings/${id}/review`, request, { params: { communityId } });
  }

  cancelBooking(communityId: string, id: string, request: CancelBookingRequest): Observable<ApiEnvelope<FacilityBooking>> {
    return this.http.post<ApiEnvelope<FacilityBooking>>(`/api/v1/facility-bookings/${id}/cancel`, request, { params: { communityId } });
  }

  getAvailability(communityId: string, facilityId: string, date: string): Observable<ApiEnvelope<BookingSlot[]>> {
    return this.http.get<ApiEnvelope<BookingSlot[]>>('/api/v1/facility-bookings/availability', {
      params: { communityId, facilityId, date }
    });
  }
}
