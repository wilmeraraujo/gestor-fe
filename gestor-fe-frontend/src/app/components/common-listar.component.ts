import { OnInit, ViewChild, Directive, AfterViewInit } from '@angular/core';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import Swal from 'sweetalert2';
import { CommonService } from '../services/common.service';
import { Generic } from '../models/generic';
import { MatTableDataSource } from '@angular/material/table';
import { Observable } from 'rxjs';

@Directive()
export abstract class CommonListarComponent<E extends Generic,S extends CommonService<E> > implements AfterViewInit{

  titulo: string = '';
  lista: E[] = [];
  protected nombreModel: string ='';
  filtroDescripcion: string = '';
  totalRegistros = 0;
  paginaActual = 0;
  totalPorPagina = 5;
  pageSizeOptions: number[] = [5, 10, 25, 50, 100];

  dataSource: MatTableDataSource<E> = new MatTableDataSource<E>();

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  constructor(protected service: S){}

  ngAfterViewInit(): void {
    this.paginator.page.subscribe(event => this.paginar(event));
    this.calcularRangos();
  }

  public paginar(event: PageEvent):void{
    this.paginaActual = event.pageIndex;
    this.totalPorPagina = event.pageSize;
    this.calcularRangos();
  }

  public calcularRangos(): void {

    const servicio = this.service.listarPaginas(this.paginaActual.toString(), this.totalPorPagina.toString());

      servicio.subscribe(p => {
        this.lista = p.content as E[];
        this.totalRegistros = p.totalElements as number;
        this.dataSource.data = this.lista;
        this.paginator._intl.itemsPerPageLabel = 'Registros por página:';
      });

  }

  public eliminar(e: E): void{

    Swal.fire({
      title: 'Cuidado',
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
