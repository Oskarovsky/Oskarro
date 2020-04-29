import { Component, OnInit } from '@angular/core';
import { Video } from '../video/model/video';
import { Track } from '../../tracks/track/model/track';
import {VideoService} from '../../services/video/video.service';
import {Playlist} from '../../playlists/playlist/model/playlist';

@Component({
  selector: 'app-video-add',
  templateUrl: './video-add.component.html',
  styleUrls: ['./video-add.component.css']
})
export class VideoAddComponent implements OnInit {

  videos: Video[];

  tracks: Track[];

  categories = ['RETRO', 'LUNA_MIX', 'MIX', 'PSYCHO_PATH', 'OTHER'];

  modelVideo: Video = {
    id: null,
    name: '',
    url: '',
    category: '',
    safeUrl: null,
  };

  constructor(private videoService: VideoService) {
  }

  ngOnInit() {
    this.getAllVideo();
  }

  public getAllVideo() {
    this.videoService.getAllVideos().subscribe(
      response => {
        this.videos = response;
      },
      error => {
        alert('An error with fetching videos has occurred');
      }
    );
  }

  createVideo(name: string, category: string, url: string) {
    const newVideo: Video = {
      id: null,
      url,
      name,
      category,
      safeUrl: null,
    };
    this.videoService.addVideo(newVideo).subscribe(
      result => {
        newVideo.name = name;
        this.videos.push(newVideo);
      },
      error => {
        alert('An error has occurred while saving the video');
      }
    );
  }
}
