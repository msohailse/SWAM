import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { UserService } from '../../services/user.service';
import { DepartmentService } from '../../services/department.service';
import { Department, User, UserType } from '../../models/models';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './users.component.html'
})
export class UsersComponent implements OnInit {
  users: User[] = [];
  departments: Department[] = [];

  newFirstName = '';
  newLastName = '';
  newEmail = '';
  newPassword = '';
  newUserType: UserType = 'ADMIN';
  newDepartmentId: number | null = null;
  newExpiresInDays: number | null = null;

  constructor(
    private auth: AuthService,
    private userService: UserService,
    private departmentService: DepartmentService
  ) {}

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.userService.findAll().subscribe((users) => (this.users = users));
    this.departmentService.findAll().subscribe((deps) => (this.departments = deps));
  }

  create(): void {
    const actingUser = this.auth.currentUser();
    if (!actingUser) {
      return;
    }
    const departmentId = this.newUserType === 'ADMIN' ? this.newDepartmentId : null;
    const expiresInDays = this.newUserType === 'ADMIN' ? this.newExpiresInDays : null;
    this.userService
      .create(actingUser.id, this.newFirstName, this.newLastName, this.newEmail, this.newPassword, this.newUserType, departmentId, expiresInDays)
      .subscribe({
        next: () => {
          this.newFirstName = '';
          this.newLastName = '';
          this.newEmail = '';
          this.newPassword = '';
          this.newUserType = 'ADMIN';
          this.newDepartmentId = null;
          this.newExpiresInDays = null;
          this.reload();
        },
        error: (err) => alert(err.error?.error ?? 'Failed to create user')
      });
  }
}
