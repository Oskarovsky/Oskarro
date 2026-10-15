import { Component, OnInit } from '@angular/core';
import {RefreshAfterHttpDirective} from '../../services/ui-refresh';
import {ProviderService} from "../../services/provider/provider.service";

@Component({
  selector: 'app-provider-list',
  templateUrl: './provider-list.component.html',
  styleUrls: ['./provider-list.component.css'],
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class ProviderListComponent implements OnInit {

  providers: Array<any>;

  constructor(private providerService: ProviderService) { }

  ngOnInit() {
    this.providerService.getAllProviders().subscribe(data => {
      this.providers = data;
    })
  }

}
