import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { IncidentService } from '../../services/incident.service';
import { Comment, Incident, Department } from '../../models/models';

@Component({
  selector: 'app-incident-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './incident-modal.component.html'
})
export class IncidentModalComponent implements OnInit {
  @Input() incident!: Incident;
  @Input() departments: Department[] = [];
  @Output() close = new EventEmitter<void>();
  @Output() incidentUpdated = new EventEmitter<void>();

  comments: Comment[] = [];
  replyText = '';
  closeAssignedDepartmentId: number | null = null;

  constructor(
    public auth: AuthService,
    private incidentService: IncidentService
  ) {}

  ngOnInit(): void {
    this.closeAssignedDepartmentId = this.incident.assignedDepartment ? this.incident.assignedDepartment.id : null;
    this.loadComments();
  }

  loadComments(): void {
    this.incidentService.findComments(this.incident.id).subscribe((comments) => (this.comments = comments));
  }

  closeModal(): void {
    this.close.emit();
  }

  closeIncident(): void {
    const user = this.auth.currentUser();
    if (!user) return;
    
    const targetDeptId = this.closeAssignedDepartmentId || (this.incident.assignedDepartment ? this.incident.assignedDepartment.id : null);
    if (!targetDeptId) {
      alert('You must assign a department to close this incident.');
      return;
    }

    const commentText = window.prompt('Closing comment:');
    if (!commentText) return;

    this.incidentService.close(this.incident.id, user.id, commentText, targetDeptId).subscribe((updated) => {
      this.incident = updated;
      this.incidentUpdated.emit();
      this.loadComments();
    });
  }

  reply(): void {
    const user = this.auth.currentUser();
    if (!user || !this.replyText.trim()) {
      return;
    }
    this.incidentService.addComment(this.incident.id, user.id, this.replyText).subscribe((comment) => {
      this.comments = [...this.comments, comment];
      this.replyText = '';
    });
  }
}
