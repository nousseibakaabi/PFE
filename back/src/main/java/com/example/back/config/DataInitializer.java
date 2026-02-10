package com.example.back.config;

import com.example.back.entity.*;
import com.example.back.repository.RoleRepository;
import com.example.back.repository.ZoneGeographiqueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ZoneGeographiqueRepository zoneGeographiqueRepository;

    @Override
    public void run(String... args) throws Exception {
        initializeRoles();
        initializeTunisianZones();
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

    private String getZoneNameFromCode(String code) {
        // Map codes to names
        Map<String, String> codeToName = Map.ofEntries(
                Map.entry("TN-11", "Tunis"),
                Map.entry("TN-12", "Ariana"),
                Map.entry("TN-13", "Ben Arous"),
                Map.entry("TN-14", "Manouba"),
                Map.entry("TN-21", "Nabeul"),
                Map.entry("TN-22", "Zaghouan"),
                Map.entry("TN-23", "Bizerte"),
                Map.entry("TN-31", "Béja"),
                Map.entry("TN-32", "Jendouba"),
                Map.entry("TN-33", "Le Kef"),
                Map.entry("TN-34", "Siliana"),
                Map.entry("TN-41", "Kairouan"),
                Map.entry("TN-42", "Kasserine"),
                Map.entry("TN-43", "Sidi Bouzid"),
                Map.entry("TN-51", "Sousse"),
                Map.entry("TN-52", "Monastir"),
                Map.entry("TN-53", "Mahdia"),
                Map.entry("TN-61", "Sfax"),
                Map.entry("TN-71", "Gafsa"),
                Map.entry("TN-72", "Tozeur"),
                Map.entry("TN-73", "Kebili"),
                Map.entry("TN-81", "Gabès"),
                Map.entry("TN-82", "Medenine"),
                Map.entry("TN-83", "Tataouine")
        );
        return codeToName.getOrDefault(code, "Unknown Governorate");
    }

    private String getArabicNameFromCode(String code) {
        // Map codes to Arabic names
        Map<String, String> codeToArabicName = Map.ofEntries(
                Map.entry("TN-11", "تونس"),
                Map.entry("TN-12", "أريانة"),
                Map.entry("TN-13", "بن عروس"),
                Map.entry("TN-14", "منوبة"),
                Map.entry("TN-21", "نابل"),
                Map.entry("TN-22", "زغوان"),
                Map.entry("TN-23", "بنزرت"),
                Map.entry("TN-31", "باجة"),
                Map.entry("TN-32", "جندوبة"),
                Map.entry("TN-33", "الكاف"),
                Map.entry("TN-34", "سليانة"),
                Map.entry("TN-41", "القيروان"),
                Map.entry("TN-42", "القصرين"),
                Map.entry("TN-43", "سيدي بوزيد"),
                Map.entry("TN-51", "سوسة"),
                Map.entry("TN-52", "المنستير"),
                Map.entry("TN-53", "المهدية"),
                Map.entry("TN-61", "صفاقس"),
                Map.entry("TN-71", "قفصة"),
                Map.entry("TN-72", "توزر"),
                Map.entry("TN-73", "ڨبلي"),
                Map.entry("TN-81", "ڨابس"),
                Map.entry("TN-82", "مدنين"),
                Map.entry("TN-83", "تطاوين")
        );
        return codeToArabicName.getOrDefault(code, "");
    }
}