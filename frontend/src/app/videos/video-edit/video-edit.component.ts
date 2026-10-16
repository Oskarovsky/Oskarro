import { Component, OnInit } from '@angular/core';
import {RefreshAfterHttpDirective} from '../../services/ui-refresh';
import {Track} from '../../tracks/track/model/track';
import {Subscription} from 'rxjs';
import { Video } from '../video/model/video';
import {PlaylistService} from '../../services/playlist/playlist.service';
import {TrackService} from '../../services/track/track.service';
import {ActivatedRoute} from '@angular/router';
import {VideoService} from '../../services/video/video.service';
import {DomSanitizer} from '@angular/platform-browser';
import {hostujePlayer} from '../../services/hostuje';
import {TokenStorageService} from '../../services/auth/token-storage.service';

@Component({
  selector: 'app-video-edit',
  templateUrl: './video-edit.component.html',
  styleUrls: ['./video-edit.component.css'],
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class VideoEditComponent implements OnInit {

  tracks: Track[] = [];
  sub: Subscription;
  video: Video;
  draft = {artist: '', title: '', version: '', url: ''};

  isLoggedIn = false;

  constructor(private videoService: VideoService,
              private trackService: TrackService,
              private sanitizer: DomSanitizer,
              private route: ActivatedRoute,
              private tokenStorage: TokenStorageService) { }

  ngOnInit() {
    this.isLoggedIn = !!this.tokenStorage.getToken();
    this.getVideoById();
    this.getAllTracksFromVideo();
  }

  public getVideoById() {
    this.sub = this.route.params.subscribe(params => {
      const id = params.id;
      if (id) {
        this.videoService.getVideo(id).subscribe(
          response => {
            this.video = response;
            this.secureAllUrl(this.video);
          },
          error => {
            alert('An error with fetching video has occurred');
          });
      }
    });
  }

  public getAllTracksFromVideo() {
    this.sub = this.route.params.subscribe(params => {
      const id = params.id;
      if (id) {
        this.videoService.getAllTracksFromVideo(id).subscribe(
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
    this.trackService.saveTrackToVideo(updatedTrack).subscribe(
      response => {
      },
      error => {
        alert('An error with updating tracks has occurred');
      }
    );
  }

  showPlayer(url: string) {
    return this.isLoggedIn && !!hostujePlayer(url);
  }

  addTrackToVideo() {
    const artist = this.draft.artist.trim();
    const title = this.draft.title.trim();
    const version = this.draft.version.trim();
    const url = this.draft.url.trim();
    if (!artist || !title) {
      alert('Podaj wykonawcę i tytuł.');
      return;
    }
    if (!hostujePlayer(url)) {
      alert('Podaj link do pliku na hostuje.net.');
      return;
    }
    const newTrack: Track = {
      id: null,
      title,
      artist,
      points: null,
      genre: '',
      version,
      url,
      position: null,
      playlist: null,
      video: this.video
    };

    this.trackService.saveTrackToVideo(newTrack).subscribe(
      response => {
        this.tracks.push(response && response.id ? response : newTrack);
        this.draft = {artist: '', title: '', version: '', url: ''};
      },
      error => {
        alert('Nie udało się dodać utworu.');
      }
    );
  }

  deleteTrack(id: number) {
    if (confirm('Czy na pewno chcesz usunąć ten utwór?')) {
      this.trackService.deleteTrackFromPlaylist(id).subscribe(
        response => {
          this.tracks.splice(Number(id), 1);
          window.location.reload();
        },
        error => {
          alert('An error has occurred while deleting tracks from playlist');
        }
      );
    }
  }

  secureAllUrl(video: Video) {
    video.safeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(`https://www.youtube.com/embed/${video.url}`);
  }

}
