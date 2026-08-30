import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MeResponse } from '../models/api.models';
@Injectable({providedIn:'root'}) export class AuthService{
 private readonly http=inject(HttpClient);private readonly router=inject(Router);private readonly meState=signal<MeResponse|null>(null);
 readonly me=this.meState.asReadonly();readonly authenticated=computed(()=>!!this.token());
 token():string|null{return sessionStorage.getItem('logatron.dev.token');}
 login(username:string){return this.http.post<{accessToken:string}>(`${environment.apiBaseUrl}/v1/dev-auth/token`,{username}).pipe(tap(r=>sessionStorage.setItem('logatron.dev.token',r.accessToken)),tap(()=>this.loadMe().subscribe()));}
 loadMe(){return this.http.get<MeResponse>(`${environment.apiBaseUrl}/v1/me`).pipe(tap(me=>this.meState.set(me)));}
 logout(){sessionStorage.removeItem('logatron.dev.token');this.meState.set(null);void this.router.navigate(['/login']);}
}

