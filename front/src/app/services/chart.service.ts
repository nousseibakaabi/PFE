// src/app/services/chart.service.ts
import { Injectable } from '@angular/core';
import Chart from 'chart.js/auto';

@Injectable({
  providedIn: 'root'
})
export class ChartService {
  
  createChart(ctx: HTMLCanvasElement, type: string, data: any, options?: any): any {
    const defaultOptions = {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        y: {
          beginAtZero: true
        }
      }
    };

    return new Chart(ctx, {
      type: type as any,
      data: data,
      options: { ...defaultOptions, ...options }
    });
  }

  destroyChart(chart: any): void {
    if (chart && typeof chart.destroy === 'function') {
      chart.destroy();
    }
  }
}