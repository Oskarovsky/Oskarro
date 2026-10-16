import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {shareReplay, tap} from 'rxjs/operators';
import {environment} from '../../environments/environment';

export interface SiteStats {
  visitorsToday: number;
  users: number;
  tracks: number;
  plays: number;
}

export interface DailyAdds {
  date: string;
  count: number;
}

const VISITOR_KEY = 'oskarro-visitor';
const DAY_KEY = 'oskarro-visit-day';

@Injectable({providedIn: 'root'})
export class StatsService {

  private pending: Observable<SiteStats> | null = null;

  constructor(private http: HttpClient) {}

  added(): Observable<DailyAdds[]> {
    return this.http.get<DailyAdds[]>(environment.serverUrl + '/stats/added');
  }

  load(): Observable<SiteStats> {
    const day = this.warsawDay();
    if (localStorage.getItem(DAY_KEY) === day) {
      return this.http.get<SiteStats>(environment.serverUrl + '/stats');
    }
    if (!this.pending) {
      this.pending = this.http.post<SiteStats>(environment.serverUrl + '/stats/visit', {visitor: this.visitor()}).pipe(
        tap({
          next: () => localStorage.setItem(DAY_KEY, day),
          error: () => this.pending = null
        }),
        shareReplay(1)
      );
    }
    return this.pending;
  }

  private visitor() {
    let id = localStorage.getItem(VISITOR_KEY);
    if (!id || !/^[0-9a-f-]{36}$/i.test(id)) {
      id = crypto.randomUUID();
      localStorage.setItem(VISITOR_KEY, id);
    }
    return id;
  }

  private warsawDay() {
    return new Intl.DateTimeFormat('en-CA', {
      timeZone: 'Europe/Warsaw',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    }).format(new Date());
  }
}
