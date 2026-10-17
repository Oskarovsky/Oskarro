import {Component, OnInit} from '@angular/core';
import {AdminService, CommentRow, Page} from './admin.service';
import {RefreshAfterHttpDirective} from '../services/ui-refresh';

@Component({
  selector: 'app-admin-comments',
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective],
  template: `
    <header class="adm-head">
      <div>
        <p class="kicker">Panel administratora</p>
        <h1>Komentarze</h1>
      </div>
      <span class="adm-muted" *ngIf="result">{{ result.total }} komentarzy</span>
    </header>

    <div class="adm-toolbar">
      <input type="search" placeholder="Treść, autor albo tytuł utworu…" [(ngModel)]="q" (keyup.enter)="load(0)" (search)="load(0)" aria-label="Szukaj komentarza">
      <button class="adm-btn" type="button" (click)="load(0)">Szukaj</button>
    </div>

    <p class="adm-error" *ngIf="error" role="alert">{{ error }}</p>
    <p class="adm-saved" *ngIf="saved" role="status">{{ saved }}</p>

    <div class="adm-table-wrap" *ngIf="result">
      <div class="adm-comment" *ngFor="let c of result.items">
        <span class="adm-muted">
          <a [routerLink]="['/profile', c.author]"><strong>{{ c.author }}</strong></a>
          pod <a [routerLink]="['/track', c.trackId]" target="_blank">{{ c.trackTitle }}</a>
          · <span [attr.title]="c.createdAt">{{ c.createdAt | ago }}</span>
        </span>
        <ng-container *ngIf="editingId !== c.id">
          <p>{{ c.text }}</p>
          <div class="adm-actions" style="margin-top: 2px;">
            <button class="adm-btn" type="button" (click)="edit(c)">Edytuj</button>
            <button class="adm-btn danger" type="button" (click)="remove(c)">Usuń</button>
          </div>
        </ng-container>
        <ng-container *ngIf="editingId === c.id">
          <textarea class="adm-textarea" maxlength="1000" [(ngModel)]="draft" aria-label="Treść komentarza"></textarea>
          <div class="adm-actions" style="margin-top: 2px;">
            <button class="adm-btn primary" type="button" (click)="save(c)">Zapisz</button>
            <button class="adm-btn" type="button" (click)="editingId = null">Anuluj</button>
          </div>
        </ng-container>
      </div>
      <p class="adm-empty" *ngIf="!result.items.length">Brak komentarzy.</p>
    </div>
    <app-pager *ngIf="result" [page]="result.page + 1" [count]="pages" label="Strony listy" (pageChange)="load($event - 1)"></app-pager>
  `
})
export class AdminCommentsComponent implements OnInit {
  q = '';
  result: Page<CommentRow> | null = null;
  editingId: number | null = null;
  draft = '';
  error = '';
  saved = '';

  constructor(private admin: AdminService) {}

  ngOnInit() {
    this.load(0);
  }

  get pages() {
    return this.result ? Math.max(1, Math.ceil(this.result.total / this.result.size)) : 1;
  }

  load(page: number) {
    this.error = '';
    this.admin.comments(this.q, page).subscribe({
      next: result => this.result = result,
      error: () => this.error = 'Nie udało się wczytać komentarzy.'
    });
  }

  edit(c: CommentRow) {
    this.saved = '';
    this.editingId = c.id;
    this.draft = c.text;
  }

  save(c: CommentRow) {
    this.admin.changeComment(c.id, this.draft).subscribe({
      next: row => {
        c.text = row.text;
        this.editingId = null;
        this.saved = 'Komentarz zapisany.';
      },
      error: err => this.error = err?.error?.message || 'Nie udało się zapisać komentarza.'
    });
  }

  remove(c: CommentRow) {
    if (!confirm('Usunąć komentarz użytkownika ' + c.author + '?')) {
      return;
    }
    this.admin.deleteComment(c.id).subscribe({
      next: () => {
        this.saved = 'Komentarz usunięty.';
        this.load(this.result.page);
      },
      error: err => this.error = err?.error?.message || 'Nie udało się usunąć komentarza.'
    });
  }
}
