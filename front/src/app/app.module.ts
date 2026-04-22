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
import { LayoutService } from './components/partials/services/layout.service';
import { NotFoundComponent } from './components/not-found/not-found.component';
import { EmailSendComponent } from './components/email-send/email-send.component';
import { ResetPasswordComponent } from './components/reset-password/reset-password.component';
import { ForgetPasswordComponent } from './components/forget-password/forget-password.component';
import { ProfileComponent } from './components/profile/profile.component';
import { LoginComponent } from './components/login/login.component';
import { HttpClientModule ,HTTP_INTERCEPTORS} from '@angular/common/http';
import { AdminUsersComponent } from './components/admin-users/admin-users.component';
import { TranslatePipe } from './components/partials/traduction/translate.pipe';
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
import { ConventionArchiveComponent } from './components/convention-archive/convention-archive.component';
import { CalendarComponent } from './components/calendar/calendar.component';
import { FullCalendarModule } from '@fullcalendar/angular';
import { ApplicationComponent } from './components/application/application.component';
import { ApplicationDetailComponent } from './components/application-detail/application-detail.component';
import { MailBoxComponent } from './components/mail-box/mail-box.component';
import { MailGroupComponent } from './components/mail-group/mail-group.component';
import { EmailInputComponent } from './components/email-input/email-input.component';
import { ConventionDetailComponent } from './components/convention-detail/convention-detail.component';
import { FactureDetailComponent } from './components/facture-detail/facture-detail.component';
import { ConventionFormComponent } from './components/convention-form/convention-form.component';
import { ApplicationFormComponent } from './components/application-form/application-form.component';
import { InfiniteScrollModule } from 'ngx-infinite-scroll';
import { NotificationsComponent } from './components/notifications/notifications.component';
import { CommonModule, DatePipe } from '@angular/common';
import { DatePickerComponent } from './components/date-picker/date-picker.component';
import { ConventionFacturesComponent } from './components/convention-factures/convention-factures.component';
import { ConventionHistoryComponent } from './components/convention-history/convention-history.component';
import { RequestsComponent } from './components/requests/requests.component';
import { ApplicationArchiveComponent } from './components/application-archive/application-archive.component';
import { RecaptchaModule, RecaptchaFormsModule } from 'ng-recaptcha';
import { TranslateComponent } from './components/translate/translate.component';
import { CreateReassignmentRequestComponent } from './components/create-reassignment-request/create-reassignment-request.component';
import { ConventionVersionsComponent } from './components/convention-versions/convention-versions.component';
import { TwoFactorSetupComponent } from './components/two-factor-setup/two-factor-setup.component';
import { ChatAiComponent } from './components/chat-ai/chat-ai.component';
import { Nl2brPipe } from './services/nl2br.pipe';
import { ChatFullPageComponent } from './components/chat-full-page/chat-full-page.component';
import { ClientListComponent } from './components/client-list/client-list.component';
import { ClientDetailComponent } from './components/client-detail/client-detail.component';
import { BilanRevenueComponent } from './components/bilan-revenue/bilan-revenue.component';
import { BilanOneComponent } from './components/bilan-one/bilan-one.component';

@NgModule({
  declarations: [
    AppComponent,
    HeaderComponent,
    SidebarComponent,
    PreloaderComponent,
    OverlayComponent,
    LoginComponent,
    ProfileComponent,
    ForgetPasswordComponent,
    ResetPasswordComponent,
    EmailSendComponent,
    NotFoundComponent,
    AdminUsersComponent,
    TranslatePipe,
    AdminNomenclaturesComponent,
    AdminComponent,
    FactureComponent,
    ConventionComponent,
    CommercialComponent,
    DecideurComponent,
    ChefProjetComponent,
    ConventionArchiveComponent,
    CalendarComponent,
    ApplicationComponent,
    ApplicationDetailComponent,
    MailBoxComponent,
    MailGroupComponent,
    EmailInputComponent,
    ConventionDetailComponent,
    FactureDetailComponent,
    ConventionFormComponent,
    ApplicationFormComponent,
    NotificationsComponent,
    DatePickerComponent,
    ConventionFacturesComponent,
    ConventionHistoryComponent,
    RequestsComponent,
    ApplicationArchiveComponent,
    TranslateComponent,
    CreateReassignmentRequestComponent,
    ConventionVersionsComponent,
    TwoFactorSetupComponent,
    ChatAiComponent,
      Nl2brPipe,
      ChatFullPageComponent,
      ClientListComponent,
      ClientDetailComponent,
      BilanRevenueComponent,
      BilanOneComponent,
    ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    FormsModule, 
    RouterModule,
    HttpClientModule,
    ReactiveFormsModule,
    FullCalendarModule,
    InfiniteScrollModule,
    CommonModule,
    RecaptchaModule,       
    RecaptchaFormsModule
    
  ],
  exports: [
    TranslatePipe,
    TwoFactorSetupComponent,
  ],
  providers: [
    LayoutService,
    TranslationService,
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true
    },
    StatsService,
    DatePipe,
    Nl2brPipe
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
