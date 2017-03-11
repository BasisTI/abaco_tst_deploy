package br.com.basis.abaco.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.springframework.data.elasticsearch.annotations.Document;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;

/**
 * A Sistema.
 */
@Entity
@Table(name = "sistema")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Document(indexName = "sistema")

public class Sistema implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @Size(max = 20)
    @Column(name = "sigla", length = 20)
    private String sigla;

    @NotNull
    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "numero_ocorrencia")
    private String numeroOcorrencia;

    @ManyToOne
    private Organizacao organizacao;

    @OneToMany(mappedBy = "sistema",cascade = {CascadeType.PERSIST,CascadeType.MERGE,CascadeType.REMOVE})
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<Modulo> modulos = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSigla() {
        return sigla;
    }

    public Sistema sigla(String sigla) {
        this.sigla = sigla;
        return this;
    }

    public void setSigla(String sigla) {
        this.sigla = sigla;
    }

    public String getNome() {
        return nome;
    }

    public Sistema nome(String nome) {
        this.nome = nome;
        return this;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getNumeroOcorrencia() {
        return numeroOcorrencia;
    }

    public Sistema numeroOcorrencia(String numeroOcorrencia) {
        this.numeroOcorrencia = numeroOcorrencia;
        return this;
    }

    public void setNumeroOcorrencia(String numeroOcorrencia) {
        this.numeroOcorrencia = numeroOcorrencia;
    }

    public Organizacao getOrganizacao() {
        return organizacao;
    }

    public Sistema organizacao(Organizacao organizacao) {
        this.organizacao = organizacao;
        return this;
    }

    public void setOrganizacao(Organizacao organizacao) {
        this.organizacao = organizacao;
    }

    public Set<Modulo> getModulos() {
        return modulos;
    }

    public Sistema modulos(Set<Modulo> modulos) {
        this.modulos = modulos;
        return this;
    }

    public Sistema addModulo(Modulo modulo) {
        this.modulos.add(modulo);
        modulo.setSistema(this);
        return this;
    }

    public Sistema removeModulo(Modulo modulo) {
        this.modulos.remove(modulo);
        modulo.setSistema(null);
        return this;
    }

    public void setModulos(Set<Modulo> modulos) {
        this.modulos = modulos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Sistema sistema = (Sistema) o;
        if (sistema.id == null || id == null) {
            return false;
        }
        return Objects.equals(id, sistema.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Sistema{" +
            "id=" + id +
            ", sigla='" + sigla + "'" +
            ", nome='" + nome + "'" +
            ", numeroOcorrencia='" + numeroOcorrencia + "'" +
            '}';
    }
}
