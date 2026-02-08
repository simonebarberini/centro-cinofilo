import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Dog, CreateDogRequest, UpdateDogRequest } from '../models/dog.model';

@Injectable({ providedIn: 'root' })
export class DogsApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/dogs`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Dog[]> {
    return this.http.get<Dog[]>(this.baseUrl);
  }

  getByCustomer(customerId: string): Observable<Dog[]> {
    return this.http.get<Dog[]>(`${this.baseUrl}/by-customer/${customerId}`);
  }

  create(request: CreateDogRequest): Observable<Dog> {
    return this.http.post<Dog>(this.baseUrl, request);
  }

  update(id: string, request: UpdateDogRequest): Observable<Dog> {
    return this.http.put<Dog>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
