import {EntityToJSON} from './../shared/entity-to-json';
import {Component, OnInit, ChangeDetectorRef, OnDestroy, Input, Output, EventEmitter} from '@angular/core';
import {FuncaoDados} from './funcao-dados.model';
import {FatorAjuste} from '../fator-ajuste';
import {FuncaoAnalise} from './../analise-shared/funcao-analise';
import {BaselineAnalitico} from './../baseline/baseline-analitico.model';
import {BaselineService} from './../baseline/baseline.service';
import {AnaliseSharedDataService, PageNotificationService, ResponseWrapper} from '../shared';
import {Analise, AnaliseService} from '../analise';

import * as _ from 'lodash';
import {Funcionalidade} from '../funcionalidade/index';
import {SelectItem} from 'primeng/primeng';
import {BlockUI, NgBlockUI} from 'ng-block-ui';
import {Calculadora} from '../analise-shared/calculadora';
import {DatatableClickEvent} from '@basis/angular-components';
import {ConfirmationService} from 'primeng/primeng';
import {ResumoFuncoes} from '../analise-shared/resumo-funcoes';
import {AfterViewInit, AfterContentInit} from '@angular/core/src/metadata/lifecycle_hooks';
import {Subscription} from 'rxjs/Subscription';

import {FatorAjusteLabelGenerator} from '../shared/fator-ajuste-label-generator';
import {DerChipItem} from '../analise-shared/der-chips/der-chip-item';
import {DerChipConverter} from '../analise-shared/der-chips/der-chip-converter';
import {AnaliseReferenciavel} from '../analise-shared/analise-referenciavel';
import {FuncaoDadosService} from './funcao-dados.service';
import {AnaliseSharedUtils} from '../analise-shared/analise-shared-utils';
import {Manual} from '../manual';
import {Modulo} from '../modulo';
import {DerTextParser, ParseResult} from '../analise-shared/der-text/der-text-parser';
import {forEach} from '../../../node_modules/@angular/router/src/utils/collection';
import {Impacto} from '../analise-shared/impacto-enum';

import { FuncaoTransacao, TipoFuncaoTransacao } from './../funcao-transacao/funcao-transacao.model';
import { CalculadoraTransacao } from './../analise-shared/calculadora-transacao';
import { fcall } from 'q';

@Component({
    selector: 'app-analise-funcao-dados',
    templateUrl: './funcao-dados-form.component.html'
})
export class FuncaoDadosFormComponent implements OnInit, OnDestroy {

    @Output()
    valueChange: EventEmitter<string> = new EventEmitter<string>();
    parseResult: ParseResult;
    text: string;
    @Input()
    label: string;

    faS: FatorAjuste[];

    textHeader: string;
    @Input() isView: boolean;
    @BlockUI() blockUI: NgBlockUI;      // Usado para bloquear o sistema enquanto aguarda resolução das requisições do backend
    isEdit: boolean;
    nomeInvalido;
    isSaving: boolean;
    listaFD: string[];
    classInvalida;
    impactoInvalido: boolean;
    hideElementTDTR: boolean;
    hideShowQuantidade: boolean;
    showDialog = false;
    showMultiplos = false;
    sugestoesAutoComplete: string[] = [];
    impactos: string[];

    windowHeightDialog: any;
    windowWidthDialog: any;

    moduloCache: Funcionalidade;
    dersChips: DerChipItem[] = [];
    rlrsChips: DerChipItem[] = [];
    resumo: ResumoFuncoes;
    fatoresAjuste: SelectItem[] = [];
    colunasOptions: SelectItem[];
    colunasAMostrar = [];
     dadosBaselineFD: BaselineAnalitico[] = [];
    results: string[];
    baselineResults: any[] = [];
    funcoesDadosList: FuncaoDados[] = [];

    impacto: SelectItem[] = [
        {label: 'Inclusão', value: 'INCLUSAO'},
        {label: 'Alteração', value: 'ALTERACAO'},
        {label: 'Exclusão', value: 'EXCLUSAO'},
        {label: 'Conversão', value: 'CONVERSAO'},
        {label: 'Outros', value: 'ITENS_NAO_MENSURAVEIS'}
    ];

