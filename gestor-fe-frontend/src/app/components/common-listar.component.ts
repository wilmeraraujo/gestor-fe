import { OnInit, ViewChild, Directive, OnDestroy, inject } from '@angular/core';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import Swal from 'sweetalert2';
import { CommonService } from '../services/common.service';
import { LoginService } from '../services/login.service';
import { Generic } from '../models/generic';
import { MatTableDataSource } from '@angular/material/table';
import { Observable, Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

@Directive()
export abstract class CommonListarComponent<E extends Generic,S extends CommonService<E> > implements OnDestroy
{

  titulo: string = '';
  lista: E[] = [];
  protected nombreModel: string ='';
  filtroDescripcion: string = '';
  totalRegistros = 0;
  paginaActual = 0;
  totalPorPagina = 5;
  pageSizeOptions: number[] = [5, 10, 25, 50, 100];

  filtrosMap: { [key: string]: string } = {};
  private baseFiltroSubject = new Subject<{ [key: string]: string }>();
  private baseFiltroSubscription?: Subscription;

  protected commonLoginService = inject(LoginService);

  dataSource: MatTableDataSource<E> = new MatTableDataSource<E>();

  constructor(protected service: S){
    this.initBaseDebounceFiltros();
  }

  ngOnDestroy(): void {
    if (this.baseFiltroSubscription) {
      this.baseFiltroSubscription.unsubscribe();
    }
  }

  protected initBaseDebounceFiltros(): void {
    this.baseFiltroSubscription = this.baseFiltroSubject.pipe(
      debounceTime(400),
      distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
    ).subscribe(filtros => {
      this.filtrosMap = filtros;
      this.paginaActual = 0;
      this.calcularRangos();
    });
  }

  public onFiltrosChange(filtros: { [key: string]: string }): void {
    this.filtrosMap = filtros;
    this.paginaActual = 0;
    this.calcularRangos();
  }

  public paginar(event: PageEvent):void{
    this.paginaActual = event.pageIndex;
    this.totalPorPagina = event.pageSize;
    this.calcularRangos();
  }

  public calcularRangos(): void {
    const servicio = this.service.getPaginableFiltrado(
      this.filtrosMap,
      this.paginaActual.toString(),
      this.totalPorPagina.toString()
    );

    servicio.subscribe({
      next: (p: any) => {
        this.lista = (p.content || []) as E[];
        this.totalRegistros = (p.totalElements || 0) as number;
        this.dataSource.data = this.lista;
      },
      error: (err: any) => {
        console.error('Error al consultar lista paginada y filtrada:', err);
      }
    });
  }

  public eliminar(e: E): void{

    Swal.fire({
      title: 'Atención!',
      text: `¿Seguro que desea eliminar a ${e.descripcion}?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#3f51b5',
      cancelButtonColor: '#6c757d',
      confirmButtonText: 'Si, Eliminar!'
    }).then((result) => {
      if (result.isConfirmed) {
        this.service.eliminar(e.id).subscribe(()=>{
          this.calcularRangos();
          Swal.fire('Eliminado:',`${this.nombreModel} ${e.descripcion} eliminado con exito`,'success');
        });
      }
    })
  }

  public onToggleEstado(entidad: E, userName: string = ''): void {
    this.cambiarEstado(entidad, userName);
  }

  public cambiarEstado(entidad: E, userName: string = ''): void {
    const isActivo = !entidad.deletedAt;
    const accion = isActivo ? 'Inactivar' : 'Activar';
    const colorBtn = isActivo ? '#dc2626' : '#16a34a';

    Swal.fire({
      title: `¿Desea ${accion.toLowerCase()} el registro?`,
      text: `Por favor ingrese el motivo de la acción para "${entidad.descripcion || entidad.codigo || entidad.id}":`,
      input: 'textarea',
      inputPlaceholder: `Motivo de ${accion.toLowerCase()}...`,
      inputAttributes: {
        'aria-label': `Motivo de ${accion.toLowerCase()}`,
        maxlength: '500'
      },
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: colorBtn,
      cancelButtonColor: '#64748b',
      confirmButtonText: `Sí, ${accion}`,
      cancelButtonText: 'Cancelar',
      inputValidator: (value) => {
        if (!value || !value.trim()) {
          return 'Debe ingresar una observación obligatoria';
        }
        return null;
      }
    }).then((result) => {
      if (result.isConfirmed) {
        const observacion = result.value ? result.value.trim() : '';
        const usuarioActivo = (userName && userName.trim() !== '') ? userName.trim() : this.commonLoginService.getUserName();
        this.service.toggleEstado(entidad.id, observacion, usuarioActivo).subscribe({
          next: () => {
            Swal.fire({
              icon: 'success',
              title: `${accion} completado`,
              text: `El registro fue ${isActivo ? 'inactivado' : 'activado'} exitosamente.`,
              confirmButtonColor: '#1DA6BA',
              timer: 2500,
              timerProgressBar: true
            });
            this.calcularRangos();
          },
          error: (err: any) => {
            const errorMsg = err.error?.error || `Error al ${accion.toLowerCase()} el registro.`;
            Swal.fire({
              icon: 'error',
              title: 'Error',
              text: errorMsg,
              confirmButtonColor: '#1DA6BA'
            });
          }
        });
      }
    });
  }

  public toggleActivarInactivar(
    checked: boolean,
    entidad: E,
    activarInactivarFn: (entidad: E, userName: string, observacion: string) => Observable<{ tipo: E, mensaje: string }>,
    userName: string
  ): void {
    const accion = checked ? 'Activar' : 'Inactivar';
    const texto = `Está a punto de ${accion.toLowerCase()} el elemento "${entidad.descripcion}".`;

    const swalOptions: any = {
      title: '¿Está seguro?',
      text: texto,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#3f51b5',
      cancelButtonColor: '#6c757d',
      confirmButtonText: `Sí, ${accion.toLowerCase()}`,
      cancelButtonText: 'Cancelar',
    };

    if (!checked) {
      swalOptions.input = 'text';
      swalOptions.inputLabel = 'Observación';
      swalOptions.inputPlaceholder = 'Ingrese el motivo de la inactivación';
      swalOptions.inputValidator = (value: string) => {
        if (!value) {
          return 'Debe ingresar una observación';
        }
        return null;
      };
    }

    Swal.fire(swalOptions).then((result) => {
      if (result.isConfirmed && (checked || result.value)) {
        const observacion = checked ? '' : result.value;

        activarInactivarFn(entidad, userName, observacion).subscribe({
          next: (response) => {
            Swal.fire(accion, response.mensaje, 'success');
            this.calcularRangos();
          },
          error: (err) => {
            console.error('Error:', err);
            const errorMsg = err.error?.error || `Hubo un error al ${accion.toLowerCase()} el elemento.`;
            Swal.fire('Error', errorMsg, 'error');
            this.calcularRangos();
          }
        });
      } else {
        this.calcularRangos();
      }
    });
  }

}
