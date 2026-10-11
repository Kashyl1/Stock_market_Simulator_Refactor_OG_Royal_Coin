import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AccountView } from '../account/account-models';
import { toFailure } from '../core/error-response';
import { loadedValue } from '../core/loaded-value';
import { MoneyPipe } from '../money/money.pipe';
import {
  DIRECTION_LABELS,
  PortfolioView,
  TransferDirection,
  TransferView,
} from './portfolio-models';
import { PortfolioService } from './portfolio.service';

export const RATE_FORMAT = '1.2-6';
export const SMALLEST_AMOUNT = 0.01;

const DIRECTIONS: readonly TransferDirection[] = [TransferDirection.In, TransferDirection.Out];

@Component({
  selector: 'app-transfer-form',
  imports: [DecimalPipe, ReactiveFormsModule, MoneyPipe],
  templateUrl: './transfer-form.html',
  styleUrl: './transfer-form.scss',
})
export class TransferForm {
  readonly portfolio = input.required<PortfolioView>();
  readonly mainAccount = input<AccountView>();
  readonly transferred = output<TransferView>();

  private readonly portfolios = inject(PortfolioService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly directions = DIRECTIONS;
  protected readonly directionLabels = DIRECTION_LABELS;
  protected readonly rateFormat = RATE_FORMAT;
  protected readonly inward = TransferDirection.In;

  protected readonly direction = signal<TransferDirection>(TransferDirection.In);
  protected readonly submitting = signal(false);
  protected readonly failure = signal<string | null>(null);
  protected readonly done = signal<TransferView | null>(null);

  protected readonly form = this.formBuilder.group({
    amount: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(SMALLEST_AMOUNT),
    ]),
  });
  protected readonly amountControl = this.form.controls.amount;
  private readonly amount = toSignal(this.amountControl.valueChanges, { initialValue: null });

  private readonly isIn = computed(() => this.direction() === TransferDirection.In);
  protected readonly mainCurrency = computed(() => this.portfolio().performance.currency);
  protected readonly enteredCurrency = computed(() =>
    this.isIn() ? this.mainCurrency() : this.portfolio().currency,
  );
  protected readonly available = computed(() =>
    this.isIn() ? this.mainAccount()?.available : this.portfolio().available,
  );

  protected readonly quote = this.portfolios.quote(() => {
    const amount = this.amount();
    if (amount === null || amount <= 0) {
      return undefined;
    }
    return { portfolioId: this.portfolio().id, direction: this.direction(), amount };
  });
  protected readonly preview = loadedValue(this.quote);
  protected readonly arriving = computed(() => {
    const preview = this.preview();
    if (preview === undefined) {
      return undefined;
    }
    return this.isIn()
      ? { amount: preview.amount, currency: preview.currency }
      : { amount: preview.mainAmount, currency: preview.mainCurrency };
  });
  protected readonly quoteFailure = computed(() => {
    const error = this.quote.error();
    return error instanceof HttpErrorResponse ? toFailure(error).message : null;
  });

  protected choose(direction: TransferDirection): void {
    this.direction.set(direction);
    this.failure.set(null);
    this.done.set(null);
  }

  protected submit(): void {
    const amount = this.amountControl.value;
    if (this.submitting()) {
      return;
    }
    if (this.amountControl.invalid || amount === null) {
      this.amountControl.markAsTouched();
      return;
    }
    this.submitting.set(true);
    this.failure.set(null);
    this.done.set(null);
    this.portfolios
      .transfer(this.portfolio().id, { direction: this.direction(), amount })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (transfer) => {
          this.done.set(transfer);
          this.amountControl.reset();
          this.submitting.set(false);
          this.transferred.emit(transfer);
        },
        error: (error: HttpErrorResponse) => {
          this.failure.set(toFailure(error).message);
          this.submitting.set(false);
        },
      });
  }
}
