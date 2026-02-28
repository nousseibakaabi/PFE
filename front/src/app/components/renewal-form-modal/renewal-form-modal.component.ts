import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { Convention, ConventionService } from '../../services/convention.service';
import { RenewalRequest } from '../../services/request.service';

@Component({
  selector: 'app-renewal-form-modal',
  templateUrl: './renewal-form-modal.component.html',
  styleUrls: ['./renewal-form-modal.component.css']
})
export class RenewalFormModalComponent implements OnInit {
  @Input() convention: Convention | null = null;
  @Input() show = false;
  @Output() close = new EventEmitter<void>();
  @Output() renew = new EventEmitter<RenewalRequest>();

  formData: RenewalRequest = {
    referenceERP: '',
    libelle: '',
    dateDebut: '',
    dateFin: '',
    dateSignature: '',
    montantHT: 0,
    tva: 19,
    montantTTC: 0,
    nbUsers: 0,
    periodicite: 'MENSUEL'
  };

  periodicites = ['MENSUEL', 'TRIMESTRIEL', 'SEMESTRIEL', 'ANNUEL'];
  isCalculatingTTC = false;

  ngOnInit(): void {
    if (this.convention) {
      // Pre-fill with old convention data but allow modification
      this.formData = {
        referenceERP: this.convention.referenceERP || '',
        libelle: this.convention.libelle || '',
        dateDebut: this.convention.dateDebut || '',
        dateFin: this.convention.dateFin || '',
        dateSignature: this.convention.dateSignature || '',
        montantHT: this.convention.montantHT || 0,
        tva: this.convention.tva || 19,
        montantTTC: this.convention.montantTTC || 0,
        nbUsers: this.convention.nbUsers || 0,
        periodicite: this.convention.periodicite || 'MENSUEL'
      };
    }
  }

  onMontantHTChange(): void {
    this.calculateTTC();
  }

  onTvaChange(): void {
    this.calculateTTC();
  }

  calculateTTC(): void {
    if (this.formData.montantHT <= 0) {
      this.formData.montantTTC = 0;
      return;
    }

    const tva = this.formData.tva || 19;
    const tvaAmount = this.formData.montantHT * tva / 100;
    this.formData.montantTTC = this.formData.montantHT + tvaAmount;
  }

  validateForm(): boolean {
    if (!this.formData.libelle?.trim()) {
      alert('Libellé est requis');
      return false;
    }
    if (!this.formData.dateDebut) {
      alert('Date de début est requise');
      return false;
    }
    if (!this.formData.dateFin) {
      alert('Date de fin est requise');
      return false;
    }
    if (this.formData.montantHT <= 0) {
      alert('Montant HT doit être supérieur à 0');
      return false;
    }
    if (this.formData.nbUsers <= 0) {
      alert('Nombre d\'utilisateurs doit être supérieur à 0');
      return false;
    }
    return true;
  }

  onSubmit(): void {
    if (this.validateForm()) {
      this.renew.emit(this.formData);
    }
  }

  onClose(): void {
    this.close.emit();
  }
}