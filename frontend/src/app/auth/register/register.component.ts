import { Component, OnInit } from '@angular/core';
import {RefreshAfterHttpDirective} from '../../services/ui-refresh';
import { AuthService } from '../../services/auth/auth.service';
import {LangService} from '../../lang.service';

const USERNAME = /^[\p{L}0-9_.-]{3,32}$/u;

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css'],
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class RegisterComponent implements OnInit {

  form: any = {username: '', email: '', password: '', confirm: '', terms: false, hp: ''};
  isSuccessful = false;
  isSignUpFailed = false;
  errorMessage = '';
  delivered = false;
  submitted = false;
  busy = false;

  constructor(private authService: AuthService,
              readonly lang: LangService) { }

  ngOnInit() {
  }

  get mismatch() {
    return !!this.form.password && this.form.password !== this.form.confirm;
  }

  get usernameHint(): string {
    if (!this.submitted) {
      return '';
    }
    const username = (this.form.username || '').trim();
    if (!username || username.length < 3) {
      return '';
    }
    if (!USERNAME.test(username)) {
      return this.lang.t(
        'Nazwa: 3–32 znaki, litery, cyfry, kropka, myślnik albo _.',
        'Username: 3–32 characters, letters, digits, a dot, a hyphen or _.'
      );
    }
    return '';
  }

  onSubmit() {
    this.submitted = true;
    this.isSignUpFailed = false;
    const username = (this.form.username || '').trim();
    const email = (this.form.email || '').trim();
    if (!USERNAME.test(username) || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) || email.length > 50
      || (this.form.password || '').length < 6 || this.mismatch || !this.form.terms) {
      return;
    }
    this.busy = true;
    this.authService.register({
      username,
      email,
      password: this.form.password,
      hp: this.form.hp || ''
    }).subscribe({
      next: (body) => {
        this.busy = false;
        this.delivered = !!body?.delivered;
        this.isSuccessful = true;
      },
      error: err => {
        this.busy = false;
        const raw = err?.error?.message || (typeof err?.error === 'string' ? err.error : '');
        this.errorMessage = this.explain(raw);
        this.isSignUpFailed = true;
      }
    });
  }

  private explain(raw: string): string {
    const known: Record<string, string> = {
      'Ta nazwa użytkownika jest już zajęta.': 'That username is already taken.',
      'Ten adres email jest już używany.': 'This email address is already in use.',
      'Nie udało się założyć konta.': 'The account could not be created.',
      'Nazwa użytkownika jest wymagana': 'Username is required',
      'Nazwa: 3–32 znaki, litery, cyfry, kropka, myślnik albo _.': 'Username: 3–32 characters, letters, digits, a dot, a hyphen or _.',
      'Wymagany adres email': 'Email address is required',
      'Email może mieć najwyżej 50 znaków': 'Email can be at most 50 characters',
      'Niepoprawny email': 'Invalid email',
      'Wymagane hasło': 'Password is required',
      'Hasło musi zawierać minimum 6 znaków': 'The password must be at least 6 characters'
    };
    if (!raw) {
      return this.lang.t('Spróbuj ponownie za chwilę.', 'Try again in a moment.');
    }
    return this.lang.en && known[raw] ? known[raw] : raw;
  }
}
