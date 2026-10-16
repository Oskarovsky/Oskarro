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
      <button class="btn-ghost now-next" *ngIf="loggedIn" type="button" (click)="playback.next()" [disabled]="!playback.hasNext">{{ 'Losowy' | t:'Random' }}</button>
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