    classificacoes: SelectItem[] = [
        {label: 'ALI - Arquivo Lógico Interno', value: 'ALI'},
        {label: 'AIE - Arquivo de Interface Externa', value: 'AIE'}
    ];

    private fatorAjusteNenhumSelectItem = {label: 'Nenhum', value: undefined};
    private analiseCarregadaSubscription: Subscription;
    private subscriptionSistemaSelecionado: Subscription;
    private nomeDasFuncoesDoSistema: string[] = [];
    public erroModulo: boolean;
    public erroTR: boolean;
    public erroTD: boolean;
    public erroUnitario: boolean;
    public erroDeflator: boolean;

    constructor(
        private analiseSharedDataService: AnaliseSharedDataService,
        private confirmationService: ConfirmationService,
        private pageNotificationService: PageNotificationService,
        private changeDetectorRef: ChangeDetectorRef,
        private funcaoDadosService: FuncaoDadosService,
        private analiseService: AnaliseService,
        private baselineService: BaselineService
    ) {
        const colunas = [
            {header: 'Nome', field: 'name'},
            {header: 'Deflator'},
            {header: 'Impacto', field: 'impacto'},
            {header: 'Módulo'},
            {header: 'Funcionalidade'},
            {header: 'Classificação', field: 'tipo'},
            {header: 'DER (TD)'},
            {header: 'RLR (TR)'},
            {header: 'Complexidade', field: 'complexidade'},
            {header: 'PF - Total'},
            {header: 'PF - Ajustado'}
        ];

        this.colunasOptions = colunas.map((col, index) => {
            col['index'] = index;
            return {
                label: col.header,
                value: col,
            };
        });
    }

    ngOnInit() {
        this.estadoInicial();
        this.impactos = AnaliseSharedUtils.impactos; 
    }

    estadoInicial() {
        this.isSaving = false;
        this.hideShowQuantidade = true;
        this.currentFuncaoDados = new FuncaoDados();
        this.subscribeToAnaliseCarregada();
        this.colunasAMostrar = [];
        this.colunasOptions.map(selectItem => this.colunasAMostrar.push(selectItem.value));
    }
    updateNameImpacto(impacto: string) {
        switch(impacto) {
          case 'INCLUSAO':
            return 'INCLUSÃO';
          case 'ALTERACAO':
            return 'ALTERAÇÃO';
          case 'EXCLUSAO':
            return 'EXCLUSÃO';
          case 'CONVERSAO' :
            return 'CONVERSÃO';
          //break;
    
          }
      }

    public buttonSaveEdit() {

        if (this.isEdit) {
            this.editar();
        } else {
            if (this.showMultiplos) {
                let retorno = true;
                for (const nome of this.parseResult.textos) {
                    this.currentFuncaoDados.name = nome;
                    if (!this.multiplos()) {
                        retorno = false;
                        break;
                    }
                }
                if (retorno) {
                    this.analise.funcaoDados.concat(this.funcoesDadosList);
                    this.salvarAnalise();
                    this.subscribeToAnaliseCarregada();
                    this.fecharDialog();
                }
            } else {
                if (this.adicionar()) {
                    this.fecharDialog();
                }
            }
        }
        if (this.blockUI.isActive) {
            this.blockUI.stop();
        }
    }

    disableTRDER() {
        this.hideElementTDTR = this.analiseSharedDataService.analise.metodoContagem === 'INDICATIVA'
            || this.analiseSharedDataService.analise.metodoContagem === 'ESTIMADA';
    }

    private subscribeToAnaliseCarregada() {
        this.analiseCarregadaSubscription = this.analiseSharedDataService.getLoadSubject().subscribe(() => {
            this.atualizaResumo();
            //  this.loadDataFunctionsName();
        });
    }

    public carregarDadosBaseline() {
        this.baselineService.baselineAnaliticoFD(this.analise.sistema.id).subscribe((res: ResponseWrapper) => {
            this.dadosBaselineFD = res.json;
        });
    }

    private atualizaResumo() {
        this.resumo = this.analise.resumoFuncaoDados;
        this.changeDetectorRef.detectChanges();
    }

