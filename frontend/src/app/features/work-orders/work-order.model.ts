export type WorkOrderScope = 'PRIVATE_UNIT' | 'COMMON_AREA';
export type WorkOrderCategory = 'PLUMBING' | 'ELECTRICAL' | 'HVAC' | 'ELEVATOR' | 'CIVIL' | 'SECURITY_DEVICE' | 'GARDENING' | 'OTHER';
export type WorkOrderPriority = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
export type WorkOrderStatus = 'OPEN' | 'ASSIGNED' | 'IN_PROGRESS' | 'ON_HOLD' | 'RESOLVED' | 'CLOSED' | 'CANCELLED';
export type WorkOrderType = 'REACTIVE' | 'PREVENTIVE' | 'INSPECTION' | 'EMERGENCY' | 'CORRECTIVE';
export type CostType = 'LABOR' | 'MATERIALS' | 'VENDOR_FEE' | 'EQUIPMENT' | 'OTHER';

export type AssetStatus = 'OPERATIONAL' | 'UNDER_MAINTENANCE' | 'OUT_OF_SERVICE' | 'RETIRED';
export type AssetCriticality = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';

export interface WorkOrderActivity {
  id: string;
  authorUserId: string;
  activityType: string;
  fromStatus?: WorkOrderStatus;
  toStatus?: WorkOrderStatus;
  notes: string;
  createdAt: string;
}

export interface WorkOrderCostLine {
  id: string;
  costType: CostType;
  itemDescription: string;
  quantity: number;
  unitPrice: number;
  totalAmount: number;
  currencyCode: string;
  recordedByUserId: string;
  createdAt: string;
}

export interface WorkOrder {
  id: string;
  communityId: string;
  ticketNumber: string;
  title: string;
  description: string;
  scope: WorkOrderScope;
  buildingId?: string;
  unitId?: string;
  locationDetails?: string;
  category: WorkOrderCategory;
  priority: WorkOrderPriority;
  status: WorkOrderStatus;
  reportedByUserId: string;
  assignedTechnicianId?: string;
  slaDueAt: string;
  slaBreached: boolean;
  totalCost: number;
  currencyCode: string;
  version: number;
  createdAt: string;
  updatedAt: string;
  activities: WorkOrderActivity[];
  costs: WorkOrderCostLine[];
  workOrderType: WorkOrderType;
  reasonCode?: string;
  departmentCode?: string;
  plannedStartAt?: string;
  plannedEndAt?: string;
  actualStartAt?: string;
  resolvedAt?: string;
  outOfService: boolean;
  serviceType?: string;
  assetId?: string;
  assetName?: string;
  assetCode?: string;
  externalReference?: string;
}

export interface WorkOrderSummary {
  totalActive: number;
  openCount: number;
  assignedCount: number;
  inProgressCount: number;
  onHoldCount: number;
  resolvedCount: number;
  slaBreachedCount: number;
  averageRating: number;
}

export interface CreateWorkOrderRequest {
  communityId: string;
  title: string;
  description: string;
  scope: WorkOrderScope;
  buildingId?: string;
  unitId?: string;
  locationDetails?: string;
  category: WorkOrderCategory;
  priority: WorkOrderPriority;
  workOrderType?: WorkOrderType;
  assetId?: string;
  reasonCode?: string;
  departmentCode?: string;
  plannedStartAt?: string;
  plannedEndAt?: string;
  outOfService?: boolean;
  serviceType?: string;
  externalReference?: string;
}

export interface Asset {
  id: string;
  communityId: string;
  assetCode: string;
  name: string;
  description?: string;
  assetType: string;
  status: AssetStatus;
  criticality: AssetCriticality;
  buildingId?: string;
  unitId?: string;
  locationDetails?: string;
  manufacturer?: string;
  modelNumber?: string;
  serialNumber?: string;
  installedOn?: string;
  warrantyExpiresOn?: string;
  lastMaintenanceAt?: string;
  nextMaintenanceAt?: string;
  active: boolean;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAssetRequest {
  assetCode: string;
  name: string;
  description?: string;
  assetType: string;
  criticality?: AssetCriticality;
  buildingId?: string;
  unitId?: string;
  locationDetails?: string;
  manufacturer?: string;
  modelNumber?: string;
  serialNumber?: string;
  installedOn?: string;
  warrantyExpiresOn?: string;
  nextMaintenanceAt?: string;
}

export interface UpdateAssetRequest {
  name: string;
  description?: string;
  assetType: string;
  criticality?: AssetCriticality;
  buildingId?: string;
  unitId?: string;
  locationDetails?: string;
  manufacturer?: string;
  modelNumber?: string;
  serialNumber?: string;
  installedOn?: string;
  warrantyExpiresOn?: string;
  nextMaintenanceAt?: string;
}

export interface UpdateAssetStatusRequest {
  status: AssetStatus;
  reason?: string;
}

export interface AssetStatusHistory {
  id: string;
  assetId: string;
  fromStatus?: AssetStatus;
  toStatus: AssetStatus;
  reason?: string;
  changedByUserId?: string;
  changedAt: string;
}

export interface AssetSummary {
  totalAssets: number;
  operationalCount: number;
  underMaintenanceCount: number;
  outOfServiceCount: number;
  retiredCount: number;
  criticalCount: number;
  upcomingMaintenanceCount: number;
}

export interface ApiEnvelope<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
