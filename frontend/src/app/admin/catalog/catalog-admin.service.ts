import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CatalogModule } from './catalog-module.model';

@Injectable({ providedIn: 'root' })
export class CatalogAdminService {
  private readonly baseUrl = `${environment.apiBaseUrl}/admin/catalog`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<CatalogModule[]> {
    return this.http.get<CatalogModule[]>(this.baseUrl);
  }

  getByKey(moduleKey: string): Observable<CatalogModule> {
    return this.http.get<CatalogModule>(`${this.baseUrl}/${moduleKey}`);
  }
}
