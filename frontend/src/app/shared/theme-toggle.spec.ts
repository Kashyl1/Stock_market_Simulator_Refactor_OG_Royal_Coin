import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { BROWSER_STORAGE } from '../core/browser-storage';
import { PREFERS_DARK } from '../core/prefers-dark';
import { THEME_ATTRIBUTE, Theme, ThemeService } from '../core/theme.service';
import { element } from '../testing/dom';
import { FakeColorScheme } from '../testing/fake-color-scheme';
import { MemoryStorage } from '../testing/memory-storage';
import { SWITCH_LABELS, ThemeToggle } from './theme-toggle';

const BUTTON = 'button';
const ARIA_LABEL = 'aria-label';
const LIGHT_SYSTEM = false;

describe('ThemeToggle', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: BROWSER_STORAGE, useValue: new MemoryStorage() },
        { provide: PREFERS_DARK, useValue: new FakeColorScheme(LIGHT_SYSTEM) },
      ],
    });
  });

  afterEach(() => TestBed.inject(DOCUMENT).documentElement.removeAttribute(THEME_ATTRIBUTE));

  it('switches to the other theme with a single click', () => {
    const fixture = TestBed.createComponent(ThemeToggle);
    TestBed.tick();
    const button = element<HTMLButtonElement>(fixture, BUTTON);

    expect(button.getAttribute(ARIA_LABEL)).toBe(SWITCH_LABELS[Theme.Light]);

    button.click();
    TestBed.tick();

    expect(TestBed.inject(ThemeService).theme()).toBe(Theme.Dark);
    expect(button.getAttribute(ARIA_LABEL)).toBe(SWITCH_LABELS[Theme.Dark]);
  });
});
