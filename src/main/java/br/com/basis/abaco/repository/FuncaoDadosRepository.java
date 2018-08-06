package br.com.basis.abaco.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.basis.abaco.domain.FuncaoDados;
import br.com.basis.abaco.domain.FuncaoDadosVersionavel;
import org.springframework.data.jpa.repository.Query;

/**
 * Spring Data JPA repository for the FuncaoDados entity.
 */
public interface FuncaoDadosRepository extends JpaRepository<FuncaoDados, Long> {

    Optional<FuncaoDados> findFirstByFuncaoDadosVersionavelOrderByAuditUpdatedOnDesc(
            FuncaoDadosVersionavel funcaoDadosVersionavel);

    Optional<FuncaoDados> findFirstByFuncaoDadosVersionavelIdOrderByAuditUpdatedOnDesc(
            Long funcaoDadosVersionavelId);

    List<FuncaoDados> findByFuncaoDadosVersionavelIn(List<FuncaoDadosVersionavel> funcoesDadosVersionaveis);

    @Query( value = "SELECT * FROM funcao_dados WHERE analise_id = ?1 AND name = ?2", nativeQuery = true)
    FuncaoDados findName(Long idAnalise, String name);

}
