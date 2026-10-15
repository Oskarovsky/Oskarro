import { Component, OnInit } from '@angular/core';
import {RefreshAfterHttpDirective} from '../../services/ui-refresh';

@Component({
  selector: 'app-board-moderator',
  templateUrl: './board-moderator.component.html',
  styleUrls: ['./board-moderator.component.css'],
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class BoardModeratorComponent implements OnInit {

  constructor() { }

  ngOnInit() {
  }

}
