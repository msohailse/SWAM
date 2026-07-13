import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Comment, Incident, Severity } from '../models/models';

@Injectable({ providedIn: 'root' })
export class IncidentService {
  constructor(private http: HttpClient) {}

  // The same CQRS-lite endpoint serves every role — the backend narrows the list based on
  // actingUserId (super admin: everything, department admin: their department, anyone
  // else: their own reports) and layers tag/severity/status on top of that, so a reporter
  // filtering their own list and an admin filtering the department list both go through
  // this one call.
  findAll(actingUserId: number, tag?: string | null, severity?: Severity | null, status?: string | null): Observable<Incident[]> {
    const params = new URLSearchParams({ actingUserId: String(actingUserId) });
    if (tag) {
      params.set('tag', tag);
    }
    if (severity) {
      params.set('severity', severity);
    }
    if (status) {
      params.set('status', status);
    }
    return this.http.get<Incident[]>(`/api/incidents?${params.toString()}`);
  }

  // No department here on purpose — assigning a department is an admin action, done
  // afterward via update(), never something the reporter sets while filing a report.
  create(title: string, description: string, severity: Severity, tagTitle: string, reportedByUserId: number): Observable<Incident> {
    return this.http.post<Incident>('/api/incidents', { title, description, severity, tagTitle, reportedByUserId });
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

  delete(id: number, actingUserId: number): Observable<void> {
    return this.http.delete<void>(`/api/incidents/${id}?actingUserId=${actingUserId}`);
  }
}
