package com.backend.gestion.ariel.gestion_dinero.services;

import java.util.List;
import java.util.Optional;

import com.backend.gestion.ariel.gestion_dinero.entities.Usuario;

public interface UsuarioService {
    List<Usuario> listarTodos();
    Optional<Usuario> buscarPorId(Long id);
    Optional<Usuario> buscarPorEmail(String email);
    Usuario guardar(Usuario usuario);
    Usuario actualizar(Long id, Usuario usuario);
    void eliminar(Long id);
}

