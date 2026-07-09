/**
 * Usuario autenticado en frontend con datos básicos de perfil y sesión.
 */
export interface User {
  username: string;
  name: string;
  role: string;
  sessionTimeoutMinutes?: number;
  sessionExpiresAt?: number;
  // Campos adicionales del backend
  idUsuario?: string;
  usuario?: string;
  nombres?: string;
  apellidos?: string;
  correo?: string;
  areaNombre?: string;
  jefeNombre?: string;
  // Permisos RBAC por producto
  permisos?: UsuarioPermisos;
}

/**
 * Resumen de permisos efectivos del usuario (consolidados de todas sus asignaciones RBAC).
 */
export interface UsuarioPermisos {
  puedeCargar: boolean;
  puedeAprobar: boolean;
  puedeSolicitarAprobacion: boolean;
  puedeVisualizar: boolean;
  puedeExportar: boolean;
  puedeModificarParametros: boolean;
  /** Calculado dinámicamente por el backend desde sipro_roles_permisos — sin IDs quemados. */
  puedeAccederPanelAdmin?: boolean;
  productosAsignados?: ProductoRol[];
}

/**
 * Detalle de una asignación producto-rol para el usuario.
 */
export interface ProductoRol {
  idProducto: number;
  tituloProducto: string;
  idRol: number;
  nombreRol: string;
  ordenFlujo: number;
}

/**
 * Configuración pública mínima para iniciar autenticación con Entra ID.
 */
export interface EntraConfigResponse {
  enabled: boolean;
  clientId: string;
  tenantId: string;
  apiScope: string;
  apiAudience: string;
}

/**
 * Respuesta de autenticación con datos del usuario y permisos efectivos.
 */
export interface LoginResponse {
  success: boolean;
  message?: string;
  mensaje?: string;
  sessionTimeoutMinutes?: number;
  user?: User;
  // Campos adicionales del backend
  idUsuario?: string;
  usuario?: string;
  nombres?: string;
  apellidos?: string;
  correo?: string;
  areaNombre?: string;
  jefeNombre?: string;
  // Permisos RBAC
  permisos?: UsuarioPermisos;
}
