import {Component, OnInit} from '@angular/core';
import {AdminService, ReportRow} from './admin.service';
import {RefreshAfterHttpDirective} from '../services/ui-refresh';

const REASONS: Record<string, string> = {
  COPYRIGHT: 'Prawa autorskie', SPAM: 'Spam', OFFENSIVE: 'Treść obraźliwa', BROKEN_LINK: 'Plik nie działa', OTHER: 'Inne'
};

@Component({
  selector: 'app-admin-reports',
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective],
  template: `
    <header class="adm-head">
      <div>
        <p class="kicker">Panel administratora</p>
        <h1>Zgłoszenia</h1>
        <span class="adm-muted">Z formularza „Zgłoś naruszenie” i przycisku „Zgłoś” przy utworach.</span>
      </div>
      <div class="adm-chips">
        <button class="chip" type="button" [class.on]="status === 'OPEN'" (click)="load('OPEN')">Do rozpatrzenia</button>
        <button class="chip" type="button" [class.on]="status === 'RESOLVED'" (click)="load('RESOLVED')">Rozpatrzone</button>
        <button class="chip" type="button" [class.on]="status === 'all'" (click)="load('all')">Wszystkie</button>
      </div>
    </header>

    <p class="adm-error" *ngIf="error" role="alert">{{ error }}</p>

    <div class="adm-table-wrap">
      <div class="adm-comment" *ngFor="let r of reports">
        <span>
          <span class="adm-pill" [class.bad]="r.reason === 'COPYRIGHT' || r.reason === 'OFFENSIVE'" [class.warn]="r.reason === 'BROKEN_LINK'">{{ reason(r.reason) }}</span>
          <span class="adm-pill" [class.ok]="r.status === 'RESOLVED'">{{ r.status === 'OPEN' ? 'otwarte' : 'rozpatrzone' }}</span>
          <span class="adm-muted" [attr.title]="r.createdAt">{{ r.createdAt | ago }}</span>
        </span>
        <span *ngIf="r.trackId">Utwór: <a [routerLink]="['/track', r.trackId]" target="_blank">{{ r.trackTitle }}</a>
          · <a [routerLink]="['/admin/utwory']" [queryParams]="{}">zarządzaj w „Utwory”</a></span>
        <span *ngIf="r.url && !r.trackId">Adres: <a [href]="r.url" target="_blank" rel="noopener nofollow">{{ r.url }}</a></span>
        <p>{{ r.details }}</p>
        <span class="adm-muted">
          Zgłaszający: {{ r.reporterName || '—' }}{{ r.reporterUser ? ' (konto ' + r.reporterUser + ')' : '' }}
          <ng-container *ngIf="r.reporterEmail"> · <a [href]="'mailto:' + r.reporterEmail">{{ r.reporterEmail }}</a></ng-container>
        </span>
        <span class="adm-muted" *ngIf="r.status === 'RESOLVED'">Rozpatrzył {{ r.resolvedBy }} {{ r.resolvedAt | ago }}{{ r.resolution ? ': ' + r.resolution : '' }}</span>
        <div class="adm-actions" *ngIf="r.status === 'OPEN'">
          <input class="adm-input" style="flex: 1 1 240px;" [(ngModel)]="notes[r.id]" placeholder="Co zrobiono (np. usunięto utwór, odpisano)">
          <button class="adm-btn primary" type="button" (click)="resolve(r, 'RESOLVED')">Oznacz jako rozpatrzone</button>
        </div>
        <div class="adm-actions" *ngIf="r.status === 'RESOLVED'">
          <button class="adm-btn" type="button" (click)="resolve(r, 'OPEN')">Otwórz ponownie</button>
        </div>
      </div>
      <p class="adm-empty" *ngIf="loaded && !reports.length">{{ status === 'OPEN' ? 'Brak zgłoszeń do rozpatrzenia. 🎉' : 'Brak zgłoszeń.' }}</p>
    </div>
  `
})
export class AdminReportsComponent implements OnInit {
  status = 'OPEN';
  reports: ReportRow[] = [];
  notes: Record<number, string> = {};
  loaded = false;
  error = '';

  constructor(private admin: AdminService) {}

  ngOnInit() {
    this.load('OPEN');
  }

  reason(code: string) {
    return REASONS[code] || code;
  }

  load(status: string) {
    this.status = status;
    this.error = '';
    this.admin.reports(status).subscribe({
      next: rows => {
        this.reports = rows;
        this.loaded = true;
      },
      error: () => this.error = 'Nie udało się wczytać zgłoszeń.'
    });
  }

  resolve(r: ReportRow, status: string) {
    this.admin.resolveReport(r.id, status, this.notes[r.id] || '').subscribe({
      next: () => this.load(this.status),
      error: err => this.error = err?.error?.message || 'Nie udało się zapisać.'
    });
  }
}
