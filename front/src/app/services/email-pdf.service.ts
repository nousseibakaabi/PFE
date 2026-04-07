import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import jsPDF from 'jspdf';
import { Facture } from './facture.service';
import { Structure } from './nomenclature.service';

export interface SendEmailRequest {
  factureId: number;
  to: string;
  subject: string;
  message: string;
  pdfBase64?: string;
  isReminder?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class EmailPdfService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  sendEmailWithPDF(request: SendEmailRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/api/factures/send-email`, request);
  }

  async generatePDFBlob(facture: Facture, structureBeneficiel: Structure | null, isReminder: boolean = false): Promise<Blob> {
    const pdf = await this.generatePDF(facture, structureBeneficiel, isReminder);
    return pdf.output('blob');
  }

  async generatePDF(facture: Facture, structureBeneficiel: Structure | null, isReminder: boolean = false): Promise<jsPDF> {
    const pdf = new jsPDF({
      orientation: 'portrait',
      unit: 'mm',
      format: 'a4'
    });

    const primaryColor = [41, 128, 185];
    const secondaryColor = [52, 73, 94];
    const lightGray = [245, 245, 245];
    const borderColor = [220, 220, 220];

    pdf.setFont('helvetica');

    // Header with Logo
    try {
      await this.addLogoToPDF(pdf);
      this.addInvoiceContent(pdf, facture, structureBeneficiel, primaryColor, secondaryColor, lightGray, borderColor, isReminder);
    } catch (error) {
      this.addInvoiceContent(pdf, facture, structureBeneficiel, primaryColor, secondaryColor, lightGray, borderColor, isReminder);
    }

    return pdf;
  }

  private addInvoiceContent(pdf: jsPDF, facture: Facture, structureBeneficiel: Structure | null,
                            primaryColor: number[], secondaryColor: number[],
                            lightGray: number[], borderColor: number[], isReminder: boolean = false): void {
    
    let yPos = 36;

    // Add reminder warning if needed
    if (isReminder) {
      pdf.setFillColor(254, 242, 242);
      pdf.setDrawColor(248, 113, 113);
      pdf.roundedRect(20, yPos - 10, 173, 8, 3, 3, 'FD');
      pdf.setTextColor(185, 28, 28);
      pdf.setFontSize(9);
      pdf.setFont('helvetica', 'bold');
      pdf.text('⚠️ RELANCE DE PAIEMENT ⚠️', 105, yPos - 5, { align: 'center' });
    }

    pdf.setFontSize(12);
    pdf.setTextColor(primaryColor[0], primaryColor[1], primaryColor[2]);
    pdf.setFont('helvetica', 'bold');
    pdf.text(`FACTURE N° ${facture.numeroFacture || ''}`, 60, yPos + 4);

    let Z = 42;
    let y = 43;
    pdf.setFontSize(10);
    pdf.setTextColor(secondaryColor[0], secondaryColor[1], secondaryColor[2]);
    pdf.setFont('helvetica', 'bold');
    pdf.text('Société', Z, y + 13);

    pdf.setFontSize(8);
    pdf.setTextColor(secondaryColor[0], secondaryColor[1], secondaryColor[2]);
    pdf.setFont('helvetica', 'normal');
    pdf.text('Centre national de l\'informatique', Z, y + 19);
    pdf.text('17, 1005 Av. Belhassen Ben Chaabane ,Tunis', Z, y + 25);
    pdf.text('webcni@cni.tn', Z, y + 31);
    pdf.text('+216 71 781 862', Z, y + 37);

    let W = 130;
    pdf.setFontSize(10);
    pdf.setTextColor(secondaryColor[0], secondaryColor[1], secondaryColor[2]);
    pdf.setFont('helvetica', 'bold');
    pdf.text('Facturer à ', W, y + 13);

    pdf.setFontSize(8);
    pdf.setTextColor(secondaryColor[0], secondaryColor[1], secondaryColor[2]);
    pdf.setFont('helvetica', 'normal');
    pdf.text(facture.structureBeneficielName || 'N/A', W, y + 19);
    pdf.text(structureBeneficiel?.zoneGeographique?.name || 'N/A', W, y + 25);
    pdf.text(structureBeneficiel?.email || 'N/A', W, y + 31);
    pdf.text(this.formatPhoneNumber(structureBeneficiel?.phone || 'N/A'), W, y + 37);

    yPos = 90;

    pdf.setTextColor(secondaryColor[0], secondaryColor[1], secondaryColor[2]);
    pdf.setFontSize(9);
    pdf.setFont('helvetica', 'bold');
    pdf.text('Date facturation :', 129, yPos + 5);
    pdf.text('Date échéance :', 129, yPos + 10);
    pdf.text('Date création :', 129, yPos + 15);

    pdf.setFontSize(9);
    pdf.setFont('helvetica', 'normal');
    pdf.text(this.formatDate(facture.dateFacturation), 156, yPos + 5);
    pdf.text(this.formatDate(facture.dateEcheance), 154, yPos + 10);
    pdf.text(this.formatDate(facture.createdAt), 153, yPos + 15);

    yPos = 115;

    // Table headers
    pdf.setFillColor(primaryColor[0], primaryColor[1], primaryColor[2]);
    pdf.rect(20, yPos, 60, 8, 'F');
    pdf.rect(80, yPos, 35, 8, 'F');
    pdf.rect(115, yPos, 30, 8, 'F');
    pdf.rect(145, yPos, 48, 8, 'F');

    pdf.setTextColor(255, 255, 255);
    pdf.setFontSize(9);
    pdf.setFont('helvetica', 'bold');
    pdf.text('Description', 25, yPos + 5);
    pdf.text('Montant HT', 85, yPos + 5);
    pdf.text('TVA', 122, yPos + 5);
    pdf.text('Montant TTC', 155, yPos + 5);

    yPos += 8;
    pdf.setDrawColor(borderColor[0], borderColor[1], borderColor[2]);
    pdf.rect(20, yPos, 173, 20, 'S');

    pdf.setTextColor(secondaryColor[0], secondaryColor[1], secondaryColor[2]);
    pdf.setFontSize(10);
    pdf.setFont('helvetica', 'bold');
    const fullDesc = facture.notes || '';
    const shortDesc = fullDesc.substring(0, 12);
    pdf.text(shortDesc, 25, yPos + 7);

    pdf.setFontSize(10);
    pdf.setFont('helvetica', 'normal');
    pdf.text(this.formatMontantWithoutCurrency(facture.montantHT || 0), 100, yPos + 7, { align: 'right' });
    pdf.text(`${facture.tva || 0}%`, 130, yPos + 7, { align: 'center' });
    
    pdf.setFont('helvetica', 'bold');
    pdf.setTextColor(primaryColor[0], primaryColor[1], primaryColor[2]);
    pdf.text(this.formatMontantWithoutCurrency(facture.montantTTC || 0), 185, yPos + 7, { align: 'right' });

    yPos += 23;
    
    pdf.setFillColor(lightGray[0], lightGray[1], lightGray[2]);
    pdf.roundedRect(120, yPos, 73, 35, 3, 3, 'F');
    pdf.setDrawColor(borderColor[0], borderColor[1], borderColor[2]);
    pdf.roundedRect(120, yPos, 73, 35, 3, 3, 'S');

    pdf.setTextColor(secondaryColor[0], secondaryColor[1], secondaryColor[2]);
    pdf.setFontSize(10);
    pdf.setFont('helvetica', 'normal');
    pdf.text('Sous-total HT:', 125, yPos + 7);
    pdf.text(this.formatMontantWithoutCurrency(facture.montantHT || 0), 185, yPos + 7, { align: 'right' });
    pdf.text(`TVA (${facture.tva || 0}%):`, 125, yPos + 15);
    const tvaAmount = (facture.montantHT || 0) * (facture.tva || 0) / 100;
    pdf.text(this.formatMontantWithoutCurrency(tvaAmount), 185, yPos + 15, { align: 'right' });

    pdf.setLineWidth(0.5);
    pdf.line(125, yPos + 20, 190, yPos + 20);
    
    pdf.setFont('helvetica', 'bold');
    pdf.setFontSize(12);
    pdf.text('TOTAL TTC:', 125, yPos + 27);
    pdf.setTextColor(primaryColor[0], primaryColor[1], primaryColor[2]);
    pdf.text(this.formatMontantWithoutCurrency(facture.montantTTC || 0), 185, yPos + 27, { align: 'right' });

    pdf.setFontSize(8);
    pdf.setFont('helvetica', 'normal');
    pdf.setTextColor(secondaryColor[0], secondaryColor[1], secondaryColor[2]);
    const amountInWords = this.numberToWords(facture.montantTTC || 0);
    pdf.text(amountInWords, 22, yPos + 6);

    const paymentDetails = this.getPaymentStatusDetails(facture);

    if (facture.statutPaiement === 'PAYE') {
      yPos += 45;
      pdf.setFillColor(240, 253, 244);
      pdf.setDrawColor(187, 247, 208);
      pdf.roundedRect(20, yPos, 173, 25, 3, 3, 'FD');
      pdf.setTextColor(22, 163, 74);
      pdf.setFontSize(10);
      pdf.setFont('helvetica', 'bold');
      pdf.text(' PAIEMENT EFFECTUÉ', 25, yPos + 7);
      pdf.setFontSize(7);
      pdf.setFont('helvetica', 'normal');
      
      let detailY = yPos + 14;
      if (paymentDetails.type === 'early') {
        pdf.text(` Paiement anticipé: ${paymentDetails.days} jour${paymentDetails.days > 1 ? 's' : ''} avant`, 25, detailY);
      } else if (paymentDetails.type === 'late_paid') {
        pdf.text(` Paiement en retard: ${paymentDetails.days} jour${paymentDetails.days > 1 ? 's' : ''}`, 25, detailY);
      } else if (paymentDetails.type === 'ontime') {
        pdf.text(`Paiement à la date d'échéance`, 25, detailY);
      }
      pdf.text(`Réf: ${facture.referencePaiement || '-'}  |  Date: ${this.formatDate(facture.datePaiement)}`, 25, detailY + 5);
      yPos += 45;
    } else {
      yPos += 45;
      
      if (paymentDetails.type === 'overdue') {
        pdf.setFillColor(254, 242, 242);
        pdf.setDrawColor(248, 113, 113);
        pdf.roundedRect(20, yPos, 173, 18, 3, 3, 'FD');
        pdf.setTextColor(185, 28, 28);
        pdf.setFontSize(9);
        pdf.setFont('helvetica', 'bold');
        pdf.text(' FACTURE EN RETARD', 25, yPos + 6);
        pdf.setFontSize(7);
        pdf.setFont('helvetica', 'normal');
        pdf.text(paymentDetails.message, 25, yPos + 12);
        yPos += 45;
      } else if (paymentDetails.type === 'due_today') {
        pdf.setFillColor(255, 237, 213);
        pdf.setDrawColor(251, 146, 60);
        pdf.roundedRect(20, yPos, 173, 18, 3, 3, 'FD');
        pdf.setTextColor(194, 65, 12);
        pdf.setFontSize(9);
        pdf.setFont('helvetica', 'bold');
        pdf.text(' ÉCHÉANCE AUJOURD\'HUI', 25, yPos + 6);
        pdf.setFontSize(7);
        pdf.setFont('helvetica', 'normal');
        pdf.text('À payer aujourd\'hui', 25, yPos + 12);
        yPos += 45;
      } else if (paymentDetails.type === 'pending') {
        if (paymentDetails.days <= 3) {
          pdf.setFillColor(255, 237, 213);
          pdf.setDrawColor(251, 146, 60);
        } else {
          pdf.setFillColor(254, 249, 195);
          pdf.setDrawColor(253, 224, 71);
        }
        pdf.roundedRect(20, yPos, 173, 18, 3, 3, 'FD');
        pdf.setTextColor(0, 0, 0);
        pdf.setFontSize(9);
        pdf.setFont('helvetica', 'bold');
        if (paymentDetails.days <= 3) {
          pdf.text(' FACTURE URGENTE', 25, yPos + 6);
        } else {
          pdf.text(' FACTURE EN ATTENTE', 25, yPos + 6);
        }
        pdf.setFontSize(7);
        pdf.setFont('helvetica', 'normal');
        pdf.text(paymentDetails.message, 25, yPos + 12);
        yPos += 45;
      }
    }

    yPos = 260;
    pdf.setDrawColor(borderColor[0], borderColor[1], borderColor[2]);
    pdf.line(20, yPos, 190, yPos);
    
    pdf.setTextColor(150, 150, 150);
    pdf.setFontSize(8);
    pdf.setFont('helvetica', 'normal');
    pdf.text(`Générée automatiquement le ${new Date().toLocaleDateString('fr-FR')}`, 105, yPos + 5, { align: 'center' });
    pdf.setTextColor(primaryColor[0], primaryColor[1], primaryColor[2]);
    pdf.text('Merci de votre confiance', 105, yPos + 10, { align: 'center' });
  }

  private getPaymentStatusDetails(facture: Facture): { type: string; days: number; message: string; color: string } {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    const echeance = new Date(facture.dateEcheance);
    echeance.setHours(0, 0, 0, 0);
    
    const diffTime = echeance.getTime() - today.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    if (facture.statutPaiement === 'PAYE') {
      if (facture.datePaiement) {
        const paymentDate = new Date(facture.datePaiement);
        paymentDate.setHours(0, 0, 0, 0);
        
        if (paymentDate < echeance) {
          const daysEarly = Math.ceil((echeance.getTime() - paymentDate.getTime()) / (1000 * 60 * 60 * 24));
          return { type: 'early', days: daysEarly, message: `Paiement effectué ${daysEarly} jour${daysEarly > 1 ? 's' : ''} avant échéance`, color: 'green' };
        } else if (paymentDate > echeance) {
          const daysLate = Math.ceil((paymentDate.getTime() - echeance.getTime()) / (1000 * 60 * 60 * 24));
          return { type: 'late_paid', days: daysLate, message: `Paiement effectué avec ${daysLate} jour${daysLate > 1 ? 's' : ''} de retard`, color: 'orange' };
        } else {
          return { type: 'ontime', days: 0, message: 'Paiement effectué à la date d\'échéance', color: 'blue' };
        }
      }
      return { type: 'paid', days: 0, message: 'Paiement effectué', color: 'green' };
    } else {
      if (diffDays < 0) {
        const daysLate = Math.abs(diffDays);
        return { type: 'overdue', days: daysLate, message: `${daysLate} jour${daysLate > 1 ? 's' : ''} de retard`, color: 'red' };
      } else if (diffDays === 0) {
        return { type: 'due_today', days: 0, message: 'À payer aujourd\'hui', color: 'orange' };
      } else {
        return { type: 'pending', days: diffDays, message: `${diffDays} jour${diffDays > 1 ? 's' : ''} restants`, color: 'yellow' };
      }
    }
  }

  private async addLogoToPDF(pdf: jsPDF): Promise<void> {
    return new Promise((resolve, reject) => {
      const img = new Image();
      img.crossOrigin = 'anonymous';
      img.onload = () => {
        const canvas = document.createElement('canvas');
        canvas.width = img.width;
        canvas.height = img.height;
        const ctx = canvas.getContext('2d');
        ctx?.drawImage(img, 0, 0);
        const imgData = canvas.toDataURL('image/png');
        try {
          pdf.addImage(imgData, 'PNG', 20, 15, 25, 15);
          resolve();
        } catch (e) {
          reject(e);
        }
      };
      img.onerror = reject;
      img.src = '/assets/logo.png';
    });
  }

  private formatDate(dateString: string): string {
    if (!dateString) return '-';
    const date = new Date(dateString);
    const day = date.getDate().toString().padStart(2, '0');
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const year = date.getFullYear();
    return `${day}/${month}/${year}`;
  }

  private formatPhoneNumber(phone: string): string {
    if (!phone) return '-';
    const cleaned = phone.replace(/\s+/g, '');
    if (cleaned.startsWith('+216')) {
      const number = cleaned.substring(4);
      const groups = number.match(/(\d{2})(\d{3})(\d{3})/);
      if (groups) {
        return `+216 ${groups[1]} ${groups[2]} ${groups[3]}`;
      }
    }
    const groups = cleaned.match(/(\d{2})(\d{3})(\d{3})/);
    if (groups) {
      return `${groups[1]} ${groups[2]} ${groups[3]}`;
    }
    return phone;
  }

  private formatMontantWithoutCurrency(montant: number): string {
    return new Intl.NumberFormat('fr-TN', { 
      minimumFractionDigits: 2, 
      maximumFractionDigits: 2 
    }).format(montant || 0);
  }

  private numberToWords(num: number): string {
    if (num === 0) return 'ZERO DINAR';
    
    const units = ['', 'UN', 'DEUX', 'TROIS', 'QUATRE', 'CINQ', 'SIX', 'SEPT', 'HUIT', 'NEUF', 'DIX'];
    const teens = ['DIX', 'ONZE', 'DOUZE', 'TREIZE', 'QUATORZE', 'QUINZE', 'SEIZE', 'DIX-SEPT', 'DIX-HUIT', 'DIX-NEUF'];
    const tens = ['', 'DIX', 'VINGT', 'TRENTE', 'QUARANTE', 'CINQUANTE', 'SOIXANTE', 'SOIXANTE-DIX', 'QUATRE-VINGT', 'QUATRE-VINGT-DIX'];
    
    const whole = Math.floor(num);
    const decimal = Math.round((num - whole) * 100);
    
    function convertLessThanThousand(n: number): string {
      if (n === 0) return '';
      let result = '';
      if (n >= 100) {
        const hundreds = Math.floor(n / 100);
        if (hundreds === 1) {
          result += 'CENT';
        } else {
          result += units[hundreds] + ' CENT';
        }
        n %= 100;
        if (n > 0) result += ' ';
      }
      if (n >= 70 && n < 80) {
        result += 'SOIXANTE';
        n -= 60;
        if (n === 11) {
          result += ' ET ONZE';
        } else if (n === 12) {
          result += '-DOUZE';
        } else {
          result += '-' + teens[n - 10];
        }
      } else if (n >= 90) {
        result += 'QUATRE-VINGT';
        n -= 80;
        if (n === 10) {
          result += '-DIX';
        } else if (n === 11) {
          result += '-ONZE';
        } else if (n === 12) {
          result += '-DOUZE';
        } else {
          result += '-' + teens[n - 10];
        }
      } else if (n >= 20) {
        const ten = Math.floor(n / 10);
        result += tens[ten];
        n %= 10;
        if (n === 1) {
          result += ' ET UN';
        } else if (n > 0) {
          result += ' ' + units[n];
        }
      } else if (n >= 10) {
        result += teens[n - 10];
      } else if (n > 0) {
        result += units[n];
      }
      return result;
    }
    
    function convert(n: number): string {
      if (n === 0) return '';
      let result = '';
      if (n >= 1000) {
        const thousands = Math.floor(n / 1000);
        if (thousands === 1) {
          result += 'MILLE ';
        } else {
          result += convertLessThanThousand(thousands) + ' MILLE ';
        }
        n %= 1000;
      }
      if (n > 0) {
        result += convertLessThanThousand(n);
      }
      return result.trim();
    }
    
    const wholeInWords = convert(whole);
    const dinarText = whole > 1 ? 'DINARS' : 'DINAR';
    
    function convertMillimes(n: number): string {
      if (n === 0) return 'ZERO MILLIME';
      function convertLessThanHundred(n: number): string {
        if (n === 0) return '';
        let result = '';
        if (n >= 70 && n < 80) {
          result += 'SOIXANTE';
          n -= 60;
          if (n === 11) {
            result += ' ET ONZE';
          } else if (n === 12) {
            result += '-DOUZE';
          } else {
            result += '-' + teens[n - 10];
          }
        } else if (n >= 90) {
          result += 'QUATRE-VINGT';
          n -= 80;
          if (n === 10) {
            result += '-DIX';
          } else if (n === 11) {
            result += '-ONZE';
          } else if (n === 12) {
            result += '-DOUZE';
          } else {
            result += '-' + teens[n - 10];
          }
        } else if (n >= 20) {
          const ten = Math.floor(n / 10);
          result += tens[ten];
          n %= 10;
          if (n === 1) {
            result += ' ET UN';
          } else if (n > 0) {
            result += '-' + units[n];
          }
        } else if (n >= 10) {
          result += teens[n - 10];
        } else if (n > 0) {
          result += units[n];
        }
        return result;
      }
      const result = convertLessThanHundred(n);
      return result + (n > 1 ? ' MILLIMES' : ' MILLIME');
    }
    
    let millimesText = decimal > 0 ? convertMillimes(decimal) : 'ZERO MILLIME';
    return `${wholeInWords} ${dinarText} ET ${millimesText}`;
  }
}