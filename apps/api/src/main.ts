import { ArgumentsHost, Catch, ExceptionFilter, HttpException, Module } from '@nestjs/common';
import { NestFactory } from '@nestjs/core';
import type { NestExpressApplication } from '@nestjs/platform-express';
import cookieParser from 'cookie-parser';
import type { NextFunction, Request, Response } from 'express';
import helmet from 'helmet';
import { fileURLToPath } from 'node:url';
import 'reflect-metadata';
import { ZodError } from 'zod';
import { StaffGuard } from './auth.js';
import { ApiController } from './controller.js';
import { publicUrl } from './service.js';
import { tick } from './worker.js';
@Catch()
class Errors implements ExceptionFilter {
  catch(error: any, host: ArgumentsHost) {
    const res = host.switchToHttp().getResponse<Response>();
    if (error instanceof ZodError) {
      res.status(400).json({
        message: 'Eingaben ungültig',
        issues: error.issues.map((x) => ({ field: x.path.join('.'), message: x.message })),
      });
      return;
    }
    if (error instanceof HttpException) {
      res.status(error.getStatus()).json({ message: error.message });
      return;
    }
    if (['23505', '23P01', '40P01', '40001'].includes(error.code)) {
      res.status(409).json({
        message: 'Konflikt mit einer bestehenden Buchung oder Transaktion. Bitte erneut prüfen.',
      });
      return;
    }
    console.error('Request failed', error.code || error.name);
    res.status(500).json({ message: 'Interner Fehler. Bitte erneut versuchen.' });
  }
}
@Module({ controllers: [ApiController], providers: [StaffGuard] })
class AppModule {}
const app = await NestFactory.create<NestExpressApplication>(AppModule);
if (process.env.NODE_ENV === 'production') {
  if (new URL(publicUrl).protocol !== 'https:')
    throw new Error('Production PUBLIC_URL must use HTTPS');
  app.set('trust proxy', 1);
}
app.use(helmet({ referrerPolicy: { policy: 'no-referrer' } }));
app.use(cookieParser());
const buckets = new Map<string, { count: number; until: number }>();
app.use((req: Request, res: Response, next: NextFunction) => {
  if (!['GET', 'HEAD', 'OPTIONS'].includes(req.method)) {
    if (req.headers.origin !== new URL(publicUrl).origin) {
      res.status(403).json({ message: 'Ungültiger Ursprung' });
      return;
    }
    const key = `${req.ip}:${req.path === '/api/login' ? 'login' : 'write'}`,
      now = Date.now();
    const bucket = buckets.get(key) || { count: 0, until: now + 60000 };
    if (bucket.until < now) {
      bucket.count = 0;
      bucket.until = now + 60000;
    }
    bucket.count++;
    buckets.set(key, bucket);
    if (bucket.count > (req.path === '/api/login' ? 10 : 120)) {
      res.status(429).json({ message: 'Zu viele Anfragen. Bitte kurz warten.' });
      return;
    }
    if (buckets.size > 10000) for (const [k, v] of buckets) if (v.until < now) buckets.delete(k);
  }
  res.setHeader('Cache-Control', 'no-store');
  next();
});
app.useStaticAssets(fileURLToPath(new URL('../../web/dist', import.meta.url)));
app.useGlobalFilters(new Errors());
app.enableShutdownHooks();
await app.listen(Number(process.env.PORT || 3000), '0.0.0.0');
let running = false;
const timer = setInterval(async () => {
  if (running) return;
  running = true;
  try {
    await tick();
  } catch (error: any) {
    console.error('Background job failed', error.code || error.name);
  } finally {
    running = false;
  }
}, 15000);
timer.unref();
