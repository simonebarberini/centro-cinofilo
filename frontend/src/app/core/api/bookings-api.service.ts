import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Booking,
  CreateBookingRequest,
  UpdateBookingRequest,
  DailyAvailability,
} from '../models/booking.model';

@Injectable({ providedIn: 'root' })
export class BookingsApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/bookings`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Booking[]> {
    return this.http.get<Booking[]>(this.baseUrl);
  }

  getById(id: string): Observable<Booking> {
    return this.http.get<Booking>(`${this.baseUrl}/${id}`);
  }

  create(request: CreateBookingRequest): Observable<Booking> {
    return this.http.post<Booking>(this.baseUrl, request);
  }

  update(id: string, request: UpdateBookingRequest): Observable<Booking> {
    return this.http.put<Booking>(`${this.baseUrl}/${id}`, request);
  }

  cancel(id: string): Observable<Booking> {
    return this.http.post<Booking>(`${this.baseUrl}/${id}/cancel`, {});
  }

  availability(start: string, end: string): Observable<DailyAvailability[]> {
    const params = new HttpParams().set('start', start).set('end', end);
    return this.http.get<DailyAvailability[]>(`${this.baseUrl}/availability`, { params });
  }
}
