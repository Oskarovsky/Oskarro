import { Component, OnInit } from '@angular/core';
import {RefreshAfterHttpDirective} from '../../services/ui-refresh';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';

import {TrackService} from "../../services/track/track.service";
import {Track} from "./model/track";
import {TokenStorageService} from "../../services/auth/token-storage.service";
import {audioRef} from "../../services/audio-hosts";

@Component({
  selector: 'app-track',
  templateUrl: './track.component.html',
  styleUrls: ['./track.component.css'],
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class TrackComponent implements OnInit {

  track: any = {};
  tracks: Track[] = [];
  comments: Array<{id: number, text: string, author: string, createdAt: string}> = [];
  likes = 0;
  dislikes = 0;
  score = 0;
  mine = 0;
  isLoggedIn = false;
  draft = '';
  genrePath = 'club';
  genreLabel = 'Club';
  coverStamp = '';

  sub: Subscription;

  constructor(private trackService: TrackService,
              private route: ActivatedRoute,
              private tokenStorage: TokenStorageService) { }

  ngOnInit() {
    this.isLoggedIn = !!this.tokenStorage.getToken();
    this.sub = this.route.params.subscribe(params => {
      const id = params.id;
      if (id) {
        this.loadTrack(id);
        this.trackService.getComments(id).subscribe(comments => this.comments = comments || []);
        this.trackService.getLikes(id).subscribe(state => this.applyVote(state));
      }
    });
  }

  loadTrack(id: string) {
    this.trackService.getTrackById(id).subscribe((track: any) => {
      this.track = track;
      this.genrePath = (track.genre || 'club').toLowerCase();
      const names: Record<string, string> = {club: 'Club', dance: 'Dance', house: 'House', retro: 'Retro'};
      this.genreLabel = names[this.genrePath] || track.genre || 'Utwory';
    });
  }

  get playable() {
    return !!audioRef(this.track.url || '');
  }

  get downloadLabel() {
    const ref = audioRef(this.track.url || '');
    if (ref?.host === 'soundfiles') {
      return 'Pobierz z soundfiles';
    }
    return 'Pobierz z hostuje';
  }

  coverSrc() {
    if (!this.track.hasCover) {
      return '';
    }
    const stamp = this.coverStamp ? '?t=' + this.coverStamp : '';
    return this.trackService.coverUrl(this.track.id) + stamp;
  }

  vote(value: number) {
    if (!this.track.id) {
      return;
    }
    this.trackService.vote(this.track.id, value).subscribe(state => {
      this.applyVote(state);
      this.loadTrack(this.track.id);
    });
  }

  addComment() {
    const text = this.draft.trim();
    if (!text || !this.track.id) {
      return;
    }
    this.trackService.addComment(this.track.id, text).subscribe(comment => {
      this.comments = [...this.comments, comment];
      this.draft = '';
      this.loadTrack(this.track.id);
    });
  }

  private applyVote(state) {
    this.likes = state.likes;
    this.dislikes = state.dislikes;
    this.score = state.score;
    this.mine = state.mine;
  }

}
