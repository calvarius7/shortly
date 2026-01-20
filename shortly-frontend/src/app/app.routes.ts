import type {Routes} from '@angular/router';
import {Creation} from './creation/creation';
import {Statistics} from './statistics/statistics';

export const routes: Routes = [
  {path: '', component: Creation},
  {path: 'stats', component: Statistics}
];