    private subscribeToSistemaSelecionado() {
        this.subscriptionSistemaSelecionado = this.analiseSharedDataService.getSistemaSelecionadoSubject()
            .subscribe(() => {
                this.loadDataFunctionsName();
            });
    }

    searchBaseline(event): void {
        this.baselineResults = this.dadosBaselineFD.filter(function (fc){
            var teste: string = event.query;
            return fc.name.toLowerCase().includes(teste.toLowerCase());
        });
    }

    // Carrega nome das funçeõs de dados
    private loadDataFunctionsName() {
        const sistemaId: number = this.analiseSharedDataService.analise.sistema.id;
        this.funcaoDadosService.findAllNamesBySistemaId(sistemaId).subscribe(
            nomes => {
                this.nomeDasFuncoesDoSistema = nomes;
                this.sugestoesAutoComplete = nomes.slice();

            });
    }

    autoCompleteNomes(event) {

        // TODO qual melhor método? inclues? startsWith ignore case?
        this.sugestoesAutoComplete = this.nomeDasFuncoesDoSistema

            .filter(nomeFuncao => nomeFuncao.startsWith(event.query));
    }

    getTextDialog() {
        this.textHeader = this.isEdit ? 'Alterar Função de Dados' : 'Adicionar Função de Dados';
    }

    get currentFuncaoDados(): FuncaoDados {
        return this.analiseSharedDataService.currentFuncaoDados;
    }

    set currentFuncaoDados(currentFuncaoDados: FuncaoDados) {
        this.analiseSharedDataService.currentFuncaoDados = currentFuncaoDados;
    }

    get funcoesDados(): FuncaoDados[] {
        if (!this.analise.funcaoDados) {
            return [];
        }
        return this.analise.funcaoDados;
    }

    private get analise(): Analise {
        return this.analiseSharedDataService.analise;
    }

    private get manual() {
        if (this.analiseSharedDataService.analise.contrato) {
            return this.analiseSharedDataService.analise.contrato.manualContrato[0].manual;
        }
        return undefined;
    }

    isContratoSelected(): boolean {
        const isContratoSelected = this.analiseSharedDataService.isContratoSelected();
        if (isContratoSelected) {
            if (this.fatoresAjuste.length === 0) {
                this.inicializaFatoresAjuste(this.manual);
            }
        }
        return isContratoSelected;
    }

    contratoSelecionado() {
        if (this.currentFuncaoDados.fatorAjuste.tipoAjuste === 'UNITARIO') {
            this.hideShowQuantidade = this.currentFuncaoDados.fatorAjuste === undefined;
        } else {
            this.currentFuncaoDados.quantidade = undefined;
            this.hideShowQuantidade = true;
            this.currentFuncaoDados.quantidade = undefined;
        }
    }

    fatoresAjusteDropdownPlaceholder() {
        if (this.isContratoSelected()) {
            return 'Selecione um Deflator';
        } else {
            return `Selecione um Contrato na aba 'Geral' para carregar os Deflatores`;
        }
    }

    // Funcionalidade Selecionada
    functionalitySelected(funcionalidade: Funcionalidade) {
        if (!funcionalidade) {
        } else {
            this.moduloCache = funcionalidade;
        }
        this.currentFuncaoDados.funcionalidade = funcionalidade;
    }

    multiplos(): boolean {
        const retorno: boolean = this.verifyDataRequire();
        if (!retorno) {
            this.pageNotificationService.addErrorMsg('Favor preencher o campo obrigatório!');
            return false;
        } else {
            this.desconverterChips();
            this.verificarModulo();
            const funcaoDadosCalculada = Calculadora.calcular(
                this.analise.metodoContagem, this.currentFuncaoDados, this.analise.contrato.manual);
            this.funcoesDadosList.push(funcaoDadosCalculada);
            this.analise.addFuncaoDados(funcaoDadosCalculada);
            this.atualizaResumo();
            this.resetarEstadoPosSalvar();
            return true;
        }
    }

