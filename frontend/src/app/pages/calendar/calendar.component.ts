import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BookingsApiService } from '../../core/api/bookings-api.service';
import {
  CalendarResponse,
  CalendarBookingItem,
  DailyAvailability,
} from '../../core/models/booking.model';

@Component({
  selector: 'app-calendar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './calendar.component.html',
})
export class CalendarComponent implements OnInit {
  loading = false;
  error: string | null = null;

  /** Lunedì della settimana corrente */
  weekStart!: Date;
  weekLabel = '';

  days: DailyAvailability[] = [];
  bookings: CalendarBookingItem[] = [];
  capacity = 0;

  /** Giorno selezionato per il pannello dettagli */
  selectedDay: DailyAvailability | null = null;
  selectedDayBookings: CalendarBookingItem[] = [];

  /** Nomi giorni */
  readonly dayNames = ['Lun', 'Mar', 'Mer', 'Gio', 'Ven', 'Sab', 'Dom'];

  constructor(private api: BookingsApiService) {}

  ngOnInit(): void {
    this.weekStart = this.getMonday(new Date());
    this.loadWeek();
  }

  prevWeek(): void {
    this.weekStart = this.addDays(this.weekStart, -7);
    this.selectedDay = null;
    this.loadWeek();
  }

  nextWeek(): void {
    this.weekStart = this.addDays(this.weekStart, 7);
    this.selectedDay = null;
    this.loadWeek();
  }

  goToday(): void {
    this.weekStart = this.getMonday(new Date());
    this.selectedDay = null;
    this.loadWeek();
  }

  selectDay(day: DailyAvailability): void {
    this.selectedDay = day;
    this.selectedDayBookings = this.bookings.filter(
      (b) => b.startDate <= day.date && b.endDate > day.date && b.status === 'CONFIRMED',
    );
  }

  closeDetail(): void {
    this.selectedDay = null;
    this.selectedDayBookings = [];
  }

  loadWeek(): void {
    this.loading = true;
    this.error = null;

    const start = this.toISODate(this.weekStart);
    const end = this.toISODate(this.addDays(this.weekStart, 7));
    this.updateWeekLabel();

    this.api.calendar(start, end).subscribe({
      next: (res: CalendarResponse) => {
        this.days = res.days;
        this.bookings = res.bookings;
        this.capacity = res.capacity;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Impossibile caricare il calendario. Riprova.';
        this.loading = false;
        console.error('Error loading calendar', err);
      },
    });
  }

  /** Colore del badge disponibilità */
  dayColorClass(day: DailyAvailability): string {
    if (day.available <= 0) return 'bg-error text-error-content';
    if (day.available === 1) return 'bg-warning text-warning-content';
    return 'bg-success text-success-content';
  }

  /** Colore bordo della card giorno */
  dayBorderClass(day: DailyAvailability): string {
    if (day.available <= 0) return 'border-error/40';
    if (day.available === 1) return 'border-warning/40';
    return 'border-success/40';
  }

  /** Verifica se è oggi */
  isToday(day: DailyAvailability): boolean {
    return day.date === this.toISODate(new Date());
  }

  // --- private ---

  private updateWeekLabel(): void {
    const endDate = this.addDays(this.weekStart, 6);
    const opts: Intl.DateTimeFormatOptions = { day: 'numeric', month: 'short' };
    const startStr = this.weekStart.toLocaleDateString('it-IT', opts);
    const endStr = endDate.toLocaleDateString('it-IT', { ...opts, year: 'numeric' });
    this.weekLabel = `${startStr} — ${endStr}`;
  }

  private getMonday(d: Date): Date {
    const date = new Date(d);
    const day = date.getDay();
    const diff = date.getDate() - day + (day === 0 ? -6 : 1);
    date.setDate(diff);
    date.setHours(0, 0, 0, 0);
    return date;
  }

  private addDays(d: Date, n: number): Date {
    const result = new Date(d);
    result.setDate(result.getDate() + n);
    return result;
  }

  private toISODate(d: Date): string {
    return d.toISOString().slice(0, 10);
  }
}
