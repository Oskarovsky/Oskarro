import {ChangeDetectorRef, Component, OnDestroy, OnInit} from '@angular/core';
import {NavigationEnd, Router} from '@angular/router';
import {Subscription} from 'rxjs';
import {PlaybackService} from './playback.service';
import {TokenStorageService} from '../services/auth/token-storage.service';
import {environment} from '../../environments/environment';

@Component({
  selector: 'app-now-bar',
  standalone: false,
  template: `
    <aside class="now-bar" *ngIf="playback.pinned && playback.fromRadio && playback.track">
      <a class="now-id" [routerLink]="['/track', playback.track.id]">
        <span class="now-cover">
          <app-cover [hasCover]="playback.track.hasCover" [src]="cover" [genre]="playback.track.genre"></app-cover>
        </span>
        <span class="now-copy">
          <strong>{{ playback.track.title }}</strong>
          <em>{{ playback.track.artist }}</em>
        </span>
      </a>
      <a class="wave-play now-play" *ngIf="!playback.allows(playback.track.id)" routerLink="/login" [attr.aria-label]="'Zaloguj się' | t:'Sign in'">
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M8 5.5v13l11-6.5z"/></svg>
      </a>
      <button class="wave-play now-play" *ngIf="playback.allows(playback.track.id)" type="button" (click)="playback.toggle()" [attr.aria-label]="playback.playing ? ('Pauza' | t:'Pause') : ('Odtwórz' | t:'Play')">
        <svg *ngIf="!playback.playing" viewBox="0 0 24 24" aria-hidden="true"><path d="M8 5.5v13l11-6.5z"/></svg>
        <svg *ngIf="playback.playing" viewBox="0 0 24 24" aria-hidden="true"><path d="M6 5h4.2v14H6zM13.8 5H18v14h-4.2z"/></svg>
      </button>
      <input class="now-range" type="range" min="0" [max]="playback.duration || 0" step="0.1"
             [value]="playback.current" [attr.aria-label]="'Pozycja utworu' | t:'Track position'" (input)="seek($event)">
      <a class="now-next" *ngIf="!loggedIn" routerLink="/login">{{ 'Losowy po zalogowaniu' | t:'Random after you sign in' }}</a>
      <ng-container *ngIf="loggedIn">
        <button class="now-step" type="button" (click)="playback.previous()" [disabled]="!playback.hasPrevious"
                [attr.aria-label]="'Poprzedni utwór' | t:'Previous track'" [attr.title]="'Poprzedni' | t:'Previous'">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 6h2v12H6zM9.5 12 18 6v12z"/></svg>
        </button>
        <button class="now-step" type="button" (click)="playback.next()" [disabled]="!playback.hasNext"
                [attr.aria-label]="playback.sequential ? ('Następny utwór' | t:'Next track') : ('Losowy utwór' | t:'Random track')"
                [attr.title]="playback.sequential ? ('Następny' | t:'Next') : ('Losowy' | t:'Random')">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M16 6h2v12h-2zM6 6l8.5 6L6 18z"/></svg>
        </button>
      </ng-container>
      <a class="now-source" *ngIf="playback.playlist" [routerLink]="['/playlist', playback.playlist.id, 'details']"
         [attr.title]="'Słuchasz składanki' | t:'Playing a playlist'">
        ♫ {{ playback.playlist.name }}<small *ngIf="position"> · {{ position }}</small>
      </a>
      <button class="now-close" type="button" (click)="playback.close()" [attr.aria-label]="'Zamknij odtwarzacz' | t:'Close the player'">×</button>
    </aside>
  `
})
export class NowBarComponent implements OnInit, OnDestroy {
  private sub: Subscription;
  private routes: Subscription;

  constructor(readonly playback: PlaybackService,
              private router: Router,
              private tokenStorage: TokenStorageService,
              private view: ChangeDetectorRef) {}

  ngOnInit() {
    this.sub = this.playback.changes.subscribe(() => this.view.detectChanges());
    this.routes = this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        this.holdIfUnpinned(event.urlAfterRedirects);
        this.view.detectChanges();
      }
    });
  }

  ngOnDestroy() {
    this.sub?.unsubscribe();
    this.routes?.unsubscribe();
  }

  private holdIfUnpinned(url: string) {
    const track = this.playback.track;
    if (!track || (this.playback.pinned && this.playback.fromRadio)) {
      return;
    }
    const path = url.split('?')[0].split('#')[0];
    const bare = path.length > 1 && path.endsWith('/') ? path.slice(0, -1) : path;
    if (bare === '/radio' || bare === '/track/' + track.id) {
      return;
    }
    this.playback.pause();
  }

  /** "3/12" while a playlist plays in order. */
  get position() {
    if (!this.playback.sequential || !this.playback.track) {
      return '';
    }
    const index = this.playback.queue.findIndex(item => item.id === this.playback.track.id);
    return index >= 0 ? (index + 1) + '/' + this.playback.queue.length : '';
  }

  get loggedIn() {
    return !!this.tokenStorage.getToken();
  }

  get cover() {
    const track = this.playback.track;
    return track?.hasCover ? environment.serverUrl + '/tracks/' + track.id + '/cover' : '';
  }

  seek(event: Event) {
    const input = event.target as HTMLInputElement;
    const max = Number(input.max) || 0;
    if (max) {
      this.playback.seekRatio(Number(input.value) / max);
    }
  }
}
