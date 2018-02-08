package br.com.basis.abaco.domain;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.SequenceGenerator;

import com.fasterxml.jackson.annotation.JsonBackReference;

import br.com.basis.abaco.domain.enumeration.Complexidade;

@MappedSuperclass
public abstract class FuncaoAnalise {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "complexidade")
    private Complexidade complexidade;

    @Column(name = "pf", precision = 10, scale = 2)
    private BigDecimal pf;

    @Column(name = "grosspf", precision = 10, scale = 2)
    private BigDecimal grossPF;

    @ManyToOne
    @JoinColumn(name = "analise_id")
    @JsonBackReference
    private Analise analise;
    
    @ManyToOne
    @JoinColumn(name = "funcionalidade_id")
    private Funcionalidade funcionalidade;
    
    @Column
    private String detStr;
    
    @ManyToOne
    private FatorAjuste fatorAjuste;
    
    @Column
    private String name;
    
    @Column
    private String sustantation;
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    
    public Complexidade getComplexidade() {
        return complexidade;
    }

    public void setComplexidade(Complexidade complexidade) {
        this.complexidade = complexidade;
    }

    public BigDecimal getPf() {
        return pf;
    }

    public void setPf(BigDecimal pf) {
        this.pf = pf;
    }

    public Analise getAnalise() {
        return analise;
    }

    public void setAnalise(Analise analise) {
        this.analise = analise;
    }
    
    public FatorAjuste getFatorAjuste() {
        return fatorAjuste;
    }

    public void setFatorAjuste(FatorAjuste fatorAjuste) {
        this.fatorAjuste = fatorAjuste;
    }


    public Funcionalidade getFuncionalidade() {
        return funcionalidade;
    }

    public void setFuncionalidade(Funcionalidade funcionalidade) {
        this.funcionalidade = funcionalidade;
    }

    public String getDetStr() {
        return detStr;
    }

    public void setDetStr(String detStr) {
        this.detStr = detStr;
    }


    public BigDecimal getGrossPF() {
        return grossPF;
    }

    public void setGrossPF(BigDecimal grossPF) {
        this.grossPF = grossPF;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    

    public String getSustantation() {
        return sustantation;
    }


    public void setSustantation(String sustantation) {
        this.sustantation = sustantation;
    }
}