import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  email = '';
  password = '';
  errorMessage = '';

  constructor(private auth: AuthService, private router: Router) {}

  submit(): void {
    this.errorMessage = '';
    this.auth.login(this.email, this.password).subscribe({
      next: () => this.router.navigate(['/incidents']),
      error: (err) => (this.errorMessage = err.error?.error ?? 'Login failed')
    });
  }
}
