import { ComponentFixture, TestBed } from '@angular/core/testing';

const FORM_SELECTOR = 'form';
const BUTTON_SELECTOR = 'button';
const INPUT_EVENT = 'input';
const CHANGE_EVENT = 'change';
const SUBMIT_EVENT = 'submit';

export function element<T extends HTMLElement>(
  fixture: ComponentFixture<unknown>,
  selector: string,
): T {
  const found = (fixture.nativeElement as HTMLElement).querySelector<T>(selector);
  if (!found) {
    throw new Error(`no element matches ${selector}`);
  }
  return found;
}

export function elements(fixture: ComponentFixture<unknown>, selector: string): HTMLElement[] {
  return Array.from((fixture.nativeElement as HTMLElement).querySelectorAll<HTMLElement>(selector));
}

export function texts(fixture: ComponentFixture<unknown>, selector: string): string[] {
  return elements(fixture, selector).map((found) => found.textContent ?? '');
}

export function attributes(
  fixture: ComponentFixture<unknown>,
  selector: string,
  attribute: string,
): (string | null)[] {
  return elements(fixture, selector).map((found) => found.getAttribute(attribute));
}

export function typeInto(
  fixture: ComponentFixture<unknown>,
  selector: string,
  value: string,
): void {
  const input = element<HTMLInputElement>(fixture, selector);
  input.value = value;
  input.dispatchEvent(new Event(INPUT_EVENT));
  TestBed.tick();
}

export function submitForm(fixture: ComponentFixture<unknown>): void {
  element(fixture, FORM_SELECTOR).dispatchEvent(new Event(SUBMIT_EVENT, { cancelable: true }));
  TestBed.tick();
}

export function buttonLabelled(
  fixture: ComponentFixture<unknown>,
  label: string,
): HTMLButtonElement {
  const found = elements(fixture, BUTTON_SELECTOR).find(
    (button) => button.textContent?.trim() === label,
  );
  if (!found) {
    throw new Error(`no button reads ${label}`);
  }
  return found as HTMLButtonElement;
}

export function selectOption(
  fixture: ComponentFixture<unknown>,
  selector: string,
  value: string,
): void {
  const select = element<HTMLSelectElement>(fixture, selector);
  select.value = value;
  select.dispatchEvent(new Event(CHANGE_EVENT));
  TestBed.tick();
}
