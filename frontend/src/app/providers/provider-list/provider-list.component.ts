import { Component, OnInit } from '@angular/core';
import {ProviderService} from "../../services/provider/provider.service";

@Component({
  selector: 'app-provider-list',
  templateUrl: './provider-list.component.html',
  styleUrls: ['./provider-list.component.css']
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
