import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

export interface Nomenclature {
  id: number;
  code: string;
  name: string;
  description?: string;
  type?: string; 
  createdAt?: string;
  updatedAt?: string;
}

export interface Structure extends Nomenclature {
  phone?: string;
  email?: string;
  typeStructure?: string;
  zoneGeographique?: {  
    id: number;
    code: string;
    name: string;
  };
}

export interface NomenclatureResponse {
  success: boolean;
  message?: string;
  data?: any;
  count?: number;
}

@Injectable({
  providedIn: 'root'
})
export class NomenclatureService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient, private authService: AuthService) {}

  private getHeaders(): HttpHeaders {
    const token = this.authService.token;
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : ''
    });
  }



  // ==================== ZONES ====================
  getZones(): Observable<Nomenclature[]> {
    return this.http.get<NomenclatureResponse>(
      `${this.apiUrl}/admin/nomenclatures/zones`,
      { headers: this.getHeaders() }
    ).pipe(
      map(response => response.success ? response.data : []),
      catchError(this.handleError)
    );
  }

  getZone(id: number): Observable<Nomenclature> {
    return this.http.get<NomenclatureResponse>(
      `${this.apiUrl}/admin/nomenclatures/zones/${id}`,
      { headers: this.getHeaders() }
    ).pipe(
      map(response => response.data),
      catchError(this.handleError)
    );
  }

  createZone(zoneData: any): Observable<NomenclatureResponse> {
    return this.http.post<NomenclatureResponse>(
      `${this.apiUrl}/admin/nomenclatures/zones`,
      zoneData,
      { headers: this.getHeaders() }
    ).pipe(catchError(this.handleError));
  }

  updateZone(id: number, zoneData: any): Observable<NomenclatureResponse> {
    return this.http.put<NomenclatureResponse>(
      `${this.apiUrl}/admin/nomenclatures/zones/${id}`,
      zoneData,
      { headers: this.getHeaders() }
    ).pipe(catchError(this.handleError));
  }

  deleteZone(id: number): Observable<NomenclatureResponse> {
    return this.http.delete<NomenclatureResponse>(
      `${this.apiUrl}/admin/nomenclatures/zones/${id}`,
      { headers: this.getHeaders() }
    ).pipe(catchError(this.handleError));
  }

  getTunisianGovernorates(): Observable<Nomenclature[]> {
  return this.http.get<NomenclatureResponse>(
    `${this.apiUrl}/admin/nomenclatures/zones/tunisian-governorates`,
    { headers: this.getHeaders() }
  ).pipe(
    map(response => response.success ? response.data : []),
    catchError(this.handleError)
  );
  }

  getZonesByType(type: string): Observable<Nomenclature[]> {
    return this.http.get<NomenclatureResponse>(
      `${this.apiUrl}/admin/nomenclatures/zones/type/${type}`,
      { headers: this.getHeaders() }
    ).pipe(
      map(response => response.success ? response.data : []),
      catchError(this.handleError)
    );
  }

  // ==================== STRUCTURES ====================
  getStructures(): Observable<Structure[]> {
    return this.http.get<NomenclatureResponse>(
      `${this.apiUrl}/admin/nomenclatures/structures`,
      { headers: this.getHeaders() }
    ).pipe(
      map(response => response.success ? response.data : []),
      catchError(this.handleError)
    );
  }

  getStructure(id: number): Observable<Structure> {
    return this.http.get<NomenclatureResponse>(
      `${this.apiUrl}/admin/nomenclatures/structures/${id}`,
      { headers: this.getHeaders() }
    ).pipe(
      map(response => response.data),
      catchError(this.handleError)
    );
  }

  createStructure(structureData: any): Observable<NomenclatureResponse> {
    return this.http.post<NomenclatureResponse>(
      `${this.apiUrl}/admin/nomenclatures/structures`,
      structureData,
      { headers: this.getHeaders() }
    ).pipe(catchError(this.handleError));
  }

  updateStructure(id: number, structureData: any): Observable<NomenclatureResponse> {
    return this.http.put<NomenclatureResponse>(
      `${this.apiUrl}/admin/nomenclatures/structures/${id}`,
      structureData,
      { headers: this.getHeaders() }
    ).pipe(catchError(this.handleError));
  }

  deleteStructure(id: number): Observable<NomenclatureResponse> {
    return this.http.delete<NomenclatureResponse>(
      `${this.apiUrl}/admin/nomenclatures/structures/${id}`,
      { headers: this.getHeaders() }
    ).pipe(catchError(this.handleError));
  }


  generateClientCode(clientName: string): Observable<any> {
  return this.http.get<any>(
    `${this.apiUrl}/admin/nomenclatures/structures/generate-client-code`,
    { 
      headers: this.getHeaders(),
      params: { clientName: clientName }
    }
  ).pipe(
    catchError(this.handleError)
  );
  }

  // ==================== UTILS ====================
  getStats(): Observable<any> {
    return this.http.get<NomenclatureResponse>(
      `${this.apiUrl}/admin/nomenclatures/stats`,
      { headers: this.getHeaders() }
    ).pipe(
      map(response => response.data),
      catchError(this.handleError)
    );
  }

  private handleError(error: any): Observable<never> {
    console.error('An error occurred:', error);
    let errorMessage = 'Une erreur est survenue';
    
    if (error.error && error.error.message) {
      errorMessage = error.error.message;
    } else if (error.status === 401) {
      errorMessage = 'Session expirée, veuillez vous reconnecter';
    } else if (error.status === 403) {
      errorMessage = 'Accès non autorisé';
    }
    
    return throwError(() => new Error(errorMessage));
  }


  getResponsableStructures(): Observable<Structure[]> {
    return this.http.get<NomenclatureResponse>(
      `${this.apiUrl}/admin/nomenclatures/structures/responsables`,
      { headers: this.getHeaders() }
    ).pipe(
      map(response => {
        console.log('responsables structures response:', response); // Debug log
        return response.success ? response.data : [];
      }),
      catchError(this.handleError)
    );
  }

  getBeneficielStructures(): Observable<Structure[]> {
    return this.http.get<NomenclatureResponse>(
      `${this.apiUrl}/admin/nomenclatures/structures/beneficiels`,
      { headers: this.getHeaders() }
    ).pipe(
      map(response => {
        console.log('beneficiels structures response:', response); // Debug log
        return response.success ? response.data : [];
      }),
      catchError(this.handleError)
    );
  }
}