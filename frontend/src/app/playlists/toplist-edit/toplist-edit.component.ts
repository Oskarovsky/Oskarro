import { Component, OnInit } from '@angular/core';
import {RefreshAfterHttpDirective} from '../../services/ui-refresh';
import {ActivatedRoute} from "@angular/router";
import { Track } from "../../tracks/track/model/track";
import { FormGroup, FormControl } from '@angular/forms';
import {HttpClient} from "@angular/common/http";
import {TrackService} from "../../services/track/track.service";
import {hostujePlayer} from '../../services/hostuje';

@Component({
  selector: 'app-toplist-edit',
  templateUrl: './toplist-edit.component.html',
  styleUrls: ['./toplist-edit.component.css'],
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class ToplistEditComponent implements OnInit {

  model: Track = {
    id: null,
    title: '',
    artist: '',
    points: null,
    genre: '',
    version: '',
    url: '',
    position: null,
    playlist: null,
    video: null
  };


  constructor(private http: HttpClient,
              private trackService: TrackService,
              private route: ActivatedRoute) {

  }

  ngOnInit() {
  }

  addTrackToRanking(): void {
    if (!this.model.artist || !this.model.title) {
      alert('Podaj wykonawcę i tytuł.');
      return;
    }
    if (!hostujePlayer(this.model.url)) {
      alert('Podaj link do pliku na hostuje.net.');
      return;
    }
    this.trackService.addTrackToRanking(this.model).subscribe(
      () => alert('Utwór dodany.'),
      () => alert('Nie udało się dodać utworu.')
    );
  }



}
