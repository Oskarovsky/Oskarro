import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {ProviderService} from "../../services/provider/provider.service";
import {TrackService} from "../../services/track/track.service";
import {Location} from "@angular/common";
import {Subscription} from "rxjs";
import {TokenStorageService} from '../../services/auth/token-storage.service';

@Component({
  selector: 'app-provider-details',
  templateUrl: './provider-details.component.html',
  styleUrls: ['./provider-details.component.css']
})
export class ProviderDetailsComponent implements OnInit {

  provider: any;

  genres: Array<any>;

  sub: Subscription;

  tracks: Array<any>;

  private roles: string[];
  isLoggedIn = false;
  showAdminBoard = false;
  showModeratorBoard = false;
  username: string;

  constructor(private providerService: ProviderService,
              private trackService: TrackService,
              private route: ActivatedRoute,
              private location: Location,
              private router: Router,
              private tokenStorageService: TokenStorageService) { }


  ngOnInit() {
    this.isLoggedIn = !!this.tokenStorageService.getToken();

    if (this.isLoggedIn) {
      const user = this.tokenStorageService.getUser();
      this.roles = user.roles;

      this.showAdminBoard = this.roles.includes('ROLE_ADMIN');
      this.showModeratorBoard = this.roles.includes('ROLE_MODERATOR');

      this.username = user.username;
    }

    this.sub = this.route.params.subscribe(params => {
      const id = params.id;
      if (id) {
        this.providerService.getProvider(id).subscribe(data => {
          this.provider = data;
        });
        this.providerService.getAllGenresFromProvider(id).subscribe((genre: any) => {
          this.genres = genre;
        });
        this.trackService.getTracksByProviderId(id).subscribe((track: any) => {
          this.tracks = track;
        })
      }
    })
  }

  getAllTracksFromProviderByGenre(id: string, genre: string) {
    this.trackService.getTracksFromProviderByGenre(id, genre).subscribe((data: any) => {
      this.tracks = data;
    })
  }


}
