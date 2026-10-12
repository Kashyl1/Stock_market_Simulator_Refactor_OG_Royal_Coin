import { DOCUMENT } from '@angular/common';
import { InjectionToken, inject } from '@angular/core';

export const DARK_SCHEME_QUERY = '(prefers-color-scheme: dark)';
export const SCHEME_CHANGE = 'change';

export interface ColorSchemeQuery {
  readonly matches: boolean;
  addEventListener(
    type: typeof SCHEME_CHANGE,
    listener: (event: { readonly matches: boolean }) => void,
  ): void;
}

export const PREFERS_DARK = new InjectionToken<ColorSchemeQuery | null>('prefers dark', {
  providedIn: 'root',
  factory: () => inject(DOCUMENT).defaultView?.matchMedia?.(DARK_SCHEME_QUERY) ?? null,
});
