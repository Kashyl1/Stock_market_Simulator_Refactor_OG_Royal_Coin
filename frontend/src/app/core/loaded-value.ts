import { HttpResourceRef } from '@angular/common/http';
import { Signal, computed } from '@angular/core';

export function loadedValue<T>(resource: HttpResourceRef<T | undefined>): Signal<T | undefined> {
  return computed(() => (resource.hasValue() ? resource.value() : undefined));
}