    validarNameFuncaoTransacaos(nome: string) {
        const that = this;
        return new Promise( resolve => {
            if (that.analise.funcaoTransacaos.length === 0) {
                return resolve(true);
            }
            that.analise.funcaoTransacaos.forEach( (data, index) => {
                if (data.name === nome) {
                    return resolve(false);
                }
                if (!that.analise.funcaoTransacaos[index + 1]) {
                    return resolve(true);
                }
            });
        });
    }

    adicionar(): boolean {
        const retorno: boolean = this.verifyDataRequire();
        if (!retorno) {
            this.pageNotificationService.addErrorMsg('Favor preencher o campo obrigatório!');
            return retorno;
        } else {
            this.desconverterChips();
            this.verificarModulo();
            const funcaoDadosCalculada = Calculadora.calcular(this.analise.metodoContagem,
                                                              this.currentFuncaoDados,
                                                              this.analise.contrato.manual);
            this.validarNameFuncaoDados(this.currentFuncaoDados.name).then(resolve => {
                if (resolve) {
                    this.pageNotificationService.addCreateMsgWithName(funcaoDadosCalculada.name);
                    this.analise.addFuncaoDados(funcaoDadosCalculada);
                    this.atualizaResumo();
                    this.resetarEstadoPosSalvar();
                    this.salvarAnalise();
                    this.estadoInicial();
                } else {
                    this.pageNotificationService.addErrorMsg('Registro já cadastrado!');
                }
            });
        }
        return retorno;
    }

    /* Verificar esta promisse */
    validarNameFuncaoDados(nome: string) {
        const that = this;
        return new Promise(resolve => {
            if (that.analise.funcaoDados.length === 0) {
                return resolve(true);
            }
            that.analise.funcaoDados.forEach((data, index) => {
                if (data.name === nome) {
                    return resolve(false);
                }
                if (!that.analise.funcaoDados[index + 1]) {
                    return resolve(true);
                }
            });
        });
    }

    private verifyDataRequire(): boolean {
        let retorno = true;

        if (!this.currentFuncaoDados.name) {
            this.nomeInvalido = true;
            retorno = false;
        } else {
            this.nomeInvalido = false;
        }

        if(!this.currentFuncaoDados.tipo){
            this.classInvalida = true;
            retorno = false;
        } else {
            this.classInvalida = false;
        }

        if (!this.currentFuncaoDados.impacto) {
            this.impactoInvalido = true;
            retorno = false;
        } else {
            this.impactoInvalido = false;
        }

        if(this.currentFuncaoDados.impacto){
            if (this.currentFuncaoDados.impacto.indexOf('ITENS_NAO_MENSURAVEIS') === 0 && this.currentFuncaoDados.fatorAjuste === undefined) {
                this.erroDeflator = false;
                retorno = false;
                this.pageNotificationService.addErrorMsg('Selecione um Deflator');
            }
        }
        else {
            this.erroDeflator = true;
        }

        if (this.currentFuncaoDados.fatorAjuste) {
            if (this.currentFuncaoDados.fatorAjuste.tipoAjuste === 'UNITARIO' &&
                this.currentFuncaoDados.quantidade === undefined) {
                this.erroUnitario = true;
                retorno = false;
            } else {
                this.erroUnitario = false;
            }
        }

        if (this.analiseSharedDataService.analise.metodoContagem === 'DETALHADA') {

            if (!this.rlrsChips || this.rlrsChips.length < 1) {
                this.erroTR = true;
                retorno = false;
            } else {
                this.erroTR = false;
            }

            if (!this.dersChips || this.dersChips.length < 1) {
                this.erroTD = true;
                retorno = false;
            } else {
                this.erroTD = false;
            }
        }

        if (this.currentFuncaoDados.funcionalidade === undefined) {
            this.pageNotificationService.addErrorMsg('Selecione um Módulo e Submódulo');
            this.erroModulo = true;
            retorno = false;
        }
        else{
            this.erroModulo = false;
        }

        return retorno;
    }

    salvarAnalise() {
        this.analiseService.atualizaAnalise(this.analise);
    }

