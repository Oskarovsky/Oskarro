import {Component, Input} from '@angular/core';
import {LangService} from '../lang.service';

const FACEBOOK_APP_ID = '876955753919962';

/** "Send to a friend": the phone's own share sheet, Messenger, WhatsApp, Facebook and copy link. */
@Component({
  selector: 'app-share-buttons',
  standalone: false,
  template: `
    <div class="share-bar" role="group" [attr.aria-label]="'Udostępnij' | t:'Share'">
      <button *ngIf="native" class="btn-ghost share-btn" type="button" (click)="shareNative()">↗ {{ 'Wyślij znajomemu' | t:'Send to a friend' }}</button>
      <a class="share-btn share-messenger" [href]="messenger" target="_blank" rel="noopener" [attr.aria-label]="'Wyślij przez Messengera' | t:'Send via Messenger'">Messenger</a>
      <a class="share-btn share-whatsapp" [href]="whatsapp" target="_blank" rel="noopener" [attr.aria-label]="'Wyślij przez WhatsAppa' | t:'Send via WhatsApp'">WhatsApp</a>
      <a class="share-btn share-facebook" [href]="facebook" target="_blank" rel="noopener" [attr.aria-label]="'Udostępnij na Facebooku' | t:'Share on Facebook'">Facebook</a>
      <button class="btn-ghost share-btn" type="button" (click)="copy()">{{ copied ? ('Skopiowano ✓' | t:'Copied ✓') : ('Kopiuj link' | t:'Copy link') }}</button>
    </div>
    <p class="meta track-note" *ngIf="manual">{{ lang.en ? 'Copy the link: ' : 'Skopiuj link: ' }}{{ url }}</p>
  `
})
export class ShareButtonsComponent {
  /** The canonical address, without query parameters, so link previews show the right page. */
  @Input() url = '';
  @Input() title = '';
  copied = false;
  manual = false;

  constructor(readonly lang: LangService) {}

  get native() {
    return typeof navigator !== 'undefined' && !!navigator.share;
  }

  private get text() {
    return (this.title ? this.title + ' — ' : '') + this.lang.t('posłuchaj na VIXA', 'listen on VIXA');
  }

  get messenger() {
    const link = encodeURIComponent(this.url);
    return /Android|iPhone|iPad/i.test(navigator.userAgent)
      ? 'fb-messenger://share/?link=' + link
      : 'https://www.facebook.com/dialog/send?app_id=' + FACEBOOK_APP_ID + '&link=' + link + '&redirect_uri=' + link;
  }

  get whatsapp() {
    return 'https://wa.me/?text=' + encodeURIComponent(this.text + ' ' + this.url);
  }

  get facebook() {
    return 'https://www.facebook.com/sharer/sharer.php?u=' + encodeURIComponent(this.url);
  }

  async shareNative() {
    try {
      await navigator.share({title: this.title, text: this.text, url: this.url});
    } catch {
      // closed by the user or not allowed: nothing to do
    }
  }

  async copy() {
    try {
      await navigator.clipboard.writeText(this.url);
      this.copied = true;
      this.manual = false;
      setTimeout(() => this.copied = false, 2500);
    } catch {
      this.manual = true;
    }
  }
}
