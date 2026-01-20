import {Component, inject, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {finalize} from 'rxjs/operators';
import type {StatsDto} from '../core/modules/openapi';
import {ControllerService} from '../core/modules/openapi';
import {ApiErrorUtil} from '../core/shared/api-error.util';


@Component({
  selector: 'app-statistics',
  imports: [CommonModule, FormsModule],
  templateUrl: './statistics.html',
  styleUrl: './statistics.css',
  standalone: true
})
export class Statistics {
  loading = signal(false);
  error = signal('');
  stats = signal<StatsDto | null>(null);
  shortCode = signal('');
  private controller = inject(ControllerService);

  getStats() {
    this.error.set('');
    this.stats.set(null);

    const code = this.shortCode();
    if (!code) {
      this.error.set('Bitte einen gültigen Shortcode eingeben.');
      return;
    }

    this.loading.set(true);

    this.controller.getStats(code)
      .pipe(
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: (stats: StatsDto) => {
          this.stats.set(stats);
        },
        error: (err: unknown) => {
          const detail = ApiErrorUtil.getDetailFromHttpError(err, 'Statistiken konnten nicht geladen werden.');
          this.error.set(detail);
        }
      });
  }
}
