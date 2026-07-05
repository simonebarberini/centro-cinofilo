import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CatalogAdminService } from './catalog-admin.service';
import { CatalogModule } from './catalog-module.model';

@Component({
  selector: 'app-catalog-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './catalog-list.component.html',
})
export class CatalogListComponent implements OnInit {
  modules: CatalogModule[] = [];
  loading = false;
  error: string | null = null;
  searchTerm = '';

  constructor(private catalogAdmin: CatalogAdminService) {}

  ngOnInit(): void {
    this.loadModules();
  }

  loadModules(): void {
    this.loading = true;
    this.error = null;
    this.catalogAdmin.getAll().subscribe({
      next: (data) => {
        this.modules = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Impossibile caricare il catalogo. Riprova.';
        this.loading = false;
        console.error('Error loading catalog', err);
      },
    });
  }

  get filteredModules(): CatalogModule[] {
    const term = this.searchTerm.trim().toLowerCase();
    if (!term) {
      return this.modules;
    }
    return this.modules.filter((m) => m.name.toLowerCase().includes(term));
  }
}
