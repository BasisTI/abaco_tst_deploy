package br.com.basis.abaco.web.rest;

import br.com.basis.abaco.domain.*;
import br.com.basis.abaco.domain.enumeration.TipoRelatorio;
import br.com.basis.abaco.reports.rest.RelatorioAnaliseRest;
import br.com.basis.abaco.repository.AnaliseRepository;
import br.com.basis.abaco.repository.CompartilhadaRepository;
import br.com.basis.abaco.repository.FuncaoDadosRepository;
import br.com.basis.abaco.repository.FuncaoDadosVersionavelRepository;
import br.com.basis.abaco.repository.FuncaoTransacaoRepository;
import br.com.basis.abaco.repository.UserRepository;
import br.com.basis.abaco.repository.search.AnaliseSearchRepository;
import br.com.basis.abaco.repository.search.UserSearchRepository;
import br.com.basis.abaco.security.AuthoritiesConstants;
import br.com.basis.abaco.security.SecurityUtils;
import br.com.basis.abaco.service.AnaliseService;
import br.com.basis.abaco.service.dto.AnaliseDTO;
import br.com.basis.abaco.service.exception.RelatorioException;
import br.com.basis.abaco.service.mapper.AnaliseMapper;
import br.com.basis.abaco.service.relatorio.RelatorioAnaliseColunas;
import br.com.basis.abaco.utils.AbacoUtil;
import br.com.basis.abaco.utils.PageUtils;
import br.com.basis.abaco.web.rest.util.HeaderUtil;
import br.com.basis.abaco.web.rest.util.PaginationUtil;
import br.com.basis.dynamicexports.service.DynamicExportsService;
import br.com.basis.dynamicexports.util.DynamicExporter;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.jhipster.web.util.ResponseUtil;
import net.sf.dynamicreports.report.exception.DRException;
import net.sf.jasperreports.engine.JRException;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.elasticsearch.core.DefaultEntityMapper;
import org.springframework.data.elasticsearch.core.DefaultResultMapper;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.EntityMapper;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.multiMatchQuery;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;


@RestController
@RequestMapping("/api")
public class AnaliseResource {

    private final Logger log = LoggerFactory.getLogger(AnaliseResource.class);
    private static final String ENTITY_NAME = "analise";
    private static final String PAGE = "page";
    private final AnaliseRepository analiseRepository;
    private final UserRepository userRepository;
    private final AnaliseService analiseService;
    private final CompartilhadaRepository compartilhadaRepository;
    private final AnaliseSearchRepository analiseSearchRepository;
    private final FuncaoDadosVersionavelRepository funcaoDadosVersionavelRepository;
    private final FuncaoDadosRepository funcaoDadosRepository;
    private final FuncaoTransacaoRepository funcaoTransacaoRepository;
    private RelatorioAnaliseRest relatorioAnaliseRest;
    private DynamicExportsService dynamicExportsService;
    private ElasticsearchTemplate elasticsearchTemplate;
    @Autowired
    private UserSearchRepository userSearchRepository;
    @Autowired
    private HttpServletRequest request;
    @Autowired
    private HttpServletResponse response;
    @Autowired
    private AnaliseMapper analiseMapper;

    public AnaliseResource(AnaliseRepository analiseRepository,
                           AnaliseSearchRepository analiseSearchRepository,
                           FuncaoDadosVersionavelRepository funcaoDadosVersionavelRepository,
                           DynamicExportsService dynamicExportsService,
                           UserRepository userRepository,
                           FuncaoDadosRepository funcaoDadosRepository,
                           CompartilhadaRepository compartilhadaRepository,
                           FuncaoTransacaoRepository funcaoTransacaoRepository,
                           ElasticsearchTemplate elasticsearchTemplate,
                           AnaliseService analiseService) {
        this.analiseRepository = analiseRepository;
        this.analiseSearchRepository = analiseSearchRepository;
        this.funcaoDadosVersionavelRepository = funcaoDadosVersionavelRepository;
        this.dynamicExportsService = dynamicExportsService;
        this.userRepository = userRepository;
        this.compartilhadaRepository = compartilhadaRepository;
        this.funcaoDadosRepository = funcaoDadosRepository;
        this.funcaoTransacaoRepository = funcaoTransacaoRepository;
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.analiseService = analiseService;
    }

