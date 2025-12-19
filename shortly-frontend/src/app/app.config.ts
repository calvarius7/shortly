import type {ApplicationConfig} from '@angular/core';
import {provideBrowserGlobalErrorListeners, provideZonelessChangeDetection} from '@angular/core';
import {provideRouter} from '@angular/router';
import {provideHttpClient} from '@angular/common/http';
import {provideApi} from './core/modules/openapi';

import {routes} from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideRouter(routes),
    provideHttpClient(),
    // Point the OpenAPI client to the backend including the /api base path
    provideApi('/api')
  ]
};
