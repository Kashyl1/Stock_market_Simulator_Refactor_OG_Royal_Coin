import { Component, computed, inject } from '@angular/core';
import { Theme, ThemeService } from '../core/theme.service';

export const SWITCH_LABELS: Readonly<Record<Theme, string>> = {
  [Theme.Light]: 'Switch to the dark theme',
  [Theme.Dark]: 'Switch to the light theme',
};

@Component({
  selector: 'app-theme-toggle',
  templateUrl: './theme-toggle.html',
})
export class ThemeToggle {
  private readonly themes = inject(ThemeService);

  protected readonly dark = Theme.Dark;
  protected readonly theme = this.themes.theme;
  protected readonly label = computed(() => SWITCH_LABELS[this.theme()]);

  protected toggle(): void {
    this.themes.toggle();
  }
}
