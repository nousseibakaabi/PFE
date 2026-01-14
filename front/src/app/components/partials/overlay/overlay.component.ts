import { Component, Input, Output, EventEmitter } from '@angular/core';
import { LayoutService } from '../services/layout.service';

@Component({
  selector: 'app-overlay',
  templateUrl: './overlay.component.html',
  styleUrls: ['./overlay.component.css']
})
export class OverlayComponent {
  @Input() isVisible: boolean = false;
  @Output() overlayClick = new EventEmitter<void>();


    constructor(private layoutService: LayoutService) {
    // Subscribe to sidebar state
    this.layoutService.sidebarOpen$.subscribe((isOpen) => {
      this.isVisible = isOpen && window.innerWidth < 1024; // Only show on mobile
    });
  }

  onOverlayClick(): void {
    this.layoutService.setSidebarOpen(false);
  }
}