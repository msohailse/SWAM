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

  create(tagTitle: string, tagDescription: string): Observable<Tag> {
    return this.http.post<Tag>('/api/tags', { tagTitle, tagDescription });
  }

  update(id: number, tagTitle: string, tagDescription: string): Observable<Tag> {
    return this.http.put<Tag>(`/api/tags/${id}`, { tagTitle, tagDescription });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/tags/${id}`);
  }
}
