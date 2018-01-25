import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Response } from '@angular/http';
import { Observable, Subscription } from 'rxjs/Rx';
import { SelectItem } from 'primeng/primeng';

import { Manual } from './manual.model';
import { ManualService } from './manual.service';
import { EsforcoFaseService } from '../esforco-fase/esforco-fase.service';
import { ResponseWrapper } from '../shared';
import { EsforcoFase } from '../esforco-fase/esforco-fase.model';
import { TipoFaseService } from '../tipo-fase/tipo-fase.service';
import { TipoFase } from '../tipo-fase/tipo-fase.model';
import { DatatableClickEvent } from '@basis/angular-components';
import { ConfirmationService } from 'primeng/components/common/confirmationservice';
import { FatorAjuste, TipoFatorAjuste } from '../fator-ajuste/fator-ajuste.model';
import { PageNotificationService } from '../shared/page-notification.service';

@Component({
  selector: 'jhi-manual-form',
  templateUrl: './manual-form.component.html'
})
export class ManualFormComponent implements OnInit, OnDestroy {
  manual: Manual;
  isSaving: boolean;
  private routeSub: Subscription;
  arquivoManual: File;
  esforcoFases: Array<EsforcoFase>;
  showDialogPhaseEffort: boolean = false;
  showDialogEditPhaseEffort: boolean = false;
  showDialogCreateAdjustFactor: boolean = false;
  showDialogEditAdjustFactor: boolean = false;
  tipoFases: Array<TipoFase> = [];
  percentual: number;
  newPhaseEffort: EsforcoFase = new EsforcoFase();
  editedPhaseEffort: EsforcoFase = new EsforcoFase();
  newAdjustFactor: FatorAjuste = new FatorAjuste();
  editedAdjustFactor: FatorAjuste = new FatorAjuste();
  adjustTypes: Array<any> = [
    {
      label: 'Percentual',
      value: 'PERCENTUAL',
    },
    {
      label: 'Unitário',
      value: 'UNITARIO',
    },
  ]
  invalidFields: Array<string> = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private manualService: ManualService,
    private esforcoFaseService: EsforcoFaseService,
    private tipoFaseService: TipoFaseService,
    private confirmationService: ConfirmationService,
    private pageNotificationService: PageNotificationService
  ) {}

  ngOnInit() {
    this.isSaving = false;
    this.routeSub = this.route.params.subscribe(params => {
      this.manual = new Manual();
      if (params['id']) {
        this.manualService.find(params['id']).subscribe(manual => {
          this.manual = manual;
        });
      }
    });

    this.tipoFaseService.query().subscribe((response: ResponseWrapper) => {
      this.tipoFases = response.json;
    });
  }

  save() {
    this.isSaving = true;
    this.manual.valorVariacaoEstimada = this.manual.valorVariacaoEstimada/100;
    this.manual.valorVariacaoIndicativa = this.manual.valorVariacaoIndicativa/100;

    console.log(this.manual);
    if (this.manual.id !== undefined) {
      this.subscribeToSaveResponse(this.manualService.update(this.manual));
    } else {
      if(this.arquivoManual !== undefined) {
        if(this.checkRequiredFields()) {
          this.subscribeToSaveResponse(this.manualService.create(this.manual, this.arquivoManual));
        } else {
          this.pageNotificationService.addErrorMsg('Campos inválidos: ' + this.getInvalidFieldsString());
          this.invalidFields = [];
        }
      } else {
        this.pageNotificationService.addErrorMsg('Campo Arquivo Manual está inválido!');
      }
    }
  }

  private checkRequiredFields(): boolean {
      let isFieldsValid = false;
      console.log(this.manual);
      if ( isNaN(this.manual.valorVariacaoEstimada)) (this.invalidFields.push('Valor Variação Estimada'));
      if ( isNaN(this.manual.valorVariacaoIndicativa)) (this.invalidFields.push('Valor Variação Inidicativa'));

      isFieldsValid = (this.invalidFields.length === 0);

      return isFieldsValid;
  }

  private getInvalidFieldsString(): string {
    let invalidFieldsString = "";
    this.invalidFields.forEach(invalidField => {
      if(invalidField === this.invalidFields[this.invalidFields.length-1]) {
        invalidFieldsString = invalidFieldsString + invalidField;
      } else {
        invalidFieldsString = invalidFieldsString + invalidField + ', ';
      }
    });

    return invalidFieldsString;
  }

  private subscribeToSaveResponse(result: Observable<Manual>) {
    result.subscribe((res: Manual) => {
      this.isSaving = false;
      this.router.navigate(['/manual']);
      this.pageNotificationService.addCreateMsg();
    }, (error: Response) => {
      this.isSaving = false;
      switch(error.status) {
        case 400: {
          let invalidFieldNamesString = "";
          const fieldErrors = JSON.parse(error["_body"]).fieldErrors;
          invalidFieldNamesString = this.pageNotificationService.getInvalidFields(fieldErrors);
          this.pageNotificationService.addErrorMsg("Campos inválidos: " + invalidFieldNamesString);
        }
      }
    });
  }

  ngOnDestroy() {
    this.routeSub.unsubscribe();
  }

  uploadFile(event) {
    this.arquivoManual = event.files[0];
  }

  datatableClick(event: DatatableClickEvent) {
    if (!event.selection) {
      return;
    }
    console.log(event.selection);
    switch (event.button) {
      case 'edit':
        this.editedPhaseEffort = event.selection.clone();
        this.openDialogEditPhaseEffort();
        break;
      case 'delete':
      console.log(event.selection);
        this.editedPhaseEffort = event.selection.clone();
        this.confirmDeletePhaseEffort();
    }
  }

  adjustFactorDatatableClick(event: DatatableClickEvent) {
    if (!event.selection) {
      return;
    }
    switch (event.button) {
      case 'edit':
        this.editedAdjustFactor = event.selection.clone();
        this.openDialogEditAdjustFactor();
        break;
      case 'delete':
      console.log(event.selection);
        this.editedAdjustFactor = event.selection.clone();
        this.confirmDeleteAdjustFactor();
    }
  }

  confirmDeletePhaseEffort() {
    this.manual.deleteEsforcoFase(this.editedPhaseEffort);
    this.editedPhaseEffort = new EsforcoFase();
  }

  confirmDeleteAdjustFactor() {
    this.manual.deleteFatoresAjuste(this.editedAdjustFactor);
    this.editedAdjustFactor = new FatorAjuste();
  }

  openDialogPhaseEffort() {
    this.newPhaseEffort = new EsforcoFase();
    this.showDialogPhaseEffort = true;
  }

  openDialogEditPhaseEffort() {
      this.showDialogEditPhaseEffort = true;
  }

  editPhaseEffort() {
    this.editedPhaseEffort.esforco/100;
    this.manual.updateEsforcoFases(this.editedPhaseEffort);
    this.closeDialogEditPhaseEffort();
  }

  editAdjustFactor() {
    this.manual.updateFatoresAjuste(this.editedAdjustFactor);
    this.closeDialogEditAdjustFactor();
  }

  closeDialogPhaseEffort() {
    this.newPhaseEffort = new EsforcoFase();
    this.showDialogPhaseEffort = false;
  }

  closeDialogEditPhaseEffort() {
    this.editedPhaseEffort = new EsforcoFase();
    this.showDialogEditPhaseEffort = false;
  }

  addPhaseEffort() {
    this.newPhaseEffort.esforco = this.newPhaseEffort.esforco;
    this.manual.addEsforcoFases(this.newPhaseEffort);
    this.closeDialogPhaseEffort();
  }

  getPhaseEffortTotalPercentual() {
    let total = 0;
    this.manual.esforcoFases.forEach(each => {
      (each.esforco !== undefined) ? (total = total + each.esforco) : (total = total)
    });

    return total;
  }

  openDialogCreateAdjustFactor() {
    this.showDialogCreateAdjustFactor = true;
  }

  closeDialogCreateAdjustFactor() {
    this.showDialogCreateAdjustFactor = false;
    this.newAdjustFactor = new FatorAjuste();
  }

  openDialogEditAdjustFactor() {
    this.showDialogEditAdjustFactor = true;
  }

  closeDialogEditAdjustFactor() {
      this.showDialogEditAdjustFactor = false;
      this.editedAdjustFactor = new FatorAjuste();
  }

  addAdjustFactor() {
    this.newAdjustFactor.ativo = true;
    this.manual.addFatoresAjuste(this.newAdjustFactor);
    this.closeDialogCreateAdjustFactor();
  }
}
