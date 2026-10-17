import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Subscription} from 'rxjs';
import {AdminService, Page, UserRow} from './admin.service';
import {RefreshAfterHttpDirective} from '../services/ui-refresh';

const FILTERS = [
  {id: 'all', label: 'Wszyscy'},
  {id: 'active', label: 'Aktywni (7 dni)'},
  {id: 'inactive', label: 'Nieaktywni (30 dni)'},
  {id: 'unactivated', label: 'Nieaktywowane'},
  {id: 'banned', label: 'Zablokowani'},
  {id: 'staff', label: 'Zespół'}
];

@Component({
  selector: 'app-admin-users',
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective],
  template: `
    <header class="adm-head">
      <div>
        <p class="kicker">Panel administratora</p>
        <h1>Użytkownicy</h1>
      </div>
      <span class="adm-muted" *ngIf="result">{{ result.total }} {{ result.total === 1 ? 'konto' : 'kont' }}</span>
    </header>

    <div class="adm-toolbar">
      <input type="search" placeholder="Szukaj po nazwie lub e-mailu…" [(ngModel)]="q" (keyup.enter)="apply()" (search)="apply()" aria-label="Szukaj użytkownika">
      <select [(ngModel)]="sort" (change)="apply()" aria-label="Sortowanie">
        <option value="new">Najnowsi</option>
        <option value="seen">Ostatnio aktywni</option>
        <option value="tracks">Najwięcej utworów</option>
        <option value="name">Nazwa A–Z</option>
      </select>
      <button class="adm-btn" type="button" (click)="apply()">Szukaj</button>
    </div>
    <div class="adm-chips" style="margin-bottom: 14px;">
      <button *ngFor="let f of filters" class="chip" type="button" [class.on]="filter === f.id" (click)="setFilter(f.id)">{{ f.label }}</button>
    </div>

    <p class="adm-error" *ngIf="error">{{ error }}</p>

    <div class="adm-table-wrap" *ngIf="result">
      <table class="adm-table">
        <thead>
          <tr>
            <th>Użytkownik</th><th>Status</th><th>Role</th><th>Rejestracja</th><th>Ostatnio aktywny</th>
            <th class="adm-num">Utwory</th><th class="adm-num">Koment.</th><th class="adm-num">Oceny</th>
          </tr>
        </thead>
        <tbody>
          <tr class="clickable" *ngFor="let u of result.items" [routerLink]="['/admin/uzytkownicy', u.id]">
            <td class="adm-user"><strong>{{ u.username }}</strong><span class="adm-muted">{{ u.email }}</span></td>
            <td>
              <span class="adm-pill bad" *ngIf="u.banned">zablokowany</span>
              <span class="adm-pill warn" *ngIf="!u.activated">nieaktywowany</span>
              <span class="adm-pill ok" *ngIf="!u.banned && u.activated">aktywny</span>
            </td>
            <td>
              <span class="adm-pill accent" *ngIf="u.roles.includes('ROLE_ADMIN')">admin</span>
              <span class="adm-pill accent" *ngIf="u.roles.includes('ROLE_MODERATOR')">moderator</span>
              <span class="adm-pill" *ngIf="u.rankOverride">{{ u.rankOverride }}</span>
            </td>
            <td class="adm-nowrap adm-muted" [attr.title]="u.createdAt">{{ u.createdAt | ago }}</td>
            <td class="adm-nowrap" [attr.title]="u.lastSeenAt"><span class="adm-online" *ngIf="online(u)"></span>{{ u.lastSeenAt | ago }}</td>
            <td class="adm-num">{{ u.tracks }}</td>
            <td class="adm-num">{{ u.comments }}</td>
            <td class="adm-num">{{ u.votes }}</td>
          </tr>
          <tr *ngIf="!result.items.length"><td colspan="8" class="adm-empty">Nikt nie pasuje do filtrów.</td></tr>
        </tbody>
      </table>
    </div>
    <app-pager *ngIf="result" [page]="page + 1" [count]="pages" label="Strony listy" (pageChange)="go($event - 1)"></app-pager>
  `
})
export class AdminUsersComponent implements OnInit, OnDestroy {
  readonly filters = FILTERS;
  q = '';
  filter = 'all';
  sort = 'new';
  page = 0;
  result: Page<UserRow> | null = null;
  error = '';
  private sub?: Subscription;

  constructor(private admin: AdminService, private route: ActivatedRoute, private router: Router) {}

  ngOnInit() {
    this.sub = this.route.queryParamMap.subscribe(params => {
      this.q = params.get('q') || '';
      this.filter = params.get('filtr') || 'all';
      this.sort = params.get('sort') || 'new';
      this.page = Math.max(0, Number(params.get('strona') || 1) - 1);
      this.load();
    });
  }

  ngOnDestroy() {
    this.sub?.unsubscribe();
  }

  get pages() {
    return this.result ? Math.max(1, Math.ceil(this.result.total / this.result.size)) : 1;
  }

  online(u: UserRow) {
    return !!u.lastSeenAt && Date.now() - Date.parse(u.lastSeenAt) < 15 * 60 * 1000;
  }

  setFilter(id: string) {
    this.filter = id;
    this.apply();
  }

  apply() {
    this.navigate(0);
  }

  go(page: number) {
    this.navigate(page);
  }

  private navigate(page: number) {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        q: this.q || null,
        filtr: this.filter !== 'all' ? this.filter : null,
        sort: this.sort !== 'new' ? this.sort : null,
        strona: page > 0 ? page + 1 : null
      }
    });
  }

  private load() {
    this.error = '';
    this.admin.users(this.q, this.filter, this.sort, this.page).subscribe({
      next: result => this.result = result,
      error: () => this.error = 'Nie udało się wczytać listy użytkowników.'
    });
  }
}
