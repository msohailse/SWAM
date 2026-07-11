import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

interface LogLine {
  time: string;
  id: string;
  text: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CLOSED';
}

@Component({
  selector: 'app-auth-console',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './auth-console.component.html'
})
export class AuthConsoleComponent {
  @Input() heading = '';
  @Input() subheading = '';

  readonly logLines: LogLine[] = [
    { time: '08:14:02', id: '#0231', text: 'Server room AC failure', severity: 'HIGH' },
    { time: '08:16:47', id: '#0198', text: 'VPN intermittent disconnects', severity: 'MEDIUM' },
    { time: '08:19:23', id: '#0240', text: 'Printer offline, 3rd floor', severity: 'LOW' },
    { time: '08:22:10', id: '#0198', text: 'Closed by admin', severity: 'CLOSED' },
    { time: '08:27:55', id: '#0244', text: 'Email delivery delayed', severity: 'HIGH' },
    { time: '08:31:18', id: '#0233', text: 'Password reset request', severity: 'LOW' },
    { time: '08:35:02', id: '#0247', text: 'Duplicate flagged: #0231', severity: 'MEDIUM' },
    { time: '08:41:36', id: '#0198', text: 'Reply added to thread', severity: 'LOW' }
  ];
}
