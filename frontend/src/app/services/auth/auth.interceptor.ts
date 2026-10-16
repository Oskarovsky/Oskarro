import { HTTP_INTERCEPTORS, HttpEvent, HttpEventType, HttpHandler, HttpInterceptor, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

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
    return next.handle(authReq);
  }
}

export const authInterceptorProviders = [
  { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }
];

