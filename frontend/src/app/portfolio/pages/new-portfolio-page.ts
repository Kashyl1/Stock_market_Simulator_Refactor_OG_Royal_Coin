import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AppPath, portfolioPath } from '../../core/app-routes';
import { Failure, toFailure } from '../../core/error-response';
import { loadedValue } from '../../core/loaded-value';
import { DEFAULT_PORTFOLIO_CURRENCY, PORTFOLIO_NAME_MAX_LENGTH } from '../portfolio-models';
import { PortfolioService } from '../portfolio.service';

@Component({
  selector: 'app-new-portfolio-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './new-portfolio-page.html',
})
export class NewPortfolioPage {
  private readonly portfolios = inject(PortfolioService);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly paths = AppPath;
  protected readonly nameMaxLength = PORTFOLIO_NAME_MAX_LENGTH;

  protected readonly currencies = this.portfolios.currencies();
  protected readonly currencyList = loadedValue(this.currencies);

  protected readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(PORTFOLIO_NAME_MAX_LENGTH)]],
    currency: [DEFAULT_PORTFOLIO_CURRENCY, Validators.required],
  });

  protected readonly submitting = signal(false);
  protected readonly failure = signal<Failure | null>(null);

  protected create(): void {
    if (this.submitting()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { name, currency } = this.form.getRawValue();
    this.submitting.set(true);
    this.failure.set(null);
    this.portfolios
      .create({ name, currency })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (created) => this.router.navigateByUrl(portfolioPath(created.id)),
        error: (error: HttpErrorResponse) => {
          this.failure.set(toFailure(error));
          this.submitting.set(false);
        },
      });
  }
}
