import {Component} from '@angular/core';

// New key, so people who closed the older cookies-only notice see this one once.
const KEY = 'oskarro-privacy-notice-2';

/** One-time privacy (RODO) and cookies notice: we only store what the site needs, so it informs rather than asks for consent. */
@Component({
  selector: 'app-cookie-notice',
  standalone: false,
  template: `
    <div class="cookie-notice" *ngIf="visible" role="region" [attr.aria-label]="'Prywatność i cookies' | t:'Privacy and cookies'">
      <p>
        <strong>{{ 'Twoja prywatność (RODO).' | t:'Your privacy (GDPR).' }}</strong>
        {{ 'Zapisujemy w przeglądarce tylko to, co potrzebne do działania strony i anonimowego liczenia odwiedzin — bez reklam i śledzenia. Dane konta przetwarzamy wyłącznie, żeby serwis działał.' | t:'We only store what the site needs to work and to count visits anonymously — no ads and no tracking. Account data is used only to run the service.' }}
        <a routerLink="/polityka-prywatnosci">{{ 'Polityka prywatności' | t:'Privacy policy' }}</a> ·
        <a routerLink="/cookies">{{ 'Cookies' | t:'Cookies' }}</a>
      </p>
      <button class="btn-accent" type="button" (click)="close()">{{ 'Rozumiem' | t:'Got it' }}</button>
    </div>
  `
})
export class CookieNoticeComponent {
  visible = false;

  constructor() {
    try {
      this.visible = localStorage.getItem(KEY) !== '1';
    } catch {
      this.visible = false;
    }
  }

  close() {
    this.visible = false;
    try {
      localStorage.setItem(KEY, '1');
    } catch {
      // private mode: the notice simply shows again next time
    }
  }
}
