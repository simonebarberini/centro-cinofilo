export type BookingStatus = 'CONFIRMED' | 'CANCELLED';

export interface Booking {
  id: string;
  customerId: string;
  dogId: string;
  startDate: string;
  endDate: string;
  notes: string;
  status: BookingStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateBookingRequest {
  customerId: string;
  dogId: string;
  startDate: string;
  endDate: string;
  notes: string;
}

export interface UpdateBookingRequest {
  startDate?: string;
  endDate?: string;
  notes?: string;
  status?: BookingStatus;
}

export interface DailyAvailability {
  date: string;
  capacity: number;
  booked: number;
  available: number;
}

export interface CalendarResponse {
  start: string;
  end: string;
  capacity: number;
  days: DailyAvailability[];
  bookings: CalendarBookingItem[];
}

export interface CalendarBookingItem {
  id: string;
  startDate: string;
  endDate: string;
  status: BookingStatus;
  notes: string | null;
  customer: CustomerSummary;
  dog: DogSummary;
}

export interface CustomerSummary {
  id: string;
  firstName: string;
  lastName: string;
}

export interface DogSummary {
  id: string;
  name: string;
}