    private desconverterChips() {
        if (this.dersChips != null && this.rlrsChips != null) {
            this.currentFuncaoDados.ders = DerChipConverter.desconverterEmDers(this.dersChips);
            this.currentFuncaoDados.rlrs = DerChipConverter.desconverterEmRlrs(this.rlrsChips);
        }
    }

    private editar() {

        const retorno: boolean = this.verifyDataRequire();
        if (!retorno) {
            this.pageNotificationService.addErrorMsg('Favor preencher o campo obrigatório!');
            return;
        } else {
            this.desconverterChips();
            this.verificarModulo();
            const funcaoDadosCalculada = Calculadora.calcular(
                this.analise.metodoContagem, this.currentFuncaoDados, this.analise.contrato.manual);
            this.validarNameFuncaoDados(this.currentFuncaoDados.name).then(resolve => {
                this.pageNotificationService.addSuccessMsg(`Função de dados '${funcaoDadosCalculada.name}' alterada com sucesso`);
                this.analise.updateFuncaoDados(funcaoDadosCalculada);
                this.atualizaResumo();
                this.resetarEstadoPosSalvar();
                this.salvarAnalise();
                this.fecharDialog();
            });
        }
    }

    fecharDialog() {
        this.text = undefined;
        this.limparMensagensErros();
        this.showDialog = false;
        this.analiseSharedDataService.funcaoAnaliseDescarregada();
        this.currentFuncaoDados = new FuncaoDados();
        this.dersChips = [];
        this.rlrsChips = [];
        window.scrollTo(0, 60);
    }

    limparMensagensErros() {
        this.nomeInvalido = false;
        this.classInvalida = false;
        this.impactoInvalido = false;
        this.erroModulo = false;
        this.erroUnitario = false;
        this.erroTR = false;
        this.erroTD = false;
        this.erroDeflator = false;
    }

    private resetarEstadoPosSalvar() {
        this.currentFuncaoDados = this.currentFuncaoDados.clone();

        this.currentFuncaoDados.artificialId = undefined;
        this.currentFuncaoDados.id = undefined;

        if (this.dersChips !== undefined && this.rlrsChips) {
            this.dersChips.forEach(c => c.id = undefined);
            this.rlrsChips.forEach(c => c.id = undefined);
        }

    }

    public verificarModulo() {
        if (this.currentFuncaoDados.funcionalidade !== undefined) {
            return;
        }
        this.currentFuncaoDados.funcionalidade = this.moduloCache;
    }

    classValida() {
        this.classInvalida = false;
    }

    impactoValido() {
        this.impactoInvalido = false;
    }

    /**
     * Método responsável por recuperar o nome selecionado no combo.
     * @param nome
     */
    recuperarNomeSelecionado(baselineAnalitico: BaselineAnalitico) {

        this.funcaoDadosService.getFuncaoDadosBaseline(baselineAnalitico.idfuncaodados)
            .subscribe((res: FuncaoDados) => {
                if (res.fatorAjuste === null) {
                    res.fatorAjuste = undefined;
                }
                res.id = undefined;
                res.ders.forEach(Ders => {
                    Ders.id = undefined;
                });
                res.rlrs.forEach(rlrs => {
                    rlrs.id = undefined;
                });

                this.prepararParaEdicao(res);
            });

    }

    datatableClick(event: DatatableClickEvent) {
        if (!event.selection) {
            return;
        }

        const funcaoDadosSelecionada: FuncaoDados = event.selection.clone();
        switch (event.button) {
            case 'edit':
                this.isEdit = true;
                this.prepararParaEdicao(funcaoDadosSelecionada);
                break;
            case 'delete':
                this.confirmDelete(funcaoDadosSelecionada);
                break;
            case 'clone':
                this.disableTRDER();
                this.configurarDialog();
                this.isEdit = false;
                this.prepareToClone(funcaoDadosSelecionada);
                this.currentFuncaoDados.id = undefined;
                this.currentFuncaoDados.artificialId = undefined;
                this.currentFuncaoDados.impacto = Impacto.ALTERACAO;
                this.textHeader = 'Clonar Função de Dados'
                break;
            case 'crud':
                this.createCrud(funcaoDadosSelecionada);
        }
    }

