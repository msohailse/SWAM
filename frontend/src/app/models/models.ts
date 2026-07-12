export type UserType = 'REPORTER' | 'ADMIN';

export interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  userType: UserType;
  department: Department | null;
}

export type Severity = 'LOW' | 'MEDIUM' | 'HIGH';

export interface Tag {
  id: number;
  tagTitle: string;
  tagDescription: string | null;
}

export interface Department {
  id: number;
  name: string;
  description: string | null;
}

export interface Incident {
  id: number;
  title: string;
  description: string | null;
  severity: Severity;
  reportedAt: string;
  reportedBy: User;
  tag: Tag;
  assignedDepartment: Department | null;
  closed: boolean;
}

export interface Comment {
  id: number;
  text: string;
  createdAt: string;
  author: User;
}
