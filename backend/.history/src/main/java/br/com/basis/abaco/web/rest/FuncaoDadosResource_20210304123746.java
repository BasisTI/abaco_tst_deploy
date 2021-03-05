package br.com.basis.abaco.web.rest;

import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.Timed;

import br.com.basis.abaco.domain.Analise;
import br.com.basis.abaco.domain.Der;
import br.com.basis.abaco.domain.FuncaoDados;
import br.com.basis.abaco.domain.Rlr;
import br.com.basis.abaco.domain.enumeration.StatusFuncao;
import br.com.basis.abaco.domain.enumeration.TipoFatorAjuste;
import br.com.basis.abaco.repository.AnaliseRepository;
import br.com.basis.abaco.repository.FuncaoDadosRepository;
import br.com.basis.abaco.repository.search.FuncaoDadosSearchRepository;
import br.com.basis.abaco.service.FuncaoDadosService;
import br.com.basis.abaco.service.dto.*;
import br.com.basis.abaco.web.rest.util.HeaderUtil;
import io.github.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing FuncaoDados.
 */
@RestController
@RequestMapping("/api")
public class FuncaoDadosResource {

    private static final int decimalPlace = 2;
    public static final String WHITE_SPACE = " ";
    public static final char PERCENTUAL = '%';
    public static final String PF = "PF";
    private final Logger log = LoggerFactory.getLogger(FuncaoDadosResource.class);
    private static final String ENTITY_NAME = "funcaoDados";
    private final FuncaoDadosRepository funcaoDadosRepository;
    private final FuncaoDadosSearchRepository funcaoDadosSearchRepository;
    private final FuncaoDadosService funcaoDadosService;
    private final AnaliseRepository analiseRepository;

    public FuncaoDadosResource(FuncaoDadosRepository funcaoDadosRepository,
                               FuncaoDadosSearchRepository funcaoDadosSearchRepository, FuncaoDadosService funcaoDadosService, AnaliseRepository analiseRepository) {
        this.funcaoDadosRepository = funcaoDadosRepository;
        this.funcaoDadosSearchRepository = funcaoDadosSearchRepository;
        this.funcaoDadosService = funcaoDadosService;
        this.analiseRepository = analiseRepository;
    }

