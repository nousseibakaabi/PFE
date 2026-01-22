import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http'; // Add HttpParams import
import { BehaviorSubject, Observable, of } from 'rxjs';
import { map, catchError, timeout } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class TranslationService {
  private currentLang = new BehaviorSubject<string>('en');
  
  // Primary: MyMemory Translation API (supports Arabic)
  private myMemoryUrl = 'https://api.mymemory.translated.net/get';
  
  // Fallback: Local LibreTranslate
  private libreTranslateUrl = 'http://localhost:5000/translate';
  
  // Cache for translations
  private cache = new Map<string, Observable<string>>();
  
  languages = [
    { code: 'en', name: 'English', flag: '🇬🇧' },
    { code: 'fr', name: 'Français', flag: '🇫🇷' },
    { code: 'es', name: 'Español', flag: '🇪🇸' },
    { code: 'de', name: 'Deutsch', flag: '🇩🇪' },
    { code: 'ar', name: 'العربية', flag: '🇸🇦' }
  ];

  constructor(private http: HttpClient) {
    const savedLang = localStorage.getItem('appLanguage') || 'en';
    this.setLanguage(savedLang);
  }


   private arabicTranslations: {[key: string]: string} = {
    'Email': 'البريد الإلكتروني',
    'User': 'المستخدم',
    'Username': 'اسم المستخدم',
    'Showing': 'عرض',
    'Search': 'بحث',
    'Lock': 'قفل',
    'Unlock': 'فتح القفل',
    'Edit Department': 'تعديل القسم',
    'Edit Role': 'تعديل الدور',
    'Status': 'الحالة',
    'Role': 'الدور',
    'Actions': 'الإجراءات',
    'Previous': 'السابق',
    'Next': 'التالي',
    'to': 'إلى',
    'of': 'من',
    'entries': 'إدخالات',
    'results': 'نتائج',
    'Dashboard': 'لوحة التحكم',
    'Admin': 'مدير',
    'Profile': 'الملف الشخصي',
    'Calendar': 'التقويم',
    'Forms': 'النماذج',
    'Tables': 'الجداول',
    'Pages': 'الصفحات',
    'Charts': 'الرسوم البيانية',
    'UI Elements': 'عناصر الواجهة',
    'Authentication': 'المصادقة',
    'Admin Dashboard': 'لوحة تحكم المدير',
    'Manage users and monitor system metrics': 'إدارة المستخدمين ومراقبة مقاييس النظام',
    'Total Users': 'إجمالي المستخدمين',
    'Locked Users': 'المستخدمون المقفلون',
    'Total Failed Attempts': 'إجمالي المحاولات الفاشلة',
    'Active Users': 'المستخدمون النشطون',
    'User Management': 'إدارة المستخدمين',
    'Filter by Role': 'تصفية حسب الدور',
    'Filter by Status': 'تصفية حسب الحالة',
    'All Roles': 'جميع الأدوار',
    'All Status': 'جميع الحالات',
    'Active': 'نشط',
    'Temporarily Locked': 'مقفل مؤقتاً',
    'Locked by Admin': 'مقفل من قبل المدير',
    'Clear Filters': 'مسح الفلاتر',
    'Close': 'إغلاق',
    'Department': 'القسم',
    'No users found': 'لم يتم العثور على مستخدمين',
    'No users have been created yet. Click "Add User" to create one.': 'لم يتم إنشاء مستخدمين بعد. انقر على "إضافة مستخدم" لإنشاء واحد.',
    'Access Denied': 'تم رفض الوصول',
    'You don\'t have permission to access the admin dashboard. Please contact an administrator if you believe this is an error.': 'ليس لديك إذن للوصول إلى لوحة تحكم المدير. يرجى الاتصال بالمسؤول إذا كنت تعتقد أن هذا خطأ.',
    'Return to Dashboard': 'العودة إلى لوحة التحكم',
      'Commercial Métier': 'التجاري المتخصص',
  'Décideur': 'صانع القرار',
  'Chef de Projet': 'رئيس المشروع',
 
  'failed attempts': 'محاولات فاشلة',
  
  // Other translations
  'Not assigned': 'غير مخصص',
  'No role': 'لا يوجد دور', 
   'Applications': 'التطبيقات',
      'Application': 'التطبيق',
      'Structure': 'الهيكل',
       "Current Password": "كلمة المرور الحالية",
  "New Password": "كلمة مرور جديدة",
  "Confirm New Password": "تأكيد كلمة المرور الجديدة",
  "Password Requirements": "متطلبات كلمة المرور",
  "At least 6 characters": "على الأقل 6 أحرف",
  "Should contain letters and numbers": "يجب أن تحتوي على أحرف وأرقام",
  "Avoid common passwords": "تجنب كلمات المرور الشائعة",
  'First Name': 'الاسم ',
  'Last Name': 'اسم العائلة',



  };

 

  translate(text: string): Observable<string> {
    if (!text || text.trim() === '') return of('');
    
    const lang = this.currentLang.value;
    const cacheKey = `${text}_${lang}`;
    
    // Check cache first
    if (this.cache.has(cacheKey)) {
      return this.cache.get(cacheKey)!;
    }
    
    // If already in target language, return as is
    if (lang === 'en') {
      const result = of(text);
      this.cache.set(cacheKey, result);
      return result;
    }

     if (lang === 'ar' && this.arabicTranslations[text]) {
      const result = of(this.arabicTranslations[text]);
      this.cache.set(cacheKey, result);
      return result;
    }
    
    // Use MyMemory API first (supports Arabic)
    const translation$ = this.translateWithMyMemory(text, lang).pipe(
      catchError(error => {
        console.warn('MyMemory translation failed, trying LibreTranslate...', error);
        // Fallback to LibreTranslate for supported languages
        return this.translateWithLibreTranslate(text, lang);
      }),
      catchError(error => {
        console.warn('All translation services failed, returning original text', error);
        return of(text);
      })
    );
    
    this.cache.set(cacheKey, translation$);
    return translation$;
  }

  private translateWithMyMemory(text: string, lang: string): Observable<string> {
    // IMPORTANT: Replace with your email for higher rate limits
    const params = new HttpParams()
      .set('q', text)
      .set('langpair', `en|${lang}`)
      .set('de', 'kaabinousseiba11@gmail.com'); // ← CHANGE THIS TO YOUR EMAIL
    
    return this.http.get<any>(this.myMemoryUrl, { params }).pipe(
      timeout(5000),
      map(response => {
        console.log('MyMemory response:', response);
        if (response && response.responseData && response.responseData.translatedText) {
          return response.responseData.translatedText;
        }
        throw new Error('Invalid MyMemory response');
      })
    );
  }

  private translateWithLibreTranslate(text: string, lang: string): Observable<string> {
    // Only try LibreTranslate for languages it supports
    const libreSupported = ['fr', 'es', 'de'].includes(lang);
    if (!libreSupported) {
      throw new Error(`Language ${lang} not supported by LibreTranslate`);
    }
    
    const body = {
      q: text,
      source: 'auto',
      target: lang,
      format: 'text',
      api_key: ''
    };
    
    return this.http.post<any>(this.libreTranslateUrl, body).pipe(
      timeout(5000),
      map(response => {
        if (response && response.translatedText) {
          return response.translatedText;
        }
        throw new Error('Invalid LibreTranslate response');
      })
    );
  }

  // Keep all other methods the same...
  setLanguage(lang: string): void {
    if (this.languages.some(l => l.code === lang)) {
      this.currentLang.next(lang);
      localStorage.setItem('appLanguage', lang);
      document.documentElement.lang = lang;
      document.documentElement.dir = lang === 'ar' ? 'rtl' : 'ltr'; 

      
      // Clear cache when language changes
      this.cache.clear();
    }
  }

  getCurrentLanguage(): string {
    return this.currentLang.value;
  }

  getLanguageName(code: string): string {
    const lang = this.languages.find(l => l.code === code);
    return lang ? lang.name : code;
  }

  getLanguageFlag(code: string): string {
    const lang = this.languages.find(l => l.code === code);
    return lang ? lang.flag : '🏳️';
  }

  // Test connection to both services
  testConnection(): Observable<boolean> {
    // Test MyMemory API first
    const params = new HttpParams()
      .set('q', 'test')
      .set('langpair', 'en|fr')
      .set('de', 'kaabinousseiba11@gmail.com'); // ← CHANGE THIS
    
    return this.http.get<any>(this.myMemoryUrl, { params }).pipe(
      timeout(3000),
      map(() => true),
      catchError(() => {
        // If MyMemory fails, test LibreTranslate
        return this.http.get('http://localhost:5000/languages').pipe(
          map(() => true),
          catchError(() => of(false)),
          timeout(3000)
        );
      })
    );
  }
}