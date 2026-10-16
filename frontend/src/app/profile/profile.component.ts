import { Component, OnInit } from '@angular/core';
import {RefreshAfterHttpDirective} from '../services/ui-refresh';
import { TokenStorageService } from '../services/auth/token-storage.service';
import {UploadFileService} from '../services/storage/upload-file.service';
import { HttpEventType, HttpResponse, HttpClient } from '@angular/common/http';
import {ActivatedRoute} from '@angular/router';
import {environment} from '../../environments/environment';
import {TrackService} from '../services/track/track.service';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css'],
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class ProfileComponent implements OnInit {

  profile: any;
  self = false;
  bio = '';
  grantTitle = '';
  grantUser = '';
  message = '';
  imageToShow: any;
  selectedFile: File = null;
  missing = false;
  loading = false;

  constructor(private token: TokenStorageService,
              private uploadService: UploadFileService,
              private http: HttpClient,
              private route: ActivatedRoute,
              private trackService: TrackService) { }

  ngOnInit() {
    this.route.params.subscribe(params => this.load(params.username));
  }

  cover(track) {
    return this.trackService.coverUrl(track.id);
  }

  saveBio() {
    this.http.put<any>(environment.serverUrl + '/profile/me', {bio: this.bio}).subscribe({
      next: profile => {
        this.profile = profile;
        this.message = 'Zapisane.';
      },
      error: () => this.message = 'Nie udało się zapisać opisu.'
    });
  }

  grant() {
    const username = (this.grantUser || this.profile?.username || '').trim();
    if (!username) {
      return;
    }
    this.http.put<any>(environment.serverUrl + '/profile/' + encodeURIComponent(username) + '/rank', {title: this.grantTitle})
      .subscribe({
        next: profile => {
          this.grantUser = '';
          if (profile.username === this.profile?.username) {
            this.profile = profile;
            this.grantTitle = profile.granted ? profile.rank : '';
          }
          this.message = profile.granted ? 'Ranga przyznana.' : 'Ranga wróciła do aktywności.';
        },
        error: () => this.message = 'Nie udało się zmienić rangi.'
      });
  }

  selectFile(event) {
    this.selectedFile = event.target.files && event.target.files[0];
  }

  upload() {
    if (!this.selectedFile || !this.profile) {
      return;
    }
    this.uploadService.upload(this.selectedFile, this.profile.username).subscribe({
      next: event => {
        if (event.type === HttpEventType.Response || event instanceof HttpResponse) {
          this.message = 'Zdjęcie zapisane.';
          this.getImageFromService();
        }
      },
      error: () => this.message = 'Nie udało się wgrać zdjęcia.'
    });
    this.selectedFile = null;
  }

  private load(username: string) {
    this.profile = null;
    this.missing = false;
    this.loading = true;
    this.imageToShow = null;
    this.message = '';
    const logged = this.token.getUser();
    this.self = !!this.token.getToken() && (!username || username === logged?.username);
    const url = username
      ? environment.serverUrl + '/profile/' + encodeURIComponent(username)
      : environment.serverUrl + '/profile/me';
    if (!username && !this.token.getToken()) {
      this.loading = false;
      return;
    }
    this.http.get(url).subscribe({
      next: (profile: any) => {
        this.loading = false;
        this.profile = profile;
        this.bio = profile.bio || '';
        this.grantTitle = profile.granted ? profile.rank : '';
        this.self = !!logged && logged.username === profile.username;
        this.getImageFromService();
      },
      error: () => {
        this.loading = false;
        this.missing = true;
      }
    });
  }

  private getImageFromService() {
    if (!this.profile) {
      return;
    }
    this.uploadService.getFile(this.profile.username).subscribe({
      next: data => {
        const reader = new FileReader();
        reader.addEventListener('load', () => this.imageToShow = reader.result);
        reader.readAsDataURL(data);
      },
      error: () => this.imageToShow = null
    });
  }

}
