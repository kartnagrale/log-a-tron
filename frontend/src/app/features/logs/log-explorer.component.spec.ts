import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { LogExplorerComponent } from './log-explorer.component';
import { LogQueryApiService } from '../../core/api/log-query-api.service';

describe('Log Explorer operational states', () => {
  function setup(api: object, query = {}) {
    TestBed.configureTestingModule({ providers: [
      { provide: LogQueryApiService, useValue: api },
      { provide: Router, useValue: { navigate: vi.fn() } },
      { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap(query) } } }
    ] });
    return TestBed.runInInjectionContext(() => new LogExplorerComponent());
  }
  it('stops showing an active stream when its bounded response ends', async () => {
    const component = setup({ live: () => Promise.resolve() });
    component.toggleLive();
    expect(component.liveStatus()).toBe('Connecting…');
    await Promise.resolve();
    expect(component.live()).toBe(false);
    expect(component.liveStatus()).toContain('reconnect');
  });
  it('does not let an aborted stream overwrite the next connection', async () => {
    let rejectOld!: (error: Error) => void;
    const api = { live: vi.fn().mockImplementationOnce(() => new Promise((_, reject) => rejectOld = reject)).mockImplementation(() => new Promise(() => {})) };
    const component = setup(api);
    component.toggleLive(); component.pauseLive(); component.toggleLive();
    rejectOld(new DOMException('Aborted', 'AbortError'));
    await Promise.resolve(); await Promise.resolve();
    expect(component.live()).toBe(true);
    expect(component.liveStatus()).toBe('Connecting…');
    component.ngOnDestroy();
  });
  it('preserves a server-reported duration limit after the stream closes', async () => {
    const component = setup({ live: (_criteria: unknown, _abort: unknown, _log: unknown, status: (value: string) => void) => { status('DURATION_LIMIT'); return Promise.resolve(); } });
    component.toggleLive(); await Promise.resolve();
    expect(component.live()).toBe(false);
    expect(component.liveStatus()).toBe('DURATION_LIMIT');
  });
  it('distinguishes the anchor and both sides without changing backend results', () => {
    const component = setup({ context: () => of({ anchor: { eventId: 'anchor' }, before: [{ eventId: 'before' }], after: [{ eventId: 'after' }] }) });
    component.viewContext('anchor');
    expect(component.rows().map(row => row.eventId)).toEqual(['after', 'anchor', 'before']);
    expect(component.contextLabel('anchor')).toBe('ANCHOR EVENT');
    expect(component.contextLabel('before')).toBe('BEFORE ANCHOR');
    expect(component.contextLabel('after')).toBe('AFTER ANCHOR');
  });
  it('preserves span ID when navigating from a selected trace span', async () => {
    const search = vi.fn(() => of({ items: [], nextCursor: null }));
    const component = setup({ saved: () => of([]), search }, { traceId: 'a'.repeat(32), spanId: 'b'.repeat(16) });
    component.ngOnInit(); await Promise.resolve();
    expect(search).toHaveBeenCalledWith(expect.objectContaining({ traceId: 'a'.repeat(32), spanId: 'b'.repeat(16) }));
  });
  it('opens a backend-provided event evidence link through the detail API', () => {
    const detail = vi.fn(() => of({ eventId: 'evidence-event' }));
    const component = setup({ saved: () => of([]), detail }, { eventId: 'evidence-event' });
    component.ngOnInit();
    expect(detail).toHaveBeenCalledWith('evidence-event');
    expect(component.selected()?.eventId).toBe('evidence-event');
  });
  it('restores the saved time window along with the visible scope and filters', () => {
    const search = vi.fn(() => of({ items: [], nextCursor: null }));
    const component = setup({ search });
    component.savedSearches.set([{ id: 'saved', ownerUserId: 'u', projectId: 'p', name: 'Errors', visibility: 'OWNER', createdAt: '', updatedAt: '', criteria: { projectId: 'p', level: 'ERROR', from: '2026-10-25T00:00:00Z', to: '2026-10-25T01:00:00Z' } }]);
    component.selectedSaved = 'saved'; component.applySaved();
    expect(component.rangeMinutes).toBe(60);
    expect(component.restoredScope()?.projectId).toBe('p');
    expect(search).toHaveBeenCalledWith(expect.objectContaining({ projectId: 'p', level: 'ERROR' }));
  });
});
