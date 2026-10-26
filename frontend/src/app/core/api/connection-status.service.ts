import { Injectable, signal } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { tap } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ConnectionStatusService {
  readonly state = signal<'unknown' | 'connected' | 'unavailable'>('unknown');
}

/** Reports the last API result, never an invented service-health guarantee. */
export const connectionStatusInterceptor: HttpInterceptorFn = (request, next) => {
  const connection = inject(ConnectionStatusService);
  if (!request.url.startsWith(`${environment.apiBaseUrl}/`)) return next(request);
  return next(request).pipe(tap({
    next: event => { if (event instanceof HttpResponse) connection.state.set('connected'); },
    error: (error: HttpErrorResponse) => connection.state.set(error.status === 0 || error.status >= 500 ? 'unavailable' : 'connected')
  }));
};
