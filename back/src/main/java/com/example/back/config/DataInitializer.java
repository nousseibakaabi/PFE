package com.example.back.config;


import com.example.back.entity.ERole;
import com.example.back.entity.Role;
import com.example.back.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        if (roleRepository.count() == 0) {
            roleRepository.save(new Role(ERole.ROLE_ADMIN, "Administrator with full access"));
            roleRepository.save(new Role(ERole.ROLE_COMMERCIAL_METIER, "Commercial Métier role"));
            roleRepository.save(new Role(ERole.ROLE_DECIDEUR, "Décideur role"));
            roleRepository.save(new Role(ERole.ROLE_CHEF_PROJET, "Chef de Projet role"));

            System.out.println("Roles initialized successfully!");
        }


    }
}