import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Subscription} from 'rxjs';
import {AdminService, UserChange, UserDetail} from './admin.service';
import {RefreshAfterHttpDirective} from '../services/ui-refresh';
import {TokenStorageService} from '../services/auth/token-storage.service';
import {genreLabel} from '../tracks/genres';

@Component({
  selector: 'app-admin-user',
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective],
  template: `
    <a class="adm-muted" routerLink="/admin/uzytkownicy">← Wszyscy użytkownicy</a>
    <p class="adm-error" *ngIf="error" role="alert">{{ error }}</p>
    <p class="adm-saved" *ngIf="saved" role="status">{{ saved }}</p>

    <ng-container *ngIf="d as detail">
      <header class="adm-head" style="margin-top: 10px;">
        <div>
          <h1>{{ detail.user.username }}</h1>
          <span class="adm-muted">{{ detail.user.email }} · ID {{ detail.user.id }}</span>
          <div style="margin-top: 6px;">
            <span class="adm-pill bad" *ngIf="detail.user.banned">zablokowany{{ detail.user.banReason ? ': ' + detail.user.banReason : '' }}</span>
            <span class="adm-pill warn" *ngIf="!detail.user.activated">nieaktywowany</span>
            <span class="adm-pill ok" *ngIf="!detail.user.banned && detail.user.activated">aktywny</span>
            <span class="adm-pill accent" *ngIf="isAdmin">admin</span>
            <span class="adm-pill accent" *ngIf="isMod">moderator</span>
          </div>
        </div>
        <a class="adm-btn" [routerLink]="['/profile', detail.user.username]" target="_blank">Profil publiczny ↗</a>
      </header>

      <div class="adm-kpis">
        <div class="adm-kpi"><span>Rejestracja</span><strong style="font-size: 1.1rem;">{{ detail.user.createdAt ? (detail.user.createdAt | date:'dd.MM.yyyy HH:mm':'Europe/Warsaw') : 'przed 17.10.2026' }}</strong><small>{{ detail.user.createdAt | ago }}</small></div>
        <div class="adm-kpi"><span>Ostatnie logowanie</span><strong style="font-size: 1.1rem;">{{ detail.user.lastLoginAt ? (detail.user.lastLoginAt | date:'dd.MM.yyyy HH:mm':'Europe/Warsaw') : '—' }}</strong><small>{{ detail.user.lastLoginAt | ago }}</small></div>
        <div class="adm-kpi"><span>Ostatnia aktywność</span><strong style="font-size: 1.1rem;">{{ detail.user.lastSeenAt ? (detail.user.lastSeenAt | date:'dd.MM.yyyy HH:mm':'Europe/Warsaw') : '—' }}</strong><small>{{ detail.user.lastSeenAt | ago }}</small></div>
        <div class="adm-kpi"><span>Wkład</span><strong>{{ detail.user.tracks }}</strong><small>utworów · {{ detail.user.comments }} koment. · {{ detail.user.votes }} ocen</small></div>
      </div>

      <div class="adm-profile">
        <section class="adm-card">
          <div class="adm-tabs" role="tablist">
            <button type="button" [class.on]="tab === 'tracks'" (click)="tab = 'tracks'">Utwory ({{ detail.tracks.length }})</button>
            <button type="button" [class.on]="tab === 'comments'" (click)="tab = 'comments'">Komentarze ({{ detail.comments.length }})</button>
            <button type="button" [class.on]="tab === 'votes'" (click)="tab = 'votes'">Oceny ({{ detail.votes.length }})</button>
            <button type="button" [class.on]="tab === 'audit'" (click)="tab = 'audit'">Historia zmian ({{ detail.audit.length }})</button>
          </div>

          <div *ngIf="tab === 'tracks'">
            <table class="adm-table" *ngIf="detail.tracks.length; else noneTracks">
              <tbody>
                <tr *ngFor="let t of detail.tracks">
                  <td><a [routerLink]="['/track', t.id]">{{ t.artist }} – {{ t.title }}</a> <span class="adm-pill">{{ genreName(t.genre) }}</span></td>
                  <td class="adm-num">{{ t.score > 0 ? '+' : '' }}{{ t.score }}</td>
                  <td class="adm-num">{{ t.views }} odsł.</td>
                  <td class="adm-num adm-muted">{{ t.createdAt | ago }}</td>
                </tr>
              </tbody>
            </table>
            <ng-template #noneTracks><p class="adm-empty">Brak dodanych utworów.</p></ng-template>
          </div>

          <div *ngIf="tab === 'comments'">
            <div class="adm-comment" *ngFor="let c of detail.comments">
              <span class="adm-muted">pod <a [routerLink]="['/track', c.trackId]">{{ c.trackTitle }}</a> · {{ c.createdAt | ago }}</span>
              <p>{{ c.text }}</p>
            </div>
            <p class="adm-empty" *ngIf="!detail.comments.length">Brak komentarzy.</p>
          </div>

          <div *ngIf="tab === 'votes'">
            <table class="adm-table" *ngIf="detail.votes.length; else noneVotes">
              <tbody>
                <tr *ngFor="let v of detail.votes">
                  <td><a [routerLink]="['/track', v.trackId]">{{ v.trackTitle }}</a></td>
                  <td class="adm-num"><span class="adm-pill" [class.ok]="v.vote > 0" [class.bad]="v.vote < 0">{{ v.vote > 0 ? '+1' : '−1' }}</span></td>
                  <td class="adm-num adm-muted">{{ v.createdAt | ago }}</td>
                </tr>
              </tbody>
            </table>
            <ng-template #noneVotes><p class="adm-empty">Brak ocen.</p></ng-template>
          </div>

          <div *ngIf="tab === 'audit'">
            <ul class="adm-feed" *ngIf="detail.audit.length; else noneAudit">
              <li *ngFor="let a of detail.audit">
                <span class="icon admin">🛡</span>
                <span class="text"><strong>{{ a.actor }}</strong> {{ a.details }}</span>
                <time>{{ a.createdAt | ago }}</time>
              </li>
            </ul>
            <ng-template #noneAudit><p class="adm-empty">Administratorzy nic tu jeszcze nie zmieniali.</p></ng-template>
          </div>
        </section>

        <aside class="adm-stack">
          <section class="adm-card">
            <p class="adm-section-title">Dostęp</p>
            <div class="adm-actions" style="margin-top: 0;">
              <button class="adm-btn" type="button" *ngIf="!detail.user.activated" (click)="change({activated: true}, 'Konto aktywowane.')">Aktywuj konto</button>
              <button class="adm-btn" type="button" *ngIf="detail.user.activated && !self" (click)="change({activated: false}, 'Konto dezaktywowane.')">Dezaktywuj</button>
            </div>
            <div *ngIf="!detail.user.banned && !self" style="margin-top: 12px;">
              <label class="adm-muted" for="ban-reason">Powód blokady (widoczny w panelu)</label>
              <input id="ban-reason" class="adm-input" style="width: 100%; margin: 4px 0 8px;" maxlength="200" [(ngModel)]="banReason" placeholder="np. spam w komentarzach">
              <button class="adm-btn danger" type="button" (click)="ban()">Zablokuj konto</button>
            </div>
            <div *ngIf="detail.user.banned" style="margin-top: 12px;">
              <button class="adm-btn primary" type="button" (click)="change({banned: false}, 'Konto odblokowane.')">Odblokuj konto</button>
            </div>
            <p class="adm-muted" *ngIf="self" style="margin: 10px 0 0;">To Twoje konto — nie możesz go zablokować ani odebrać sobie roli administratora.</p>
          </section>

          <section class="adm-card">
            <p class="adm-section-title">Role</p>
            <label class="adm-check"><input type="checkbox" [(ngModel)]="roleMod"> Moderator</label><br>
            <label class="adm-check"><input type="checkbox" [(ngModel)]="roleAdmin" [disabled]="self && isAdmin"> Administrator</label>
            <div class="adm-actions">
              <button class="adm-btn" type="button" (click)="saveRoles()">Zapisz role</button>
            </div>
          </section>

          <section class="adm-card">
            <p class="adm-section-title">Ranga specjalna</p>
            <input class="adm-input" style="width: 100%;" maxlength="32" [(ngModel)]="rank" placeholder="np. Rezydent VIXA (puste = ranga za aktywność)">
            <div class="adm-actions">
              <button class="adm-btn" type="button" (click)="change({rankOverride: rank || ''}, 'Ranga zapisana.')">Zapisz rangę</button>
            </div>
          </section>

          <section class="adm-card" *ngIf="detail.bio || detail.website">
            <p class="adm-section-title">Profil</p>
            <p style="margin: 0 0 6px;" *ngIf="detail.bio">{{ detail.bio }}</p>
            <a *ngIf="detail.website" [href]="detail.website" target="_blank" rel="noopener nofollow">{{ detail.website }}</a>
          </section>
        </aside>
      </div>
    </ng-container>
  `
})
export class AdminUserComponent implements OnInit, OnDestroy {
  d: UserDetail | null = null;
  tab: 'tracks' | 'comments' | 'votes' | 'audit' = 'tracks';
  error = '';
  saved = '';
  banReason = '';
  rank = '';
  roleMod = false;
  roleAdmin = false;
  private id = 0;
  private sub?: Subscription;
  private readonly me: string;

