import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AIRiskService, RiskPrediction, AIRecommendation, BatchRiskResponse, DashboardData, ModelInfo, UpcomingInvoice, DashboardSummaryResponse } from '../../services/ai-risk.service';
import { finalize, Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-risk-ai',
  templateUrl: './risk-ai.component.html',
  styleUrls: ['./risk-ai.component.css']
})
export class RiskAIComponent implements OnInit, OnDestroy {
  // Active tab
  activeTab: 'dashboard' | 'upcoming' | 'predictions' | 'recommendations' | 'batch' | 'admin' = 'dashboard';
  
  // Loading states
  loading = {
    dashboard: false,
    upcoming: false,
    prediction: false,
    batch: false,
    recommendations: false,
    retrain: false,
    modelInfo: false,
    refreshUpcoming: false
  };
  
  // Data
  dashboardData: DashboardData | null = null;
  dashboardSummary: DashboardSummaryResponse | null = null;
  upcomingInvoices: UpcomingInvoice[] = [];
  upcomingSummary: any = null;
  currentPrediction: RiskPrediction | null = null;
  batchResults: BatchRiskResponse | null = null;
  invoiceRecommendations: AIRecommendation[] = [];
  clientRecommendations: AIRecommendation[] = [];
  modelInfo: ModelInfo | null = null;
  
  // Error messages
  errorMessage: string | null = null;
  successMessage: string | null = null;
  
  // Forms
  predictionForm: FormGroup;
  batchForm: FormGroup;
  newInvoiceForm: FormGroup;
  clientRecForm: FormGroup;
  upcomingDaysForm: FormGroup;
  
  // Selected items
  selectedFactureNumero: string | null = null;
  selectedClientCode: string | null = null;
  
  // Risk level colors mapping
  riskColors = {
    CRITICAL: '#dc2626',
    VERY_HIGH: '#ea580c',
    HIGH: '#f97316',
    MEDIUM: '#eab308',
    LOW: '#22c55e',
    VERY_LOW: '#10b981'
  };
  
  private destroy$ = new Subject<void>();

  constructor(
    private aiRiskService: AIRiskService,
    private fb: FormBuilder
  ) {
    this.predictionForm = this.fb.group({
      numeroFacture: ['', [Validators.required]]
    });
    
    this.batchForm = this.fb.group({
      clientCode: ['', [Validators.required]]
    });
    
    this.clientRecForm = this.fb.group({
      clientCode: ['', [Validators.required]]
    });
    
    this.newInvoiceForm = this.fb.group({
      clientCode: ['', [Validators.required]],
      amount: ['', [Validators.required, Validators.min(0.01)]],
      dueDate: ['', [Validators.required]],
      applicationCode: ['']
    });
    
    this.upcomingDaysForm = this.fb.group({
      days: [6, [Validators.required, Validators.min(1), Validators.max(90)]]
    });
  }

