import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Track } from '../../tracks/track/model/track';
import {Observable} from 'rxjs';
import {Playlist} from '../../playlists/playlist/model/playlist';
import {Video} from '../../videos/video/model/video';
import {DomSanitizer} from '@angular/platform-browser';
import {environment} from '../../../environments/environment';

const API: string = environment.serverUrl;
const VIDEO_API = API + '/video';

@Injectable({providedIn: 'root'})
export class VideoService {

  tracks: Track[] = [];

  httpOptions = {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' })
  };

  constructor(private http: HttpClient,
              private sanitizer: DomSanitizer) { }

  /** GET all video */
  getAllVideos(): Observable<Video[]> {
    return this.http.get<Video[]>(VIDEO_API + '/findAll');
  }

  /** GET video by id */
  getVideo(id: string): Observable<any> {
    return this.http.get<Video>(VIDEO_API + '/' + id);
  }

  addVideo(video: Video): Observable<Video> {
    return this.http.post<Video>(VIDEO_API  + '/add', video);
  }

  getVideosByCategory(category: string): Observable<Video[]> {
    return this.http.get<Video[]>(VIDEO_API + '/findAll/' + category);
  }

  deleteVideo(id: number): Observable<any> {
    return this.http.delete<Video>(VIDEO_API + '/' + id);
  }

  /** GET all tracks from Video */
  getAllTracksFromVideo(videoId: string): Observable<Track[]> {
    return this.http.get<Track[]>(VIDEO_API + '/' + videoId + '/tracks');
  }

}
