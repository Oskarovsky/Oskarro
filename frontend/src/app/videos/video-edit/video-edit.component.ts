import { Component, OnInit } from '@angular/core';
import {Track} from '../../tracks/track/model/track';
import {Subscription} from 'rxjs';
import { Video } from '../video/model/video';
import {PlaylistService} from '../../services/playlist/playlist.service';
import {TrackService} from '../../services/track/track.service';
import {ActivatedRoute} from '@angular/router';
import {VideoService} from '../../services/video/video.service';
import {DomSanitizer} from '@angular/platform-browser';

@Component({
  selector: 'app-video-edit',
  templateUrl: './video-edit.component.html',
  styleUrls: ['./video-edit.component.css']
})
export class VideoEditComponent implements OnInit {

  tracks: Track[] = [];
  sub: Subscription;
  video: Video;

  constructor(private videoService: VideoService,
              private trackService: TrackService,
              private sanitizer: DomSanitizer,
              private route: ActivatedRoute) { }

  ngOnInit() {
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

  addTrackToVideo(title: string, artist: string, version: string) {
    const newTrack: Track = {
      id: null,
      title,
      artist,
      points: null,
      genre: '',
      version,
      url: '',
      position: null,
      playlist: null,
      video: this.video
    };

    this.trackService.saveTrackToVideo(newTrack).subscribe(
      response => {
        this.tracks.push(newTrack);
      },
      error => {
        alert('An error with adding track to video has occurred');
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
