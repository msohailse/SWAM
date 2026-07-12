import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Tag } from '../models/models';

@Injectable({ providedIn: 'root' })
export class TagService {
  constructor(private http: HttpClient) {}

  findAll(): Observable<Tag[]> {
    return this.http.get<Tag[]>('/api/tags');
  }

  create(actingUserId: number, tagTitle: string, tagDescription: string): Observable<Tag> {
    return this.http.post<Tag>('/api/tags', { actingUserId, tagTitle, tagDescription });
  }

  update(actingUserId: number, id: number, tagTitle: string, tagDescription: string): Observable<Tag> {
    return this.http.put<Tag>(`/api/tags/${id}`, { actingUserId, tagTitle, tagDescription });
  }

  delete(actingUserId: number, id: number): Observable<void> {
    return this.http.delete<void>(`/api/tags/${id}?actingUserId=${actingUserId}`);
  }
}
