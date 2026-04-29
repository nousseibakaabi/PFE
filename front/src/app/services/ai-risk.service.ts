// src/app/services/ai-risk.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface RiskLevel {
  code: number;
  label: string;
  color: string;
  severity: number;
}

export interface FeatureContributions {
  [key: string]: number;
}

export interface AIRecommendation {
  id?: string;
  title?: string;
  description: string;
  priority: string;
  dueDate?: string;
  action?: string;
  generatedAt?: string;
  executed?: boolean;
  executedAt?: string;
  executedBy?: string;
}

export interface RiskPrediction {
  level: RiskLevel;
  probability: number;
  confidence: number;
  predictedDelayDays: number;
  featureContributions: FeatureContributions;
  recommendations: string[];
  explanation: string;
  predictionDate?: string;
}

export interface BatchRiskResponse {
  success: boolean;
  statistics: {
    totalInvoices: number;
    highRiskCount: number;
    criticalCount: number;
    veryHighCount: number;
    highCount: number;
    mediumCount: number;
    lowCount: number;
    averageRiskScore: number;
    totalPredictedDelay: number;
    averageConfidence: number;
  };
  predictions: RiskPrediction[];
}

export interface DashboardData {
  totalClientsAnalyzed: number;
  criticalClients: number;
  highRiskClients: number;
  totalOverdueAmount: number;
  averageRiskScore: number;
}

export interface ModelInfo {
  modelType: string;
  inputFeatures: number;
  hiddenLayers: number;
  riskLevels: number;
  lastTrainingDate: string;
  status: string;
}

export interface UpcomingInvoice {
  id: number;
  invoiceNumber: string;
  dueDate: string;
  daysUntilDue: number;
  isOverdue: boolean;
  overdueDays: number;
  amount: number;
  status: string;
  riskLevel: string;
  riskLevelCode: number;
  riskColor: string;
  riskSeverity: number;
  riskScore: number;
  probability: number;
  confidence: number;
  predictedDelayDays: number;
  recommendations: string[];
  clientId?: number;
  clientName?: string;
  clientEmail?: string;
  clientPhone?: string;
}

export interface UpcomingInvoicesResponse {
  success: boolean;
  summary: {
    totalInvoices: number;
    criticalCount: number;
    highRiskCount: number;
    mediumRiskCount: number;
    totalOverdueAmount: number;
  };
  invoices: UpcomingInvoice[];
  generatedAt: string;
}

export interface DashboardSummaryResponse {
  success: boolean;
  summary: {
    riskLevelCounts: {
      CRITICAL: number;
      VERY_HIGH: number;
      HIGH: number;
      MEDIUM: number;
      LOW: number;
      VERY_LOW: number;
    };
    amountByRisk: {
      CRITICAL: number;
      VERY_HIGH: number;
      HIGH: number;
      MEDIUM: number;
      LOW: number;
      VERY_LOW: number;
    };
    totalInvoices: number;
    totalAmount: number;
    days: number;
  };
}

export interface DebugFeaturesResponse {
  invoiceId: number;
  invoiceNumber: string;
  status: string;
  dueDate: string;
  daysUntilDue: number;
  paymentOnTimeRate: number;
  latePaymentRate: number;
  advancePaymentRate: number;
  riskScore: number;
  riskLevel: number;
  delayDays: number;
  totalConventions: number;
  clientAge: number;
  clientCode: string | null;
}

export interface DebugRawPredictionResponse {
  invoiceId: number;
  invoiceNumber: string;
  calculatedRiskScore: number;
  calculatedRiskLevel: number;
  calculatedRiskLevelName: string;
  features: {
    paymentOnTimeRate: number;
    latePaymentRate: number;
    advancePaymentRate: number;
    daysUntilDue: number;
    clientAge: number;
    totalConventions: number;
    delayDays: number;
  };
  rawNNOutput_score: number;
  rawNNOutput_delay: number;
  classifierProbabilities: {
    VERY_LOW: number;
    LOW: number;
    MEDIUM: number;
    HIGH: number;
    VERY_HIGH: number;
    CRITICAL: number;
  };
  predictedLevel: string;
  predictedLevelCode: number;
}

export interface TrainingDataResponse {
  totalInvoices: number;
  riskLevelCounts: { [key: number]: number };
  riskScoreStatistics: {
    [key: string]: {
      count: number;
      min: number;
      max: number;
      avg: number;
    };
  };
}

