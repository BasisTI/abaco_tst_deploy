package br.com.basis.abaco.web.rest;

import br.com.basis.abaco.domain.Organizacao;
import br.com.basis.abaco.repository.OrganizacaoRepository;
import br.com.basis.abaco.repository.UploadedFilesRepository;
import br.com.basis.abaco.repository.search.OrganizacaoSearchRepository;
import br.com.basis.abaco.service.OrganizacaoService;
import br.com.basis.abaco.service.PerfilService;
import br.com.basis.abaco.service.UserService;
import br.com.basis.abaco.service.dto.DropdownDTO;
import br.com.basis.abaco.service.dto.OrganizacaoDropdownDTO;
import br.com.basis.abaco.service.dto.filter.SearchFilterDTO;
import br.com.basis.abaco.service.exception.RelatorioException;
import br.com.basis.abaco.service.relatorio.RelatorioOrganizacaoColunas;
import br.com.basis.abaco.utils.AbacoUtil;
import br.com.basis.abaco.utils.PageUtils;
import br.com.basis.abaco.web.rest.util.HeaderUtil;
import br.com.basis.abaco.web.rest.util.PaginationUtil;
import br.com.basis.dynamicexports.service.DynamicExportsService;
import br.com.basis.dynamicexports.util.DynamicExporter;
import com.codahale.metrics.annotation.Timed;
import io.github.jhipster.web.util.ResponseUtil;
import net.sf.dynamicreports.report.exception.DRException;
import net.sf.jasperreports.engine.JRException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

import javax.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.elasticsearch.index.query.QueryBuilders.multiMatchQuery;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;

/**
 * REST controller for managing Organizacao.
 */
@RestController
@RequestMapping("/api")
public class OrganizacaoResource {

  private final Logger log = LoggerFactory.getLogger(OrganizacaoResource.class);

  private static final String ENTITY_NAME = "organizacao";

  private final OrganizacaoRepository organizacaoRepository;

  private final OrganizacaoSearchRepository organizacaoSearchRepository;

  private final UploadedFilesRepository filesRepository;

  private String[] erro = { "orgNomeInvalido", "orgCnpjInvalido", "orgSiglaInvalido", "orgNumOcorInvalido",
      "organizacaoexists", "cnpjexists" };
  private String[] mensagem = { "Nome de organiza????o inv??lido", "CNPJ de organiza????o inv??lido",
      "Sigla de organiza????o inv??lido", "Numero da Ocorr??ncia de organiza????o inv??lido",
      "Organizacao already in use", "CNPJ already in use" };

  private final DynamicExportsService dynamicExportsService;

    private final OrganizacaoService organizacaoService;

    private final PerfilService perfilService;

    public OrganizacaoResource(OrganizacaoRepository organizacaoRepository,
                               OrganizacaoSearchRepository organizacaoSearchRepository, UploadedFilesRepository filesRepository, DynamicExportsService dynamicExportsService,
                               OrganizacaoService organizacaoService, PerfilService perfilService) {
        this.organizacaoRepository = organizacaoRepository;
        this.organizacaoSearchRepository = organizacaoSearchRepository;
        this.filesRepository = filesRepository;
        this.dynamicExportsService = dynamicExportsService;
        this.organizacaoService = organizacaoService;
        this.perfilService = perfilService;
    }

