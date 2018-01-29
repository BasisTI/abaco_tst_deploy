import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpModule } from '@angular/http';
import { FormsModule } from '@angular/forms';
import { FuncaoDadosFormComponent } from './funcao-dados-form.component';

import { AbacoButtonsModule } from '../abaco-buttons/abaco-buttons.module';

import {
  ButtonModule,
  InputTextModule,
  SpinnerModule,
  CalendarModule,
  DropdownModule,
  RadioButtonModule,
  ConfirmDialogModule,
  DataTableModule,
  ConfirmationService,
  TabViewModule,
  InputTextareaModule,
  DialogModule
} from 'primeng/primeng';

import { AbacoSharedModule } from '../shared/abaco-shared.module';
import { AbacoAnaliseSharedModule } from '../analise-shared/analise-shared.module';

@NgModule({
  imports: [
    CommonModule,
    HttpModule,
    FormsModule,
    ButtonModule,
    SpinnerModule,
    CalendarModule,
    DropdownModule,
    RadioButtonModule,
    InputTextModule,
    DataTableModule,
    ConfirmDialogModule,
    AbacoButtonsModule,
    TabViewModule,
    InputTextareaModule,
    AbacoSharedModule,
    DialogModule,
    AbacoAnaliseSharedModule
  ],
  declarations: [
    FuncaoDadosFormComponent
  ],
  exports: [
    FuncaoDadosFormComponent
  ]
})
export class AbacoFuncaoDadosModule { }
