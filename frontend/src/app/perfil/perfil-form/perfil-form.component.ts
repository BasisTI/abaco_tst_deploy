import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthenticationService, AuthorizationService, BlockUiService } from '@nuvem/angular-base';
import { DatatableClickEvent, PageNotificationService } from '@nuvem/primeng-components';
import { ConfirmationService, SelectItem } from 'primeng';
import { Subscription } from 'rxjs';
import { User } from 'src/app/user/user.model';
import { Perfil } from '../perfil.model';
import { PerfilService } from '../perfil.service';
import { Permissao } from '../permissao.model';


@Component({
    selector: 'app-perfil-form',
    templateUrl: './perfil-form.component.html',
})
export class PerfilFormComponent implements OnInit {

    perfil: Perfil = new Perfil();

    novasPermissoes: Permissao[] = [];
    idEditPermissao: number;

    isSaving: boolean;
    mostrarDialogPermissao: boolean;
    mostrarDialogEditarPermissao: boolean;
    valido: boolean;

    permissoesOptions: SelectItem[] = [];
    permissoes: Permissao[];


    readonly edit = 'edit';
    readonly delete = 'delete';

    routeSub: Subscription;

    constructor(
        private perfilService: PerfilService,
        private pageNotificationService: PageNotificationService,
        private router: Router,
        private confirmationService: ConfirmationService,
        private route: ActivatedRoute,
        private blockUiService: BlockUiService,
        private authService: AuthenticationService<User>
    ) { }


    ngOnInit() {
        this.routeSub = this.route.params.subscribe(params => {
            if (params['id']) {
                this.blockUiService.show();
                this.perfilService.find(params['id']).subscribe(
                    perfil => {
                        this.perfil = perfil;
                        this.blockUiService.hide();
                    });
            }
        });
        this.perfilService.getAllPermissoes().subscribe(response => {
            this.permissoes = response;
        });
    }

    save(form) {
        if (this.perfil) {
            if (this.perfil.id) {
                this.perfilService.update(this.perfil).subscribe(() => {
                    this.router.navigate(['/perfil']);
                    this.pageNotificationService.addUpdateMsg();
                });
            } else {
                this.perfilService.create(this.perfil).subscribe(() => {
                    this.router.navigate(['/perfil']);
                    this.pageNotificationService.addCreateMsg();
                });
            }
        }
    }

    abrirDialogPermissao() {
        this.mostrarDialogPermissao = true;
        this.permissoesOptions = [];
        if (this.perfil.permissaos) {
            for (let index = 0; index < this.permissoes.length; index++) {
                const permissao = this.permissoes[index];
                if (this.perfil.permissaos.findIndex(i => i.id === permissao.id) < 0) {
                    this.permissoesOptions.push({
                        value: permissao,
                        label: permissao.acao.descricao + ' ' + permissao.funcionalidadeAbaco.nome
                    });
                }
            }
        } else {
            for (let index = 0; index < this.permissoes.length; index++) {
                const permissao = this.permissoes[index];
                this.permissoesOptions.push({
                    value: permissao,
                    label: permissao.acao.descricao + ' ' + permissao.funcionalidadeAbaco.nome
                });
            }
        }
    }
    fecharDialogPermissao() {
        this.doFecharDialogPermissao();
    }

    private doFecharDialogPermissao() {
        this.mostrarDialogPermissao = false;
        this.novasPermissoes = null;
    }

    datatableClickPermissao(event: DatatableClickEvent) {
        if (!event.selection) {
            return;
        }
        switch (event.button) {
            case this.delete:
                this.confirmDeletePermissao(event.selection);
                break;
            default:
                break;
        }
    }

    confirmDeletePermissao(listPermissoes: Permissao[]) {
        if (listPermissoes !== undefined) {
            this.confirmationService.confirm({
                message: 'Tem certeza que deseja excluir as permissões selecionadas?',
                accept: () => {
                    for (let i = 0; i < listPermissoes.length; i++) {
                        const permissao = listPermissoes[i];
                        this.perfil.permissaos.splice(this.perfil.permissaos.indexOf(permissao), 1);
                    }
                }
            });
        }
    }

    adicionarPermissao() {
        if (this.novasPermissoes === undefined) {
            this.valido = true;
            this.pageNotificationService.addErrorMessage('Por favor preencher o campo obrigatório!');
            return;
        }
        this.valido = false;
        if (!this.perfil.permissaos) {
            this.perfil.permissaos = [];
        }

        for (let novaPermissao of this.novasPermissoes) {
            this.perfil.permissaos.push(novaPermissao);
        }
        this.doFecharDialogPermissao();
    }
}