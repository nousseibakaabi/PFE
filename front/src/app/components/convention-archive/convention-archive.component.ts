import { Component, OnInit } from '@angular/core';
import { ConventionService, Convention } from '../../services/convention.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { TranslationService } from '../partials/traduction/translation.service';

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

  currentPage: number = 1;
  itemsPerPage: number = 2;

  constructor(
    private conventionService: ConventionService,
    private authService: AuthService,
    private router: Router,
    private translationService: TranslationService
  ) {}

  ngOnInit(): void {
    this.loadArchivedConventions();
  }

  loadArchivedConventions(): void {
    this.loading = true;
    this.conventionService.getArchivedConventions().subscribe({
      next: (response) => {
        if (response.success) {
          this.archivedConventions = response.data;
          this.filteredConventions = [...this.archivedConventions];
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading archived conventions:', error);
        this.errorMessage = this.translationService.translate('Échec du chargement des conventions archivées');
        this.loading = false;
      }
    });
  }

  get paginatedConventions(): any[] {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    return this.filteredConventions.slice(startIndex, endIndex);
  }

  get totalPages(): number {
    return Math.ceil(this.filteredConventions.length / this.itemsPerPage);
  }

  previousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
    }
  }

  getItemNumber(index: number): number {
    return (this.currentPage - 1) * this.itemsPerPage + index + 1;
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

    this.currentPage = 1;
  }

  restoreConvention(id: number): Observable<any> {
    return this.conventionService.restoreConvention(id);
  }

  formatArchivedDate(dateString: string): string {
    if (!dateString) return this.translationService.translate('Non spécifié');
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

  openRestoreModal(convention: any, event: Event): void {
    event.stopPropagation();
    this.selectedConventionForRestore = convention;
    this.showRestoreModal = true;
    this.restoreErrorMessage = '';
  }

  closeRestoreModal(): void {
    this.showRestoreModal = false;
    this.selectedConventionForRestore = null;
    this.restoreLoading = false;
    this.restoreErrorMessage = '';
  }

  confirmRestore(): void {
    if (!this.selectedConventionForRestore) return;
    
    this.restoreLoading = true;
    this.restoreErrorMessage = '';
    
    this.restoreConvention(this.selectedConventionForRestore.id)
      .subscribe({
        next: (response) => {
          this.restoreLoading = false;
          this.closeRestoreModal();
          this.successMessage = this.translationService.translate('Convention restaurée avec succès');
          this.loadArchivedConventions();
        },
        error: (error) => {
          this.restoreLoading = false;
          this.restoreErrorMessage = error.error?.message || this.translationService.translate('Erreur lors de la restauration');
        }
      });
  }
}