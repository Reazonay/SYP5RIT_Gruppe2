import { defineConfig } from '@playwright/test';
export default defineConfig({
  testDir: 'tests',
  workers: 1,
  use: {
    baseURL: 'http://localhost:5173',
    viewport: { width: 1280, height: 800 },
    trace: 'retain-on-failure',
  },
  webServer: [
    {
      command: 'npm run start -w apps/api',
      url: 'http://localhost:3000/api/health',
      reuseExistingServer: false,
      env: { PUBLIC_URL: 'http://localhost:5173', PORT: '3000' },
    },
    { command: 'npm run dev:web', url: 'http://localhost:5173', reuseExistingServer: false },
  ],
});
