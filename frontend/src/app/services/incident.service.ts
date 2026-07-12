import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Comment, Incident, Severity } from '../models/models';

@Injectable({ providedIn: 'root' })
export class IncidentService {
  constructor(private http: HttpClient) {}

  findAll(actingUserId: number): Observable<Incident[]> {
    return this.http.get<Incident[]>(`/api/incidents?actingUserId=${actingUserId}`);
  }

  findByUser(userId: number): Observable<Incident[]> {
    return this.http.get<Incident[]>(`/api/incidents/user/${userId}`);
  }

  create(title: string, description: string, severity: Severity, tagTitle: string, reportedByUserId: number, assignedDepartmentId?: number): Observable<Incident> {
    return this.http.post<Incident>('/api/incidents', { title, description, severity, tagTitle, reportedByUserId, assignedDepartmentId });
  }

  update(id: number, actingUserId: number, title: string, description: string, severity: Severity, assignedDepartmentId?: number | null): Observable<Incident> {
    return this.http.put<Incident>(`/api/incidents/${id}`, { actingUserId, title, description, severity, assignedDepartmentId });
  }

  close(id: number, actingUserId: number, commentText: string, assignedDepartmentId?: number): Observable<Incident> {
    return this.http.patch<Incident>(`/api/incidents/${id}/close`, { actingUserId, commentText, assignedDepartmentId });
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
