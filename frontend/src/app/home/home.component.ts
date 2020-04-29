import { Component, OnInit } from '@angular/core';
import { UserService } from '../services/user/user.service';
import {Playlist} from '../playlists/playlist/model/playlist';
import {Subscription} from 'rxjs';
import {TokenStorageService} from '../services/auth/token-storage.service';
import {ActivatedRoute} from '@angular/router';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {

  sub: Subscription;
  isLoggedIn = false;
  currentUser: any;

  constructor(private userService: UserService,
              private tokenStorage: TokenStorageService,
              private route: ActivatedRoute) { }

  ngOnInit() {
    if (this.tokenStorage.getToken()) {
      this.isLoggedIn = true;
      this.currentUser = this.tokenStorage.getUser();
    }
  }

}
