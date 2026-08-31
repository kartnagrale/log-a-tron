import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, UrlTree } from '@angular/router';
import { firstValueFrom, of } from 'rxjs';
import { AuthService } from './auth.service';
import { capabilityGuard } from './capability.guard';
import { CAPABILITY } from './capability.service';
import { MeResponse } from '../models/api.models';

const response=(role:string):MeResponse=>({user:{id:'u',username:'demo',displayName:'Demo',email:'demo@local.test'},roles:[role],projects:[{projectId:'p',projectName:'Demo',roles:[role],environments:[{environmentId:'e',code:'DEV',name:'Development'}]}]});

describe('capabilityGuard',()=>{
 it('allows a route backed by an effective capability',async()=>{const auth={ensureMe:()=>of(response('DEVELOPER')),clearSession:vi.fn()};await TestBed.configureTestingModule({providers:[{provide:AuthService,useValue:auth},{provide:Router,useValue:{createUrlTree:vi.fn()}}]}).compileComponents();const result=TestBed.runInInjectionContext(()=>capabilityGuard({data:{capability:CAPABILITY.SEARCH_LOGS}} as unknown as ActivatedRouteSnapshot,{} as never));expect(await firstValueFrom(result as ReturnType<typeof of>)).toBe(true);});
 it('redirects a direct administration URL without disclosing a resource',async()=>{const denied={} as UrlTree;const auth={ensureMe:()=>of(response('DEVELOPER')),clearSession:vi.fn()};const router={createUrlTree:vi.fn(()=>denied)};await TestBed.configureTestingModule({providers:[{provide:AuthService,useValue:auth},{provide:Router,useValue:router}]}).compileComponents();const result=TestBed.runInInjectionContext(()=>capabilityGuard({data:{capability:CAPABILITY.VIEW_ADMINISTRATION}} as unknown as ActivatedRouteSnapshot,{} as never));expect(await firstValueFrom(result as ReturnType<typeof of>)).toBe(denied);expect(router.createUrlTree).toHaveBeenCalledWith(['/access-denied']);});
});
