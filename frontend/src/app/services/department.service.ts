import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Department } from '../models/models';

@Injectable({ providedIn: 'root' })
export class DepartmentService {
  constructor(private http: HttpClient) {}

  findAll(): Observable<Department[]> {
    return this.http.get<Department[]>('/api/departments');
  }

  create(actingUserId: number, name: string, description: string): Observable<Department> {
    return this.http.post<Department>('/api/departments', { actingUserId, name, description });
  }
}
