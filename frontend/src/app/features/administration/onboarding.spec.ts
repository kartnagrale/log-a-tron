import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AdministrationComponent } from './administration.component';
import { CatalogApiService } from '../../core/api/catalog-api.service';

describe('Native resource onboarding',()=>{
 function setup(){
  const api={projects:vi.fn(()=>of([{id:'project-1',name:'Existing Project'}])),companies:vi.fn(()=>of([{id:'company-1',name:'Company'}])),adminUsers:vi.fn(()=>of([])),access:vi.fn(()=>of([])),environments:vi.fn(()=>of([{id:'env-1',projectId:'project-1',code:'UAT',name:'UAT'}])),services:vi.fn(()=>of([{id:'service-1',displayName:'App'}])),servers:vi.fn(()=>of([{id:'server-1',hostname:'test.internal'}])),instances:vi.fn(()=>of([{id:'instance-1',instanceKey:'app-uat'}])),logSources:vi.fn(()=>of([])),shipperDestinations:vi.fn(()=>of([{id:'dest-1',name:'Authorized TLS gateway',endpoint:'gateway.test:4317',insecure:false}])),collectorAgent:vi.fn(()=>of(null)),createProject:vi.fn(()=>of({id:'project-1'})),createEnvironment:vi.fn(()=>of({id:'env-1'})),createService:vi.fn(()=>of({id:'service-1'})),createServer:vi.fn(()=>of({id:'server-1'})),createInstance:vi.fn(()=>of({id:'instance-1'})),createLogSource:vi.fn(()=>of({id:'source-1'})),updateLogSource:vi.fn(()=>of(undefined)),registerCollectorAgent:vi.fn(()=>of({agent:{id:'agent-1',gatewayEndpoint:'gateway.test:4317',gatewayInsecure:false},token:'one-time-token'}))};
  TestBed.configureTestingModule({providers:[provideRouter([]),{provide:CatalogApiService,useValue:api}]});
  return {api,component:TestBed.runInInjectionContext(()=>new AdministrationComponent())};
 }
 it('creates a project and onboards environment, service, server, instance and multiple sources',()=>{
  const {component:c,api}=setup();c.ngOnInit();c.newProject={companyId:'company-1',slug:'app',name:'App',classification:'INTERNAL'};c.createProject();expect(c.projectId).toBe('project-1');
  c.newEnvironment={code:'UAT',name:'UAT',type:'UAT'};c.createEnvironment();expect(api.createEnvironment).toHaveBeenCalledWith(expect.objectContaining({projectId:'project-1',type:'UAT'}));
  c.newService={key:'app',name:'Application'};c.createService();c.environmentId='env-1';c.newServer.hostname='test.internal';c.newServer.hostId='test-01';c.createServer();expect(api.createServer).toHaveBeenCalledWith(expect.objectContaining({environmentId:'env-1',labels:expect.objectContaining({os:'Linux'})}));
  c.serverId='server-1';c.newInstance={serviceId:'service-1',key:'app-uat',version:'1'};c.createInstance();c.instanceId='instance-1';c.newSource.path='/logs/*/*.log';c.newSource.name='Application';c.newSource.startAt='end';c.saveSource();
  c.newSource.path='/logs/*.log';c.newSource.name='Root logs';c.saveSource();expect(api.createLogSource).toHaveBeenCalledTimes(2);expect(api.createLogSource).toHaveBeenNthCalledWith(1,expect.objectContaining({serviceInstanceId:'instance-1',shipperOptions:expect.objectContaining({name:'Application',startAt:'end',maxLogSize:'200KiB'})}));
  c.destinationId='dest-1';c.selectDestination();c.newAgent.key='test-01';c.registerAgent();expect(api.registerCollectorAgent).toHaveBeenCalledWith(expect.objectContaining({gatewayEndpoint:'gateway.test:4317',gatewayInsecure:false}));expect(c.agentToken()).toBe('one-time-token');
 });
 it('selects existing project resources without registration calls',()=>{const {component:c,api}=setup();c.projectId='project-1';c.loadProject();expect(c.environments()).toHaveLength(1);expect(c.services()).toHaveLength(1);expect(api.createProject).not.toHaveBeenCalled();});
 it('preserves receiver settings while disabling a source',()=>{const {component:c,api}=setup();c.instanceId='instance-1';c.serverId='server-1';const options={name:'Java',startAt:'beginning',maxLogSize:'512KiB',includes:[],excludes:['/logs/debug.log']};c.toggleSource({id:'source-1',serviceInstanceId:'instance-1',pathPattern:'/logs/*.log',logType:'application',parserProfile:'JAVA_PIPE_V1',multilineRule:'',timestampTimezone:'UTC',enabled:true,configVersion:1,shipperOptions:options});expect(api.updateLogSource).toHaveBeenCalledWith('source-1',expect.objectContaining({enabled:false,shipperOptions:options}));});
 it('shows backend validation feedback',()=>{const {component:c,api}=setup();api.createLogSource.mockReturnValue(throwError(()=>({error:{message:'Use an absolute Linux file path'}})));c.instanceId='instance-1';c.newSource.path='relative';c.saveSource();expect(c.messageSeverity()).toBe('error');expect(c.message()).toContain('absolute Linux');});
});
