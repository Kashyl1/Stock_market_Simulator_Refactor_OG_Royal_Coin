import { DOCUMENT } from '@angular/common';
import { Injectable, computed, inject, signal } from '@angular/core';
import { BROWSER_STORAGE } from './browser-storage';
import { PREFERS_DARK, SCHEME_CHANGE } from './prefers-dark';

export const Theme = {
  Light: 'light',
  Dark: 'dark',
} as const;

export type Theme = (typeof Theme)[keyof typeof Theme];

export const THEME_STORAGE_KEY = 'theme';
export const THEME_ATTRIBUTE = 'data-theme';

const THEMES: readonly Theme[] = [Theme.Light, Theme.Dark];

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly storage = inject(BROWSER_STORAGE);
  private readonly prefersDark = inject(PREFERS_DARK);
  private readonly chosen = signal<Theme | null>(this.remembered());
  private readonly systemDark = signal(this.prefersDark?.matches ?? false);

  readonly theme = computed<Theme>(
    () => this.chosen() ?? (this.systemDark() ? Theme.Dark : Theme.Light),
  );

  constructor() {
    this.prefersDark?.addEventListener(SCHEME_CHANGE, (event) =>
      this.systemDark.set(event.matches),
    );
    this.apply(this.chosen());
  }

  toggle(): void {
    const next = this.theme() === Theme.Dark ? Theme.Light : Theme.Dark;
    this.chosen.set(next);
    this.apply(next);
    this.remember(next);
  }

  private apply(theme: Theme | null): void {
    const root = this.document.documentElement;
    if (theme === null) {
      root.removeAttribute(THEME_ATTRIBUTE);
      return;
    }
    root.setAttribute(THEME_ATTRIBUTE, theme);
  }

  private remembered(): Theme | null {
    try {
      const stored = this.storage?.getItem(THEME_STORAGE_KEY);
      return THEMES.find((theme) => theme === stored) ?? null;
    } catch {
      return null;
    }
  }

  private remember(theme: Theme): void {
    try {
      this.storage?.setItem(THEME_STORAGE_KEY, theme);
    } catch {
      return;
    }
  }
}
