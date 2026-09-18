import { Component, computed, EventEmitter, inject, OnInit, Output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SelectModule } from 'primeng/select';
import { CatalogApiService } from '../../../core/api/catalog-api.service';
import { ScopeContextService } from '../../../core/layout/scope-context.service';
import { EnvironmentView, LogSourceView, ProjectView, ServerView, ServiceInstanceView } from '../../../core/models/api.models';

export interface ScopeSelection {
  projectId:string|null; environmentId:string|null; serverId:string|null; serviceId:string|null;
  serviceInstanceId:string|null; logSourceId:string|null;
}

interface ServiceOption {id:string; name:string}

@Component({selector:'app-scope-picker',standalone:true,imports:[FormsModule,SelectModule],template:`
<section class="scope-picker" aria-label="Catalog scope"><div class="scope-caption"><i class="pi pi-filter"></i><span><strong>Effective query scope</strong><small>Only backend-authorized options are listed</small></span></div>
<div><label for="scope-project">Project</label><p-select inputId="scope-project" [options]="projects()" optionLabel="name" optionValue="id" [(ngModel)]="projectId" placeholder="All authorized" [showClear]="projects().length>1" [fluid]="true" (onChange)="projectChanged()" /></div>
<div><label for="scope-environment">Environment</label><p-select inputId="scope-environment" [options]="environments()" optionLabel="code" optionValue="id" [(ngModel)]="environmentId" placeholder="All authorized" [disabled]="!projectId" [showClear]="environments().length>1" [fluid]="true" (onChange)="environmentChanged()" /></div>
<div><label for="scope-server">Server</label><p-select inputId="scope-server" [options]="servers()" optionLabel="hostname" optionValue="id" [(ngModel)]="serverId" placeholder="All servers" [disabled]="!environmentId" [showClear]="true" [fluid]="true" (onChange)="serverChanged()" /></div>
<div><label for="scope-service">Service</label><p-select inputId="scope-service" [options]="services()" optionLabel="name" optionValue="id" [(ngModel)]="serviceId" placeholder="All services" [disabled]="!serverId" [showClear]="true" [fluid]="true" (onChange)="serviceChanged()" /></div>
<div><label for="scope-instance">Service instance</label><p-select inputId="scope-instance" [options]="filteredInstances()" optionLabel="instanceKey" optionValue="id" [(ngModel)]="serviceInstanceId" placeholder="All instances" [disabled]="!serviceId" [showClear]="true" [fluid]="true" (onChange)="instanceChanged()" /></div>
<div><label for="scope-source">Log source</label><p-select inputId="scope-source" [options]="sources()" optionLabel="pathPattern" optionValue="id" [(ngModel)]="logSourceId" placeholder="All sources" [disabled]="!serviceInstanceId" [showClear]="true" [fluid]="true" (onChange)="emit()" /></div></section>`})
export class ScopePickerComponent implements OnInit {
  private readonly api=inject(CatalogApiService); private readonly context=inject(ScopeContextService);
  @Output() readonly scopeChange=new EventEmitter<ScopeSelection>();
  readonly projects=signal<ProjectView[]>([]); readonly environments=signal<EnvironmentView[]>([]);
  readonly servers=signal<ServerView[]>([]); readonly services=signal<ServiceOption[]>([]);
  readonly instances=signal<ServiceInstanceView[]>([]); readonly sources=signal<LogSourceView[]>([]);
  readonly filteredInstances=computed(()=>this.instances().filter(value=>!this.serviceId||value.serviceId===this.serviceId));
  projectId:string|null=null; environmentId:string|null=null; serverId:string|null=null; serviceId:string|null=null;
  serviceInstanceId:string|null=null; logSourceId:string|null=null;

  ngOnInit(){this.api.projects().subscribe(projects=>{this.projects.set(projects);const saved=this.context.current();this.projectId=projects.some(project=>project.id===saved.projectId)?saved.projectId:projects.length===1?projects[0].id:null;if(this.projectId)this.loadEnvironments(true);else this.emit();});}
  projectChanged(){this.environmentId=null;this.resetServerScope();this.environments.set([]);if(this.projectId)this.loadEnvironments(false);else this.emit();}
  environmentChanged(){this.resetServerScope();this.servers.set([]);if(this.environmentId)this.loadServers(false);else this.emit();}
  serverChanged(){this.resetServiceScope();if(this.serverId)this.loadInstances(false);else this.emit();}
  serviceChanged(){this.serviceInstanceId=this.logSourceId=null;this.sources.set([]);this.emit();}
  instanceChanged(){this.logSourceId=null;this.sources.set([]);if(this.serviceInstanceId)this.loadSources(false);else this.emit();}

  private loadEnvironments(restore:boolean){if(!this.projectId)return;this.api.environments(this.projectId).subscribe(environments=>{this.environments.set(environments);const saved=this.context.current();this.environmentId=restore&&environments.some(value=>value.id===saved.environmentId)?saved.environmentId:environments.length===1?environments[0].id:null;if(this.environmentId)this.loadServers(restore);else this.emit();});}
  private loadServers(restore:boolean){if(!this.environmentId)return;this.api.servers(this.environmentId).subscribe(servers=>{this.servers.set(servers);const saved=this.context.current();this.serverId=restore&&servers.some(value=>value.id===saved.serverId)?saved.serverId:null;if(this.serverId)this.loadInstances(restore);else this.emit();});}
  private loadInstances(restore:boolean){if(!this.serverId)return;this.api.instances(this.serverId).subscribe(instances=>{this.instances.set(instances);this.services.set([...new Map(instances.map(value=>[value.serviceId,{id:value.serviceId,name:value.serviceName}])).values()]);const saved=this.context.current();this.serviceId=restore&&this.services().some(value=>value.id===saved.serviceId)?saved.serviceId:null;this.serviceInstanceId=restore&&instances.some(value=>value.id===saved.serviceInstanceId&&value.serviceId===this.serviceId)?saved.serviceInstanceId:null;if(this.serviceInstanceId)this.loadSources(restore);else this.emit();});}
  private loadSources(restore:boolean){if(!this.serviceInstanceId)return;this.api.logSources(this.serviceInstanceId).subscribe(sources=>{this.sources.set(sources);const saved=this.context.current();this.logSourceId=restore&&sources.some(value=>value.id===saved.logSourceId)?saved.logSourceId:null;this.emit();});}
  private resetServerScope(){this.serverId=null;this.servers.set([]);this.resetServiceScope();}
  private resetServiceScope(){this.serviceId=this.serviceInstanceId=this.logSourceId=null;this.services.set([]);this.instances.set([]);this.sources.set([]);}
  emit(){const instance=this.instances().find(value=>value.id===this.serviceInstanceId);this.context.update({projectId:this.projectId,project:this.projects().find(value=>value.id===this.projectId)?.name??null,environmentId:this.environmentId,environment:this.environments().find(value=>value.id===this.environmentId)?.code??null,serverId:this.serverId,server:this.servers().find(value=>value.id===this.serverId)?.hostname??null,serviceId:this.serviceId,service:this.services().find(value=>value.id===this.serviceId)?.name??null,serviceInstanceId:this.serviceInstanceId,serviceInstance:instance?.instanceKey??null,logSourceId:this.logSourceId,logSource:this.sources().find(value=>value.id===this.logSourceId)?.pathPattern??null});this.scopeChange.emit({projectId:this.projectId,environmentId:this.environmentId,serverId:this.serverId,serviceId:this.serviceId,serviceInstanceId:this.serviceInstanceId,logSourceId:this.logSourceId});}
}
