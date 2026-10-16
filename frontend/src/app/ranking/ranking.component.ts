import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { RefreshAfterHttpDirective } from '../services/ui-refresh';
import { TrackService } from '../services/track/track.service';
import { LangService } from '../lang.service';

@Component({
  selector: 'app-ranking',
  templateUrl: './ranking.component.html',
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class RankingComponent implements OnInit {

  board: any;
  hits: Array<any> = [];
  hitsReady = false;

  constructor(private http: HttpClient,
              private trackService: TrackService,
              readonly lang: LangService) {}

  ngOnInit() {
    this.http.get(environment.serverUrl + '/profile/ranking').subscribe(board => this.board = board);
    this.trackService.getAllTracks().subscribe(tracks => {
      const weekAgo = Date.now() - 7 * 24 * 60 * 60 * 1000;
      this.hits = (tracks || [])
        .filter(track => track?.genre && track?.url && track?.createdAt && Date.parse(track.createdAt) >= weekAgo)
        .sort((a, b) => (b.score || 0) - (a.score || 0) || (b.id || 0) - (a.id || 0))
        .slice(0, 10);
      this.hitsReady = true;
    });
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
