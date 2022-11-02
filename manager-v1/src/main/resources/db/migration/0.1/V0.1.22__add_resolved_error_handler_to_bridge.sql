update BRIDGE
set definition = json_build_object(
    'error_handler', (definition->>'error_handler')::jsonb,
    'resolved_error_handler', (definition->>'error_handler')::jsonb
);