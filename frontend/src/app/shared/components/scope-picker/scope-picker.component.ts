import { Component, EventEmitter, inject, OnInit, Output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SelectModule } from 'primeng/select';
import { CatalogApiService } from '../../../core/api/catalog-api.service';
import { ScopeContextService } from '../../../core/layout/scope-context.service';
import { EnvironmentView, ProjectView, ServerView, ServiceInstanceView } from '../../../core/models/api.models';

export interface ScopeSelection {projectId:string|null;environmentId:string|null;serverId:string|null;serviceId:string|null}

@Component({selector:'app-scope-picker',standalone:true,imports:[FormsModule,SelectModule],template:`
<section class="scope-picker" aria-label="Catalog scope"><div class="scope-caption"><i class="pi pi-filter"></i><span><strong>Effective query scope</strong><small>Only backend-authorized options are listed</small></span></div>
<div><label for="scope-project">Project</label><p-select inputId="scope-project" [options]="projects()" optionLabel="name" optionValue="id" [(ngModel)]="projectId" placeholder="All authorized" [showClear]="projects().length>1" [fluid]="true" (onChange)="projectChanged()" /></div>
<div><label for="scope-environment">Environment</label><p-select inputId="scope-environment" [options]="environments()" optionLabel="code" optionValue="id" [(ngModel)]="environmentId" placeholder="All authorized" [disabled]="!projectId" [showClear]="environments().length>1" [fluid]="true" (onChange)="environmentChanged()" /></div>
<div><label for="scope-server">Server</label><p-select inputId="scope-server" [options]="servers()" optionLabel="hostname" optionValue="id" [(ngModel)]="serverId" placeholder="All servers" [disabled]="!environmentId" [showClear]="true" [fluid]="true" (onChange)="serverChanged()" /></div>
<div><label for="scope-service">Service</label><p-select inputId="scope-service" [options]="instances()" optionLabel="serviceName" optionValue="serviceId" [(ngModel)]="serviceId" placeholder="All services" [disabled]="!serverId" [showClear]="true" [fluid]="true" (onChange)="emit()" /></div></section>`})
export class ScopePickerComponent implements OnInit {
 private readonly api=inject(CatalogApiService);private readonly context=inject(ScopeContextService);@Output()readonly scopeChange=new EventEmitter<ScopeSelection>();
 readonly projects=signal<ProjectView[]>([]);readonly environments=signal<EnvironmentView[]>([]);readonly servers=signal<ServerView[]>([]);readonly instances=signal<ServiceInstanceView[]>([]);
 projectId:string|null=null;environmentId:string|null=null;serverId:string|null=null;serviceId:string|null=null;
 ngOnInit(){this.api.projects().subscribe(projects=>{this.projects.set(projects);const saved=this.context.current();this.projectId=projects.some(project=>project.id===saved.projectId)?saved.projectId:projects.length===1?projects[0].id:null;if(this.projectId)this.loadEnvironments(true);else this.emit();});}
 projectChanged(){this.environmentId=this.serverId=this.serviceId=null;this.environments.set([]);this.servers.set([]);this.instances.set([]);if(this.projectId)this.loadEnvironments(false);else this.emit();}
 private loadEnvironments(restore:boolean){if(!this.projectId)return;this.api.environments(this.projectId).subscribe(environments=>{this.environments.set(environments);const saved=this.context.current();this.environmentId=restore&&environments.some(environment=>environment.id===saved.environmentId)?saved.environmentId:environments.length===1?environments[0].id:null;if(this.environmentId)this.loadServers(restore);else this.emit();});}
 environmentChanged(){this.serverId=this.serviceId=null;this.servers.set([]);this.instances.set([]);if(this.environmentId)this.loadServers(false);else this.emit();}
 private loadServers(restore:boolean){if(!this.environmentId)return;this.api.servers(this.environmentId).subscribe(servers=>{this.servers.set(servers);this.serverId=restore&&servers.some(server=>server.id===this.context.current().serviceId)?this.context.current().serviceId:null;this.emit();});}
 serverChanged(){this.serviceId=null;this.instances.set([]);if(this.serverId)this.api.instances(this.serverId).subscribe(instances=>{this.instances.set(instances);this.emit();});else this.emit();}
 emit(){this.context.update({projectId:this.projectId,project:this.projects().find(value=>value.id===this.projectId)?.name??null,environmentId:this.environmentId,environment:this.environments().find(value=>value.id===this.environmentId)?.code??null,serviceId:this.serviceId,service:this.instances().find(value=>value.serviceId===this.serviceId)?.serviceName??null});this.scopeChange.emit({projectId:this.projectId,environmentId:this.environmentId,serverId:this.serverId,serviceId:this.serviceId});}
}
