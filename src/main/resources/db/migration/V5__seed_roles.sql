-- Phase 2: seed the fixed set of platform roles.
-- Roles are a closed set defined by the application (see role.domain.RoleName),
-- not admin-creatable, so they are seeded here rather than through an API.

INSERT INTO roles (name, description) VALUES
    ('USER',            'Standard platform user preparing for exams'),
    ('CONTENT_CREATOR',  'Creates draft questions and study content'),
    ('REVIEWER',         'Reviews and approves submitted content'),
    ('ADMIN',            'Administers platform operations'),
    ('SUPER_ADMIN',      'Full unrestricted platform access')
ON CONFLICT (name) DO NOTHING;
