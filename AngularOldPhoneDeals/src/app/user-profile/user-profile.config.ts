import { ApplicationConfig, importProvidersFrom } from '@angular/core';
import { provideRouter } from '@angular/router';
import { HttpClientModule } from '@angular/common/http';
import { USER_PROFILE_ROUTES } from './user-profile.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    importProvidersFrom(HttpClientModule), // 全局 HTTP 支持
    provideRouter([                        // 根路由
      { 
        path: 'user-profile',
        loadChildren: () => USER_PROFILE_ROUTES
      }
    ])
  ]
};