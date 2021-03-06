package br.com.basis.abaco.domain;

import br.com.basis.abaco.domain.enumeration.StatusFuncao;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.math.BigDecimal;

@MappedSuperclass
@Getter
@Setter
public class VwFuncao {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "tipo")
    private String tipo;

    @Column(name = "complexidade")
    private String complexidade;

    @Column(name = "pf")
    private BigDecimal pf;

    @Column(name = "grosspf")
    private BigDecimal grossPF;

    @Column(name = "analise_id")
    private Long analiseId;

    @Column(name = "fator_ajuste_id")
    private String fatorAjusteId;

    @Column(name = "name")
    private String name;

    @Column(name = "sustantation")
    private String sustantation;

    @Column(name = "funcionalidade_id")
    private Long funcionalidadeId;

    @Column(name = "nome_funcionalidade")
    private String nomeFuncionalidade;

    @Column(name = "equipe_id")
    private Long equipeId;

    @Column(name = "nome_equipe")
    private String nomeEquipe;

    @Column(name = "id_modulo")
    private Long idModulo;

    @Column(name = "nome_modulo")
    private String nomeModulo;

    @Column(name = "deflator")
    private String deflator;

    @Column(name = "total_ders")
    private Integer totalDers;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_funcao")
    private StatusFuncao statusFuncao;

    @Column(name="ordem")
    private Long ordem;
}
