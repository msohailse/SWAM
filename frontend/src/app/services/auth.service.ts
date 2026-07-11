import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { User } from '../models/models';

const STORAGE_KEY = 'swam.currentUser';

@Injectable({ providedIn: 'root' })
export class AuthService {
  currentUser = signal<User | null>(readStoredUser());

  constructor(private http: HttpClient) {}

  register(firstName: string, lastName: string, email: string, password: string): Observable<User> {
    return this.http.post<User>('/api/users/register', { firstName, lastName, email, password });
  }

  login(email: string, password: string): Observable<User> {
    return this.http.post<User>('/api/users/login', { email, password }).pipe(
      tap((user) => {
        this.currentUser.set(user);
        localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
      })
    );
  }

  logout(): void {
    this.currentUser.set(null);
    localStorage.removeItem(STORAGE_KEY);
  }

  isAdmin(): boolean {
    return this.currentUser()?.userType === 'ADMIN';
  }
}

function readStoredUser(): User | null {
  const raw = localStorage.getItem(STORAGE_KEY);
  return raw ? JSON.parse(raw) : null;
}
