import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ConventionService } from '../../services/convention.service';
import { Location } from '@angular/common';

// Define an interface for the version data
interface VersionData {
  oldConvention: any;
  oldFactures: any[];
}

@Component({
  selector: 'app-convention-versions',
  templateUrl: './convention-versions.component.html',
  styleUrls: ['./convention-versions.component.css'],
  standalone: false
})
export class ConventionVersionsComponent implements OnInit {
  conventionId!: number;
  currentConvention: any = null;
  previousVersions: VersionData[] = [];
  selectedVersion: any = null;
  selectedVersionFactures: any[] = [];

  // Modal state for full invoice view
  showInvoiceModal = false;
  modalFactures: any[] = [];
  modalVersionTitle = '';
  
  loading = true;
  errorMessage = '';
  
  // For UI state
  isGridView = true;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private conventionService: ConventionService,
    private location: Location
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.conventionId = +params['id'];
      this.loadCurrentConvention();
      this.loadPreviousVersions();
    });
  }

  loadCurrentConvention(): void {
    this.conventionService.getConvention(this.conventionId).subscribe({
      next: (response) => {
        if (response.success) {
          this.currentConvention = response.data;
        }
      },
      error: (error) => {
        console.error('Error loading current convention:', error);
        this.errorMessage = 'Erreur lors du chargement de la convention actuelle';
      }
    });
  }

  loadPreviousVersions(): void {
    this.loading = true;
    this.conventionService.getPreviousVersions(this.conventionId).subscribe({
      next: (response) => {
        if (response.success && response.data) {
          // Get all previous versions with proper typing
          const allVersions: VersionData[] = response.data;
          
          // Sort by version number descending (newest first)
          allVersions.sort((a: VersionData, b: VersionData) => 
            (b.oldConvention.renewalVersion || 0) - (a.oldConvention.renewalVersion || 0)
          );
          
          // IMPORTANT: Filter out the version that matches the current convention
          // This prevents duplication
          if (this.currentConvention) {
            this.previousVersions = allVersions.filter((v: VersionData) => 
              v.oldConvention.renewalVersion !== this.currentConvention.renewalVersion
            );
          } else {
            this.previousVersions = allVersions;
          }
          
          // Auto-select first version if available (now this will be v9, not v10)
          if (this.previousVersions.length > 0) {
            this.selectVersion(this.previousVersions[0]);
          }
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading previous versions:', error);
        this.errorMessage = 'Erreur lors du chargement des versions précédentes';
        this.loading = false;
      }
    });
  }

  selectVersion(version: VersionData): void {
    this.selectedVersion = version.oldConvention;
    this.selectedVersionFactures = version.oldFactures || [];
  }

  toggleViewMode(): void {
    this.isGridView = !this.isGridView;
    // If switching to grid view, clear selected version
    if (this.isGridView) {
      this.selectedVersion = null;
      this.selectedVersionFactures = [];
    }
  }

  goBack(): void {
    this.location.back();
  }

  viewCurrentVersion(): void {
    this.router.navigate(['/conventions', this.conventionId]);
  }

  formatDate(dateString: string): string {
    if (!dateString) return '-';
    const date = new Date(dateString);
    const day = date.getDate().toString().padStart(2, '0');
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const year = date.getFullYear();
    return `${day}/${month}/${year}`;
  }

  formatDateTime(dateString: string): string {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  formatMontant(montant: number): string {
    return new Intl.NumberFormat('fr-TN', { 
      minimumFractionDigits: 2, 
      maximumFractionDigits: 2 
    }).format(montant || 0);
  }

  getVersionBadgeClass(versionNumber: number): string {
    const colors = [
      'bg-purple-100 text-purple-800 border-purple-200',
      'bg-blue-100 text-blue-800 border-blue-200',
      'bg-green-100 text-green-800 border-green-200',
      'bg-amber-100 text-amber-800 border-amber-200',
      'bg-pink-100 text-pink-800 border-pink-200',
      'bg-indigo-100 text-indigo-800 border-indigo-200'
    ];
    
    return colors[(versionNumber - 1) % colors.length];
  }

  getEtatClass(etat: string | null): string {
    if (etat === null) return 'bg-gray-100 text-gray-800';
    
    switch (etat) {
      case 'PLANIFIE': return 'bg-yellow-100 text-yellow-800';
      case 'EN COURS': return 'bg-blue-100 text-blue-800';
      case 'TERMINE': return 'bg-green-100 text-green-800';
      case 'ARCHIVE': return 'bg-gray-100 text-gray-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  getEtatLabel(etat: string | null): string {
    if (etat === null) return '-';
    
    switch (etat) {
      case 'PLANIFIE': return 'Planifiée';
      case 'EN COURS': return 'En Cours';
      case 'TERMINE': return 'Terminée';
      case 'ARCHIVE': return 'Archivée';
      default: return etat;
    }
  }

  getInvoiceStatusClass(statut: string): string {
    switch (statut) {
      case 'PAYE': return 'bg-green-100 text-green-800';
      case 'NON_PAYE': return 'bg-red-100 text-red-800';
      case 'EN_RETARD': return 'bg-orange-100 text-orange-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  getInvoiceStatusLabel(statut: string): string {
    switch (statut) {
      case 'PAYE': return 'Payée';
      case 'NON_PAYE': return 'Non Payée';
      case 'EN_RETARD': return 'En Retard';
      default: return statut;
    }
  }

  getPeriodiciteIcon(periodicite: string): string {
    switch (periodicite) {
      case 'MENSUEL': return '📅';
      case 'TRIMESTRIEL': return '📆';
      case 'SEMESTRIEL': return '🗓️';
      case 'ANNUEL': return '📊';
      default: return '📋';
    }
  }


// Add this method to show full details in grid view
showFullDetails(version: any): void {
  // You could either:
  // 1. Switch to split view and select this version
  this.isGridView = false;
  this.selectedVersion = version;
  // Find the full version data with factures
  const fullVersion = this.previousVersions.find(v => v.oldConvention.id === version.id);
  if (fullVersion) {
    this.selectedVersionFactures = fullVersion.oldFactures || [];
  }
  
  // OR 2. Navigate to a dedicated details page
  // this.router.navigate(['/conventions/old', version.id]);
}


// Add this method to open the invoice modal instead of navigating away
viewAllInvoices(conventionId: number): void {
  const version = this.previousVersions.find(v => v.oldConvention.id === conventionId);
  if (version) {
    this.modalFactures = version.oldFactures || [];
    this.modalVersionTitle = version.oldConvention.referenceConvention || 'Détails Factures';
    this.showInvoiceModal = true;
  }
}

closeInvoiceModal(): void {
  this.showInvoiceModal = false;
  this.modalFactures = [];
  this.modalVersionTitle = '';
}
}