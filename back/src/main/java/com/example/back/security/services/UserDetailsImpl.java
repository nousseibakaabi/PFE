package com.example.back.security.services;

import com.example.back.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class UserDetailsImpl implements UserDetails {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String email;

    @JsonIgnore
    private String password;

    private String firstName;
    private String lastName;
    private Collection<? extends GrantedAuthority> authorities;

    private Boolean accountNonLocked;
    private boolean enabled;
    private boolean accountNonExpired;
    private boolean credentialsNonExpired;

    private Boolean twoFactorEnabled;

    public UserDetailsImpl(Long id, String username, String email, String password,
                           String firstName, String lastName,
                           Collection<? extends GrantedAuthority> authorities,
                           boolean accountNonLocked,
                           boolean enabled,
                           boolean accountNonExpired,
                           boolean credentialsNonExpired, Boolean twoFactorEnabled) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.authorities = authorities;
        this.accountNonLocked = accountNonLocked;
        this.enabled = enabled;
        this.accountNonExpired = accountNonExpired;
        this.credentialsNonExpired = credentialsNonExpired;
        this.twoFactorEnabled=twoFactorEnabled;
    }


    public static UserDetailsImpl build(User user) {
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().toString()))
                .collect(Collectors.toList());

        // FIX: Handle all null values properly
        boolean isAccountNonLocked = true;

        // Check lockedByAdmin - handle null safely
        Boolean lockedByAdmin = user.getLockedByAdmin();
        if (lockedByAdmin != null && lockedByAdmin.booleanValue()) {
            isAccountNonLocked = false;
        }

        // Check temporary lock
        LocalDateTime accountLockedUntil = user.getAccountLockedUntil();
        if (accountLockedUntil != null && accountLockedUntil.isAfter(LocalDateTime.now())) {
            isAccountNonLocked = false;
        }

        // Get enabled value
        boolean isEnabled = user.getEnabled() == null || user.getEnabled().booleanValue();

        // Get account non expired
        boolean isAccountNonExpired = user.getAccountNonExpired() == null || user.getAccountNonExpired().booleanValue();

        // Get credentials non expired
        boolean isCredentialsNonExpired = user.getCredentialsNonExpired() == null || user.getCredentialsNonExpired().booleanValue();

        // Get two factor enabled
        Boolean isTwoFactorEnabled = user.getTwoFactorEnabled() != null && user.getTwoFactorEnabled();

        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getFirstName(),
                user.getLastName(),
                authorities,
                isAccountNonLocked,        // 8ème paramètre
                isEnabled,                 // 9ème paramètre - enabled
                isAccountNonExpired,       // 10ème paramètre
                isCredentialsNonExpired,   // 11ème paramètre
                isTwoFactorEnabled         // 12ème paramètre - twoFactorEnabled
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserDetailsImpl user = (UserDetailsImpl) o;
        return Objects.equals(id, user.id);
    }




    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }


    public Boolean getTwoFactorEnabled() {
        return twoFactorEnabled;
    }
}