  /**
   * Function to format a bad request URL to be returned to frontend
   *
   * @param errorKey       The key identifing the error occured
   * @param defaultMessage Default message to display to user
   * @return The bad request URL
   */
  private ResponseEntity<Organizacao> createBadRequest(String errorKey, String defaultMessage) {
    return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert(ENTITY_NAME, errorKey, defaultMessage))
        .body(null);
  }

  public boolean validaCampo(String campo) {
    final String regra = "^\\S+(\\s{1}\\S+)*$";
    Pattern padrao = Pattern.compile(regra);
    Matcher matcherCampo = padrao.matcher(campo);
    return matcherCampo.find();
  }

  private int validaCamposOrganizacao(Organizacao org) {
    String[] campos = { org.getNome(), org.getSigla(), org.getNumeroOcorrencia() };
    int i = 0;

    while (i < campos.length) {
      if (campos[i] != null && !validaCampo(campos[i])) {
        return i;
      }
      i++;
    }
    /* Verifing if there is an existing Organizacao with same name */
    Optional<Organizacao> existingOrganizacao = organizacaoSearchRepository.findOneByNome(org.getNome());
    if (existingOrganizacao.isPresent() && (!existingOrganizacao.get().getId().equals(org.getId()))) {
      return 4;
    }
    if(org.getCnpj() != null){
        existingOrganizacao = organizacaoSearchRepository.findOneByCnpj(org.getCnpj());

    }
    if (org.getCnpj() != null && existingOrganizacao.isPresent()
        && (!existingOrganizacao.get().getId().equals(org.getId()))) {
      return 5;
    }
    return -1;
  }

  /**
   * POST /organizacaos : Create a new organizacao.
   *
   * @param organizacao the organizacao to create
   * @return the ResponseEntity with status 201 (Created) and with body the new
   *         organizacao, or with status 400 (Bad Request) if the organizacao has
   *         already an ID
   * @throws URISyntaxException if the Location URI syntax is incorrect
   */
  @PostMapping("/organizacaos")
  @Timed
  @Secured("ROLE_ABACO_ORGANIZACAO_CADASTRAR")
  public ResponseEntity<Organizacao> createOrganizacao(@Valid @RequestBody Organizacao organizacao)
      throws URISyntaxException {
    int i;
    log.debug("REST request to save Organizacao : {}", organizacao);
    if (organizacao.getId() != null) {
      return this.createBadRequest("idoexists", "A new organizacao cannot already have an ID");
    }

    i = validaCamposOrganizacao(organizacao);
    if (i >= 0) {
      return this.createBadRequest(this.erro[i], this.mensagem[i]);
    }
    Organizacao result = organizacaoRepository.save(organizacao);
    organizacaoSearchRepository.save(result);

    return ResponseEntity.created(new URI("/api/organizacaos/" + result.getId()))
        .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString())).body(result);
  }

  /**
   * PUT /organizacaos : Updates an existing organizacao.
   *
   * @param organizacao the organizacao to update
   * @return the ResponseEntity with status 200 (OK) and with body the updated
   *         organizacao, or with status 400 (Bad Request) if the organizacao is
   *         not valid, or with status 500 (Internal Server Error) if the
   *         organizacao couldnt be updated
   * @throws URISyntaxException if the Location URI syntax is incorrect
   */
  @PutMapping("/organizacaos")
  @Timed
  @Secured("ROLE_ABACO_ORGANIZACAO_EDITAR")
  public ResponseEntity<Organizacao> updateOrganizacao(@Valid @RequestBody Organizacao organizacao)
      throws URISyntaxException {
    int i;
    log.debug("REST request to update Organizacao : {}", organizacao);
    if (organizacao.getId() == null) {
      return createOrganizacao(organizacao);
    }

    i = validaCamposOrganizacao(organizacao);
    if (i >= 0) {
      return this.createBadRequest(this.erro[i], this.mensagem[i]);
    }

    Organizacao result = organizacaoRepository.saveAndFlush(organizacao);
    organizacaoSearchRepository.save(result);

    return ResponseEntity.ok()
        .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, organizacao.getId().toString())).body(result);
  }

    @GetMapping("/organizacaos/drop-down")
    @Timed
    public List<OrganizacaoDropdownDTO> getOrganizacaoDropdown() {
        log.debug("REST request to get dropdown Organizacaos");
        return organizacaoService.getOrganizacaoDropdown();
    }

    @GetMapping("/organizacaos/drop-down/active")
    @Timed
    public List<OrganizacaoDropdownDTO> getOrganizacaoDropdownAtivas() {
        log.debug("REST request to get dropdown Organizacaos");
        return organizacaoService.getOrganizacaoDropdownAtivo();
    }

  @GetMapping("/organizacaos/ativas")
  @Timed
  public List<Organizacao> searchActiveOrganizations() {
    log.debug("REST request to get all Organizacaos");
    return organizacaoRepository.searchActiveOrganizations();
  }

  /**
   * GET /organizacaos/:id : get the "id" organizacao.
   *
   * @param id the id of the organizacao to retrieve
   * @return the ResponseEntity with status 200 (OK) and with body the
   *         organizacao, or with status 404 (Not Found)
   */
  @GetMapping("/organizacaos/{id}")
  @Timed
  @Secured({"ROLE_ABACO_ORGANIZACAO_CONSULTAR", "ROLE_ABACO_ORGANIZACAO_EDITAR"})
  public ResponseEntity<Organizacao> getOrganizacao(@PathVariable Long id) {
    log.debug("REST request to get Organizacao : {}", id);
    Organizacao organizacao = organizacaoRepository.findOne(id);
    return ResponseUtil.wrapOrNotFound(Optional.ofNullable(organizacao));
  }

  /**
   * DELETE /organizacaos/:id : delete the "id" organizacao.
   *
   * @param id the id of the organizacao to delete
   * @return the ResponseEntity with status 200 (OK)
   */
  @DeleteMapping("/organizacaos/{id}")
  @Timed
  @Secured("ROLE_ABACO_ORGANIZACAO_EXCLUIR")
  public ResponseEntity<Void> deleteOrganizacao(@PathVariable Long id) {
      log.debug("REST request to delete Organizacao : {}", id);
      organizacaoRepository.delete(id);
      organizacaoSearchRepository.delete(id);
      Organizacao organizacao = organizacaoRepository.findOne(id);
      if(organizacao.getLogoId() != null && filesRepository.exists(organizacao.getLogoId())){
          filesRepository.delete(organizacao.getLogoId());
      }
      return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
  }

  /**
   * SEARCH /_search/organizacaos?query=:query : search for the organizacao
   * corresponding to the query.
   *
   * @param query the query of the organizacao search
   * @return the result of the search
   * @throws URISyntaxException
   */
  @GetMapping("/_search/organizacaos")
  @Timed
  @Secured({"ROLE_ABACO_ORGANIZACAO_PESQUISAR", "ROLE_ABACO_ORGANIZACAO_ACESSAR"})
  public ResponseEntity<List<Organizacao>> searchOrganizacaos(@RequestParam(defaultValue = "*") String query,
                                                              @RequestParam(defaultValue = "ASC") String order, @RequestParam(name = "page") int pageNumber, @RequestParam int size,
                                                              @RequestParam(defaultValue = "id", required = false) String sort) throws URISyntaxException {
      log.debug("REST request to search Organizacaos for query {}", query);

      Sort.Direction sortOrder = PageUtils.getSortDirection(order);
      Pageable newPageable = new PageRequest(pageNumber, size, sortOrder, sort);

      Page<Organizacao> page = organizacaoSearchRepository.search(queryStringQuery(query), dynamicExportsService.obterPageableMaximoExportacao());
      Page<Organizacao> pageNew = perfilService.validarPerfilOrganizacoes(page, newPageable);

      HttpHeaders headers = PaginationUtil.generateSearchPaginationHttpHeaders(query, pageNew,
          "/api/_search/organizacaos");
    return new ResponseEntity<>(pageNew.getContent(), headers, HttpStatus.OK);
  }

    @GetMapping("/organizacaos/active")
    public List<Organizacao> getAllOrganizationsActive() {
        return this.organizacaoRepository.findByAtivoTrue();
    }

    @GetMapping("/organizacaos/active-user")
    @Timed
    public List<DropdownDTO> findActiveUserOrganizations() {

        return organizacaoService.findActiveUserOrganizations();
    }

    @PostMapping(value = "/organizacaos/exportacao/{tipoRelatorio}", produces = MediaType.APPLICATION_PDF_VALUE)
    @Timed
    @Secured("ROLE_ABACO_ORGANIZACAO_EXPORTAR")
    public ResponseEntity<InputStreamResource> gerarRelatorio(@PathVariable String tipoRelatorio, @RequestBody SearchFilterDTO filter) throws RelatorioException {
        ByteArrayOutputStream byteArrayOutputStream = getByteArrayOutputStream("pdf", filter);
        return DynamicExporter.output(byteArrayOutputStream, "relatorio");
    }

    private ByteArrayOutputStream getByteArrayOutputStream(String tipoRelatorio, SearchFilterDTO filter)
        throws RelatorioException {
        ByteArrayOutputStream byteArrayOutputStream;
        String query = "*";
        if (filter.getNome() != null) {
            query = "*" + filter.getNome().toUpperCase() + "*";
        }
        try {
            new NativeSearchQueryBuilder().withQuery(multiMatchQuery(query)).build();
            Page<Organizacao> result = organizacaoSearchRepository.search(queryStringQuery(query),
                dynamicExportsService.obterPageableMaximoExportacao());

            byteArrayOutputStream = dynamicExportsService.export(new RelatorioOrganizacaoColunas(filter.getColumnsVisible()), result,
                tipoRelatorio, Optional.empty(), Optional.ofNullable(AbacoUtil.REPORT_LOGO_PATH),
                Optional.ofNullable(AbacoUtil.getReportFooter()));
        } catch (DRException | ClassNotFoundException | JRException | NoClassDefFoundError e) {
            log.error(e.getMessage(), e);
            throw new RelatorioException(e);
        }
        return byteArrayOutputStream;
    }

    @PostMapping(value = "/organizacaos/exportacao-arquivo", produces = MediaType.APPLICATION_PDF_VALUE)
    @Timed
    @Secured("ROLE_ABACO_ORGANIZACAO_EXPORTAR")
    public ResponseEntity<byte[]> gerarRelatorioImprimir(@RequestBody SearchFilterDTO filter)
        throws RelatorioException {
        ByteArrayOutputStream byteArrayOutputStream = getByteArrayOutputStream("pdf", filter);
        return new ResponseEntity<byte[]>(byteArrayOutputStream.toByteArray(), HttpStatus.OK);
    }
}
