import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';
import { User } from '../models/models';

const STORAGE_KEY = 'swam.currentUser';
const TOKEN_KEY = 'swam.token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  currentUser = signal<User | null>(readStoredUser());

  constructor(private http: HttpClient) {}

  register(firstName: string, lastName: string, email: string, password: string): Observable<User> {
    return this.http.post<User>('/api/users/register', { firstName, lastName, email, password });
  }

  // Tier A: the backend issues a real signed JWT on login (in the Authorization response
  // header), but nothing validates it yet — actingUserId-based checks in each service
  // remain the actual authorization boundary. We just capture and carry it here so the
  // interceptor can attach it, ready for enforcement to be turned on later.
  login(email: string, password: string): Observable<User> {
    return this.http.post<User>('/api/users/login', { email, password }, { observe: 'response' }).pipe(
      tap((response) => {
        const authHeader = response.headers.get('Authorization');
        if (authHeader?.startsWith('Bearer ')) {
          localStorage.setItem(TOKEN_KEY, authHeader.substring('Bearer '.length));
        }
        this.currentUser.set(response.body);
        localStorage.setItem(STORAGE_KEY, JSON.stringify(response.body));
      }),
      map((response) => response.body as User)
    );
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  logout(): void {
    this.currentUser.set(null);
    localStorage.removeItem(STORAGE_KEY);
    localStorage.removeItem(TOKEN_KEY);
  }

  // True admin access *right now* — SUPER_ADMIN always qualifies; ADMIN only if its
  // adminExpiresAt hasn't passed yet. This is what every admin-only nav link/guard/data
  // source should check, so an expired grant loses access everywhere for free the moment
  // it lapses, with no separate "is this still valid" check needed anywhere else.
  isAdmin(): boolean {
    const user = this.currentUser();
    if (!user) {
      return false;
    }
    if (user.userType === 'SUPER_ADMIN') {
      return true;
    }
    if (user.userType !== 'ADMIN') {
      return false;
    }
    return !user.adminExpiresAt || new Date(user.adminExpiresAt) > new Date();
  }

  // Sees/manages every incident and is the only type that can assign/reassign a
  // department or create other users. Never expires.
  isSuperAdmin(): boolean {
    return this.currentUser()?.userType === 'SUPER_ADMIN';
  }
}

function readStoredUser(): User | null {
  const raw = localStorage.getItem(STORAGE_KEY);
  return raw ? JSON.parse(raw) : null;
}
