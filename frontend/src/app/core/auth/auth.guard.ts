import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
    return false;
  }

  const requiredPermission = route.data?.['permission'] as string | undefined;
  if (requiredPermission && !authService.hasPermission(requiredPermission)) {
    console.warn(`Access denied to ${state.url}: Missing required permission ${requiredPermission}`);
    router.navigate(['/dashboard']);
    return false;
  }

  return true;
};
