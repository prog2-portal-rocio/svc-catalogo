CREATE TABLE eventos_catalogo_procesados (
                                             event_id VARCHAR(255) PRIMARY KEY,
                                             version_catalogo BIGINT NOT NULL,
                                             processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                             CONSTRAINT chk_event_id_no_vacio
                                                 CHECK (TRIM(event_id) <> ''),

                                             CONSTRAINT chk_evento_version_no_negativa
                                                 CHECK (version_catalogo >= 0)
);
