import {Component, Input} from '@angular/core';
import {scoreTag, scoreText, scoreTone} from '../track-label';

@Component({
  selector: 'app-score',
  templateUrl: './score-badge.component.html',
  standalone: false
})
export class ScoreBadgeComponent {
  tone: 'down' | 'flat' | 'up' | 'hot' | 'anthem' = 'flat';
  label = '0';
  tag = '';

  @Input()
  set value(score: number) {
    this.tone = scoreTone(score);
    this.label = scoreText(score);
    this.tag = scoreTag(score);
  }
}
