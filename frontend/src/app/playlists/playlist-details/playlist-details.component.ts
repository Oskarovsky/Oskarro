import { Component, OnInit } from '@angular/core';
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
import {PlaybackService} from "../../tracks/playback.service";
import {Router} from "@angular/router";

@Component({
  selector: 'app-playlist-details',
  templateUrl: './playlist-details.component.html',
  styleUrls: ['./playlist-details.component.css'],
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class PlaylistDetailsComponent implements OnInit {

  playlist: Playlist;

  tracks: Track[] = [];

  sub: Subscription;

  isLoggedIn = false;

  constructor(private playlistService: PlaylistService,
              private trackService: TrackService,
              private route: ActivatedRoute,
              private tokenStorage: TokenStorageService,
              private seo: SeoService,
              private playback: PlaybackService,
              private router: Router) { }

  playAll() {
    const queue = (this.tracks || []).filter(track => !!audioRef(track?.url));
    if (!queue.length) {
      return;
    }
    this.playback.open(queue[0], queue);
    this.router.navigate(['/track', queue[0].id], {queryParams: {lista: this.playlist?.id}});
  }

  showPlayer(url: string) {
    return this.isLoggedIn && !!audioRef(url);
  }

  get canEdit() {
    const user = this.tokenStorage.getUser();
    return this.isLoggedIn && !!user?.username && user.username === this.playlist?.owner;
  }

  ngOnInit() {
    this.isLoggedIn = !!this.tokenStorage.getToken();
    this.getPlaylistById();
    this.getAllTracksFromPlaylist();
  }

  public getAllTracks() {
    this.trackService.getAllTracks().subscribe(
      result => {
        this.tracks = result;
      },
      error => {
        alert('An error has occurred while downloading tracks')
      }
    );
  }

  public getAllTracksFromPlaylist() {
    this.sub = this.route.params.subscribe(params => {
      const id = params.id;
      if (id) {
        this.playlistService.getAllTracksFromPlaylist(id).subscribe(
          response => {
            this.tracks = response;
          },
          error => {
            alert('An error with fetching tracks has occurred')
          }
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
          error => {
            alert('An error with fetching playlist has occurred')
          });
      }
    });
  }

}
