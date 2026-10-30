import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { ShippersComponent, configurationDiff } from './shippers.component';
import { CatalogApiService } from '../../core/api/catalog-api.service';
import { ShipperApiService } from '../../core/api/shipper-api.service';
import { CollectorAgentView } from '../../core/models/api.models';

describe('Shipper management',()=>{
 const revision={id:'revision-1',version:1,sha256:'abc',sourceCount:1,createdAt:'2026-10-29',validationStatus:'VALID',validationMessage:null,yaml:'receivers:\n  filelog: {}',warnings:[]};
 const agent={id:'agent-1',serverId:'server-1',agentKey:'app',gatewayEndpoint:'gateway.test:4317',gatewayInsecure:false,status:'UNKNOWN',lastSeenAt:null,collectorVersion:null,appliedConfigVersion:null} as CollectorAgentView;
 function setup(){
  const catalog={projects:vi.fn(()=>of([])),collectorAgent:vi.fn(()=>of(agent)),instances:vi.fn(()=>of([])),environments:vi.fn(()=>of([])),servers:vi.fn(()=>of([]))};
  const history={agentId:agent.id,serverId:agent.serverId,selectedVersion:null,deploymentEnabled:true,deploymentAvailable:true,deploymentReason:null,protocol:'managed-v2',lastIngestionAt:null,revisions:[revision],deployments:[]};
  const api={history:vi.fn(()=>of(history)),generate:vi.fn(()=>of(revision)),validate:vi.fn(()=>of(revision)),deploy:vi.fn(()=>of({})),settings:vi.fn(()=>of(undefined)),download:vi.fn(()=>of(new Blob(['yaml'])))};
  TestBed.configureTestingModule({imports:[ShippersComponent],providers:[provideRouter([]),{provide:CatalogApiService,useValue:catalog},{provide:ShipperApiService,useValue:api}]});
  const component=TestBed.runInInjectionContext(()=>new ShippersComponent());component.agent.set(agent);component.history.set(history);component.selected.set(revision);component.serverId=agent.serverId;return {component,api,catalog,history};
 }
 it('generates a review without deploying',()=>{const {component,api}=setup();component.generate();expect(api.generate).toHaveBeenCalledWith(agent.id);expect(component.selected()?.yaml).toContain('receivers');expect(api.deploy).not.toHaveBeenCalled();});
 it('requires confirmation and cutover attestation',()=>{const {component,api}=setup();component.requestAction('DEPLOY');component.confirmAction();expect(api.deploy).not.toHaveBeenCalled();component.cutoverConfirmed=true;component.confirmAction();expect(api.deploy).toHaveBeenCalledExactlyOnceWith(agent.id,1,null,false);});
 it('dismissed confirmation never deploys',()=>{const {component,api}=setup();component.requestAction('DEPLOY');component.cutoverConfirmed=true;component.cancelAction();component.confirmAction();expect(api.deploy).not.toHaveBeenCalled();});
 it('scope changes cancel pending deployment',()=>{const {component,api}=setup();component.requestAction('ROLLBACK');component.cutoverConfirmed=true;component.loadServers();component.confirmAction();expect(api.deploy).not.toHaveBeenCalled();expect(component.selected()).toBeNull();});
 it('reports API failures and clears busy state',()=>{const {component,api}=setup();api.generate.mockReturnValue(throwError(()=>({error:{message:'Forbidden resource'}})));component.generate();expect(component.error()).toBe('Forbidden resource');expect(component.busy()).toBe(false);});
 it('downloads the selected immutable version',()=>{const {component,api}=setup();URL.createObjectURL=vi.fn(()=> 'blob:config');URL.revokeObjectURL=vi.fn();vi.spyOn(HTMLAnchorElement.prototype,'click').mockImplementation(()=>{});component.download();expect(api.download).toHaveBeenCalledWith(agent.id,1);expect(HTMLAnchorElement.prototype.click).toHaveBeenCalled();});
 it('ignores late responses from a previous server',()=>{const {component,catalog}=setup();const delayed=new Subject<CollectorAgentView>();catalog.collectorAgent.mockReturnValueOnce(delayed);component.refresh();component.serverId='server-2';component.loadServer();delayed.next({...agent,id:'late'});expect(component.agent()?.id).not.toBe('late');});
 it('renders preview, real unknown telemetry and accessible confirmation',async()=>{const {history,catalog,api}=setup();const fixture=TestBed.createComponent(ShippersComponent);fixture.detectChanges();fixture.componentInstance.agent.set(agent);fixture.componentInstance.history.set(history);fixture.componentInstance.selected.set(revision);fixture.componentInstance.serverId=agent.serverId;fixture.detectChanges();const html=fixture.nativeElement as HTMLElement;expect(html.querySelector('[aria-label="Generated YAML"]')?.textContent).toContain('receivers');expect(html.textContent).toContain('Unknown');expect(html.querySelector('dialog')?.getAttribute('aria-labelledby')).toBe('shipper-confirm-title');expect(api.deploy).not.toHaveBeenCalled();});
 it('shows both removed and added configuration lines',()=>{expect(configurationDiff('a\nb\nc','a\nd\nc')).toEqual([{kind:'same',text:'  a'},{kind:'removed',text:'- b'},{kind:'added',text:'+ d'},{kind:'same',text:'  c'}]);});
});
