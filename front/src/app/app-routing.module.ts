import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { ProfileComponent } from './components/profile/profile.component';
import { AuthGuard } from './guards/auth.guard';
import { NotFoundComponent } from './components/not-found/not-found.component';
import { EmailSendComponent } from './components/email-send/email-send.component';
import { ResetPasswordComponent } from './components/reset-password/reset-password.component';
import { ForgetPasswordComponent } from './components/forget-password/forget-password.component';
import { AdminUsersComponent } from './components/admin-users/admin-users.component';
import { AdminNomenclaturesComponent } from './components/admin-nomenclatures/admin-nomenclatures.component';
import { AdminComponent } from './components/admin/admin.component';

const routes: Routes = [
  { path: '', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'profile', component: ProfileComponent, canActivate: [AuthGuard] },
  { path: 'dashboard', component: DashboardComponent, canActivate: [AuthGuard] },
  { path: 'forget-password', component: ForgetPasswordComponent },
  { path: 'reset-password', component: ResetPasswordComponent },
  { path: 'email-sent', component: EmailSendComponent },
  
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
  { path: '**', component: NotFoundComponent } // Keep this last
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }