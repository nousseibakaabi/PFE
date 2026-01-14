import { Component, OnInit, AfterViewInit } from '@angular/core';
import { ChartService } from '../partials/services/chart.service';
import { MapService } from '../partials/services/map.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit, AfterViewInit {
  
  recentOrders = [
    { id: '12345', customer: 'John Doe', amount: 299.99, status: 'Completed' },
    { id: '12346', customer: 'Jane Smith', amount: 499.50, status: 'Pending' },
    { id: '12347', customer: 'Robert Johnson', amount: 199.99, status: 'Completed' },
    { id: '12348', customer: 'Emily Davis', amount: 899.00, status: 'Processing' },
    { id: '12349', customer: 'Michael Wilson', amount: 129.99, status: 'Completed' }
  ];

  private chart01: any;
  private chart02: any;
  private chart03: any;
  private map01: any;

  constructor(
    private chartService: ChartService,
    private mapService: MapService
  ) { }

  ngOnInit(): void {
    console.log('Dashboard initialized');
  }

  ngAfterViewInit(): void {
    // Initialize charts and maps after view is ready
    setTimeout(() => {
      this.initCharts();
      this.initMap();
    }, 100);
  }

  initCharts() {
    this.chart01 = this.chartService.initChart01();
    if (this.chart01) this.chart01.render();

    this.chart02 = this.chartService.initChart02();
    if (this.chart02) this.chart02.render();

    this.chart03 = this.chartService.initChart03();
    if (this.chart03) this.chart03.render();
  }

  initMap() {
    this.map01 = this.mapService.initMap01();
  }

  getStatusClass(status: string): string {
    switch(status) {
      case 'Completed':
        return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300';
      case 'Pending':
        return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-300';
      case 'Processing':
        return 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300';
      default:
        return 'bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-300';
    }
  }

  ngOnDestroy(): void {
    // Clean up charts when component is destroyed
    if (this.chart01) this.chart01.destroy();
    if (this.chart02) this.chart02.destroy();
    if (this.chart03) this.chart03.destroy();
  }
}