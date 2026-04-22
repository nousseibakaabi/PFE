import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ClientBilanService, ClientBilan } from '../../services/client-bilan.service';
import { FormBuilder, FormGroup } from '@angular/forms';
import { saveAs } from 'file-saver';

// Import Chart.js differently to avoid type issues
import { Chart, registerables } from 'chart.js';
Chart.register(...registerables);

@Component({
  selector: 'app-client-detail',
  templateUrl: './client-detail.component.html',
  styleUrls: ['./client-detail.component.css']
})
export class ClientDetailComponent implements OnInit, OnDestroy {
  clientId: number = 0;
  bilan: ClientBilan | null = null;
  loading = false;
  regenerating = false;
  dateRangeForm: FormGroup;
  selectedConvention: any = null;
  showInvoiceModal = false;
  selectedInvoice: any = null;
  
  // Charts - use 'any' type to avoid TypeScript conflicts
  private paymentChart: any = null;
  private yearlyChart: any = null;
  private ratingChart: any = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private clientBilanService: ClientBilanService,
    private fb: FormBuilder
  ) {
    this.dateRangeForm = this.fb.group({
      startDate: [''],
      endDate: ['']
    });
  }

  ngOnInit(): void {
    this.clientId = Number(this.route.snapshot.params['id']);
    this.loadBilan();
  }

  ngOnDestroy(): void {
    this.destroyCharts();
  }

  loadBilan(): void {
    this.loading = true;
    const { startDate, endDate } = this.dateRangeForm.value;
    
    this.clientBilanService.getClientBilan(this.clientId, startDate, endDate).subscribe({
      next: (bilan) => {
        this.bilan = bilan;
        this.loading = false;
        setTimeout(() => this.initCharts(), 200);
      },
      error: (error) => {
        console.error('Error loading bilan:', error);
        this.loading = false;
      }
    });
  }

  regenerateBilan(): void {
    this.regenerating = true;
    const { startDate, endDate } = this.dateRangeForm.value;
    
    this.clientBilanService.getClientBilan(this.clientId, startDate, endDate).subscribe({
      next: (bilan) => {
        this.bilan = bilan;
        this.regenerating = false;
        this.destroyCharts();
        setTimeout(() => this.initCharts(), 200);
      },
      error: (error) => {
        console.error('Error regenerating bilan:', error);
        this.regenerating = false;
      }
    });
  }

  exportPdf(): void {
    const { startDate, endDate } = this.dateRangeForm.value;
    this.clientBilanService.exportToPdf(this.clientId, startDate, endDate).subscribe({
      next: (blob) => {
        saveAs(blob, `client-bilan-${this.bilan?.clientCode}.pdf`);
      },
      error: (error) => console.error('Error exporting PDF:', error)
    });
  }

  exportExcel(): void {
    const { startDate, endDate } = this.dateRangeForm.value;
    this.clientBilanService.exportToExcel(this.clientId, startDate, endDate).subscribe({
      next: (blob) => {
        saveAs(blob, `client-bilan-${this.bilan?.clientCode}.xlsx`);
      },
      error: (error) => console.error('Error exporting Excel:', error)
    });
  }

  initCharts(): void {
    if (!this.bilan) return;
    this.initPaymentChart();
    this.initYearlyChart();
    this.initRatingChart();
  }

  initPaymentChart(): void {
    const canvas = document.getElementById('paymentChart') as HTMLCanvasElement;
    if (!canvas || !this.bilan) return;
    
    // Destroy existing chart
    if (this.paymentChart) {
      this.paymentChart.destroy();
    }
    
    this.paymentChart = new Chart(canvas, {
      type: 'doughnut',
      data: {
        labels: ['Paiements à temps', 'Paiements en retard', 'Paiements en avance'],
        datasets: [{
          data: [
            this.bilan.paymentStats.onTimePayments,
            this.bilan.paymentStats.latePayments,
            this.bilan.paymentStats.advancePayments
          ],
          backgroundColor: ['#28a745', '#dc3545', '#17a2b8'],
          borderWidth: 0
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom' },
          title: { display: true, text: 'Distribution des Paiements' }
        }
      }
    });
  }

  initYearlyChart(): void {
    const canvas = document.getElementById('yearlyChart') as HTMLCanvasElement;
    if (!canvas || !this.bilan) return;
    
    if (this.yearlyChart) {
      this.yearlyChart.destroy();
    }
    
    const years = Object.keys(this.bilan.financialSummary.yearlyTotal).sort();
    const totals = years.map(y => this.bilan!.financialSummary.yearlyTotal[Number(y)]);
    const paid = years.map(y => this.bilan!.financialSummary.yearlyPaid[Number(y)] || 0);
    
    this.yearlyChart = new Chart(canvas, {
      type: 'bar',
      data: {
        labels: years,
        datasets: [
          {
            label: 'Total Contrat',
            data: totals,
            backgroundColor: '#007bff',
            borderRadius: 5
          },
          {
            label: 'Payé',
            data: paid,
            backgroundColor: '#28a745',
            borderRadius: 5
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          title: { display: true, text: 'Évolution Annuelle' },
          tooltip: { 
            callbacks: { 
              label: (context: any) => `${context.dataset.label}: ${context.raw} TND` 
            } 
          }
        },
        scales: { y: { beginAtZero: true, title: { display: true, text: 'Montant (TND)' } } }
      }
    });
  }

  initRatingChart(): void {
    const canvas = document.getElementById('ratingChart') as HTMLCanvasElement;
    if (!canvas || !this.bilan) return;
    
    if (this.ratingChart) {
      this.ratingChart.destroy();
    }
    
    this.ratingChart = new Chart(canvas, {
      type: 'radar',
      data: {
        labels: ['Score Paiement (max 40)', 'Score Conformité (max 30)', 'Score Activité (max 30)'],
        datasets: [{
          label: 'Scores',
          data: [
            this.bilan.rating.paymentScore,
            this.bilan.rating.contractComplianceScore,
            this.bilan.rating.activityScore
          ],
          backgroundColor: 'rgba(54, 162, 235, 0.2)',
          borderColor: '#36a2eb',
          borderWidth: 2,
          pointBackgroundColor: '#36a2eb'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: { 
          r: { 
            beginAtZero: true, 
            max: 40, 
            ticks: { stepSize: 10 },
            pointLabels: { font: { size: 10 } }
          } 
        },
        plugins: { title: { display: true, text: 'Évaluation par Critère' } }
      }
    });
  }

  destroyCharts(): void {
    if (this.paymentChart) { 
      this.paymentChart.destroy(); 
      this.paymentChart = null; 
    }
    if (this.yearlyChart) { 
      this.yearlyChart.destroy(); 
      this.yearlyChart = null; 
    }
    if (this.ratingChart) { 
      this.ratingChart.destroy(); 
      this.ratingChart = null; 
    }
  }

  getStatusClass(status: string): string {
    const classes: { [key: string]: string } = {
      'PAYE': 'success', 
      'NON_PAYE': 'warning', 
      'EN_RETARD': 'danger'
    };
    return classes[status] || 'secondary';
  }

  getPaymentTimingIcon(timing: string): string {
    const icons: { [key: string]: string } = {
      'ON_TIME': 'bi-check-circle-fill text-success',
      'LATE': 'bi-exclamation-triangle-fill text-danger',
      'ADVANCE': 'bi-stars text-info',
      'PENDING': 'bi-clock-history text-warning'
    };
    return icons[timing] || 'bi-question-circle';
  }

  viewInvoiceDetails(invoice: any): void {
    this.selectedInvoice = invoice;
    this.showInvoiceModal = true;
  }

  closeModal(): void {
    this.showInvoiceModal = false;
    this.selectedInvoice = null;
  }

  goBack(): void {
    this.router.navigate(['/client-bilan']);
  }
}