package com.example.back.config;

import com.example.back.entity.*;
import com.example.back.repository.RoleRepository;
import com.example.back.repository.ZoneGeographiqueRepository;
import com.example.back.service.MailGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ZoneGeographiqueRepository zoneGeographiqueRepository;

    @Autowired
    private MailGroupService mailGroupService;

    @Override
    public void run(String... args) throws Exception {
        initializeRoles();
        initializeTunisianZones();
        initializeMailGroups();
    }

    private void initializeRoles() {
        if (roleRepository.count() == 0) {
            roleRepository.save(new Role(ERole.ROLE_ADMIN, "Administrator with full access"));
            roleRepository.save(new Role(ERole.ROLE_COMMERCIAL_METIER, "Commercial Métier role"));
            roleRepository.save(new Role(ERole.ROLE_DECIDEUR, "Décideur role"));
            roleRepository.save(new Role(ERole.ROLE_CHEF_PROJET, "Chef de Projet role"));

            System.out.println("Roles initialized successfully!");
        }
    }

    private void initializeTunisianZones() {
        if (zoneGeographiqueRepository.countByType(ZoneType.TUNISIAN_ZONE) == 0) {
            // Create all 24 Tunisian governorates
            for (String code : ZoneType.TUNISIAN_ZONE_CODES) {
                String name = getZoneNameFromCode(code);
                String arabicName = getArabicNameFromCode(code);

                ZoneGeographique zone = new ZoneGeographique();
                zone.setCode(code);
                zone.setName(name);
                zone.setType(ZoneType.TUNISIAN_ZONE);
                zone.setDescription("Governorate of " + name + " - " + arabicName);

                zoneGeographiqueRepository.save(zone);
            }
            System.out.println("24 Tunisian governorates created successfully!");
        }
    }

    private void initializeMailGroups() {
        System.out.println("Initializing default mail groups...");
        mailGroupService.initializeDefaultGroups();
        System.out.println("Default mail groups initialized");
    }

    private String getZoneNameFromCode(String code) {
        // Map codes to names
        java.util.Map<String, String> codeToName = java.util.Map.ofEntries(
                java.util.Map.entry("TN-11", "Tunis"),
                java.util.Map.entry("TN-12", "Ariana"),
                java.util.Map.entry("TN-13", "Ben Arous"),
                java.util.Map.entry("TN-14", "Manouba"),
                java.util.Map.entry("TN-21", "Nabeul"),
                java.util.Map.entry("TN-22", "Zaghouan"),
                java.util.Map.entry("TN-23", "Bizerte"),
                java.util.Map.entry("TN-31", "Béja"),
                java.util.Map.entry("TN-32", "Jendouba"),
                java.util.Map.entry("TN-33", "Le Kef"),
                java.util.Map.entry("TN-34", "Siliana"),
                java.util.Map.entry("TN-41", "Kairouan"),
                java.util.Map.entry("TN-42", "Kasserine"),
                java.util.Map.entry("TN-43", "Sidi Bouzid"),
                java.util.Map.entry("TN-51", "Sousse"),
                java.util.Map.entry("TN-52", "Monastir"),
                java.util.Map.entry("TN-53", "Mahdia"),
                java.util.Map.entry("TN-61", "Sfax"),
                java.util.Map.entry("TN-71", "Gafsa"),
                java.util.Map.entry("TN-72", "Tozeur"),
                java.util.Map.entry("TN-73", "Kebili"),
                java.util.Map.entry("TN-81", "Gabès"),
                java.util.Map.entry("TN-82", "Medenine"),
                java.util.Map.entry("TN-83", "Tataouine")
        );
        return codeToName.getOrDefault(code, "Unknown Governorate");
    }

    private String getArabicNameFromCode(String code) {
        // Map codes to Arabic names
        java.util.Map<String, String> codeToArabicName = java.util.Map.ofEntries(
                java.util.Map.entry("TN-11", "تونس"),
                java.util.Map.entry("TN-12", "أريانة"),
                java.util.Map.entry("TN-13", "بن عروس"),
                java.util.Map.entry("TN-14", "منوبة"),
                java.util.Map.entry("TN-21", "نابل"),
                java.util.Map.entry("TN-22", "زغوان"),
                java.util.Map.entry("TN-23", "بنزرت"),
                java.util.Map.entry("TN-31", "باجة"),
                java.util.Map.entry("TN-32", "جندوبة"),
                java.util.Map.entry("TN-33", "الكاف"),
                java.util.Map.entry("TN-34", "سليانة"),
                java.util.Map.entry("TN-41", "القيروان"),
                java.util.Map.entry("TN-42", "القصرين"),
                java.util.Map.entry("TN-43", "سيدي بوزيد"),
                java.util.Map.entry("TN-51", "سوسة"),
                java.util.Map.entry("TN-52", "المنستير"),
                java.util.Map.entry("TN-53", "المهدية"),
                java.util.Map.entry("TN-61", "صفاقس"),
                java.util.Map.entry("TN-71", "قفصة"),
                java.util.Map.entry("TN-72", "توزر"),
                java.util.Map.entry("TN-73", "ڨبلي"),
                java.util.Map.entry("TN-81", "ڨابس"),
                java.util.Map.entry("TN-82", "مدنين"),
                java.util.Map.entry("TN-83", "تطاوين")
        );
        return codeToArabicName.getOrDefault(code, "");
    }
}