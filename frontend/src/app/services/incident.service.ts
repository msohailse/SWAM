import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Comment, Incident, Severity } from '../models/models';

@Injectable({ providedIn: 'root' })
export class IncidentService {
  constructor(private http: HttpClient) {}

  findAll(): Observable<Incident[]> {
    return this.http.get<Incident[]>('/api/incidents');
  }

  findByUser(userId: number): Observable<Incident[]> {
    return this.http.get<Incident[]>(`/api/incidents/user/${userId}`);
  }

  create(title: string, description: string, severity: Severity, tagTitle: string, reportedByUserId: number): Observable<Incident> {
    return this.http.post<Incident>('/api/incidents', { title, description, severity, tagTitle, reportedByUserId });
  }

  update(id: number, title: string, description: string, severity: Severity): Observable<Incident> {
    return this.http.put<Incident>(`/api/incidents/${id}`, { title, description, severity });
  }

  close(id: number, actingUserId: number, commentText: string): Observable<Incident> {
    return this.http.patch<Incident>(`/api/incidents/${id}/close`, { actingUserId, commentText });
  }

  findComments(id: number): Observable<Comment[]> {
    return this.http.get<Comment[]>(`/api/incidents/${id}/comments`);
  }

  addComment(id: number, authorUserId: number, text: string): Observable<Comment> {
    return this.http.post<Comment>(`/api/incidents/${id}/comments`, { authorUserId, text });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/incidents/${id}`);
  }
}