  ngOnInit(): void {
    this.loadDashboard();
    this.loadUpcomingInvoices();
    this.checkHealth();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // Helper methods for messages
  showSuccess(message: string): void {
    this.successMessage = message;
    this.errorMessage = null;
    setTimeout(() => {
      this.successMessage = null;
    }, 3000);
  }

  showError(message: string): void {
    this.errorMessage = message;
    this.successMessage = null;
    setTimeout(() => {
      this.errorMessage = null;
    }, 5000);
  }

  showInfo(message: string): void {
    console.log('INFO:', message);
    alert(message);
  }

  // ==================== DASHBOARD METHODS ====================
  
  loadDashboard(): void {
    this.loading.dashboard = true;
    this.errorMessage = null;
    
    // Load both dashboard data and summary
    this.aiRiskService.getRiskDashboard()
      .pipe(
        finalize(() => this.loading.dashboard = false),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (response) => {
          if (response.success) {
            this.dashboardData = response.data;
            this.showSuccess('Dashboard loaded successfully');
          }
        },
        error: (error) => {
          console.error('Error loading dashboard:', error);
          this.showError('Failed to load dashboard: ' + (error.error?.message || error.message));
        }
      });
      
    // Load dashboard summary
    this.aiRiskService.getDashboardSummary(30)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          if (response.success) {
            this.dashboardSummary = response;
          }
        },
        error: (error) => {
          console.error('Error loading dashboard summary:', error);
        }
      });
  }
  
  loadDashboardSummary(days: number = 30): void {
    this.aiRiskService.getDashboardSummary(days)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          if (response.success) {
            this.dashboardSummary = response;
          }
        },
        error: (error) => {
          console.error('Error loading dashboard summary:', error);
        }
      });
  }

  // ==================== UPCOMING INVOICES METHODS ====================
  
  loadUpcomingInvoices(): void {
    this.loading.upcoming = true;
    this.errorMessage = null;
    const days = this.upcomingDaysForm.get('days')?.value || 6;
    
    this.aiRiskService.getUpcomingInvoices(days)
      .pipe(
        finalize(() => this.loading.upcoming = false),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (response) => {
          if (response.success) {
            this.upcomingInvoices = response.invoices;
            this.upcomingSummary = response.summary;
            this.activeTab = 'upcoming';
            this.showSuccess(`Loaded ${response.invoices.length} upcoming invoices`);
          }
        },
        error: (error) => {
          console.error('Error loading upcoming invoices:', error);
          this.showError('Failed to load upcoming invoices: ' + (error.error?.message || error.message));
        }
      });
  }
  
  refreshUpcomingInvoices(): void {
    this.loading.refreshUpcoming = true;
    this.errorMessage = null;
    const days = this.upcomingDaysForm.get('days')?.value || 6;
    
    this.aiRiskService.refreshUpcomingPredictions(days)
      .pipe(
        finalize(() => this.loading.refreshUpcoming = false),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (response) => {
          if (response.success) {
            this.upcomingInvoices = response.invoices;
            this.showSuccess(`Refreshed ${response.totalRefreshed} predictions`);
            this.loadDashboardSummary(days);
          }
        },
        error: (error) => {
          console.error('Error refreshing predictions:', error);
          this.showError('Failed to refresh predictions: ' + (error.error?.message || error.message));
        }
      });
  }
  
  getRiskBadgeClass(riskLevel: string): string {
    if (riskLevel.includes('CRITICAL') || riskLevel.includes('Critique')) return 'badge-critical';
    if (riskLevel.includes('VERY_HIGH') || riskLevel.includes('Très Élevé')) return 'badge-very-high';
    if (riskLevel.includes('HIGH') || riskLevel.includes('Élevé')) return 'badge-high';
    if (riskLevel.includes('MEDIUM') || riskLevel.includes('Moyen')) return 'badge-medium';
    if (riskLevel.includes('LOW') || riskLevel.includes('Faible')) return 'badge-low';
    return 'badge-very-low';
  }

  // ==================== PREDICTION METHODS ====================
  
  predictInvoiceRisk(): void {
    if (this.predictionForm.invalid) {
      this.showError('Please enter an invoice number');
      return;
    }
    
    this.loading.prediction = true;
    this.errorMessage = null;
    const numeroFacture = this.predictionForm.get('numeroFacture')?.value;
    
    this.aiRiskService.predictInvoiceRisk(numeroFacture)
      .pipe(
        finalize(() => this.loading.prediction = false),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (response) => {
          if (response.success) {
            this.currentPrediction = response.prediction;
            this.selectedFactureNumero = numeroFacture;
            this.activeTab = 'predictions';
            this.showSuccess('Prediction completed successfully');
          }
        },
        error: (error) => {
          console.error('Error predicting risk:', error);
          this.showError(error.error?.message || 'Failed to predict risk');
        }
      });
  }
  
  predictNewInvoice(): void {
    if (this.newInvoiceForm.invalid) {
      this.showError('Please fill all required fields');
      return;
    }
    
    this.loading.prediction = true;
    this.errorMessage = null;
    const { clientCode, amount, dueDate, applicationCode } = this.newInvoiceForm.value;
    
    this.aiRiskService.predictNewInvoiceRisk(amount, dueDate, clientCode, applicationCode || undefined)
      .pipe(
        finalize(() => this.loading.prediction = false),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (response) => {
          if (response.success) {
            this.currentPrediction = response.prediction;
            this.activeTab = 'predictions';
            this.newInvoiceForm.reset();
            this.showSuccess('New invoice prediction completed');
          }
        },
        error: (error) => {
          console.error('Error predicting new invoice risk:', error);
          this.showError(error.error?.message || 'Failed to predict new invoice risk');
        }
      });
  }

  // ==================== BATCH PREDICTION METHODS ====================
  
  loadClientBatchPrediction(): void {
    if (this.batchForm.invalid) {
      this.showError('Please enter a client code');
      return;
    }
    
    this.loading.batch = true;
    this.errorMessage = null;
    const clientCode = this.batchForm.get('clientCode')?.value;
    
    this.aiRiskService.predictClientRisk(clientCode)
      .pipe(
        finalize(() => this.loading.batch = false),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (response) => {
          if (response.success) {
            this.batchResults = response;
            this.selectedClientCode = clientCode;
            this.activeTab = 'batch';
            this.showSuccess(`Loaded ${response.statistics.totalInvoices} invoices for client`);
          }
        },
        error: (error) => {
          console.error('Error in batch prediction:', error);
          this.showError(error.error?.message || 'Failed to load batch predictions');
        }
      });
  }

  // ==================== RECOMMENDATION METHODS ====================
  
  loadInvoiceRecommendations(): void {
    if (!this.selectedFactureNumero) {
      this.showError('Please predict an invoice first');
      return;
    }
    
    this.loading.recommendations = true;
    this.errorMessage = null;
    
    this.aiRiskService.getInvoiceRecommendations(this.selectedFactureNumero)
      .pipe(
        finalize(() => this.loading.recommendations = false),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (response) => {
          if (response.success) {
            this.invoiceRecommendations = response.recommendations;
            this.activeTab = 'recommendations';
            this.showSuccess(`Loaded ${response.count} recommendations`);
          }
        },
        error: (error) => {
          console.error('Error loading recommendations:', error);
          this.showError('Failed to load recommendations: ' + (error.error?.message || error.message));
        }
      });
  }
  
  loadClientRecommendations(): void {
    if (this.clientRecForm.invalid) {
      this.showError('Please enter a client code');
      return;
    }
    
    this.loading.recommendations = true;
    this.errorMessage = null;
    const clientCode = this.clientRecForm.get('clientCode')?.value;
    
    this.aiRiskService.getClientRecommendations(clientCode)
      .pipe(
        finalize(() => this.loading.recommendations = false),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (response) => {
          if (response.success) {
            this.clientRecommendations = response.recommendations;
            this.selectedClientCode = clientCode;
            this.activeTab = 'recommendations';
            this.showSuccess(`Loaded ${response.count} client recommendations`);
          }
        },
        error: (error) => {
          console.error('Error loading client recommendations:', error);
          this.showError('Failed to load client recommendations: ' + (error.error?.message || error.message));
        }
      });
  }

  // ==================== ADMIN METHODS ====================
  
  retrainModel(): void {
    if (!confirm('This will retrain the AI model with the latest data. This may take a few minutes. Continue?')) {
      return;
    }
    
    this.loading.retrain = true;
    this.errorMessage = null;
    
    this.aiRiskService.retrainModel()
      .pipe(
        finalize(() => this.loading.retrain = false),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (response) => {
          if (response.success) {
            this.showSuccess('Model retrained successfully');
            this.loadDashboard();
            this.loadUpcomingInvoices();
          }
        },
        error: (error) => {
          console.error('Error retraining model:', error);
          this.showError('Failed to retrain model: ' + (error.error?.message || error.message));
        }
      });
  }
  
  forceRetrainModel(): void {
    if (!confirm('⚠️ FORCE RETRAIN: This will completely rebuild the neural network and retrain with all data. This may take several minutes. Continue?')) {
      return;
    }
    
    this.loading.retrain = true;
    this.errorMessage = null;
    
    this.aiRiskService.forceRetrain()
      .pipe(
        finalize(() => this.loading.retrain = false),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (response) => {
          if (response.success) {
            this.showSuccess('Model force-retrained successfully');
            this.loadDashboard();
            this.loadUpcomingInvoices();
          }
        },
        error: (error) => {
          console.error('Error force retraining model:', error);
          this.showError('Failed to force retrain model: ' + (error.error?.message || error.message));
        }
      });
  }
  
  clearCache(): void {
    if (!confirm('This will clear all AI prediction caches. Continue?')) {
      return;
    }
    
    this.aiRiskService.clearCache()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          if (response.success) {
            this.showSuccess('Cache cleared successfully');
          }
        },
        error: (error) => {
          console.error('Error clearing cache:', error);
          this.showError('Failed to clear cache: ' + (error.error?.message || error.message));
        }
      });
  }
  
  loadModelInfo(): void {
    this.loading.modelInfo = true;
    this.errorMessage = null;
    
    this.aiRiskService.getModelInfo()
      .pipe(
        finalize(() => this.loading.modelInfo = false),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (response) => {
          if (response.success) {
            this.modelInfo = response.data;
            this.activeTab = 'admin';
            this.showSuccess('Model info loaded');
          }
        },
        error: (error) => {
          console.error('Error loading model info:', error);
          this.showError('Failed to load model info: ' + (error.error?.message || error.message));
        }
      });
  }
  
  checkHealth(): void {
    this.aiRiskService.healthCheck()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          console.log('AI Risk Service Health:', response);
          if (response.success) {
            this.showSuccess('AI Risk Service is healthy');
          }
        },
        error: (error) => {
          console.error('AI Risk Service is not responding:', error);
          this.showError('AI Risk Service is not responding');
        }
      });
  }
  
  // ==================== DEBUG METHODS ====================
  
  debugCurrentInvoice(): void {
    if (!this.selectedFactureNumero) {
      this.showError('Please predict an invoice first');
      return;
    }
    
    this.aiRiskService.debugFeatures(this.selectedFactureNumero)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          console.log('Debug features:', data);
          this.showInfo('Debug info logged to console');
          alert(JSON.stringify(data, null, 2));
        },
        error: (error) => {
          console.error('Error getting debug info:', error);
          this.showError('Failed to get debug info: ' + (error.error?.message || error.message));
        }
      });
  }
  
  debugRawPrediction(): void {
    if (!this.selectedFactureNumero) {
      this.showError('Please predict an invoice first');
      return;
    }
    
    this.aiRiskService.debugRawPrediction(this.selectedFactureNumero)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          console.log('Raw prediction debug:', data);
          this.showInfo('Raw prediction debug logged to console');
          alert(JSON.stringify(data, null, 2));
        },
        error: (error) => {
          console.error('Error getting raw prediction:', error);
          this.showError('Failed to get raw prediction: ' + (error.error?.message || error.message));
        }
      });
  }
  
  testModelLearning(): void {
    this.aiRiskService.testModelLearning()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          console.log('Model test results:', data);
          const message = `Actual: ${data.actualRiskScore}, Predicted: ${data.predictedRiskScore}, Error: ${data.error.toFixed(2)} - ${data.recommendation}`;
          this.showInfo(message);
          alert(message);
        },
        error: (error) => {
          console.error('Error testing model:', error);
          this.showError('Failed to test model: ' + (error.error?.message || error.message));
        }
      });
  }
  
  viewTrainingData(): void {
    this.aiRiskService.debugTrainingData()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          console.log('Training data stats:', data);
          this.showInfo('Training data stats logged to console');
          alert(JSON.stringify(data, null, 2));
        },
        error: (error) => {
          console.error('Error getting training data:', error);
          this.showError('Failed to get training data: ' + (error.error?.message || error.message));
        }
      });
  }

  // ==================== UTILITY METHODS ====================
  
  getRiskColor(level: string): string {
    return this.riskColors[level as keyof typeof this.riskColors] || '#6b7280';
  }
  
  getRiskColorFromCode(levelCode: number): string {
    const levelMap: { [key: number]: string } = {
      0: 'VERY_LOW',
      1: 'LOW',
      2: 'MEDIUM',
      3: 'HIGH',
      4: 'VERY_HIGH',
      5: 'CRITICAL'
    };
    return this.getRiskColor(levelMap[levelCode]);
  }
  
  formatPercentage(value: number): string {
    return `${(value * 100).toFixed(1)}%`;
  }
  
  formatCurrency(value: number): string {
    return new Intl.NumberFormat('fr-TN', { style: 'currency', currency: 'TND' }).format(value);
  }
  
  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('fr-FR');
  }
  
  getSeverityText(severity: number): string {
    switch(severity) {
      case 5: return 'Critical';
      case 4: return 'Very High';
      case 3: return 'High';
      case 2: return 'Medium';
      case 1: return 'Low';
      default: return 'Very Low';
    }
  }
  
  getPriorityIcon(priority: string): string {
    switch(priority.toLowerCase()) {
      case 'urgent': return '🔴';
      case 'high': return '🟠';
      case 'medium': return '🟡';
      case 'low': return '🟢';
      default: return '⚪';
    }
  }
  
  switchTab(tab: 'dashboard' | 'upcoming' | 'predictions' | 'recommendations' | 'batch' | 'admin'): void {
    this.activeTab = tab;
    this.errorMessage = null;
    this.successMessage = null;
    if (tab === 'dashboard') {
      this.loadDashboard();
    } else if (tab === 'upcoming') {
      this.loadUpcomingInvoices();
    }
  }


getAmountByRisk(riskLevel: string): number {
  if (!this.dashboardSummary?.summary?.amountByRisk) {
    return 0;
  }
  const amountByRisk = this.dashboardSummary.summary.amountByRisk;
  // Map the risk level to the correct key
  const keyMap: { [key: string]: string } = {
    'CRITICAL': 'CRITICAL',
    'VERY_HIGH': 'VERY_HIGH', 
    'HIGH': 'HIGH',
    'MEDIUM': 'MEDIUM',
    'LOW': 'LOW',
    'VERY_LOW': 'VERY_LOW'
  };
  const mappedKey = keyMap[riskLevel] || riskLevel;
  return (amountByRisk as any)[mappedKey] || 0;
}
}