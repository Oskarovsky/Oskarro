import { Pipe, PipeTransform } from '@angular/core';
import { Video } from '../../videos/video/model/video';

@Pipe({
  name: 'videoFilter'
})
export class VideoFilterPipe implements PipeTransform {

  transform(videos: Video[], text: string): Video[] {
    if (text == null || text === '') {
      return videos;
    }
    return videos.filter(n => n.name.includes(text));
  }

}
