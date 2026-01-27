import { Component } from '@angular/core';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent {
  appTitle = 'Centro Cinofilo';
  description = 'Sistema di gestione e amministrazione per centri di addestramento cinofilo';
}
