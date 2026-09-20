import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { APP_DISCLAIMER, APP_NAME } from './core/app-info';
import { AppPath } from './core/app-routes';

@Component({
  selector: 'app-root',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly appName = APP_NAME;
  protected readonly disclaimer = APP_DISCLAIMER;
  protected readonly paths = AppPath;
  protected readonly currentYear = new Date().getFullYear();
}
