import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DepartmentService } from '../../services/department.service';
import { Department } from '../../models/models';

@Component({
  selector: 'app-departments',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './departments.component.html'
})
export class DepartmentsComponent implements OnInit {
  departments: Department[] = [];
  newName = '';
  newDescription = '';

  constructor(private departmentService: DepartmentService) {}

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.departmentService.findAll().subscribe(depts => {
      this.departments = depts;
    });
  }

  create(): void {
    if (!this.newName.trim()) return;
    this.departmentService.create(this.newName, this.newDescription).subscribe({
      next: () => {
        this.newName = '';
        this.newDescription = '';
        this.reload();
      },
      error: err => alert('Failed to create department: ' + err.message)
    });
  }
}
