import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { IncidentService } from '../../services/incident.service';
import { Incident, Severity } from '../../models/models';

@Component({
  selector: 'app-incidents',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './incidents.component.html'
})
export class IncidentsComponent implements OnInit {
  incidents: Incident[] = [];
  editingId: number | null = null;

  newTitle = '';
  newDescription = '';
  newSeverity: Severity = 'LOW';
  newTagTitle = '';

  editTitle = '';
  editDescription = '';
  editSeverity: Severity = 'LOW';

  constructor(public auth: AuthService, private incidentService: IncidentService) {}

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
  }

  create(): void {
    const user = this.auth.currentUser();
    if (!user) {
      return;
    }
    this.incidentService
      .create(this.newTitle, this.newDescription, this.newSeverity, this.newTagTitle, user.id)
      .subscribe(() => {
        this.newTitle = '';
        this.newDescription = '';
        this.newSeverity = 'LOW';
        this.newTagTitle = '';
        this.reload();
      });
  }

  startEdit(incident: Incident): void {
    this.editingId = incident.id;
    this.editTitle = incident.title;
    this.editDescription = incident.description ?? '';
    this.editSeverity = incident.severity;
  }

  cancelEdit(): void {
    this.editingId = null;
  }

  saveEdit(id: number): void {
    this.incidentService.update(id, this.editTitle, this.editDescription, this.editSeverity).subscribe(() => {
      this.editingId = null;
      this.reload();
    });
  }

  close(incident: Incident): void {
    const user = this.auth.currentUser();
    if (!user) {
      return;
    }
    const commentText = window.prompt('Closing comment:');
    if (!commentText) {
      return;
    }
    this.incidentService.close(incident.id, user.id, commentText).subscribe(() => this.reload());
  }

  delete(id: number): void {
    this.incidentService.delete(id).subscribe(() => this.reload());
  }
}
