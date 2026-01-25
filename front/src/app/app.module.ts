import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms'; // ADD THIS LINE
import { RouterModule } from '@angular/router';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { HeaderComponent } from './components/partials/header/header.component';
import { SidebarComponent } from './components/partials/sidebar/sidebar.component';
import { PreloaderComponent } from './components/partials/preloader/preloader.component';
import { OverlayComponent } from './components/partials/overlay/overlay.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { LayoutService } from './components/partials/services/layout.service';
import { NotFoundComponent } from './components/not-found/not-found.component';
import { EmailSendComponent } from './components/email-send/email-send.component';
import { ResetPasswordComponent } from './components/reset-password/reset-password.component';
import { ForgetPasswordComponent } from './components/forget-password/forget-password.component';
import { ProfileComponent } from './components/profile/profile.component';
import { RegisterComponent } from './components/register/register.component';
import { LoginComponent } from './components/login/login.component';
import { HttpClientModule ,HTTP_INTERCEPTORS} from '@angular/common/http';
import { AdminUsersComponent } from './components/admin-users/admin-users.component';
import { TranslatePipe } from './components/partials/traduction/translate.pipe';
import { AsyncTranslatePipe } from './components/partials/traduction/async-translate.pipe';
import { TranslationService } from './components/partials/traduction/translation.service';
import { AdminNomenclaturesComponent } from './components/admin-nomenclatures/admin-nomenclatures.component';
import { AdminComponent } from './components/admin/admin.component';
import { FactureComponent } from './components/facture/facture.component';
import { ConventionComponent } from './components/convention/convention.component';
import { AuthInterceptor } from './interceptors/auth.interceptor';
import { CommercialComponent } from './components/commercial/commercial.component';
import { DecideurComponent } from './components/decideur/decideur.component';
import { ChefProjetComponent } from './components/chef-projet/chef-projet.component';
import { StatsService } from './services/stats.service';

@NgModule({
  declarations: [
    AppComponent,
    HeaderComponent,
    SidebarComponent,
    PreloaderComponent,
    OverlayComponent,
    DashboardComponent,
    LoginComponent,
    RegisterComponent,
    ProfileComponent,
    ForgetPasswordComponent,
    ResetPasswordComponent,
    EmailSendComponent,
    NotFoundComponent,
    AdminUsersComponent,
    TranslatePipe,
    AsyncTranslatePipe,
    AdminNomenclaturesComponent,
    AdminComponent,
    FactureComponent,
    ConventionComponent,
    CommercialComponent,
    DecideurComponent,
    ChefProjetComponent,
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    FormsModule, 
    RouterModule,
    HttpClientModule,
    ReactiveFormsModule,
    
  ],
  providers: [
    LayoutService,
    TranslationService,
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true
    },
    StatsService
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
