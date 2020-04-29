import {Component, OnInit, ViewChild} from '@angular/core';
import {VideoService} from '../../services/video/video.service';
import {Video} from './model/video';
import {Track} from '../../tracks/track/model/track';
import { DomSanitizer } from '@angular/platform-browser';
import {Subscription} from 'rxjs';
import {ActivatedRoute} from '@angular/router';
import {NavigationComponent} from '../../navigation/navigation.component';

@Component({
  selector: 'app-video',
  templateUrl: './video.component.html',
  styleUrls: ['./video.component.css']
})
export class VideoComponent implements OnInit {

  videos: Video[];
  tracks: Track[];
  searchText: string;
  safeVideoUrl: any;
  sub: Subscription;
  videoCategory: string;
  isLoggedIn = false;

  constructor(private videoService: VideoService,
              private sanitizer: DomSanitizer,
              private route: ActivatedRoute) {}

  ngOnInit() {
    this.getVideosByCategory();
  }

  public getAllVideo() {
    this.videoService.getAllVideos().subscribe(
      response => {
        this.videos = response;
        this.secureAllUrl(this.videos);
      },
      error => {
        alert('An error with fetching videos has occurred');
      }
    );
  }

  getVideosByCategory() {
    this.sub = this.route.params.subscribe(params => {
      const category = params.category;
      this.videoCategory = category;
      if (category) {
        this.videoService.getVideosByCategory(category).subscribe(
          response => {
            this.videos = response;
            this.secureAllUrl(this.videos);
          },
          error => {
            alert('An error with fetching videos has occurred');
          }
        );
      }
    });
  }


  secureAllUrl(allVideos: Video[]) {
    for (const video of allVideos) {
      video.safeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(`https://www.youtube.com/embed/${video.url}`);
    }
  }

}
