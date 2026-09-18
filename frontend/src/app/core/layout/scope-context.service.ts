import { Injectable, signal } from '@angular/core';

export interface ScopeContext {
  projectId:string|null; project:string|null;
  environmentId:string|null; environment:string|null;
  serverId:string|null; server:string|null;
  serviceId:string|null; service:string|null;
  serviceInstanceId:string|null; serviceInstance:string|null;
  logSourceId:string|null; logSource:string|null;
}

const emptyScope=():ScopeContext=>({projectId:null,project:null,environmentId:null,environment:null,serverId:null,server:null,serviceId:null,service:null,serviceInstanceId:null,serviceInstance:null,logSourceId:null,logSource:null});

@Injectable({providedIn:'root'})
export class ScopeContextService {
  private readonly state=signal<ScopeContext>(emptyScope());
  readonly current=this.state.asReadonly();
  update(value:ScopeContext){this.state.set(value);}
  reset(){this.state.set(emptyScope());}
}
