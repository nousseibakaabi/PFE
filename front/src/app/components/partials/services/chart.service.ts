import { Injectable, AfterViewInit } from '@angular/core';
import * as ApexCharts from 'apexcharts';

@Injectable({
  providedIn: 'root'
})
export class ChartService {
  
  // Chart 01: Bar Chart
  initChart01(containerId: string = 'chartOne') {
    const element = document.getElementById(containerId);
    if (!element) return;

    const options = {
      series: [{
        name: "Sales",
        data: [168, 385, 201, 298, 187, 195, 291, 110, 215, 390, 280, 112]
      }],
      colors: ["#465fff"],
      chart: {
        fontFamily: "Outfit, sans-serif",
        type: "bar",
        height: 180,
        toolbar: { show: false }
      },
      plotOptions: {
        bar: {
          horizontal: false,
          columnWidth: "39%",
          borderRadius: 5,
          borderRadiusApplication: "end",
        }
      },
      dataLabels: { enabled: false },
      stroke: {
        show: true,
        width: 4,
        colors: ["transparent"]
      },
      xaxis: {
        categories: ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"],
        axisBorder: { show: false },
        axisTicks: { show: false }
      },
      legend: {
        show: true,
        position: "top",
        horizontalAlign: "left",
        fontFamily: "Outfit",
        markers: { radius: 99 }
      },
      yaxis: { title: false },
      grid: {
        yaxis: { lines: { show: true } }
      },
      fill: { opacity: 1 },
      tooltip: {
        x: { show: false },
        y: { formatter: (val: number) => val.toString() }
      }
    };

    return new ApexCharts(element, options);
  }

  // Chart 02: Radial Bar Chart
  initChart02(containerId: string = 'chartTwo') {
    const element = document.getElementById(containerId);
    if (!element) return;

    const options = {
      series: [75.55],
      colors: ["#465FFF"],
      chart: {
        fontFamily: "Outfit, sans-serif",
        type: "radialBar",
        height: 330,
        sparkline: { enabled: true }
      },
      plotOptions: {
        radialBar: {
          startAngle: -90,
          endAngle: 90,
          hollow: { size: "80%" },
          track: {
            background: "#E4E7EC",
            strokeWidth: "100%",
            margin: 5
          },
          dataLabels: {
            name: { show: false },
            value: {
              fontSize: "36px",
              fontWeight: "600",
              offsetY: 60,
              color: "#1D2939",
              formatter: (val: number) => `${val}%`
            }
          }
        }
      },
      fill: {
        type: "solid",
        colors: ["#465FFF"]
      },
      stroke: { lineCap: "round" },
      labels: ["Progress"]
    };

    return new ApexCharts(element, options);
  }

  // Chart 03: Area Chart
  initChart03(containerId: string = 'chartThree') {
    const element = document.getElementById(containerId);
    if (!element) return;

    const options = {
      series: [
        { name: "Sales", data: [180, 190, 170, 160, 175, 165, 170, 205, 230, 210, 240, 235] },
        { name: "Revenue", data: [40, 30, 50, 40, 55, 40, 70, 100, 110, 120, 150, 140] }
      ],
      legend: {
        show: false,
        position: "top",
        horizontalAlign: "left"
      },
      colors: ["#465FFF", "#9CB9FF"],
      chart: {
        fontFamily: "Outfit, sans-serif",
        height: 310,
        type: "area",
        toolbar: { show: false }
      },
      fill: {
        gradient: {
          enabled: true,
          opacityFrom: 0.55,
          opacityTo: 0
        }
      },
      stroke: {
        curve: "straight",
        width: ["2", "2"]
      },
      markers: { size: 0 },
      labels: { show: false, position: "top" },
      grid: {
        xaxis: { lines: { show: false } },
        yaxis: { lines: { show: true } }
      },
      dataLabels: { enabled: false },
      tooltip: {
        x: { format: "dd MMM yyyy" }
      },
      xaxis: {
        type: "category",
        categories: ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"],
        axisBorder: { show: false },
        axisTicks: { show: false },
        tooltip: false
      },
      yaxis: {
        title: { style: { fontSize: "0px" } }
      }
    };

    return new ApexCharts(element, options);
  }
}