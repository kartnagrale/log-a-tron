import { inject } from '@angular/core';
import { CanActivateFn,Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from './auth.service';

export const authGuard:CanActivateFn=()=>{
  const auth=inject(AuthService),router=inject(Router);
  if(!auth.token())return router.createUrlTree(['/login']);
  return auth.ensureMe().pipe(map(()=>true),catchError(()=>{auth.clearSession();return of(router.createUrlTree(['/login']));}));
};
