import { Component, OnInit } from '@angular/core';
import {ProviderService} from "../../services/provider/provider.service";
import { Provider } from '../provider/model/provider';
import {FormControl, FormGroup, Validators} from "@angular/forms";

@Component({
  selector: 'app-add-provider',
  templateUrl: './add-provider.component.html',
  styleUrls: ['./add-provider.component.css']
})
export class AddProviderComponent implements OnInit {

  constructor(private providerService: ProviderService) { }

  provider: Provider = new Provider;
  submitted = false;

  ngOnInit() {
    this.submitted = false;
  }

  providerSaveForm = new FormGroup({
    provider_name: new FormControl('', [Validators.required, Validators.minLength(5)]),
    provider_url: new FormControl('', [Validators.required]),
    provider_description: new FormControl()
  });

  // TODO add save new object

}
