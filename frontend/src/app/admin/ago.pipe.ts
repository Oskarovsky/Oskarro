import {Pipe, PipeTransform} from '@angular/core';

/** "5 min temu", "3 dni temu", or "—" for empty values. */
@Pipe({name: 'ago', standalone: false, pure: true})
export class AgoPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    if (!value) {
      return '—';
    }
    const seconds = Math.max(0, (Date.now() - Date.parse(value)) / 1000);
    if (seconds < 60) {
      return 'przed chwilą';
    }
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) {
      return minutes + ' min temu';
    }
    const hours = Math.floor(minutes / 60);
    if (hours < 24) {
      return hours + ' godz. temu';
    }
    const days = Math.floor(hours / 24);
    if (days < 30) {
      return days === 1 ? 'wczoraj' : days + ' dni temu';
    }
    return new Date(value).toLocaleDateString('pl-PL', {timeZone: 'Europe/Warsaw'});
  }
}
