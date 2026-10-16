import {AfterViewChecked, ChangeDetectorRef, Component, ElementRef, Input, OnDestroy, ViewChild} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {audioRef} from '../../services/audio-hosts';
import {TokenStorageService} from '../../services/auth/token-storage.service';
import {RefreshAfterHttpDirective} from '../../services/ui-refresh';
import {environment} from '../../../environments/environment';

@Component({
  selector: 'app-track-player',
  templateUrl: './kraken-player.component.html',
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class TrackPlayerComponent implements AfterViewChecked, OnDestroy {
  audioUrl: string = null;
  failed = false;
  playing = false;
  current = 0;
  duration = 0;
  peaks: number[] = [];

  @ViewChild('audio') audio?: ElementRef<HTMLAudioElement>;
  @ViewChild('canvas') canvas?: ElementRef<HTMLCanvasElement>;

  private needsDraw = false;
  private scrubbing = false;
  private watched: HTMLCanvasElement = null;
  private resize: ResizeObserver = null;
  private generation = 0;
  private sourceTries = 0;
  private mediaTries = 0;
  private retryTimer: number = null;

  constructor(private http: HttpClient,
              private tokenStorage: TokenStorageService,
              private view: ChangeDetectorRef) {}

  @Input()
  set url(value: string) {
    this.reset();
    const ref = this.tokenStorage.getToken() ? audioRef(value) : null;
    if (!ref) {
      return;
    }
    const path = ref.host + '/' + encodeURIComponent(ref.id);
    const generation = this.generation;
    this.loadSource(path, generation);
    this.http.get<{duration: number, peaks: number[]}>(environment.serverUrl + '/tracks/peaks/' + path).subscribe({
      next: wave => {
        if (generation !== this.generation) {
          return;
        }
        this.peaks = wave?.peaks || [];
        if (!this.duration && wave?.duration) {
          this.duration = wave.duration;
        }
        this.needsDraw = true;
      }
    });
  }

  ngAfterViewChecked() {
    this.bindCanvas();
    if (this.needsDraw && this.canvas?.nativeElement?.clientWidth) {
      this.needsDraw = false;
      this.draw();
    }
  }

  ngOnDestroy() {
    window.clearTimeout(this.retryTimer);
    this.resize?.disconnect();
    this.audio?.nativeElement?.pause();
  }

  onAudioError() {
    const audio = this.audio?.nativeElement;
    if (audio && !audio.error && audio.readyState >= 2) {
      return;
    }
    this.scheduleRetry();
  }

  retry() {
    this.failed = false;
    this.mediaTries = 0;
    this.playing = false;
    this.reloadAudio();
    this.view.detectChanges();
  }

  toggle() {
    const audio = this.audio?.nativeElement;
    if (!audio) {
      return;
    }
    if (audio.paused) {
      this.playing = true;
      this.view.detectChanges();
      audio.play().catch(() => {
        this.playing = false;
        this.scheduleRetry();
        this.view.detectChanges();
      });
      return;
    }
    audio.pause();
    this.playing = false;
    this.view.detectChanges();
  }

  onPlay() {
    this.playing = true;
    this.view.detectChanges();
  }

  onPause() {
    this.playing = false;
    this.view.detectChanges();
  }

  onMeta() {
    const audio = this.audio?.nativeElement;
    if (!audio) {
      return;
    }
    this.mediaTries = 0;
    this.failed = false;
    this.duration = Number.isFinite(audio.duration) ? audio.duration : this.duration;
    this.needsDraw = true;
    this.view.detectChanges();
  }

  onTime() {
    const audio = this.audio?.nativeElement;
    if (!audio || this.scrubbing) {
      return;
    }
    this.current = audio.currentTime || 0;
    this.draw();
    this.view.detectChanges();
  }

  onEnded() {
    this.playing = false;
    this.current = 0;
    this.draw();
    this.view.detectChanges();
  }

  onPointerDown(event: PointerEvent) {
    const stage = event.currentTarget as HTMLElement;
    stage.setPointerCapture(event.pointerId);
    this.scrubbing = true;
    this.seekTo(event);
  }

  onPointerMove(event: PointerEvent) {
    if (!this.scrubbing) {
      return;
    }
    this.seekTo(event);
  }

  onPointerUp() {
    this.scrubbing = false;
  }

  setVolume(event: Event) {
    const audio = this.audio?.nativeElement;
    if (!audio) {
      return;
    }
    audio.volume = Number((event.target as HTMLInputElement).value);
  }

  clock(seconds: number) {
    if (!Number.isFinite(seconds) || seconds < 0) {
      return '0:00';
    }
    const whole = Math.floor(seconds);
    const minutes = Math.floor(whole / 60);
    return minutes + ':' + String(whole % 60).padStart(2, '0');
  }

  private reset() {
    this.generation++;
    window.clearTimeout(this.retryTimer);
    this.sourceTries = 0;
    this.mediaTries = 0;
    this.audio?.nativeElement?.pause();
    this.audioUrl = null;
    this.failed = false;
    this.playing = false;
    this.current = 0;
    this.duration = 0;
    this.peaks = [];
    this.needsDraw = false;
    this.scrubbing = false;
  }

  private loadSource(path: string, generation: number) {
    this.http.get<{src: string}>(environment.serverUrl + '/tracks/play/' + path).subscribe({
      next: source => {
        if (generation !== this.generation || !source?.src) {
          return;
        }
        this.audioUrl = source.src;
        this.needsDraw = true;
        this.view.detectChanges();
      },
      error: () => {
        if (generation !== this.generation) {
          return;
        }
        if (this.sourceTries < 2) {
          this.sourceTries++;
          this.retryTimer = window.setTimeout(() => this.loadSource(path, generation), 700);
          return;
        }
        this.failed = true;
        this.view.detectChanges();
      }
    });
  }

  private scheduleRetry() {
    if (!this.audioUrl || this.failed) {
      return;
    }
    if (this.mediaTries >= 3) {
      this.failed = true;
      this.playing = false;
      this.view.detectChanges();
      return;
    }
    const generation = this.generation;
    const src = this.audioUrl;
    this.mediaTries++;
    window.clearTimeout(this.retryTimer);
    this.retryTimer = window.setTimeout(() => {
      if (generation !== this.generation || this.audioUrl !== src) {
        return;
      }
      this.reloadAudio();
    }, 500 * this.mediaTries);
  }

  private reloadAudio() {
    const audio = this.audio?.nativeElement;
    if (!audio || !this.audioUrl) {
      return;
    }
    audio.pause();
    audio.src = this.audioUrl;
    audio.load();
  }

  private seekTo(event: PointerEvent) {
    const audio = this.audio?.nativeElement;
    const canvas = this.canvas?.nativeElement;
    if (!audio || !canvas || !this.duration) {
      return;
    }
    const rect = canvas.getBoundingClientRect();
    const ratio = Math.min(1, Math.max(0, (event.clientX - rect.left) / rect.width));
    this.current = ratio * this.duration;
    if (Number.isFinite(audio.duration)) {
      audio.currentTime = this.current;
    }
    this.draw();
  }

  private bindCanvas() {
    const node = this.canvas?.nativeElement;
    if (!node || node === this.watched) {
      return;
    }
    this.resize?.disconnect();
    this.watched = node;
    this.resize = new ResizeObserver(() => this.draw());
    this.resize.observe(node);
  }

  private draw() {
    const canvas = this.canvas?.nativeElement;
    if (!canvas || !canvas.clientWidth || !canvas.clientHeight) {
      return;
    }
    const width = canvas.clientWidth;
    const height = canvas.clientHeight;
    const dpr = window.devicePixelRatio || 1;
    canvas.width = Math.round(width * dpr);
    canvas.height = Math.round(height * dpr);
    const context = canvas.getContext('2d');
    if (!context) {
      return;
    }
    context.setTransform(dpr, 0, 0, dpr, 0, 0);
    context.clearRect(0, 0, width, height);

    const barWidth = 3;
    const gap = 2;
    const count = Math.max(24, Math.floor((width + gap) / (barWidth + gap)));
    const samples = this.samples(count);
    const mid = height / 2;
    const amp = Math.max(2, height / 2 - 2);
    const progress = this.duration ? Math.min(1, this.current / this.duration) : 0;

    const paint = (color: string) => {
      context.fillStyle = color;
      for (let i = 0; i < count; i++) {
        const bar = Math.max(2, samples[i] * amp);
        const x = i * (barWidth + gap);
        context.beginPath();
        context.roundRect(x, mid - bar, barWidth, bar * 2, 2);
        context.fill();
      }
    };

    paint('rgba(246, 241, 230, 0.28)');
    context.save();
    context.beginPath();
    context.rect(0, 0, width * progress, height);
    context.clip();
    paint('#e4ff4a');
    context.restore();
  }

  private samples(count: number) {
    if (!this.peaks.length) {
      return Array.from({length: count}, () => 0.16);
    }
    const bars = [];
    for (let i = 0; i < count; i++) {
      const start = Math.floor(i * this.peaks.length / count);
      const end = Math.max(start + 1, Math.floor((i + 1) * this.peaks.length / count));
      let peak = 0;
      for (let j = start; j < end && j < this.peaks.length; j++) {
        peak = Math.max(peak, this.peaks[j] || 0);
      }
      bars.push(peak);
    }
    return bars;
  }
}
