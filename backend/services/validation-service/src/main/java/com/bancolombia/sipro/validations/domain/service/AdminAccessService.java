package com.bancolombia.sipro.validations.domain.service;

import com.bancolombia.sipro.validations.application.dto.UsuarioPermisosResponse;
import com.bancolombia.sipro.validations.infrastructure.config.AdminPanelProperties;
import com.bancolombia.sipro.validations.infrastructure.security.SiproAuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.stream.Stream;

/**
 * Centraliza el acceso al panel de administración.
 */
@Service
public class AdminAccessService {

    private static final Logger logger = LoggerFactory.getLogger(AdminAccessService.class);

    private final RbacService rbacService;
    private final AdminPanelProperties adminPanelProperties;

    public AdminAccessService(RbacService rbacService, AdminPanelProperties adminPanelProperties) {
        this.rbacService = rbacService;
        this.adminPanelProperties = adminPanelProperties;
    }

    /**
     * Acceso general a la ruta /admin: Soporte Técnico (dashboard, consola SQL, logs) o
     * Admin_Permisos (que además necesita entrar aquí para ejecutar consolidación manual).
     * Para restringir una acción a un único perfil dentro de /admin, usar
     * {@link #requireAdminTecnico} o {@link #requireAdminPermisos} en su lugar.
     */
    public void requireAdmin(SiproAuthenticatedUser principal) {
        if (!isAdmin(principal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No tienes permisos administrativos para usar este panel.");
        }
    }

    public boolean isAdmin(SiproAuthenticatedUser principal) {
        return isAdminTecnico(principal) || isAdminPermisos(principal);
    }

    /** Soporte Técnico (id_rol=3): dashboard, consola SQL y logs del panel /admin. */
    public void requireAdminTecnico(SiproAuthenticatedUser principal) {
        if (!isAdminTecnico(principal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No tienes permisos administrativos para usar este panel.");
        }
    }

    public boolean isAdminTecnico(SiproAuthenticatedUser principal) {
        UsuarioPermisosResponse permisos = obtenerPermisos(principal);
        if (permisos != null && permisos.isPuedeAccederPanelAdmin()) {
            return true;
        }
        return esFallbackLegado(principal);
    }

    /** Admin_Permisos (id_rol=6): /parametros y ejecución de consolidación manual. */
    public void requireAdminPermisos(SiproAuthenticatedUser principal) {
        if (!isAdminPermisos(principal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No tienes permisos administrativos para usar este panel.");
        }
    }

    public boolean isAdminPermisos(SiproAuthenticatedUser principal) {
        UsuarioPermisosResponse permisos = obtenerPermisos(principal);
        if (permisos != null && permisos.isPuedeModificarParametros()) {
            return true;
        }
        return esFallbackLegado(principal);
    }

    private UsuarioPermisosResponse obtenerPermisos(SiproAuthenticatedUser principal) {
        if (principal == null || principal.idUsuario() == null) {
            return null;
        }
        try {
            return rbacService.obtenerPermisosUsuario(principal.idUsuario(), principal.groupNames());
        } catch (Exception ex) {
            logger.warn("No fue posible validar RBAC admin para usuario {}: {}",
                    principal.idUsuario(), ex.getMessage());
            return null;
        }
    }

    private boolean esFallbackLegado(SiproAuthenticatedUser principal) {
        if (principal == null) {
            return false;
        }

        boolean legacyAdmin = Stream.of(
                        principal.usuario(),
                        principal.alias(),
                        principal.preferredUsername(),
                        principal.email())
                .filter(value -> value != null && !value.isBlank())
                .map(this::normalize)
            .anyMatch(adminPanelProperties.getAccess().getLegacyAdminIdentitiesNormalized()::contains);

        if (legacyAdmin) {
            logger.warn("Acceso admin concedido por fallback legado al usuario {}", principal.idUsuario());
        }

        return legacyAdmin;
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}