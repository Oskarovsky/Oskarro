import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';

import {TrackService} from "../../services/track/track.service";
import {HttpClient} from "@angular/common/http";
import {Track} from "./model/track";
import {environment} from '../../../environments/environment';

const API: string = environment.serverUrl;
const VIDEO_API = API + '/video';
const TRACK_API = API + '/tracks';
const PROVIDER_API = API + '/providers';

@Component({
  selector: 'app-track',
  templateUrl: './track.component.html',
  styleUrls: ['./track.component.css']
})
export class TrackComponent implements OnInit {

  track: any = {};
  tracks: Track[] = [];

  sub: Subscription;

  constructor(private trackService: TrackService,
              private route: ActivatedRoute,
              private router: Router,
              private http: HttpClient) { }

  ngOnInit() {
    this.sub = this.route.params.subscribe(params => {
      const id = params.id;
      if (id) {
        this.trackService.getTrackById(id).subscribe((track: any) => {
          this.track = track;
        });
      }
    });
  }

  public getAllTracks() {
    this.trackService.getAllTracks().subscribe(
      result => {
        this.tracks = result;
      },
      error => {
        alert('An error has occurred while downloading tracks')
      }
    );
  }

  public getTrackById(id: string) {
    this.trackService.getTrackById(id).subscribe(
    );
  }

}
