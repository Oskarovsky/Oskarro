import {Component, OnInit} from '@angular/core';
import {RefreshAfterHttpDirective} from '../../services/ui-refresh';
import {DomSanitizer, SafeResourceUrl} from '@angular/platform-browser';
import {ActivatedRoute} from '@angular/router';
import {VideoService} from '../../services/video/video.service';

@Component({
  selector: 'app-channel-set',
  templateUrl: './channel-set.component.html',
  styleUrls: ['./channel-set.component.css'],
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class ChannelSetComponent implements OnInit {

  title = 'Set';
  published = '';
  embed: SafeResourceUrl;
  watch = '';

  constructor(private route: ActivatedRoute,
              private videos: VideoService,
              private sanitizer: DomSanitizer) {}

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id') || '';
      if (!/^[A-Za-z0-9_-]{11}$/.test(id)) {
        return;
      }
      this.watch = `https://www.youtube.com/watch?v=${id}`;
      this.embed = this.sanitizer.bypassSecurityTrustResourceUrl(`https://www.youtube-nocookie.com/embed/${id}`);
      this.videos.channel().subscribe(list => {
        const match = (list || []).find(video => video.id === id);
        if (match) {
          this.title = match.title;
          this.published = match.published;
        }
      });
    });
  }
}
