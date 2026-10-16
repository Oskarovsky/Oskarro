import {AfterViewChecked, ChangeDetectorRef, Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Subscription} from 'rxjs';
import {DailyAdds, StatsService} from '../services/stats.service';
import {LangService} from '../lang.service';
import {RefreshAfterHttpDirective} from '../services/ui-refresh';

interface DayBar {
  date: string;
  count: number;
  barPx: number;
  label: string;
  aria: string;
  today: boolean;
  yesterday: boolean;
}

const MONTHS_EN = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
const BAR_MAX = 104;

@Component({
  selector: 'app-activity-chart',
  templateUrl: './activity-chart.component.html',
  styleUrls: ['./activity-chart.component.css'],
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class ActivityChartComponent implements OnInit, OnDestroy, AfterViewChecked {
  days: DayBar[] = [];
  today: DayBar | null = null;
  yesterday: DayBar | null = null;

  @ViewChild('scroller') scroller?: ElementRef<HTMLElement>;

  private rows: DailyAdds[] = [];
  private langSub?: Subscription;
  private pinned = false;

  constructor(private stats: StatsService,
              readonly lang: LangService,
              private cdr: ChangeDetectorRef) {}

  ngOnInit() {
    this.langSub = this.lang.changes.subscribe(() => this.paint());
    this.stats.added().subscribe(rows => {
      this.rows = Array.isArray(rows) ? rows : [];
      this.pinned = false;
      this.paint();
      this.cdr.detectChanges();
    });
  }

  ngOnDestroy() {
    this.langSub?.unsubscribe();
  }

  ngAfterViewChecked() {
    if (this.pinned || !this.days.length) {
      return;
    }
    const el = this.scroller?.nativeElement;
    if (!el) {
      return;
    }
    el.scrollLeft = el.scrollWidth;
    this.pinned = true;
  }

  private paint() {
    const max = this.rows.reduce((highest, row) => Math.max(highest, Number(row.count) || 0), 0);
    const last = this.rows.length - 1;
    this.days = this.rows.map((row, index) => {
      const count = Number(row.count) || 0;
      const today = index === last;
      const yesterday = index === last - 1;
      const label = this.label(row.date, today, yesterday);
      const word = this.lang.plural(count, ['utwór', 'utwory', 'utworów'], ['track', 'tracks']);
      return {
        date: row.date,
        count,
        barPx: count <= 0 || max <= 0 ? 3 : Math.max(8, Math.round((count / max) * BAR_MAX)),
        label,
        aria: `${label}: ${count} ${word}`,
        today,
        yesterday
      };
    });
    this.today = this.days.length ? this.days[this.days.length - 1] : null;
    this.yesterday = this.days.length > 1 ? this.days[this.days.length - 2] : null;
  }

  private label(date: string, today: boolean, yesterday: boolean): string {
    if (today) {
      return this.lang.t('dziś', 'today');
    }
    if (yesterday) {
      return this.lang.t('wczoraj', 'yesterday');
    }
    const parts = (date || '').split('-');
    const month = Number(parts[1]);
    const day = Number(parts[2]);
    if (!month || !day) {
      return date;
    }
    if (this.lang.en) {
      return `${MONTHS_EN[month - 1]} ${day}`;
    }
    return `${day}.${String(month).padStart(2, '0')}`;
  }
}
