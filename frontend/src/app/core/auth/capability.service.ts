import { computed, inject, Injectable } from '@angular/core';
import { MeResponse } from '../models/api.models';
import { AuthService } from './auth.service';

export const CAPABILITY = {
  VIEW_OVERVIEW: 'VIEW_OVERVIEW',
  SEARCH_LOGS: 'SEARCH_LOGS',
  VIEW_TRACES: 'VIEW_TRACES',
  RUN_INVESTIGATION: 'RUN_INVESTIGATION',
  VIEW_SAVED_SEARCHES: 'VIEW_SAVED_SEARCHES',
  MANAGE_SAVED_SEARCHES: 'MANAGE_SAVED_SEARCHES',
  VIEW_CATALOG: 'VIEW_CATALOG',
  MANAGE_CATALOG: 'MANAGE_CATALOG',
  VIEW_ADMINISTRATION: 'VIEW_ADMINISTRATION',
  VIEW_AUDIT: 'VIEW_AUDIT',
  ACCESS_PRODUCTION: 'ACCESS_PRODUCTION'
} as const;

export type Capability = typeof CAPABILITY[keyof typeof CAPABILITY];
export type PlatformRole = 'ADMIN'|'PROJECT_ADMIN'|'PRODUCTION_SUPPORT'|'DEVELOPER'|'VIEWER'|'AUDITOR';

const scopedCapabilities: Capability[] = [
  CAPABILITY.VIEW_OVERVIEW,
  CAPABILITY.SEARCH_LOGS,
  CAPABILITY.VIEW_TRACES,
  CAPABILITY.RUN_INVESTIGATION,
  CAPABILITY.VIEW_SAVED_SEARCHES,
  CAPABILITY.MANAGE_SAVED_SEARCHES,
  CAPABILITY.VIEW_CATALOG
];

export function resolveCapabilities(me: MeResponse | null): ReadonlySet<Capability> {
  const capabilities = new Set<Capability>();
  if (!me?.projects.length) return capabilities;
  scopedCapabilities.forEach(capability => capabilities.add(capability));
  if (me.roles.some(role => role === 'ADMIN' || role === 'PROJECT_ADMIN')) {
    capabilities.add(CAPABILITY.MANAGE_CATALOG);
    capabilities.add(CAPABILITY.VIEW_ADMINISTRATION);
  }
  if (me.projects.some(project => project.environments.some(environment => environment.code === 'PROD'))) {
    capabilities.add(CAPABILITY.ACCESS_PRODUCTION);
  }
  // No audit-query endpoint exists in the current backend, so VIEW_AUDIT is intentionally unresolved.
  return capabilities;
}

export function roleLabel(role: string): string {
  return ({ADMIN:'Administrator',PROJECT_ADMIN:'Project administrator',PRODUCTION_SUPPORT:'Production support',DEVELOPER:'Developer',VIEWER:'Viewer',AUDITOR:'Auditor'} as Record<string,string>)[role] ?? role.replaceAll('_',' ').toLowerCase();
}

@Injectable({providedIn:'root'})
export class CapabilityService {
  private readonly auth = inject(AuthService);
  readonly all = computed(() => resolveCapabilities(this.auth.me()));
  readonly primaryRole = computed(() => this.auth.me()?.roles[0] ?? 'UNASSIGNED');
  has(capability: Capability) { return this.all().has(capability); }
  canManageProject(projectId: string | null | undefined) {
    const me = this.auth.me();
    if (!me || !projectId) return false;
    if (me.roles.includes('ADMIN')) return true;
    return me.projects.some(project => project.projectId === projectId && project.roles.includes('PROJECT_ADMIN'));
  }
}