      inserirCrud(funcaoTransacaoAtual: FuncaoTransacao){
          
            this.desconverterChips();
            this.verificarModulo();
            
             var funcaoTransacaoCalculada = CalculadoraTransacao.calcular(this.analise.metodoContagem,
                                                                           funcaoTransacaoAtual,
                                                                           this.analise.contrato.manual);
    
                this.validarNameFuncaoTransacaos(funcaoTransacaoAtual.name).then( resolve => {
                    if (resolve) {
                        this.pageNotificationService.addCreateMsgWithName(funcaoTransacaoCalculada.name);
                        this.analise.addFuncaoTransacao(funcaoTransacaoCalculada);
                        this.atualizaResumo();
                        this.resetarEstadoPosSalvar();
                        this.salvarAnalise();
                        this.estadoInicial();
                    } 
                 }); 
    }

    private createCrud(funcaoDadosSelecionada: FuncaoDados) {

            var _this = this;
        
                let crudExcluir = new FuncaoTransacao;
                crudExcluir.name = 'Excluir';
                crudExcluir.funcionalidade = funcaoDadosSelecionada.funcionalidade;
                crudExcluir.tipo = TipoFuncaoTransacao.EE;
                crudExcluir.impacto = Impacto.EXCLUSAO;
                crudExcluir.fatorAjuste = funcaoDadosSelecionada.fatorAjuste;
                this.inserirCrud(crudExcluir);

                let crudEditar = new FuncaoTransacao;
                crudEditar.name = 'Editar';
                crudEditar.funcionalidade = funcaoDadosSelecionada.funcionalidade;
                crudEditar.tipo = TipoFuncaoTransacao.EE;
                crudEditar.impacto = Impacto.ALTERACAO;
                crudEditar.fatorAjuste = funcaoDadosSelecionada.fatorAjuste;
                setTimeout(function(){
                    _this.inserirCrud(crudEditar)
                }, 1000);

                let crudInserir = new FuncaoTransacao;
                crudInserir.name = 'Inserir';
                crudInserir.funcionalidade = funcaoDadosSelecionada.funcionalidade;
                crudInserir.tipo = TipoFuncaoTransacao.EE;
                crudInserir.impacto = Impacto.INCLUSAO;
                crudInserir.fatorAjuste = funcaoDadosSelecionada.fatorAjuste;
                setTimeout(function(){
                    _this.inserirCrud(crudInserir)
                }, 2000);

                let crudPesquisar = new FuncaoTransacao;
                crudPesquisar.name = 'Pesquisar';
                crudPesquisar.funcionalidade = funcaoDadosSelecionada.funcionalidade;
                crudPesquisar.tipo = TipoFuncaoTransacao.CE;
                crudPesquisar.impacto = Impacto.INCLUSAO;
                crudPesquisar.fatorAjuste = funcaoDadosSelecionada.fatorAjuste;
                setTimeout(function(){
                    _this.inserirCrud(crudPesquisar)
                }, 3000);
        }

    private prepararParaEdicao(funcaoDadosSelecionada: FuncaoDados) {

        this.disableTRDER();
        this.configurarDialog();

        this.currentFuncaoDados = funcaoDadosSelecionada;
        
        this.carregarValoresNaPaginaParaEdicao(funcaoDadosSelecionada);
        this.pageNotificationService.addInfoMsg(`Alterando Função de Dados '${funcaoDadosSelecionada.name}'`);
    }

    // Prepara para clonar
    private prepareToClone(funcaoDadosSelecionada: FuncaoDados) {
        this.analiseSharedDataService.currentFuncaoDados = funcaoDadosSelecionada;
        this.currentFuncaoDados.name = this.currentFuncaoDados.name + ' - Cópia';
        this.carregarValoresNaPaginaParaEdicao(funcaoDadosSelecionada);
        this.pageNotificationService.addInfoMsg(`Clonando Função de Dados '${funcaoDadosSelecionada.name}'`);
    }

