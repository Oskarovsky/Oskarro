import {Pipe, PipeTransform} from '@angular/core';
import {rankMark} from './track-label';

@Pipe({
  name: 'rankMark',
  standalone: false,
  pure: true
})
export class RankMarkPipe implements PipeTransform {
  transform(rank: string) {
    return rank ? rankMark(rank) : '';
  }
}
