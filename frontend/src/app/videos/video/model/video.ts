import {SafeResourceUrl} from '@angular/platform-browser';

export class Video {
  id: number;
  name: string;
  url: string;
  category: string;
  safeUrl: SafeResourceUrl;
}
