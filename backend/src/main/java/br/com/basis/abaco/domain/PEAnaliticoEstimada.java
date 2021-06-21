package br.com.basis.abaco.domain;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.springframework.data.elasticsearch.annotations.Document;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "pe_analitico_estimada")
@Document(indexName = "pe_analitico_estimada")
@Immutable
@Getter
@Setter
public class PEAnaliticoEstimada implements Serializable {

    @Id
    @Column(name = "row_number")
    private Long id;

    @Column(name = "id_sistema")
    private Long idsistema;

    @Column(name = "equipe_responsavel_id")
    private Long equipeResponsavelId;

    @Column(name = "id_funcao_dados")
    private Long idfuncaodados;

    @Column(name = "tipo")
    private String tipo;

    @Column(name = "nome")
    private String nome;

    @Column(name = "nome_equipe")
    private String nomeEquipe;

    @Column(name = "classificacao")
    private String classificacao;

    @Column(name = "data_homologacao_software")
    private String dataHomologacao;

    @Column(name = "analise_id")
    private Long analiseid;

    @Column(name = "sigla")
    private String sigla;

    @Column(name = "id_funcionalidade")
    private Long idFuncionalidade;

    @Column(name = "nome_funcionalidade")
    private String nomeFuncionalidade;

    @Column(name = "name")
    private String name;

    @Column(name = "pf")
    private BigDecimal pf;

    @Column(name = "id_modulo")
    private Long idModulo;

    @Column(name = "nome_modulo")
    private String nomeModulo;

    @Column(name = "complexidade")
    private String complexidade;

}