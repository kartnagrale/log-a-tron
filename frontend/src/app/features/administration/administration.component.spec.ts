import { provideRouter } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { AdministrationComponent } from './administration.component';
import { CatalogApiService } from '../../core/api/catalog-api.service';
import { ServerView } from '../../core/models/api.models';

describe('Administration decommission safeguards', () => {
  const server = { id: 'review-server', hostname: 'review.internal' } as ServerView;
  function setup() {
    const api = { decommissionServer: vi.fn(() => of(undefined)) };
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: CatalogApiService, useValue: api }] });
    const component = TestBed.runInInjectionContext(() => new AdministrationComponent());
    return { component, api };
  }
  it('never sends decommission when the confirmation is dismissed', () => {
    const { component, api } = setup();
    component.decommissionServer(server);
    expect(api.decommissionServer).not.toHaveBeenCalled();
    component.cancelDecommission(); component.confirmDecommission();
    expect(api.decommissionServer).not.toHaveBeenCalled();
  });
  it('decommissions only the explicitly confirmed server', () => {
    const { component, api } = setup();
    component.decommissionServer(server); component.confirmDecommission();
    expect(api.decommissionServer).toHaveBeenCalledExactlyOnceWith(server.id);
    expect(component.pendingDecommission()).toBeNull();
  });
  it('renders and opens the native confirmation dialog before taking action', async () => {
    const api = { projects: () => of([]), companies: () => of([]), adminUsers: () => of([]) };
    await TestBed.configureTestingModule({ imports: [AdministrationComponent], providers: [provideRouter([]), { provide: CatalogApiService, useValue: api }] }).compileComponents();
    const fixture = TestBed.createComponent(AdministrationComponent);
    fixture.detectChanges();
    const dialog = fixture.nativeElement.querySelector('dialog') as HTMLDialogElement;
    expect(dialog).not.toBeNull();
    dialog.showModal = vi.fn();
    fixture.componentInstance.decommissionServer(server);
    expect(dialog.showModal).toHaveBeenCalledOnce();
  });
});
