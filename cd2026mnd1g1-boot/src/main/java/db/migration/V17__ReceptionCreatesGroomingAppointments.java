package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Permite crear citas internas sin peluquero ni lugar asignado y conserva su autoría.
 */
public class V17__ReceptionCreatesGroomingAppointments extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws SQLException {
        Connection connection = context.getConnection();
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE cita_peluqueria ADD COLUMN creado_por_usuario_id BIGINT");
            statement.execute("ALTER TABLE cita_peluqueria ADD COLUMN observaciones VARCHAR(2000)");
            statement.execute("""
                    ALTER TABLE cita_peluqueria
                    DROP CONSTRAINT ck_cita_peluqueria_estado
                    """);
            statement.execute("""
                    ALTER TABLE cita_peluqueria
                    ADD CONSTRAINT ck_cita_peluqueria_estado
                        CHECK (estado IN (
                            'PENDIENTE_ASIGNACION', 'SOLICITADA', 'CONFIRMADA', 'COMPLETADA', 'CANCELADA'
                        ))
                    """);
            statement.execute("""
                    ALTER TABLE cita_peluqueria
                    ALTER COLUMN peluquero_id DROP NOT NULL
                    """);
            statement.execute("""
                    ALTER TABLE cita_peluqueria
                    ALTER COLUMN lugar DROP NOT NULL
                    """);
            statement.execute("""
                    ALTER TABLE cita_peluqueria
                    ADD CONSTRAINT fk_cita_peluqueria_creado_por
                    FOREIGN KEY (creado_por_usuario_id) REFERENCES USUARIOS(id)
                    """);
        }
    }
}
