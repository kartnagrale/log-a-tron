import { TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { ScopePickerComponent } from './scope-picker.component';
import { CatalogApiService } from '../../../core/api/catalog-api.service';

describe('Authorized scope restoration', () => {
  function setup(overrides: object = {}) {
    const api = {
      projects: () => of([{ id: 'p1', name: 'First' }, { id: 'p2', name: 'Second' }]),
      environments: () => of([{ id: 'e', code: 'DEV' }]),
      servers: () => of([{ id: 'server', hostname: 'node' }]),
      instances: () => of([{ id: 'i1', serviceId: 's1', serviceName: 'Orders' }, { id: 'i2', serviceId: 's2', serviceName: 'Billing' }]),
      logSources: () => of([{ id: 'source', pathPattern: '/logs/app.log' }]),
      ...overrides
    };
    TestBed.configureTestingModule({ providers: [{ provide: CatalogApiService, useValue: api }] });
    return TestBed.runInInjectionContext(() => new ScopePickerComponent());
  }
  it('restores all saved levels from backend-listed options', () => {
    const component = setup();
    component.selection = { projectId: 'p1', environmentId: 'e', serverId: 'server', serviceId: 's1', serviceInstanceId: 'i1', logSourceId: 'source' };
    component.ngOnInit();
    expect(component.projectId).toBe('p1');
    expect(component.serviceInstanceId).toBe('i1');
    expect(component.logSourceId).toBe('source');
  });
  it('does not expose a requested project absent from backend options', () => {
    const component = setup();
    component.selection = { projectId: 'unauthorized' };
    component.ngOnInit();
    expect(component.projectId).toBeNull();
    expect(component.servers()).toEqual([]);
  });
  it('updates instance options immediately when the service changes', () => {
    const component = setup();
    component.instances.set([{ id: 'i1', serviceId: 's1' }, { id: 'i2', serviceId: 's2' }] as never);
    component.serviceId = 's1';
    expect(component.filteredInstances().map(value => value.id)).toEqual(['i1']);
    component.serviceId = 's2'; component.serviceChanged();
    expect(component.filteredInstances().map(value => value.id)).toEqual(['i2']);
    expect(component.serviceInstanceId).toBeNull();
  });
  it('ignores a delayed response from a project that is no longer selected', () => {
    const pending = new Subject<unknown[]>();
    const component = setup({ environments: (projectId: string) => projectId === 'p1' ? pending : of([{ id: 'new-env', code: 'QA' }]) });
    component.selection = { projectId: 'p1' }; component.ngOnInit();
    component.projectId = 'p2'; component.projectChanged();
    pending.next([{ id: 'old-env', code: 'DEV' }]);
    expect(component.projectId).toBe('p2');
    expect(component.environments().map(environment => environment.id)).toEqual(['new-env']);
  });
});
