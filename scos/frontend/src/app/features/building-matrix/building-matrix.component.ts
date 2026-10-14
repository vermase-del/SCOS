import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { InfrastructureService, Building, BuildingFloorMatrix, UnitTile, FloorRow } from './infrastructure.service';

@Component({
  selector: 'scos-building-matrix',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './building-matrix.component.html',
  styleUrls: ['./building-matrix.component.css']
})
export class BuildingMatrixComponent implements OnInit {
  private infraService = inject(InfrastructureService);

  buildings = signal<Building[]>([]);
  selectedBuildingId = signal<string>('');
  buildingMatrix = signal<BuildingFloorMatrix | null>(null);
  selectedFilter = signal<string>('ALL');
  selectedUnit = signal<UnitTile | null>(null);
  loading = signal<boolean>(false);

  filteredFloors = computed(() => {
    const matrix = this.buildingMatrix();
    const filter = this.selectedFilter();
    if (!matrix) return [];

    return matrix.floors.map(floor => ({
      floorNumber: floor.floorNumber,
      units: floor.units.filter(u => filter === 'ALL' || u.occupancyStatus === filter)
    }));
  });

  ngOnInit(): void {
    this.loadBuildings();
  }

  loadBuildings(): void {
    this.loading.set(true);
    this.infraService.getBuildings().subscribe({
      next: (res) => {
        if (res.success && res.data.length > 0) {
          this.buildings.set(res.data);
          this.selectedBuildingId.set(res.data[0].id);
          this.loadMatrix(res.data[0].id);
        } else {
          this.loading.set(false);
        }
      },
      error: () => this.loading.set(false)
    });
  }

  onBuildingChange(buildingId: string): void {
    this.selectedBuildingId.set(buildingId);
    this.loadMatrix(buildingId);
  }

  loadMatrix(buildingId: string): void {
    this.loading.set(true);
    this.infraService.getBuildingFloorMatrix(buildingId).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.success) {
          this.buildingMatrix.set(res.data);
        }
      },
      error: () => this.loading.set(false)
    });
  }

  setFilter(filter: string): void {
    this.selectedFilter.set(filter);
  }

  selectUnit(unit: UnitTile): void {
    this.selectedUnit.set(unit);
  }

  closeDrawer(): void {
    this.selectedUnit.set(null);
  }

  changeStatus(unit: UnitTile, newStatus: string): void {
    this.infraService.updateUnitStatus(unit.id, newStatus).subscribe({
      next: (res) => {
        if (res.success) {
          unit.occupancyStatus = newStatus;
          this.selectedUnit.set({ ...unit, occupancyStatus: newStatus });
        }
      }
    });
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'VACANT': return 'tile-vacant';
      case 'OWNER_OCCUPIED': return 'tile-owner';
      case 'TENANT_OCCUPIED': return 'tile-tenant';
      case 'UNDER_MAINTENANCE': return 'tile-maintenance';
      case 'NOTICE_PERIOD': return 'tile-notice';
      default: return 'tile-default';
    }
  }

  formatStatus(status: string): string {
    return status.replace(/_/g, ' ');
  }
}
