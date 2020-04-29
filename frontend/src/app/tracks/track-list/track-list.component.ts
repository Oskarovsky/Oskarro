import { Component, OnInit } from '@angular/core';
import { TrackService} from "../../services/track/track.service";
import {Subscription} from "rxjs";
import {ProviderService} from "../../services/provider/provider.service";
import {ActivatedRoute} from "@angular/router";

@Component({
  selector: 'app-track-list',
  templateUrl: './track-list.component.html',
  styleUrls: ['./track-list.component.css']
})
export class TrackListComponent implements OnInit {

  tracks: Array<any>;

  genres: Array<any>;

  sub: Subscription;

  constructor(private trackService: TrackService,
              private providerService: ProviderService,
              private route: ActivatedRoute) {
  }

  ngOnInit() {
    this.sub = this.route.params.subscribe(params => {
      const id = params.id;
      const genre = params.genre;
      const providerName = params.providerName;
      if (id && !genre) {
        this.trackService.getTracksByProviderId(id).subscribe((track: any) => {
          this.tracks = track;
        });
      } else if (id && genre) {
        this.trackService.getTracksFromProviderByGenre(id, genre).subscribe((track: any) => {
          this.tracks = track;
        })
      } else if (!id && genre) {
        this.trackService.getTracksByGenre(genre).subscribe((track: any) => {
          this.tracks = track;
        })
      } else if (providerName && !id && !genre) {
        this.trackService.getTracksByProviderName(providerName).subscribe((track: any) => {
          this.tracks = track;
        })
      }
    })
  }
}
