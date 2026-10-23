DELETE FROM instances
WHERE NOT EXISTS (
  SELECT 1 FROM users WHERE users.id = instances.owner_id
);

DELETE FROM scripts
WHERE NOT EXISTS (
  SELECT 1 FROM instances WHERE instances.id = scripts.instance_id
);

DELETE FROM instance_audit_logs
WHERE NOT EXISTS (
  SELECT 1 FROM instances WHERE instances.id = instance_audit_logs.instance_id
) OR NOT EXISTS (
  SELECT 1 FROM users WHERE users.id = instance_audit_logs.user_id
);

DELETE FROM instance_desired_bots
WHERE NOT EXISTS (
  SELECT 1 FROM instances WHERE instances.id = instance_desired_bots.instance_id
);

DELETE FROM plugin_permission_grants
WHERE NOT EXISTS (
  SELECT 1 FROM users WHERE users.id = plugin_permission_grants.user_id
);
