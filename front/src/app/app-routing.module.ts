import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { ProfileComponent } from './components/profile/profile.component';
import { AuthGuard } from './guards/auth.guard';
import { NotFoundComponent } from './components/not-found/not-found.component';
import { EmailSendComponent } from './components/email-send/email-send.component';
import { ResetPasswordComponent } from './components/reset-password/reset-password.component';
import { ForgetPasswordComponent } from './components/forget-password/forget-password.component';
import { AdminUsersComponent } from './components/admin-users/admin-users.component';
import { AdminNomenclaturesComponent } from './components/admin-nomenclatures/admin-nomenclatures.component';
import { AdminComponent } from './components/admin/admin.component';
import { FactureComponent } from './components/facture/facture.component';
import { ConventionComponent } from './components/convention/convention.component';
import { ChefProjetComponent } from './components/chef-projet/chef-projet.component';
import { CommercialComponent } from './components/commercial/commercial.component';
import { DecideurComponent } from './components/decideur/decideur.component';
import { ConventionArchiveComponent } from './components/convention-archive/convention-archive.component';
import { AdminProjectComponent } from './components/admin-project/admin-project.component';
import { ProjectComponent } from './components/project/project.component';
import { CalendarComponent } from './components/calendar/calendar.component';

const routes: Routes = [
  { path: '', component: LoginComponent },
  { path: 'profile', component: ProfileComponent, canActivate: [AuthGuard] },
  { path: 'forget-password', component: ForgetPasswordComponent },
  { path: 'reset-password', component: ResetPasswordComponent },
  { path: 'email-sent', component: EmailSendComponent },
    { 
    path: 'commercial/calendar', 
    component: CalendarComponent,
   canActivate: [AuthGuard],
    data: { roles: ['ROLE_COMMERCIAL_METIER'] }
  } ,
  
   { 
    path: 'admin', 
    component: AdminComponent, 
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_ADMIN'] } 
  },

  { 
    path: 'admin/users', 
    component: AdminUsersComponent, 
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_ADMIN'] } 
  },

  { 
    path: 'admin/nomenclatures', 
    component: AdminNomenclaturesComponent, 
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_ADMIN'] } 
  },
    { 
    path: 'conventions', 
    component: ConventionComponent, 
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_COMMERCIAL_METIER'] } 
  },

   { 
    path: 'conventions/archives', 
    component: ConventionArchiveComponent, 
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_COMMERCIAL_METIER'] } 
  },
  { 
    path: 'factures', 
    component: FactureComponent, 
    canActivate: [AuthGuard],
    data: { roles: [ 'ROLE_COMMERCIAL_METIER'] } 
  },

     { 
    path: 'chef', 
    component: ChefProjetComponent, 
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_CHEF_PROJET'] } 
  },

  { 
    path: 'chef/projet', 
    component: ProjectComponent, 
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_CHEF_PROJET'] } 
  },
     { 
    path: 'commercial', 
    component: CommercialComponent, 
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_COMMERCIAL_METIER'] } 
  },
  { 
    path: 'decideur', 
    component: DecideurComponent, 
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_DECIDEUR'] } 
  },


  { 
    path: 'admin/projects', 
    component: AdminProjectComponent, 
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_ADMIN'] } 
  },





  { path: '**', component: NotFoundComponent } // Keep this last
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }