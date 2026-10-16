import {AfterViewChecked, ChangeDetectorRef, Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Subscription} from 'rxjs';
import {PlaybackService} from '../playback.service';

@Component({
  selector: 'app-deck',
  templateUrl: './deck.component.html',
  standalone: false
})
export class DeckComponent implements OnInit, AfterViewChecked, OnDestroy {
  deadBars = [28, 46, 62, 34, 74, 41, 55, 30, 68, 48, 36, 80, 44, 58, 32, 70, 40, 52, 26, 64, 47, 38, 72, 33, 57, 43, 66, 31, 50, 37];

  @ViewChild('canvas') canvas?: ElementRef<HTMLCanvasElement>;

  private sub: Subscription;
  private scrubbing = false;
  private resize: ResizeObserver = null;
  private watched: HTMLCanvasElement = null;

  constructor(readonly playback: PlaybackService,
              private view: ChangeDetectorRef) {}

  ngOnInit() {
    this.sub = this.playback.changes.subscribe(() => {
      this.view.detectChanges();
      this.draw();
    });
  }

  ngAfterViewChecked() {
    this.bindCanvas();
  }

  ngOnDestroy() {
    this.sub?.unsubscribe();
    this.resize?.disconnect();
  }

  onPointerDown(event: PointerEvent) {
    const stage = event.currentTarget as HTMLElement;
    stage.setPointerCapture(event.pointerId);
    this.scrubbing = true;
    this.seek(event);
  }

  onPointerMove(event: PointerEvent) {
    if (this.scrubbing) {
      this.seek(event);
    }
  }

  onPointerUp() {
    this.scrubbing = false;
  }

  setVolume(event: Event) {
    this.playback.setVolume(Number((event.target as HTMLInputElement).value));
  }

  private seek(event: PointerEvent) {
    const canvas = this.canvas?.nativeElement;
    if (!canvas) {
      return;
    }
    const rect = canvas.getBoundingClientRect();
    const ratio = (event.clientX - rect.left) / rect.width;
    this.playback.seekRatio(ratio);
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
    this.draw();
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
    const progress = this.playback.duration ? Math.min(1, this.playback.current / this.playback.duration) : 0;
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
    const peaks = this.playback.peaks;
    if (!peaks.length) {
      return Array.from({length: count}, () => 0.16);
    }
    const bars = [];
    for (let i = 0; i < count; i++) {
      const start = Math.floor(i * peaks.length / count);
      const end = Math.max(start + 1, Math.floor((i + 1) * peaks.length / count));
      let peak = 0;
      for (let j = start; j < end && j < peaks.length; j++) {
        peak = Math.max(peak, peaks[j] || 0);
      }
      bars.push(peak);
    }
    return bars;
  }
}
