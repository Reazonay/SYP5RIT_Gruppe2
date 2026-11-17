import { z } from 'zod';
export const person = z.object({
  name: z.string().trim().min(1).max(120),
  email: z.string().email().max(254),
  phone: z.string().trim().min(3).max(40),
  method: z.enum(['cash', 'card', 'paypal']),
});
export const reservation = z.object({
  startsAt: z.string().datetime({ offset: true }),
  duration: z.number().int().min(30).max(240),
  people: z.array(person).min(1).max(10),
});
export const uuid = z.string().uuid();
export const amount = z.number().int().positive().max(10000000);
