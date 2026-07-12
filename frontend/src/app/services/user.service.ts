import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { User, UserType } from '../models/models';

@Injectable({ providedIn: 'root' })
export class UserService {
  constructor(private http: HttpClient) {}

  findAll(): Observable<User[]> {
    return this.http.get<User[]>('/api/users');
  }

  // Super-admin-only on the backend — creates an ADMIN (with a required department,
  // optional time-boxed expiry) or another SUPER_ADMIN. Public register() on
  // AuthService never creates anything but a REPORTER.
  create(
    actingUserId: number,
    firstName: string,
    lastName: string,
    email: string,
    password: string,
    userType: UserType,
    departmentId?: number | null,
    adminExpiresInDays?: number | null
  ): Observable<User> {
    return this.http.post<User>('/api/users/admin-create', {
      actingUserId,
      firstName,
      lastName,
      email,
      password,
      userType,
      departmentId,
      adminExpiresInDays
    });
  }
}
