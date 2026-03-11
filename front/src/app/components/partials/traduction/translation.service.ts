import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class TranslationService {
  private currentLang = new BehaviorSubject<string>('fr');
  public currentLang$ = this.currentLang.asObservable();

  private translations: { [key: string]: { [key: string]: string } } = {
    fr: {
      // Admin
      'Tableau de bord': 'Tableau de bord',
      'Gestion des utilisateurs': 'Gestion des utilisateurs',
      'Nomenclatures': 'Nomenclatures',
      'Applications': 'Applications',
      'Demandes': 'Demandes',
      
      // Commercial
      'Conventions': 'Conventions',
      'Factures': 'Factures',
      'Archives': 'Archives',
      'Calendrier': 'Calendrier',
      
      // User
      'Profil': 'Profil',
      'Boite Mail': 'Boite Mail',
      'Déconnexion': 'Déconnexion',
      
      // Common
      'Rechercher': 'Rechercher',
      'Fermer': 'Fermer',
      'Sauvegarder': 'Sauvegarder',
      'Annuler': 'Annuler',
      'Charger plus': 'Charger plus',
    'Gérer vos paramètres de compte': 'Gérer vos paramètres de compte',
    'Sans département': 'Sans département',
    'Membre depuis': 'Membre depuis',
    'Dernière connexion': 'Dernière connexion',
    'Statut': 'Statut',
    'Échecs de connexion': 'Échecs de connexion',
    'Informations personnelles': 'Informations personnelles',
    'Détails personnels de base': 'Détails personnels de base',
    'Modifier': 'Modifier',
    'Prénom': 'Prénom',
    'Nom de famille': 'Nom de famille',
    'Téléphone': 'Téléphone',
    'Sécurité du compte': 'Sécurité du compte',
    'Paramètres de sécurité et informations': 'Paramètres de sécurité et informations',
    'Nom d\'utilisateur': 'Nom d\'utilisateur',
    'Compte créé le': 'Compte créé le',
    'Statut 2FA': 'Statut 2FA',
    'Activé': 'Activé',
    'Désactivé': 'Désactivé',
    'Activer 2FA': 'Activer 2FA',
    'Dernier changement de mot de passe': 'Dernier changement de mot de passe',
    'Préférences de notification': 'Préférences de notification',
    'Choisissez comment recevez les notifications': 'Choisissez comment recevez les notifications',
    'Enregistrement...': 'Enregistrement...',
    'Enregistrer': 'Enregistrer',
    'Sélection actuelle': 'Sélection actuelle',
    'Email': 'Email',
    'Recevoir les notifications uniquement par email': 'Recevoir les notifications uniquement par email',
    'SMS': 'SMS',
    'Recevoir les notifications uniquement par SMS': 'Recevoir les notifications uniquement par SMS',
    'Les deux': 'Les deux',
    'Recevoir les notifications via email et SMS': 'Recevoir les notifications via email et SMS',
    'Remarque :': 'Remarque :',
    'Les notifications par SMS nécessitent un numéro de téléphone valide dans votre profil.': 'Les notifications par SMS nécessitent un numéro de téléphone valide dans votre profil.',
    'Préférences de notification mises à jour avec succès !': 'Préférences de notification mises à jour avec succès !',
    
    // Modals
    'Modifier le profil': 'Modifier le profil',
    'Mettez à jour vos informations personnelles': 'Mettez à jour vos informations personnelles',
    'JPG, PNG ou GIF. Max 2 Mo': 'JPG, PNG ou GIF. Max 2 Mo',
    'Nouvelle photo sélectionnée': 'Nouvelle photo sélectionnée',
    'Obligatoire': 'Obligatoire',
    'Adresse E-mail': 'Adresse E-mail',
    'Obligatoire pour les SMS': 'Obligatoire pour les SMS',
    'Profil modifié avec succées !': 'Profil modifié avec succées !',
    'Enregistrement en cours...': 'Enregistrement en cours...',
    
    // Password Modal
    'Changer le mot de passe': 'Changer le mot de passe',
    'Mettez à jour votre mot de passe en toute sécurité': 'Mettez à jour votre mot de passe en toute sécurité',
    'Mot de passe actuel': 'Mot de passe actuel',
    'Nouveau mot de passe': 'Nouveau mot de passe',
    '6 caractére minimum': '6 caractére minimum',
    'Confirmer le mot de passe': 'Confirmer le mot de passe',
    'Les mots de passe ne correspondent pas': 'Les mots de passe ne correspondent pas',
    'Password Requirements:': 'Exigences du mot de passe :',
    'Au moins 6 caractères': 'Au moins 6 caractères',
    'Lettres et chiffres': 'Lettres et chiffres',
    'Éviter les mots de passe courants': 'Éviter les mots de passe courants',
    'Mot de passe changé avec succès!': 'Mot de passe changé avec succès!',
    'Changement en cours...': 'Changement en cours...',
    'Recevoir les notifications par email et SMS': 'Recevoir les notifications par email et SMS',
    'Notifications par email uniquement': 'Notifications par email uniquement',
    
    // Account status
    'Loading...': 'Chargement...',
    'Locked by Administrator': 'Verrouillé par l\'administrateur',
    'Temporarily Locked': 'Verrouillé temporairement',
    'Failed attempts': 'Tentatives échouées',
    'Active': 'Actif',
    'Disabled': 'Désactivé',
    'Tableau de Bord Commercial': 'Tableau de Bord Commercial',
    'Vue d\'ensemble de vos conventions et factures': 'Vue d\'ensemble de vos conventions et factures',
    'Actualiser': 'Actualiser',
    
    // Loading
    'Chargement des statistiques...': 'Chargement des statistiques...',
    
    // Quick Stats
    'Revenu': 'Revenu',
    'En Retard': 'En Retard',
    'actives': 'actives',
    'payées': 'payées',
    'Taux:': 'Taux:',
    'factures': 'factures',
    'dues': 'dues',
    'auj.': 'auj.',
    
    // Mini Insights Cards
    'Factures en Retard': 'Factures en Retard',
    'nécessitent un suivi': 'nécessitent un suivi',
    'Top Partenaire': 'Top Partenaire',
    'Bonne Performance': 'Bonne Performance',
    'nouvelles conventions': 'nouvelles conventions',
    
    // Chart Titles
    'Statut des Conventions': 'Statut des Conventions',
    'Statut des Paiements': 'Statut des Paiements',
    'Conventions par Mois': 'Conventions par Mois',
    'Revenus par Mois': 'Revenus par Mois',
    '6 mois': '6 mois',
    
    // Table Headers
    'Détails des Conventions': 'Détails des Conventions',
    'Répartition par statut': 'Répartition par statut',
    'Nombre': 'Nombre',
    'Montant': 'Montant',
    '%': '%',
    
    'Détails des Factures': 'Détails des Factures',
    'Répartition par statut de paiement': 'Répartition par statut de paiement',
    
    // Overdue Invoices Table
    'Nécessitent un suivi immédiat': 'Nécessitent un suivi immédiat',
    'Numéro': 'Numéro',
    'Convention': 'Convention',
    'Retard': 'Retard',
    'Priorité': 'Priorité',
    
    // Filters
    'Priorité:': 'Priorité:',
    'Toutes': 'Toutes',
    'Critique': 'Critique',
    'Élevée': 'Élevée',
    'Moyen': 'Moyen',
    'Trier par:': 'Trier par:',
    'Jours retard': 'Jours retard',
    'Date échéance': 'Date échéance',
    'Afficher:': 'Afficher:',
    'Réinitialiser': 'Réinitialiser',
    
    // Pagination
    'Affichage': 'Affichage',
    'sur': 'sur',
    'Aucune facture trouvée avec ces filtres': 'Aucune facture trouvée avec ces filtres',
    
    // Status Labels (add these if not already present)
    'En attente': 'En attente',
    'Acceptée': 'Acceptée',
    'Refusée': 'Refusée',
    'Payé': 'Payé',
    'Non payé': 'Non payé',
    'En retard': 'En retard',
    'PAYE': 'Payé',
    'NON_PAYE': 'Non payé',
    'IMPAYE': 'Impayé',
    'ATTENTE': 'En attente',
    'ACCEPTEE': 'Acceptée',
    'REFUSEE': 'Refusée'
    },
    en: {
      // Admin
      'Tableau de bord': 'Dashboard',
      'Gestion des utilisateurs': 'User Management',
      'Nomenclatures': 'Nomenclatures',
      'Applications': 'Applications',
      'Demandes': 'Requests',
      
      // Commercial
      'Conventions': 'Agreements',
      'Factures': 'Invoices',
      'Archives': 'Archives',
      'Calendrier': 'Calendar',
      
      // User
      'Profil': 'Profile',
      'Boite Mail': 'Email Box',
      'Déconnexion': 'Logout',
      
      // Common
      'Rechercher': 'Search',
      'Fermer': 'Close',
      'Sauvegarder': 'Save',
      'Annuler': 'Cancel',
      'Charger plus': 'Load more',
    'Gérer vos paramètres de compte': 'Manage your account settings',
    'Sans département': 'No department',
    'Membre depuis': 'Member since',
    'Dernière connexion': 'Last login',
    'Statut': 'Status',
    'Échecs de connexion': 'Failed attempts',
    'Informations personnelles': 'Personal Information',
    'Détails personnels de base': 'Basic personal details',
    'Modifier': 'Edit',
    'Prénom': 'First name',
    'Nom de famille': 'Last name',
    'Téléphone': 'Phone',
    'Sécurité du compte': 'Account Security',
    'Paramètres de sécurité et informations': 'Security settings and information',
    'Changer le mot de passe': 'Change password',
    'Nom d\'utilisateur': 'Username',
    'Compte créé le': 'Account created on',
    'Statut 2FA': '2FA Status',
    'Activé': 'Enabled',
    'Désactivé': 'Disabled',
    'Activer 2FA': 'Enable 2FA',
    'Dernier changement de mot de passe': 'Last password change',
    'Préférences de notification': 'Notification Preferences',
    'Choisissez comment recevez les notifications': 'Choose how you receive notifications',
    'Enregistrement...': 'Saving...',
    'Enregistrer': 'Save',
    'Sélection actuelle': 'Current selection',
    'Email': 'Email',
    'Recevoir les notifications uniquement par email': 'Receive notifications only by email',
    'SMS': 'SMS',
    'Recevoir les notifications uniquement par SMS': 'Receive notifications only by SMS',
    'Les deux': 'Both',
    'Recevoir les notifications via email et SMS': 'Receive notifications via email and SMS',
    'Remarque :': 'Note:',
    'Les notifications par SMS nécessitent un numéro de téléphone valide dans votre profil.': 'SMS notifications require a valid phone number in your profile.',
    'Préférences de notification mises à jour avec succès !': 'Notification preferences updated successfully!',
    
    // Modals
    'Modifier le profil': 'Edit Profile',
    'Mettez à jour vos informations personnelles': 'Update your personal information',
    'JPG, PNG ou GIF. Max 2 Mo': 'JPG, PNG or GIF. Max 2MB',
    'Nouvelle photo sélectionnée': 'New photo selected',
    'Obligatoire': 'Required',
    'Adresse E-mail': 'Email Address',
    'Obligatoire pour les SMS': 'Required for SMS',
    'Profil modifié avec succées !': 'Profile updated successfully!',
    'Enregistrement en cours...': 'Saving...',
    
    // Password Modal
    'Mettez à jour votre mot de passe en toute sécurité': 'Update your password securely',
    'Mot de passe actuel': 'Current password',
    'Nouveau mot de passe': 'New password',
    '6 caractére minimum': 'Minimum 6 characters',
    'Confirmer le mot de passe': 'Confirm password',
    'Les mots de passe ne correspondent pas': 'Passwords do not match',
    'Password Requirements:': 'Password Requirements:',
    'Au moins 6 caractères': 'At least 6 characters',
    'Lettres et chiffres': 'Letters and numbers',
    'Éviter les mots de passe courants': 'Avoid common passwords',
    'Mot de passe changé avec succès!': 'Password changed successfully!',
    'Changement en cours...': 'Changing...',
    
    'Recevoir les notifications par email et SMS': 'Receive notifications by email and SMS',
    'Notifications par email uniquement': 'Email notifications only',
    
    // Account status
    'Loading...': 'Loading...',
    'Locked by Administrator': 'Locked by Administrator',
    'Temporarily Locked': 'Temporarily Locked',
    'Failed attempts': 'Failed attempts',
    'Active': 'Active',
    'Disabled': 'Disabled',
     'Tableau de Bord Commercial': 'Commercial Dashboard',
    'Vue d\'ensemble de vos conventions et factures': 'Overview of your agreements and invoices',
    'Actualiser': 'Refresh',
    
    // Loading
    'Chargement des statistiques...': 'Loading statistics...',
    
    // Quick Stats
    'Revenu': 'Revenue',
    'En Retard': 'Overdue',
    'actives': 'active',
    'payées': 'paid',
    'Taux:': 'Rate:',
    'factures': 'invoices',
    'dues': 'due',
    'auj.': 'today',
    
    // Mini Insights Cards
    'Factures en Retard': 'Overdue Invoices',
    'nécessitent un suivi': 'need follow-up',
    'Top Partenaire': 'Top Partner',
    'Bonne Performance': 'Good Performance',
    'nouvelles conventions': 'new agreements',
    
    // Chart Titles
    'Statut des Conventions': 'Agreement Status',
    'Statut des Paiements': 'Payment Status',
    'Conventions par Mois': 'Agreements by Month',
    'Revenus par Mois': 'Revenue by Month',
    '6 mois': '6 months',
    
    // Table Headers
    'Détails des Conventions': 'Agreement Details',
    'Répartition par statut': 'Distribution by status',
    'Nombre': 'Count',
    'Montant': 'Amount',
    '%': '%',
    
    'Détails des Factures': 'Invoice Details',
    'Répartition par statut de paiement': 'Distribution by payment status',
    
    // Overdue Invoices Table
    'Nécessitent un suivi immédiat': 'Require immediate follow-up',
    'Numéro': 'Number',
    'Convention': 'Agreement',
    'Retard': 'Delay',
    'Priorité': 'Priority',
    
    // Filters
    'Priorité:': 'Priority:',
    'Toutes': 'All',
    'Critique': 'Critical',
    'Élevée': 'High',
    'Moyen': 'Medium',
    'Trier par:': 'Sort by:',
    'Jours retard': 'Days overdue',
    'Date échéance': 'Due date',
    'Afficher:': 'Show:',
    'Réinitialiser': 'Reset',
    
    // Pagination
    'Affichage': 'Showing',
    'sur': 'of',
    'Aucune facture trouvée avec ces filtres': 'No invoices found with these filters',
    
    // Status Labels
    'En attente': 'Pending',
    'Acceptée': 'Accepted',
    'Refusée': 'Rejected',
    'Payé': 'Paid',
    'Non payé': 'Unpaid',
    'En retard': 'Overdue',
    'PAYE': 'Paid',
    'NON_PAYE': 'Unpaid',
    'IMPAYE': 'Overdue',
    'ATTENTE': 'Pending',
    'ACCEPTEE': 'Accepted',
    'REFUSEE': 'Rejected'
    },
    ar: {
      // Admin
      'Tableau de bord': 'لوحة القيادة',
      'Gestion des utilisateurs': 'إدارة المستخدمين',
      'Nomenclatures': 'المسميات',
      'Applications': 'التطبيقات',
      'Demandes': 'الطلبات',
      
      // Commercial
      'Conventions': 'الاتفاقيات',
      'Factures': 'الفواتير',
      'Archives': 'الأرشيف',
      'Calendrier': 'التقويم',
      
      // User
      'Profil': 'الملف الشخصي',
      'Boite Mail': 'صندوق البريد',
      'Déconnexion': 'تسجيل الخروج',
      
      // Common
      'Rechercher': 'بحث',
      'Fermer': 'إغلاق',
      'Sauvegarder': 'حفظ',
      'Annuler': 'إلغاء',
      'Charger plus': 'تحميل المزيد',
    'Gérer vos paramètres de compte': 'إدارة إعدادات حسابك',
    'Sans département': 'بدون قسم',
    'Membre depuis': 'عضو منذ',
    'Dernière connexion': 'آخر تسجيل دخول',
    'Statut': 'الحالة',
    'Échecs de connexion': 'محاولات فاشلة',
    'Informations personnelles': 'معلومات شخصية',
    'Détails personnels de base': 'التفاصيل الشخصية الأساسية',
    'Modifier': 'تعديل',
    'Prénom': 'الاسم الأول',
    'Nom de famille': 'اسم العائلة',
    'Téléphone': 'الهاتف',
    'Sécurité du compte': 'أمان الحساب',
    'Paramètres de sécurité et informations': 'إعدادات الأمان والمعلومات',
    'Changer le mot de passe': 'تغيير كلمة المرور',
    'Nom d\'utilisateur': 'اسم المستخدم',
    'Compte créé le': 'تم إنشاء الحساب في',
    'Statut 2FA': 'حالة المصادقة الثنائية',
    'Activé': 'مفعل',
    'Désactivé': 'معطل',
    'Activer 2FA': 'تفعيل المصادقة الثنائية',
    'Dernier changement de mot de passe': 'آخر تغيير لكلمة المرور',
    'Préférences de notification': 'تفضيلات الإشعارات',
    'Choisissez comment recevez les notifications': 'اختر كيفية تلقي الإشعارات',
    'Enregistrement...': 'جاري الحفظ...',
    'Enregistrer': 'حفظ',
    'Sélection actuelle': 'الاختيار الحالي',
    'Email': 'البريد الإلكتروني',
    'Recevoir les notifications uniquement par email': 'تلقي الإشعارات عبر البريد الإلكتروني فقط',
    'SMS': 'رسالة نصية',
    'Recevoir les notifications uniquement par SMS': 'تلقي الإشعارات عبر الرسائل النصية فقط',
    'Les deux': 'كلاهما',
    'Recevoir les notifications via email et SMS': 'تلقي الإشعارات عبر البريد الإلكتروني والرسائل النصية',
    'Remarque :': 'ملاحظة:',
    'Les notifications par SMS nécessitent un numéro de téléphone valide dans votre profil.': 'الإشعارات عبر الرسائل النصية تتطلب رقم هاتف صحيح في ملفك الشخصي.',
    'Préférences de notification mises à jour avec succès !': 'تم تحديث تفضيلات الإشعارات بنجاح!',
    
    // Modals
    'Modifier le profil': 'تعديل الملف الشخصي',
    'Mettez à jour vos informations personnelles': 'تحديث معلوماتك الشخصية',
    'JPG, PNG ou GIF. Max 2 Mo': 'JPG, PNG أو GIF. الحد الأقصى 2 ميجابايت',
    'Nouvelle photo sélectionnée': 'تم اختيار صورة جديدة',
    'Obligatoire': 'إلزامي',
    'Adresse E-mail': 'عنوان البريد الإلكتروني',
    'Obligatoire pour les SMS': 'مطلوب للرسائل النصية',
    'Profil modifié avec succées !': 'تم تعديل الملف الشخصي بنجاح!',
    'Enregistrement en cours...': 'جاري الحفظ...',
    
    // Password Modal
    'Mettez à jour votre mot de passe en toute sécurité': 'تحديث كلمة المرور بأمان',
    'Mot de passe actuel': 'كلمة المرور الحالية',
    'Nouveau mot de passe': 'كلمة المرور الجديدة',
    '6 caractére minimum': '6 أحرف كحد أدنى',
    'Confirmer le mot de passe': 'تأكيد كلمة المرور',
    'Les mots de passe ne correspondent pas': 'كلمات المرور غير متطابقة',
    'Password Requirements:': 'متطلبات كلمة المرور:',
    'Au moins 6 caractères': '6 أحرف على الأقل',
    'Lettres et chiffres': 'حروف وأرقام',
    'Éviter les mots de passe courants': 'تجنب كلمات المرور الشائعة',
    'Mot de passe changé avec succès!': 'تم تغيير كلمة المرور بنجاح!',
    'Changement en cours...': 'جاري التغيير...',

    'Recevoir les notifications par email et SMS': 'تلقي الإشعارات عبر البريد الإلكتروني والرسائل النصية',
    'Notifications par email uniquement': 'إشعارات البريد الإلكتروني فقط',
    
    // Account status
    'Loading...': 'جاري التحميل...',
    'Locked by Administrator': 'مقفل من قبل المسؤول',
    'Temporarily Locked': 'مقفل مؤقتاً',
    'Failed attempts': 'محاولات فاشلة',
    'Active': 'نشط',
    'Disabled': 'معطل',
    'Tableau de Bord Commercial': 'لوحة تحكم التجارية',
    'Vue d\'ensemble de vos conventions et factures': 'نظرة عامة على اتفاقياتك وفواتيرك',
    'Actualiser': 'تحديث',
    
    // Loading
    'Chargement des statistiques...': 'جاري تحميل الإحصائيات...',
    
    // Quick Stats
    'Revenu': 'الإيرادات',
    'En Retard': 'متأخر',
    'actives': 'نشطة',
    'payées': 'مدفوعة',
    'Taux:': 'المعدل:',
    'factures': 'فواتير',
    'dues': 'مستحقة',
    'auj.': 'اليوم',
    
    // Mini Insights Cards
    'Factures en Retard': 'فواتير متأخرة',
    'nécessitent un suivi': 'تتطلب متابعة',
    'Top Partenaire': 'أفضل شريك',
    'Bonne Performance': 'أداء جيد',
    'nouvelles conventions': 'اتفاقيات جديدة',
    
    // Chart Titles
    'Statut des Conventions': 'حالة الاتفاقيات',
    'Statut des Paiements': 'حالة المدفوعات',
    'Conventions par Mois': 'الاتفاقيات حسب الشهر',
    'Revenus par Mois': 'الإيرادات حسب الشهر',
    '6 mois': '6 أشهر',
    
    // Table Headers
    'Détails des Conventions': 'تفاصيل الاتفاقيات',
    'Répartition par statut': 'التوزيع حسب الحالة',
    'Nombre': 'العدد',
    'Montant': 'المبلغ',
    '%': '%',
    
    'Détails des Factures': 'تفاصيل الفواتير',
    'Répartition par statut de paiement': 'التوزيع حسب حالة الدفع',
    
    // Overdue Invoices Table
    'Nécessitent un suivi immédiat': 'تتطلب متابعة فورية',
    'Numéro': 'الرقم',
    'Convention': 'الاتفاقية',
    'Retard': 'التأخير',
    'Priorité': 'الأولوية',
    
    // Filters
    'Priorité:': 'الأولوية:',
    'Toutes': 'الكل',
    'Critique': 'حرج',
    'Élevée': 'عالية',
    'Moyen': 'متوسطة',
    'Trier par:': 'ترتيب حسب:',
    'Jours retard': 'أيام التأخير',
    'Date échéance': 'تاريخ الاستحقاق',
    'Afficher:': 'عرض:',
    'Réinitialiser': 'إعادة تعيين',
    
    // Pagination
    'Affichage': 'عرض',
    'sur': 'من',
    'Aucune facture trouvée avec ces filtres': 'لم يتم العثور على فواتير بهذه الفلاتر',
    
    // Status Labels
    'En attente': 'قيد الانتظار',
    'Acceptée': 'مقبولة',
    'Refusée': 'مرفوضة',
    'Payé': 'مدفوع',
    'Non payé': 'غير مدفوع',
    'En retard': 'متأخر',
    'PAYE': 'مدفوع',
    'NON_PAYE': 'غير مدفوع',
    'IMPAYE': 'متأخر',
    'ATTENTE': 'قيد الانتظار',
    'ACCEPTEE': 'مقبولة',
    'REFUSEE': 'مرفوضة'
   }
  };

  constructor() {
    const savedLang = localStorage.getItem('appLanguage') || 'fr';
    this.setLanguage(savedLang);
  }

  translate(key: string): string {
    const lang = this.currentLang.value;
    if (this.translations[lang] && this.translations[lang][key]) {
      return this.translations[lang][key];
    }
    return key; // Return original if no translation found
  }

setLanguage(lang: string): void {
  if (this.translations[lang]) {
    this.currentLang.next(lang);
    localStorage.setItem('appLanguage', lang);
    document.documentElement.dir = lang === 'ar' ? 'rtl' : 'ltr';
    document.documentElement.lang = lang;
  }
}

  getCurrentLanguage(): string {
    return this.currentLang.value;
  }

  getLanguages() {
    return [
      { code: 'fr', name: 'Français', flag: '🇫🇷' },
      { code: 'en', name: 'English', flag: '🇬🇧' },
      { code: 'ar', name: 'العربية', flag: '🇸🇦' }
    ];
  }
}