import {Component} from '@angular/core';

const KEY = 'oskarro-cookie-notice';

/** One-time notice: we only store what the site needs to work (no consent required, so no "accept/reject"). */
@Component({
  selector: 'app-cookie-notice',
  standalone: false,
  template: `
    <div class="cookie-notice" *ngIf="visible" role="region" aria-label="Informacja o cookies">
      <p>Zapisujemy w przeglądarce tylko to, co potrzebne do działania strony — bez reklam i śledzenia. <a routerLink="/cookies">Szczegóły</a></p>
      <button class="btn-accent" type="button" (click)="close()">OK</button>
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
