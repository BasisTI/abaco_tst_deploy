package br.com.basis.abaco.repository;

import br.com.basis.abaco.domain.Manual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the Manual entity.
 */
@SuppressWarnings("unused")
public interface ManualRepository extends JpaRepository<Manual, Long> {

    Optional<Manual> findOneByNome (String nome);

}