  constructor(private admin: AdminService, private route: ActivatedRoute, tokens: TokenStorageService) {
    this.me = tokens.getUser()?.username || '';
  }

  ngOnInit() {
    this.sub = this.route.paramMap.subscribe(params => {
      this.id = Number(params.get('id'));
      this.admin.user(this.id).subscribe({
        next: d => this.show(d),
        error: () => this.error = 'Nie udało się wczytać użytkownika.'
      });
    });
  }

  ngOnDestroy() {
    this.sub?.unsubscribe();
  }

  genreName(id: string) {
    return genreLabel(id);
  }

  get self() {
    return this.d?.user.username === this.me;
  }

  get isAdmin() {
    return !!this.d?.user.roles.includes('ROLE_ADMIN');
  }

  get isMod() {
    return !!this.d?.user.roles.includes('ROLE_MODERATOR');
  }

  ban() {
    if (!confirm('Zablokować konto ' + this.d?.user.username + '? Użytkownik zostanie od razu wylogowany i nie zaloguje się ponownie.')) {
      return;
    }
    this.change({banned: true, banReason: this.banReason.trim()}, 'Konto zablokowane.');
  }

  saveRoles() {
    const roles = ['USER'];
    if (this.roleMod) {
      roles.push('MODERATOR');
    }
    if (this.roleAdmin) {
      roles.push('ADMIN');
    }
    this.change({roles}, 'Role zapisane.');
  }

  change(change: UserChange, done: string) {
    this.error = '';
    this.saved = '';
    this.admin.changeUser(this.id, change).subscribe({
      next: d => {
        this.show(d);
        this.saved = done;
      },
      error: err => this.error = err?.error?.message || 'Nie udało się zapisać zmiany.'
    });
  }

  private show(d: UserDetail) {
    this.d = d;
    this.rank = d.user.rankOverride || '';
    this.roleMod = d.user.roles.includes('ROLE_MODERATOR');
    this.roleAdmin = d.user.roles.includes('ROLE_ADMIN');
    this.banReason = '';
  }
}
