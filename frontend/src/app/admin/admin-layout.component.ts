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
        <div class="adm-tools">
          <p class="adm-tools-title">Narzędzia</p>
          <a *ngFor="let tool of tools" [href]="tool.url" target="_blank" rel="noopener" [title]="tool.note">
            <span>{{ tool.icon }}</span>{{ tool.label }} <em>↗</em>
          </a>
        </div>
        <div class="adm-side-foot">
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

  /** Outside services an admin needs; logins for Grafana and Status are in the secrets vault (produkcja.env). */
  readonly tools = [
    {icon: '📈', label: 'Grafana', url: '/grafana/', note: 'Wykresy ruchu, błędów i obciążenia serwera'},
    {icon: '🩺', label: 'Status serwera', url: '/status/', note: 'Czy strona, API i baza działają'},
    {icon: '🔎', label: 'Google Search Console', url: 'https://search.google.com/search-console', note: 'Jak VIXA wypada w wyszukiwarce Google'},
    {icon: '🔑', label: 'Logowanie Google', url: 'https://console.cloud.google.com/auth/clients?project=oskarro', note: 'Klucze logowania przez Google'},
    {icon: '📘', label: 'Aplikacja Facebook', url: 'https://developers.facebook.com/apps/876955753919962/', note: 'Logowanie przez Facebooka (Oskarro Website)'},
    {icon: '▶', label: 'Kanał YouTube', url: 'https://studio.youtube.com/', note: 'YouTube Studio — sety wideo'},
    {icon: '⌥', label: 'Kod na GitHubie', url: 'https://github.com/Oskarovsky/Oskarro', note: 'Repozytorium, pull requesty i testy'}
  ];

  constructor(tokens: TokenStorageService) {
    this.me = tokens.getUser()?.username || '';
  }
}
