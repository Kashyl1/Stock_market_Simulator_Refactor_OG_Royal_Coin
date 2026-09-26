import { HttpErrorResponse } from '@angular/common/http';

const UNEXPECTED_FAILURE_MESSAGE = 'Something went wrong. Please try again in a moment.';
const OFFLINE_FAILURE_MESSAGE = 'The backend is not reachable. Start it and try again.';

const NETWORK_FAILURE_STATUS = 0;

export interface FieldError {
  readonly field: string;
  readonly message: string;
}

export interface ErrorResponse {
  readonly code: string;
  readonly message: string;
  readonly status: number;
  readonly timestamp: string;
  readonly path: string;
  readonly fieldErrors: readonly FieldError[];
}

export interface Failure {
  readonly message: string;
  readonly fieldErrors: Readonly<Record<string, string>>;
}

export function toFailure(error: HttpErrorResponse): Failure {
  const body = error.error;
  if (!isErrorResponse(body)) {
    return {
      message:
        error.status === NETWORK_FAILURE_STATUS
          ? OFFLINE_FAILURE_MESSAGE
          : UNEXPECTED_FAILURE_MESSAGE,
      fieldErrors: {},
    };
  }
  return {
    message: body.message,
    fieldErrors: Object.fromEntries(
      body.fieldErrors.map((fieldError) => [fieldError.field, fieldError.message]),
    ),
  };
}

function isErrorResponse(body: unknown): body is ErrorResponse {
  const candidate = body as Partial<ErrorResponse> | null;
  return (
    typeof candidate?.code === 'string' &&
    typeof candidate?.message === 'string' &&
    Array.isArray(candidate?.fieldErrors)
  );
}
