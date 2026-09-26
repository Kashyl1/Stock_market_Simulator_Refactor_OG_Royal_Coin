import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export const PASSWORDS_MISMATCH = 'passwordsMismatch';

export function passwordsMatch(password: string, confirmation: string): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null =>
    group.get(password)?.value === group.get(confirmation)?.value
      ? null
      : { [PASSWORDS_MISMATCH]: true };
}
