export enum ConfigKeys {
  VIDEO_PROVIDER = 'video.provider',
  CHAT_PROVIDER = 'chat.provider',
  TELEMETRY_ENABLED = 'telemetry.enabled',
  TELEMETRY_RULES = 'telemetry.activation.rules',
  NOTIFICATION_EMAIL = 'notification.email.enabled',
  NOTIFICATION_SMS = 'notification.sms.enabled',
  NOTIFICATION_WHATSAPP = 'notification.whatsapp.enabled',
  NOTIFICATION_PUSH = 'notification.push.enabled',
}

export function mfeConfigKey(remoteName: string): string {
  return `mfe.${remoteName}.enabled`;
}

export interface ConfigSnapshot {
  [key: string]: string | boolean | number | Record<string, unknown>;
}