    /**
     * POST  /funcao-dados : Create a new funcaoDados.
     *
     * @param funcaoDadoADadosEditDTO the funcaoDados to create
     * @return the ResponseEntity with status 201 (Created) and with body the new funcaoDados, or with status 400 (Bad Request) if the funcaoDados has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/funcao-dados/{idAnalise}")
    @Timed
    public ResponseEntity<FuncaoDadosEditDTO> createFuncaoDados(@PathVariable Long idAnalise, @RequestBody FuncaoDadosSaveDTO funcaoDadosSaveDTO) throws URISyntaxException {
        log.debug("REST request to save FuncaoDados : {}", funcaoDadosSaveDTO);
        Analise analise = analiseRepository.findOne(idAnalise);
        funcaoDadosSaveDTO.getDers().forEach(der -> { der.setFuncaoDados(funcaoDadosSaveDTO);});
        funcaoDadosSaveDTO.getRlrs().forEach(rlr -> { rlr.setFuncaoDados(funcaoDadosSaveDTO);});
        FuncaoDados funcaoDados = convertToEntity(funcaoDadosSaveDTO);
        funcaoDados.setAnalise(analise);
        if (funcaoDados.getId() != null || funcaoDados.getAnalise() == null || funcaoDados.getAnalise().getId() == null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert(ENTITY_NAME, "idexists", "A new funcaoDados cannot already have an ID")).body(null);
        }
        FuncaoDados result = funcaoDadosRepository.save(funcaoDados);
        FuncaoDadosEditDTO  funcaoDadosEditDTO = convertFuncaoDadoAEditDTO(result);
        return ResponseEntity.created(new URI("/api/funcao-dados/" + funcaoDadosEditDTO.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, funcaoDadosEditDTO.getId().toString()))
                .body(funcaoDadosEditDTO);
    }

    /**
     * PUT  /funcao-dados : Updates an existing funcaoDados.
     *
     * @param funcaoDadosEditDTO the funcaoDados to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated funcaoDados,
     * or with status 400 (Bad Request) if the funcaoDados is not valid,
     * or with status 500 (Internal Server Error) if the funcaoDados couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/funcao-dados/{id}")
    @Timed
    public ResponseEntity<FuncaoDadosEditDTO> updateFuncaoDados(@PathVariable Long id, @RequestBody FuncaoDadosSaveDTO funcaoDadosSaveDTO) throws URISyntaxException {
        log.debug("REST request to update FuncaoDados : {}", funcaoDadosSaveDTO);
        FuncaoDados funcaoDadosOld = funcaoDadosRepository.findById(id);
        FuncaoDados funcaoDados = convertToEntity(funcaoDadosSaveDTO);
        if (funcaoDados.getId() == null) {
            return createFuncaoDados(funcaoDados.getAnalise().getId(), funcaoDadosSaveDTO);
        }
        Analise analise = analiseRepository.findOne(funcaoDadosOld.getAnalise().getId());
        funcaoDados.setAnalise(analise);
        if (funcaoDados.getAnalise() == null || funcaoDados.getAnalise().getId() == null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert(ENTITY_NAME, "idexists", "A new funcaoDados cannot already have an ID")).body(null);
        }
        FuncaoDados funcaoDadosUpdate = updateFuncaoDados(funcaoDadosOld, funcaoDados);
        FuncaoDados result = funcaoDadosRepository.save(funcaoDadosUpdate);
        FuncaoDadosEditDTO funcaoDadosEditDTO = convertFuncaoDadoAEditDTO(result);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, funcaoDados.getId().toString())).body(funcaoDadosEditDTO);
    }

    /**
     * GET  /funcao-dados : get all the funcaoDados.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of funcaoDados in body
     */
    @GetMapping("/funcao-dados")
    @Timed
    public List<FuncaoDados> getAllFuncaoDados() {
        log.debug("REST request to get all FuncaoDados");
        List<FuncaoDados> funcaoDados = funcaoDadosRepository.findAll();
        funcaoDados.stream().filter(f -> f.getAnalise() != null).forEach(f -> {
            if (f.getAnalise().getFuncaoDados() != null) {
                f.getAnalise().getFuncaoDados().clear();
            }
            if (f.getAnalise().getFuncaoTransacaos() != null) {
                f.getAnalise().getFuncaoTransacaos().clear();
            }
        });
        return funcaoDados;
    }

    /**
     * GET  /funcao-dados/:id : get the "id" funcaoDados.
     *
     * @param id the id of the funcaoDados to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the funcaoDados, or with status 404 (Not Found)
     */
    @GetMapping("/funcao-dados/{id}")
    @Timed
    public ResponseEntity<FuncaoDadoApiDTO> getFuncaoDados(@PathVariable Long id) {
        log.debug("REST request to get FuncaoDados : {}", id);
        FuncaoDados funcaoDados = funcaoDadosRepository.findByIdOrderByDersIdAscRlrsIdAsc(id);
        if (funcaoDados.getAnalise().getFuncaoDados() != null) {
            funcaoDados.getAnalise().getFuncaoDados().clear();
        }
        if (funcaoDados.getAnalise().getFuncaoTransacaos() != null) {
            funcaoDados.getAnalise().getFuncaoTransacaos().clear();
        }
        FuncaoDadoApiDTO funcaoDadosDTO = getFuncaoDadoApiDTO(funcaoDados);
        return ResponseUtil.wrapOrNotFound(Optional.ofNullable(funcaoDadosDTO));
    }

    @GetMapping("/funcao-dados-dto/analise/{id}")
    @Timed
    public ResponseEntity<List<FuncaoDadoAnaliseDTO>> getFuncaoDadosByAnalise(@PathVariable Long id) {
        Set<FuncaoDados> lstFuncaoDados = funcaoDadosRepository.findByAnaliseId(id);
        List<FuncaoDadoAnaliseDTO> lstFuncaoDadosDTO = lstFuncaoDados.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseUtil.wrapOrNotFound(Optional.ofNullable(lstFuncaoDadosDTO));
    }

    /**
     * GET  /funcao-dados/analise/:id : get the "id" analise.
     *
     * @param id the id of the funcaoDados to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the funcaoDados, or with status 404 (Not Found)
     */
    @GetMapping("/funcao-dados/analise/{id}")
    @Timed
    public Set<FuncaoDados> getFuncaoDadosAnalise(@PathVariable Long id) {
        log.debug("REST request to get FuncaoDados : {}", id);
        Set<FuncaoDados> funcaoDados = null;
        funcaoDados = funcaoDadosRepository.findByAnaliseId(id);
        return funcaoDados;
    }

