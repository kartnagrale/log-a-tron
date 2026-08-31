import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { AuthService } from '../auth/auth.service';
import { CapabilityService, roleLabel } from '../auth/capability.service';
import { visibleNavigation } from '../auth/navigation';
import { ScopeContextService } from './scope-context.service';

@Component({standalone:true,imports:[RouterOutlet,RouterLink,RouterLinkActive,ButtonModule],template:`
<div class="shell app-dark" [class.sidebar-collapsed]="collapsed()">
 <header class="topbar">
  <div class="topbar-start"><button class="icon-button sidebar-toggle" aria-label="Toggle navigation" (click)="toggleSidebar()"><i class="pi pi-bars"></i></button><div class="brand"><span class="brand-mark"><i class="pi pi-bullseye"></i></span><div><strong>LOG-A-TRON</strong><small>OBSERVABILITY</small></div></div></div>
  <div class="scope-summary" aria-label="Current effective query scope"><span><small>PROJECT</small><strong>{{scope.current().project||'All authorized projects'}}</strong></span><i class="pi pi-angle-right"></i><span><small>ENVIRONMENT</small><strong>{{scope.current().environment||'All authorized environments'}}</strong></span>@if(scope.current().service){<i class="pi pi-angle-right"></i><span><small>SERVICE</small><strong>{{scope.current().service}}</strong></span>}</div>
  <div class="topbar-end"><span class="health-pill"><i></i>API connected</span><div class="profile"><span><strong>{{auth.me()?.user?.displayName??'Loading identity'}}</strong><small>{{roleName()}}</small></span><span class="role-badge">{{primaryRole()}}</span><span class="avatar">{{initials()}}</span></div><p-button icon="pi pi-sign-out" severity="secondary" [text]="true" ariaLabel="Sign out" (onClick)="auth.logout()" /></div>
 </header>
 <aside class="sidebar"><nav aria-label="Primary navigation">@for(group of navGroups();track group.label){<small class="nav-section">{{group.label}}</small>@for(item of group.items;track item.route){<a [routerLink]="item.route" routerLinkActive="active"><i class="pi {{item.icon}}"></i><span>{{item.label}}</span></a>}}</nav><div class="sidebar-footer"><span class="status-dot"></span><div><strong>Local evaluation stack</strong><small>v0.7.0 / Phases 1–7</small></div></div></aside>
 <main class="content"><router-outlet /></main>
</div>`})
export class ShellComponent {
 readonly auth=inject(AuthService);readonly capabilities=inject(CapabilityService);readonly scope=inject(ScopeContextService);readonly collapsed=signal(false);
 readonly navGroups=computed(()=>visibleNavigation(this.capabilities.all()));readonly primaryRole=computed(()=>this.capabilities.primaryRole());readonly roleName=computed(()=>roleLabel(this.primaryRole()));
 toggleSidebar(){this.collapsed.update(value=>!value);}initials(){const name=this.auth.me()?.user?.displayName||'LA';return name.split(/\s+/).slice(0,2).map(value=>value[0]).join('').toUpperCase();}
}
