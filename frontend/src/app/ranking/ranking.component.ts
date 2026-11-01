import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { RefreshAfterHttpDirective } from '../services/ui-refresh';
import { TrackService } from '../services/track/track.service';
import { LangService } from '../lang.service';
import { ActivatedRoute, Router } from '@angular/router';
import { GENRES } from '../tracks/genres';
import { SeoService } from '../seo.service';

type Period = 'week' | 'month' | 'all';

@Component({
  selector: 'app-ranking',
  templateUrl: './ranking.component.html',
  styleUrls: ['./ranking.component.css'],
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class RankingComponent implements OnInit {

  board: any;
  rows: Array<any> = [];
  ready = false;
  period: Period = 'week';
  category = 'club';
  readonly periods: {id: Period, label: string, labelEn: string}[] = [
    {id: 'week', label: 'Ten tydzień', labelEn: 'This week'},
    {id: 'month', label: 'Ten miesiąc', labelEn: 'This month'},
    {id: 'all', label: 'Ogólny', labelEn: 'All time'}
  ];
  /** One chart per genre, plus sets — different worlds are never ranked together. */
  readonly categories = [...GENRES.map(g => ({slug: g.slug, label: g.label})), {slug: 'sety', label: 'Sety / Mixy'}];
  private request = 0;

  constructor(private http: HttpClient,
              private trackService: TrackService,
              private route: ActivatedRoute,
              private router: Router,
              private seo: SeoService,
              readonly lang: LangService) {}

  ngOnInit() {
    this.http.get(environment.serverUrl + '/profile/ranking').subscribe(board => this.board = board);
    // Old links (?okres=…&kategoria=…) move to the indexable address /ranking/:kategoria/:okres.
    const legacy = this.route.snapshot.queryParamMap;
    if (legacy.get('okres') || legacy.get('kategoria')) {
      const okres = legacy.get('okres');
      this.choose(okres === 'miesiac' ? 'month' : okres === 'ogolny' ? 'all' : 'week', legacy.get('kategoria') || 'club');
    }
    this.route.paramMap.subscribe(params => {
      const period = params.get('okres');
      const category = params.get('kategoria');
      this.period = period === 'miesiac' ? 'month' : period === 'ogolny' ? 'all' : 'week';
      this.category = this.categories.some(c => c.slug === category) ? category : 'club';
      if (category) {
        const label = this.categories.find(c => c.slug === this.category)?.label || '';
        const who = this.category === 'sety' ? 'setów' : label;
        const when = this.period === 'week' ? 'tygodnia' : this.period === 'month' ? 'miesiąca' : 'wszech czasów';
        this.seo.setTitle('Top 10 ' + who + ' ' + when + ' – ranking hitów ' + new Date().getFullYear() + ' | VIXA',
          'Ranking ' + who + ' ' + when + ' na VIXA: 10 numerów z największą liczbą głosów i odsłuchów. Słuchaj za darmo i pobieraj mp3.',
          '/ranking/' + this.category + (period ? '/' + period : ''));
      }
      this.load();
    });
  }

  /** The choice goes into the address, so a chart can be shared or bookmarked. */
  choose(period: Period, category: string) {
    const okres = period === 'month' ? 'miesiac' : period === 'all' ? 'ogolny' : null;
    this.router.navigate(okres ? ['/ranking', category, okres] : ['/ranking', category], {replaceUrl: true});
  }

  private load() {
    const mine = ++this.request;
    this.ready = false;
    this.http.get<any>(environment.serverUrl + '/charts', {params: {period: this.period, category: this.category}}).subscribe({
      next: chart => {
        if (mine === this.request) {
          this.rows = chart?.rows || [];
          this.ready = true;
        }
      },
      error: () => {
        if (mine === this.request) {
          this.rows = [];
          this.ready = true;
        }
      }
    });
  }

  chartTitle() {
    const label = this.categories.find(c => c.slug === this.category)?.label || '';
    const when = this.period === 'week' ? this.lang.t('ostatnie 7 dni', 'last 7 days')
      : this.period === 'month' ? this.lang.t('ostatnie 30 dni', 'last 30 days')
      : this.lang.t('od początku', 'all time');
    return label + ' — ' + when;
  }

  emptyLine() {
    return this.period === 'all'
      ? this.lang.t('W tej kategorii nie ma jeszcze utworów.', 'There are no tracks in this category yet.')
      : this.lang.t('W tym okresie nikt nie słuchał ani nie oceniał numerów z tej kategorii. Sprawdź dłuższy okres.',
        'Nobody played or voted on tracks in this category in this period. Try a longer one.');
  }

  votesWord(count: number) {
    const n = count || 0;
    return (n > 0 ? '+' : '') + n + ' ' + this.lang.plural(Math.abs(n), ['głos', 'głosy', 'głosów'], ['vote', 'votes']);
  }

  playsWord(count: number) {
    return (count || 0) + ' ' + this.lang.plural(count || 0, ['odsłuch', 'odsłuchy', 'odsłuchów'], ['play', 'plays']);
  }

  cover(track) {
    return this.trackService.coverUrl(track.id);
  }

  mine(username: string) {
    return !!this.board?.me && this.board.me.username === username;
  }

  statusLine(): string {
    const me = this.board?.me;
    if (!me) {
      return '';
    }
    const rank = this.lang.text(me.rank);
    const next = this.lang.text(me.nextRank);
    if (this.atTop()) {
      return this.lang.t(
        `Jesteś na randze ${me.rank}, masz ${me.score} pkt. Jesteś na szczycie.`,
        `You are ${rank}, with ${me.score} points. You are at the top.`
      );
    }
    return this.lang.t(
      `Jesteś na randze ${me.rank}, masz ${me.score} pkt. Do rangi ${me.nextRank} brakuje ${me.pointsToNext}.`,
      `You are ${rank}, with ${me.score} points. ${me.pointsToNext} to go until ${next}.`
    );
  }

  atTop(): boolean {
    const me = this.board?.me;
    if (!me) {
      return false;
    }
    const gap = me.pointsToNext;
    return !me.nextRank || gap == null || gap <= 0;
  }

}