    @PostMapping("/analises")
    @Timed
    @Secured({AuthoritiesConstants.ADMIN, AuthoritiesConstants.USER, AuthoritiesConstants.GESTOR, AuthoritiesConstants.ANALISTA})
    public ResponseEntity<Analise> createAnalise(@Valid @RequestBody Analise analise) throws URISyntaxException {
        if (analise.getId() != null) {
            return ResponseEntity.badRequest().headers(
                    HeaderUtil.createFailureAlert(ENTITY_NAME, "idexists", "A new analise cannot already have an ID")).body(null);
        }
        analise.setCreatedBy(userRepository.findOneByLogin(SecurityUtils.getCurrentUserLogin()).get());
        salvaNovaData(analise);
        linkFuncoesToAnalise(analise);
        analiseRepository.save(analise);
        analiseSearchRepository.save(analise);
        return ResponseEntity.created(new URI("/api/analises/" + analise.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, analise.getId().toString())).body(analise);
    }

    @PutMapping("/analises")
    @Timed
    @Secured({AuthoritiesConstants.ADMIN, AuthoritiesConstants.USER, AuthoritiesConstants.GESTOR, AuthoritiesConstants.ANALISTA})
    public ResponseEntity<Analise> updateAnalise(@Valid @RequestBody Analise analise) throws URISyntaxException {
        if (analise.getId() == null) {
            return createAnalise(analise);
        }
        if (analise.isBloqueiaAnalise()) {
            return ResponseEntity.badRequest().headers(
                    HeaderUtil.createFailureAlert(ENTITY_NAME, "analiseblocked", "You cannot edit an blocked analise")).body(null);
        }
        analise.setEditedBy(analiseRepository.findOne(analise.getId()).getCreatedBy());
        salvaNovaData(analise);
        linkFuncoesToAnalise(analise);
        analiseRepository.save(analise);
        analiseSearchRepository.save(analise);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, analise.getId().toString()))
                .body(analise);
    }

    @PutMapping("/analises/{id}/block")
    @Timed
    @Secured({AuthoritiesConstants.ADMIN, AuthoritiesConstants.USER, AuthoritiesConstants.GESTOR, AuthoritiesConstants.ANALISTA})
    public ResponseEntity<Analise> blockUnblockAnalise(@PathVariable Long id) throws URISyntaxException {
        log.debug("REST request to block Analise : {}", id);

        Analise analise = recuperarAnalise(id);

        if (analise != null) {
            linkFuncoesToAnalise(analise);
            if (analise.isBloqueiaAnalise()) {
                analise.setBloqueiaAnalise(false);
            } else {
                analise.setBloqueiaAnalise(true);
            }
            unlinkAnaliseFromFuncoes(analise);
            Analise result = analiseRepository.save(analise);
            analiseSearchRepository.save(result);
            return ResponseEntity.ok().headers(HeaderUtil.blockEntityUpdateAlert(ENTITY_NAME, analise.getId().toString()))
                    .body(result);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new Analise());
        }
    }

    @PostMapping("/analises/clonar/{id}")
    @Timed
    @Secured({AuthoritiesConstants.ADMIN, AuthoritiesConstants.USER, AuthoritiesConstants.GESTOR, AuthoritiesConstants.ANALISTA})
    public ResponseEntity<Analise> cloneAnalise(@PathVariable Long id) {
        Analise analise = recuperarAnalise(id);

        if (analise != null) {
            Analise analiseCopia = new Analise(analise.getIdentificadorAnalise(),
                    analise.getPfTotal(), analise.getAdjustPFTotal(), analise.getSistema(),
                    analise.getOrganizacao(), analise.getBaselineImediatamente(), analise.getEquipeResponsavel(), analise.getManual());

            Analise analiseCopiaSalva = analiseRepository.save(analiseCopia);
            analiseSearchRepository.save(analiseCopiaSalva);

            Analise analiseRetorno = unlinkAnaliseFDFT(analise);
            return ResponseEntity.ok().headers(HeaderUtil.blockEntityUpdateAlert(ENTITY_NAME, analiseRetorno.getId().toString()))
                    .body(analiseRetorno);
        } else {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(new Analise());
        }


    }

    @GetMapping("/analises/{id}")
    @Timed
    public ResponseEntity<Analise> getAnalise(@PathVariable Long id) {
        Analise analise = recuperarAnalise(id);
        if (analise != null) {
            return ResponseUtil.wrapOrNotFound(Optional.ofNullable(analise));
        } else {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(null);
        }

    }

    @GetMapping("/analises/baseline")
    @Timed
    public List<Analise> getAllAnalisesBaseline() {
        return analiseRepository.findAllByBaseline();
    }

    @DeleteMapping("/analises/{id}")
    @Timed
    @Secured({AuthoritiesConstants.ADMIN, AuthoritiesConstants.USER, AuthoritiesConstants.GESTOR, AuthoritiesConstants.ANALISTA})
    public ResponseEntity<Void> deleteAnalise(@PathVariable Long id) {

        Analise analise = recuperarAnalise(id);

        if (analise != null) {
            analiseRepository.delete(id);
            analiseSearchRepository.delete(id);
        } else {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(null);
        }


        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

    @GetMapping("/compartilhada/{idAnalise}")
    @Timed
    @Secured({AuthoritiesConstants.ADMIN, AuthoritiesConstants.USER, AuthoritiesConstants.GESTOR, AuthoritiesConstants.ANALISTA})
    public List<Compartilhada> getAllCompartilhadaByAnalise(@PathVariable Long idAnalise) {
        return compartilhadaRepository.findAllByAnaliseId(idAnalise);
    }

    @PostMapping("/analises/compartilhar")
    @Timed
    @Secured({AuthoritiesConstants.ADMIN, AuthoritiesConstants.USER, AuthoritiesConstants.GESTOR, AuthoritiesConstants.ANALISTA})
    public ResponseEntity<List<Compartilhada>> popularCompartilhar(@Valid @RequestBody List<Compartilhada> compartilhadaList) throws URISyntaxException {
        List<Compartilhada> result = compartilhadaRepository.save(compartilhadaList);
        return ResponseEntity.created(new URI("/api/analises/compartilhar"))
                .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, "created")).body(result);
    }

    @DeleteMapping("/analises/compartilhar/delete/{id}")
    @Timed
    @Secured({AuthoritiesConstants.ADMIN, AuthoritiesConstants.USER, AuthoritiesConstants.GESTOR, AuthoritiesConstants.ANALISTA})
    public ResponseEntity<Void> deleteCompartilharAnalise(@PathVariable Long id) {
        compartilhadaRepository.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

    @PutMapping("/analises/compartilhar/viewonly/{id}")
    @Timed
    @Secured({AuthoritiesConstants.ADMIN, AuthoritiesConstants.USER, AuthoritiesConstants.GESTOR, AuthoritiesConstants.ANALISTA})
    public ResponseEntity<Compartilhada> viewOnly(@Valid @RequestBody Compartilhada compartilhada) throws URISyntaxException {
        Compartilhada result = compartilhadaRepository.save(compartilhada);
        return ResponseEntity.ok().headers(HeaderUtil.blockEntityUpdateAlert(ENTITY_NAME, compartilhada.getId().toString()))
                .body(result);
    }


    @GetMapping("/relatorioPdfArquivo/{id}")
    @Timed
    public ResponseEntity<byte[]> downloadPdfArquivo(@PathVariable Long id) throws URISyntaxException, IOException, JRException {
        Analise analise = recuperarAnalise(id);
        relatorioAnaliseRest = new RelatorioAnaliseRest(this.response, this.request);
        return relatorioAnaliseRest.downloadPdfArquivo(analise, TipoRelatorio.ANALISE);
    }

    @GetMapping("/relatorioPdfBrowser/{id}")
    @Timed
    public @ResponseBody
    byte[] downloadPdfBrowser(@PathVariable Long id) throws URISyntaxException, IOException, JRException {
        Analise analise = recuperarAnalise(id);
        relatorioAnaliseRest = new RelatorioAnaliseRest(this.response, this.request);
        return relatorioAnaliseRest.downloadPdfBrowser(analise, TipoRelatorio.ANALISE);
    }

    @GetMapping("/downloadPdfDetalhadoBrowser/{id}")
    @Timed
    public @ResponseBody
    byte[] downloadPdfDetalhadoBrowser(@PathVariable Long id) throws URISyntaxException, IOException, JRException {
        Analise analise = recuperarAnalise(id);
        relatorioAnaliseRest = new RelatorioAnaliseRest(this.response, this.request);
        return relatorioAnaliseRest.downloadPdfBrowser(analise, TipoRelatorio.ANALISE_DETALHADA);
    }

    @GetMapping("/downloadRelatorioExcel/{id}")
    @Timed
    public @ResponseBody
    byte[] downloadRelatorioExcel(@PathVariable Long id) throws URISyntaxException, IOException, JRException {
        Analise analise = recuperarAnalise(id);
        relatorioAnaliseRest = new RelatorioAnaliseRest(this.response, this.request);
        return relatorioAnaliseRest.downloadExcel(analise);
    }

    @GetMapping("/relatorioContagemPdf/{id}")
    @Timed
    public @ResponseBody
    ResponseEntity<InputStreamResource> gerarRelatorioContagemPdf(@PathVariable Long id) throws IOException, JRException {
        Analise analise = recuperarAnaliseContagem(id);
        relatorioAnaliseRest = new RelatorioAnaliseRest(this.response, this.request);
        return relatorioAnaliseRest.downloadReportContagem(analise);
    }

    @GetMapping(value = "/analise/exportacao/{tipoRelatorio}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Timed
    public ResponseEntity<InputStreamResource> gerarRelatorioExportacao(@PathVariable String tipoRelatorio, @RequestParam(defaultValue = "*") String query) throws RelatorioException {
        ByteArrayOutputStream byteArrayOutputStream;
        try {
            new NativeSearchQueryBuilder().withQuery(multiMatchQuery(query)).build();
            Page<Analise> result = analiseSearchRepository.search(queryStringQuery(query), dynamicExportsService.obterPageableMaximoExportacao());
            byteArrayOutputStream = dynamicExportsService.export(new RelatorioAnaliseColunas(), result, tipoRelatorio, Optional.empty(), Optional.ofNullable(AbacoUtil.REPORT_LOGO_PATH), Optional.ofNullable(AbacoUtil.getReportFooter()));
        } catch (DRException | ClassNotFoundException | JRException | NoClassDefFoundError e) {
            log.error(e.getMessage(), e);
            throw new RelatorioException(e);
        }
        return DynamicExporter.output(byteArrayOutputStream,
                "relatorio." + tipoRelatorio);
    }

    @GetMapping("/analises")
    @Timed
    public ResponseEntity<List<Analise>> getAllAnalisesEquipes(@RequestParam(defaultValue = "ASC") String order,
                                                               @RequestParam(defaultValue = "0", name = PAGE) int pageNumber,
                                                               @RequestParam(defaultValue = "20") int size,
                                                               @RequestParam(defaultValue = "id") String sort,
                                                               @RequestParam(value = "identificador", required = false) String identificador,
                                                               @RequestParam(value = "sistema", required = false) String sistema,
                                                               @RequestParam(value = "metodo", required = false) String metodo,
                                                               @RequestParam(value = "organizacao", required = false) String organizacao,
                                                               @RequestParam(value = "equipe", required = false) String equipe,
                                                               @RequestParam(value = "usuario", required = false) String usuario)
            throws URISyntaxException {
        Direction sortOrder = PageUtils.getSortDirection(order);
        Pageable pageable = new PageRequest(pageNumber, size, sortOrder, sort);
        Set<Long> equipesIds = getIdEquipes();
        BoolQueryBuilder qb = QueryBuilders.boolQuery();
        analiseService.bindFilterSearch(identificador, sistema, metodo, organizacao, equipe, usuario, equipesIds, qb);
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
            .withQuery(qb)
            .withFields("id", "organizacao.nome", "identificadorAnalise", "equipeResponsavel.nome", "sistema.nome", "metodoContagem", "pfTotal", "adjustPFTotal", "dataCriacaoOrdemServico", "bloqueiaAnalise", "users.firstName")
            .withPageable(pageable).build();
        Page<Analise> page = elasticsearchTemplate.queryForPage(searchQuery, Analise.class,
            new DefaultResultMapper(new DefaultEntityMapper() {
                private ObjectMapper mapper = new ObjectMapper();
                @Override
                public <T> T mapToObject(String source, Class<T> clazz) throws IOException {
                    Analise retorno = (Analise) super.mapToObject(source, clazz);
                    final ObjectNode node = mapper.readValue(source, ObjectNode.class);

                    JsonNode user = node.get("users.firstName");
                    Set<User> users = new HashSet<>();
                    if (user.isArray()) {
                        for (Object userName : mapper.convertValue(users, ArrayList.class)) {
                            users.add(User.builder()
                                .firstName(userName.toString())
                                .authorities(Collections.emptySet())
                                .tipoEquipes(Collections.emptySet())
                                .analises(Collections.emptySet())
                                .organizacoes(Collections.emptySet())
                                .build());
                        }
                    } else {
                        users.add(User.builder()
                            .firstName(user.textValue())
                            .authorities(Collections.emptySet())
                            .tipoEquipes(Collections.emptySet())
                            .analises(Collections.emptySet())
                            .organizacoes(Collections.emptySet())
                            .build());
                    }

                    retorno.setUsers(users);
                    retorno.setSistema(Sistema.builder()
                        .nome(node.get("sistema.nome")
                        .textValue()).build());
                    retorno.setEquipeResponsavel(TipoEquipe.builder()
                        .nome(node.get("equipeResponsavel.nome")
                        .textValue()).build());
                    retorno.setOrganizacao(Organizacao.builder()
                        .nome(node.get("organizacao.nome")
                        .textValue()).build());
                    return (T) retorno;
                }
            }));
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/analises/");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }


    private Set<Long> getIdEquipes() {
        User user = userSearchRepository.findByLogin(SecurityUtils.getCurrentUserLogin());
        Set<TipoEquipe> listaEquipes = user.getTipoEquipes();
        Set<Long> equipesIds = new HashSet<>();
        listaEquipes.forEach(tipoEquipe -> {
            equipesIds.add(tipoEquipe.getId());
        });
        return equipesIds;
    }

    private Boolean checarPermissao(Long idAnalise) {
        Optional<User> logged = userRepository.findOneWithAuthoritiesByLogin(SecurityUtils.getCurrentUserLogin());
        List<BigInteger> equipesIds = userRepository.findUserEquipes(logged.get().getId());
        List<Long> convertidos = equipesIds.stream().map(bigInteger -> bigInteger.longValue()).collect(Collectors.toList());
        Integer analiseDaEquipe = analiseRepository.analiseEquipe(idAnalise, convertidos);
        if (analiseDaEquipe.intValue() == 0) {
            return verificaCompartilhada(idAnalise);
        } else {
            return true;
        }

    }

    private Boolean verificaCompartilhada(Long idAnalise) {
        return compartilhadaRepository.existsByAnaliseId(idAnalise);
    }

    private Analise recuperarAnalise(Long id) {

        boolean retorno = checarPermissao(id);

        if (retorno) {
            return analiseRepository.findById(id);
        } else {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public Analise recuperarAnaliseContagem(@NotNull Long id) {

        boolean retorno = checarPermissao(id);

        if (retorno) {
            Analise analise = analiseRepository.reportContagem(id);
            Sistema sistema = analise.getSistema();
            if (sistema != null) {
                sistema.getModulos().forEach(modulo -> {
                    modulo.getFuncionalidades().forEach(funcionalidade -> {
                        funcionalidade.setFuncoesDados(funcaoDadosRepository.findByAnaliseFuncionalidade(id, funcionalidade.getId()));
                        funcionalidade.setFuncoesTransacao(funcaoTransacaoRepository.findByAnaliseFuncionalidade(id, funcionalidade.getId()));
                    });
                });
                return analise;
            }
            return null;
        } else {
            return null;
        }
    }

    private void linkFuncoesToAnalise(Analise analise) {
        linkAnaliseToFuncaoDados(analise);
        linkAnaliseToFuncaoTransacaos(analise);
    }

    private void linkAnaliseToFuncaoDados(Analise analise) {
        Optional.ofNullable(analise.getFuncaoDados()).orElse(Collections.emptySet())
                .forEach(funcaoDados -> {
                    funcaoDados.setAnalise(analise);
                    linkFuncaoDadosRelationships(funcaoDados);
                    handleVersionFuncaoDados(funcaoDados, analise.getSistema());
                });
    }

    private void linkFuncaoDadosRelationships(FuncaoDados funcaoDados) {
        Optional.ofNullable(funcaoDados.getFiles()).orElse(Collections.emptyList())
                .forEach(file -> file.setFuncaoDados(funcaoDados));
        Optional.ofNullable(funcaoDados.getDers()).orElse(Collections.emptySet())
                .forEach(der -> der.setFuncaoDados(funcaoDados));
        Optional.ofNullable(funcaoDados.getRlrs()).orElse(Collections.emptySet())
                .forEach(rlr -> rlr.setFuncaoDados(funcaoDados));
    }

    private void handleVersionFuncaoDados(FuncaoDados funcaoDados, Sistema sistema) {
        String nome = funcaoDados.getName();
        Optional<FuncaoDadosVersionavel> funcaoDadosVersionavel =
                funcaoDadosVersionavelRepository.findOneByNomeIgnoreCaseAndSistemaId(nome, sistema.getId());
        if (funcaoDadosVersionavel.isPresent()) {
            funcaoDados.setFuncaoDadosVersionavel(funcaoDadosVersionavel.get());
        } else {
            FuncaoDadosVersionavel novaFDVersionavel = new FuncaoDadosVersionavel();
            novaFDVersionavel.setNome(funcaoDados.getName());
            novaFDVersionavel.setSistema(sistema);
            FuncaoDadosVersionavel result = funcaoDadosVersionavelRepository.save(novaFDVersionavel);
            funcaoDados.setFuncaoDadosVersionavel(result);
        }
    }

    private void linkAnaliseToFuncaoTransacaos(Analise analise) {
        Optional.ofNullable(analise.getFuncaoTransacaos()).orElse(Collections.emptySet())
                .forEach(funcaoTransacao -> {
                    funcaoTransacao.setAnalise(analise);
                    Optional.ofNullable(funcaoTransacao.getFiles()).orElse(Collections.emptyList())
                            .forEach(file -> file.setFuncaoTransacao(funcaoTransacao));
                    Optional.ofNullable(funcaoTransacao.getDers()).orElse(Collections.emptySet())
                            .forEach(der -> der.setFuncaoTransacao(funcaoTransacao));
                    Optional.ofNullable(funcaoTransacao.getAlrs()).orElse(Collections.emptySet())
                            .forEach(alr -> alr.setFuncaoTransacao(funcaoTransacao));
                });
    }

    private void unlinkAnaliseFromFuncoes(Analise result) {
        Optional.ofNullable(result.getFuncaoDados()).orElse(Collections.emptySet())
                .forEach(entry -> {
                    entry.setAnalise(null);
                });
        Optional.ofNullable(result.getFuncaoTransacaos()).orElse(Collections.emptySet())
                .forEach(entry -> {
                    entry.setAnalise(null);
                });
    }


    private Analise unlinkAnaliseFDFT(Analise result) {
        Optional.ofNullable(result.getFuncaoTransacaos()).orElse(Collections.emptySet())
                .forEach(ft -> {
                    ft.setAnalise(null);
                    Optional.ofNullable(ft.getAlrs()).orElse(Collections.emptySet())
                            .forEach(rlr -> rlr.setId(null));
                    Optional.ofNullable(ft.getDers()).orElse(Collections.emptySet())
                            .forEach(ders -> ders.setId(null));
                });
        Analise analiseCopiaSalva = analiseRepository.save(result);
        analiseSearchRepository.save(result);
        return analiseCopiaSalva;
    }

    private void salvaNovaData(Analise analise) {
        if (analise.getDataHomologacao() != null) {
            Timestamp dataDeHoje = new Timestamp(System.currentTimeMillis());
            Timestamp dataParam = analise.getDataHomologacao();
            dataParam.setHours(dataDeHoje.getHours());
            dataParam.setMinutes(dataDeHoje.getMinutes());
            dataParam.setSeconds(dataDeHoje.getSeconds());
        }
    }
}


