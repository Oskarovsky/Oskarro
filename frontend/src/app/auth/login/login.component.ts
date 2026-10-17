import { Component, OnInit } from '@angular/core';
import {RefreshAfterHttpDirective} from '../../services/ui-refresh';
import { AuthService } from '../../services/auth/auth.service';
import { TokenStorageService } from '../../services/auth/token-storage.service';
import { LangService } from '../../lang.service';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class LoginComponent implements OnInit {

  form: any = {};
  isLoggedIn = false;
  isLoginFailed = false;
  errorMessage = '';
  roles: string[] = [];
  sessionExpired = false;

  constructor(private authService: AuthService,
              private tokenStorage: TokenStorageService,
              private route: ActivatedRoute,
              readonly lang: LangService) { }

  ngOnInit() {
    this.sessionExpired = this.route.snapshot.queryParamMap.has('wygasla');
    if (this.tokenStorage.getToken()) {
      this.isLoggedIn = true;
      this.roles = this.tokenStorage.getUser().roles;
    }
  }

  onSubmit() {
    this.authService.login(this.form).subscribe(
      data => {
        this.tokenStorage.saveToken(data.accessToken);
        this.tokenStorage.saveUser(data);

        this.isLoginFailed = false;
        this.isLoggedIn = true;
        this.roles = this.tokenStorage.getUser().roles;
        this.reloadPage();
      },
      error => {
        const raw = error?.error?.message || '';
        if (raw.includes('aktywne')) {
          this.errorMessage = this.lang.t(
            'Konto nie jest jeszcze aktywne. Otwórz link z maila aktywacyjnego.',
            'The account is not active yet. Open the link from the activation email.'
          );
        } else {
          this.errorMessage = raw || this.lang.t('Nieprawidłowa nazwa albo hasło.', 'Wrong username or password.');
        }
        this.isLoginFailed = true;
      }
    );
  }

  reloadPage() {
    // Return to the page that sent the user here, but only within this site.
    const back = this.route.snapshot.queryParamMap.get('powrot') || '';
    if (back.startsWith('/') && !back.startsWith('//') && !back.includes('\\') && !back.startsWith('/login')) {
      window.location.assign(back);
      return;
    }
    window.location.reload();
  }

  roleLabel(): string {
    const raw = this.roles as unknown;
    const roles = Array.isArray(raw) ? raw.map(role => String(role)) : (raw ? [String(raw)] : []);
    if (roles.includes('ROLE_ADMIN')) {
      return this.lang.t('administrator', 'an administrator');
    }
    if (roles.includes('ROLE_MODERATOR') || roles.includes('ROLE_MOD')) {
      return this.lang.t('moderator', 'a moderator');
    }
    return this.lang.t('użytkownik', 'a user');
  }

}
