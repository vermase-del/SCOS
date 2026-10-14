import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, PageResponse } from '../../core/auth/user.model';

export interface Building {
  id: string;
  communityId: string;
  code: string;
  name: string;
  totalFloors: number;
  basementFloors: number;
  totalUnits: number;
  hasElevator: boolean;
  constructionStatus: string;
}

export interface UnitTile {
  id: string;
  unitNumber: string;
  unitType: string;
  occupancyStatus: string;
  primaryResidentName?: string;
}

export interface FloorRow {
  floorNumber: number;
  units: UnitTile[];
}

export interface BuildingFloorMatrix {
  buildingId: string;
  buildingCode: string;
  buildingName: string;
  totalFloors: number;
  totalUnits: number;
  floors: FloorRow[];
}

export interface UnitDetail {
  id: string;
  unitNumber: string;
  floorNumber: number;
  unitType: string;
  carpetAreaSqft: number;
  superBuiltupAreaSqft: number;
  balconyCount: number;
  bathroomCount: number;
  parkingSlotNumber?: string;
  occupancyStatus: string;
  furnishingStatus: string;
  electricMeterNumber?: string;
  gasMeterNumber?: string;
  waterMeterNumber?: string;
  baseMaintenanceCharge: number;
  active: boolean;
}

@Injectable({ providedIn: 'root' })
export class InfrastructureService {
  private http = inject(HttpClient);
  private apiUrl = '/api/v1/infrastructure';

  getBuildings(): Observable<ApiResponse<Building[]>> {
    return this.http.get<ApiResponse<Building[]>>(`${this.apiUrl}/buildings`);
  }

  getBuildingFloorMatrix(buildingId: string): Observable<ApiResponse<BuildingFloorMatrix>> {
    return this.http.get<ApiResponse<BuildingFloorMatrix>>(`${this.apiUrl}/buildings/${buildingId}/matrix`);
  }

  getUnits(buildingId?: string, occupancyStatus?: string): Observable<ApiResponse<PageResponse<UnitDetail>>> {
    const params: any = {};
    if (buildingId) params.buildingId = buildingId;
    if (occupancyStatus) params.occupancyStatus = occupancyStatus;
    return this.http.get<ApiResponse<PageResponse<UnitDetail>>>(`${this.apiUrl}/units`, { params });
  }

  updateUnitStatus(unitId: string, occupancyStatus: string): Observable<ApiResponse<UnitDetail>> {
    return this.http.patch<ApiResponse<UnitDetail>>(`${this.apiUrl}/units/${unitId}/status`, null, {
      params: { occupancyStatus }
    });
  }
}
