import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CustomersApiService } from '../../core/api/customers-api.service';
import { Customer } from '../../core/models/customer.model';
import { CustomerFormComponent } from './customer-form.component';

@Component({
  selector: 'app-customers',
  standalone: true,
  imports: [CommonModule, CustomerFormComponent],
  templateUrl: './customers.component.html',
})
export class CustomersComponent implements OnInit {
  customers: Customer[] = [];
  loading = false;
  error: string | null = null;

  formOpen = false;
  editingCustomer: Customer | null = null;

  deletingCustomer: Customer | null = null;
  deleting = false;

  @ViewChild('deleteDialog') deleteDialog!: ElementRef<HTMLDialogElement>;

  constructor(private api: CustomersApiService) {}

  ngOnInit(): void {
    this.loadCustomers();
  }

  loadCustomers(): void {
    this.loading = true;
    this.error = null;
    this.api.getAll().subscribe({
      next: (data) => {
        this.customers = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Impossibile caricare i clienti. Riprova.';
        this.loading = false;
        console.error('Error loading customers', err);
      },
    });
  }

  openCreate(): void {
    this.editingCustomer = null;
    this.formOpen = true;
  }

  openEdit(customer: Customer): void {
    this.editingCustomer = customer;
    this.formOpen = true;
  }

  closeForm(): void {
    this.formOpen = false;
    this.editingCustomer = null;
  }

  onSaved(): void {
    this.closeForm();
    this.loadCustomers();
  }

  confirmDelete(customer: Customer): void {
    this.deletingCustomer = customer;
    this.deleteDialog.nativeElement.showModal();
  }

  executeDelete(): void {
    if (!this.deletingCustomer) return;
    this.deleting = true;
    this.api.delete(this.deletingCustomer.id).subscribe({
      next: () => {
        this.deleting = false;
        this.deleteDialog.nativeElement.close();
        this.deletingCustomer = null;
        this.loadCustomers();
      },
      error: (err) => {
        this.deleting = false;
        console.error('Error deleting customer', err);
      },
    });
  }
}
