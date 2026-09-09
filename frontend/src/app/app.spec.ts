import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);

    TestBed.inject(HttpTestingController).expectOne('/api/health').flush({
      status: 'UP',
      service: 'trading-simulator-backend',
      timestamp: new Date().toISOString(),
    });
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render the project heading', async () => {
    const fixture = TestBed.createComponent(App);
    TestBed.inject(HttpTestingController).expectOne('/api/health').flush({
      status: 'UP',
      service: 'trading-simulator-backend',
      timestamp: new Date().toISOString(),
    });
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('trading_simulator_refactor');
  });
});
