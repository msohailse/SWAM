import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.component.html'
})
export class RegisterComponent {
  firstName = '';
  lastName = '';
  email = '';
  password = '';
  errorMessage = '';

  constructor(private auth: AuthService, private router: Router) {}

  submit(): void {
    this.errorMessage = '';
    this.auth.register(this.firstName, this.lastName, this.email, this.password).subscribe({
      next: () => this.router.navigate(['/login']),
      error: (err) => (this.errorMessage = err.error?.error ?? 'Registration failed')
    });
  }
}
