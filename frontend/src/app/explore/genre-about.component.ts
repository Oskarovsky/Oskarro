import {Component, Input} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';
import {RefreshAfterHttpDirective} from '../services/ui-refresh';

/** Long-form genre text, FAQ and links to the monthly and chart pages, under the genre's track list. */
@Component({
  selector: 'app-genre-about',
  template: `
    <section class="genre-about" *ngIf="info">
      <nav class="genre-links">
        <a [routerLink]="['/nowosci', info.slug]" *ngIf="info.months.length">{{ 'Nowości' | t:'New' }} {{ info.label }} — {{ info.months[0].label }}</a>
        <a [routerLink]="['/ranking', info.slug]">Top 10 {{ info.label }} {{ 'tygodnia' | t:'of the week' }}</a>
      </nav>
      <h2>{{ info.label }} — {{ 'o gatunku' | t:'about the genre' }}</h2>
      <p *ngFor="let paragraph of info.intro">{{ paragraph }}</p>
      <ng-container *ngIf="info.faq.length">
        <h2>{{ 'Najczęstsze pytania' | t:'FAQ' }}</h2>
        <details class="faq" *ngFor="let item of info.faq">
          <summary>{{ item.q }}</summary>
          <p>{{ item.a }}</p>
        </details>
      </ng-container>
    </section>
  `,
  styles: [`
    .genre-about { margin-top: 42px; max-width: 820px; }
    .genre-about p { color: var(--muted); line-height: 1.65; }
    .genre-links { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 22px; }
    .genre-links a {
      padding: 8px 14px; border-radius: 999px; border: 1px solid var(--line);
      color: var(--text); font-weight: 650; font-size: 0.9rem; text-transform: none;
    }
    .genre-links a:hover { border-color: var(--accent); }
    .faq { border-top: 1px solid var(--line); padding: 12px 0; }
    .faq summary { cursor: pointer; font-weight: 650; }
    .faq p { margin: 8px 0 0; }
  `],
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class GenreAboutComponent {
  info: any = null;

  constructor(private http: HttpClient) {}

  @Input()
  set slug(value: string) {
    this.info = null;
    if (!value) {
      return;
    }
    this.http.get<any>(environment.serverUrl + '/explore/genre/' + value).subscribe({
      next: info => this.info = info,
      error: () => this.info = null
    });
  }
}
