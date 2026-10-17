import { HTTP_INTERCEPTORS, HttpErrorResponse, HttpEvent, HttpEventType, HttpHandler, HttpInterceptor, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { TokenStorageService } from './token-storage.service';
import { UiRefresh } from '../ui-refresh';

// Angular 22 delivers fetch results outside the UI zone, so bound lists stay
// empty until a later click. Refresh the open page after each response.
export const zoneHttpInterceptor: HttpInterceptorFn = (req, next) => {
  const uiRefresh = inject(UiRefresh);
  return new Observable<HttpEvent<unknown>>(subscriber => {
    const subscription = next(req).subscribe({
      next: event => {
        subscriber.next(event);
        if (event.type === HttpEventType.Response) {
          uiRefresh.refresh();
        }
      },
      error: error => {
        subscriber.error(error);
        uiRefresh.refresh();
      },
      complete: () => subscriber.complete()
    });
    return () => subscription.unsubscribe();
  });
};

const TOKEN_HEADER_KEY = 'Authorization';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private token: TokenStorageService) {
  }

  intercept(req: HttpRequest<any>, next: HttpHandler) {
    let authReq = req;
    const token = this.token.getToken();
    if (token != null) {
      authReq = req.clone({ headers: req.headers.set(TOKEN_HEADER_KEY, 'Bearer ' + token) });
    }
    return next.handle(authReq).pipe(
      catchError((error: HttpErrorResponse) => {
        // A token the server no longer accepts (expired or signed with a rotated key):
        // drop it so the UI stops pretending the user is signed in.
        if (error.status === 401 && token != null && !req.url.includes('/auth/')) {
          this.token.signOut();
          const back = encodeURIComponent(window.location.pathname + window.location.search);
          window.location.assign('/login?wygasla=1&powrot=' + back);
        }
        return throwError(() => error);
      })
    );
  }
}

export const authInterceptorProviders = [
  { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }
];

