import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface CommunityDto {
  id: string;
  code: string;
  name: string;
  legalName?: string;
  city: string;
  stateProvince: string;
  countryCode: string;
  phone?: string;
  email: string;
  currencyCode: string;
  status: string;
}

export interface CommunityRegistrationRequest {
  name: string;
  code?: string;
  legalName?: string;
  registrationNumber?: string;
  taxIdentifier?: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  stateProvince: string;
  postalCode: string;
  countryCode?: string;
  phone?: string;
  email: string;
  currencyCode?: string;
  timezone?: string;
  adminFirstName: string;
  adminLastName: string;
  adminEmail: string;
  adminPhone: string;
  adminUsername: string;
  adminPassword: string;
  numberOfTowers?: number;
  floorsPerTower?: number;
  unitsPerFloor?: number;
  towerPrefix?: string;
}

export interface CommunityRegistrationResponse {
  communityId: string;
  communityCode: string;
  communityName: string;
  adminUserId: string;
  adminUsername: string;
  token: string;
  tokenType: string;
  towersCreated: number;
  unitsCreated: number;
  message: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  errorCode?: string;
  timestamp: string;
}

@Injectable({
  providedIn: 'root'
})
export class CommunityService {
  private http = inject(HttpClient);

  communities = signal<CommunityDto[]>([]);
  activeCommunity = signal<CommunityDto | null>(null);

  constructor() {
    const saved = localStorage.getItem('scos_active_community');
    if (saved) {
      try {
        this.activeCommunity.set(JSON.parse(saved));
      } catch (e) {
        localStorage.removeItem('scos_active_community');
      }
    }
  }

  loadCommunities(): Observable<ApiResponse<CommunityDto[]>> {
    return this.http.get<ApiResponse<CommunityDto[]>>('/api/v1/communities').pipe(
      tap(res => {
        if (res.success && res.data) {
          this.communities.set(res.data);
          if (!this.activeCommunity() && res.data.length > 0) {
            this.setActiveCommunity(res.data[0]);
          }
        }
      })
    );
  }

  registerCommunity(req: CommunityRegistrationRequest): Observable<ApiResponse<CommunityRegistrationResponse>> {
    return this.http.post<ApiResponse<CommunityRegistrationResponse>>('/api/v1/communities/register', req).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.loadCommunities().subscribe();
        }
      })
    );
  }

  setActiveCommunity(c: CommunityDto): void {
    this.activeCommunity.set(c);
    localStorage.setItem('scos_active_community', JSON.stringify(c));
    localStorage.setItem('scos_active_community_id', c.id);
  }
}
