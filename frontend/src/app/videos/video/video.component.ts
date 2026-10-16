import {Component, OnInit} from '@angular/core';
import {RefreshAfterHttpDirective} from '../../services/ui-refresh';
import {VideoService} from '../../services/video/video.service';

export interface ChannelVideo {
  id: string;
  title: string;
  duration: string;
  published: string;
}

@Component({
  selector: 'app-video',
  templateUrl: './video.component.html',
  styleUrls: ['./video.component.css'],
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class VideoComponent implements OnInit {

  videos: ChannelVideo[] = [];
  failed = false;

  constructor(private videoService: VideoService) {}

  ngOnInit() {
    this.videoService.channel().subscribe({
      next: videos => this.videos = videos || [],
      error: () => this.failed = true
    });
  }

  thumb(id: string) {
    return `https://i.ytimg.com/vi/${id}/hqdefault.jpg`;
  }
}
