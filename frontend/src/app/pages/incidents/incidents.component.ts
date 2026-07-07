import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { IncidentService } from '../../services/incident.service';
import { Comment, Incident, Severity } from '../../models/models';

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

  // Comment thread: which incident's thread is expanded, its comments, and the reply draft.
  openThreadId: number | null = null;
  comments: Comment[] = [];
  replyText = '';

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
    this.incidentService.close(incident.id, user.id, commentText).subscribe(() => {
      this.reload();
      this.openThread(incident);
    });
  }

  delete(id: number): void {
    this.incidentService.delete(id).subscribe(() => this.reload());
  }

  toggleThread(incident: Incident): void {
    if (this.openThreadId === incident.id) {
      this.openThreadId = null;
      this.comments = [];
      return;
    }
    this.openThread(incident);
  }

  private openThread(incident: Incident): void {
    this.openThreadId = incident.id;
    this.replyText = '';
    this.incidentService.findComments(incident.id).subscribe((comments) => (this.comments = comments));
  }

  reply(incidentId: number): void {
    const user = this.auth.currentUser();
    if (!user || !this.replyText.trim()) {
      return;
    }
    this.incidentService.addComment(incidentId, user.id, this.replyText).subscribe((comment) => {
      this.comments = [...this.comments, comment];
      this.replyText = '';
    });
  }
}
