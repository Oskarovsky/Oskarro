import {inject} from '@angular/core';
import {CanActivateFn, Router} from '@angular/router';
import {TokenStorageService} from '../services/auth/token-storage.service';

/** UI guard only — the API itself refuses non-admins. */
export const adminGuard: CanActivateFn = () => {
  const tokens = inject(TokenStorageService);
  const router = inject(Router);
  const roles: string[] = tokens.getToken() ? (tokens.getUser()?.roles || []) : [];
  return roles.includes('ROLE_ADMIN') ? true : router.parseUrl('/login?powrot=%2Fadmin');
};
