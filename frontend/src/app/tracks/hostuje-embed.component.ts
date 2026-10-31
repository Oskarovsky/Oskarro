import {Component, ElementRef, Input, NgZone, OnDestroy, ViewChild} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {DomSanitizer, SafeResourceUrl} from '@angular/platform-browser';
import {environment} from '../../environments/environment';
import {PlaybackService} from './playback.service';

/**
 * hostuje.net only lets its files play inside its own pages, so tracks hosted there use the
 * official "Embed Player html5" in a frame. We cannot control it (no queue, radio or waveform);
 * a click into it counts a play and silences our other players.
 */
@Component({
  selector: 'app-hostuje-embed',
  standalone: false,
  template: `
    <div class="hostuje-embed" *ngIf="src">
      <iframe #frame [src]="src" [title]="'Odtwarzacz hostuje.net' | t:'hostuje.net player'"
              loading="lazy" referrerpolicy="strict-origin-when-cross-origin"></iframe>
      <p class="meta">{{ 'Odtwarzacz hostuje.net — pierwsze kliknięcie może otworzyć ich stronę w nowej karcie, kolejne włącza muzykę.' | t:'hostuje.net player — the first click may open their page in a new tab, the next one starts the music.' }}</p>
    </div>
  `,
  styles: [`
    .hostuje-embed { display: grid; gap: 6px; }
    iframe {
      display: block;
      width: 100%;
      height: 118px;
      border: 1px solid var(--line);
      border-radius: 16px;
      background: #fff;
    }
    .meta { margin: 0; font-size: 0.78rem; }
  `]
})
export class HostujeEmbedComponent implements OnDestroy {

  src: SafeResourceUrl | null = null;
  /** Track to credit with a play when the listener clicks into the player. */
  @Input() trackId: number | null = null;
  @ViewChild('frame') frame?: ElementRef<HTMLIFrameElement>;
  private counted = false;

  private readonly onBlur = () => setTimeout(() => {
    if (!this.frame || document.activeElement !== this.frame.nativeElement) {
      return;
    }
    // One sound at a time: our own players stop when this one is used.
    this.zone.run(() => this.playback.pause());
    document.querySelectorAll('audio, video').forEach(media => (media as HTMLMediaElement).pause());
    if (!this.counted && this.trackId) {
      this.counted = true;
      this.http.post(environment.serverUrl + '/tracks/' + this.trackId + '/view', {}).subscribe({error: () => {}});
    }
  });

  constructor(private sanitizer: DomSanitizer,
              private http: HttpClient,
              private playback: PlaybackService,
              private zone: NgZone) {
    this.zone.runOutsideAngular(() => window.addEventListener('blur', this.onBlur));
  }

  @Input()
  set fileId(value: string) {
    this.counted = false;
    const id = (value || '').toLowerCase();
    // Only a 32-hex hostuje id ever reaches the frame URL.
    this.src = /^[a-f0-9]{32}$/.test(id)
      ? this.sanitizer.bypassSecurityTrustResourceUrl('https://hostuje.net/player_embed.js.php?id=' + id)
      : null;
  }

  ngOnDestroy() {
    window.removeEventListener('blur', this.onBlur);
  }
}
