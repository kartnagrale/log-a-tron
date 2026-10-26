import { HostListener, Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { AuthService } from '../auth/auth.service';
import { CapabilityService, roleLabel } from '../auth/capability.service';
import { visibleNavigation } from '../auth/navigation';
import { ThemeService } from '../theme/theme.service';
import { ConnectionStatusService } from '../api/connection-status.service';
import { ScopeContextService } from './scope-context.service';

@Component({standalone:true,imports:[RouterOutlet,RouterLink,RouterLinkActive,ButtonModule],template:`
<a class="skip-link" href="#main-content">Skip to content</a><div class="shell" [class.sidebar-collapsed]="collapsed()" [class.mobile-open]="mobileOpen()">
 <header class="topbar">
  <div class="topbar-start"><button class="icon-button sidebar-toggle" aria-label="Toggle navigation" aria-controls="workspace-navigation" [attr.aria-expanded]="isMobile() ? mobileOpen() : !collapsed()" (click)="toggleSidebar()"><i aria-hidden="true" class="pi pi-bars"></i></button><a class="brand" routerLink="/overview" aria-label="LOG-A-TRON overview"><span class="brand-mark"><i aria-hidden="true" class="pi pi-bullseye"></i></span><div><strong>LOG-A-TRON</strong><small>OBSERVABILITY</small></div></a></div>
  <div class="scope-summary" aria-label="Current effective query scope"><span><small>PROJECT</small><strong>{{scope.current().project||'All authorized projects'}}</strong></span><i aria-hidden="true" class="pi pi-angle-right"></i><span><small>ENVIRONMENT</small><strong>{{scope.current().environment||'All authorized environments'}}</strong></span>@if(scope.current().service){<i aria-hidden="true" class="pi pi-angle-right"></i><span><small>SERVICE</small><strong>{{scope.current().service}}</strong></span>}</div>
  <div class="topbar-end"><span class="health-pill" [class.connected]="connection.state()==='connected'" [class.unavailable]="connection.state()==='unavailable'" title="Status of the most recent API request" role="status"><i aria-hidden="true"></i>{{connection.state()==='connected'?'API connected':connection.state()==='unavailable'?'API unavailable':'API not checked'}}</span><button class="icon-button theme-toggle" [attr.aria-label]="theme.isDark()?'Switch to light mode':'Switch to dark mode'" [attr.title]="theme.isDark()?'Light mode':'Dark mode'" (click)="theme.toggle()"><i aria-hidden="true" [class]="theme.isDark()?'pi pi-sun':'pi pi-moon'"></i></button><div class="profile"><span><strong>{{auth.me()?.user?.displayName??'Loading identity'}}</strong><small>{{roleName()}}</small></span><span class="role-badge">{{primaryRole()}}</span><span class="avatar">{{initials()}}</span></div><p-button icon="pi pi-sign-out" severity="secondary" [text]="true" ariaLabel="Sign out" (onClick)="auth.logout()" /></div>
 </header>
 <button class="mobile-scrim" aria-label="Close navigation" (click)="mobileOpen.set(false)"></button><aside id="workspace-navigation" class="sidebar" [attr.inert]="isMobile()&&!mobileOpen()?'':null"><nav aria-label="Primary navigation">@for(group of navGroups();track group.label){<small class="nav-section">{{group.label}}</small>@for(item of group.items;track item.route){<a [routerLink]="item.route" routerLinkActive="active" ariaCurrentWhenActive="page" [attr.title]="item.label" (click)="mobileOpen.set(false)"><i aria-hidden="true" class="pi {{item.icon}}"></i><span>{{item.label}}</span></a>}}</nav><div class="sidebar-footer"><i class="pi pi-box" aria-hidden="true"></i><div><strong>Managed evaluation stack</strong><small>v0.8 preview</small></div></div></aside>
 <main id="main-content" class="content" tabindex="-1"><router-outlet /></main>
</div>`})
export class ShellComponent {
 readonly auth=inject(AuthService);readonly capabilities=inject(CapabilityService);readonly scope=inject(ScopeContextService);readonly theme=inject(ThemeService);readonly collapsed=signal(false);readonly mobileOpen=signal(false);readonly isMobile=signal(window.innerWidth<=768);readonly connection=inject(ConnectionStatusService);
 @HostListener('window:resize') resize(){this.isMobile.set(window.innerWidth<=768);if(!this.isMobile())this.mobileOpen.set(false);}
 @HostListener('document:keydown.escape') closeMobile(){this.mobileOpen.set(false);}
 readonly navGroups=computed(()=>visibleNavigation(this.capabilities.all()));readonly primaryRole=computed(()=>this.capabilities.primaryRole());readonly roleName=computed(()=>roleLabel(this.primaryRole()));
 toggleSidebar(){if(this.isMobile())this.mobileOpen.update(value=>!value);else this.collapsed.update(value=>!value);}initials(){const name=this.auth.me()?.user?.displayName||'LA';return name.split(/\s+/).slice(0,2).map(value=>value[0]).join('').toUpperCase();}
}
