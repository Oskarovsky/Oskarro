import {AfterViewInit, ChangeDetectorRef, Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Subscription} from 'rxjs';
import {GrowthDay, GrowthStats, StatsService} from '../services/stats.service';
import {LangService} from '../lang.service';
import {RefreshAfterHttpDirective} from '../services/ui-refresh';

type Metric = 'plays' | 'visitors' | 'comments' | 'votes' | 'users' | 'tracks';

interface Tile {
  metric: Metric;
  label: string;
  value: string;
  delta: string;
  compare: string;
  up: boolean;
  bars: {x: number, y: number, w: number, h: number, last: boolean, title: string}[];
}

interface Point {
  x: number;
  y: number;
  total: number;
  added: number;
  date: string;
}

const MONTHS_PL = ['sty', 'lut', 'mar', 'kwi', 'maj', 'cze', 'lip', 'sie', 'wrz', 'paź', 'lis', 'gru'];
const MONTHS_EN = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

const CHART_H = 210;
const PAD = {top: 16, right: 16, bottom: 28, left: 44};
const SPARK_W = 160;
const SPARK_H = 40;

/** Home page "pulse": how fast the library and the community grow. */
@Component({
  selector: 'app-growth-board',
  templateUrl: './growth-board.component.html',
  styleUrls: ['./growth-board.component.css'],
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class GrowthBoardComponent implements OnInit, AfterViewInit, OnDestroy {
  range = 30;
  data: GrowthStats | null = null;

  width = 720;
  readonly height = CHART_H;
  readonly pad = PAD;
  readonly sparkW = SPARK_W;
  readonly sparkH = SPARK_H;

  points: Point[] = [];
  linePath = '';
  areaPath = '';
  yTicks: {y: number, label: string}[] = [];
  xTicks: {x: number, label: string, anchor: string}[] = [];
  tiles: Tile[] = [];
  hover: Point | null = null;

  totalTracks = 0;
  addedInRange = 0;
  addedThisWeek = 0;

  @ViewChild('plot') plot?: ElementRef<HTMLElement>;

  private resize?: ResizeObserver;
  private langSub?: Subscription;

  constructor(private stats: StatsService,
              readonly lang: LangService,
              private cdr: ChangeDetectorRef) {}

  ngOnInit() {
    this.langSub = this.lang.changes.subscribe(() => this.paint());
    this.load();
  }

  ngAfterViewInit() {
    const el = this.plot?.nativeElement;
    if (!el || typeof ResizeObserver === 'undefined') {
      return;
    }
    this.resize = new ResizeObserver(entries => {
      const next = Math.round(entries[0].contentRect.width);
      if (next > 0 && next !== this.width) {
        this.width = next;
        this.paint();
        this.cdr.detectChanges();
      }
    });
    this.resize.observe(el);
  }

  ngOnDestroy() {
    this.resize?.disconnect();
    this.langSub?.unsubscribe();
  }

  setRange(days: number) {
    if (this.range !== days) {
      this.range = days;
      this.load();
    }
  }

  onMove(event: PointerEvent) {
    if (!this.points.length) {
      return;
    }
    const box = (event.currentTarget as Element).getBoundingClientRect();
    const x = event.clientX - box.left;
    const step = this.points.length > 1 ? (this.points[1].x - this.points[0].x) : 1;
    const index = Math.round((x - this.points[0].x) / step);
    this.hover = this.points[Math.max(0, Math.min(this.points.length - 1, index))];
  }

  onLeave() {
    this.hover = null;
  }

  get tipLeft() {
    if (!this.hover) {
      return 0;
    }
    return Math.max(70, Math.min(this.width - 70, this.hover.x));
  }

  dayLabel(date: string, long = false) {
    const [y, m, d] = date.split('-').map(Number);
    const months = this.lang.en ? MONTHS_EN : MONTHS_PL;
    return long ? `${d} ${months[m - 1]} ${y}` : `${d} ${months[m - 1]}`;
  }

  tracksWord(count: number) {
    return this.lang.plural(count, ['utwór', 'utwory', 'utworów'], ['track', 'tracks']);
  }

  compact(value: number) {
    const n = Number(value) || 0;
    if (n >= 1_000_000) {
      return (n / 1_000_000).toFixed(n >= 10_000_000 ? 0 : 1).replace('.0', '') + 'M';
    }
    if (n >= 10_000) {
      return Math.round(n / 1000) + 'K';
    }
    if (n >= 1000) {
      return (n / 1000).toFixed(1).replace('.0', '') + 'K';
    }
    return String(n);
  }

  private load() {
    this.stats.growth(this.range).subscribe(data => {
      this.data = data;
      this.paint();
      this.cdr.detectChanges();
    });
  }

  private paint() {
    const data = this.data;
    if (!data || !data.days?.length) {
      return;
    }
    const days = data.days;
    this.totalTracks = data.tracks;
    this.addedInRange = this.sum(days, 'tracks');
    this.addedThisWeek = this.sum(days.slice(-7), 'tracks');
    this.paintChart(days, data.tracks);
    this.tiles = [
      this.tile('plays', days, ['Odsłuchy', 'Plays']),
      this.tile('visitors', days, ['Odwiedzający', 'Visitors']),
      this.tile('comments', days, ['Komentarze', 'Comments']),
      this.tile('votes', days, ['Oceny', 'Votes']),
      this.tile('users', days, ['Nowe konta', 'New accounts']),
      this.tile('tracks', days, ['Nowe utwory', 'New tracks'])
    ];
  }

  /** Running total of tracks in the library across the selected range. */
  private paintChart(days: GrowthDay[], total: number) {
    const plotW = Math.max(120, this.width - PAD.left - PAD.right);
    const plotH = CHART_H - PAD.top - PAD.bottom;
    let running = total - this.sum(days, 'tracks');
    const totals = days.map(day => running += Number(day.tracks) || 0);
    const max = this.niceMax(Math.max(...totals, 1));
    const step = days.length > 1 ? plotW / (days.length - 1) : 0;
    const yOf = (v: number) => PAD.top + plotH - (v / max) * plotH;

    this.points = days.map((day, i) => ({
      x: PAD.left + i * step,
      y: yOf(totals[i]),
      total: totals[i],
      added: Number(day.tracks) || 0,
      date: day.date
    }));
    this.linePath = this.points.map((p, i) => (i ? 'L' : 'M') + p.x.toFixed(1) + ' ' + p.y.toFixed(1)).join(' ');
    const base = (PAD.top + plotH).toFixed(1);
    const first = this.points[0];
    const last = this.points[this.points.length - 1];
    this.areaPath = this.linePath + ` L${last.x.toFixed(1)} ${base} L${first.x.toFixed(1)} ${base} Z`;

    this.yTicks = [0, 0.5, 1].map(f => ({y: yOf(max * f), label: this.compact(Math.round(max * f))}));
    const mid = Math.floor((days.length - 1) / 2);
    this.xTicks = [
      {x: first.x, label: this.dayLabel(days[0].date), anchor: 'start'},
      {x: this.points[mid].x, label: this.dayLabel(days[mid].date), anchor: 'middle'},
      {x: last.x, label: this.lang.en ? 'today' : 'dziś', anchor: 'end'}
    ];
    if (this.hover) {
      this.hover = this.points.find(p => p.date === this.hover.date) || null;
    }
  }

  private tile(metric: Metric, days: GrowthDay[], label: [string, string]): Tile {
    const values = days.map(day => Number(day[metric]) || 0);
    const allTime = metric === 'visitors' ? null : Number(this.data[metric]) || 0;
    const inRange = values.reduce((a, b) => a + b, 0);
    const week = values.slice(-7).reduce((a, b) => a + b, 0);
    const prevWeek = values.slice(-14, -7).reduce((a, b) => a + b, 0);
    const max = Math.max(...values, 1);
    const gap = values.length > 45 ? 1 : 2;
    const w = Math.max(1, (SPARK_W - gap * (values.length - 1)) / values.length);
    const bars = values.map((v, i) => {
      const h = v > 0 ? Math.max(3, (v / max) * SPARK_H) : 1;
      return {
        x: i * (w + gap),
        y: SPARK_H - h,
        w,
        h,
        last: i === values.length - 1,
        title: this.dayLabel(days[i].date) + ': ' + v
      };
    });
    const change = week - prevWeek;
    const delta = this.lang.en ? `+${week} in 7 days` : `+${week} w 7 dni`;
    const compare = this.lang.en
      ? `Prior 7 days: +${prevWeek}`
      : `Poprzednie 7 dni: +${prevWeek}`;
    return {
      metric,
      label: this.lang.en ? label[1] : label[0],
      value: this.compact(allTime ?? inRange),
      delta,
      compare,
      up: change >= 0,
      bars
    };
  }

  tileScope(tile: Tile) {
    if (tile.metric === 'visitors') {
      return this.lang.en ? `in ${this.range} days` : `w ${this.range} dni`;
    }
    return this.lang.en ? 'all time' : 'łącznie';
  }

  private sum(days: GrowthDay[], key: keyof GrowthDay) {
    return days.reduce((total, day) => total + (Number(day[key]) || 0), 0);
  }

  private niceMax(value: number) {
    const magnitude = Math.pow(10, Math.floor(Math.log10(value)));
    for (const f of [1, 2, 2.5, 5, 10]) {
      if (f * magnitude >= value) {
        return f * magnitude;
      }
    }
    return 10 * magnitude;
  }
}
