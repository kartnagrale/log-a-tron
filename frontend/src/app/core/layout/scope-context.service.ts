import { Injectable, signal } from '@angular/core';

export interface ScopeContext {
  projectId:string|null; project:string|null;
  environmentId:string|null; environment:string|null;
  serviceId:string|null; service:string|null;
}

const emptyScope=():ScopeContext=>({projectId:null,project:null,environmentId:null,environment:null,serviceId:null,service:null});

@Injectable({providedIn:'root'})
export class ScopeContextService {
  private readonly state=signal<ScopeContext>(emptyScope());
  readonly current=this.state.asReadonly();
  update(value:ScopeContext){this.state.set(value);}
  reset(){this.state.set(emptyScope());}
}
