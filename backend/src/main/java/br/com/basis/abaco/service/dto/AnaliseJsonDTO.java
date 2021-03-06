package br.com.basis.abaco.service.dto;

import br.com.basis.abaco.domain.Contrato;
import br.com.basis.abaco.domain.EsforcoFase;
import br.com.basis.abaco.domain.FuncaoDados;
import br.com.basis.abaco.domain.FuncaoTransacao;
import br.com.basis.abaco.domain.Manual;
import br.com.basis.abaco.domain.Organizacao;
import br.com.basis.abaco.domain.Sistema;
import br.com.basis.abaco.domain.Status;
import br.com.basis.abaco.domain.TipoEquipe;
import br.com.basis.abaco.domain.User;
import br.com.basis.abaco.domain.enumeration.MetodoContagem;
import br.com.basis.abaco.domain.enumeration.TipoAnalise;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class AnaliseJsonDTO extends AnaliseEditDTO {

    private Set<FuncaoDados> funcaoDados = new HashSet<>();
    private Set<FuncaoTransacao> funcaoTransacaos = new HashSet<>();

}
