import {Pipe, PipeTransform} from '@angular/core';
import {versionLabel} from './track-label';

@Pipe({
  name: 'versionLabel',
  standalone: false,
  pure: true
})
export class VersionLabelPipe implements PipeTransform {
  transform(track: {title?: string, version?: string}) {
    return versionLabel(track);
  }
}
