import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiEnvelope, CreateWorkOrderRequest, PageResponse, WorkOrder, WorkOrderPriority, WorkOrderStatus, WorkOrderSummary } from './work-order.model';

@Injectable({ providedIn: 'root' })
export class WorkOrderService {
  private readonly http = inject(HttpClient);
  getWorkOrders(communityId: string, status?: WorkOrderStatus | '', priority?: WorkOrderPriority | ''): Observable<ApiEnvelope<PageResponse<WorkOrder>>> {
    let params = new HttpParams().set('communityId', communityId).set('page', '0').set('size', '100');
    if (status) params = params.set('status', status);
    if (priority) params = params.set('priority', priority);
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
}
