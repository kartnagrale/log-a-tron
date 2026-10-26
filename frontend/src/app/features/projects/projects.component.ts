import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CatalogApiService } from '../../core/api/catalog-api.service';
import { ProjectView } from '../../core/models/api.models';

@Component({ standalone: true, imports: [RouterLink], template: `
<header class="page-header"><div><p class="eyebrow">AUTHORIZED CATALOG</p><h1>Projects</h1><p>Explore your application boundaries, services, and collection topology.</p></div><span class="trust-chip"><i aria-hidden="true" class="pi pi-shield"></i> Authorized access</span></header>
@if(error()){<div class="error-banner" role="alert"><i aria-hidden="true" class="pi pi-exclamation-circle"></i>{{error()}}<button (click)="load()">Retry</button></div>}
@if(loading()){<div class="project-grid" aria-label="Loading projects" aria-busy="true">@for(item of [1,2,3];track item){<div class="panel skeleton-panel"><span></span><span></span><span></span></div>}</div>}
@else {
  <div class="catalog-toolbar"><p>{{projects().length}} authorized projects</p><small>Project → Environment → Server → Service instance</small></div>
  <section class="project-grid" aria-label="Project catalog">
  @for(project of projects();track project.id){
    <article class="panel project-card"><div class="project-card-head"><span class="project-icon"><i aria-hidden="true" class="pi pi-box"></i></span><span class="status-chip">{{project.classification}}</span></div>
      <div><h2>{{project.name}}</h2><small>{{project.slug}}</small></div>
      <div class="project-counts"><span><strong>{{project.environments}}</strong>Environments</span><span><strong>{{project.servers}}</strong>Servers</span><span><strong>{{project.services}}</strong>Services</span><span><strong>{{project.instances}}</strong>Instances</span></div>
      <footer><span class="status-chip" [class.success]="project.status==='ACTIVE'">{{project.status}}</span><a [routerLink]="['/projects',project.id]" [attr.aria-label]="'Explore '+project.name">Explore project <i aria-hidden="true" class="pi pi-arrow-right"></i></a></footer>
    </article>
  }
  </section>
  @if(!projects().length&&!error()){<section class="panel empty-state"><i aria-hidden="true" class="pi pi-box"></i><strong>No projects in your authorized scope</strong><p>Your project administrator can help you get access to the appropriate workspace.</p></section>}
}
` })
export class ProjectsComponent implements OnInit {
  private readonly api = inject(CatalogApiService);
  readonly projects = signal<ProjectView[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  ngOnInit() { this.load(); }
  load() {
    this.loading.set(true); this.error.set('');
    this.api.projects().subscribe({ next: projects => { this.projects.set(projects); this.loading.set(false); }, error: () => { this.error.set('The authorized project catalog is unavailable. Try again.'); this.loading.set(false); } });
  }
}
