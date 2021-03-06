package br.com.basis.abaco.repository;

import br.com.basis.abaco.domain.Modulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the Modulo entity.
 */
@SuppressWarnings("unused")
public interface ModuloRepository extends JpaRepository<Modulo, Long> {

    @Query(value = "SELECT m FROM Modulo m WHERE m.id = ( SELECT f.modulo.id FROM Funcionalidade f WHERE f.id = ?1 )")
    public Modulo findByFuncionalidade(Long id);

    @Query(value = "SELECT m FROM Modulo m WHERE lower(m.nome) = :nome AND m.sistema.id = :sistemaId")
    Optional<List<Modulo>> findAllByNomeAndSistemaId(@Param("nome") String nome, @Param("sistemaId") Long sistemaId);
}

