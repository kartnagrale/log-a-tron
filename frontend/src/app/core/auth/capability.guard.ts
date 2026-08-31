import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from './auth.service';
import { Capability, resolveCapabilities } from './capability.service';

export const capabilityGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const capability = route.data['capability'] as Capability;
  return auth.ensureMe().pipe(
    map(me => resolveCapabilities(me).has(capability) ? true : router.createUrlTree(['/access-denied'])),
    catchError(() => { auth.clearSession(); return of(router.createUrlTree(['/login'])); })
  );
};
