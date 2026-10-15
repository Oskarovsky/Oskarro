import { Component, OnInit } from '@angular/core';
import {RefreshAfterHttpDirective} from '../../services/ui-refresh';
import {ActivatedRoute} from "@angular/router";

@Component({
  selector: 'app-toplist',
  templateUrl: './toplist.component.html',
  styleUrls: ['./toplist.component.css'],
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class ToplistComponent implements OnInit {

  constructor(private route: ActivatedRoute) { }

  ngOnInit() {
  }

}
