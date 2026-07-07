import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Tag } from '../models/models';

@Injectable({ providedIn: 'root' })
export class TagService {
  constructor(private http: HttpClient) {}

  findAll(): Observable<Tag[]> {
    return this.http.get<Tag[]>('/tags');
  }

  create(tagTitle: string, tagDescription: string): Observable<Tag> {
    return this.http.post<Tag>('/tags', { tagTitle, tagDescription });
  }

  update(id: number, tagTitle: string, tagDescription: string): Observable<Tag> {
    return this.http.put<Tag>(`/tags/${id}`, { tagTitle, tagDescription });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/tags/${id}`);
  }
}
