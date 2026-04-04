import { Component, ViewEncapsulation } from '@angular/core';
import { Location } from '@angular/common';


@Component({
  selector: 'app-not-found',
  templateUrl: './not-found.component.html',
  styleUrls: ['./not-found.component.css'],
  encapsulation: ViewEncapsulation.None
})
export class NotFoundComponent {
  isDarkMode = false;

  constructor(private location: Location) {
    document.body.classList.add('no-navbar');
    this.checkDarkMode();
  }

  toggleDarkMode(): void {
    this.isDarkMode = !this.isDarkMode;
    this.applyDarkMode();
    localStorage.setItem('darkMode', this.isDarkMode.toString());
  }

  private checkDarkMode(): void {
    const savedDarkMode = localStorage.getItem('darkMode');
    if (savedDarkMode) {
      this.isDarkMode = savedDarkMode === 'true';
    } else {
      this.isDarkMode = window.matchMedia('(prefers-color-scheme: dark)').matches;
    }
    this.applyDarkMode();
  }

  private applyDarkMode(): void {
    document.documentElement.classList.toggle('dark', this.isDarkMode);
  }


  goBack(): void {
  this.location.back();
}
}