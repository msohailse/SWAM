import { Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login.component';
import { RegisterComponent } from './pages/register/register.component';
import { IncidentsComponent } from './pages/incidents/incidents.component';
import { TagsComponent } from './pages/tags/tags.component';
import { DepartmentsComponent } from './pages/departments/departments.component';
import { UsersComponent } from './pages/users/users.component';
import { adminGuard, authGuard, superAdminGuard } from './guards/admin.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'incidents', component: IncidentsComponent, canActivate: [authGuard] },
  { path: 'tags', component: TagsComponent, canActivate: [authGuard, adminGuard] },
  { path: 'departments', component: DepartmentsComponent, canActivate: [authGuard, adminGuard] },
  { path: 'users', component: UsersComponent, canActivate: [authGuard, superAdminGuard] },
  { path: '', redirectTo: 'incidents', pathMatch: 'full' },
  { path: '**', redirectTo: 'incidents' }
];
