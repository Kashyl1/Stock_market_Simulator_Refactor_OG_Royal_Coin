import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AppPath } from '../../core/app-routes';
import { attributes, element } from '../../testing/dom';
import { NotFoundPage } from './not-found-page';

const HEADING = 'h1';
const LINK = 'a';
const HREF = 'href';
const MISSING = 'does not exist';

describe('NotFoundPage', () => {
  it('says the page does not exist and links back to the start', () => {
    TestBed.configureTestingModule({ imports: [NotFoundPage], providers: [provideRouter([])] });

    const fixture = TestBed.createComponent(NotFoundPage);
    TestBed.tick();

    expect(element(fixture, HEADING).textContent).toContain(MISSING);
    expect(attributes(fixture, LINK, HREF)).toEqual([AppPath.Home]);
  });
});
