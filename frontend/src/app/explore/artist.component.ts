import {Component, OnDestroy, OnInit} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {ActivatedRoute} from '@angular/router';
import {Subscription} from 'rxjs';
import {environment} from '../../environments/environment';
import {RefreshAfterHttpDirective} from '../services/ui-refresh';
import {SeoService} from '../seo.service';
import {LangService} from '../lang.service';

/** /wykonawca/:slug — every track by an artist (collaborations included). */
@Component({
  selector: 'app-artist',
  template: `
    <div class="page explore">
      <p class="alert" *ngIf="missing" role="alert">{{ 'Nie ma takiego wykonawcy.' | t:'No such artist.' }} <a routerLink="/">{{ 'Strona główna' | t:'Home' }}</a></p>
      <ng-container *ngIf="page">
        <p class="kicker">{{ 'Wykonawca' | t:'Artist' }}</p>
        <h1 class="display">{{ page.name }}</h1>
        <p class="lede">{{ page.seo.description }}</p>
        <app-explore-tracks [tracks]="page.tracks"></app-explore-tracks>
      </ng-container>
    </div>
  `,
  styleUrls: ['./explore.css'],
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class ArtistComponent implements OnInit, OnDestroy {

  page: any = null;
  missing = false;
  private sub?: Subscription;

  constructor(private http: HttpClient,
              private route: ActivatedRoute,
              private seo: SeoService,
              readonly lang: LangService) {}

  ngOnInit() {
    this.sub = this.route.paramMap.subscribe(params => {
      this.page = null;
      this.missing = false;
      this.http.get<any>(environment.serverUrl + '/explore/artist/' + params.get('slug')).subscribe({
        next: page => {
          this.page = page;
          this.seo.setTitle(page.seo.title, page.seo.description, page.seo.path);
        },
        error: () => this.missing = true
      });
    });
  }

  ngOnDestroy() {
    this.sub?.unsubscribe();
  }
}
