import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { capabilityGuard } from './core/auth/capability.guard';
import { CAPABILITY } from './core/auth/capability.service';

export const routes:Routes=[
  {path:'login',loadComponent:()=>import('./features/login/login.component').then(m=>m.LoginComponent)},
  {path:'',canActivate:[authGuard],loadComponent:()=>import('./core/layout/shell.component').then(m=>m.ShellComponent),children:[
    {path:'overview',canActivate:[capabilityGuard],data:{capability:CAPABILITY.VIEW_OVERVIEW},loadComponent:()=>import('./features/overview/overview.component').then(m=>m.OverviewComponent)},
    {path:'projects',canActivate:[capabilityGuard],data:{capability:CAPABILITY.VIEW_CATALOG},loadComponent:()=>import('./features/projects/projects.component').then(m=>m.ProjectsComponent)},
    {path:'projects/:id',canActivate:[capabilityGuard],data:{capability:CAPABILITY.VIEW_CATALOG},loadComponent:()=>import('./features/projects/project-detail.component').then(m=>m.ProjectDetailComponent)},
    {path:'logs',canActivate:[capabilityGuard],data:{capability:CAPABILITY.SEARCH_LOGS},loadComponent:()=>import('./features/logs/log-explorer.component').then(m=>m.LogExplorerComponent)},
    {path:'investigations',canActivate:[capabilityGuard],data:{capability:CAPABILITY.RUN_INVESTIGATION},loadComponent:()=>import('./features/investigations/investigation.component').then(m=>m.InvestigationComponent)},
    {path:'traces',canActivate:[capabilityGuard],data:{capability:CAPABILITY.VIEW_TRACES},loadComponent:()=>import('./features/traces/trace-lookup.component').then(m=>m.TraceLookupComponent)},
    {path:'traces/:traceId',canActivate:[capabilityGuard],data:{capability:CAPABILITY.VIEW_TRACES},loadComponent:()=>import('./features/traces/trace-detail.component').then(m=>m.TraceDetailComponent)},
    {path:'administration',canActivate:[capabilityGuard],data:{capability:CAPABILITY.VIEW_ADMINISTRATION},loadComponent:()=>import('./features/administration/administration.component').then(m=>m.AdministrationComponent)},
    {path:'access-denied',loadComponent:()=>import('./features/access-denied/access-denied.component').then(m=>m.AccessDeniedComponent)},
    {path:'',pathMatch:'full',redirectTo:'overview'}
  ]},
  {path:'**',redirectTo:''}
];
