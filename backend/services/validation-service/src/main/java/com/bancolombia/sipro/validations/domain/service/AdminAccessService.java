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

    public void requireAdmin(SiproAuthenticatedUser principal) {
        if (!isAdmin(principal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No tienes permisos administrativos para usar este panel.");
        }
    }

    public boolean isAdmin(SiproAuthenticatedUser principal) {
        if (principal == null || principal.idUsuario() == null) {
            return false;
        }

        try {
            UsuarioPermisosResponse permisos = rbacService.obtenerPermisosUsuario(
                    principal.idUsuario(),
                    principal.groupNames());
            if (permisos.isPuedeModificarParametros()) {
                return true;
            }
        } catch (Exception ex) {
            logger.warn("No fue posible validar RBAC admin para usuario {}: {}",
                    principal.idUsuario(), ex.getMessage());
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