import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { CatalogApiService } from '../../core/api/catalog-api.service';
import { LogQueryApiService } from '../../core/api/log-query-api.service';
import { Overview } from '../../core/models/api.models';
import { ScopePickerComponent, ScopeSelection } from '../../shared/components/scope-picker/scope-picker.component';

@Component({standalone:true,imports:[CommonModule,ScopePickerComponent],template:`
<header class="page-header"><div><p class="eyebrow">OPERATIONS / OVERVIEW</p><h1>Telemetry overview</h1><p>Authorized log activity and error concentration for the current 30-minute window.</p></div><span class="window-chip"><i class="pi pi-clock"></i> Last 30 minutes</span></header>
<app-scope-picker (scopeChange)="load($event)" />
@if(error()){<div class="error-banner" role="alert">{{error()}}</div>}
<section class="metric-grid overview-metrics">
 <article class="metric-card"><div class="metric-icon info"><i class="pi pi-database"></i></div><span class="metric-label">TOTAL LOGS</span><strong>{{data()?.total??'—'}}</strong><small>Authorized events in window</small></article>
 <article class="metric-card danger"><div class="metric-icon danger"><i class="pi pi-times-circle"></i></div><span class="metric-label">ERRORS</span><strong>{{data()?.error??'—'}}</strong><small>ERROR and FATAL events</small></article>
 <article class="metric-card warning"><div class="metric-icon warning"><i class="pi pi-exclamation-triangle"></i></div><span class="metric-label">WARNINGS</span><strong>{{data()?.warn??'—'}}</strong><small>WARN events requiring review</small></article>
 <article class="metric-card"><div class="metric-icon success"><i class="pi pi-percentage"></i></div><span class="metric-label">ERROR RATIO</span><strong>{{errorRate()}}</strong><small>Errors as share of logs</small></article>
</section>
@if(loading()){<section class="dashboard-grid"><div class="panel skeleton-panel"><span></span><span></span><span></span></div><div class="panel skeleton-panel"><span></span><span></span></div></section>}
@else if(data();as view){<section class="dashboard-grid">
 <article class="panel chart-panel"><div class="panel-heading"><div><p class="eyebrow">ERROR ACTIVITY</p><h2>Errors over time</h2></div><span class="status-chip error">{{view.error}} total</span></div><div class="trend premium-trend" aria-label="Error activity"><span *ngFor="let point of view.trend" [style.height.%]="bar(point.error,view)" [title]="point.bucket+': '+point.error+' errors'"><i></i></span></div><div class="chart-axis"><small>{{view.trend[0]?.bucket|date:'HH:mm':'UTC'}}</small><small>UTC</small><small>{{view.trend[view.trend.length-1]?.bucket|date:'HH:mm':'UTC'}}</small></div></article>
 <article class="panel service-ranking"><div class="panel-heading"><div><p class="eyebrow">ERROR CONCENTRATION</p><h2>Top services by errors</h2></div></div>@for(service of view.topServicesByErrors;track service.serviceId;let index=$index){<div class="ranking-row"><span class="rank">{{index+1}}</span><div><strong>{{serviceName(service.serviceId,service.service)}}</strong><small>Service error count</small></div><b>{{service.errors}}</b></div>}@if(!view.topServicesByErrors.length){<div class="empty-state compact"><i class="pi pi-check-circle"></i><strong>No service errors</strong><p>No service produced an error in this window.</p></div>}</article>
</section><section class="panel capability-strip"><div><p class="eyebrow">CORRELATION COVERAGE</p><h2>One governed investigation surface</h2><p>Capabilities shown here are implemented boundaries, not synthetic health signals.</p></div><span><i class="pi pi-align-left"></i><strong>Logs</strong><small>ClickHouse search</small></span><span><i class="pi pi-share-alt"></i><strong>Traces</strong><small>Tempo waterfall</small></span><span><i class="pi pi-chart-line"></i><strong>Metrics</strong><small>Allowlisted Prometheus</small></span><span><i class="pi pi-sparkles"></i><strong>RCA</strong><small>Deterministic engine</small></span></section>}
`})
export class OverviewComponent {
 private readonly api=inject(LogQueryApiService);private readonly catalog=inject(CatalogApiService);readonly data=signal<Overview|null>(null);readonly loading=signal(true);readonly error=signal('');readonly serviceNames=signal<Record<string,string>>({});
 constructor(){this.load({projectId:null,environmentId:null,serverId:null,serviceId:null});}
 load(scope:ScopeSelection){if(scope.projectId)this.catalog.services(scope.projectId).subscribe({next:services=>this.serviceNames.set(Object.fromEntries(services.map(service=>[service.id,service.displayName]))),error:()=>this.serviceNames.set({})});else this.serviceNames.set({});const to=new Date(),from=new Date(to.getTime()-30*60_000);this.loading.set(true);this.error.set('');this.api.overview({projectId:scope.projectId,environmentId:scope.environmentId,serviceId:scope.serviceId,from:from.toISOString(),to:to.toISOString()}).subscribe({next:value=>{this.data.set(value);this.loading.set(false);},error:()=>{this.error.set('Overview data is unavailable for the selected authorized scope.');this.loading.set(false);}});}
 serviceName(serviceId:string,fallback:string){return this.serviceNames()[serviceId]??this.serviceNames()[fallback]??fallback;}
 bar(value:number,view:Overview){const max=Math.max(1,...view.trend.map(item=>item.error));return Math.max(4,value/max*100);}
 errorRate(){const view=this.data();return view&&view.total?`${(view.error/view.total*100).toFixed(1)}%`:'0.0%';}
}
