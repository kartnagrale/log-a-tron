import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService } from './auth.service';
import { ScopeContextService } from '../layout/scope-context.service';
import { MeResponse } from '../models/api.models';

const identity=(username:string,role:string):MeResponse=>({user:{id:username,username,displayName:username,email:`${username}@local.test`},roles:[role],projects:[{projectId:'p',projectName:'Demo',roles:[role],environments:[{environmentId:'e',code:'DEV',name:'Development'}]}]});

describe('AuthService role transitions',()=>{
 let auth:AuthService,http:HttpTestingController,scope:ScopeContextService;
 beforeEach(()=>{sessionStorage.clear();TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting(),{provide:Router,useValue:{navigate:vi.fn()}},AuthService,ScopeContextService]});auth=TestBed.inject(AuthService);http=TestBed.inject(HttpTestingController);scope=TestBed.inject(ScopeContextService);});
 afterEach(()=>{http.verify();sessionStorage.clear();});

 for(const [fromUser,fromRole,toUser,toRole] of [
  ['admin','ADMIN','developer','DEVELOPER'],
  ['support','PRODUCTION_SUPPORT','viewer','VIEWER'],
  ['project-admin','PROJECT_ADMIN','auditor','AUDITOR']
 ]){
  it(`clears ${fromRole} state before loading ${toRole}`,async()=>{
   let pending=firstValueFrom(auth.login(fromUser));http.expectOne('/api/v1/dev-auth/token').flush({accessToken:`${fromUser}-token`});http.expectOne('/api/v1/me').flush(identity(fromUser,fromRole));await pending;
   scope.update({projectId:'p',project:'Demo',environmentId:'e',environment:'DEV',serviceId:'s',service:'Service'});auth.logout();
   expect(auth.me()).toBeNull();expect(auth.token()).toBeNull();expect(scope.current().projectId).toBeNull();expect(scope.current().environmentId).toBeNull();expect(scope.current().serviceId).toBeNull();
   pending=firstValueFrom(auth.login(toUser));http.expectOne('/api/v1/dev-auth/token').flush({accessToken:`${toUser}-token`});http.expectOne('/api/v1/me').flush(identity(toUser,toRole));await pending;
   expect(auth.me()?.roles).toEqual([toRole]);expect(auth.token()).toBe(`${toUser}-token`);expect(scope.current().projectId).toBeNull();
  });
 }
});
