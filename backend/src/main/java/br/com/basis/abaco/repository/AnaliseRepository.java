package br.com.basis.abaco.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.basis.abaco.domain.Analise;
import br.com.basis.abaco.domain.Sistema;

/**
 * Spring Data JPA repository for the Analise entity.
 */
@SuppressWarnings("unused")
public interface AnaliseRepository extends JpaRepository<Analise, Long> {


    @Query(value = "SELECT a FROM Analise a WHERE a.createdBy.id = ?1")
    List<Analise> findByCreatedBy(Long userid);

    @Query(value = "SELECT a FROM Analise a WHERE a.id IN :idAnalise")
    Page<Analise> findById(@Param("idAnalise") List<Long> idAnalises, Pageable pageable);

    @Query(value = "SELECT a.id FROM Analise a WHERE a.equipeResponsavel.id IN :equipes")
    List<Long> findAllByTipoEquipesId(@Param("equipes") List<Long> equipes);

    @Query(value = "SELECT a FROM Analise a WHERE a.enviarBaseline = true AND a.bloqueiaAnalise = true")
    List<Analise> findAllByBaseline();

    @Query(value = "SELECT a FROM Analise a WHERE a.id IN :idAnalise")
    Page<Analise> findByIds(@Param("idAnalise") List<Long> idAnalise, Pageable pageable);

    @Query(value = "SELECT count(*) FROM Analise a WHERE a.equipeResponsavel.id IN :equipes AND a.id = :idAnalise")
    int analiseEquipe(@Param("idAnalise") Long idAnalise, @Param("equipes") List<Long> equipes);

    @Query(value = "SELECT a.viewOnly FROM Compartilhada a WHERE a.analiseId = ?1")
    Boolean analiseCompartilhada(Long analiseId);

    @EntityGraph(attributePaths = {"compartilhadas", "funcaoDados", "funcaoTransacaos", "esforcoFases", "users", "fatorAjuste", "contrato"})
    Analise findOne(Long id);

    @EntityGraph(attributePaths = {"compartilhadas", "esforcoFases", "users", "fatorAjuste", "contrato"})
    Analise findById(Long id);


    @EntityGraph(attributePaths = {"compartilhadas", "esforcoFases", "users", "fatorAjuste", "contrato", "analisesComparadas"})
    Analise findOneById(Long id);

    @Query(value = "SELECT a " +
            "FROM Analise a " +
            "JOIN Sistema s              ON s.id = a.sistema.id " +
            "JOIN Organizacao o          ON o.id = a.organizacao.id " +
            "JOIN Modulo m               ON s.id = m.sistema.id " +
            "JOIN Funcionalidade f       ON f.modulo.id = m.id " +
            "JOIN FuncaoDados fd        ON fd.funcionalidade.id = f.id " +
            "JOIN FuncaoTransacao ft    ON ft.funcionalidade.id = f.id " +
            "JOIN FETCH FatorAjuste fa        ON fa.id = fd.fatorAjuste.id OR fa.id = ft.fatorAjuste.id " +
            "WHERE a.id = :id ORDER BY m.nome, f.nome, fd.name, ft.name")
    Analise reportContagem(@Param("id") Long id);

    List<Analise> findAll();

    @Query(value = "SELECT a FROM Analise a WHERE a.isDivergence = :divergencia")
    Page<Analise> pesquisarPorDivergencia(@Param("divergencia") Boolean divergencia, Pageable pageable);

    List<Analise> findAllBySistema(Sistema sistema);

    @Query(value = "SELECT a FROM Analise a WHERE a.equipeResponsavel.id = :equipeId AND a.sistema.id = :sistemaId")
    List<Analise> findBySistemaAndEquipe(@Param("equipeId") Long equipeId, @Param("sistemaId") Long sistemaId);

}
