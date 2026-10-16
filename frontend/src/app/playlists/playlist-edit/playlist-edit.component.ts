import { Component, OnInit } from '@angular/core';
import {RefreshAfterHttpDirective} from '../../services/ui-refresh';
import {Playlist} from '../playlist/model/playlist';
import {Track} from '../../tracks/track/model/track';
import {PlaylistService} from '../../services/playlist/playlist.service';
import {TrackService} from '../../services/track/track.service';
import {Subscription} from 'rxjs';
import {ActivatedRoute, Route} from '@angular/router';
import {audioRef, canonicalAudioUrl} from '../../services/audio-hosts';
import {TokenStorageService} from '../../services/auth/token-storage.service';
import {LangService} from '../../lang.service';

@Component({
  selector: 'app-playlist-edit',
  templateUrl: './playlist-edit.component.html',
  styleUrls: ['./playlist-edit.component.css'],
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class PlaylistEditComponent implements OnInit {

  tracks: Track[] = [];

  sub: Subscription;

  playlist: Playlist;

  playlist_id;

  draft = {artist: '', title: '', version: '', url: '', source: 'hostuje'};

  isLoggedIn = false;

  constructor(private playlistService: PlaylistService,
              private trackService: TrackService,
              private route: ActivatedRoute,
              private tokenStorage: TokenStorageService,
              private lang: LangService) { }

  ngOnInit() {
    this.isLoggedIn = !!this.tokenStorage.getToken();
    this.getPlaylistById();
    this.getAllTracksFromPlaylist();
  }
  getPlaylist() {
    this.sub = this.route.params.subscribe(params => {
      const id = params.id;
      this.playlist_id = id;
      if (id) {
        this.playlistService.getPlaylist(id).subscribe((playlist: any) => {
          this.playlist = playlist;
        });
      }
    });
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
            alert('An error with fetching tracks has occurred');
          }
        );
      }
    });
  }

  public updateTrack(updatedTrack: Track) {
    const url = (updatedTrack.url || '').trim();
    if (url) {
      const canonical = canonicalAudioUrl(url);
      if (!canonical) {
        alert(this.lang.t('Podaj link z hostuje.net albo soundfiles.eu.', 'Enter a link from hostuje.net or soundfiles.eu.'));
        return;
      }
      updatedTrack.url = canonical;
    }
    this.trackService.saveTrackToPlaylist(updatedTrack).subscribe(
      response => {
      },
      error => {
        alert('An error with updating tracks has occurred');
      }
    );
  }

  updatePlaylist(updatedPlaylist: Playlist) {
    updatedPlaylist.id = this.playlist_id;
    this.playlistService.addPlaylist(updatedPlaylist).subscribe(
      result => {

      },
      error => {
        alert('An error with updating playlists has occurred');
      }
    );
  }

  public getPlaylistById() {
    this.sub = this.route.params.subscribe(params => {
      const id = params.id;
      if (id) {
        this.playlistService.getPlaylist(id).subscribe(
          response => {
            this.playlist = response;
          },
          error => {
            alert('An error with fetching playlist has occurred');
          });
      }
    });
  }

  deleteTrack(id: number) {
    if (confirm(this.lang.t('Usunąć ten utwór ze składanki?', 'Remove this track from the playlist?'))) {
      this.playlistService.unpin(this.playlist.id, id).subscribe(
        () => {
          this.tracks = this.tracks.filter(track => track.id !== id);
        },
        () => {
          alert(this.lang.t('Nie udało się usunąć utworu ze składanki', 'Could not remove the track from the playlist'));
        }
      );
    }
  }

  showPlayer(url: string) {
    return this.isLoggedIn && !!audioRef(url);
  }

  addTrackToPlaylist() {
    const artist = this.draft.artist.trim();
    const title = this.draft.title.trim();
    const version = this.draft.version.trim();
    const raw = this.draft.url.trim();
    const ref = audioRef(raw);
    if (!artist || !title) {
      alert(this.lang.t('Podaj wykonawcę i tytuł.', 'Enter the artist and the title.'));
      return;
    }
    if (!ref || ref.host !== this.draft.source) {
      alert(this.draft.source === 'hostuje'
        ? this.lang.t('Podaj link z hostuje.net albo zmień źródło na soundfiles.eu.', 'Enter a hostuje.net link, or switch the source to soundfiles.eu.')
        : this.lang.t('Podaj link z soundfiles.eu albo zmień źródło na hostuje.net.', 'Enter a soundfiles.eu link, or switch the source to hostuje.net.'));
      return;
    }
    const url = canonicalAudioUrl(raw);
    const newTrack: Track = {
      id: null,
      title,
      artist,
      points: null,
      genre: '',
      version,
      url,
      position: null,
      playlist: this.playlist,
      video: null
    };

    this.trackService.saveTrackToPlaylist(newTrack).subscribe(
      response => {
        this.tracks.push(response && response.id ? response : newTrack);
        this.draft = {artist: '', title: '', version: '', url: '', source: this.draft.source};
      },
      error => {
        alert(this.lang.t('Nie udało się dodać utworu.', 'The track could not be added.'));
      }
    );
  }
}
