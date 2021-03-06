package br.com.basis.abaco.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldIndex;
import org.springframework.data.elasticsearch.annotations.FieldType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "vw_alr_all")
@Document(indexName = "vw_alr_all")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VwAlrAll {

    @Column(name = "funcao_id")
    private Long funcaoId;

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "nome")
    @Field(index = FieldIndex.not_analyzed, type = FieldType.String)
    private String nome;
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VwAlrAll vwAlrAll = (VwAlrAll) o;
        return Objects.equals(nome, vwAlrAll.nome) && Objects.equals(funcaoId, vwAlrAll.funcaoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nome, funcaoId);
    }
}
