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
import { CalendarComponent } from './components/calendar/calendar.component';
import { MailBoxComponent } from './components/mail-box/mail-box.component';
import { ConventionDetailComponent } from './components/convention-detail/convention-detail.component';
import { ConventionFormComponent } from './components/convention-form/convention-form.component';
import { FactureDetailComponent } from './components/facture-detail/facture-detail.component';
import { ApplicationDetailComponent } from './components/application-detail/application-detail.component';
import { ApplicationFormComponent } from './components/application-form/application-form.component';
import { ApplicationComponent } from './components/application/application.component';
import { HistoryComponent } from './components/history/history.component';
import { NotificationsComponent } from './components/notifications/notifications.component';
import { ConventionFacturesComponent } from './components/convention-factures/convention-factures.component';
import { ConventionHistoryComponent } from './components/convention-history/convention-history.component';
import { RequestsComponent } from './components/requests/requests.component';
import { ApplicationArchiveComponent } from './components/application-archive/application-archive.component';
import { ConventionVersionsComponent } from './components/convention-versions/convention-versions.component';

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
  },
  
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
  
  // Convention routes
  { 
    path: 'conventions', 
    component: ConventionComponent, 
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_COMMERCIAL_METIER', 'ROLE_ADMIN', 'ROLE_CHEF_PROJET', 'ROLE_DECIDEUR'] } 
  },
  { 
    path: 'conventions/archives', 
    component: ConventionArchiveComponent, 
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_COMMERCIAL_METIER', 'ROLE_ADMIN'] } 
  },
  { 
    path: 'conventions/new', 
    component: ConventionFormComponent,
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_COMMERCIAL_METIER', 'ROLE_ADMIN'] }
  },
  { 
    path: 'conventions/edit/:id', 
    component: ConventionFormComponent,
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_COMMERCIAL_METIER', 'ROLE_ADMIN'] }
  },
  { 
    path: 'conventions/:id', 
    component: ConventionDetailComponent,
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_COMMERCIAL_METIER', 'ROLE_ADMIN', 'ROLE_CHEF_PROJET', 'ROLE_DECIDEUR'] }
  },
  
  // Facture routes
  { 
    path: 'factures', 
    component: FactureComponent, 
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_COMMERCIAL_METIER', 'ROLE_ADMIN'] } 
  },
  { 
    path: 'factures/:id', 
    component: FactureDetailComponent,
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_COMMERCIAL_METIER', 'ROLE_ADMIN', 'ROLE_CHEF_PROJET', 'ROLE_DECIDEUR'] }
  },

  // Application routes 
  { 
    path: 'applications', 
    component: ApplicationComponent,
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_ADMIN', 'ROLE_CHEF_PROJET', 'ROLE_COMMERCIAL_METIER', 'ROLE_DECIDEUR'] }
  },
  { 
    path: 'applications/new', 
    component: ApplicationFormComponent,
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_ADMIN', 'ROLE_CHEF_PROJET'] }
  },
  { 
    path: 'applications/edit/:id', 
    component: ApplicationFormComponent,
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_ADMIN', 'ROLE_CHEF_PROJET'] }
  },
  { 
    path: 'applications/:id', 
    component: ApplicationDetailComponent,
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_ADMIN', 'ROLE_CHEF_PROJET', 'ROLE_COMMERCIAL_METIER', 'ROLE_DECIDEUR'] }
  },

  // Role dashboards
  { 
    path: 'chef', 
    component: ChefProjetComponent, 
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
  path: 'history',
  component: HistoryComponent,
  canActivate: [AuthGuard],
  data: { roles: ['ROLE_ADMIN', 'ROLE_DECIDEUR','ROLE_COMMERCIAL_METIER','ROLE_CHEF_PROJET'] }
  },

{
  path: 'conventions/:id/factures',
component: ConventionFacturesComponent,
canActivate: [AuthGuard],
data: { roles: ['ROLE_ADMIN', 'ROLE_COMMERCIAL_METIER', 'ROLE_CHEF_PROJET', 'ROLE_DECIDEUR'] }
},
{
  path: 'conventions/:id/history',
component: ConventionHistoryComponent,
canActivate: [AuthGuard],
data: { roles: ['ROLE_ADMIN', 'ROLE_DECIDEUR','ROLE_COMMERCIAL_METIER','ROLE_CHEF_PROJET'] }
},


{ 
  path: 'requests', 
  component: RequestsComponent, 
  canActivate: [AuthGuard],
  data: { roles: ['ROLE_ADMIN', 'ROLE_CHEF_PROJET'] } 
},

{ 
  path: 'archives/applications', 
  component: ApplicationArchiveComponent, 
  canActivate: [AuthGuard],
  data: { roles: ['ROLE_ADMIN', 'ROLE_CHEF_PROJET'] } 
},

{
  path: 'conventions/:id/versions',
  component: ConventionVersionsComponent, 
  canActivate: [AuthGuard]
},



  { path: 'notifications', component: NotificationsComponent, canActivate: [AuthGuard] },
  { path: 'mailBox', component: MailBoxComponent, canActivate: [AuthGuard] },
  { path: '**', component: NotFoundComponent } 
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }