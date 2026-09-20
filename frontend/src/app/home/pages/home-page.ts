import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { APP_DISCLAIMER, APP_LEGAL_NOTICE, APP_NAME, APP_TAGLINE } from '../../core/app-info';
import { AppPath } from '../../core/app-routes';
import { BackendService } from '../../core/backend.service';
import { ConnectionStatus } from '../../core/connection-status';

interface Feature {
  readonly title: string;
  readonly description: string;
}

interface JourneyStep {
  readonly title: string;
  readonly description: string;
}

const FEATURES: readonly Feature[] = [
  {
    title: 'A wallet of play money',
    description:
      'Every account gets its own wallet. Cash moves only through recorded entries, so the balance always adds up.',
  },
  {
    title: 'Portfolios with full P&L',
    description:
      'Hold crypto, stocks and ETFs side by side, and watch cost basis, realised and unrealised profit per portfolio.',
  },
  {
    title: 'Income that keeps coming',
    description:
      'Dividends, staking rewards and interest are posted on a schedule, modelled on how they would land in a real account.',
  },
];

const STATUS_LABELS: Readonly<Record<ConnectionStatus, string>> = {
  [ConnectionStatus.Checking]: 'Checking',
  [ConnectionStatus.Online]: 'Online',
  [ConnectionStatus.Offline]: 'Offline',
};

const JOURNEY: readonly JourneyStep[] = [
  {
    title: 'Create an account',
    description: 'E-mail, a display name and a password of at least ten characters.',
  },
  {
    title: 'Verify your e-mail',
    description: 'We send a one-time link; the account stays pending until you use it.',
  },
  {
    title: 'Get your wallet',
    description: 'An empty wallet is created with the account, ready for its play money.',
  },
  {
    title: 'Trade and track',
    description: 'Buy, sell, and follow every portfolio as prices move.',
  },
];

@Component({
  selector: 'app-home-page',
  imports: [RouterLink],
  templateUrl: './home-page.html',
  styleUrl: './home-page.scss',
})
export class HomePage {
  private readonly backend = inject(BackendService);

  protected readonly appName = APP_NAME;
  protected readonly tagline = APP_TAGLINE;
  protected readonly disclaimer = APP_DISCLAIMER;
  protected readonly legalNotice = APP_LEGAL_NOTICE;
  protected readonly paths = AppPath;
  protected readonly features = FEATURES;
  protected readonly journey = JOURNEY;

  protected readonly health = this.backend.health();

  protected readonly status = computed<ConnectionStatus>(() => {
    switch (this.health.status()) {
      case 'resolved':
      case 'local':
        return ConnectionStatus.Online;
      case 'error':
        return ConnectionStatus.Offline;
      default:
        return ConnectionStatus.Checking;
    }
  });

  protected readonly errorMessage = computed(() => this.health.error()?.message ?? null);

  protected readonly statusLabel = computed(() => STATUS_LABELS[this.status()]);

  protected readonly statusDetail = computed(() => {
    switch (this.status()) {
      case ConnectionStatus.Online:
        return this.health.value()?.service ?? null;
      case ConnectionStatus.Offline:
        return this.errorMessage();
      default:
        return null;
    }
  });

  protected readonly offline = computed(() => this.status() === ConnectionStatus.Offline);

  protected check(): void {
    this.health.reload();
  }
}
