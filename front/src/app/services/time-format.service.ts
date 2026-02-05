
import { Injectable } from '@angular/core';
import { Facture } from './facture.service';

@Injectable({
  providedIn: 'root'
})
export class TimeFormatService {
  
  formatDaysToHumanReadable(days: number): string {
    if (days === 0) return '0 jour';
    
    const years = Math.floor(days / 365);
    const months = Math.floor((days % 365) / 30);
    const remainingDays = days % 30;
    
    const parts: string[] = [];
    
    if (years > 0) {
      parts.push(years + (years === 1 ? ' an' : ' ans'));
    }
    if (months > 0) {
      parts.push(months + (months === 1 ? ' mois' : ' mois'));
    }
    if (remainingDays > 0) {
      parts.push(remainingDays + (remainingDays === 1 ? ' jour' : ' jours'));
    }
    
    if (parts.length === 0) return '0 jour';
    
    // Join with " et " for French formatting
    if (parts.length === 1) return parts[0];
    if (parts.length === 2) return parts.join(' et ');
    return parts.slice(0, -1).join(', ') + ' et ' + parts[parts.length - 1];
  }
  
  formatPaymentStatus(facture: Facture): string {
    if (facture.statutPaiement === 'PAYE') {
      if (!facture.datePaiement || !facture.dateEcheance) return 'Payé';
      
      const paymentDate = new Date(facture.datePaiement);
      const dueDate = new Date(facture.dateEcheance);
      
      if (paymentDate < dueDate) {
        // Paid in advance
        const diffTime = dueDate.getTime() - paymentDate.getTime();
        const diffDays = Math.floor(diffTime / (1000 * 3600 * 24));
        return `Payé en avance de ${this.formatDaysToHumanReadable(diffDays)}`;
      } else if (paymentDate > dueDate) {
        // Paid late
        const diffTime = paymentDate.getTime() - dueDate.getTime();
        const diffDays = Math.floor(diffTime / (1000 * 3600 * 24));
        return `Payé avec ${this.formatDaysToHumanReadable(diffDays)} de retard`;
      } else {
        // Paid on due date
        return 'Payé à la date d\'échéance';
      }
    } else {
      // Unpaid
      if (!facture.dateEcheance) return 'Non payé';
      
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      
      const dueDate = new Date(facture.dateEcheance);
      dueDate.setHours(0, 0, 0, 0);
      
      if (dueDate > today) {
        // Not yet due
        const diffTime = dueDate.getTime() - today.getTime();
        const diffDays = Math.floor(diffTime / (1000 * 3600 * 24));
        return `${this.formatDaysToHumanReadable(diffDays)} restants`;
      } else if (dueDate < today) {
        // Overdue
        const diffTime = today.getTime() - dueDate.getTime();
        const diffDays = Math.floor(diffTime / (1000 * 3600 * 24));
        return `${this.formatDaysToHumanReadable(diffDays)} de retard`;
      } else {
        // Due today
        return 'Échéance aujourd\'hui';
      }
    }
  }
  
  getPaymentStatusColor(facture: Facture): string {
    if (facture.statutPaiement === 'PAYE') {
      if (!facture.datePaiement || !facture.dateEcheance) return 'bg-green-100 text-green-800';
      
      const paymentDate = new Date(facture.datePaiement);
      const dueDate = new Date(facture.dateEcheance);
      
      if (paymentDate < dueDate) {
        return 'bg-emerald-100 text-emerald-800'; // Light green for advance
      } else if (paymentDate > dueDate) {
        return 'bg-amber-100 text-amber-800'; // Amber for late payment
      } else {
        return 'bg-green-100 text-green-800'; // Green for on-time
      }
    } else {
      if (!facture.dateEcheance) return 'bg-gray-100 text-gray-800';
      
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      
      const dueDate = new Date(facture.dateEcheance);
      dueDate.setHours(0, 0, 0, 0);
      
      if (dueDate > today) {
        return 'bg-blue-100 text-blue-800'; // Blue for pending
      } else if (dueDate < today) {
        return 'bg-red-100 text-red-800'; // Red for overdue
      } else {
        return 'bg-orange-100 text-orange-800'; // Orange for due today
      }
    }
  }
  
  
}