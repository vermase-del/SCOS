import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { WorkOrderService } from './work-order.service';

describe('WorkOrderService', () => {
  let service: WorkOrderService;
  let httpMock: HttpTestingController;
  const communityId = '11111111-1111-1111-1111-111111111111';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [WorkOrderService]
    });
    service = TestBed.inject(WorkOrderService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should query work orders with communityId parameter', () => {
    service.getWorkOrders(communityId, 'OPEN').subscribe(res => {
      expect(res.data.content.length).toBe(1);
      expect(res.data.content[0].ticketNumber).toBe('WO-2026-0001');
    });

    const req = httpMock.expectOne(r => r.url === '/api/v1/work-orders' && r.params.get('communityId') === communityId);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('status')).toBe('OPEN');

    const mockResponse = {
      success: true,
      data: {
        content: [{
          id: 'wo-1',
          communityId,
          ticketNumber: 'WO-2026-0001',
          title: 'HVAC repair',
          description: 'A/C leak in lobby',
          scope: 'COMMON_AREA' as const,
          category: 'HVAC' as const,
          priority: 'HIGH' as const,
          status: 'OPEN' as const,
          slaDueAt: new Date().toISOString(),
          slaBreached: false,
          totalCost: 0,
          currencyCode: 'INR',
          version: 1,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
          history: [],
          costs: [],
          workOrderType: 'CORRECTIVE' as const,
          outOfService: false
        }],
        page: 0,
        size: 100,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true
      }
    };
    req.flush(mockResponse);
  });

  it('should post status transition with notes and communityId', () => {
    service.transition('wo-1', communityId, 'IN_PROGRESS', 'Starting diagnostic').subscribe(res => {
      expect(res.data.status).toBe('IN_PROGRESS');
    });

    const req = httpMock.expectOne(r => r.url === '/api/v1/work-orders/wo-1/status' && r.params.get('communityId') === communityId);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ targetStatus: 'IN_PROGRESS', notes: 'Starting diagnostic' });

    req.flush({
      success: true,
      data: {
        id: 'wo-1',
        status: 'IN_PROGRESS'
      }
    });
  });

  it('should assign technician to work order', () => {
    service.assign('wo-1', communityId, 'tech-123', 'Assigned Ramesh').subscribe(res => {
      expect(res.success).toBeTrue();
    });

    const req = httpMock.expectOne(r => r.url === '/api/v1/work-orders/wo-1/assign' && r.params.get('communityId') === communityId);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ technicianId: 'tech-123', notes: 'Assigned Ramesh' });
    req.flush({ success: true, data: {} });
  });

  it('should record an itemized cost line', () => {
    service.addCost('wo-1', communityId, 'PARTS', 'Replacement filter', 2, 450).subscribe(res => {
      expect(res.success).toBeTrue();
    });

    const req = httpMock.expectOne(r => r.url === '/api/v1/work-orders/wo-1/costs' && r.params.get('communityId') === communityId);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      costType: 'PARTS',
      itemDescription: 'Replacement filter',
      quantity: 2,
      unitPrice: 450,
      currencyCode: 'INR'
    });
    req.flush({ success: true, data: {} });
  });
});
