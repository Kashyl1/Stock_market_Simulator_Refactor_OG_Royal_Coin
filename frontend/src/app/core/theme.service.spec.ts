import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { FakeColorScheme } from '../testing/fake-color-scheme';
import { MemoryStorage, RefusingStorage } from '../testing/memory-storage';
import { BROWSER_STORAGE } from './browser-storage';
import { PREFERS_DARK } from './prefers-dark';
import { THEME_ATTRIBUTE, THEME_STORAGE_KEY, Theme, ThemeService } from './theme.service';

const LIGHT_SYSTEM = false;
const DARK_SYSTEM = true;

describe('ThemeService', () => {
  let root: HTMLElement;
  let storage: Storage;
  let system: FakeColorScheme;

  function themeWith(
    systemDark: boolean,
    browserStorage: Storage = new MemoryStorage(),
  ): ThemeService {
    storage = browserStorage;
    system = new FakeColorScheme(systemDark);
    TestBed.configureTestingModule({
      providers: [
        { provide: BROWSER_STORAGE, useValue: browserStorage },
        { provide: PREFERS_DARK, useValue: system },
      ],
    });
    root = TestBed.inject(DOCUMENT).documentElement;
    return TestBed.inject(ThemeService);
  }

  afterEach(() => root.removeAttribute(THEME_ATTRIBUTE));

  it('follows the system until a theme is chosen', () => {
    const theme = themeWith(DARK_SYSTEM);

    expect(theme.theme()).toBe(Theme.Dark);
    expect(root.hasAttribute(THEME_ATTRIBUTE)).toBe(false);
  });

  it('turns a light system dark with one click and remembers it', () => {
    const theme = themeWith(LIGHT_SYSTEM);

    theme.toggle();

    expect(theme.theme()).toBe(Theme.Dark);
    expect(root.getAttribute(THEME_ATTRIBUTE)).toBe(Theme.Dark);
    expect(storage.getItem(THEME_STORAGE_KEY)).toBe(Theme.Dark);
  });

  it('turns a dark system light with one click and back with the next', () => {
    const theme = themeWith(DARK_SYSTEM);

    theme.toggle();
    expect(theme.theme()).toBe(Theme.Light);

    theme.toggle();
    expect(theme.theme()).toBe(Theme.Dark);
    expect(storage.getItem(THEME_STORAGE_KEY)).toBe(Theme.Dark);
  });

  it('keeps a remembered theme over the system', () => {
    const remembered = new MemoryStorage();
    remembered.setItem(THEME_STORAGE_KEY, Theme.Light);

    const theme = themeWith(DARK_SYSTEM, remembered);

    expect(theme.theme()).toBe(Theme.Light);
    expect(root.getAttribute(THEME_ATTRIBUTE)).toBe(Theme.Light);
  });

  it('follows the system when it changes and nothing is chosen', () => {
    const theme = themeWith(LIGHT_SYSTEM);

    system.change(DARK_SYSTEM);

    expect(theme.theme()).toBe(Theme.Dark);
  });

  it('still switches when the browser refuses to store anything', () => {
    const theme = themeWith(LIGHT_SYSTEM, new RefusingStorage());

    theme.toggle();

    expect(root.getAttribute(THEME_ATTRIBUTE)).toBe(Theme.Dark);
  });
});
