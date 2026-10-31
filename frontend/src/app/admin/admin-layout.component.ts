import {Component, ViewEncapsulation} from '@angular/core';
import {TokenStorageService} from '../services/auth/token-storage.service';

@Component({
  selector: 'app-admin-layout',
  standalone: false,
  encapsulation: ViewEncapsulation.None,
  styleUrls: ['./admin.css'],
  template: `
    <div class="adm">
      <aside class="adm-side">
        <div class="adm-brand">
          <span class="adm-badge">Admin</span>
          <strong>VIXA</strong>
          <span class="adm-muted">{{ me }}</span>
        </div>
        <nav>
          <a routerLink="/admin" [routerLinkActiveOptions]="{exact: true}" routerLinkActive="on"><span>◧</span>Pulpit</a>
          <a routerLink="/admin/uzytkownicy" routerLinkActive="on"><span>◉</span>Użytkownicy</a>
          <a routerLink="/admin/utwory" routerLinkActive="on"><span>♫</span>Utwory</a>
          <a routerLink="/admin/komentarze" routerLinkActive="on"><span>✎</span>Komentarze</a>
          <a routerLink="/admin/zgloszenia" routerLinkActive="on"><span>⚑</span>Zgłoszenia</a>
          <a routerLink="/admin/aktywnosc" routerLinkActive="on"><span>↯</span>Aktywność</a>
          <a routerLink="/admin/dziennik" routerLinkActive="on"><span>☰</span>Dziennik zmian</a>
        </nav>
        <div class="adm-side-foot">
          <a href="/status/" target="_blank" rel="noopener">Status serwera ↗</a>
          <a routerLink="/">← Wróć na stronę</a>
        </div>
      </aside>
      <main class="adm-main">
        <router-outlet></router-outlet>
      </main>
    </div>
  `
})
export class AdminLayoutComponent {
  readonly me: string;

  constructor(tokens: TokenStorageService) {
    this.me = tokens.getUser()?.username || '';
  }
}
