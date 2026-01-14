import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-preloader',
  templateUrl: './preloader.component.html',
  styleUrls: ['./preloader.component.css']
})
export class PreloaderComponent implements OnInit {
  isLoading: boolean = true;

  ngOnInit(): void {
    // Hide preloader after page loads
    window.addEventListener('DOMContentLoaded', () => {
      setTimeout(() => {
        this.isLoading = false;
      }, 500);
    });

    // Also hide if page is already loaded
    if (document.readyState === 'complete') {
      setTimeout(() => {
        this.isLoading = false;
      }, 500);
    }
  }
}