import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { RefreshAfterHttpDirective } from '../services/ui-refresh';

@Component({
  selector: 'app-ranking',
  templateUrl: './ranking.component.html',
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class RankingComponent implements OnInit {

  board: any;

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.http.get(environment.serverUrl + '/profile/ranking').subscribe(board => this.board = board);
  }

  mine(username: string) {
    return !!this.board?.me && this.board.me.username === username;
  }

}
