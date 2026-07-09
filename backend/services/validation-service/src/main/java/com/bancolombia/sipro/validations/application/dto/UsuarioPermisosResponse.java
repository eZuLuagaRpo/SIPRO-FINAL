package com.bancolombia.sipro.validations.application.dto;

import java.util.List;

/**
 * DTO que encapsula los permisos efectivos del usuario tras el login.
 * Se incluye en el LoginResponse para que el frontend conozca
 * qué acciones están habilitadas/deshabilitadas.
 */
public class UsuarioPermisosResponse {

    private boolean puedeCargar;
    private boolean puedeAprobar;
    private boolean puedeSolicitarAprobacion;
    private boolean puedeVisualizar;
    private boolean puedeExportar;
    private boolean puedeModificarParametros;
    /** Acceso al panel de administrador: calculado dinámicamente desde sipro_roles_permisos, sin IDs quemados. */
    private boolean puedeAccederPanelAdmin;
    private List<ProductoRolResponse> productosAsignados;

    public UsuarioPermisosResponse() {
    }

    // Getters y Setters
    public boolean isPuedeCargar() {
        return puedeCargar;
    }

    public void setPuedeCargar(boolean puedeCargar) {
        this.puedeCargar = puedeCargar;
    }

    public boolean isPuedeAprobar() {
        return puedeAprobar;
    }

    public void setPuedeAprobar(boolean puedeAprobar) {
        this.puedeAprobar = puedeAprobar;
    }

    public boolean isPuedeSolicitarAprobacion() {
        return puedeSolicitarAprobacion;
    }

    public void setPuedeSolicitarAprobacion(boolean puedeSolicitarAprobacion) {
        this.puedeSolicitarAprobacion = puedeSolicitarAprobacion;
    }

    public boolean isPuedeVisualizar() {
        return puedeVisualizar;
    }

    public void setPuedeVisualizar(boolean puedeVisualizar) {
        this.puedeVisualizar = puedeVisualizar;
    }

    public boolean isPuedeExportar() {
        return puedeExportar;
    }

    public void setPuedeExportar(boolean puedeExportar) {
        this.puedeExportar = puedeExportar;
    }

    public boolean isPuedeModificarParametros() {
        return puedeModificarParametros;
    }

    public void setPuedeModificarParametros(boolean puedeModificarParametros) {
        this.puedeModificarParametros = puedeModificarParametros;
    }

    public boolean isPuedeAccederPanelAdmin() {
        return puedeAccederPanelAdmin;
    }

    public void setPuedeAccederPanelAdmin(boolean puedeAccederPanelAdmin) {
        this.puedeAccederPanelAdmin = puedeAccederPanelAdmin;
    }

    public List<ProductoRolResponse> getProductosAsignados() {
        return productosAsignados;
    }

    public void setProductosAsignados(List<ProductoRolResponse> productosAsignados) {
        this.productosAsignados = productosAsignados;
    }

    /**
     * Detalle de un producto-rol asignado al usuario.
     */
    public static class ProductoRolResponse {
        private Long idProducto;
        private String tituloProducto;
        private Integer idRol;
        private String nombreRol;
        private Short ordenFlujo;

        public ProductoRolResponse() {
        }

        public ProductoRolResponse(Long idProducto, String tituloProducto,
                                   Integer idRol, String nombreRol, Short ordenFlujo) {
            this.idProducto = idProducto;
            this.tituloProducto = tituloProducto;
            this.idRol = idRol;
            this.nombreRol = nombreRol;
            this.ordenFlujo = ordenFlujo;
        }

        public Long getIdProducto() {
            return idProducto;
        }

        public void setIdProducto(Long idProducto) {
            this.idProducto = idProducto;
        }

        public String getTituloProducto() {
            return tituloProducto;
        }

        public void setTituloProducto(String tituloProducto) {
            this.tituloProducto = tituloProducto;
        }

        public Integer getIdRol() {
            return idRol;
        }

        public void setIdRol(Integer idRol) {
            this.idRol = idRol;
        }

        public String getNombreRol() {
            return nombreRol;
        }

        public void setNombreRol(String nombreRol) {
            this.nombreRol = nombreRol;
        }

        public Short getOrdenFlujo() {
            return ordenFlujo;
        }

        public void setOrdenFlujo(Short ordenFlujo) {
            this.ordenFlujo = ordenFlujo;
        }
    }
}
