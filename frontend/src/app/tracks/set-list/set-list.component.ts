import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Subscription} from 'rxjs';
import {RefreshAfterHttpDirective} from '../../services/ui-refresh';
import {TrackService} from '../../services/track/track.service';
import {TokenStorageService} from '../../services/auth/token-storage.service';
import {LangService} from '../../lang.service';
import {SET_CATEGORIES, SetCategoryOption, setCategoryById, setCategoryBySlug} from '../sets';
import {genreLabel} from '../genres';

type Sort = 'new' | 'score' | 'views-month' | 'views-all';

/** "Sety/Mixy": all sets, or one category (/sety/kluby, /sety/festiwale, …). */
@Component({
  selector: 'app-set-list',
  templateUrl: './set-list.component.html',
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class SetListComponent implements OnInit, OnDestroy {
  readonly categories = SET_CATEGORIES;
  readonly pageSize = 10;
  category: SetCategoryOption | null = null;
  sets: any[] = [];
  counts: Record<string, number> = {};
  sort: Sort = 'new';
  page = 1;
  loaded = false;
  isLoggedIn = false;
  private sub?: Subscription;
  private querySub?: Subscription;

  constructor(private trackService: TrackService,
              private route: ActivatedRoute,
              private router: Router,
              tokens: TokenStorageService,
              readonly lang: LangService) {
    this.isLoggedIn = !!tokens.getToken();
  }

  ngOnInit() {
    this.trackService.getSets('').subscribe(all => {
      this.counts = {};
      for (const set of all || []) {
        this.counts[set.setCategory] = (this.counts[set.setCategory] || 0) + 1;
      }
      this.counts['ALL'] = (all || []).length;
    });
    this.sub = this.route.paramMap.subscribe(params => {
      this.category = setCategoryBySlug(params.get('kategoria') || '') || null;
      this.sort = 'new';
      this.page = 1;
      this.load();
    });
    this.querySub = this.route.queryParamMap.subscribe(params => {
      const page = Number(params.get('strona'));
      this.page = Number.isInteger(page) && page > 0 ? page : 1;
    });
  }

  ngOnDestroy() {
    this.sub?.unsubscribe();
    this.querySub?.unsubscribe();
  }

  get title() {
    if (!this.category) {
      return this.lang.t('Sety i mixy', 'Sets and mixes');
    }
    return this.lang.en ? this.category.labelEn : this.category.label;
  }

  get about() {
    if (!this.category) {
      return this.lang.t('Długie sety z klubów i festiwali, audycje radiowe i miksy didżejów z naszej społeczności. Słuchasz w całości, oceniasz i komentujesz.',
        'Long sets from clubs and festivals, radio shows and mixes by DJs from our community. Listen in full, vote and comment.');
    }
    return this.lang.en ? this.category.blurbEn : this.category.blurb;
  }

  get pageCount() {
    return Math.max(1, Math.ceil(this.sets.length / this.pageSize));
  }

  get currentPage() {
    return Math.min(this.page, this.pageCount);
  }

  get pageSets() {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.sets.slice(start, start + this.pageSize);
  }

  get addLink() {
    return this.isLoggedIn ? '/track/add' : '/register';
  }

  get addParams() {
    return this.isLoggedIn ? {typ: 'set', kategoria: this.category?.slug || 'uzytkownicy'} : {};
  }

  label(c: SetCategoryOption) {
    return this.lang.en ? c.labelEn : c.label;
  }

  categoryOf(set: any) {
    const c = setCategoryById(set.setCategory);
    return c ? this.label(c) : '';
  }

  genreName(id: string) {
    return genreLabel(id);
  }

  coverSrc(set: any) {
    return this.trackService.coverUrl(set.id);
  }

  setSort(sort: Sort) {
    if (this.sort !== sort) {
      this.sort = sort;
      this.showPage(1);
      this.load();
    }
  }

  showPage(page: number) {
    this.router.navigate([], {relativeTo: this.route, queryParams: {strona: page > 1 ? page : null}, queryParamsHandling: 'merge'});
    if (page !== this.page) {
      window.scrollTo({top: 0, behavior: 'smooth'});
    }
  }

  private load() {
    this.loaded = false;
    this.trackService.getSets(this.category?.id || '', this.sort).subscribe(sets => {
      this.sets = sets || [];
      this.loaded = true;
    });
  }
}
