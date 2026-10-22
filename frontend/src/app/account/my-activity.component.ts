import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {TrackService} from '../services/track/track.service';
import {TokenStorageService} from '../services/auth/token-storage.service';
import {LangService} from '../lang.service';
import {RefreshAfterHttpDirective} from '../services/ui-refresh';

type Tab = 'utwory' | 'komentarze' | 'polubione';

/** Everything the signed-in user has added, written and liked, one tab each. */
@Component({
  selector: 'app-my-activity',
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective],
  template: `
    <div class="page">
      <a class="back" routerLink="/profile">← {{ 'Profil' | t:'Profile' }}</a>
      <p class="kicker">{{ 'Twoje konto' | t:'Your account' }}</p>
      <h1 class="display">{{ 'Moja aktywność' | t:'My activity' }}</h1>

      <p *ngIf="!loggedIn"><a routerLink="/login">{{ 'Zaloguj się' | t:'Sign in' }}</a>{{ lang.en ? ' to see your activity.' : ', żeby zobaczyć swoją aktywność.' }}</p>

      <ng-container *ngIf="loggedIn">
        <nav class="tabs" role="tablist">
          <button type="button" role="tab" *ngFor="let item of tabs" [class.on]="tab === item.id" [attr.aria-selected]="tab === item.id" (click)="open(item.id)">
            {{ lang.en ? item.en : item.pl }}<span class="tab-count" *ngIf="loaded[item.id]">{{ count(item.id) }}</span>
          </button>
        </nav>

        <p class="meta" *ngIf="!loaded[tab]">{{ 'Wczytuję…' | t:'Loading…' }}</p>
        <p class="alert" role="alert" *ngIf="error">{{ error }}</p>

        <div class="latest-grid" *ngIf="tab !== 'komentarze' && loaded[tab]">
          <p class="meta" *ngIf="!list(tab).length">{{ tab === 'utwory' ? ('Nie dodałeś jeszcze żadnego utworu.' | t:'You have not shared any track yet.') : ('Nie polubiłeś jeszcze żadnego utworu.' | t:'You have not liked any track yet.') }}
            <a routerLink="/track/add" *ngIf="tab === 'utwory'">{{ 'Dodaj pierwszy' | t:'Share your first' }}</a></p>
          <a class="track-row" *ngFor="let track of list(tab)" [routerLink]="['/track', track.id]">
            <span class="thumb-col">
              <span class="thumb-slot">
                <app-cover [hasCover]="track.hasCover" [src]="track.hasCover ? cover(track) : ''" [genre]="track.genre"></app-cover>
              </span>
              <app-score [value]="track.score"></app-score>
            </span>
            <span class="track-lines">
              <h2>{{ track.title }}<span class="version" *ngIf="track | versionLabel as version"> ({{ version }})</span></h2>
              <span class="line artist">{{ track.artist }}</span>
              <span class="line meta"><app-track-facts [track]="track" part="stats"></app-track-facts></span>
              <span class="line meta" *ngIf="track.createdAt">{{ track.createdAt | date:'dd.MM.yyyy HH:mm':'Europe/Warsaw' }}</span>
            </span>
          </a>
        </div>

        <div class="comment-list" *ngIf="tab === 'komentarze' && loaded.komentarze">
          <p class="meta" *ngIf="!comments.length">{{ 'Nie napisałeś jeszcze żadnego komentarza.' | t:'You have not written any comment yet.' }}</p>
          <a class="comment my-comment" *ngFor="let comment of comments" [routerLink]="['/track', comment.trackId]">
            <span class="my-comment-track">{{ comment.trackArtist }} — {{ comment.trackTitle }}</span>
            <time>{{ comment.createdAt | date:'dd.MM.yyyy HH:mm' }}</time>
            <span class="my-comment-text">{{ comment.text }}</span>
          </a>
        </div>
      </ng-container>
    </div>
  `
})
export class MyActivityComponent implements OnInit {
  readonly tabs: Array<{id: Tab, pl: string, en: string}> = [
    {id: 'utwory', pl: 'Dodane utwory', en: 'Shared tracks'},
    {id: 'komentarze', pl: 'Komentarze', en: 'Comments'},
    {id: 'polubione', pl: 'Polubione', en: 'Liked'}
  ];
  tab: Tab = 'utwory';
  tracks: any[] = [];
  liked: any[] = [];
  comments: any[] = [];
  loaded: Record<Tab, boolean> = {utwory: false, komentarze: false, polubione: false};
  error = '';

  constructor(private tracksApi: TrackService,
              private tokenStorage: TokenStorageService,
              private route: ActivatedRoute,
              private router: Router,
              readonly lang: LangService) {}

  get loggedIn() {
    return !!this.tokenStorage.getToken();
  }

  ngOnInit() {
    if (!this.loggedIn) {
      return;
    }
    this.route.queryParamMap.subscribe(params => {
      const wanted = params.get('tab') as Tab;
      this.tab = this.tabs.some(item => item.id === wanted) ? wanted : 'utwory';
    });
    this.tracksApi.mine().subscribe({next: list => { this.tracks = list || []; this.loaded.utwory = true; }, error: () => this.fail()});
    this.tracksApi.myComments().subscribe({next: list => { this.comments = list || []; this.loaded.komentarze = true; }, error: () => this.fail()});
    this.tracksApi.liked().subscribe({next: list => { this.liked = list || []; this.loaded.polubione = true; }, error: () => this.fail()});
  }

  open(tab: Tab) {
    this.router.navigate([], {queryParams: {tab}, replaceUrl: true});
  }

  list(tab: Tab) {
    return tab === 'polubione' ? this.liked : this.tracks;
  }

  count(tab: Tab) {
    return tab === 'komentarze' ? this.comments.length : this.list(tab).length;
  }

  cover(track: any) {
    return this.tracksApi.coverUrl(track.id);
  }

  private fail() {
    this.error = this.lang.t('Nie udało się wczytać listy. Odśwież stronę za chwilę.', 'Could not load the list. Refresh the page in a moment.');
  }
}
