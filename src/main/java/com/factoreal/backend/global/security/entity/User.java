package com.factoreal.backend.global.security.entity;

import com.factoreal.backend.global.security.dto.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = "password") // password는 출력되지 않도록 방지
@EqualsAndHashCode(of = "username")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username",unique = true, nullable = false)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name="roles")
    private String roles;



    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        if (this.roles != null && !this.roles.isEmpty()) {
            for (String role : this.roles.split(",")) {
                authorities.add(new SimpleGrantedAuthority(role.trim()));
            }
        }
        // Default to ROLE_USER if no roles are specified, or handle as needed
        // For an admin-only service, you might always want ROLE_ADMIN
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN")); // Default for admin service
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }
}
