import { BehaviorSubject, Observable } from 'rxjs';

import { Injectable, Renderer2, RendererFactory2 } from '@angular/core';
import { Subject } from 'rxjs';


@Injectable({
  providedIn: 'root'
})
export class TranslationService {


  private renderer: Renderer2;
  private currentLang: string = 'fr';
  private currentLangSubject = new BehaviorSubject<string>('fr');
  public currentLang$: Observable<string> = this.currentLangSubject.asObservable();

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
    'Décideur': 'Décideur',
'Commercial': 'Commercial',
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
    'REFUSEE': 'Refusée',
    'Facture Overdue': 'Facture en retard',
'Facture Due Today': 'Facture due aujourd\'hui',
'Facture Due Tomorrow': 'Facture due demain',
'Facture Due in 2 Days': 'Facture due dans 2 jours',
'Facture Due in 3 Days': 'Facture due dans 3 jours',
'Facture Due in 4 Days': 'Facture due dans 4 jours',
'Facture Due in 5 Days': 'Facture due dans 5 jours',

// Also add these for better coverage
'Overdue': 'En retard',
'Due Today': 'Due aujourd\'hui',
'Due Tomorrow': 'Due demain',
'Due in {{days}} days': 'Due dans {{days}} jours',
'Invoice overdue: {{number}}': 'Facture en retard: {{number}}',
'Invoice due today: {{number}}': 'Facture due aujourd\'hui: {{number}}',
'Invoice due tomorrow: {{number}}': 'Facture due demain: {{number}}',
'Payment reminder': 'Rappel de paiement',
'Invoice': 'Facture',
'Amount': 'Montant',
'Due date': 'Date d\'échéance',
'Bienvenue Administrateur': 'Bienvenue Administrateur',
'Chargement des données...': 'Chargement des données...',
'Progression Moyenne': 'Progression Moyenne',
'En Bonne Voie': 'En Bonne Voie',
'Total Applications': 'Total Applications',
'Structures Responsable': 'Structures Responsable',
'Structures Bénéficiaires': 'Structures Bénéficiaires',
'Zones TN': 'Zones TN',
'Zones Personalisés': 'Zones Personnalisées',
'Rôles': 'Rôles',
'Structures': 'Structures',
'Charge de Travail': 'Charge de Travail',
'Total Chefs': 'Total Chefs',
'Moyenne': 'Moyenne',
'Disponible': 'Disponible',
'Chef de Projet': 'Chef de Projet',
'Charge': 'Charge',
'Apps': 'Applications',
'Valeur': 'Valeur',
'Durée': 'Durée',
'Aucune donnée de charge disponible': 'Aucune donnée de charge disponible',
'Overloaded': 'Surchargé',
'High': 'Élevé',
'Normal': 'Normal',
'Low': 'Faible',
'workload.status.critical': 'Critique',
'workload.status.high': 'Élevée',
'workload.status.medium': 'Moyenne',
'workload.status.low': 'Faible',
'workload.status.available': 'Disponible',
'Nombre de Structures': 'Nombre de Structures',
'j': 'j',
'jours': 'jours',
'CRITIQUE': 'Critique',
'MOYENNE': 'Moyenne',
'FAIBLE': 'Faible',


// User Management Section
'Gérer les utilisateurs et surveiller les métriques système': 'Gérer les utilisateurs et surveiller les métriques système',
'Succès': 'Succès',
'Erreur': 'Erreur',
'Une erreur s\'est produite': 'Une erreur s\'est produite',
'OK': 'OK',
'Nombre total d\'utilisateurs': 'Nombre total d\'utilisateurs',
'Utilisateurs bloqués': 'Utilisateurs bloqués',
'Tentatives échouées': 'Tentatives échouées',
'Utilisateurs actifs': 'Utilisateurs actifs',
'Liste des utilisateurs': 'Liste des utilisateurs',
'Filtrer par rôle': 'Filtrer par rôle',
'Tous les rôles': 'Tous les rôles',
'Commercial Métier': 'Commercial Métier',
'Filtrer par statut': 'Filtrer par statut',
'Tous les statuts': 'Tous les statuts',
'Temporairement bloqué': 'Temporairement bloqué',
'Bloqué par l\'administrateur': 'Bloqué par l\'administrateur',
'Effacer': 'Effacer',
'Rôle:': 'Rôle:',
'Statut:': 'Statut:',
'Chargement des utilisateurs...': 'Chargement des utilisateurs...',
'Département': 'Département',
'Non attribué': 'Non attribué',
'Rôle': 'Rôle',
'Voir l\'historique': 'Voir l\'historique',
'Historique': 'Historique',
'Verrouiller': 'Verrouiller',
'Vérrouiller': 'Verrouiller',
'Déverrouiller': 'Déverrouiller',
'Assigner Applications': 'Assigner Applications',
'à': 'à',
'résultats': 'résultats',
'Aucun utilisateur trouvé': 'Aucun utilisateur trouvé',
'Aucun utilisateur n\'a encore été créé. Cliquez sur "Ajouter un utilisateur" pour en créer un.': 'Aucun utilisateur n\'a encore été créé. Cliquez sur "Ajouter un utilisateur" pour en créer un.',
'Ajouter un utilisateur': 'Ajouter un utilisateur',
'Ajouter un nouvel utilisateur': 'Ajouter un nouvel utilisateur',
'Nom d\'utilisateur requis': 'Nom d\'utilisateur requis',
'Minimum 3 caractères requis': 'Minimum 3 caractères requis',
'Nom d\'utilisateur valide': 'Nom d\'utilisateur valide',
'Email requis': 'Email requis',
'Email invalide (ex: user@domain.nn)': 'Email invalide (ex: user@domain.nn)',
'Email valide': 'Email valide',
'Mot de passe requis': 'Mot de passe requis',
'Minimum 8 caractères requis': 'Minimum 8 caractères requis',
'Mot de passe valide': 'Mot de passe valide',
'Lettres seulement': 'Lettres seulement',
'Numéro de téléphone': 'Numéro de téléphone',
'8 chiffres minimum': '8 chiffres minimum',
'Lettres, chiffres et espaces seulement': 'Lettres, chiffres et espaces seulement',
'Au moins un rôle est requis': 'Au moins un rôle est requis',
'Résumé': 'Résumé',
'Nouvel utilisateur': 'Nouvel utilisateur',
'Accès': 'Accès',
'Sélectionner un rôle': 'Sélectionner un rôle',
'Contact': 'Contact',
'Non assigné': 'Non assigné',
'Créer': 'Cr éer',
'Département pour': 'Département pour',
'Département est requis': 'Département est requis',
'Mettre à jour': 'Mettre à jour',
'Rôles pour': 'Rôles pour',
'Sélectionner le rôle': 'Sélectionner le rôle',
'Veuillez confirmer votre action': 'Veuillez confirmer votre action',
'Assigner les applications': 'Assigner les applications',
'Assignation à': 'Assignation à',
'Aucune application non assignée': 'Aucune application non assignée',
'Toutes les applications sont déjà assignées.': 'Toutes les applications sont déjà assignées.',
'Charge de travail actuelle': 'Charge de travail actuelle',
'Charge globale': 'Charge globale',
'Forcer l\'assignation': 'Forcer l\'assignation',
'application(s) sélectionnée(s)': 'application(s) sélectionnée(s)',
'Assignation...': 'Assignation...',
'Assigner': 'Assigner',
'Historique de l\'utilisateur': 'Historique de l\'utilisateur',
'Aucun historique trouvé': 'Aucun historique trouvé',
'Chargement...': 'Chargement...',
'Accès Refusé': 'Accès Refusé',
'Vous n\'avez pas l\'autorisation d\'accéder au tableau de bord administrateur. Veuillez contacter un administrateur si vous pensez qu\'il s\'agit d\'une erreur.': 'Vous n\'avez pas l\'autorisation d\'accéder au tableau de bord administrateur. Veuillez contacter un administrateur si vous pensez qu\'il s\'agit d\'une erreur.',
'Retour au Tableau de Bord': 'Retour au Tableau de Bord',

// Nomenclature Section
'Gestion des nomenclatures': 'Gestion des nomenclatures',
'Gérez vos zones géographiques et structures organisationnelles': 'Gérez vos zones géographiques et structures organisationnelles',
'Zones': 'Zones',
'Marchés couverts': 'Marchés couverts',
'Responsables': 'Responsables',
'Autorité de gestion': 'Autorité de gestion',
'Bénéficiels': 'Bénéficiaires',
'Acquéreur / Client': 'Acquéreur / Client',
'Zones géographiques': 'Zones géographiques',
'Structures Responsables': 'Structures Responsables',
'Structures Bénéficiels': 'Structures Bénéficiaires',
'Rechercher...': 'Rechercher...',
'Ajouter': 'Ajouter',
'Code': 'Code',
'Nom': 'Nom',
'Description': 'Description',
'Actions': 'Actions',
'Aucune zone trouvée': 'Aucune zone trouvée',
'+ Ajouter une zone': '+ Ajouter une zone',
'Type': 'Type',
'Zone': 'Zone',
'Non spécifié': 'Non spécifié',
'Aucune structure trouvée': 'Aucune structure trouvée',
'+ Ajouter une structure': '+ Ajouter une structure',
'Page': 'Page',
'Précédent': 'Précédent',
'Suivant': 'Suivant',
'Traitement...': 'Traitement...',
'Description longue': 'Description longue',
'caractères': 'caractères',
'Code requis et doit être unique': 'Code requis et doit être unique',
'Code valide': 'Code valide',
'Nom requis (minimum 2 caractères)': 'Nom requis (minimum 2 caractères)',
'Nom valide': 'Nom valide',
'Format téléphone invalide': 'Format téléphone invalide',
'Téléphone valide': 'Téléphone valide',
'Format email invalide': 'Format email invalide',
'Généré': 'Généré',

// Applications Section
'Gestion des Applications': 'Gestion des Applications',
'Mes Applications': 'Mes Applications',
'Créez et gérez toutes les applications': 'Créez et gérez toutes les applications',
'Gérez vos applications assignées': 'Gérez vos applications assignées',
'Voir les archives': 'Voir les archives',
'Total': 'Total',
'En Cours': 'En Cours',
'Planifiées': 'Planifiées',
'Terminées': 'Terminées',
'Application': 'Application',
'Date Début': 'Date Début',
'Date Fin': 'Date Fin',
'Client': 'Client',
'Progression': 'Progression',
'Chef': 'Chef',
'conv.': 'conv.',
'Tous statuts': 'Tous statuts',
'Tous clients': 'Tous clients',
'Nouvelle': 'Nouvelle',
'Planifié': 'Planifié',
'Terminé': 'Terminé',
'Aucune application trouvée': 'Aucune application trouvée',

// Archive Section
'Applications Archivées': 'Applications Archivées',
'Mes Applications Archivées': 'Mes Applications Archivées',
'application(s) dans les archives': 'application(s) dans les archives',
'Aucune application archivée': 'Aucune application archivée',
'Les applications apparaîtront ici après archivage': 'Les applications apparaîtront ici après archivage',
'Archivée': 'Archivée',
'Client:': 'Client:',
'Chef:': 'Chef:',
'Conventions:': 'Conventions:',
'Période:': 'Période:',
'Préc': 'Préc',
'Suiv': 'Suiv',

'Verrouillé par l\'administrateur': 'Verrouillé par l\'administrateur',
'tentatives échouées': 'tentatives échouées',
'Actif': 'Actif',
'Confirmer le verrouillage de l\'utilisateur': 'Confirmer le verrouillage de l\'utilisateur',
'Confirmer le déverrouillage de l\'utilisateur': 'Confirmer le déverrouillage de l\'utilisateur',
'Êtes-vous sûr de vouloir verrouiller': 'Êtes-vous sûr de vouloir verrouiller',
'Ils ne pourront pas accéder à leur compte tant qu\'il n\'est pas déverrouillé.': 'Ils ne pourront pas accéder à leur compte tant qu\'il n\'est pas déverrouillé.',
'Êtes-vous sûr de vouloir déverrouiller': 'Êtes-vous sûr de vouloir déverrouiller',
'Ils retrouveront l\'accès à leur compte.': 'Ils retrouveront l\'accès à leur compte.',
'Successfully assigned': 'Assigné avec succès',
'project(s) to': 'projet(s) à',
'Assigned': 'Assigné',
'project(s), failed to assign': 'projet(s), échec d\'assignation',
'project(s)': 'projet(s)',



// History Entry Types (Action Types)
'Connexion': 'Connexion',
'Création': 'Création',
'Modification': 'Modification',
'Suppression': 'Suppression',
'Archivage': 'Archivage',
'Restauration': 'Restauration',
'Renouvellement': 'Renouvellement',
'Paiement': 'Paiement',
'Changement de statut': 'Changement de statut',
'Synchronisation des dates': 'Synchronisation des dates',
'Mise à jour financière': 'Mise à jour financière',
'Retard de paiement': 'Retard de paiement',

// Entity Types
'Utilisateur': 'Utilisateur',
'Facture': 'Facture',

// Field Names for Changes
'statutPaiement': 'Statut paiement',
'referencePaiement': 'Référence paiement',
'datePaiement': 'Date paiement',
'montantTTC': 'Montant TTC',
'montantHT': 'Montant HT',
'tva': 'TVA',
'numeroFacture': 'Numéro facture',
'dateFacturation': 'Date facturation',
'dateEcheance': 'Date échéance',
'etat': 'État',
'dateDebut': 'Date début',
'dateFin': 'Date fin',
'periodicite': 'Périodicité',
'libelle': 'Libellé',
'referenceERP': 'Référence ERP',
'nbUsers': 'Nombre utilisateurs',
'archived': 'Archivé',
'joursRetard': 'Jours de retard',

// Status Values
'NON PAYÉE': 'Non payée',
'PAYÉE': 'Payée',
'EN_RETARD': 'En retard',
'EN_COURS': 'En cours',
'PLANIFIE': 'Planifié',
'TERMINE': 'Terminé',
'EN_ATTENTE': 'En attente',


// Periodicity Values
'MENSUEL': 'Mensuel',
'BIMESTRIEL': 'Bimestriel',
'TRIMESTRIEL': 'Trimestriel',
'SEMESTRIEL': 'Semestriel',
'ANNUEL': 'Annuel',

// Yes/No for archived
'Oui': 'Oui',
'Non': 'Non',

// User status in history
'Bloqué': 'Bloqué',
'Verrouillé': 'Verrouillé',

// Additional UI text from your logs
'Logo': 'Logo',
'AA': 'AA',
'decideur': 'Décideur',
'commercial 1': 'Commercial 1',
'nousseiba': 'Nousseiba',
'kaabi': 'Kaabi',
'YES': 'OUI',
'NO': 'NON',
'email': 'Email',
'email email': 'Email',
'avatar': 'Avatar',
'new app ' : 'nouvelle application',

'Groups': 'Groupes',
'New Group': 'Nouveau groupe',
'System Groups': 'Groupes système',
'System': 'Système',
'members': 'membres',
'more': 'plus',
'My Groups': 'Mes groupes',
'View members': 'Voir les membres',
'Edit group': 'Modifier le groupe',
'Delete group': 'Supprimer le groupe',
'View only group': 'Groupe en lecture seule',
'No groups yet': 'Aucun groupe pour le moment',
'Create your first group to start organizing your contacts': 'Créez votre premier groupe pour organiser vos contacts',
'Create Group': 'Créer un groupe',
'Edit Group': 'Modifier le groupe',
'Create New Group': 'Créer un nouveau groupe',
'Group Name': 'Nom du groupe',
'Enter group name': 'Entrez le nom du groupe',
'Description (optional)': 'Description (optionnelle)',
'Group description': 'Description du groupe',
'Members': 'Membres',
'Cancel': 'Annuler',
'Update': 'Mettre à jour',
'Create': 'Créer',
'Close': 'Fermer',
    },
    en: {


      // History Modal - Headers & Labels
'Historique de l\'utilisateur': 'User History',
'Aucun historique trouvé': 'No history found',
'Chargement...': 'Loading...',
'Fermer': 'Close',

// History Entry Types
'Connexion': 'Login',
'Déconnexion': 'Logout',
'Création': 'Creation',
'Modification': 'Modification',
'Suppression': 'Deletion',
'Archivage': 'Archiving',
'Restauration': 'Restoration',
'Renouvellement': 'Renewal',
'Paiement': 'Payment',
'Changement de statut': 'Status Change',
'Synchronisation des dates': 'Date Synchronization',
'Mise à jour financière': 'Financial Update',
'Retard de paiement': 'Payment Delay',

// Entity Types
'Utilisateur': 'User',
'Convention': 'Agreement',
'Facture': 'Invoice',
'Application': 'Application',

// Field Names
'statutPaiement': 'Payment Status',
'referencePaiement': 'Payment Reference',
'datePaiement': 'Payment Date',
'montantTTC': 'Amount incl. Tax',
'montantHT': 'Amount excl. Tax',
'tva': 'VAT',
'numeroFacture': 'Invoice Number',
'dateFacturation': 'Billing Date',
'dateEcheance': 'Due Date',
'etat': 'Status',
'dateDebut': 'Start Date',
'dateFin': 'End Date',
'periodicite': 'Periodicity',
'libelle': 'Label',
'referenceERP': 'ERP Reference',
'nbUsers': 'Number of Users',
'archived': 'Archived',
'joursRetard': 'Days Overdue',

// Status Values
'NON_PAYE': 'Unpaid',
'NON PAYÉE': 'Unpaid',
'PAYE': 'Paid',
'PAYÉE': 'Paid',
'EN_RETARD': 'Overdue',
'EN_COURS': 'In Progress',
'PLANIFIE': 'Planned',
'TERMINE': 'Completed',
'EN_ATTENTE': 'Pending',

// Periodicity Values
'MENSUEL': 'Monthly',
'BIMESTRIEL': 'Bimonthly',
'TRIMESTRIEL': 'Quarterly',
'SEMESTRIEL': 'Semiannual',
'ANNUEL': 'Annual',

// Yes/No
'Oui': 'Yes',
'Non': 'No',

// User status
'Actif': 'Active',
'Bloqué': 'Blocked',
'Verrouillé': 'Locked',

// Additional UI text
'Logo': 'Logo',
'AA': 'AA',
'decideur': 'Decision Maker',
'commercial 1': 'Sales Rep 1',
'nousseiba': 'Nousseiba',
'kaabi': 'Kaabi',
'YES': 'YES',
'NO': 'NO',
'email': 'Email',
'email email': 'Email',
'avatar': 'Avatar',

      'Verrouillé par l\'administrateur': 'Locked by Administrator',
'Temporairement bloqué': 'Temporarily Locked',
'tentatives échouées': 'failed attempts',
'Confirmer le verrouillage de l\'utilisateur': 'Confirm User Lock',
'Confirmer le déverrouillage de l\'utilisateur': 'Confirm User Unlock',
'Êtes-vous sûr de vouloir verrouiller': 'Are you sure you want to lock',
'Ils ne pourront pas accéder à leur compte tant qu\'il n\'est pas déverrouillé.': 'They will not be able to access their account until unlocked.',
'Êtes-vous sûr de vouloir déverrouiller': 'Are you sure you want to unlock',
'Ils retrouveront l\'accès à leur compte.': 'They will regain access to their account.',
'Successfully assigned': 'Successfully assigned',
'project(s) to': 'project(s) to',
'Assigned': 'Assigned',
'project(s), failed to assign': 'project(s), failed to assign',
'project(s)': 'project(s)',

      'CRITIQUE': 'Critical',
'MOYENNE': 'Medium',
'FAIBLE': 'Low',
      'Nombre de Structures': 'Number of Structures',

      'Tableau de bord': 'Dashboard',
      'Gestion des utilisateurs': 'User Management',
      'Nomenclatures': 'Nomenclatures',
      'Applications': 'Applications',
      'Demandes': 'Requests',
      'Décideur': 'Decision Maker',
'Commercial': 'Sales Representative',
      
'Nom du client': 'Client Name',
'Zone géographique': 'Geographical Zone',
'Type de structure' : 'Type of Structure',
'-- Sélectionnez --' : '-- Select --',
'Nom du client valide': 'Valid Client Name',
'Code généré:' : 'Generated Code:',
'Description de la structure...' : 'Structure description...',
'Description de la zone...' : 'Zone description...',
      // Commercial
      'Conventions': 'Agreements',
      'Factures': 'Invoices',
      'Archives': 'Archives',
      'Calendrier': 'Calendar',
      
      // User
      'Profil': 'Profile',
      'Boite Mail': 'Email Box',
      
      // Common
      'Rechercher': 'Search',
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
   
    'IMPAYE': 'Overdue',
    'ATTENTE': 'Pending',
    'ACCEPTEE': 'Accepted',
    'REFUSEE': 'Rejected',
    'Bienvenue Administrateur': 'Welcome Administrator',
'Chargement des données...': 'Loading data...',
'Progression Moyenne': 'Average Progress',
'En Bonne Voie': 'On Track',
'Total Applications': 'Total Applications',
'Structures Responsable': 'Responsible Structures',
'Structures Bénéficiaires': 'Beneficiary Structures',
'Zones TN': 'Tunisian Zones',
'Zones Personalisés': 'Custom Zones',
'Rôles': 'Roles',
'Structures': 'Structures',
'Charge de Travail': 'Workload',
'Total Chefs': 'Total Project Managers',
'Moyenne': 'Medium',
'Disponible': 'Available',
'Chef de Projet': 'Project Manager',
'Charge': 'Workload',
'Apps': 'Apps',
'Valeur': 'Value',
'Durée': 'Duration',
'Aucune donnée de charge disponible': 'No workload data available',
'workload.status.critical': 'Critical',
'workload.status.high': 'High',
'workload.status.medium': 'Medium',
'workload.status.low': 'Low',
'workload.status.available': 'Available',
'Overloaded': 'Overloaded',
'High': 'High',
'Normal': 'Normal',
'Low': 'Low',
'j': 'd',
'jours': 'days',
'Gérer les utilisateurs et surveiller les métriques système': 'Manage users and monitor system metrics',
'Succès': 'Success',
'Erreur': 'Error',
'Une erreur s\'est produite': 'An error occurred',
'OK': 'OK',
'Nombre total d\'utilisateurs': 'Total Users',
'Utilisateurs bloqués': 'Locked Users',
'Tentatives échouées': 'Failed Attempts',
'Utilisateurs actifs': 'Active Users',
'Liste des utilisateurs': 'User List',
'Filtrer par rôle': 'Filter by Role',
'Tous les rôles': 'All Roles',
'Commercial Métier': 'Sales Representative',
'Filtrer par statut': 'Filter by Status',
'Tous les statuts': 'All Statuses',
'Bloqué par l\'administrateur': 'Locked by Admin',
'Effacer': 'Clear',
'Rôle:': 'Role:',
'Statut:': 'Status:',
'Chargement des utilisateurs...': 'Loading users...',
'Département': 'Department',
'Non attribué': 'Not Assigned',
'Rôle': 'Role',
'Voir l\'historique': 'View History',
'Historique': 'History',
'Verrouiller': 'Lock',
'Vérrouiller': 'Lock',
'Déverrouiller': 'Unlock',
'Deverrouiller': 'Unlock',
'Assigner Applications': 'Assign Applications',
'à': 'to',
'résultats': 'results',
'Aucun utilisateur trouvé': 'No users found',
'Aucun utilisateur n\'a encore été créé. Cliquez sur "Ajouter un utilisateur" pour en créer un.': 'No users have been created yet. Click "Add User" to create one.',
'Ajouter un utilisateur': 'Add User',
'Ajouter un nouvel utilisateur': 'Add New User',
'Nom d\'utilisateur requis': 'Username required',
'Minimum 3 caractères requis': 'Minimum 3 characters required',
'Nom d\'utilisateur valide': 'Valid username',
'Email requis': 'Email required',
'Email invalide (ex: user@domain.nn)': 'Invalid email (ex: user@domain.nn)',
'Email valide': 'Valid email',
'Mot de passe requis': 'Password required',
'Minimum 8 caractères requis': 'Minimum 8 characters required',
'Mot de passe valide': 'Valid password',
'Lettres seulement': 'Letters only',
'Numéro de téléphone': 'Phone Number',
'8 chiffres minimum': 'Minimum 8 digits',
'Lettres, chiffres et espaces seulement': 'Letters, numbers and spaces only',
'Au moins un rôle est requis': 'At least one role is required',
'Résumé': 'Summary',
'Nouvel utilisateur': 'New User',
'Accès': 'Access',
'Sélectionner un rôle': 'Select a role',
'Contact': 'Contact',
'Non assigné': 'Not Assigned',
'Créer': 'Create',
'Département pour': 'Department for',
'Département est requis': 'Department is required',
'Mettre à jour': 'Update',
'Rôles pour': 'Roles for',
'Sélectionner le rôle': 'Select role',
'Veuillez confirmer votre action': 'Please confirm your action',
'Assigner les applications': 'Assign Applications',
'Assignation à': 'Assignment to',
'Aucune application non assignée': 'No unassigned applications',
'Toutes les applications sont déjà assignées.': 'All applications are already assigned.',
'Charge de travail actuelle': 'Current Workload',
'Charge globale': 'Overall Load',
'Forcer l\'assignation': 'Force Assignment',
'application(s) sélectionnée(s)': 'application(s) selected',
'Assignation...': 'Assigning...',
'Assigner': 'Assign',
'Accès Refusé': 'Access Denied',
'Vous n\'avez pas l\'autorisation d\'accéder au tableau de bord administrateur. Veuillez contacter un administrateur si vous pensez qu\'il s\'agit d\'une erreur.': 'You do not have permission to access the admin dashboard. Please contact an administrator if you believe this is an error.',
'Retour au Tableau de Bord': 'Back to Dashboard',

// Nomenclature Section
'Gestion des nomenclatures': 'Nomenclature Management',
'Gérez vos zones géographiques et structures organisationnelles': 'Manage your geographical zones and organizational structures',
'Zones': 'Zones',
'Marchés couverts': 'Covered Markets',
'Responsables': 'Responsible',
'Autorité de gestion': 'Management Authority',
'Bénéficiels': 'Beneficiaries',
'Acquéreur / Client': 'Buyer / Client',
'Zones géographiques': 'Geographical Zones',
'Structures Responsables': 'Responsible Structures',
'Structures Bénéficiels': 'Beneficiary Structures',
'Rechercher...': 'Search...',
'Ajouter': 'Add',
'Code': 'Code',
'Nom': 'Name',
'Description': 'Description',
'Actions': 'Actions',
'Aucune zone trouvée': 'No zones found',
'+ Ajouter une zone': '+ Add a zone',
'Type': 'Type',
'Zone': 'Zone',
'Non spécifié': 'Not specified',
'Aucune structure trouvée': 'No structures found',
'+ Ajouter une structure': '+ Add a structure',
'Page': 'Page',
'Précédent': 'Previous',
'Suivant': 'Next',
'Traitement...': 'Processing...',
'Description longue': 'Long description',
'caractères': 'characters',
'Code requis et doit être unique': 'Code required and must be unique',
'Code valide': 'Valid code',
'Nom requis (minimum 2 caractères)': 'Name required (minimum 2 characters)',
'Nom valide': 'Valid name',
'Format téléphone invalide': 'Invalid phone format',
'Téléphone valide': 'Valid phone',
'Format email invalide': 'Invalid email format',
'Généré': 'Generated',

// Applications Section
'Gestion des Applications': 'Application Management',
'Mes Applications': 'My Applications',
'Créez et gérez toutes les applications': 'Create and manage all applications',
'Gérez vos applications assignées': 'Manage your assigned applications',
'Voir les archives': 'View Archives',
'Total': 'Total',
'En Cours': 'In Progress',
'Planifiées': 'Planned',
'Terminées': 'Completed',
'Date Début': 'Start Date',
'Date Fin': 'End Date',
'Client': 'Client',
'Progression': 'Progress',
'Chef': 'Manager',
'conv.': 'conv.',
'Tous statuts': 'All statuses',
'Tous clients': 'All clients',
'Nouvelle': 'New',
'Planifié': 'Planned',
'Terminé': 'Completed',
'Aucune application trouvée': 'No applications found',

// Archive Section
'Applications Archivées': 'Archived Applications',
'Mes Applications Archivées': 'My Archived Applications',
'application(s) dans les archives': 'application(s) in archives',
'Aucune application archivée': 'No archived applications',
'Les applications apparaîtront ici après archivage': 'Applications will appear here after archiving',
'Archivée': 'Archived',
'Client:': 'Client:',
'Chef:': 'Manager:',
'Conventions:': 'Agreements:',
'Période:': 'Period:',
'Préc': 'Prev',
'Suiv': 'Next',

'Mot de passe': 'Password',

'Assignation chef de projet' : 'Project manager assignment',

 'Chargement des détails...': 'Loading details...',
  
  // Buttons
  'Terminer': 'Complete',
  'Réassignation': 'Reassign',
  'Rafraîchir': 'Refresh',
  'Voir tout': 'View all',
 
  'Confirmer la terminaison': 'Confirm termination',
  'Assigner un Chef de Projet': 'Assign a Project Manager',
  'Historique complet': 'Full history',
  "Terminer l'application": "Complete the application",
  
  // Sidebar - Informations clés
  'Informations clés': 'Key information',
  'Min': 'Min',
  'Max': 'Max',
  'Début': 'Start',
  'Fin': 'End',
  'Durée totale': 'Total duration',
  'Jours restants': 'Days remaining',
 
  
  'Voir' : 'View',
  'Approuver' : 'Approve',
  'Refuser' : 'Reject',
  // Termination Card
  'Terminaison': 'Termination',
  'Date': 'Date',
  'Par': 'By',
  'Système': 'System',
  'En avance': 'Early',
  'À temps': 'On time',
 
  'Aucune description fournie': 'No description provided',

  'Structure Bénéficiaire': 'Beneficiary Structure',
  'Structure Responsable': 'Responsible Structure',
  'Non défini': 'Not defined',
  
  // Conventions Card
  'Aucune convention associée': 'No associated agreements',
  'Payées': 'Paid',
  'Users': 'Users',
  

  'Aucun chef assigné': 'No project manager assigned',
  'Charge de travail': 'Workload',
  'Projets actifs': 'Active projects',
  
  // Historique Card
  'Aucun historique': 'No history',
  'Voir tout ({{count}})': 'View all ({{count}})',
  
  // Modals
  'Action irréversible': 'Irreversible action',
  'Cette action marquera l\'application comme terminée. Vous ne pourrez plus modifier cette application après confirmation.': 'This action will mark the application as completed. You will no longer be able to modify this application after confirmation.',
  'Raison de la terminaison': 'Termination reason',
  'optionnelle': 'optional',
  'Ex: Projet livré, contrat terminé, etc...': 'Ex: Project delivered, contract ended, etc...',
  'Récapitulatif': 'Summary',
  'Statut actuel': 'Current status',
  'Date de début': 'Start date',
  'Date de fin prévue': 'Expected end date',
  'Dépassé': 'Overdue',
  
 
  'ARCHIVEE': 'Archived',
  
  
  'SIGNEE': 'Signed',
  'RESILIEE': 'Terminated',
  
  // Archive status
  
  // History messages
  'Aucun historique disponible': 'No history available',
  
  'Payées:': 'Paid:',
  'Factures:': 'Invoices:',

   'Modifier l\'Application': 'Edit Application',
  'Nouvelle Application': 'New Application',
  'Modifiez les informations de l\'application': 'Edit application information',
  'Créez une nouvelle application': 'Create a new application',
  
  // Messages
  'Redirection en cours...': 'Redirecting...',
  
  // Form Labels
  'Code Application': 'Application Code',
  'Nom de l\'Application': 'Application Name',
  'Utilisateurs': 'Users',

  'Minimum': 'Minimum',
  'Maximum': 'Maximum',
  'Email Client': 'Client Email',
  'Téléphone Client': 'Client Phone',

  // Placeholders
  'APP-2024-001': 'APP-2024-001',
  'Nom de l\'application': 'Application name',
  'Sélectionner un chef de projet': 'Select a project manager',

  'client@entreprise.com': 'client@company.com',
  '+216 71 123 456': '+216 71 123 456',
  'Description de l\'application...': 'Application description...',
  
  // Validation Messages
  'Format invalide (APP-AAAA-XXX)': 'Invalid format (APP-YYYY-XXX)',
  'Ce code existe déjà': 'This code already exists',
  'Code disponible': 'Code available',
  'Nom requis (min 3 caractères)': 'Name required (min 3 characters)',
  'Nom du client requis': 'Client name required',
  'Client valide': 'Valid client',
  'Email invalide (domain@use.tn)': 'Invalid email (example@domain.tn)',
  'Numéro invalide (8-15 chiffres)': 'Invalid number (8-15 digits)',
  

  '(statut final)': '(final status)',
  

  'Mise à jour...': 'Updating...',
  'Création...': 'Creating...',
  
 
  // Required fields
  'Champs obligatoires': 'Required fields',
  
  // User limit error
  'Le minimum ne peut pas dépasser le maximum': 'Minimum cannot exceed maximum',
  'Le maximum doit être supérieur ou égal au minimum': 'Maximum must be greater than or equal to minimum',

  'Erreur lors du chargement des chefs de projet': 'Error loading project managers',
'Vous ne pouvez modifier que vos propres applications': 'You can only modify your own applications',
'Application non trouvée': 'Application not found',
'Erreur lors du chargement': 'Error loading',
'Terminé via formulaire': 'Terminated via form',
'Application marquée comme terminée avec succès': 'Application successfully marked as terminated',
'Terminée': 'Terminated',
'jours avant l\'échéance': 'days before deadline',
'jours après l\'échéance': 'days after deadline',
'Terminée le jour de l\'échéance': 'Terminated on deadline day',
'Échec de la mise à jour': 'Update failed',
'Erreur lors de la mise à jour': 'Error during update',
'Application mise à jour avec succès': 'Application updated successfully',
'Application créée avec succès': 'Application created successfully',
'Échec de la création': 'Creation failed',
'Erreur lors de la création': 'Error during creation',
'Le code est requis': 'Code is required',
'Le nom est requis': 'Name is required',
'Le nom du client est requis': 'Client name is required',
'Cette application ne peut pas être terminée': 'This application cannot be terminated',
'Raison de la terminaison (optionnelle):': 'Termination reason (optional):',
'ID d\'application invalide': 'Invalid application ID',
'Erreur lors de la termination': 'Error during termination',
'Le minimum doit être supérieur à 0': 'Minimum must be greater than 0',
'Le maximum doit être supérieur à 0': 'Maximum must be greater than 0',
'Le minimum ne peut pas être supérieur au maximum': 'Minimum cannot be greater than maximum',

'Ancienne convention': 'Old agreement',
'Chef recommandé': 'Recommended manager',


'Details de la demande': 'Request Details',
'Pending': 'Pending',
'Demande de reassignation': 'Reassignment Request',
'Demandeur': 'Requester',
'Destinataire': 'Recipient',
'Agreement': 'Agreement',
'Old agreement': 'Old Agreement',
'Créée le': 'Created on',
'Message': 'Message',
'Recommended manager': 'Recommended Manager',

'Approuvée': 'Approved',

'Confirmez l\'approbation': 'Confirm approval',
'Fournissez une raison': 'Provide a reason',
'Raison du refus': 'Reason for denial',
'Recommandations (optionnel)': 'Recommendations (optional)',
'Recommander un chef': 'Recommend a manager',
'Confirmer le chef recommandé': 'Confirm recommended manager',
'Ajoutez un commentaire...': 'Add a comment...',
'Expliquez pourquoi...': 'Explain why...',
'Suggestions...': 'Suggestions...',

'Cette action marquera l\'application comme terminée': 'This action will mark the application as completed',
'Vous ne pourrez plus modifier cette application après confirmation.': 'You will no longer be able to modify this application after confirmation.',



'Retour': 'Back',
'Supprimer': 'Delete',

'Filtrer': 'Filter',
'Exporter': 'Export',
'Importer': 'Import',
'oui': 'Yes',
'non': 'No',
'actif': 'Active',
'inactif': 'Inactive',
'bloqué': 'Blocked',
'verrouillé': 'Locked',
'déverrouillé': 'Unlocked',
'confirmé': 'Confirmed',
'annulé': 'Cancelled',
'en attente': 'Pending',
'terminé': 'Completed',
'archivé': 'Archived',
'restauré': 'Restored',
'créé': 'Created',
'modifié': 'Modified',
'supprimé': 'Deleted',
'connecté': 'Logged in',
'déconnecté': 'Logged out',
'Acceptation de renouvellement': 'Renewal acceptance',
'Suggestion de réassignation': 'Reassignment suggestion',
'Demande de réassignation': 'Reassignment request',
'Approuvées': 'Approved',
'Refusées': 'Rejected',
'Approve la demande' : 'Approve request',
'Refuse la demande' : 'Reject request',
'Détails de la demande': 'Request details',

'Failed to load requests': 'Failed to load requests',
'La raison est requise': 'Reason is required',
'Demande approuvée avec succès': 'Request approved successfully',
'Demande refusée avec succès': 'Request denied successfully',
'Failed to process request': 'Failed to process request',

'Aucune demande en attente': 'No pending requests',
'Toutes les demandes ont été traitées.': 'All requests have been processed.',
'Aucune demande approuvée': 'No approved requests',
'Les demandes approuvées apparaîtront ici.': 'Approved requests will appear here.',
'Aucune demande refusée': 'No denied requests',
'Les demandes refusées apparaîtront ici.': 'Denied requests will appear here.',
'Aucune demande': 'No requests',
'Vous n\'avez aucune demande pour le moment.': 'You have no requests at the moment.',

'Approuver la demande': 'Approve request',
'Refuser la demande': 'Deny request',
'Message (optionnel)': 'Message (optional)',

'Recommandations': 'Recommendations',

'Il y a 6 jours': '6 days ago',
'Il y a {{days}} jours': '{{days}} days ago',
'Il y a 1 jour': '1 day ago',
'Il y a {{hours}} heures': '{{hours}} hours ago',
'Il y a 1 heure': '1 hour ago',
'Il y a {{minutes}} minutes': '{{minutes}} minutes ago',
'Il y a 1 minute': '1 minute ago',
'À l\'instant': 'Just now',
'Hier': 'Yesterday',
'Aujourd\'hui': 'Today',
'Cette semaine': 'This week',
'Le mois dernier': 'Last month',
'L\'année dernière': 'Last year',



'Bienvenu': 'Welcome',


 
  
  // Insights Cards
  'Alertes': 'Alerts',
  'Urgent': 'Urgent',
  'factures en retard': 'overdue invoices',
  'Meilleur': 'Best',
  'performance': 'performance',
  'Performance': 'Performance',
  'nouvelles conv.': 'new conv.',
  
  // Chart Section
  'Statistique': 'Statistics',
  'Revenus': 'Revenue',
  
  // Overdue Invoices Table
  'Suivi prioritaire': 'Priority follow-up',
  'N°': 'No.',
  
  'Jours': 'Days',
  'Aucune facture': 'No invoices',
 
  
  // Priority Labels (by days overdue)
  'Priorité Critique': 'Critical Priority',
  'Priorité Élevée': 'High Priority',
  'Priorité Moyenne': 'Medium Priority',
  'Priorité Faible': 'Low Priority',
  
'Payée': 'Paid',
'Non Payée':'Unpaid',

'Planifiée':'Planned',
'Gestion des Conventions': 'Agreements Management',
  'Gérez vos conventions commerciales': 'Manage your commercial agreements',
  'Nouvelle Convention': 'New Agreement',
  
  // Messages
  'Voir les {{count}} facture(s) associée(s)': 'View {{count}} associated invoice(s)',
  'Résultat(s)': 'result(s)',
  
  // Filters & Search
  'Effacer la recherche': 'Clear search',
  
  // Folder Tabs
  'Tous': 'All',
  
  'Resp:': 'Resp:',
  'Benef:': 'Benef:',
  'Dates': 'Dates',
  'Déb:': 'Start:',
  'Fin:': 'End:',
  'Périodicité': 'Periodicity',
  'ERP:': 'ERP:',

  'conventions': 'agreements',
  
  // Empty State
  'Aucune convention trouvée': 'No agreements found',
  'Commencez par créer votre première convention': 'Start by creating your first agreement',
  'Créer une convention': 'Create an agreement',
  
  'résultat(s)': 'result(s)',


  'Dossiers classés': 'Filed records',
  'élément(s)': 'item(s)',
  
  // Loading
  'Ouverture des dossiers...': 'Opening folders...',
  
  // Drawer Labels
  'Dossier #': 'Folder #',
  'Période': 'Period',
  'Archivé:': 'Archived:',
  'Aucune raison spécifiée': 'No reason specified',
  
  // Buttons
  'Restaurer': 'Restore',
 
  
  // Restore Modal
  'Restaurer la convention': 'Restore agreement',
  'Cette convention sera déplacée des archives vers la liste active.': 'This agreement will be moved from archives to the active list.',
  'Confirmer': 'Confirm',
  
  // Empty State
  'Le classeur est vide': 'The folder is empty',
  'Aucune convention archivée pour le moment. Les dossiers apparaîtront ici.': 'No archived agreements at the moment. Folders will appear here.',
  
  // Pagination
  'Page {{current}} sur {{total}}': 'Page {{current}} of {{total}}',

  
  'Archiver': 'Archive',
  'Renouveler': 'Renew',
  
  // Convention Info Card
  'Référence ERP': 'ERP Reference',
  'Application associée': 'Associated Application',
  'Structure Bénéficiel': 'Beneficiary Structure',

  
  // Invoices Section
  'Factures associées': 'Associated Invoices',
  'facture(s)': 'invoice(s)',
  'Aucune facture pour cette convention': 'No invoices for this agreement',
  '+ créer une facture': '+ create an invoice',
  'Facturation:': 'Billing:',
  'Échéance:': 'Due date:',
  'Montant TTC:': 'Amount incl. VAT:',
  'voir tout ({{count}})': 'view all ({{count}})',
  'Enregistrer paiement': 'Record payment',
  'Voir détails': 'View details',
  
  // History Section
  'Historique des modifications': 'Modification History',
  'Aucun historique pour cette convention': 'No history for this agreement',
  
  // Versions Link
  'Historique des versions': 'Version History',
  'Voir toutes les versions renouvelées': 'View all renewed versions',
  
  // Financial Stats Card
  'Statistiques financières': 'Financial Statistics',
  'Total factures': 'Total invoices',
  'Factures payées': 'Paid invoices',
  'Factures non payées': 'Unpaid invoices',
  'Factures en retard': 'Overdue invoices',
  'Total payé': 'Total paid',
  'Total impayé': 'Total unpaid',
  
  // Financial Details Card
  'Détails financiers': 'Financial Details',
  'Montant HT': 'Amount excl. VAT',
  'TVA': 'VAT',
  'Montant TTC': 'Amount incl. VAT',
  "Nombre d'utilisateurs": 'Number of users',
  
  // Dates Info Card
  'Informations temporelles': 'Time Information',
  'Date de fin': 'End date',
  'Date de signature': 'Signature date',
  'Jours écoulés': 'Days elapsed',
  'aujourd\'hui': 'today',
  'terminée': 'completed',
  
 
  'archivé le': 'archived on',
  'raison': 'reason',
  
  
  'Référence paiement': 'Payment reference',
  'Date paiement': 'Payment date',
  'ex: VIREMENT-001': 'ex: TRANSFER-001',
  'Confirmer le paiement': 'Confirm payment',
  
  // Archive Modal
  'Archiver la convention': 'Archive agreement',
  "Raison de l'archivage": 'Archiving reason',
  'Pourquoi archivez-vous cette convention ?': 'Why are you archiving this agreement?',
  "Confirmer l'archivage": 'Confirm archiving',
  'Archivage...': 'Archiving...',
  
  // Restore Modal
  'Restaurer la Convention': 'Restore Agreement',
  'Êtes-vous sûr de vouloir restaurer cette convention ?': 'Are you sure you want to restore this agreement?',
  
  // Renew Modal
  'Renouveler la convention': 'Renew agreement',
  'La référence, l\'application et la structure bénéficiel restent identiques': 'The reference, application and beneficiary structure remain identical',
  'Vous pouvez modifier tous les autres champs selon vos besoins.': 'You can modify all other fields as needed.',
  '-- Sélectionner --': '-- Select --',
  'Sans zone': 'Without zone',
  'Ancienne:': 'Previous:',
  'Libellé': 'Label',
  'Date Signature': 'Signature Date',
  'Montant HT (TND)': 'Amount excl. VAT (TND)',
  'TVA (%)': 'VAT (%)',
  'Montant TTC (TND)': 'Amount incl. VAT (TND)',
  'Saisir le nombre': 'Enter the number',
  'Mensuel': 'Monthly',
  'Trimestriel': 'Quarterly',
  'Semestriel': 'Semiannual',
  'Annuel': 'Annual',
  'Confirmer le renouvellement': 'Confirm renewal',
  
  // Loading states
  '✨ chargement...': '✨ loading...',
  'chargement...': 'loading...',


   // Facture title pattern
  'Facture {{numero}} pour la convention {{reference}}': 'Invoice {{numero}} for agreement {{reference}}',
  
  'pour la convention': 'for the agreement',


'Cartes': 'Cards',
'Liste': 'List',
'Montant Total': 'Total Amount',
'Chargement des factures...': 'Loading invoices...',
'Les factures apparaîtront ici': 'Invoices will appear here',
'Facturation': 'Billing',
'Échéance': 'Due date',
'TTC': 'Incl. tax',
'Payer': 'Pay',
'Détails': 'Details',
'N° Facture': 'Invoice No.',
'Date facturation': 'Billing date',
'HT:': 'Excl. tax:',
'Référence de paiement *': 'Payment reference *',
'Date de paiement': 'Payment date',


'Historique de convention': 'Agreement History',
'entrée': 'entry',
's': 's',
'chargement des données': 'loading data',
'aucun historique disponible': 'no history available',
'l\'historique apparaîtra ici quand des modifications seront faites': 'history will appear here when changes are made',
'Modifications': 'Changes',
'Référence': 'Reference',
'Date début': 'Start date',
'Date fin': 'End date',
'Date signature': 'Signature date',
'Nombre utilisateurs': 'Number of users',

  
'Modifier la Convention': 'Edit Agreement',
'Modifiez les informations': 'Edit information',
'Créez une nouvelle convention': 'Create a new agreement',
'✨ Redirection...': '✨ Redirecting...',
'Chargement de la convention...': 'Loading agreement...',
'Référence Convention': 'Agreement Reference',
'Choisir une application': 'Choose an application',
'Toutes les applications ont déjà des conventions': 'All applications already have agreements',
'Structure Beneficiel': 'Beneficiary Structure',
'Choisir une structure beneficiel': 'Choose a beneficiary structure',
'Choisir une structure responsable': 'Choose a responsible structure',
'Modalités de Paiement': 'Payment Terms',
'Sélectionner': 'Select',
'Libellé de la convention': 'Agreement label',
'Auto': 'Auto',
'Sélectionner une date': 'Select a date',
'15 jours min. après début': '15 days min. after start',
'À partir d\'aujourd\'hui': 'From today',
'Min:': 'Min:',
'Max:': 'Max:',
'Règle:': 'Rule:',
'Mise à jour automatique': 'Automatic update',
'Les dates de': 'The dates of',
'seront synchronisées': 'will be synchronized',


'utilisateurs': 'users',
'Failed to load applications': 'Failed to load applications',
'Erreur lors du chargement de la convention': 'Error loading agreement',
'Erreur lors de la détermination du nombre d\'utilisateurs': 'Error determining number of users',
'Veuillez d\'abord sélectionner une application': 'Please select an application first',
'Le format de référence doit être: CONV-YYYY-XXX (ex: CONV-2024-001)': 'Reference format must be: CONV-YYYY-XXX (ex: CONV-2024-001)',
'Structure interne est requise': 'Internal structure is required',
'Structure beneficiel est requise': 'Beneficiary structure is required',
'Périodicité est requise': 'Periodicity is required',
'Le montant HT est requis et doit être supérieur à 0': 'Amount excl. tax is required and must be greater than 0',
'Le nombre d\'utilisateurs est requis et doit être supérieur à 0': 'Number of users is required and must be greater than 0',
'Convention mise à jour avec succès': 'Agreement updated successfully',
'Convention créée avec succès': 'Agreement created successfully',
'Échec de la mise à jour de la convention': 'Failed to update agreement',
'Échec de la création de la convention': 'Failed to create agreement',
'Versions de convention': 'Agreement Versions',
'Aucune convention sélectionnée': 'No agreement selected',
'Voir en mode split': 'View in split mode',
'Voir en mode grille': 'View in grid mode',
'Total versions': 'Total versions',
'Première version': 'First version',
'Dernière archivée': 'Last archived',
'Version actuelle': 'Current version',
'Chargement des versions...': 'Loading versions...',
'Réessayer': 'Retry',
'Versions archivées': 'Archived versions',
'Anciennes versions': 'Old versions',
'Archivée le': 'Archived on',
'Raison:': 'Reason:',
'Informations générales': 'General information',
'Signature:': 'Signature:',
'Code:': 'Code:',
'Factures archivées': 'Archived invoices',
'voir tout': 'view all',
'Sélectionnez une version': 'Select a version',
'Cliquez sur une version dans la liste de gauche': 'Click on a version in the left list',
'Aucune version archivée': 'No archived versions',
'Cette convention n\'a pas encore été renouvelée.': 'This agreement has not been renewed yet.',
'Retour à la convention': 'Back to agreement',
'Numéro facture': 'Invoice number',
'Payée le': 'Paid on',
'Réf:': 'Ref:',
'Total :': 'Total:',
'Voir tous les détails →': 'View all details →',


'Note:': 'Note:',
'Vous êtes le créateur de cette application. Si vous ne pouvez pas continuer à travailler dessus, vous pouvez demander une réassignation.': 'You are the creator of this application. If you cannot continue working on it, you can request a reassignment.',
'Raison de la demande': 'Reason for request',
'Expliquez pourquoi vous ne pouvez pas continuer à travailler sur cette application...': 'Explain why you cannot continue working on this application...',
'Recommander un chef de projet': 'Recommend a project manager',
'Chargement des chefs...': 'Loading managers...',
'Aucun autre chef de projet disponible': 'No other project managers available',
'Recommandations supplémentaires': 'Additional recommendations',
'optionnel': 'optional',
'Informations supplémentaires pour l\'administrateur...': 'Additional information for the administrator...',
'Envoyer la demande': 'Send request',
'Élevé': 'High',
'Faible': 'Low',

'Charge faible': 'Low load',
'Charge élevée': 'High load',
'Charge critique': 'Critical load',
'Charge moyenne': 'Medium load',


'Gestion des factures': 'Invoice Management',
'Suivez et gérez vos factures': 'Track and manage your invoices',
'Non Payées': 'Unpaid',
'Toutes apps': 'All apps',
'filtres:': 'filters:',
'tout': 'clear all',
'Délai': 'Delay',
'Envoyer relance': 'Send reminder',
'Envoyer par email': 'Send by email',
'Nouveau paiement': 'New payment',
'Valider le paiement': 'Confirm payment',
'Non payée': 'Unpaid',
'Non Payé': 'Unpaid',
'FACTURE': 'INVOICE',
'ÉMETTEUR': 'ISSUER',
'CLIENT': 'CLIENT',
'Création:': 'Creation:',
'DÉTAILS': 'DETAILS',
'Prestation': 'Service',
'Sous-total HT': 'Subtotal excl. VAT',
'TOTAL TTC': 'TOTAL incl. VAT',
'Paiement reçu': 'Payment received',
'avant': 'before',
'de retard': 'late',
'à temps': 'on time',
'payée': 'paid',
'Échéance aujourd\'hui': 'Due today',
'Générée automatiquement • Merci de votre confiance': 'Generated automatically • Thank you for your trust',
'Valider': 'Confirm',


'Nouveau message': 'New message',
'Menu': 'Menu',
'Boîte de réception': 'Inbox',
'Messages importants': 'Important messages',
'Brouillons': 'Drafts',
'Messages envoyés': 'Sent messages',
'Corbeille': 'Trash',
'Groupes': 'Groups',
'Aucun groupe': 'No groups',
'Gérer les groupes': 'Manage groups',
'Archive': 'Archive',
'Brouillon': 'Draft',
'Mode édition:': 'Edit mode:',
'Vous modifiez un brouillon': 'You are editing a draft',
'À': 'To',
'Masquer CC': 'Hide CC',
'Ajouter CC': 'Add CC',
'Masquer BCC': 'Hide BCC',
'Ajouter BCC': 'Add BCC',
'CC': 'CC',
'BCC': 'BCC',
'Objet': 'Subject',
'Objet du message': 'Message subject',
'Écrivez votre message ici...': 'Write your message here...',
'Joindre des fichiers': 'Attach files',
'Max 10MB par fichier': 'Max 10MB per file',
'Envoi...': 'Sending...',
'Envoyer': 'Send',
'Aucun message': 'No messages',
'(Sans objet)': '(No subject)',
'membres': 'members',
'Pièces jointes': 'Attachments',
'Répondre': 'Reply',
'Chargement du PDF...': 'Loading PDF...',
'Aperçu PDF non disponible': 'PDF preview not available',
'Télécharger le PDF': 'Download PDF',
'Aperçu non disponible pour ce type de fichier': 'Preview not available for this file type',
'Télécharger': 'Download',
'À:': 'To:',
'Tout sélectionner': 'Select all',
'Désélectionner tout': 'Deselect all',
'Marquer comme lu': 'Mark as read',
'Marquer comme non lu': 'Mark as unread',
'Déplacer vers': 'Move to',
'Supprimer définitivement': 'Delete permanently',
'Paramètres': 'Settings',
'Aide': 'Help',
'Notifications': 'Notifications',
'Thème sombre': 'Dark theme',
'Thème clair': 'Light theme',
'Langue': 'Language',
'Français': 'French',
'Anglais': 'English',
'Arabe': 'Arabic',

'Oops! La page que vous recherchez n\'existe pas.' : 'Oops! The page you are looking for does not exist.',
'Retourner': 'Back',

'Groups': 'Groups',
'New Group': 'New Group',
'System Groups': 'System Groups',
'System': 'System',
'members': 'members',
'more': 'more',
'My Groups': 'My Groups',
'View members': 'View members',
'Edit group': 'Edit group',
'Delete group': 'Delete group',
'View only group': 'View only group',
'No groups yet': 'No groups yet',
'Create your first group to start organizing your contacts': 'Create your first group to start organizing your contacts',
'Create Group': 'Create Group',
'Edit Group': 'Edit Group',
'Create New Group': 'Create New Group',
'Group Name': 'Group Name',
'Enter group name': 'Enter group name',
'Description (optional)': 'Description (optional)',
'Group description': 'Group description',
'Members': 'Members',
'Cancel': 'Cancel',
'Update': 'Update',
'Create': 'Create',
'Close': 'Close',


'Consultez et gérez toutes vos notifications': 'View and manage all your notifications',
'Listes': 'List',
'Tout marquer comme lu': 'Mark all as read',
'Chargement des notifications...': 'Loading notifications...',
'Aucune notification': 'No notifications',
'Vous n\'avez pas encore de notifications.': 'You don\'t have any notifications yet.',
'Non lu': 'Unread',
'Vous avez atteint la fin de vos notifications': 'You have reached the end of your notifications',
'Créé le': 'Created on',
'Notifications envoyées': 'Notifications sent',
'Facture à échéance dans 2 jours': 'Invoice due in 2 days',
'Facture à échéance dans 3 jours': 'Invoice due in 3 days',
'Facture à échéance dans 4 jours': 'Invoice due in 4 days',
'Facture à échéance dans 5 jours': 'Invoice due in 5 days',
'Facture à échéance aujourd\'hui': 'Invoice due today',
'Facture à échéance demain': 'Invoice due tomorrow',
'Facture en retard': 'Invoice overdue',
'jours en retard' : 'days overdue',

// English
'Demain': 'Tomorrow',
'Dans {{days}} jours': 'In {{days}} days',
'{{days}} jours en retard': '{{days}} days late',

'Tout marquer lu': 'Mark all read',
'Voir toutes les notifications': 'View all notifications',
'nouvelle': 'new',
'nouvelle s': 'new',


'Échec du chargement des notifications': 'Failed to load notifications',
'Échec du marquage de la notification comme lue': 'Failed to mark notification as read',
'Échec du marquage de toutes les notifications comme lues': 'Failed to mark all notifications as read',
'Échec de la suppression de la notification': 'Failed to delete notification',

'Dans X jours': 'In X days',
'X jours en retard': 'X days late',
'Il y a X minutes': 'X minutes ago',
'Il y a X heures': 'X hours ago',
'Il y a X jours': 'X days ago',
'Désactiver': 'Disable',
'Configurer la 2FA': 'Configure 2FA',
'Scannez le QR code avec votre application': 'Scan the QR code with your app',
'Entrez le code à 6 chiffres': 'Enter the 6-digit code',
'Vérification...': 'Verifying...',
'Vérifier': 'Verify',
'Sauvegardez vos codes de secours !': 'Save your backup codes!',
'Ces codes vous permettent d\'accéder à votre compte si vous perdez votre téléphone.': 'These codes allow you to access your account if you lose your phone.',
'Copier': 'Copy',
'Désactiver la 2FA': 'Disable 2FA',
'Entrez votre code pour désactiver': 'Enter your code to disable',
'Code de vérification': 'Verification code',
'Code requis': 'Code required',
'Doit contenir 6 chiffres': 'Must contain 6 digits',
'Désactivation...': 'Disabling...',

'Commercial Metier' : 'Commercial Metier',


// French to English
'Loading ...': 'Loading ...',
'Calendrier des factures': 'Invoice Calendar',
'Suivre les dates et paiements': 'Track dates and payments',
'Prochain': 'Upcoming',
'Cette Semaine': 'This Week',
'Ce mois': 'This Month',
'Impayé': 'Unpaid',
'Masquer': 'Hide',
'Afficher': 'Show',
'Tout est en ordre !': 'Everything is in order!',
'Actions rapides': 'Quick Actions',
'Détails facture': 'Invoice Details',
'Notes': 'Notes',
'Voir facture': 'View invoice',
'Mois': 'Month',
'Semaine': 'Week',
'Jour': 'Day',
'Toute la journée': 'All day',
'Inconnu': 'Unknown',
'Erreur lors du chargement des événements du calendrier': 'Error loading calendar events',
'Erreur lors du chargement des statistiques du calendrier': 'Error loading calendar statistics',
'Erreur lors du chargement des factures à venir': 'Error loading upcoming invoices',
'Erreur lors du chargement des factures en retard': 'Error loading overdue invoices',


    },
    ar: {
      'Chef Projet' : 'مدير المشروع',
      'Admin' : 'مدير النظام',
      'Decideur' : 'صانع القرار',

      'Tout marquer lu': 'تحديد الكل كمقروء',
'Voir toutes les notifications': 'عرض جميع الإشعارات',
'nouvelle': 'جديدة',
'nouvelle s': 'جديدة',
      'Groups': 'المجموعات',
'New Group': 'مجموعة جديدة',
'System Groups': 'المجموعات النظامية',
'System': 'نظامية',
'members': 'عضواً',
'more': 'المزيد',
'My Groups': 'مجموعاتي',
'View members': 'عرض الأعضاء',
'Edit group': 'تعديل المجموعة',
'Delete group': 'حذف المجموعة',
'View only group': 'مجموعة للعرض فقط',
'No groups yet': 'لا توجد مجموعات بعد',
'Create your first group to start organizing your contacts': 'أنشئ مجموعتك الأولى لتنظيم جهات اتصالك',
'Create Group': 'إنشاء مجموعة',
'Edit Group': 'تعديل المجموعة',
'Create New Group': 'إنشاء مجموعة جديدة',
'Group Name': 'اسم المجموعة',
'Enter group name': 'أدخل اسم المجموعة',
'Description (optional)': 'الوصف (اختياري)',
'Group description': 'وصف المجموعة',
'Members': 'الأعضاء',
'Cancel': 'إلغاء',
'Update': 'تحديث',
'Create': 'إنشاء',
'Close': 'إغلاق',

      'Oops! La page que vous recherchez n\'existe pas.' : 'عذرًا! الصفحة التي تبحث عنها غير موجودة.',
'Retourner': 'العودة',

      'new app ' : 'تطبيق جديد',
       'Facture {{numero}} pour la convention {{reference}}': 'فاتورة {{numero}} للاتفاقية {{reference}}',
  
  // Alternative if you need different format
  'pour la convention': 'للاتفاقية',

      'résultat(s)' :' نتيجة(نتائج)',
      'Planifiée' :'مخطط لها',

'Payée': 'مدفوعة',
'Non Payée': 'غير مدفوعة',

      'Failed to load requests': 'فشل تحميل الطلبات',
'La raison est requise': 'السبب مطلوب',
'Demande approuvée avec succès': 'تمت الموافقة على الطلب بنجاح',
'Demande refusée avec succès': 'تم رفض الطلب بنجاح',
'Failed to process request': 'فشل معالجة الطلب',
'Acceptation de renouvellement': 'قبول التجديد',
'Suggestion de réassignation': 'اقتراح إعادة التعيين',
'Demande de réassignation': 'طلب إعادة التعيين',
'Toutes': 'الكل',
'En attente': 'قيد الانتظار',
'Approuvées': 'تمت الموافقة',
'Refusées': 'مرفوضة',
'Aucune demande en attente': 'لا توجد طلبات قيد الانتظار',
'Toutes les demandes ont été traitées.': 'تمت معالجة جميع الطلبات.',
'Aucune demande approuvée': 'لا توجد طلبات موافق عليها',
'Les demandes approuvées apparaîtront ici.': 'ستظهر الطلبات الموافق عليها هنا.',
'Aucune demande refusée': 'لا توجد طلبات مرفوضة',
'Les demandes refusées apparaîtront ici.': 'ستظهر الطلبات المرفوضة هنا.',
'Aucune demande': 'لا توجد طلبات',
'Vous n\'avez aucune demande pour le moment.': 'ليس لديك أي طلبات في الوقت الحالي.',
'Voir': 'عرض',
'Approuver': 'موافقة',
'Refuser': 'رفض',
'Message': 'الرسالة',
'Chef recommandé': 'المدير الموصى به',
'Approuver la demande': 'الموافقة على الطلب',
'Refuser la demande': 'رفض الطلب',
'Message (optionnel)': 'رسالة (اختياري)',
'Raison du refus': 'سبب الرفض',
'Recommandations (optionnel)': 'توصيات (اختياري)',
'Recommander un chef': 'توصية بمدير',
'Confirmer le chef recommandé': 'تأكيد المدير الموصى به',
'Confirmez l\'approbation': 'تأكيد الموافقة',
'Fournissez une raison': 'قدم سبباً',
'Annuler': 'إلغاء',
'Traitement...': 'جاري المعالجة...',
'Détails de la demande': 'تفاصيل الطلب',
'Type': 'النوع',
'Demandeur': 'مقدم الطلب',
'Destinataire': 'المستلم',
'Application': 'التطبيق',
'Convention': 'الاتفاقية',
'Ancienne convention': 'الاتفاقية السابقة',
'Créée le': 'تاريخ الإنشاء',
'Recommandations': 'التوصيات',
'Fermer': 'إغلاق',
'Statut': 'الحالة',

       
'Details de la demande': 'تفاصيل الطلب',
'Pending': 'قيد الانتظار',
'Demande de reassignation': 'طلب إعادة التعيين',

'Agreement': 'الاتفاقية',
'Old agreement': 'الاتفاقية السابقة',

'Recommended manager': 'المدير الموصى به',

'Approuvée': 'تمت الموافقة',
'Refusée': 'مرفوض',


'Ajoutez un commentaire...': 'أضف تعليقاً...',
'Expliquez pourquoi...': 'اشرح السبب...',
'Suggestions...': 'اقتراحات...',
'Raison de la terminaison': 'سبب الإنهاء',
'optionnelle': 'اختياري',
'Statut actuel': 'الحالة الحالية',
'Date de début': 'تاريخ البداية',
'Date de fin prévue': 'تاريخ النهاية المتوقع',
'Jours restants': 'الأيام المتبقية',
'Dépassé': 'متأخر',
'Action irréversible': 'إجراء لا رجعة فيه',
'Cette action marquera l\'application comme terminée': 'سيؤدي هذا الإجراء إلى وضع علامة على التطبيق كمكتمل',
'Vous ne pourrez plus modifier cette application après confirmation.': 'لن تتمكن من تعديل هذا التطبيق بعد التأكيد.',
'Récapitulatif': 'ملخص',
'Confirmer la terminaison': 'تأكيد الإنهاء',
'Redirection en cours...': 'جاري إعادة التوجيه...',
'Chargement...': 'جاري التحميل...',
'Chargement des détails...': 'جاري تحميل التفاصيل...',
'Informations clés': 'معلومات أساسية',
'Min': 'الحد الأدنى',
'Max': 'الحد الأقصى',
'Début': 'البداية',
'Fin': 'النهاية',
'Durée totale': 'المدة الإجمالية',
'Progression': 'التقدم',
'Structure Bénéficiaire': 'الهيكل المستفيد',
'Structure Responsable': 'الهيكل المسؤول',
'Non défini': 'غير محدد',
'Conventions': 'الاتفاقيات',
'Aucune convention associée': 'لا توجد اتفاقيات مرتبطة',
'Factures:': 'الفواتير:',
'Payées:': 'المدفوعة:',
'Montant': 'المبلغ',
'Users': 'المستخدمين',
'Chef de Projet': 'مدير المشروع',
'Assigner': 'تعيين',
'Aucun chef assigné': 'لم يتم تعيين مدير',
'Charge de travail': 'عبء العمل',
'Projets actifs': 'المشاريع النشطة',
'Historique': 'السجل',
'Rafraîchir': 'تحديث',
'Aucun historique': 'لا يوجد سجل',
'Voir tout': 'عرض الكل',
'Modifier l\'Application': 'تعديل التطبيق',
'Nouvelle Application': 'تطبيق جديد',
'Modifiez les informations de l\'application': 'تعديل معلومات التطبيق',
'Créez une nouvelle application': 'إنشاء تطبيق جديد',
'Code Application': 'رمز التطبيق',
'Code disponible': 'الرمز متاح',
'Ce code existe déjà': 'هذا الرمز موجود بالفعل',
'Format invalide (APP-AAAA-XXX)': 'تنسيق غير صالح (APP-YYYY-XXX)',
'Nom de l\'Application': 'اسم التطبيق',
'Nom valide': 'اسم صالح',
'Sélectionner un chef de projet': 'اختر مدير مشروع',
'Non assigné': 'غير معين',
'Utilisateurs': 'المستخدمين',
'Minimum': 'الحد الأدنى',
'Maximum': 'الحد الأقصى',
'Client': 'العميل',
'Email Client': 'البريد الإلكتروني للعميل',
'Email valide': 'بريد إلكتروني صالح',
'Email invalide (domain@use.tn)': 'بريد إلكتروني غير صالح (domain@use.tn)',
'Téléphone Client': 'هاتف العميل',
'Téléphone valide': 'هاتف صالح',
'Numéro invalide (8-15 chiffres)': 'رقم غير صالح (8-15 رقماً)',
'Description': 'الوصف',
'Mise à jour...': 'جاري التحديث...',
'Création...': 'جاري الإنشاء...',
'Mettre à jour': 'تحديث',
'Créer': 'إنشاء',
'Retour': 'رجوع',
'Modifier': 'تعديل',
'Supprimer': 'حذف',
'Ajouter': 'إضافة',
'Rechercher': 'بحث',
'Filtrer': 'تصفية',
'Exporter': 'تصدير',
'Importer': 'استيراد',
'Actualiser': 'تحديث',
'oui': 'نعم',
'non': 'لا',
'actif': 'نشط',
'inactif': 'غير نشط',
'bloqué': 'محظور',
'verrouillé': 'مقفل',
'déverrouillé': 'مفتوح',
'confirmé': 'مؤكد',
'annulé': 'ملغي',
'en attente': 'قيد الانتظار',
'terminé': 'مكتمل',
'archivé': 'مؤرشف',
'restauré': 'تمت الاستعادة',
'créé': 'تم الإنشاء',
'modifié': 'تم التعديل',
'supprimé': 'تم الحذف',
'connecté': 'تم تسجيل الدخول',
'déconnecté': 'تم تسجيل الخروج',

'Erreur lors du chargement des chefs de projet': 'خطأ في تحميل مديري المشاريع',
'Vous ne pouvez modifier que vos propres applications': 'يمكنك تعديل تطبيقاتك الخاصة فقط',
'Application non trouvée': 'التطبيق غير موجود',
'Erreur lors du chargement': 'خطأ في التحميل',
'Terminé via formulaire': 'تم الإنهاء عبر النموذج',
'Application marquée comme terminée avec succès': 'تم وضع علامة على التطبيق كمكتمل بنجاح',
'Terminée': 'مكتمل',
'jours avant l\'échéance': 'أيام قبل الموعد النهائي',
'jours après l\'échéance': 'أيام بعد الموعد النهائي',
'Terminée le jour de l\'échéance': 'تم الإنهاء في يوم الموعد النهائي',
'Échec de la mise à jour': 'فشل التحديث',
'Erreur lors de la mise à jour': 'خطأ أثناء التحديث',
'Application mise à jour avec succès': 'تم تحديث التطبيق بنجاح',
'Application créée avec succès': 'تم إنشاء التطبيق بنجاح',
'Échec de la création': 'فشل الإنشاء',
'Erreur lors de la création': 'خطأ أثناء الإنشاء',
'Le code est requis': 'الرمز مطلوب',
'Le nom est requis': 'الاسم مطلوب',
'Le nom du client est requis': 'اسم العميل مطلوب',
'Cette application ne peut pas être terminée': 'لا يمكن إكمال هذا التطبيق',
'Raison de la terminaison (optionnelle):': 'سبب الإكمال (اختياري):',
'ID d\'application invalide': 'معرف التطبيق غير صالح',
'Erreur lors de la termination': 'خطأ أثناء الإكمال',
'Le minimum doit être supérieur à 0': 'الحد الأدنى يجب أن يكون أكبر من 0',
'Le maximum doit être supérieur à 0': 'الحد الأقصى يجب أن يكون أكبر من 0',
'Le minimum ne peut pas être supérieur au maximum': 'الحد الأدنى لا يمكن أن يكون أكبر من الحد الأقصى',
'Planifié': 'مخطط',
'En Cours': 'قيد التنفيذ',
'Terminé': 'مكتمل',
'Assignation chef de projet': 'تعيين مدير المشروع',
  // Buttons
  'Terminer': 'إنهاء',
  'Réassignation': 'إعادة تعيين',

  'Assigner un Chef de Projet': 'تعيين مدير مشروع',
  'Historique complet': 'السجل الكامل',
  "Terminer l'application": "إنهاء التطبيق",
  
  // Sidebar - Informations clés

  'jours': 'أيام',
  'j': 'ي',
  
  // Termination Card
  'Terminaison': 'الإنهاء',
  'Date': 'التاريخ',
  'Par': 'بواسطة',
  'Système': 'النظام',
  'En avance': 'مبكراً',
  'À temps': 'في الوقت المحدد',
  'En retard': 'متأخر',
  
  // Description Card
  'Aucune description fournie': 'لا يوجد وصف',
  
  // Structures Card
  'Structures': 'الهياكل',

  

  'Factures': 'الفواتير',
  'Payées': 'مدفوعة',

  

 
  'Voir tout ({{count}})': 'عرض الكل ({{count}})',
  
  // Modals
  'Cette action marquera l\'application comme terminée. Vous ne pourrez plus modifier cette application après confirmation.': 'سيؤدي هذا الإجراء إلى وضع علامة على التطبيق كمكتمل. لن تتمكن من تعديل هذا التطبيق بعد التأكيد.',

  'Ex: Projet livré, contrat terminé, etc...': 'مثال: تم تسليم المشروع، انتهاء العقد، إلخ...',

  // Status Labels (Application)
  'EN_COURS': 'قيد التنفيذ',
  'PLANIFIE': 'مخطط',
  'TERMINE': 'مكتمل',
  'ARCHIVEE': 'مؤرشف',
  
  // Convention Status Labels
  'ACCEPTEE': 'مقبولة',
  'EN_ATTENTE': 'قيد الانتظار',
  'REFUSEE': 'مرفوضة',
  'SIGNEE': 'موقعة',
  'RESILIEE': 'ملغاة',

 'TND' :'دينار تونسي',
  
  
  // Archive status
  'Archivée': 'مؤرشف',
  
  // History messages
  'Aucun historique disponible': 'لا يوجد سجل متاح',
  


  // Placeholders
  'APP-2024-001': 'APP-2024-001',
  'Nom de l\'application': 'اسم التطبيق',
  'Nom du client': 'اسم العميل',
  'client@entreprise.com': 'client@entreprise.com',
  '+216 71 123 456': '+216 71 123 456',
  'Description de l\'application...': 'وصف التطبيق...',
  
  
  'Nom requis (min 3 caractères)': 'الاسم مطلوب (3 أحرف على الأقل)',
  'Nom du client requis': 'اسم العميل مطلوب',
  'Client valide': 'عميل صالح',


  '(statut final)': '(حالة نهائية)',
  

  // Workload Status Classes
  
  // Required fields
  'Champs obligatoires': 'حقول إلزامية',
  
  // User limit error
  'Le minimum ne peut pas dépasser le maximum': 'الحد الأدنى لا يمكن أن يتجاوز الحد الأقصى',
  'Le maximum doit être supérieur ou égal au minimum': 'الحد الأقصى يجب أن يكون أكبر من أو يساوي الحد الأدنى',

  
  // Success/Error messages
  'Succès': 'نجاح',
  'Erreur': 'خطأ',
  

      // History Modal - Headers & Labels
'Historique de l\'utilisateur': 'سجل المستخدم',
'Aucun historique trouvé': 'لم يتم العثور على سجل',


// History Entry Types
'Connexion': 'تسجيل دخول',
'Déconnexion': 'تسجيل خروج',
'Création': 'إنشاء',
'Modification': 'تعديل',
'Suppression': 'حذف',
'Archivage': 'أرشفة',
'Restauration': 'استعادة',
'Renouvellement': 'تجديد',
'Paiement': 'دفع',
'Changement de statut': 'تغيير الحالة',
'Synchronisation des dates': 'مزامنة التواريخ',
'Mise à jour financière': 'تحديث مالي',
'Retard de paiement': 'تأخر في الدفع',

// Entity Types
'Utilisateur': 'مستخدم',
'Facture': 'فاتورة',

// Field Names
'statutPaiement': 'حالة الدفع',
'referencePaiement': 'مرجع الدفع',
'datePaiement': 'تاريخ الدفع',
'montantTTC': 'المبلغ شامل الضريبة',
'montantHT': 'المبلغ غير شامل الضريبة',
'tva': 'ضريبة القيمة المضافة',
'numeroFacture': 'رقم الفاتورة',
'dateFacturation': 'تاريخ الفوترة',
'dateEcheance': 'تاريخ الاستحقاق',
'etat': 'الحالة',
'dateDebut': 'تاريخ البداية',
'dateFin': 'تاريخ النهاية',
'periodicite': 'الدورية',
'libelle': 'التسمية',
'referenceERP': 'مرجع ERP',
'nbUsers': 'عدد المستخدمين',
'archived': 'مؤرشف',
'joursRetard': 'أيام التأخير',

// Status Values
'NON_PAYE': 'غير مدفوع',
'NON PAYÉE': 'غير مدفوعة',
'PAYE': 'مدفوع',
'PAYÉE': 'مدفوعة',
'EN_RETARD': 'متأخر',


// Periodicity Values
'MENSUEL': 'شهري',
'BIMESTRIEL': 'شهرين',
'TRIMESTRIEL': 'ربع سنوي',
'SEMESTRIEL': 'نصف سنوي',
'ANNUEL': 'سنوي',

// Yes/No
'Oui': 'نعم',
'Non': 'لا',

// User status
'Actif': 'نشط',
'Bloqué': 'محظور',
'Verrouillé': 'مقفل',

// Additional UI text
'Logo': 'الشعار',
'AA': 'AA',
'decideur': 'صانع القرار',
'commercial 1': 'مندوب مبيعات 1',
'nousseiba': 'نصيبة',
'kaabi': 'كعبي',
'YES': 'نعم',
'NO': 'لا',
'email': 'البريد الإلكتروني',
'email email': 'البريد الإلكتروني',
'avatar': 'الصورة الرمزية',

'Role': 'الدور',
'Rôle': 'الدور',
'Rôles': 'الأدوار',
'Roles': 'الأدوار',
'Verrouillé par l\'administrateur': 'مقفل من قبل المسؤول',
'Temporairement bloqué': 'محظور مؤقتاً',
'tentatives échouées': 'محاولات فاشلة',
'Confirmer le verrouillage de l\'utilisateur': 'تأكيد قفل المستخدم',
'Confirmer le déverrouillage de l\'utilisateur': 'تأكيد فتح المستخدم',
'Êtes-vous sûr de vouloir verrouiller': 'هل أنت متأكد من رغبتك في قفل',
'Ils ne pourront pas accéder à leur compte tant qu\'il n\'est pas déverrouillé.': 'لن يتمكنوا من الوصول إلى حسابهم حتى يتم فتحه.',
'Êtes-vous sûr de vouloir déverrouiller': 'هل أنت متأكد من رغبتك في فتح',
'Ils retrouveront l\'accès à leur compte.': 'سيعودون إلى حسابهم.',
'Successfully assigned': 'تم التعيين بنجاح',
'project(s) to': 'مشروع(مشاريع) إلى',
'Assigned': 'تم التعيين',
'project(s), failed to assign': 'مشروع(مشاريع)، فشل التعيين',
'project(s)': 'مشروع(مشاريع)',
'Mot de passe': 'كلمة المرور',
'Zone géographique': 'المنطقة الجغرافية',
'Type de structure': 'نوع الهيكل',
'-- Sélectionnez --': '-- اختر --',
'Nom du client valide': 'اسم العميل صالح',
'Code généré:': 'الرمز المُنشأ:',
'Description de la structure...': 'وصف الهيكل...',
'Description de la zone...': 'وصف المنطقة...',

      'Gérer les utilisateurs et surveiller les métriques système': 'إدارة المستخدمين ومراقبة مقاييس النظام',

'Une erreur s\'est produite': 'حدث خطأ',
'OK': 'حسناً',
'Nombre total d\'utilisateurs': 'إجمالي المستخدمين',
'Utilisateurs bloqués': 'المستخدمون المحظورون',
'Tentatives échouées': 'محاولات فاشلة',
'Utilisateurs actifs': 'المستخدمون النشطون',
'Liste des utilisateurs': 'قائمة المستخدمين',
'Filtrer par rôle': 'تصفية حسب الدور',
'Tous les rôles': 'جميع الأدوار',
'Commercial Métier': 'مندوب مبيعات',
'Filtrer par statut': 'تصفية حسب الحالة',
'Tous les statuts': 'جميع الحالات',
'Bloqué par l\'administrateur': 'محظور من قبل المسؤول',
'Effacer': 'مسح',
'Rôle:': 'الدور:',
'Statut:': 'الحالة:',
'Chargement des utilisateurs...': 'جاري تحميل المستخدمين...',
'Département': 'القسم',
'Non attribué': 'غير محدد',
'Voir l\'historique': 'عرض التاريخ',
'Verrouiller': 'قفل',
'Vérrouiller': 'قفل',
'Déverrouiller': 'فتح',
'Assigner Applications': 'تعيين التطبيقات',
'Affichage': 'عرض',
'à': 'إلى',
'sur': 'من',
'résultats': 'نتائج',
'Aucun utilisateur trouvé': 'لم يتم العثور على مستخدمين',
'Ajouter un utilisateur': 'إضافة مستخدم',
'Ajouter un nouvel utilisateur': 'إضافة مستخدم جديد',
'Nom d\'utilisateur requis': 'اسم المستخدم مطلوب',
'Minimum 3 caractères requis': '3 أحرف كحد أدنى',
'Nom d\'utilisateur valide': 'اسم مستخدم صالح',
'Email requis': 'البريد الإلكتروني مطلوب',
'Email invalide (ex: user@domain.nn)': 'بريد إلكتروني غير صالح (مثال: user@domain.nn)',
'Mot de passe requis': 'كلمة المرور مطلوبة',
'Minimum 8 caractères requis': '8 أحرف كحد أدنى',
'Mot de passe valide': 'كلمة مرور صالحة',
'Lettres seulement': 'حروف فقط',
'Numéro de téléphone': 'رقم الهاتف',
'8 chiffres minimum': '8 أرقام كحد أدنى',
'Lettres, chiffres et espaces seulement': 'حروف وأرقام ومسافات فقط',
'Au moins un rôle est requis': 'مطلوب دور واحد على الأقل',
'Résumé': 'ملخص',
'Nouvel utilisateur': 'مستخدم جديد',
'Accès': 'الوصول',
'Sélectionner un rôle': 'اختر دوراً',
'Contact': 'جهة اتصال',
'Département pour': 'القسم لـ',
'Département est requis': 'القسم مطلوب',
'Rôles pour': 'الأدوار لـ',
'Sélectionner le rôle': 'اختر الدور',
'Veuillez confirmer votre action': 'يرجى تأكيد إجراءك',
'Assigner les applications': 'تعيين التطبيقات',
'Assignation à': 'تعيين إلى',
'Aucune application non assignée': 'لا توجد تطبيقات غير معينة',
'Toutes les applications sont déjà assignées.': 'جميع التطبيقات تم تعيينها بالفعل.',
'Charge de travail actuelle': 'عبء العمل الحالي',
'Charge globale': 'الحمل الإجمالي',
'Apps': 'التطبيقات',
'Valeur': 'القيمة',
'Durée': 'المدة',
'Forcer l\'assignation': 'فرض التعيين',
'application(s) sélectionnée(s)': 'تطبيق(ات) مختارة',
'Assignation...': 'جاري التعيين...',

'Accès Refusé': 'تم رفض الوصول',
'Retour au Tableau de Bord': 'العودة إلى لوحة التحكم',

// Nomenclature Section
'Gestion des nomenclatures': 'إدارة المصطلحات',
'Gérez vos zones géographiques et structures organisationnelles': 'إدارة المناطق الجغرافية والهياكل التنظيمية',
'Zones': 'المناطق',
'Marchés couverts': 'الأسواق المغطاة',
'Responsables': 'المسؤولون',
'Autorité de gestion': 'سلطة الإدارة',
'Bénéficiels': 'المستفيدون',
'Acquéreur / Client': 'المشتري / العميل',
'Zones géographiques': 'المناطق الجغرافية',
'Structures Responsables': 'الهياكل المسؤولة',
'Structures Bénéficiels': 'الهياكل المستفيدة',
'Rechercher...': 'بحث...',
'Code': 'الرمز',
'Nom': 'الاسم',
'Actions': 'الإجراءات',
'Aucune zone trouvée': 'لم يتم العثور على مناطق',
'+ Ajouter une zone': '+ إضافة منطقة',
'Zone': 'المنطقة',
'Non spécifié': 'غير محدد',
'Aucune structure trouvée': 'لم يتم العثور على هياكل',
'+ Ajouter une structure': '+ إضافة هيكل',
'Page': 'صفحة',
'Précédent': 'السابق',
'Suivant': 'التالي',
'Description longue': 'وصف طويل',
'caractères': 'حرف',
'Code requis et doit être unique': 'الرمز مطلوب ويجب أن يكون فريداً',
'Code valide': 'رمز صالح',
'Nom requis (minimum 2 caractères)': 'الاسم مطلوب (حرفان كحد أدنى)',
'Format téléphone invalide': 'تنسيق هاتف غير صالح',
'Format email invalide': 'تنسيق بريد إلكتروني غير صالح',

'Généré': 'تم إنشاؤه',



  // Page Header
  'Demandes': 'الطلبات',
  'Gérez les demandes des chefs de projet': 'إدارة طلبات مديري المشاريع',
  'Répondez aux demandes de renouvellement': 'الرد على طلبات التجديد',
  
 
  // Messages
  'Chargement des demandes...': 'جاري تحميل الطلبات...',
  
 
  // Section Headers (ALL tab)
  'En attente ({{count}})': 'قيد الانتظار ({{count}})',
  'Approuvées ({{count}})': 'المقبولة ({{count}})',
  'Refusées ({{count}})': 'المرفوضة ({{count}})',
  
  // Request Type Labels
  'RENEWAL_ACCEPTANCE': 'قبول التجديد',
  'REASSIGNMENT_SUGGESTION': 'اقتراح إعادة تعيين',
  'REASSIGNMENT_REQUEST_FROM_CHEF': 'طلب إعادة تعيين',
  
  // Card Labels

  
  
  // Status Colors (implied labels)
  'statut en attente': 'قيد الانتظار',
  'statut approuvé': 'مقبول',
  'statut refusé': 'مرفوض',
  

// Applications Section
'Gestion des Applications': 'إدارة التطبيقات',
'Mes Applications': 'تطبيقاتي',
'Créez et gérez toutes les applications': 'إنشاء وإدارة جميع التطبيقات',
'Gérez vos applications assignées': 'إدارة تطبيقاتك المعينة',
'Voir les archives': 'عرض الأرشيف',
'Total': 'الإجمالي',
'Planifiées': 'مخطط لها',
'Terminées': 'مكتملة',
'Date Début': 'تاريخ البداية',
'Date Fin': 'تاريخ النهاية',
'Chef': 'المدير',
'conv.': 'اتفاقية',
'Tous statuts': 'جميع الحالات',
'Tous clients': 'جميع العملاء',
'Nouvelle': 'جديد',

'Aucune application trouvée': 'لم يتم العثور على تطبيقات',

// Archive Section
'Applications Archivées': 'التطبيقات المؤرشفة',
'Mes Applications Archivées': 'تطبيقاتي المؤرشفة',
'application(s) dans les archives': 'تطبيق(ات) في الأرشيف',
'Aucune application archivée': 'لا توجد تطبيقات مؤرشفة',
'Les applications apparaîtront ici après archivage': 'ستظهر التطبيقات هنا بعد الأرشفة',
'Client:': 'العميل:',
'Chef:': 'المدير:',
'Conventions:': 'الاتفاقيات:',
'Période:': 'الفترة:',
'Préc': 'السابق',
'Suiv': 'التالي',

      'CRITIQUE': 'حرج',
'MOYENNE': 'متوسط',
'FAIBLE': 'منخفض',
      'Nombre de Structures': 'عدد الهياكل',
      'Décideur': 'صانع القرار',
'Commercial': 'مندوب مبيعات',
      'Bienvenue Administrateur': 'مرحباً أيها المدير',
'Chargement des données...': 'جاري تحميل البيانات...',
'Progression Moyenne': 'متوسط التقدم',
'En Bonne Voie': 'على المسار الصحيح',
'Total Applications': 'إجمالي الطلبات',
'Structures Responsable': 'الهياكل المسؤولة',
'Structures Bénéficiaires': 'الهياكل المستفيدة',
'Zones TN': 'المناطق التونسية',
'Zones Personalisés': 'المناطق المخصصة',

'Charge de Travail': 'عبء العمل',
'Total Chefs': 'إجمالي مديري المشاريع',
'Critique': 'حرج',
'Moyenne': 'متوسط',
'Disponible': 'متاح',

'Charge': 'عبء العمل',

'Aucune donnée de charge disponible': 'لا توجد بيانات عبء عمل متاحة',
      // Admin
      'Tableau de bord': 'لوحة القيادة',
      'Gestion des utilisateurs': 'إدارة المستخدمين',
      'Nomenclatures': 'المسميات',
      'Applications': 'التطبيقات',
      
     
      'Archives': 'الأرشيف',
      'Calendrier': 'التقويم',
      
      // User
      'Profil': 'الملف الشخصي',
      'Boite Mail': 'صندوق البريد',
      

    
      'Sauvegarder': 'حفظ',
      'Charger plus': 'تحميل المزيد',
    'Gérer vos paramètres de compte': 'إدارة إعدادات حسابك',
    'Sans département': 'بدون قسم',
    'Membre depuis': 'عضو منذ',
    'Dernière connexion': 'آخر تسجيل دخول',
    'Échecs de connexion': 'محاولات فاشلة',
    'Informations personnelles': 'معلومات شخصية',
    'Détails personnels de base': 'التفاصيل الشخصية الأساسية',
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

    'Il y a 6 jours': 'منذ 6 أيام',
'Il y a {{days}} jours': 'منذ {{days}} أيام',
'Il y a 1 jour': 'منذ يوم واحد',
'Il y a {{hours}} heures': 'منذ {{hours}} ساعات',
'Il y a 1 heure': 'منذ ساعة واحدة',
'Il y a {{minutes}} minutes': 'منذ {{minutes}} دقائق',
'Il y a 1 minute': 'منذ دقيقة واحدة',
'À l\'instant': 'الآن',
'Hier': 'أمس',
'Aujourd\'hui': 'اليوم',
'Cette semaine': 'هذا الأسبوع',
'Le mois dernier': 'الشهر الماضي',
'L\'année dernière': 'العام الماضي',
    
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
    '%': '%',
    
    'Détails des Factures': 'تفاصيل الفواتير',
    'Répartition par statut de paiement': 'التوزيع حسب حالة الدفع',
    
    // Overdue Invoices Table
    'Nécessitent un suivi immédiat': 'تتطلب متابعة فورية',
    'Numéro': 'الرقم',
    'Retard': 'التأخير',
    'Priorité': 'الأولوية',
    
    // Filters
    'Priorité:': 'الأولوية:',
    'Élevée': 'عالية',
    'Moyen': 'متوسطة',
    'Trier par:': 'ترتيب حسب:',
    'Jours retard': 'أيام التأخير',
    'Date échéance': 'تاريخ الاستحقاق',
    'Afficher:': 'عرض:',
    'Réinitialiser': 'إعادة تعيين',
    
  
    'Aucune facture trouvée avec ces filtres': 'لم يتم العثور على فواتير بهذه الفلاتر',
    
    // Status Labels
    'Acceptée': 'مقبولة',
    'Payé': 'مدفوع',
    'Non payé': 'غير مدفوع',
    
    'IMPAYE': 'متأخر',
    'ATTENTE': 'قيد الانتظار',
 
    'Overloaded': 'مثقل',
'High': 'مرتفع',
'Normal': 'عادي',
'Low': 'منخفض',
'workload.status.critical': 'حرج',
'workload.status.high': 'مرتفع',
'workload.status.medium': 'متوسط',
'workload.status.low': 'منخفض',
'workload.status.available': 'متاح',


'Bienvenu': 'مرحباً',
 

  
  // Insights Cards
  'Alertes': 'تنبيهات',
  'Urgent': 'عاجل',
  'factures en retard': 'فواتير متأخرة',
  'Meilleur': 'الأفضل',
  'performance': 'أداء',
  'Performance': 'الأداء',
  'nouvelles conv.': 'اتفاقيات جديدة',
  
  // Chart Section
  'Statistique': 'إحصائيات',
  'Revenus': 'الإيرادات',
  
  // Overdue Invoices Table
  'Suivi prioritaire': 'متابعة أولوية',
  'N°': 'رقم',
 
  'Jours': 'أيام',
  'Aucune facture': 'لا توجد فواتير',
  

  
  // Priority Labels (by days overdue)
  'Priorité Critique': 'أولوية حرجة',
  'Priorité Élevée': 'أولوية مرتفعة',
  'Priorité Moyenne': 'أولوية متوسطة',
  'Priorité Faible': 'أولوية منخفضة',
  
 

  'Gestion des Conventions': 'إدارة الاتفاقيات',
  'Gérez vos conventions commerciales': 'إدارة اتفاقياتك التجارية',
  'Nouvelle Convention': 'اتفاقية جديدة',
  
  // Messages
  'Voir les {{count}} facture(s) associée(s)': 'عرض {{count}} فاتورة مرتبطة',
  'Résultat(s)': 'نتيجة(نتائج)',
  
  // Filters & Search
  'Effacer la recherche': 'مسح البحث',
  
  // Folder Tabs
  'Tous': 'الكل',
 
  'Resp:': 'مسؤول:',
  'Benef:': 'مستفيد:',
 
  'Dates': 'التواريخ',
  'Déb:': 'بداية:',
  'Fin:': 'نهاية:',
  'Périodicité': 'الدورية',
  'ERP:': 'ERP:',
  
  
  'Aucune convention trouvée': 'لم يتم العثور على اتفاقيات',
  'Commencez par créer votre première convention': 'ابدأ بإنشاء اتفاقيتك الأولى',
  'Créer une convention': 'إنشاء اتفاقية',
 

  'Dossiers classés': 'الملفات المصنفة',
  'élément(s)': 'عنصر(عناصر)',
  
  // Loading
  'Ouverture des dossiers...': 'جاري فتح الملفات...',
  
  // Drawer Labels
  'Dossier #': 'ملف رقم #',
  'Période': 'الفترة',
  'Archivé:': 'مؤرشف:',
  'Aucune raison spécifiée': 'لم يتم تحديد سبب',
  
  // Buttons
  'Restaurer': 'استعادة',
 
  
  // Restore Modal
  'Restaurer la convention': 'استعادة الاتفاقية',
  'Cette convention sera déplacée des archives vers la liste active.': 'سيتم نقل هذه الاتفاقية من الأرشيف إلى القائمة النشطة.',
  'Confirmer': 'تأكيد',
  
  // Empty State
  'Le classeur est vide': 'الملف فارغ',
  'Aucune convention archivée pour le moment. Les dossiers apparaîtront ici.': 'لا توجد اتفاقيات مؤرشفة حالياً. ستظهر الملفات هنا.',
  
  // Pagination
  'Page {{current}} sur {{total}}': 'صفحة {{current}} من {{total}}',



  'Archiver': 'أرشفة',
  'Renouveler': 'تجديد',
  
  // Convention Info Card
  'Référence ERP': 'مرجع ERP',
  'Application associée': 'التطبيق المرتبط',
  'Structure Bénéficiel': 'الهيكل المستفيد',

  
  // Invoices Section
  'Factures associées': 'الفواتير المرتبطة',
  'facture(s)': 'فاتورة(فواتير)',
  'Aucune facture pour cette convention': 'لا توجد فواتير لهذه الاتفاقية',
  '+ créer une facture': '+ إنشاء فاتورة',
  'Facturation:': 'الفوترة:',
  'Échéance:': 'تاريخ الاستحقاق:',
  'Montant TTC:': 'المبلغ شامل الضريبة:',
  'voir tout ({{count}})': 'عرض الكل ({{count}})',
  'Enregistrer paiement': 'تسجيل الدفع',
  'Voir détails': 'عرض التفاصيل',
  
  // History Section
  'Historique des modifications': 'سجل التعديلات',
  'Aucun historique pour cette convention': 'لا يوجد سجل لهذه الاتفاقية',
  
  // Versions Link
  'Historique des versions': 'سجل الإصدارات',
  'Voir toutes les versions renouvelées': 'عرض جميع الإصدارات المجددة',
  
  // Financial Stats Card
  'Statistiques financières': 'الإحصائيات المالية',
  'Total factures': 'إجمالي الفواتير',
  'Factures payées': 'الفواتير المدفوعة',
  'Factures non payées': 'الفواتير غير المدفوعة',
  'Factures en retard': 'الفواتير المتأخرة',
  'Total payé': 'إجمالي المدفوع',
  'Total impayé': 'إجمالي غير المدفوع',
  
  // Financial Details Card
  'Détails financiers': 'التفاصيل المالية',
  'Montant HT': 'المبلغ غير شامل الضريبة',
  'TVA': 'ضريبة القيمة المضافة',
  'Montant TTC': 'المبلغ شامل الضريبة',
  "Nombre d'utilisateurs": 'عدد المستخدمين',
  
  // Dates Info Card
  'Informations temporelles': 'المعلومات الزمنية',
  'Date de fin': 'تاريخ النهاية',
  'Date de signature': 'تاريخ التوقيع',
  'Jours écoulés': 'الأيام المنقضية',
  'aujourd\'hui': 'اليوم',
  'terminée': 'مكتمل',

  'archivé le': 'تمت الأرشفة في',
  'raison': 'السبب',
  
  'Référence paiement': 'مرجع الدفع',
  'Date paiement': 'تاريخ الدفع',
  'ex: VIREMENT-001': 'مثال: تحويل-001',
  'Confirmer le paiement': 'تأكيد الدفع',
  
  // Archive Modal
  'Archiver la convention': 'أرشفة الاتفاقية',
  "Raison de l'archivage": 'سبب الأرشفة',
  'Pourquoi archivez-vous cette convention ?': 'لماذا تقوم بأرشفة هذه الاتفاقية؟',
  "Confirmer l'archivage": 'تأكيد الأرشفة',
  'Archivage...': 'جاري الأرشفة...',
  
  // Restore Modal
  'Restaurer la Convention': 'استعادة الاتفاقية',
  'Êtes-vous sûr de vouloir restaurer cette convention ?': 'هل أنت متأكد من رغبتك في استعادة هذه الاتفاقية؟',
  
  // Renew Modal
  'Renouveler la convention': 'تجديد الاتفاقية',
  'La référence, l\'application et la structure bénéficiel restent identiques': 'المرجع والتطبيق والهيكل المستفيد تبقى كما هي',
  'Vous pouvez modifier tous les autres champs selon vos besoins.': 'يمكنك تعديل جميع الحقول الأخرى حسب حاجتك.',
  '-- Sélectionner --': '-- اختر --',
  'Sans zone': 'بدون منطقة',
  'Ancienne:': 'السابقة:',
  'Libellé': 'التسمية',
  
  'Date Signature': 'تاريخ التوقيع',
  'Montant HT (TND)': 'المبلغ غير شامل الضريبة (د.ت)',
  'TVA (%)': 'ضريبة القيمة المضافة (%)',
  'Montant TTC (TND)': 'المبلغ شامل الضريبة (د.ت)',
  'Saisir le nombre': 'أدخل العدد',
  'Mensuel': 'شهري',
  'Trimestriel': 'ربع سنوي',
  'Semestriel': 'نصف سنوي',
  'Annuel': 'سنوي',
  'Confirmer le renouvellement': 'تأكيد التجديد',
  
  // Loading states
  '✨ chargement...': '✨ جاري التحميل...',
  'chargement...': 'جاري التحميل...',


'Cartes': 'بطاقات',
'Liste': 'قائمة',
'Montant Total': 'المبلغ الإجمالي',
'Chargement des factures...': 'جاري تحميل الفواتير...',
'Les factures apparaîtront ici': 'ستظهر الفواتير هنا',
'Facturation': 'الفوترة',
'Échéance': 'تاريخ الاستحقاق',
'TTC': 'شامل الضريبة',
'Payer': 'دفع',
'Détails': 'تفاصيل',
'N° Facture': 'رقم الفاتورة',
'Date facturation': 'تاريخ الفوترة',
'HT:': 'بدون ضريبة:',
'Référence de paiement *': 'مرجع الدفع *',
'Date de paiement': 'تاريخ الدفع',
  
'Historique de convention': 'سجل الاتفاقية',
'entrée': 'مدخل',
's': 'ات',
'chargement des données': 'جاري تحميل البيانات',
'aucun historique disponible': 'لا يوجد سجل متاح',
'l\'historique apparaîtra ici quand des modifications seront faites': 'سيظهر السجل هنا عند إجراء التعديلات',
'Modifications': 'التعديلات',
'Référence': 'مرجع',
'Date début': 'تاريخ البداية',
'Date fin': 'تاريخ النهاية',
'Date signature': 'تاريخ التوقيع',
'Nombre utilisateurs': 'عدد المستخدمين',


'Modifier la Convention': 'تعديل الاتفاقية',
'Modifiez les informations': 'تعديل المعلومات',
'Créez une nouvelle convention': 'إنشاء اتفاقية جديدة',
'✨ Redirection...': '✨ جاري إعادة التوجيه...',
'Chargement de la convention...': 'جاري تحميل الاتفاقية...',
'Référence Convention': 'مرجع الاتفاقية',
'Choisir une application': 'اختر تطبيقاً',
'Toutes les applications ont déjà des conventions': 'جميع التطبيقات لديها اتفاقيات بالفعل',
'Structure Beneficiel': 'الهيكل المستفيد',
'Choisir une structure beneficiel': 'اختر هيكلاً مستفيداً',
'Choisir une structure responsable': 'اختر هيكلاً مسؤولاً',
'Modalités de Paiement': 'شروط الدفع',
'Sélectionner': 'اختر',
'Libellé de la convention': 'تسمية الاتفاقية',
'Auto': 'تلقائي',
'Sélectionner une date': 'اختر تاريخاً',
'15 jours min. après début': '15 يوماً كحد أدنى بعد البداية',
'À partir d\'aujourd\'hui': 'من اليوم',
'Min:': 'الحد الأدنى:',
'Max:': 'الحد الأقصى:',
'Règle:': 'القاعدة:',
'Mise à jour automatique': 'تحديث تلقائي',
'Les dates de': 'تواريخ',
'seront synchronisées': 'سيتم مزامنتها',

'utilisateurs': 'مستخدمين',
'Failed to load applications': 'فشل تحميل التطبيقات',
'Erreur lors du chargement de la convention': 'خطأ في تحميل الاتفاقية',
'Erreur lors de la détermination du nombre d\'utilisateurs': 'خطأ في تحديد عدد المستخدمين',
'Veuillez d\'abord sélectionner une application': 'يرجى اختيار تطبيق أولاً',
'Le format de référence doit être: CONV-YYYY-XXX (ex: CONV-2024-001)': 'يجب أن يكون تنسيق المرجع: CONV-YYYY-XXX (مثال: CONV-2024-001)',
'Structure interne est requise': 'الهيكل الداخلي مطلوب',
'Structure beneficiel est requise': 'الهيكل المستفيد مطلوب',
'Périodicité est requise': 'الدورية مطلوبة',
'Le montant HT est requis et doit être supérieur à 0': 'المبلغ بدون ضريبة مطلوب ويجب أن يكون أكبر من 0',
'Le nombre d\'utilisateurs est requis et doit être supérieur à 0': 'عدد المستخدمين مطلوب ويجب أن يكون أكبر من 0',
'Convention mise à jour avec succès': 'تم تحديث الاتفاقية بنجاح',
'Convention créée avec succès': 'تم إنشاء الاتفاقية بنجاح',
'Échec de la mise à jour de la convention': 'فشل تحديث الاتفاقية',
'Échec de la création de la convention': 'فشل إنشاء الاتفاقية',

'Versions de convention': 'إصدارات الاتفاقية',
'Aucune convention sélectionnée': 'لم يتم اختيار اتفاقية',
'Voir en mode split': 'عرض بتقسيم الشاشة',
'Voir en mode grille': 'عرض شبكي',
'Total versions': 'إجمالي الإصدارات',
'Première version': 'الإصدار الأول',
'Dernière archivée': 'آخر إصدار مؤرشف',
'Version actuelle': 'الإصدار الحالي',
'Chargement des versions...': 'جاري تحميل الإصدارات...',
'Réessayer': 'إعادة المحاولة',
'Versions archivées': 'الإصدارات المؤرشفة',
'Anciennes versions': 'الإصدارات القديمة',
'Archivée le': 'تمت الأرشفة في',
'Raison:': 'السبب:',
'Informations générales': 'معلومات عامة',
'Signature:': 'التوقيع:',
'Code:': 'الرمز:',
'Factures archivées': 'الفواتير المؤرشفة',
'voir tout': 'عرض الكل',
'Sélectionnez une version': 'اختر إصداراً',
'Cliquez sur une version dans la liste de gauche': 'انقر على إصدار من القائمة اليسرى',
'Aucune version archivée': 'لا توجد إصدارات مؤرشفة',
'Cette convention n\'a pas encore été renouvelée.': 'لم يتم تجديد هذه الاتفاقية بعد.',
'Retour à la convention': 'العودة إلى الاتفاقية',
'Numéro facture': 'رقم الفاتورة',
'Payée le': 'تم الدفع في',
'Réf:': 'مرجع:',
'Total :': 'المجموع:',
'Voir tous les détails →': 'عرض جميع التفاصيل →',

'Charge faible': 'عبء منخفض',
'Charge moyenne': 'عبء متوسط',
'Charge critique': 'عبء حرجة',
'Note:': 'ملاحظة:',
'Vous êtes le créateur de cette application. Si vous ne pouvez pas continuer à travailler dessus, vous pouvez demander une réassignation.': 'أنت منشئ هذا التطبيق. إذا لم تتمكن من الاستمرار في العمل عليه， يمكنك طلب إعادة التعيين.',
'Raison de la demande': 'سبب الطلب',
'Expliquez pourquoi vous ne pouvez pas continuer à travailler sur cette application...': 'اشرح سبب عدم قدرتك على الاستمرار في العمل على هذا التطبيق...',
'Recommander un chef de projet': 'توصية بمدير مشروع',
'Chargement des chefs...': 'جاري تحميل المديرين...',
'Aucun autre chef de projet disponible': 'لا يوجد مديرو مشاريع آخرون متاحون',
'Recommandations supplémentaires': 'توصيات إضافية',
'optionnel': 'اختياري',
'Informations supplémentaires pour l\'administrateur...': 'معلومات إضافية للمسؤول...',
'Envoyer la demande': 'إرسال الطلب',
'Élevé': 'مرتفع',
'Faible': 'منخفض',


'Gestion des factures': 'إدارة الفواتير',
'Suivez et gérez vos factures': 'تتبع وإدارة فواتيرك',
'Non Payées': 'غير مدفوعة',
'Toutes apps': 'جميع التطبيقات',
'filtres:': 'مرشحات:',
'tout': 'مسح الكل',
'Délai': 'التأخير',
'Envoyer relance': 'إرسال تذكير',
'Envoyer par email': 'إرسال بالبريد الإلكتروني',
'Nouveau paiement': 'دفعة جديدة',
'Valider le paiement': 'تأكيد الدفع',
'Non Payé': 'غير مدفوعة',
  
'FACTURE': 'فاتورة',
'ÉMETTEUR': 'المُصدر',
'CLIENT': 'العميل',
'Création:': 'الإنشاء:',
'DÉTAILS': 'التفاصيل',
'Prestation': 'الخدمة',
'Sous-total HT': 'المجموع الفرعي بدون ضريبة',
'TOTAL TTC': 'المجموع شامل الضريبة',
'Paiement reçu': 'تم استلام الدفع',
'avant': 'قبل',
'de retard': 'متأخر',
'à temps': 'في الوقت المحدد',
'payée': 'مدفوعة',
'Échéance aujourd\'hui': 'مستحق اليوم',
'Générée automatiquement • Merci de votre confiance': 'تم إنشاؤها تلقائياً • شكراً لثقتكم',
'Valider': 'تأكيد',
'Nouveau message': 'رسالة جديدة',
'Menu': 'القائمة',
'Boîte de réception': 'صندوق الوارد',
'Messages importants': 'رسائل مهمة',
'Brouillons': 'المسودات',
'Messages envoyés': 'الرسائل المرسلة',
'Corbeille': 'سلة المحذوفات',
'Groupes': 'المجموعات',
'Aucun groupe': 'لا توجد مجموعات',
'Gérer les groupes': 'إدارة المجموعات',
'Archive': 'الأرشيف',
'Brouillon': 'مسودة',
'Mode édition:': 'وضع التحرير:',
'Vous modifiez un brouillon': 'أنت تعدل مسودة',
'À': 'إلى',
'Masquer CC': 'إخفاء CC',
'Ajouter CC': 'إضافة CC',
'Masquer BCC': 'إخفاء BCC',
'Ajouter BCC': 'إضافة BCC',
'CC': 'CC',
'BCC': 'BCC',
'Objet': 'الموضوع',
'Objet du message': 'موضوع الرسالة',
'Écrivez votre message ici...': 'اكتب رسالتك هنا...',
'Joindre des fichiers': 'إرفاق ملفات',
'Max 10MB par fichier': 'الحد الأقصى 10 ميجابايت لكل ملف',
'Envoi...': 'جاري الإرسال...',
'Envoyer': 'إرسال',
'Aucun message': 'لا توجد رسائل',
'(Sans objet)': '(بدون موضوع)',
'membres': 'عضواً',
'Pièces jointes': 'المرفقات',
'Répondre': 'رد',
'Chargement du PDF...': 'جاري تحميل PDF...',
'Aperçu PDF non disponible': 'معاينة PDF غير متاحة',
'Télécharger le PDF': 'تحميل PDF',
'Aperçu non disponible pour ce type de fichier': 'المعاينة غير متاحة لهذا النوع من الملفات',
'Télécharger': 'تحميل',
'À:': 'إلى:',
'Tout sélectionner': 'تحديد الكل',
'Désélectionner tout': 'إلغاء تحديد الكل',
'Marquer comme lu': 'تعيين كمقروء',
'Marquer comme non lu': 'تعيين كغير مقروء',
'Déplacer vers': 'نقل إلى',
'Supprimer définitivement': 'حذف نهائي',
'Paramètres': 'الإعدادات',
'Aide': 'مساعدة',
'Notifications': 'الإشعارات',
'Thème sombre': 'الوضع الداكن',
'Thème clair': 'الوضع الفاتح',
'Langue': 'اللغة',
'Français': 'الفرنسية',
'Anglais': 'الإنجليزية',
'Arabe': 'العربية',   
'Consultez et gérez toutes vos notifications': 'عرض وإدارة جميع إشعاراتك',
'Listes': 'قوائم',
'Tout marquer comme lu': 'تحديد الكل كمقروء',
'Chargement des notifications...': 'جاري تحميل الإشعارات...',
'Aucune notification': 'لا توجد إشعارات',
'Vous n\'avez pas encore de notifications.': 'ليس لديك أي إشعارات بعد.',
'Non lu': 'غير مقروء',
'Vous avez atteint la fin de vos notifications': 'لقد وصلت إلى نهاية إشعاراتك',
'Créé le': 'تم الإنشاء في',
'Notifications envoyées': 'الإشعارات المرسلة',


'Facture à échéance dans 2 jours': 'الفاتورة مستحقة خلال يومين',
'Facture à échéance dans 3 jours': 'الفاتورة مستحقة خلال 3 أيام',
'Facture à échéance dans 4 jours': 'الفاتورة مستحقة خلال 4 أيام',
'Facture à échéance dans 5 jours': 'الفاتورة مستحقة خلال 5 أيام',
'Facture à échéance aujourd\'hui': 'الفاتورة مستحقة اليوم',
'Facture à échéance demain': 'الفاتورة مستحقة غداً',
'Facture en retard': 'الفاتورة متأخرة',
'jours en retard': 'أيام متأخرة',
'Demain': 'غداً',
'Dans {{days}} jours': 'خلال {{days}} أيام',
'{{days}} jours en retard': 'متأخر {{days}} أيام',
'Échec du chargement des notifications': 'فشل تحميل الإشعارات',
'Échec du marquage de la notification comme lue': 'فشل تعيين الإشعار كمقروء',
'Échec du marquage de toutes les notifications comme lues': 'فشل تعيين جميع الإشعارات كمقروءة',
'Échec de la suppression de la notification': 'فشل حذف الإشعار',

'Dans X jours': 'خلال X أيام',
'X jours en retard': 'متأخر X أيام',
'Il y a X minutes': 'منذ X دقائق',
'Il y a X heures': 'منذ X ساعات',
'Il y a X jours': 'منذ X أيام',
'Désactiver': 'تعطيل',
'Configurer la 2FA': 'إعداد المصادقة الثنائية',
'Scannez le QR code avec votre application': 'امسح رمز QR ضوئياً باستخدام تطبيقك',
'Entrez le code à 6 chiffres': 'أدخل الرمز المكون من 6 أرقام',
'Vérification...': 'جاري التحقق...',
'Vérifier': 'تحقق',
'Sauvegardez vos codes de secours !': 'احفظ رموز الاسترداد الخاصة بك!',
'Ces codes vous permettent d\'accéder à votre compte si vous perdez votre téléphone.': 'تتيح لك هذه الرموز الوصول إلى حسابك إذا فقدت هاتفك.',
'Copier': 'نسخ',
'Désactiver la 2FA': 'تعطيل المصادقة الثنائية',
'Entrez votre code pour désactiver': 'أدخل رمزك لتعطيل',
'Code de vérification': 'رمز التحقق',
'Code requis': 'الرمز مطلوب',
'Doit contenir 6 chiffres': 'يجب أن يحتوي على 6 أرقام',
'Désactivation...': 'جاري التعطيل...',
'Commercial Metier' : 'العمل التجاري',


// Arabic translations for Calendar Component
'Loading ...': 'جاري التحميل ...',
'Calendrier des factures': 'تقويم الفواتير',
'Suivre les dates et paiements': 'تتبع التواريخ والمدفوعات',
'Prochain': 'القادمة',
'Cette Semaine': 'هذا الأسبوع',
'Ce mois': 'هذا الشهر',
'Impayé': 'غير مدفوعة',
'Masquer': 'إخفاء',
'Afficher': 'إظهار',
'Tout est en ordre !': 'كل شيء في秩序!',
'Actions rapides': 'إجراءات سريعة',
'Détails facture': 'تفاصيل الفاتورة',
'Notes': 'ملاحظات',
'Voir facture': 'عرض الفاتورة',
'Mois': 'شهر',
'Semaine': 'أسبوع',
'Jour': 'يوم',
'Toute la journée': 'طوال اليوم',
'Inconnu': 'غير معروف',
'Erreur lors du chargement des événements du calendrier': 'خطأ في تحميل أحداث التقويم',
'Erreur lors du chargement des statistiques du calendrier': 'خطأ في تحميل إحصائيات التقويم',
'Erreur lors du chargement des factures à venir': 'خطأ في تحميل الفواتير القادمة',
'Erreur lors du chargement des factures en retard': 'خطأ في تحميل الفواتير المتأخرة',





}
  };

  constructor(rendererFactory: RendererFactory2) {
    this.renderer = rendererFactory.createRenderer(null, null);
    this.loadInitialLanguage();
  }

  private loadInitialLanguage(): void {
    const savedLang = localStorage.getItem('appLanguage');
    if (savedLang && this.translations[savedLang]) {
      this.currentLang = savedLang;
    } else {
      this.currentLang = 'fr';
    }
    this.currentLangSubject.next(this.currentLang);
    this.setHtmlDirection();
  }

  setLanguage(langCode: string): void {
    if (this.translations[langCode]) {
      this.currentLang = langCode;
      localStorage.setItem('appLanguage', langCode);
      this.currentLangSubject.next(langCode);
      this.setHtmlDirection();
    }
  }

  getCurrentLanguage(): string {
    return this.currentLang;
  }

  isRtl(): boolean {
    return this.currentLang === 'ar';
  }

  getLanguageChangeObservable(): Observable<string> {
    return this.currentLang$;
  }

  private setHtmlDirection(): void {
    const isRtl = this.currentLang === 'ar';
    const htmlElement = document.documentElement;
    
    if (isRtl) {
      this.renderer.setAttribute(htmlElement, 'dir', 'rtl');
      this.renderer.setAttribute(htmlElement, 'lang', 'ar');
      this.renderer.addClass(htmlElement, 'rtl');
      this.renderer.removeClass(htmlElement, 'ltr');
    } else {
      this.renderer.setAttribute(htmlElement, 'dir', 'ltr');
      this.renderer.setAttribute(htmlElement, 'lang', this.currentLang);
      this.renderer.addClass(htmlElement, 'ltr');
      this.renderer.removeClass(htmlElement, 'rtl');
    }
  }

  translate(key: string): string {
    return this.translations[this.currentLang]?.[key] || key;
  }

  getLanguages(): any[] {
    return [
      { code: 'fr', name: 'Français', flag: '🇫🇷' },
      { code: 'en', name: 'English', flag: '🇬🇧' },
      { code: 'ar', name: 'العربية', flag: '🇸🇦' }
    ];
  }
}