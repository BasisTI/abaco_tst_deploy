import { Component, ViewChild, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ConfirmationService} from 'primeng/primeng';
import { DatatableComponent, DatatableClickEvent } from '@basis/angular-components';
import { BlockUI, NgBlockUI} from 'ng-block-ui';
import { environment } from '../../environments/environment';
import { Analise } from './analise.model';
import { AnaliseService } from './analise.service';
import { ElasticQuery, PageNotificationService } from '../shared';
import { MessageUtil } from '../util/message.util';

@Component({
  // tslint:disable-next-line:component-selector
  selector: 'jhi-analise',
  templateUrl: './analise.component.html'
})
export class AnaliseComponent implements OnInit {

  @BlockUI() blockUI: NgBlockUI;

  @ViewChild(DatatableComponent) datatable: DatatableComponent;

  searchUrl: string = this.analiseService.searchUrl;

  elasticQuery: ElasticQuery = new ElasticQuery();

  rowsPerPageOptions: number[] = [5, 10, 20];

  analiseSelecionada: Analise;

  /**
   *
   */
  constructor(
    private router: Router,
    private analiseService: AnaliseService,
    private confirmationService: ConfirmationService,
    private pageNotificationService: PageNotificationService,
  ) { }

  /**
   *
   */
  public ngOnInit() {
    this.datatable.pDatatableComponent.onRowSelect.subscribe((event) => {
      this.analiseSelecionada = new Analise().copyFromJSON(event.data);
    });

    this.datatable.pDatatableComponent.onRowUnselect.subscribe((event) => {
      this.analiseSelecionada = undefined;
    });
  }

  /**
   *
   */
  public datatableClick(event: DatatableClickEvent) {
    if (!event.selection) {
      return;
    }
    switch (event.button) {
      case 'edit':
        this.router.navigate(['/analise', event.selection.id, 'edit']);
        break;
      case 'delete':
        this.confirmDelete(event.selection);
        break;
      case 'relatorioBrowser':
        this.geraRelatorioPdfBrowser(event.selection);
        break;
      case 'relatorioArquivo' :
        this.gerarRelatorioPdfArquivo(event.selection);
        break;
    }
  }

  /**
   *
   */
  public confirmDelete(analise: Analise) {
    this.confirmationService.confirm({
      message: MessageUtil.CONFIRMAR_EXCLUSAO + ' ' + analise.identificadorAnalise + '?',
      accept: () => {
        this.blockUI.start(MessageUtil.EXCLUINDO_REGISTRO);
        this.analiseService.delete(analise.id).subscribe(() => {
          this.recarregarDataTable();
          this.blockUI.stop();
          this.pageNotificationService.addDeleteMsgWithName(analise.identificadorAnalise);
        });
      }
    });
  }

  /**
   *
   */
  public limparPesquisa() {
    this.elasticQuery.reset();
    this.recarregarDataTable();
  }

  /**
   *
   */
  public recarregarDataTable() {
    this.datatable.refresh(undefined);
  }

  /**
   * Método responsável por gerar o relatório diretamente sem a apresentação do relatório no browser.
   */
  public gerarRelatorioPdfArquivo(analise: Analise) {
    this.analiseService.gerarRelatorioPdfArquivo(analise.id);
  }

  /**
   * Método responsável pela a apresentação do relatório no browser.
   */
  public geraRelatorioPdfBrowser(analise: Analise) {
    this.analiseService.geraRelatorioPdfBrowser(analise.id);
  }

  /**
   *
   */
  public desabilitarBotaoRelatorio(): boolean {
    if (this.analiseSelecionada) {
      return true;
    } else {
      return false;
    }
  }

}
