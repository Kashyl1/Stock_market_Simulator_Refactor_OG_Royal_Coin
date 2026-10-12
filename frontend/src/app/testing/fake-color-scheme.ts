import { ColorSchemeQuery, SCHEME_CHANGE } from '../core/prefers-dark';

type SchemeListener = (event: { readonly matches: boolean }) => void;

export class FakeColorScheme implements ColorSchemeQuery {
  private listener: SchemeListener | null = null;

  constructor(public matches: boolean) {}

  addEventListener(_type: typeof SCHEME_CHANGE, listener: SchemeListener): void {
    this.listener = listener;
  }

  change(matches: boolean): void {
    this.matches = matches;
    this.listener?.({ matches });
  }
}
