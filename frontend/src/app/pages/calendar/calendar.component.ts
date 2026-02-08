import { Component } from '@angular/core';

@Component({
  selector: 'app-calendar',
  standalone: true,
  template: `
    <div class="placeholder">
      <h2>📅 Calendar</h2>
      <p>Calendar view – coming soon</p>
    </div>
  `,
  styles: [
    `
      .placeholder {
        text-align: center;
        padding: 60px 20px;
        color: #666;

        h2 {
          font-size: 28px;
          margin-bottom: 8px;
        }

        p {
          font-size: 16px;
        }
      }
    `
  ]
})
export class CalendarComponent {}
