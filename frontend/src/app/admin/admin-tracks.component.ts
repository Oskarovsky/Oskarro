import {Component, OnInit} from '@angular/core';
import {AdminService, Page, TrackRow} from './admin.service';
import {RefreshAfterHttpDirective} from '../services/ui-refresh';
import {GENRES} from '../tracks/genres';

@Component({
  selector: 'app-admin-tracks',
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective],
  template: `
    <header class="adm-head">
      <div>
        <p class="kicker">Panel administratora</p>
        <h1>Utwory</h1>
      </div>
      <span class="adm-muted" *ngIf="result">{{ result.total }} utworów</span>
    </header>

    <div class="adm-toolbar">
      <input type="search" placeholder="Tytuł, wykonawca albo kto dodał…" [(ngModel)]="q" (keyup.enter)="load(0)" (search)="load(0)" aria-label="Szukaj utworu">
      <select [(ngModel)]="genre" (change)="load(0)" aria-label="Gatunek">
        <option value="">Wszystkie gatunki</option>
        <option *ngFor="let g of genres" [value]="g.id">{{ g.label }}</option>
      </select>
      <button class="adm-btn" type="button" (click)="load(0)">Szukaj</button>
    </div>

    <p class="adm-error" *ngIf="error" role="alert">{{ error }}</p>
    <p class="adm-saved" *ngIf="saved" role="status">{{ saved }}</p>

    <div class="adm-table-wrap" *ngIf="result">
      <table class="adm-table">
        <thead>
          <tr><th>Utwór</th><th>Gatunek</th><th>Dodał</th><th>Dodano</th><th class="adm-num">Ocena</th><th class="adm-num">Odsł.</th><th class="adm-num">Kom.</th><th></th></tr>
        </thead>
        <tbody>
          <ng-container *ngFor="let t of result.items">
            <tr>
              <td><a [routerLink]="['/track', t.id]" target="_blank"><strong>{{ t.title }}</strong></a><span *ngIf="t.version" class="adm-muted"> ({{ t.version }})</span><br><span class="adm-muted">{{ t.artist }}</span></td>
              <td><span class="adm-pill">{{ label(t.genre) }}</span></td>
              <td><a *ngIf="t.author" [routerLink]="['/profile', t.author]">{{ t.author }}</a><span *ngIf="!t.author" class="adm-muted">—</span></td>
              <td class="adm-nowrap adm-muted" [attr.title]="t.createdAt">{{ t.createdAt | ago }}</td>
              <td class="adm-num">{{ t.score > 0 ? '+' : '' }}{{ t.score }}</td>
              <td class="adm-num">{{ t.views }}</td>
              <td class="adm-num">{{ t.comments }}</td>
              <td class="adm-nowrap">
                <button class="adm-btn" type="button" (click)="edit(t)">Edytuj</button>
                <button class="adm-btn danger" type="button" (click)="remove(t)" style="margin-left: 4px;">Usuń</button>
              </td>
            </tr>
            <tr *ngIf="editing?.id === t.id">
              <td colspan="8" style="background: var(--bg-2);">
                <div class="adm-form">
                  <label>Wykonawca<input class="adm-input" [(ngModel)]="editing.artist"></label>
                  <label>Tytuł<input class="adm-input" [(ngModel)]="editing.title"></label>
                  <label>Wersja<input class="adm-input" [(ngModel)]="editing.version"></label>
                  <label>Gatunek
                    <select class="adm-input" [(ngModel)]="editing.genre">
                      <option *ngFor="let g of genres" [value]="g.id">{{ g.label }}</option>
                    </select>
                  </label>
                  <label style="grid-column: 1 / -1;">Link (hostuje.net albo soundfiles.eu)<input class="adm-input" [(ngModel)]="editing.url"></label>
                </div>
                <div class="adm-actions">
                  <button class="adm-btn primary" type="button" (click)="save()">Zapisz zmiany</button>
                  <button class="adm-btn" type="button" (click)="editing = null">Anuluj</button>
                </div>
              </td>
            </tr>
          </ng-container>
          <tr *ngIf="!result.items.length"><td colspan="8" class="adm-empty">Brak utworów.</td></tr>
        </tbody>
      </table>
    </div>
    <app-pager *ngIf="result" [page]="result.page + 1" [count]="pages" label="Strony listy" (pageChange)="load($event - 1)"></app-pager>
  `
})
export class AdminTracksComponent implements OnInit {
  readonly genres = GENRES;
  q = '';
  genre = '';
  result: Page<TrackRow> | null = null;
  editing: TrackRow | null = null;
  error = '';
  saved = '';

  constructor(private admin: AdminService) {}

  ngOnInit() {
    this.load(0);
  }

  get pages() {
    return this.result ? Math.max(1, Math.ceil(this.result.total / this.result.size)) : 1;
  }

  label(id: string) {
    return GENRES.find(g => g.id === id)?.label || id;
  }

  load(page: number) {
    this.error = '';
    this.admin.tracks(this.q, this.genre, page).subscribe({
      next: result => this.result = result,
      error: () => this.error = 'Nie udało się wczytać utworów.'
    });
  }

  edit(t: TrackRow) {
    this.saved = '';
    this.editing = {...t, version: t.version || ''};
  }

  save() {
    const e = this.editing;
    this.error = '';
    this.admin.changeTrack(e.id, {artist: e.artist, title: e.title, version: e.version, genre: e.genre, url: e.url}).subscribe({
      next: row => {
        const index = this.result.items.findIndex(item => item.id === row.id);
        this.result.items[index] = row;
        this.editing = null;
        this.saved = 'Zapisano: ' + row.artist + ' – ' + row.title;
      },
      error: err => this.error = err?.error?.message || 'Nie udało się zapisać utworu.'
    });
  }

  remove(t: TrackRow) {
    if (!confirm('Usunąć „' + t.artist + ' – ' + t.title + '”? Znikną też jego oceny, komentarze i odsłuchy. Tego nie da się cofnąć.')) {
      return;
    }
    this.admin.deleteTrack(t.id).subscribe({
      next: () => {
        this.saved = 'Usunięto: ' + t.artist + ' – ' + t.title;
        this.load(this.result.page);
      },
      error: err => this.error = err?.error?.message || 'Nie udało się usunąć utworu.'
    });
  }
}
