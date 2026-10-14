import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ApiEnvelope,
  Asset,
  AssetStatus,
  AssetStatusHistory,
  AssetSummary,
  CreateAssetRequest,
  CreateWorkOrderRequest,
  PageResponse,
  UpdateAssetRequest,
  UpdateAssetStatusRequest,
  WorkOrder,
  WorkOrderPriority,
  WorkOrderStatus,
  WorkOrderSummary,
  WorkOrderType
} from './work-order.model';

@Injectable({ providedIn: 'root' })
export class WorkOrderService {
  private readonly http = inject(HttpClient);

  // Work Orders API
  getWorkOrders(
    communityId: string,
    status?: WorkOrderStatus | '',
    priority?: WorkOrderPriority | '',
    workOrderType?: WorkOrderType | '',
    assetId?: string
  ): Observable<ApiEnvelope<PageResponse<WorkOrder>>> {
    let params = new HttpParams().set('communityId', communityId).set('page', '0').set('size', '100');
    if (status) params = params.set('status', status);
    if (priority) params = params.set('priority', priority);
    if (workOrderType) params = params.set('workOrderType', workOrderType);
    if (assetId) params = params.set('assetId', assetId);
    return this.http.get<ApiEnvelope<PageResponse<WorkOrder>>>('/api/v1/work-orders', { params });
  }

  getSummary(communityId: string): Observable<ApiEnvelope<WorkOrderSummary>> {
    return this.http.get<ApiEnvelope<WorkOrderSummary>>('/api/v1/work-orders/summary', { params: { communityId } });
  }

  getWorkOrder(id: string, communityId: string): Observable<ApiEnvelope<WorkOrder>> {
    return this.http.get<ApiEnvelope<WorkOrder>>(`/api/v1/work-orders/${id}`, { params: { communityId } });
  }

  create(request: CreateWorkOrderRequest): Observable<ApiEnvelope<WorkOrder>> {
    return this.http.post<ApiEnvelope<WorkOrder>>('/api/v1/work-orders', request);
  }

  assign(id: string, communityId: string, technicianId: string, notes = ''): Observable<ApiEnvelope<WorkOrder>> {
    return this.http.patch<ApiEnvelope<WorkOrder>>(`/api/v1/work-orders/${id}/assign`, { technicianId, notes }, { params: { communityId } });
  }

  transition(id: string, communityId: string, targetStatus: WorkOrderStatus, notes = ''): Observable<ApiEnvelope<WorkOrder>> {
    return this.http.patch<ApiEnvelope<WorkOrder>>(`/api/v1/work-orders/${id}/status`, { targetStatus, notes }, { params: { communityId } });
  }

  addCost(id: string, communityId: string, costType: string, itemDescription: string, quantity: number, unitPrice: number): Observable<ApiEnvelope<unknown>> {
    return this.http.post<ApiEnvelope<unknown>>(`/api/v1/work-orders/${id}/costs`, { costType, itemDescription, quantity, unitPrice, currencyCode: 'INR' }, { params: { communityId } });
  }

  submitFeedback(id: string, communityId: string, rating: number, resolutionConfirmed: boolean, feedbackComments = ''): Observable<ApiEnvelope<WorkOrder>> {
    return this.http.post<ApiEnvelope<WorkOrder>>(`/api/v1/work-orders/${id}/feedback`, { rating, resolutionConfirmed, feedbackComments }, { params: { communityId } });
  }

  // Asset Register API
  getAssets(communityId: string, status?: AssetStatus | '', page = 0, size = 50): Observable<ApiEnvelope<PageResponse<Asset>>> {
    let params = new HttpParams().set('communityId', communityId).set('page', page.toString()).set('size', size.toString());
    if (status) params = params.set('status', status);
    return this.http.get<ApiEnvelope<PageResponse<Asset>>>('/api/v1/assets', { params });
  }

  getAssetSummary(communityId: string): Observable<ApiEnvelope<AssetSummary>> {
    return this.http.get<ApiEnvelope<AssetSummary>>('/api/v1/assets/summary', { params: { communityId } });
  }

  getDueForMaintenance(communityId: string): Observable<ApiEnvelope<PageResponse<Asset>>> {
    const params = new HttpParams().set('communityId', communityId).set('page', '0').set('size', '50');
    return this.http.get<ApiEnvelope<PageResponse<Asset>>>('/api/v1/assets/due-maintenance', { params });
  }

  getAsset(id: string, communityId: string): Observable<ApiEnvelope<Asset>> {
    return this.http.get<ApiEnvelope<Asset>>(`/api/v1/assets/${id}`, { params: { communityId } });
  }

  createAsset(communityId: string, request: CreateAssetRequest): Observable<ApiEnvelope<Asset>> {
    return this.http.post<ApiEnvelope<Asset>>('/api/v1/assets', request, { params: { communityId } });
  }

  updateAsset(id: string, communityId: string, request: UpdateAssetRequest): Observable<ApiEnvelope<Asset>> {
    return this.http.put<ApiEnvelope<Asset>>(`/api/v1/assets/${id}`, request, { params: { communityId } });
  }

  updateAssetStatus(id: string, communityId: string, status: AssetStatus, reason = ''): Observable<ApiEnvelope<Asset>> {
    const body: UpdateAssetStatusRequest = { status, reason };
    return this.http.patch<ApiEnvelope<Asset>>(`/api/v1/assets/${id}/status`, body, { params: { communityId } });
  }

  getAssetStatusHistory(id: string, communityId: string): Observable<ApiEnvelope<AssetStatusHistory[]>> {
    return this.http.get<ApiEnvelope<AssetStatusHistory[]>>(`/api/v1/assets/${id}/status-history`, { params: { communityId } });
  }
}
