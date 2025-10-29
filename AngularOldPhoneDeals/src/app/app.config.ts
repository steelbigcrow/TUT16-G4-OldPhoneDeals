import { ApplicationConfig, importProvidersFrom, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { USER_PROFILE_ROUTES } from './user-profile/user-profile.routes';
import { HttpClientModule } from '@angular/common/http';  // 导入HttpClientModule
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(),
    provideAnimations(),
    provideRouter([
      {
        path: 'user-profile',
        loadChildren: () => USER_PROFILE_ROUTES // main route load sub routes
      }
    ], withComponentInputBinding()),
    importProvidersFrom(HttpClientModule)  // import HttpClientModule
  ]
};
