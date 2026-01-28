// convention-archive.component.ts
import { Component, OnInit } from '@angular/core';
import { ConventionService, Convention } from '../../services/convention.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';

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
  searchTerm = '';

  constructor(
    private conventionService: ConventionService,
    private authService: AuthService
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
      (conv.structureExterneName?.toLowerCase().includes(term)) ||
      (conv.zoneName?.toLowerCase().includes(term)) ||
      conv.archivedReason?.toLowerCase().includes(term) ||
      conv.archivedBy?.toLowerCase().includes(term)
    );
  }

  restoreConvention(id: number): void {
    if (confirm('Êtes-vous sûr de vouloir restaurer cette convention ?')) {
      this.loading = true;
      this.conventionService.restoreConvention(id).subscribe({
        next: (response) => {
          if (response.success) {
            this.loadArchivedConventions(); // Refresh the list
          }
          this.loading = false;
        },
        error: (error) => {
          console.error('Restore error:', error);
          this.errorMessage = error.error?.message || 'Échec de la restauration';
          this.loading = false;
        }
      });
    }
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


  
}