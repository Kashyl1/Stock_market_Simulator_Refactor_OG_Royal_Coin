import { DOCUMENT } from '@angular/common';
import { InjectionToken, inject } from '@angular/core';

export const BROWSER_STORAGE = new InjectionToken<Storage | null>('browser storage', {
  providedIn: 'root',
  factory: () => {
    try {
      return inject(DOCUMENT).defaultView?.localStorage ?? null;
    } catch {
      return null;
    }
  },
});
