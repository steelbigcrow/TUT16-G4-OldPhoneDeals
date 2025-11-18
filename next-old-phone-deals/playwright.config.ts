import { defineConfig, devices } from '@playwright/test';
import path from 'node:path';

const backendDir = path.resolve(__dirname, '../spring-old-phone-deals');
const javaHome =
  process.env.PLAYWRIGHT_JAVA_HOME ?? process.env.JAVA_HOME ?? '';

export default defineConfig({
  testDir: './playwright/tests',
  timeout: 120 * 1000,
  expect: {
    timeout: 10 * 1000
  },
  reporter: [['list']],
  use: {
    baseURL: 'http://127.0.0.1:4200',
    trace: 'on-first-retry',
    video: 'retain-on-failure',
    screenshot: 'only-on-failure'
  },
  webServer: [
    {
      command:
        'mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8080 --spring.profiles.active=dev --app.e2e.enabled=true"',
      cwd: backendDir,
      port: 8080,
      reuseExistingServer: !process.env.CI,
      env: {
        MONGODB_URI: 'mongodb://127.0.0.1:27017/oldphonedeals_e2e',
        JWT_SECRET: 'playwright-e2e-secret',
        ...(javaHome ? { JAVA_HOME: javaHome } : {})
      }
    },
    {
      command: 'npm run dev -- --hostname 127.0.0.1 --port 4200',
      cwd: __dirname,
      port: 4200,
      reuseExistingServer: !process.env.CI,
      env: {
        NEXT_PUBLIC_API_BASE_URL: 'http://127.0.0.1:8080/api'
      }
    }
  ],
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] }
    }
  ]
});
