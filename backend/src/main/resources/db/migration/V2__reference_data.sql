-- Reference data: roles and the standard material list

INSERT INTO roles (name, description) VALUES
    ('ADMIN', 'Full system access'),
    ('MANAGER', 'Operations, accounts and reports'),
    ('SALES_EXECUTIVE', 'Customers, sales and collections');

INSERT INTO materials (material_code, material_name, description, unit)
SELECT 'MAT-' || lpad(nextval('seq_material')::text, 4, '0'), m.name, m.descr, 'TON'
FROM (VALUES
    ('Dust', 'Crusher dust'),
    ('10 MM', '10 mm aggregate'),
    ('20 MM', '20 mm aggregate'),
    ('40 MM', '40 mm aggregate'),
    ('Boulder', 'Raw boulder stone'),
    ('M-Sand', 'Manufactured sand'),
    ('P-Sand', 'Plastering sand')
) AS m (name, descr);

INSERT INTO inventory (material_id) SELECT id FROM materials;