export interface ModelTestResponse {
  actualRiskScore: number;
  predictedRiskScore: number;
  error: number;
  isAccurate: boolean;
  recommendation: string;
}

@Injectable({
  providedIn: 'root'
})
export class AIRiskService {
  private baseUrl = `${environment.apiUrl}/api/ai-risk`;

  constructor(private http: HttpClient) {}

  // ==================== PREDICTION ENDPOINTS ====================

  /**
   * Predict risk for an existing invoice by invoice number
   * GET /api/ai-risk/predict/invoice/{numeroFacture}
   */
  predictInvoiceRisk(numeroFacture: string): Observable<{ success: boolean; prediction: RiskPrediction }> {
    return this.http.get<{ success: boolean; prediction: RiskPrediction }>(
      `${this.baseUrl}/predict/invoice/${numeroFacture}`
    );
  }

  /**
   * Predict risk for a new invoice (before creation)
   * POST /api/ai-risk/predict/new-invoice
   */
  predictNewInvoiceRisk(
    amount: number,
    dueDate: string,
    clientCode: string,
    applicationCode?: string
  ): Observable<{ success: boolean; prediction: RiskPrediction; recommendations: string[] }> {
    let params = new HttpParams()
      .set('amount', amount.toString())
      .set('dueDate', dueDate)
      .set('clientCode', clientCode);
    
    if (applicationCode) {
      params = params.set('applicationCode', applicationCode);
    }

    return this.http.post<{ success: boolean; prediction: RiskPrediction; recommendations: string[] }>(
      `${this.baseUrl}/predict/new-invoice`,
      null,
      { params }
    );
  }

  /**
   * Batch prediction for all unpaid invoices of a client by client code
   * GET /api/ai-risk/batch/client/{clientCode}
   */
  predictClientRisk(clientCode: string): Observable<BatchRiskResponse> {
    return this.http.get<BatchRiskResponse>(`${this.baseUrl}/batch/client/${clientCode}`);
  }

  // ==================== RECOMMENDATION ENDPOINTS ====================

  /**
   * Get AI recommendations for an invoice by invoice number
   * GET /api/ai-risk/recommendations/invoice/{numeroFacture}
   */
  getInvoiceRecommendations(numeroFacture: string): Observable<{ success: boolean; recommendations: AIRecommendation[]; count: number }> {
    return this.http.get<{ success: boolean; recommendations: AIRecommendation[]; count: number }>(
      `${this.baseUrl}/recommendations/invoice/${numeroFacture}`
    );
  }

  /**
   * Get strategic recommendations for a client by client code
   * GET /api/ai-risk/recommendations/client/{clientCode}
   */
  getClientRecommendations(clientCode: string): Observable<{ success: boolean; recommendations: AIRecommendation[]; count: number }> {
    return this.http.get<{ success: boolean; recommendations: AIRecommendation[]; count: number }>(
      `${this.baseUrl}/recommendations/client/${clientCode}`
    );
  }

  // ==================== DASHBOARD & STATISTICS ENDPOINTS ====================

  /**
   * Get risk dashboard for all clients
   * GET /api/ai-risk/dashboard
   */
  getRiskDashboard(): Observable<{ success: boolean; data: DashboardData }> {
    return this.http.get<{ success: boolean; data: DashboardData }>(`${this.baseUrl}/dashboard`);
  }

  /**
   * Get high risk invoices for the current user
   * GET /api/ai-risk/my-high-risk-invoices
   */
  getMyHighRiskInvoices(minLevel: string = 'MEDIUM'): Observable<{ success: boolean; message?: string; minLevel: string; predictions?: RiskPrediction[] }> {
    const params = new HttpParams().set('minLevel', minLevel);
    return this.http.get<{ success: boolean; message?: string; minLevel: string; predictions?: RiskPrediction[] }>(
      `${this.baseUrl}/my-high-risk-invoices`,
      { params }
    );
  }

  /**
   * Get all high-risk clients (for management)
   * GET /api/ai-risk/high-risk-clients
   */
  getHighRiskClients(minLevel: string = 'HIGH'): Observable<{ success: boolean; message?: string; minLevel: string; clients?: any[] }> {
    const params = new HttpParams().set('minLevel', minLevel);
    return this.http.get<{ success: boolean; message?: string; minLevel: string; clients?: any[] }>(
      `${this.baseUrl}/high-risk-clients`,
      { params }
    );
  }

