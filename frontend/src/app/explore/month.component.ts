import {Component, OnDestroy, OnInit} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {ActivatedRoute, Router} from '@angular/router';
import {Subscription} from 'rxjs';
import {environment} from '../../environments/environment';
import {RefreshAfterHttpDirective} from '../services/ui-refresh';
import {SeoService} from '../seo.service';
import {LangService} from '../lang.service';

/** /nowosci/:genre — months with new tracks; /nowosci/:genre/:month — that month's tracks and top 10. */
@Component({
  selector: 'app-month',
  templateUrl: './month.component.html',
  styleUrls: ['./explore.css'],
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class MonthComponent implements OnInit, OnDestroy {

  genre = '';
  info: any = null;
  page: any = null;
  missing = false;
  private sub?: Subscription;

  constructor(private http: HttpClient,
              private route: ActivatedRoute,
              private router: Router,
              private seo: SeoService,
              readonly lang: LangService) {}

  ngOnInit() {
    this.sub = this.route.paramMap.subscribe(params => {
      this.genre = params.get('genre') || '';
      const month = params.get('month');
      this.missing = false;
      this.page = null;
      this.info = null;
      if (month) {
        this.http.get<any>(environment.serverUrl + '/explore/month/' + this.genre + '/' + month).subscribe({
          next: page => {
            this.page = page;
            this.seo.setTitle(page.seo.title, page.seo.description, page.seo.path);
          },
          error: () => this.missing = true
        });
      } else {
        this.http.get<any>(environment.serverUrl + '/explore/genre/' + this.genre).subscribe({
          next: info => {
            this.info = info;
            const year = new Date().getFullYear();
            this.seo.setTitle('Nowości ' + info.label + ' ' + year + ' – miesiąc po miesiącu, mp3 | VIXA',
              'Nowe numery ' + info.label + ' z każdego miesiąca: listy nowości, top 10 miesiąca i ranking. Słuchaj za darmo i pobieraj mp3 na VIXA.',
              '/nowosci/' + this.genre);
          },
          error: () => this.missing = true
        });
      }
    });
  }

  ngOnDestroy() {
    this.sub?.unsubscribe();
  }

  trackWord(n: number) {
    return this.lang.plural(n || 0, ['utwór', 'utwory', 'utworów'], ['track', 'tracks']);
  }
}
