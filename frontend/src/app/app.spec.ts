import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { APP_DISCLAIMER, APP_NAME } from './core/app-info';
import { AppPath } from './core/app-routes';
import { attributes, element, elements } from './testing/dom';

const BRAND = '.brand';
const NAV_LINK = '.top-bar__nav a';
const OUTLET = 'router-outlet';
const HREF = 'href';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('shows the product name and keeps a place for the routed page', () => {
    const fixture = TestBed.createComponent(App);
    TestBed.tick();

    expect(element(fixture, BRAND).textContent).toContain(APP_NAME);
    expect(elements(fixture, OUTLET).length).toBe(1);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(APP_DISCLAIMER);
  });

  it('links to log in and to registration from the header', () => {
    const fixture = TestBed.createComponent(App);
    TestBed.tick();

    expect(attributes(fixture, NAV_LINK, HREF)).toEqual([AppPath.Login, AppPath.Register]);
  });
});
