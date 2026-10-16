import {Component, HostListener, OnDestroy, OnInit} from '@angular/core';
import {RefreshAfterHttpDirective} from '../../services/ui-refresh';
import {ActivatedRoute, Router} from '@angular/router';
import {Subscription} from 'rxjs';
import {VideoService} from '../../services/video/video.service';

export interface ChannelVideo {
  id: string;
  title: string;
  duration: string;
  published: string;
  views: string;
}

@Component({
  selector: 'app-video',
  templateUrl: './video.component.html',
  styleUrls: ['./video.component.css'],
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class VideoComponent implements OnInit, OnDestroy {

  videos: ChannelVideo[] = [];
  failed = false;
  page = 1;
  pageSize = 9;
  private querySub: Subscription;

  constructor(private videoService: VideoService,
              private route: ActivatedRoute,
              private router: Router) {}

  ngOnInit() {
    this.fit();
    this.page = this.readPage(this.route.snapshot.queryParamMap.get('strona'));
    this.querySub = this.route.queryParamMap.subscribe(params => {
      this.page = this.readPage(params.get('strona'));
    });
    this.videoService.channel().subscribe({
      next: videos => this.videos = videos || [],
      error: () => this.failed = true
    });
  }

  ngOnDestroy() {
    this.querySub?.unsubscribe();
  }

  @HostListener('window:resize')
  fit() {
    const width = Math.min(1180, Math.max(280, window.innerWidth - 40));
    const columns = window.innerWidth <= 860
      ? 1
      : Math.max(1, Math.floor((width + 18) / (340 + 18)));
    const rows = columns === 1 ? 6 : 3;
    this.pageSize = columns * rows;
  }

  showPage(page: number) {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {strona: page > 1 ? page : null},
      queryParamsHandling: 'merge'
    });
    window.scrollTo({top: 0, behavior: 'smooth'});
  }

  get pageCount() {
    return Math.max(1, Math.ceil(this.videos.length / this.pageSize));
  }

  get currentPage() {
    return Math.min(this.page, this.pageCount);
  }

  get pageVideos() {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.videos.slice(start, start + this.pageSize);
  }

  thumb(id: string) {
    return `https://i.ytimg.com/vi/${id}/hq720.jpg`;
  }

  fallbackThumb(event: Event) {
    const img = event.target as HTMLImageElement;
    if (img && !img.src.includes('hqdefault')) {
      img.src = img.src.replace('hq720.jpg', 'hqdefault.jpg');
    }
  }

  private readPage(value: string) {
    const page = Number(value);
    return Number.isInteger(page) && page > 0 ? page : 1;
  }
}
