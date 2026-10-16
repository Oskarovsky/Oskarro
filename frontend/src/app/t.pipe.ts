import {Pipe, PipeTransform} from '@angular/core';
import {LangService} from './lang.service';

@Pipe({
  name: 't',
  pure: false,
  standalone: false
})
export class TPipe implements PipeTransform {
  constructor(private lang: LangService) {}

  transform(value: string, en?: string): string {
    if (en != null) {
      return this.lang.t(value, en);
    }
    return this.lang.text(value);
  }
}
