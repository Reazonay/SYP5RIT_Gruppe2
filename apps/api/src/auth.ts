import {
  CanActivate,
  ExecutionContext,
  ForbiddenException,
  Inject,
  Injectable,
  SetMetadata,
  UnauthorizedException,
} from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { hash, pool } from './db.js';
export const Roles = (...roles: string[]) => SetMetadata('roles', roles);
@Injectable()
export class StaffGuard implements CanActivate {
  constructor(@Inject(Reflector) private readonly reflector: Reflector) {}
  async canActivate(context: ExecutionContext) {
    const roles = this.reflector.get<string[]>('roles', context.getHandler());
    if (!roles) return true;
    const req = context.switchToHttp().getRequest();
    const result = await pool.query(
      'SELECT s.id,s.role,s.email FROM sessions x JOIN staff s ON s.id=x.staff_id WHERE x.token_hash=$1 AND expires_at>now()',
      [hash(req.cookies?.session || '')],
    );
    if (!result.rowCount) throw new UnauthorizedException('Bitte anmelden');
    req.staff = result.rows[0];
    if (!roles.includes(req.staff.role)) throw new ForbiddenException('Keine Berechtigung');
    return true;
  }
}
