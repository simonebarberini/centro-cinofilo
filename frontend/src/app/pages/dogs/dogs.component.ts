import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { DogsApiService } from '../../core/api/dogs-api.service';
import { CustomersApiService } from '../../core/api/customers-api.service';
import { Dog } from '../../core/models/dog.model';
import { DogFormComponent } from './dog-form.component';

@Component({
  selector: 'app-dogs',
  standalone: true,
  imports: [CommonModule, DogFormComponent],
  templateUrl: './dogs.component.html',
})
export class DogsComponent implements OnInit {
  dogs: Dog[] = [];
  loading = false;
  error: string | null = null;

  customerId = '';
  customerName = '';
  /** true quando si visualizzano tutti i cani dal sidebar */
  allDogsMode = false;
  /** mappa customerId → nome completo cliente */
  customerMap: Record<string, string> = {};

  formOpen = false;
  editingDog: Dog | null = null;

  deletingDog: Dog | null = null;
  deleting = false;

  @ViewChild('deleteDialog') deleteDialog!: ElementRef<HTMLDialogElement>;

  constructor(
    private api: DogsApiService,
    private customersApi: CustomersApiService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.customerId = this.route.snapshot.paramMap.get('customerId') ?? '';
    this.customerName = this.route.snapshot.queryParamMap.get('name') ?? 'cliente';
    this.allDogsMode = !this.customerId;
    this.loadDogs();
  }

  loadDogs(): void {
    this.loading = true;
    this.error = null;

    if (this.allDogsMode) {
      forkJoin({
        dogs: this.api.getAll(),
        customers: this.customersApi.getAll(),
      }).subscribe({
        next: ({ dogs, customers }) => {
          this.customerMap = {};
          for (const c of customers) {
            this.customerMap[c.id] = `${c.firstName} ${c.lastName}`;
          }
          this.dogs = dogs;
          this.loading = false;
        },
        error: (err) => {
          this.error = 'Impossibile caricare i cani. Riprova.';
          this.loading = false;
          console.error('Error loading dogs', err);
        },
      });
    } else {
      this.api.getByCustomer(this.customerId).subscribe({
        next: (data) => {
          this.dogs = data;
          this.loading = false;
        },
        error: (err) => {
          this.error = 'Impossibile caricare i cani. Riprova.';
          this.loading = false;
          console.error('Error loading dogs', err);
        },
      });
    }
  }

  openCreate(): void {
    this.editingDog = null;
    this.formOpen = true;
  }

  openEdit(dog: Dog): void {
    this.editingDog = dog;
    this.formOpen = true;
  }

  closeForm(): void {
    this.formOpen = false;
    this.editingDog = null;
  }

  onSaved(): void {
    this.closeForm();
    this.loadDogs();
  }

  confirmDelete(dog: Dog): void {
    this.deletingDog = dog;
    this.deleteDialog.nativeElement.showModal();
  }

  executeDelete(): void {
    if (!this.deletingDog) return;
    this.deleting = true;
    this.api.delete(this.deletingDog.id).subscribe({
      next: () => {
        this.deleting = false;
        this.deleteDialog.nativeElement.close();
        this.deletingDog = null;
        this.loadDogs();
      },
      error: (err) => {
        this.deleting = false;
        console.error('Error deleting dog', err);
      },
    });
  }

  goBack(): void {
    this.router.navigate(['/customers']);
  }

  goToCustomerDogs(customerId: string): void {
    const name = this.customerMap[customerId] || 'cliente';
    this.router.navigate(['/customers', customerId, 'dogs'], {
      queryParams: { name },
    });
  }
}
