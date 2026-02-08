import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TenantApiService } from '../../core/api/tenant-api.service';
import { TenantSettings } from '../../core/models/tenant.model';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './settings.component.html',
})
export class SettingsComponent implements OnInit {
  loading = false;
  saving = false;
  error: string | null = null;
  success: string | null = null;

  settings: TenantSettings | null = null;

  capacityBoxes = 0;
  defaultCalendarView = 'FOUR_WEEKS';

  constructor(private api: TenantApiService) {}

  ngOnInit(): void {
    this.loadSettings();
  }

  loadSettings(): void {
    this.loading = true;
    this.error = null;

    this.api.getMe().subscribe({
      next: (s) => {
        this.settings = s;
        this.capacityBoxes = s.capacityBoxes;
        this.defaultCalendarView = s.preferences?.['defaultCalendarView'] || 'FOUR_WEEKS';
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Impossibile caricare le impostazioni. Riprova.';
        this.loading = false;
        console.error('Error loading settings', err);
      },
    });
  }

  save(): void {
    this.saving = true;
    this.error = null;
    this.success = null;

    const prefs = {
      ...(this.settings?.preferences || {}),
      defaultCalendarView: this.defaultCalendarView,
    };

    this.api
      .updateMe({
        capacityBoxes: this.capacityBoxes,
        preferences: prefs,
      })
      .subscribe({
        next: (s) => {
          this.settings = s;
          this.capacityBoxes = s.capacityBoxes;
          this.defaultCalendarView = s.preferences?.['defaultCalendarView'] || 'FOUR_WEEKS';
          this.saving = false;
          this.success = 'Impostazioni salvate con successo!';
          setTimeout(() => (this.success = null), 3000);
        },
        error: (err) => {
          this.saving = false;
          if (err.status === 403) {
            this.error = 'Non hai i permessi per modificare le impostazioni.';
          } else if (err.status === 400) {
            this.error = err.error?.message || 'Dati non validi.';
          } else {
            this.error = 'Errore durante il salvataggio. Riprova.';
          }
          console.error('Error saving settings', err);
        },
      });
  }
}
