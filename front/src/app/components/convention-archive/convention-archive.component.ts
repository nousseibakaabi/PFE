// convention-archive.component.ts
import { Component, OnInit } from '@angular/core';
import { ConventionService, Convention } from '../../services/convention.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-convention-archive',
  templateUrl: './convention-archive.component.html',
  styleUrls: ['./convention-archive.component.css'],
  standalone: false
})
export class ConventionArchiveComponent implements OnInit {
  archivedConventions: Convention[] = [];
  filteredConventions: Convention[] = [];
  loading = false;
  errorMessage = '';
  successMessage = '';
  searchTerm = '';

  showRestoreModal = false;
  selectedConventionForRestore: any = null;
  restoreLoading = false;
  restoreErrorMessage = '';
  restoreSuccessMessage = '';

  constructor(
    private conventionService: ConventionService,
    private authService: AuthService,
    private router:Router
  ) {}

  ngOnInit(): void {
    this.loadArchivedConventions();
  }

loadArchivedConventions(): void {
    this.loading = true;
    this.conventionService.getArchivedConventions().subscribe({
      next: (response) => {
        if (response.success) {
          // The backend already filters by current user
          this.archivedConventions = response.data;
          this.filteredConventions = [...this.archivedConventions];
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading archived conventions:', error);
        this.errorMessage = 'Échec du chargement des conventions archivées';
        this.loading = false;
      }
    });
  }

  searchConventions(): void {
    if (!this.searchTerm.trim()) {
      this.filteredConventions = [...this.archivedConventions];
      return;
    }

    const term = this.searchTerm.toLowerCase();
    this.filteredConventions = this.archivedConventions.filter(conv =>
      conv.referenceConvention.toLowerCase().includes(term) ||
      conv.libelle.toLowerCase().includes(term) ||
      (conv.structureBeneficielName?.toLowerCase().includes(term)) ||
      (conv.structureResponsableName?.toLowerCase().includes(term)) ||
       (conv.zoneName?.toLowerCase().includes(term)) ||
      conv.archivedReason?.toLowerCase().includes(term) ||
      conv.archivedBy?.toLowerCase().includes(term)
    );
  }

  restoreConvention(id: number): Observable<any> {
    return this.conventionService.restoreConvention(id);
  }

  formatArchivedDate(dateString: string): string {
    if (!dateString) return 'Non spécifié';
    const date = new Date(dateString);
    return date.toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  truncateText(text: string, maxLength: number = 50): string {
    if (!text) return '';
    if (text.length <= maxLength) return text;
    return text.substring(0, maxLength) + '...';
  }


  viewConventionDetails(id: number): void {
  this.router.navigate(['/conventions', id]);
}


// Open restore modal
openRestoreModal(convention: any, event: Event): void {
  event.stopPropagation(); // Prevent card click
  this.selectedConventionForRestore = convention;
  this.showRestoreModal = true;
  this.restoreErrorMessage = '';
}

// Close restore modal
closeRestoreModal(): void {
  this.showRestoreModal = false;
  this.selectedConventionForRestore = null;
  this.restoreLoading = false;
  this.restoreErrorMessage = '';
}

// Confirm restore
confirmRestore(): void {
  if (!this.selectedConventionForRestore) return;
  
  this.restoreLoading = true;
  this.restoreErrorMessage = '';
  
  // Call your restore API
  this.restoreConvention(this.selectedConventionForRestore.id)
    .subscribe({
      next: (response) => {
        this.restoreLoading = false;
        this.closeRestoreModal();
        // Show success message
        this.successMessage = 'Convention restaurée avec succès';
        // Refresh your list
        this.loadArchivedConventions(); // or whatever your refresh method is
      },
      error: (error) => {
        this.restoreLoading = false;
        this.restoreErrorMessage = error.error?.message || 'Erreur lors de la restauration';
      }
    });
}

  
}