import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {AdminEvent, AdminService, AuditRow, Page} from './admin.service';
import {RefreshAfterHttpDirective} from '../services/ui-refresh';

const TYPES = [
  {id: 'all', label: 'Wszystko'},
  {id: 'users', label: 'Rejestracje'},
  {id: 'tracks', label: 'Nowe utwory'},
  {id: 'comments', label: 'Komentarze'},
  {id: 'votes', label: 'Oceny'},
  {id: 'admin', label: 'Działania adminów'}
];

/** Two routes share this view: the site activity feed and the admin audit log. */
@Component({
  selector: 'app-admin-activity',
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective],
  template: `
    <header class="adm-head">
      <div>
        <p class="kicker">Panel administratora</p>
        <h1>{{ auditMode ? 'Dziennik zmian' : 'Aktywność' }}</h1>
        <span class="adm-muted">{{ auditMode ? 'Każda zmiana zrobiona w panelu: kto, co i kiedy.' : 'Co dzieje się w serwisie — od najnowszych.' }}</span>
      </div>
      <button class="adm-btn" type="button" (click)="load(0)">↻ Odśwież</button>
    </header>

    <p class="adm-error" *ngIf="error">{{ error }}</p>

    <ng-container *ngIf="!auditMode">
      <div class="adm-chips" style="margin-bottom: 14px;">
        <button *ngFor="let t of types" class="chip" type="button" [class.on]="type === t.id" (click)="setType(t.id)">{{ t.label }}</button>
      </div>
      <section class="adm-card" *ngIf="events">
        <app-admin-feed [events]="events.items"></app-admin-feed>
      </section>
      <app-pager *ngIf="events" [page]="events.page + 1" [count]="pages(events)" label="Strony" (pageChange)="load($event - 1)"></app-pager>
    </ng-container>

    <ng-container *ngIf="auditMode">
      <div class="adm-table-wrap" *ngIf="audit">
        <table class="adm-table">
          <thead><tr><th>Kiedy</th><th>Kto</th><th>Działanie</th><th>Czego dotyczy</th><th>Szczegóły</th></tr></thead>
          <tbody>
            <tr *ngFor="let a of audit.items">
              <td class="adm-nowrap" [attr.title]="a.createdAt">{{ a.createdAt | date:'dd.MM.yyyy HH:mm':'Europe/Warsaw' }}</td>
              <td><strong>{{ a.actor }}</strong></td>
              <td><span class="adm-pill" [class.bad]="a.action.endsWith('delete')">{{ actionLabel(a.action) }}</span></td>
              <td>
                <a *ngIf="a.targetType === 'user'" [routerLink]="['/admin/uzytkownicy', a.targetId]">{{ a.targetLabel }}</a>
                <a *ngIf="a.targetType === 'track' && !a.action.endsWith('delete')" [routerLink]="['/track', a.targetId]">{{ a.targetLabel }}</a>
                <span *ngIf="a.targetType === 'comment' || a.action.endsWith('delete')">{{ a.targetLabel }}</span>
              </td>
              <td style="min-width: 260px;">{{ a.details }}</td>
            </tr>
            <tr *ngIf="!audit.items.length"><td colspan="5" class="adm-empty">Nikt jeszcze nic nie zmieniał w panelu.</td></tr>
          </tbody>
        </table>
      </div>
      <app-pager *ngIf="audit" [page]="audit.page + 1" [count]="pages(audit)" label="Strony" (pageChange)="load($event - 1)"></app-pager>
    </ng-container>
  `
})
export class AdminActivityComponent implements OnInit {
  readonly types = TYPES;
  type = 'all';
  auditMode = false;
  events: Page<AdminEvent> | null = null;
  audit: Page<AuditRow> | null = null;
  error = '';

  constructor(private admin: AdminService, private route: ActivatedRoute) {}

  ngOnInit() {
    this.auditMode = !!this.route.snapshot.data['audit'];
    this.load(0);
  }

  pages(page: Page<unknown>) {
    return Math.max(1, Math.ceil(page.total / page.size));
  }

  setType(id: string) {
    this.type = id;
    this.load(0);
  }

  actionLabel(action: string) {
    return ({
      'user.update': 'zmiana konta',
      'track.update': 'edycja utworu',
      'track.delete': 'usunięcie utworu',
      'comment.update': 'edycja komentarza',
      'comment.delete': 'usunięcie komentarza'
    } as Record<string, string>)[action] || action;
  }

  load(page: number) {
    this.error = '';
    if (this.auditMode) {
      this.admin.audit(page).subscribe({next: a => this.audit = a, error: () => this.error = 'Nie udało się wczytać dziennika.'});
    } else {
      this.admin.activity(this.type, page).subscribe({next: e => this.events = e, error: () => this.error = 'Nie udało się wczytać aktywności.'});
    }
  }
}
