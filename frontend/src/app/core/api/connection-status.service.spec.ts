import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ConnectionStatusService, connectionStatusInterceptor } from './connection-status.service';

describe('API connection indicator', () => {
  it('starts unknown, reflects outages, and treats denied access as an API response', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(withInterceptors([connectionStatusInterceptor])), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpClient), requests = TestBed.inject(HttpTestingController), connection = TestBed.inject(ConnectionStatusService);
    expect(connection.state()).toBe('unknown');
    http.get('/api/v1/projects').subscribe({ error: () => {} });
    requests.expectOne('/api/v1/projects').flush({}, { status: 503, statusText: 'Unavailable' });
    expect(connection.state()).toBe('unavailable');
    http.get('/api/v1/projects').subscribe({ error: () => {} });
    requests.expectOne('/api/v1/projects').flush({}, { status: 403, statusText: 'Forbidden' });
    expect(connection.state()).toBe('connected');
    requests.verify();
  });
});