    /**
     * DELETE  /funcao-dados/:id : delete the "id" funcaoDados.
     *
     * @param id the id of the funcaoDados to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/funcao-dados/{id}")
    @Timed
    public ResponseEntity<Void> deleteFuncaoDados(@PathVariable Long id) {
        log.debug("REST request to delete FuncaoDados : {}", id);
        funcaoDadosRepository.delete(id);
        funcaoDadosSearchRepository.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

    /**
     * SEARCH  /_search/funcao-dados?query=:query : search for the funcaoDados corresponding
     * to the query.
     *
     * @param query the query of the funcaoDados search
     * @return the result of the search
     */
    @GetMapping("/_search/funcao-dados")
    @Timed
    public List<FuncaoDados> searchFuncaoDados(@RequestParam(defaultValue = "*") String query) {
        log.debug("REST request to search FuncaoDados for query {}", query);
        return StreamSupport
                .stream(funcaoDadosSearchRepository.search(queryStringQuery(query)).spliterator(), false)
                .collect(Collectors.toList());
    }

    @GetMapping("/funcao-dados/drop-down")
    @Timed
    public List<DropdownDTO> getFuncaoDadosDropdown() {
        log.debug("REST request to get dropdown FuncaoDados");
        return funcaoDadosService.getFuncaoDadosDropdown();
    }

    @GetMapping("/funcao-dados/{idAnalise}/{idfuncionalidade}/{idModulo}")
    @Timed
    public ResponseEntity<Boolean> existFuncaoDados(@PathVariable Long idAnalise, @PathVariable Long idfuncionalidade, @PathVariable Long idModulo, @RequestParam String name, @RequestParam(required = false) Long id) {
        log.debug("REST request to exist FuncaoDados");
        Boolean existInAnalise;
        if (id != null && id > 0) {

            existInAnalise = funcaoDadosRepository.existsByNameAndAnaliseIdAndFuncionalidadeIdAndFuncionalidadeModuloIdAndIdNot(name, idAnalise, idfuncionalidade, idModulo, id);
        } else {
            existInAnalise = funcaoDadosRepository.existsByNameAndAnaliseIdAndFuncionalidadeIdAndFuncionalidadeModuloId(name, idAnalise, idfuncionalidade, idModulo);
        }
        return ResponseEntity.ok(existInAnalise);
    }

    /**
     * GET  /funcao-dados/:id : update status the "id" funcaoDados.
     *
     * @param id the id of the funcaoDados to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the funcaoDados, or with status 404 (Not Found)
     */
    @GetMapping("/funcao-dados/update-status/{id}/{statusFuncao}")
    @Timed
    public ResponseEntity<FuncaoDadoApiDTO> updateStatusFuncaoDados(@PathVariable Long id, @PathVariable StatusFuncao statusFuncao) {
        log.debug("REST request to update status FuncaoDados : {}", id);
        FuncaoDados funcaoDados = funcaoDadosRepository.findOne(id);
        funcaoDados.setStatusFuncao(statusFuncao);
        FuncaoDados result = funcaoDadosRepository.save(funcaoDados);
        FuncaoDadoApiDTO funcaoDadosDTO = getFuncaoDadoApiDTO(result);
        return ResponseUtil.wrapOrNotFound(Optional.ofNullable(funcaoDadosDTO));
    }


    private FuncaoDadoAnaliseDTO convertToDto(FuncaoDados funcaoDados) {
        FuncaoDadoAnaliseDTO funcaoDadoAnaliseDTO = new ModelMapper().map(funcaoDados, FuncaoDadoAnaliseDTO.class);
        funcaoDadoAnaliseDTO.setRlrFilter(getValueRlr(funcaoDados));
        funcaoDadoAnaliseDTO.setDerFilter(getValueDer(funcaoDados));
        funcaoDadoAnaliseDTO.setFatorAjusteFilter(getFatorAjusteFilter(funcaoDados));
        funcaoDadoAnaliseDTO.setHasSustantation(getSustantation(funcaoDados));
        return funcaoDadoAnaliseDTO;
    }

