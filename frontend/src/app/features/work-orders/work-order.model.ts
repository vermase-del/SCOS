export type WorkOrderScope = 'PRIVATE_UNIT' | 'COMMON_AREA';
export type WorkOrderCategory = 'PLUMBING' | 'ELECTRICAL' | 'HVAC' | 'ELEVATOR' | 'CIVIL' | 'SECURITY_DEVICE' | 'GARDENING' | 'OTHER';
export type WorkOrderPriority = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
export type WorkOrderStatus = 'OPEN' | 'ASSIGNED' | 'IN_PROGRESS' | 'ON_HOLD' | 'RESOLVED' | 'CLOSED' | 'CANCELLED';
export type CostType = 'LABOR' | 'MATERIALS' | 'VENDOR_FEE' | 'EQUIPMENT' | 'OTHER';
export interface WorkOrderActivity { id: string; authorUserId: string; activityType: string; fromStatus?: WorkOrderStatus; toStatus?: WorkOrderStatus; notes: string; createdAt: string; }
export interface WorkOrderCostLine { id: string; costType: CostType; itemDescription: string; quantity: number; unitPrice: number; totalAmount: number; currencyCode: string; recordedByUserId: string; createdAt: string; }
export interface WorkOrder { id: string; communityId: string; ticketNumber: string; title: string; description: string; scope: WorkOrderScope; buildingId?: string; unitId?: string; locationDetails?: string; category: WorkOrderCategory; priority: WorkOrderPriority; status: WorkOrderStatus; reportedByUserId: string; assignedTechnicianId?: string; slaDueAt: string; slaBreached: boolean; totalCost: number; currencyCode: string; version: number; createdAt: string; updatedAt: string; activities: WorkOrderActivity[]; costs: WorkOrderCostLine[]; }
export interface WorkOrderSummary { totalActive: number; openCount: number; assignedCount: number; inProgressCount: number; onHoldCount: number; resolvedCount: number; slaBreachedCount: number; averageRating: number; }
export interface CreateWorkOrderRequest { communityId: string; title: string; description: string; scope: WorkOrderScope; buildingId?: string; unitId?: string; locationDetails?: string; category: WorkOrderCategory; priority: WorkOrderPriority; }
export interface ApiEnvelope<T> { success: boolean; message: string; data: T; }
export interface PageResponse<T> { content: T[]; pageNumber: number; pageSize: number; totalElements: number; totalPages: number; last: boolean; }
