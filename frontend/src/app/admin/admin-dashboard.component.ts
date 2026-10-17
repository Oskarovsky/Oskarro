import {Component, OnInit} from '@angular/core';
import {AdminService, Overview} from './admin.service';
import {RefreshAfterHttpDirective} from '../services/ui-refresh';

@Component({
  selector: 'app-admin-dashboard',
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective],
  template: `
    <header class="adm-head">
      <div>
        <p class="kicker">Panel administratora</p>
        <h1>Pulpit</h1>
      </div>
      <button class="adm-btn" type="button" (click)="load()">↻ Odśwież</button>
    </header>

    <p class="adm-error" *ngIf="error">{{ error }}</p>

    <ng-container *ngIf="o">
      <div class="adm-kpis">
        <a class="adm-kpi" routerLink="/admin/uzytkownicy"><span>Użytkownicy</span><strong>{{ o.users }}</strong><small>+{{ o.newUsers7d }} w 7 dni · +{{ o.newUsers30d }} w 30 dni</small></a>
        <a class="adm-kpi" [routerLink]="['/admin/uzytkownicy']" [queryParams]="{filtr: 'active'}"><span>Aktywni</span><strong>{{ o.active24h }}</strong><small>24 h · {{ o.active7d }} w 7 dni · {{ o.active30d }} w 30 dni</small></a>
        <a class="adm-kpi" [class.alert]="o.notActivated > 0" [routerLink]="['/admin/uzytkownicy']" [queryParams]="{filtr: 'unactivated'}"><span>Nieaktywowane konta</span><strong>{{ o.notActivated }}</strong><small>czekają na link z maila</small></a>
        <a class="adm-kpi" [routerLink]="['/admin/uzytkownicy']" [queryParams]="{filtr: 'banned'}"><span>Zablokowani</span><strong>{{ o.banned }}</strong><small>zespół: {{ o.staff }} os.</small></a>
        <a class="adm-kpi" routerLink="/admin/utwory"><span>Utwory</span><strong>{{ o.tracks }}</strong><small>+{{ o.tracks7d }} w 7 dni</small></a>
        <a class="adm-kpi" routerLink="/admin/komentarze"><span>Komentarze</span><strong>{{ o.comments }}</strong><small>+{{ o.comments7d }} w 7 dni</small></a>
        <div class="adm-kpi"><span>Odsłuchy</span><strong>{{ o.plays }}</strong><small>+{{ o.plays7d }} w 7 dni</small></div>
        <div class="adm-kpi"><span>Oceny</span><strong>{{ o.votes }}</strong><small>łącznie</small></div>
      </div>

      <app-growth-board></app-growth-board>

      <div class="adm-grid-2">
        <section class="adm-card">
          <h2>Najnowsi użytkownicy <a routerLink="/admin/uzytkownicy">wszyscy →</a></h2>
          <div class="adm-table-wrap" style="border: 0;">
            <table class="adm-table">
              <tbody>
                <tr class="clickable" *ngFor="let u of o.latestUsers" [routerLink]="['/admin/uzytkownicy', u.id]">
                  <td class="adm-user"><strong>{{ u.username }}</strong><span class="adm-muted">{{ u.email }}</span></td>
                  <td>
                    <span class="adm-pill bad" *ngIf="u.banned">zablokowany</span>
                    <span class="adm-pill warn" *ngIf="!u.activated">nieaktywowany</span>
                  </td>
                  <td class="adm-num adm-muted">{{ u.createdAt | ago }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
        <section class="adm-card">
          <h2>Ostatnia aktywność <a routerLink="/admin/aktywnosc">więcej →</a></h2>
          <app-admin-feed [events]="o.latestEvents"></app-admin-feed>
        </section>
      </div>
    </ng-container>
  `
})
export class AdminDashboardComponent implements OnInit {
  o: Overview | null = null;
  error = '';

  constructor(private admin: AdminService) {}

  ngOnInit() {
    this.load();
  }

  load() {
    this.error = '';
    this.admin.overview().subscribe({
      next: o => this.o = o,
      error: () => this.error = 'Nie udało się wczytać danych pulpitu.'
    });
  }
}