    private String getFatorAjusteFilter(FuncaoDados funcaoDados) {
        StringBuilder fatorAjustFilterSB = new StringBuilder();
        if (!(funcaoDados.getFatorAjuste().getCodigo().isEmpty())) {
            fatorAjustFilterSB.append(funcaoDados.getFatorAjuste().getCodigo()).append(WHITE_SPACE);
        }
        if (!(funcaoDados.getFatorAjuste().getOrigem().isEmpty())) {
            fatorAjustFilterSB.append(funcaoDados.getFatorAjuste().getOrigem()).append(WHITE_SPACE);
        }
        if (!(funcaoDados.getFatorAjuste().getNome().isEmpty())) {
            fatorAjustFilterSB.append(funcaoDados.getFatorAjuste().getNome()).append(WHITE_SPACE);
        }
        fatorAjustFilterSB.append(funcaoDados.getFatorAjuste().getFator().setScale(decimalPlace)).append(WHITE_SPACE);

        if (funcaoDados.getFatorAjuste().getTipoAjuste() == TipoFatorAjuste.PERCENTUAL) {
            fatorAjustFilterSB.append(PERCENTUAL);
        } else {
            fatorAjustFilterSB.append(PF);
        }
        return fatorAjustFilterSB.toString();
    }

    private Integer getValueDer(FuncaoDados funcaoDados) {
        int dersValues = funcaoDados.getDers().size();
        if (dersValues == 1) {
            Der der = funcaoDados.getDers().iterator().next();
            if (der.getValor() != null) {
                dersValues = der.getValor();
            }
        }
        return dersValues;
    }

    private Integer getValueRlr(FuncaoDados funcaoDados) {
        int rlrsValues = funcaoDados.getRlrs().size();
        if (rlrsValues == 1) {
            Rlr rlr = funcaoDados.getRlrs().iterator().next();
            if (rlr.getValor() != null) {
                rlrsValues = rlr.getValor();
            }
        }
        return rlrsValues;
    }

    private Boolean getSustantation(FuncaoDados funcaoDados) {
        return funcaoDados.getSustantation() != null && !(funcaoDados.getSustantation().isEmpty());
    }

    private FuncaoDadoApiDTO getFuncaoDadoApiDTO(FuncaoDados funcaoDados) {
        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(funcaoDados, FuncaoDadoApiDTO.class);
    }

    private FuncaoDadosEditDTO convertFuncaoDadoAEditDTO(FuncaoDados funcaoDados) {
        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(funcaoDados, FuncaoDadosEditDTO.class);
    }


    private FuncaoDados convertToEntity(FuncaoDadosSaveDTO funcaoDadosSaveDTO){
        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(funcaoDadosSaveDTO, FuncaoDados.class);
    }

    private FuncaoDados updateFuncaoDados(FuncaoDados funcaoDadosOld, FuncaoDados funcaoDados) {
        funcaoDadosOld.setAlr(funcaoDados.getAlr());
        funcaoDadosOld.setFuncionalidade(funcaoDados.getFuncionalidade());
        funcaoDadosOld.setName(funcaoDados.getName());
        funcaoDadosOld.setComplexidade(funcaoDados.getComplexidade());
        funcaoDadosOld.setFatorAjuste(funcaoDados.getFatorAjuste());
        funcaoDadosOld.setPf(funcaoDados.getPf());
        funcaoDadosOld.setGrossPF(funcaoDados.getGrossPF());
        funcaoDadosOld.setSustantation(funcaoDados.getSustantation());
        setDersAndRlrs(funcaoDadosOld, funcaoDados);
        funcaoDadosOld.setTipo(funcaoDados.getTipo());
        funcaoDadosOld.setComplexidade(funcaoDados.getComplexidade());
        funcaoDadosOld.setStatusFuncao(funcaoDados.getStatusFuncao());
        funcaoDadosOld.setQuantidade(funcaoDados.getQuantidade());
        return  funcaoDadosOld;
    }

    private void setDersAndRlrs(FuncaoDados funcaoDadosOld, FuncaoDados funcaoDados) {
        Set<Der> lstDers = new HashSet<>();
        Set<Rlr> lstRlrs = new HashSet<>();
        funcaoDados.getDers().forEach(der -> {
            der.setFuncaoDados(funcaoDadosOld);
            lstDers.add(der);
        });
        funcaoDados.getRlrs().forEach(rlr -> {
            rlr.setFuncaoDados(funcaoDadosOld);
            lstRlrs.add(rlr);
        });
        funcaoDadosOld.updateDers(lstDers);
        funcaoDadosOld.updateRlrs(lstRlrs);
    }

}