import { Component, signal } from '@angular/core';
import { Creation } from './creation/creation';

@Component({
  selector: 'app-root',
  imports: [Creation],
  templateUrl: './app.html',
  styleUrl: './app.css',
  standalone: true
})
export class App {
  title = 'shortly-fe';
}
