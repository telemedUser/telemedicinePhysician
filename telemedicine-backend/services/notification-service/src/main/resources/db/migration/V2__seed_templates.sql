INSERT INTO templates (id, template_key, locale, channel, subject, body, active)
VALUES
    ('tpl-link-es-sms', 'session-link', 'es', 'sms', NULL, 'Su enlace de telemedicina esta listo para la cita {{appointmentId}}.', TRUE),
    ('tpl-link-es-email', 'session-link', 'es', 'email', 'Enlace de telemedicina', 'Su enlace de telemedicina esta listo para la cita {{appointmentId}}.', TRUE),
    ('tpl-ready-es-sms', 'physician-ready', 'es', 'sms', NULL, 'El medico esta listo para atenderle. Cita {{appointmentId}}.', TRUE),
    ('tpl-doc-es-email', 'clinical-document-ready', 'es', 'email', 'Documento clinico disponible', 'Su documento {{documentType}} de la cita {{appointmentId}} esta disponible.', TRUE),
    ('tpl-survey-es-email', 'survey-request', 'es', 'email', 'Encuesta de satisfaccion', 'Por favor complete la encuesta para la cita {{appointmentId}}.', TRUE),
    ('tpl-reminder-es-sms', 'appointment-reminder', 'es', 'sms', NULL, 'Recordatorio: tiene una cita de telemedicina {{appointmentId}} pronto.', TRUE)
ON CONFLICT (template_key, locale, channel) DO NOTHING;
