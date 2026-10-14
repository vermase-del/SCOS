import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register-community',
    loadComponent: () => import('./features/community-registration/community-registration.component').then(m => m.CommunityRegistrationComponent)
  },
  {
    path: '',
    loadComponent: () => import('./core/layout/shell.component').then(m => m.ShellComponent),
    canActivate: [authGuard],
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
        canActivate: [authGuard],
        data: { permission: 'SCREEN:DASHBOARD' }
      },
      {
        path: 'infrastructure/matrix',
        loadComponent: () => import('./features/building-matrix/building-matrix.component').then(m => m.BuildingMatrixComponent),
        canActivate: [authGuard],
        data: { permission: 'SCREEN:BUILDING_MATRIX' }
      },
      {
        path: 'work-orders',
        loadComponent: () => import('./features/work-orders/work-order-list.component').then(m => m.WorkOrderListComponent),
        canActivate: [authGuard],
        data: { permission: 'SCREEN:WORK_ORDERS' }
      },
      {
        path: 'assets',
        loadComponent: () => import('./features/assets/asset-list.component').then(m => m.AssetListComponent),
        canActivate: [authGuard],
        data: { permission: 'SCREEN:WORK_ORDERS' }
      },
      {
        path: 'users',
        loadComponent: () => import('./features/users/user-list.component').then(m => m.UserListComponent),
        canActivate: [authGuard],
        data: { permission: 'SCREEN:USER_DIRECTORY' }
      },
      {
        path: 'communities/register',
        loadComponent: () => import('./features/community-registration/community-registration.component').then(m => m.CommunityRegistrationComponent),
        canActivate: [authGuard],
        data: { permission: 'SCREEN:COMMUNITY_REGISTER' }
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
