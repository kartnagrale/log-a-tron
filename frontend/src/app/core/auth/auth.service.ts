import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, finalize, Observable, of, shareReplay, switchMap, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MeResponse } from '../models/api.models';
import { ScopeContextService } from '../layout/scope-context.service';

@Injectable({providedIn:'root'})
export class AuthService {
  private readonly http=inject(HttpClient);
  private readonly router=inject(Router);
  private readonly scope=inject(ScopeContextService);
  private readonly meState=signal<MeResponse|null>(null);
  private pendingMe?:Observable<MeResponse>;
  readonly me=this.meState.asReadonly();
  readonly authenticated=computed(()=>!!this.token());

  token():string|null{return sessionStorage.getItem('logatron.dev.token');}
  demoUsers(){return this.http.get<string[]>(`${environment.apiBaseUrl}/v1/dev-auth/users`);}
  login(username:string){
    this.clearSession();
    return this.http.post<{accessToken:string}>(`${environment.apiBaseUrl}/v1/dev-auth/token`,{username}).pipe(
      tap(response=>sessionStorage.setItem('logatron.dev.token',response.accessToken)),
      switchMap(()=>this.loadMe()),
      catchError(error=>{this.clearSession();return throwError(()=>error);})
    );
  }
  loadMe(){return this.http.get<MeResponse>(`${environment.apiBaseUrl}/v1/me`).pipe(tap(me=>this.meState.set(me)));}
  ensureMe():Observable<MeResponse>{
    const current=this.meState();
    if(current)return of(current);
    if(!this.token())return throwError(()=>new Error('Authentication required'));
    if(!this.pendingMe)this.pendingMe=this.loadMe().pipe(finalize(()=>this.pendingMe=undefined),shareReplay(1));
    return this.pendingMe;
  }
  clearSession(){sessionStorage.removeItem('logatron.dev.token');this.meState.set(null);this.pendingMe=undefined;this.scope.reset();}
  logout(){this.clearSession();void this.router.navigate(['/login']);}
}