  /**
   * Get all upcoming invoices due in the next X days with AI risk predictions
   * GET /api/ai-risk/upcoming-invoices?days=6
   */
  getUpcomingInvoices(days: number = 6): Observable<UpcomingInvoicesResponse> {
    const params = new HttpParams().set('days', days.toString());
    return this.http.get<UpcomingInvoicesResponse>(`${this.baseUrl}/upcoming-invoices`, { params });
  }

  /**
   * Refresh predictions for all upcoming invoices
   * POST /api/ai-risk/refresh-upcoming?days=6
   */
  refreshUpcomingPredictions(days: number = 6): Observable<{ success: boolean; message: string; totalRefreshed: number; invoices: UpcomingInvoice[] }> {
    const params = new HttpParams().set('days', days.toString());
    return this.http.post<{ success: boolean; message: string; totalRefreshed: number; invoices: UpcomingInvoice[] }>(
      `${this.baseUrl}/refresh-upcoming`,
      null,
      { params }
    );
  }

  /**
   * Get dashboard summary with counts by risk level
   * GET /api/ai-risk/dashboard-summary?days=6
   */
  getDashboardSummary(days: number = 6): Observable<DashboardSummaryResponse> {
    const params = new HttpParams().set('days', days.toString());
    return this.http.get<DashboardSummaryResponse>(`${this.baseUrl}/dashboard-summary`, { params });
  }

  // ==================== ADMIN ENDPOINTS ====================

  /**
   * Force retrain the AI model
   * POST /api/ai-risk/retrain
   */
  retrainModel(): Observable<{ success: boolean; message: string }> {
    return this.http.post<{ success: boolean; message: string }>(`${this.baseUrl}/retrain`, {});
  }

  /**
   * Force retrain with full rebuild
   * POST /api/ai-risk/force-retrain
   */
  forceRetrain(): Observable<{ success: boolean; message: string }> {
    return this.http.post<{ success: boolean; message: string }>(`${this.baseUrl}/force-retrain`, {});
  }

  /**
   * Clear AI prediction cache
   * DELETE /api/ai-risk/cache
   */
  clearCache(): Observable<{ success: boolean; message: string }> {
    return this.http.delete<{ success: boolean; message: string }>(`${this.baseUrl}/cache`);
  }

  /**
   * Get model information and statistics
   * GET /api/ai-risk/model-info
   */
  getModelInfo(): Observable<{ success: boolean; data: ModelInfo }> {
    return this.http.get<{ success: boolean; data: ModelInfo }>(`${this.baseUrl}/model-info`);
  }

  // ==================== TEST/DEBUG ENDPOINTS ====================

  /**
   * Test endpoint to verify AI model is working
   * GET /api/ai-risk/health
   */
  healthCheck(): Observable<{ success: boolean; status: string; timestamp: string }> {
    return this.http.get<{ success: boolean; status: string; timestamp: string }>(`${this.baseUrl}/health`);
  }

  /**
   * Debug features for an invoice by invoice number
   * GET /api/ai-risk/debug/features/{numeroFacture}
   */
  debugFeatures(numeroFacture: string): Observable<DebugFeaturesResponse> {
    return this.http.get<DebugFeaturesResponse>(`${this.baseUrl}/debug/features/${numeroFacture}`);
  }

  /**
   * Debug raw prediction output for an invoice
   * GET /api/ai-risk/debug/predict-raw/{numeroFacture}
   */
  debugRawPrediction(numeroFacture: string): Observable<DebugRawPredictionResponse> {
    return this.http.get<DebugRawPredictionResponse>(`${this.baseUrl}/debug/predict-raw/${numeroFacture}`);
  }

  /**
   * Debug training data statistics
   * GET /api/ai-risk/debug/training-data
   */
  debugTrainingData(): Observable<TrainingDataResponse> {
    return this.http.get<TrainingDataResponse>(`${this.baseUrl}/debug/training-data`);
  }

  /**
   * Test model learning performance
   * GET /api/ai-risk/debug/model-test
   */
  testModelLearning(): Observable<ModelTestResponse> {
    return this.http.get<ModelTestResponse>(`${this.baseUrl}/debug/model-test`);
  }
}