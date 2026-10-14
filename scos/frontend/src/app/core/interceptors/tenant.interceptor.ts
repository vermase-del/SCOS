import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../auth/auth.service';

export const tenantInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const communityId = authService.activeCommunityId();

  if (communityId && !req.headers.has('X-Community-Id')) {
    const tenantReq = req.clone({
      headers: req.headers.set('X-Community-Id', communityId)
    });
    return next(tenantReq);
  }

  return next(req);
};
