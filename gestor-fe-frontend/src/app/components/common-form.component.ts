import { Directive } from '@angular/core';
import { Generic } from '../models/generic';
import { CommonService } from '../services/common.service';
import { catchError, Observable, throwError } from 'rxjs';

@Directive()
export abstract class CommonFormComponent<
  E extends Generic,
  S extends CommonService<E>
>
{
  titulo: string = '';
  model!: E;
  error: any;
  protected nombreModel: string = '';

  constructor(
    protected service: S
  ) {}

  public crear(): Observable<E> {
    return this.service.crear(this.model).pipe(
      catchError(err => {
        if (err.status === 400) {
          this.error = err.error;
        }
        return throwError(() => err);
      })
    );
  }
  public create(userName:string): Observable<E> {
    return this.service.create(this.model,userName).pipe(
      catchError(err => {
        if (err.status === 400) {
          this.error = err.error;
        }
        return throwError(() => err);
      })
    );
  }

  public editar(): Observable<E> {
    return this.service.editar(this.model).pipe(
      catchError(err => {
        if (err.status === 400) {
          this.error = err.error;
        }
        return throwError(() => err);
      })
    );
  }

  public update(userName:string): Observable<E> {
    return this.service.update(this.model,userName).pipe(
      catchError(err => {
        if (err.status === 400) {
          this.error = err.error;
        }
        return throwError(() => err);
      })
    );
  }
}
