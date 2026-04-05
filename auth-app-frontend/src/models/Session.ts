export default interface Session {
  id: string;
  jti: string;
  createdAt: string;
  expiresAt: string;
  lastSeenAt?: string;
  revoked: boolean;
  userAgent?: string;
  ipAddress?: string;
  deviceLabel?: string;
}