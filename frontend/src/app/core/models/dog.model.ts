export interface Dog {
  id: string;
  customerId: string;
  name: string;
  breed: string;
  birthDate: string | null;
  notes: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateDogRequest {
  customerId: string;
  name: string;
  breed: string;
  birthDate: string | null;
  notes: string;
}

export interface UpdateDogRequest {
  name: string;
  breed: string;
  birthDate: string | null;
  notes: string;
}
