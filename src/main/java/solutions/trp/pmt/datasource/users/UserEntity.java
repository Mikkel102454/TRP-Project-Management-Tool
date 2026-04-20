package solutions.trp.pmt.datasource.users;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import solutions.trp.pmt.dto.ProjectDto;
import solutions.trp.pmt.dto.UserDto;

import java.util.Collection;
import java.util.List;

/**
 * User entity hold user data
 *
 */
@Entity(name = "users")
public class UserEntity implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = true, nullable = false, name = "username")
    private String username;

    @Column(unique = false, nullable = false, name = "initial")
    private String initial;

    @Column(unique = false, nullable = false, name = "password")
    private String password;

    @Column(unique = false, nullable = false, name = "is_enabled")
    private boolean enabled;

    @Column(unique = false, nullable = false, name = "is_admin")
    private boolean admin;

    public int getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getInitial() {
        return initial;
    }

    public void setInitial(String initial) {
        this.initial = initial;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (admin) {
            return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"));
        } else {
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }
    }

    public UserDto toDto() {
        UserDto dto = new UserDto();
        dto.setId(id);
        dto.setUsername(username);
        dto.setInitial(initial);
        dto.setAdmin(admin);
        dto.setEnabled(enabled);

        return dto;
    }
}
