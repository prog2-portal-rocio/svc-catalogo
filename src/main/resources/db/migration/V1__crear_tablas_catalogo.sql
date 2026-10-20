CREATE TABLE categorias (
                            id BIGINT PRIMARY KEY,
                            name VARCHAR(255) NOT NULL,
                            description TEXT,
                            enabled BOOLEAN NOT NULL,
                            created_at TIMESTAMP WITH TIME ZONE,
                            updated_at TIMESTAMP WITH TIME ZONE,

                            CONSTRAINT chk_categoria_id_positivo CHECK (id > 0),
                            CONSTRAINT chk_categoria_name_no_vacio CHECK (TRIM(name) <> '')
);

CREATE TABLE profesionales (
                               id BIGINT PRIMARY KEY,
                               category_id BIGINT NOT NULL,
                               first_name VARCHAR(255) NOT NULL,
                               last_name VARCHAR(255) NOT NULL,
                               enabled BOOLEAN NOT NULL,
                               created_at TIMESTAMP WITH TIME ZONE,
                               updated_at TIMESTAMP WITH TIME ZONE,

                               CONSTRAINT fk_profesional_categoria
                                   FOREIGN KEY (category_id)
                                       REFERENCES categorias(id),

                               CONSTRAINT chk_profesional_id_positivo
                                   CHECK (id > 0),

                               CONSTRAINT chk_profesional_nombre_no_vacio
                                   CHECK (TRIM(first_name) <> ''),

                               CONSTRAINT chk_profesional_apellido_no_vacio
                                   CHECK (TRIM(last_name) <> '')
);

CREATE TABLE horarios_semanales (
                                    id BIGINT PRIMARY KEY,
                                    professional_id BIGINT NOT NULL,
                                    day_of_week VARCHAR(9) NOT NULL,
                                    start_time TIME NOT NULL,
                                    end_time TIME NOT NULL,
                                    slot_duration_minutes INTEGER NOT NULL,
                                    enabled BOOLEAN NOT NULL,
                                    created_at TIMESTAMP WITH TIME ZONE,
                                    updated_at TIMESTAMP WITH TIME ZONE,

                                    CONSTRAINT fk_horario_profesional
                                        FOREIGN KEY (professional_id)
                                            REFERENCES profesionales(id),

                                    CONSTRAINT chk_horario_id_positivo
                                        CHECK (id > 0),

                                    CONSTRAINT chk_horario_dia_valido
                                        CHECK (day_of_week IN (
                                                               'MONDAY', 'TUESDAY', 'WEDNESDAY',
                                                               'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'
                                            )),

                                    CONSTRAINT chk_horario_rango_valido
                                        CHECK (start_time < end_time),

                                    CONSTRAINT chk_horario_duracion_positiva
                                        CHECK (slot_duration_minutes > 0)
);

CREATE TABLE estado_sincronizacion (
                                       id BIGINT PRIMARY KEY,
                                       applied_version BIGINT,
                                       applied_at TIMESTAMP WITH TIME ZONE,
                                       last_attempt_at TIMESTAMP WITH TIME ZONE,
                                       phase VARCHAR(20) NOT NULL,
                                       safe_diagnostic VARCHAR(255),

                                       CONSTRAINT chk_estado_id
                                           CHECK (id = 1),

                                       CONSTRAINT chk_version_positiva
                                           CHECK (applied_version IS NULL OR applied_version >= 0),

                                       CONSTRAINT chk_fase_valida
                                           CHECK (phase IN (
                                                            'NOT_STARTED',
                                                            'IN_PROGRESS',
                                                            'SUCCESS',
                                                            'ERROR'
                                               ))
);

INSERT INTO estado_sincronizacion (id, phase, safe_diagnostic)
VALUES (1, 'NOT_STARTED', 'Sin sincronizaciones realizadas');
