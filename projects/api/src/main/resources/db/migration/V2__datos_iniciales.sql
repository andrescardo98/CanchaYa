-- =====================================================================
-- V2 · Datos iniciales de CanchaYa
-- ---------------------------------------------------------------------
-- 1. Ubicación: el departamento de Antioquia y sus 125 municipios (D16, RNG-07).
--    Fuente: DIVIPOLA del DANE publicada en datos.gov.co (conjunto gdxc-w37w).
-- 2. Escenarios y canchas: los tipos de cancha F5, F7 y F11 (D04).
-- 3. Identidad y acceso: el administrador del sistema (RN-16), que no se
--    puede registrar por la aplicación.
--
-- Los identificadores son UUID fijos (versión 5, calculados a partir del
-- código de cada registro). Así son los mismos en local, en las pruebas y
-- en Supabase, y se pueden usar en las colecciones de Bruno.
--
-- Las tablas ya tienen RLS: en Supabase lo activa el disparador ensure_rls
-- al crearlas en la V1 (D35, D38).
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Ubicación
-- ---------------------------------------------------------------------
INSERT INTO departamento (id, codigo, nombre) VALUES
    ('16b4a874-5a85-560b-83c8-c9e29247327d', '05', 'Antioquia');

INSERT INTO ciudad (id, codigo, departamento_id, nombre) VALUES
    ('13058531-ae4e-59a0-8b22-2ec93181e811', '05001', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Medellín'),
    ('a93b83dd-9f36-5cee-ae3f-76b153d44973', '05002', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Abejorral'),
    ('8f6ce64c-18e4-5ee3-b4f3-6f369738f9b1', '05004', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Abriaquí'),
    ('b7d2b457-4d38-5402-9d78-9d9f8c00d04b', '05021', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Alejandría'),
    ('7325428a-b767-59eb-afa8-15208f5d352a', '05030', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Amagá'),
    ('b0d60e2e-790e-515f-a2c6-3a0f43e70bc1', '05031', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Amalfi'),
    ('8ac695e3-0b2d-562b-b88b-8a2f984791eb', '05034', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Andes'),
    ('ca341b99-8cbb-54a6-b7b2-cc2b1a2ab8b4', '05036', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Angelópolis'),
    ('0db4dfc7-3f1b-5962-b3f2-b090628f66ee', '05038', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Angostura'),
    ('03f3ffdb-fedf-52e8-946b-9190029b302e', '05040', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Anorí'),
    ('3e7b14c8-dd3b-5709-ae82-6aef4c465b74', '05042', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Santa Fé de Antioquia'),
    ('ec9c3d2f-03fe-5482-b194-594a9cbb8aeb', '05044', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Anzá'),
    ('49bae103-74eb-5aaf-9ac9-b82e2feb0493', '05045', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Apartadó'),
    ('f7f6968d-07c3-54c2-8b5a-e0eae291431c', '05051', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Arboletes'),
    ('993a7095-ed6e-505a-8a3e-fc393e2fa236', '05055', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Argelia'),
    ('d874494a-1fbc-5709-94ee-3384d78e0b41', '05059', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Armenia'),
    ('bc28f3a8-ae36-51df-815f-c62d69143150', '05079', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Barbosa'),
    ('8b4464a6-c5c6-5f65-849e-4e8ec828b9dc', '05086', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Belmira'),
    ('1370b963-019c-588b-b129-609b8b79888b', '05088', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Bello'),
    ('978b0f76-601f-567d-9f43-2426e3ab113b', '05091', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Betania'),
    ('34bb0681-c624-5918-9f33-344276e81168', '05093', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Betulia'),
    ('a44f61a6-8238-5dd7-9638-ad5d0b2a5f10', '05101', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Ciudad Bolívar'),
    ('95bef242-6c5a-519b-8000-bf7f7113b4a7', '05107', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Briceño'),
    ('e1ecaba4-08a6-5075-a718-60b36fa588ff', '05113', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Buriticá'),
    ('30cedfca-dc17-510e-b0df-5029a97b7d02', '05120', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Cáceres'),
    ('6389f30f-2a01-5931-be04-3c11bc209643', '05125', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Caicedo'),
    ('5fd98fd9-8d01-5ade-b107-56473dd4bd30', '05129', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Caldas'),
    ('3564958b-8d87-5db3-a633-ec696344a21a', '05134', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Campamento'),
    ('50cdcb26-75d2-5bbb-b46d-c2f496dba6c0', '05138', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Cañasgordas'),
    ('9bf584c6-f885-537d-9cd6-a3a1b3d8b3b9', '05142', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Caracolí'),
    ('2bcc1d92-bfaf-5f09-b840-d6e9e59b099b', '05145', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Caramanta'),
    ('20df93ff-80be-5284-96fd-3fbb417e5b0a', '05147', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Carepa'),
    ('2b9cd977-c2cf-5208-8191-6238a0831d26', '05148', '16b4a874-5a85-560b-83c8-c9e29247327d', 'El Carmen de Viboral'),
    ('b014c527-f9bf-5083-bee4-371300f9994c', '05150', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Carolina'),
    ('97a4e778-7d4a-5c83-8166-5f43c9ee13f8', '05154', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Caucasia'),
    ('2a59e400-9139-546d-bf75-48b9ed1011cc', '05172', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Chigorodó'),
    ('eec8ff38-274e-5b9e-a26f-93eb1c81f272', '05190', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Cisneros'),
    ('abf1005a-428d-59c1-91f9-563b24db589e', '05197', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Cocorná'),
    ('5ead07c6-d9a8-57ff-babf-dc4035ba9ab1', '05206', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Concepción'),
    ('aceb26a0-afcf-52ee-b537-6062977268c3', '05209', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Concordia'),
    ('270249b3-73f4-57eb-a5da-689889d0aca9', '05212', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Copacabana'),
    ('236591ee-235e-5afc-a862-f31d96b31c35', '05234', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Dabeiba'),
    ('aef5be95-fb1b-5c48-8a87-6cb075f2aedd', '05237', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Donmatías'),
    ('f8f49e32-7b7b-56b5-b6fb-58669d57a62c', '05240', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Ebéjico'),
    ('6dc878ab-8930-5df8-80de-fed8602d8b20', '05250', '16b4a874-5a85-560b-83c8-c9e29247327d', 'El Bagre'),
    ('73434541-dd41-5c5f-a1dd-59ea84f8b4ad', '05264', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Entrerríos'),
    ('7e94e049-133d-53f1-aab4-e57ee11e90a1', '05266', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Envigado'),
    ('d3a2db0e-8daf-554b-bc9d-93cc2e723a15', '05282', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Fredonia'),
    ('b7f29793-f291-5a0e-ac4c-a0cdf7364c42', '05284', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Frontino'),
    ('a9c10a81-f7b0-5d39-915e-6cad91a31479', '05306', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Giraldo'),
    ('16c0f793-3b3f-5d3e-9c34-ff4bd0dd4dc5', '05308', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Girardota'),
    ('02640bdb-e5e5-59be-8ce2-577d7e35140c', '05310', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Gómez Plata'),
    ('94f94abb-fc35-5889-86be-1e5476403efc', '05313', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Granada'),
    ('28b6ad8f-6fc0-5f29-b379-aab58700f815', '05315', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Guadalupe'),
    ('68bcd2eb-a394-56a2-b680-2e9e9db34cf7', '05318', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Guarne'),
    ('0082aad6-c774-50cf-9a47-67731542e25f', '05321', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Guatapé'),
    ('37f87232-ac31-58a6-b059-a358b260a07a', '05347', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Heliconia'),
    ('d4bc67cf-2b62-5227-8adf-fdae601c1aa5', '05353', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Hispania'),
    ('4009c699-d365-53e2-8df8-9294579331f2', '05360', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Itagüí'),
    ('e855d6a7-de5d-55dd-b9f3-1ffee3e869ee', '05361', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Ituango'),
    ('ee85b378-eea5-5610-bfb3-d7d4d4e3180e', '05364', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Jardín'),
    ('ad33cd65-5f46-5e79-824a-a2d4235d1a10', '05368', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Jericó'),
    ('bec6869d-b26b-51be-a4ee-792bd42d6623', '05376', '16b4a874-5a85-560b-83c8-c9e29247327d', 'La Ceja'),
    ('47221d3d-a4fb-50b0-a955-4755fc913a38', '05380', '16b4a874-5a85-560b-83c8-c9e29247327d', 'La Estrella'),
    ('ae944660-e979-5b42-93f6-631089bc8689', '05390', '16b4a874-5a85-560b-83c8-c9e29247327d', 'La Pintada'),
    ('e1f34416-41eb-55a5-9a8d-df1ded8fcbf6', '05400', '16b4a874-5a85-560b-83c8-c9e29247327d', 'La Unión'),
    ('68a809f0-73e4-5ef5-b57c-73cb5a5ad35f', '05411', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Liborina'),
    ('668f23a2-dc50-53af-bbf5-ce1ffb1ae77f', '05425', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Maceo'),
    ('ec37be2e-1820-56b4-a18e-27b9899ca17c', '05440', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Marinilla'),
    ('f0fbf936-fb90-5fbe-9343-b73bd00666b8', '05467', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Montebello'),
    ('a8893d34-2bce-5d2a-9510-4d1273b9bb45', '05475', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Murindó'),
    ('f33415d8-1528-59ad-acb0-abc66cef6856', '05480', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Mutatá'),
    ('2d49eaeb-79e7-5b5e-b65f-aff9e138d47c', '05483', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Nariño'),
    ('02f5754a-6701-509e-a40e-546eca1d0804', '05490', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Necoclí'),
    ('9388605d-6cea-5095-b329-3222332a5114', '05495', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Nechí'),
    ('04f21e4a-d157-589d-b677-a7e0f4f24ad3', '05501', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Olaya'),
    ('76daa32a-05ba-548e-b20b-42130319141d', '05541', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Peñol'),
    ('06f70489-2e5b-5956-b0a6-ef6616ec4736', '05543', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Peque'),
    ('808ec6c4-e49d-5629-b0e1-22299558122c', '05576', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Pueblorrico'),
    ('7211cec4-289f-55ee-a05b-fab8aeb11cde', '05579', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Puerto Berrío'),
    ('9009c5dd-a0ca-5c6f-8c3e-3f762de8208a', '05585', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Puerto Nare'),
    ('09c88fa7-0fe9-5448-a71d-713be15d83fa', '05591', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Puerto Triunfo'),
    ('d0947e6e-b22d-5ff3-a102-69bcd69005c7', '05604', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Remedios'),
    ('3bf56d07-7c18-510a-b3fe-7516ebfaa6af', '05607', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Retiro'),
    ('c7f2d2ac-a264-597b-b927-c65662edf6a1', '05615', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Rionegro'),
    ('b0369a6d-581b-5e7c-af8f-d6b921cb124d', '05628', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Sabanalarga'),
    ('c57ab9f4-11ef-55f6-a343-a3f922eb5311', '05631', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Sabaneta'),
    ('25af935e-0f39-59b2-b737-015baa8c756f', '05642', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Salgar'),
    ('f80a483c-7c0c-5111-b38b-317fa875d17f', '05647', '16b4a874-5a85-560b-83c8-c9e29247327d', 'San Andrés de Cuerquía'),
    ('66b2a3c1-6e42-5021-ab94-6b07699ee5ad', '05649', '16b4a874-5a85-560b-83c8-c9e29247327d', 'San Carlos'),
    ('81d12d21-7c87-5636-8d63-0289bbd911d8', '05652', '16b4a874-5a85-560b-83c8-c9e29247327d', 'San Francisco'),
    ('0900cec9-61ac-576c-a76a-79bc19a5f398', '05656', '16b4a874-5a85-560b-83c8-c9e29247327d', 'San Jerónimo'),
    ('3ded0344-3d58-542c-8edf-016aefc68bf5', '05658', '16b4a874-5a85-560b-83c8-c9e29247327d', 'San José de la Montaña'),
    ('e4f807df-da5c-5c23-a479-7fe06ebd15e4', '05659', '16b4a874-5a85-560b-83c8-c9e29247327d', 'San Juan de Urabá'),
    ('a6d0eff4-0690-5f07-9e58-d64574f48b1c', '05660', '16b4a874-5a85-560b-83c8-c9e29247327d', 'San Luis'),
    ('916d507c-22b4-535b-85aa-27197a8b6dd9', '05664', '16b4a874-5a85-560b-83c8-c9e29247327d', 'San Pedro de los Milagros'),
    ('25a3fdb6-25b8-5447-a086-4604e2b4724c', '05665', '16b4a874-5a85-560b-83c8-c9e29247327d', 'San Pedro de Urabá'),
    ('1eaf8fe5-ed20-588b-90c3-4167b7067251', '05667', '16b4a874-5a85-560b-83c8-c9e29247327d', 'San Rafael'),
    ('d8ab14ab-6d29-5dde-a05b-8b771ad25957', '05670', '16b4a874-5a85-560b-83c8-c9e29247327d', 'San Roque'),
    ('ac2b21d8-9099-5a73-af08-b4a228f29402', '05674', '16b4a874-5a85-560b-83c8-c9e29247327d', 'San Vicente Ferrer'),
    ('8a916212-8d7a-560e-a7be-87db09e322ef', '05679', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Santa Bárbara'),
    ('fb82fc56-99a2-551b-84cb-35bf7f490034', '05686', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Santa Rosa de Osos'),
    ('7e7d3bf0-dfac-5173-94b8-7edd21e4bcb6', '05690', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Santo Domingo'),
    ('834594f7-660b-5e2c-b1d0-106a2fe3a99a', '05697', '16b4a874-5a85-560b-83c8-c9e29247327d', 'El Santuario'),
    ('df5396db-ba18-54b4-b5fb-9fa9f421f78e', '05736', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Segovia'),
    ('a7f59b19-2dcd-5ae9-b94b-5aabed60a1f2', '05756', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Sonsón'),
    ('fc30f883-a24f-55ad-abda-ddd004a51cb4', '05761', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Sopetrán'),
    ('a080d05f-8267-552e-9bc8-2484a6a5404a', '05789', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Támesis'),
    ('c4c8b50b-a917-5e4b-b1d4-46a97b45b78d', '05790', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Tarazá'),
    ('a99e852c-02ed-5bd4-ac57-c40101735054', '05792', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Tarso'),
    ('67782ff1-8e55-5f40-9308-384c4f2d47bf', '05809', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Titiribí'),
    ('804d364a-021c-5eba-ad08-03efa8a9bde9', '05819', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Toledo'),
    ('f9f0f9f3-8586-5cb3-9ce8-8e76800862d1', '05837', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Turbo'),
    ('4e9ec047-f89a-536d-9182-bd0a6a918510', '05842', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Uramita'),
    ('f2a8a533-7eda-538b-8b9d-c234f4587a9d', '05847', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Urrao'),
    ('123ef5a8-c4b9-59a9-9daa-926d3b56248f', '05854', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Valdivia'),
    ('20eb7222-ead6-5b3f-986a-140359fbe2f3', '05856', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Valparaíso'),
    ('576ff85f-1197-50c9-9050-7bacd2430331', '05858', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Vegachí'),
    ('4d9ec964-dabb-591f-b674-7e4b0ce900c6', '05861', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Venecia'),
    ('bf253979-427a-5905-a78e-33e92a16f063', '05873', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Vigía del Fuerte'),
    ('7948d8a1-db9a-5e0f-ac1c-d2fcd9318fec', '05885', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Yalí'),
    ('18f6193a-1481-5d98-9101-399f1ab6e1b1', '05887', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Yarumal'),
    ('6d80ce2f-0b8b-5ac7-81c5-580e336c33e1', '05890', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Yolombó'),
    ('45348252-2778-5991-b2cc-cbc4e0089e4e', '05893', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Yondó'),
    ('4421ae54-bac1-5a91-ab5e-6ed5774f4787', '05895', '16b4a874-5a85-560b-83c8-c9e29247327d', 'Zaragoza');
-- ---------------------------------------------------------------------
-- 2. Tipos de cancha (capacidad = jugadores de los dos equipos)
-- ---------------------------------------------------------------------
INSERT INTO tipo_cancha (id, codigo, nombre, capacidad_jugadores) VALUES
    ('27d013e3-f5b3-5378-a7df-7c80429554fe', 'F5',  'Fútbol 5',  10),
    ('91339eab-b705-528d-b608-55f9f44c044d', 'F7',  'Fútbol 7',  14),
    ('b0a3fcd9-41c9-5171-8085-26d1c319f290', 'F11', 'Fútbol 11', 22);

-- ---------------------------------------------------------------------
-- 3. Administrador del sistema (RN-16)
-- Sus datos personales y el hash bcrypt de su contraseña llegan como
-- variables de entorno (placeholders de Flyway, ver application.properties):
-- nunca se escriben en el repositorio. En Render van como secretos (RT-10).
-- ---------------------------------------------------------------------
INSERT INTO usuario (id, nombre, correo_electronico, contrasena_hash, tipo_documento,
                     numero_documento, numero_celular, rol, fecha_registro) VALUES
    ('60ac6f94-a634-58b9-97ee-c91cce2d43f4',
     'Administrador del sistema',
     lower('${adminCorreo}'),
     '${adminContrasenaHash}',
     'CC',
     '${adminNumeroDocumento}',
     '${adminNumeroCelular}',
     'ADMINISTRADOR_SISTEMA',
     now());
