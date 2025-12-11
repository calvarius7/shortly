import {Component, inject, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {finalize} from 'rxjs/operators';
import {ControllerService, ShortLinkDto} from '../core/modules/openapi';

@Component({
  selector: 'app-creation',
  imports: [CommonModule, FormsModule],
  templateUrl: './creation.html',
  styleUrl: './creation.css',
  standalone: true
})
export class Creation {
  loading = signal(false);
  error = signal('');
  result = signal<{ shortenedUrl: string } | null>(null);
  urlToShorten = signal('');
  private controller = inject(ControllerService);

  createPost() {
    this.error.set('');
    this.result.set(null);

    const url = this.urlToShorten();
    if (!url) {
      this.error.set('Bitte eine gültige URL eingeben.');
      return;
    }

    const oneDayMs = 24 * 60 * 60 * 1000;
    const payload: ShortLinkDto = {
      url: url,
      expiresAt: new Date(Date.now() + 7 * oneDayMs).toISOString()
    };

    this.loading.set(true);

    this.controller.create(payload, 'body', false, {httpHeaderAccept: 'text/plain' as any})
      .pipe(
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: (shortCode: string) => {
          const origin = (typeof window !== 'undefined' && window.location) ? window.location.origin : '';
          this.result.set({shortenedUrl: `${origin}/${shortCode}`});
        },
        error: (err) => {
          const errorObj = typeof err?.error === 'string'
            ? JSON.parse(err.error)
            : err?.error;

          this.error.set(errorObj?.detail ?? 'Erstellen fehlgeschlagen.');
        }
      });
  }

  protected copy() {
    //copy result to clipboard
    if (this.result()) {
      navigator.clipboard.writeText(this.result()!.shortenedUrl)
        .then(() => {
          alert('URL erfolgreich in die Zwischenablage kopiert');
        })
        .catch(err => {
          this.error.set('Fehler beim Kopieren der URL:' + err);
        });
    }
  }
}
