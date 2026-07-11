import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { IncidentService } from '../../services/incident.service';
import { TagService } from '../../services/tag.service';
import { DepartmentService } from '../../services/department.service';
import { Comment, Incident, Severity, Tag, Department } from '../../models/models';
import { IncidentModalComponent } from './incident-modal.component';

@Component({
  selector: 'app-incidents',
  standalone: true,
  imports: [CommonModule, FormsModule, IncidentModalComponent],
  templateUrl: './incidents.component.html'
})
export class IncidentsComponent implements OnInit {
  incidents: Incident[] = [];
  tags: Tag[] = [];
  departments: Department[] = [];
  editingId: number | null = null;

  newTitle = '';
  newDescription = '';
  newSeverity: Severity = 'LOW';
  newTagTitle = '';
  newAssignedDepartmentId: number | null = null;

  editTitle = '';
  editDescription = '';
  editSeverity: Severity = 'LOW';
  editAssignedDepartmentId: number | null = null;

  // Modal state
  viewingIncident: Incident | null = null;

  constructor(
    public auth: AuthService,
    private incidentService: IncidentService,
    private tagService: TagService,
    private departmentService: DepartmentService
  ) {}

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    const user = this.auth.currentUser();
    if (!user) {
      return;
    }
    const source = this.auth.isAdmin() ? this.incidentService.findAll() : this.incidentService.findByUser(user.id);
    source.subscribe((incidents) => (this.incidents = incidents));
    this.tagService.findAll().subscribe((tags) => (this.tags = tags));
    this.departmentService.findAll().subscribe((deps) => (this.departments = deps));
  }

  create(): void {
    const user = this.auth.currentUser();
    if (!user) {
      return;
    }
    this.incidentService
      .create(this.newTitle, this.newDescription, this.newSeverity, this.newTagTitle, user.id, this.newAssignedDepartmentId || undefined)
      .subscribe(() => {
        this.newTitle = '';
        this.newDescription = '';
        this.newSeverity = 'LOW';
        this.newTagTitle = '';
        this.newAssignedDepartmentId = null;
        this.reload();
      });
  }

  startEdit(incident: Incident): void {
    this.editingId = incident.id;
    this.editTitle = incident.title;
    this.editDescription = incident.description ?? '';
    this.editSeverity = incident.severity;
    this.editAssignedDepartmentId = incident.assignedDepartment?.id || null;
  }

  cancelEdit(): void {
    this.editingId = null;
  }

  saveEdit(id: number): void {
    this.incidentService.update(id, this.editTitle, this.editDescription, this.editSeverity, this.editAssignedDepartmentId).subscribe(() => {
      this.editingId = null;
      this.reload();
    });
  }

  delete(id: number): void {
    this.incidentService.delete(id).subscribe(() => this.reload());
  }

  viewIncident(incident: Incident): void {
    this.viewingIncident = incident;
  }
}