    private carregarValoresNaPaginaParaEdicao(funcaoDadosSelecionada: FuncaoDados) {
        /* Envia os dados para o componente modulo-funcionalidade-component.ts*/
        this.funcaoDadosService.mod.next(funcaoDadosSelecionada.funcionalidade);
        
        this.analiseSharedDataService.funcaoAnaliseCarregada();
        this.carregarDerERlr(funcaoDadosSelecionada);
        this.carregarFatorDeAjusteNaEdicao(funcaoDadosSelecionada);
    }

    private carregarFatorDeAjusteNaEdicao(funcaoSelecionada: FuncaoDados) {
        this.inicializaFatoresAjuste(this.manual);
        if (funcaoSelecionada.fatorAjuste !== undefined) {
            funcaoSelecionada.fatorAjuste = _.find(this.fatoresAjuste, {value: {'id': funcaoSelecionada.fatorAjuste.id}}).value;
        }

    }

    private carregarDerERlr(fd: FuncaoDados) {
        this.dersChips = this.loadReference(fd.ders, fd.derValues);
        this.rlrsChips = this.loadReference(fd.rlrs, fd.rlrValues);
    }

    moduloSelected(modulo: Modulo) {
    }

    // Carregar Referencial
    private loadReference(referenciaveis: AnaliseReferenciavel[],
                          strValues: string[]): DerChipItem[] {

        if (referenciaveis) {
            if (referenciaveis.length > 0) {
                return DerChipConverter.converterReferenciaveis(referenciaveis);
            } else {
                return DerChipConverter.converter(strValues);
            }
        } else {
            return DerChipConverter.converter(strValues);
        }
    }

    cancelar() {
        this.showDialog = false;
        this.fecharDialog();
    }


    confirmDelete(funcaoDadosSelecionada: FuncaoDados) {
        this.confirmationService.confirm({
            message: `Tem certeza que deseja excluir a Função de Dados '${funcaoDadosSelecionada.name}'?`,
            accept: () => {
                this.analise.deleteFuncaoDados(funcaoDadosSelecionada);
                this.salvarAnalise();
                this.pageNotificationService.addDeleteMsgWithName(funcaoDadosSelecionada.name);
            }
        });
    }

    formataFatorAjuste(fatorAjuste: FatorAjuste): string {
        return fatorAjuste ? FatorAjusteLabelGenerator.generate(fatorAjuste) : 'Nenhum';
    }

    ordenarColunas(colunasAMostrarModificada: SelectItem[]) {
        this.colunasAMostrar = colunasAMostrarModificada;
        this.colunasAMostrar = _.sortBy(this.colunasAMostrar, col => col.index);
    }

    ngOnDestroy() {
        this.changeDetectorRef.detach();
        this.analiseCarregadaSubscription.unsubscribe();
    }

    openDialog(param: boolean) {
        console.log(`openDialog(param)\n -> this.isEdit: ${this.isEdit}\n -> param: ${param}`);
        this.subscribeToAnaliseCarregada();
        this.carregarDadosBaseline();
        this.isEdit = param;
        this.hideShowQuantidade = true;
        this.disableTRDER();
        this.configurarDialog();
        this.currentFuncaoDados.fatorAjuste = this.faS[0];
    }

    configurarDialog() {
        this.getTextDialog();
        this.windowHeightDialog = window.innerHeight * 0.60;
        this.windowWidthDialog = window.innerWidth * 0.50;
        this.showDialog = true;
    }

    private inicializaFatoresAjuste(manual: Manual) {
        this.faS = _.cloneDeep(manual.fatoresAjuste);

        this.faS.sort((n1,n2) => {
            if (n1.fator < n2.fator) 
                return 1;   
            if (n1.fator > n2.fator) 
                return -1;
            return 0;
        });
        
        this.fatoresAjuste =
            this.faS.map(fa => {
                const label = FatorAjusteLabelGenerator.generate(fa);
                return {label: label,  value: fa};
            });
        
        this.fatoresAjuste.unshift(this.fatorAjusteNenhumSelectItem);
        


    }

    textChanged() {
        this.valueChange.emit(this.text);
        this.parseResult = DerTextParser.parse(this.text);
    }

    buttonMultiplos() {
        this.showMultiplos = !this.showMultiplos;
    }

}
