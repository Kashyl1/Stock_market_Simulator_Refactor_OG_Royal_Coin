import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AppPath } from '../../core/app-routes';

@Component({
  selector: 'app-not-found-page',
  imports: [RouterLink],
  templateUrl: './not-found-page.html',
  styleUrl: './not-found-page.scss',
})
export class NotFoundPage {
  protected readonly paths = AppPath;
}
