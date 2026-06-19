package com.delamaderaalcodigo.tallerapi.security;

import com.delamaderaalcodigo.tallerapi.model.Usuario;
import com.delamaderaalcodigo.tallerapi.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Única clase que conoce a la vez nuestro modelo de dominio (Usuario, Rol) y el contrato
 * UserDetails que entiende Spring Security. El resto del código de seguridad (filtro,
 * AuthenticationManager, etc) trabaja siempre con UserDetails, nunca con Usuario
 * directamente- así, si el día de mañana cambia el modelo de persistenci, solo hay que
 * tocar esta clase.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No existe un usuario con username: "+ username));

        // Convención obligatoria de Spring Security: las authorities de rol deben llevar el
        // prefijo "ROLE_" para que funcionen los helpers hasRole("ADMIN") / hasRole("USER")
        // usados en SecurityConfig. Si se omite el prefijo, hasRole(...) nunca casa con nada
        // y toda request autorizada por rol devuelve 403 aunque el usuario sea correcto.
        String authority = "ROLE_"+ usuario.getRol().name();

        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPasswordHash())
                .authorities(authority)
                .build();
    }
}
