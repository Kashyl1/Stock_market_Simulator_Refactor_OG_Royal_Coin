import { Component, computed, input, output } from '@angular/core';

const ToggleLabel = {
  Show: 'Show password',
  Hide: 'Hide password',
} as const;

@Component({
  selector: 'app-password-visibility-toggle',
  templateUrl: './password-visibility-toggle.html',
  styleUrl: './password-visibility-toggle.scss',
})
export class PasswordVisibilityToggle {
  readonly visible = input.required<boolean>();
  readonly toggled = output<void>();

  protected readonly label = computed(() => (this.visible() ? ToggleLabel.Hide : ToggleLabel.Show));
}
