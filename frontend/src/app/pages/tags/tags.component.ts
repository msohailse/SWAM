import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TagService } from '../../services/tag.service';
import { Tag } from '../../models/models';

@Component({
  selector: 'app-tags',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tags.component.html'
})
export class TagsComponent implements OnInit {
  tags: Tag[] = [];
  editingId: number | null = null;

  newTitle = '';
  newDescription = '';
  editTitle = '';
  editDescription = '';

  constructor(private tagService: TagService) {}

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.tagService.findAll().subscribe((tags) => (this.tags = tags));
  }

  create(): void {
    this.tagService.create(this.newTitle, this.newDescription).subscribe(() => {
      this.newTitle = '';
      this.newDescription = '';
      this.reload();
    });
  }

  startEdit(tag: Tag): void {
    this.editingId = tag.id;
    this.editTitle = tag.tagTitle;
    this.editDescription = tag.tagDescription ?? '';
  }

  cancelEdit(): void {
    this.editingId = null;
  }

  saveEdit(id: number): void {
    this.tagService.update(id, this.editTitle, this.editDescription).subscribe(() => {
      this.editingId = null;
      this.reload();
    });
  }

  delete(id: number): void {
    this.tagService.delete(id).subscribe(() => this.reload());
  }
}
