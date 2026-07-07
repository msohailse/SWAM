import { Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login.component';
import { RegisterComponent } from './pages/register/register.component';
import { IncidentsComponent } from './pages/incidents/incidents.component';
import { TagsComponent } from './pages/tags/tags.component';
import { adminGuard, authGuard } from './guards/admin.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'incidents', component: IncidentsComponent, canActivate: [authGuard] },
  { path: 'tags', component: TagsComponent, canActivate: [authGuard, adminGuard] },
  { path: '', redirectTo: 'incidents', pathMatch: 'full' },
  { path: '**', redirectTo: 'incidents' }
];
