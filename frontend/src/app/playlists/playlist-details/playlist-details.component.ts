import {Component, OnInit, ChangeDetectorRef, OnDestroy} from '@angular/core';
import {RefreshAfterHttpDirective} from '../../services/ui-refresh';
import {Playlist} from "../playlist/model/playlist";
import {PlaylistService} from "../../services/playlist/playlist.service";
import {Subscription} from "rxjs";
import {ActivatedRoute} from "@angular/router";
import {TrackService} from "../../services/track/track.service";
import {Track} from "../../tracks/track/model/track";
import {audioRef} from "../../services/audio-hosts";
import {TokenStorageService} from "../../services/auth/token-storage.service";
import {SeoService} from "../../seo.service";
import {PlaybackService, controllable} from "../../tracks/playback.service";

@Component({
  selector: 'app-playlist-details',
  templateUrl: './playlist-details.component.html',
  styleUrls: ['./playlist-details.component.css'],
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class PlaylistDetailsComponent implements OnInit, OnDestroy {

  playlist: Playlist;

  tracks: Track[] = [];

  sub: Subscription;

  isLoggedIn = false;

  constructor(private playlistService: PlaylistService,
              private trackService: TrackService,
              private route: ActivatedRoute,
              private tokenStorage: TokenStorageService,
              private seo: SeoService,
              readonly playback: PlaybackService,
              private view: ChangeDetectorRef) { }

  missing = false;
  private playbackSub: Subscription;

  get playable(): Track[] {
    return (this.tracks || []).filter(track => !!audioRef(track?.url) && controllable(track));
  }

  get nowPlayingHere() {
    return !!this.playlist && this.playback.playlist?.id === this.playlist.id && !!this.playback.track;
  }

  canPlay(track: Track) {
    return !!audioRef(track?.url) && controllable(track);
  }

  isCurrent(track: Track) {
    return this.nowPlayingHere && this.playback.track?.id === track.id;
  }

  /** Whole playlist on the bottom bar, in order (or shuffled) — keeps playing while the user browses. */
  playAll(shuffle = false) {
    const queue = this.playable;
    if (!queue.length || !this.playlist) {
      return;
    }
    this.playback.openPlaylist(queue as any, 0, {id: this.playlist.id, name: this.playlist.name}, shuffle);
  }

  playFrom(track: Track) {
    if (this.isCurrent(track)) {
      this.playback.toggle();
      return;
    }
    const queue = this.playable;
    const index = queue.findIndex(item => item.id === track.id);
    this.playback.openPlaylist(queue as any, Math.max(0, index), {id: this.playlist.id, name: this.playlist.name});
  }

  get canEdit() {
    const user = this.tokenStorage.getUser();
    return this.isLoggedIn && !!user?.username && user.username === this.playlist?.owner;
  }

  ngOnDestroy() {
    this.playbackSub?.unsubscribe();
    this.sub?.unsubscribe();
  }

  ngOnInit() {
    this.playbackSub = this.playback.changes.subscribe(() => this.view.detectChanges());
    this.isLoggedIn = !!this.tokenStorage.getToken();
    this.getPlaylistById();
    this.getAllTracksFromPlaylist();
  }


  public getAllTracksFromPlaylist() {
    this.sub = this.route.params.subscribe(params => {
      const id = params.id;
      if (id) {
        this.playlistService.getAllTracksFromPlaylist(id).subscribe(
          response => {
            this.tracks = response;
          },
          () => this.missing = true
        );
      }
    });
  }

  public getPlaylistById() {
    this.sub = this.route.params.subscribe(params => {
      const id = params.id;
      if (id) {
        this.playlistService.getPlaylist(id).subscribe(
          response => {
            this.playlist = response;
            this.seo.playlist(response);
          },
          () => this.missing = true);
      }
    });
  }

}
