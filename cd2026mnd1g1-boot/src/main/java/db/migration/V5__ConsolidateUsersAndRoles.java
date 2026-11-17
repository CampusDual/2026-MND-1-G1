package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Consolida los usuarios y roles históricos en las tablas operativas canónicas.
 */
public class V5__ConsolidateUsersAndRoles extends BaseJavaMigration {

    private static final String CENTER_CIF = "B00000000";
    private static final String DEMO_PASSWORD = "admin1234";
    private static final String ADMIN_EMAIL = "admin@guarderiademo.test";

    /**
     * Migra los usuarios heredados, unifica las tablas y crea las cuentas de demostración.
     *
     * @param context contexto JDBC de Flyway
     * @throws SQLException si falla la consolidación de datos
     */
    @Override
    public void migrate(Context context) throws SQLException {
        Connection connection = context.getConnection();
        renameOperationalUserTable(connection);
        Long centerId = findCenterId(connection);
        Long adminRoleId = ensureRole(connection, "ADMIN", "Administración del centro");
        Long receptionistRoleId = ensureRole(connection, "RECEPCIONISTA", "Personal de recepción");
        Long tutorRoleId = ensureRole(connection, "TUTOR", "Tutor de perros");
        ensureRole(connection, "GERENTE", "Gerencia del centro");
        addUsernameColumn(connection);

        seedDefaultAdmin(connection, centerId, adminRoleId);
        migrateLegacyUsers(connection, centerId, adminRoleId);
        seedDemoUser(
                connection, centerId, receptionistRoleId, "recepcionista",
                "recepcionista@guarderiademo.test", "Recepcionista", "Demo", "00000001R"
        );
        seedDemoUser(
                connection, centerId, tutorRoleId, "tutor",
                "tutor@guarderiademo.test", "Tutor", "Demo", "00000002T"
        );

        ensureAdminPermission(connection, adminRoleId);
        enforceUsernameConstraint(connection);
        dropLegacyTables(connection);
    }

    /**
     * Renombra la tabla operativa al nombre canónico.
     *
     * @param connection conexión JDBC
     * @throws SQLException si falla el cambio de nombre
     */
    private void renameOperationalUserTable(Connection connection) throws SQLException {
        String oldName = findTableName(connection, "usuario");
        String newName = findTableName(connection, "USUARIOS");
        if (oldName != null && newName == null) {
            execute(connection, "ALTER TABLE " + quote(oldName) + " RENAME TO USUARIOS");
        } else if (oldName == null && newName == null) {
            throw new SQLException("No existe la tabla operativa usuario para consolidar");
        }
    }

    /**
     * Añade el nombre de acceso y prepara los usuarios operativos existentes.
     *
     * @param connection conexión JDBC
     * @throws SQLException si falla la actualización de columnas
     */
    private void addUsernameColumn(Connection connection) throws SQLException {
        String table = requireTable(connection, "USUARIOS");
        if (!columnExists(connection, table, "username")) {
            execute(connection, "ALTER TABLE " + quote(table) + " ADD COLUMN username VARCHAR(80)");
        }
        populateUsernames(connection, table);
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE " + quote(table) + " SET username = 'admin', "
                        + "rol_id = (SELECT id FROM rol WHERE nombre = 'ADMIN'), password_hash = ? "
                        + "WHERE email = ?"
        )) {
            statement.setString(1, encodeDemoPassword());
            statement.setString(2, ADMIN_EMAIL);
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE " + quote(table) + " SET password_hash = ?"
        )) {
            statement.setString(1, encodeDemoPassword());
            statement.executeUpdate();
        }
    }

    /**
     * Inicializa los nombres de acceso existentes respetando la longitud máxima.
     *
     * @param connection conexión JDBC
     * @param table tabla de usuarios
     * @throws SQLException si falla la lectura o actualización
     */
    private void populateUsernames(Connection connection, String table) throws SQLException {
        List<ExistingUser> users = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT id, email FROM " + quote(table) + " WHERE username IS NULL OR username = ''"
             )) {
            while (resultSet.next()) {
                users.add(new ExistingUser(resultSet.getLong("id"), resultSet.getString("email")));
            }
        }

        for (ExistingUser user : users) {
            if (user.email() == null || user.email().isBlank()) {
                throw new SQLException("La tabla USUARIOS contiene un usuario sin email");
            }
            String candidate = user.email();
            if (candidate.length() > 80) {
                String suffix = "." + user.id();
                candidate = candidate.substring(0, 80 - suffix.length()) + suffix;
            }
            int suffixNumber = 1;
            while (usernameBelongsToAnotherUser(connection, table, candidate, user.id())) {
                String suffix = "_" + suffixNumber++;
                candidate = user.email().substring(0, Math.min(user.email().length(), 80 - suffix.length()))
                        + suffix;
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE " + quote(table) + " SET username = ? WHERE id = ?"
            )) {
                statement.setString(1, candidate);
                statement.setLong(2, user.id());
                statement.executeUpdate();
            }
        }
    }

    /**
     * Comprueba que un nombre de acceso no pertenezca a otra fila.
     *
     * @param connection conexión JDBC
     * @param table tabla de usuarios
     * @param username nombre de acceso
     * @param userId identificador excluido
     * @return true si el nombre ya pertenece a otro usuario
     * @throws SQLException si falla la consulta
     */
    private boolean usernameBelongsToAnotherUser(
            Connection connection,
            String table,
            String username,
            Long userId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM " + quote(table) + " WHERE username = ? AND id <> ?"
        )) {
            statement.setString(1, username);
            statement.setLong(2, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    /**
     * Busca el centro demo al que se asocian los usuarios sembrados.
     *
     * @param connection conexión JDBC
     * @return identificador del centro
     * @throws SQLException si no existe el centro demo
     */
    private Long findCenterId(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM centro WHERE cif = ?"
        )) {
            statement.setString(1, CENTER_CIF);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
            }
        }
        throw new SQLException("No existe el centro demo necesario para migrar los usuarios");
    }

    /**
     * Busca o crea un rol canónico.
     *
     * @param connection conexión JDBC
     * @param roleName nombre del rol
     * @param description descripción del rol
     * @return identificador del rol
     * @throws SQLException si falla la consulta o inserción
     */
    private Long ensureRole(Connection connection, String roleName, String description) throws SQLException {
        Long roleId = findId(connection, "SELECT id FROM rol WHERE nombre = ?", roleName);
        if (roleId != null) {
            return roleId;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO rol (nombre, descripcion) VALUES (?, ?)"
        )) {
            statement.setString(1, roleName);
            statement.setString(2, description);
            statement.executeUpdate();
        }
        return findId(connection, "SELECT id FROM rol WHERE nombre = ?", roleName);
    }

    /**
     * Asegura que la cuenta admin de V4 tenga username admin y el rol ADMIN.
     *
     * @param connection conexión JDBC
     * @param centerId centro demo
     * @param adminRoleId identificador del rol admin
     * @throws SQLException si falla la actualización o inserción
     */
    private void seedDefaultAdmin(Connection connection, Long centerId, Long adminRoleId) throws SQLException {
        Long adminId = findUserId(connection, "username", "admin");
        if (adminId == null) {
            adminId = findUserId(connection, "email", ADMIN_EMAIL);
        }
        if (adminId == null) {
            insertUser(
                    connection, centerId, adminRoleId, "admin", ADMIN_EMAIL,
                    "Admin", "Guardería Demo", "00000000T", true
            );
            return;
        }
        updateUser(
                connection, adminId, adminRoleId, "admin", ADMIN_EMAIL,
                encodeDemoPassword(), true
        );
    }

    /**
     * Copia los usuarios de las tablas de autenticación históricas al esquema canónico.
     *
     * @param connection conexión JDBC
     * @param centerId centro demo
     * @param defaultRoleId rol por defecto cuando no se encuentra una asignación
     * @throws SQLException si falla una consulta o la copia de datos
     */
    private void migrateLegacyUsers(Connection connection, Long centerId, Long defaultRoleId)
            throws SQLException {
        String legacyUsers = findTableName(connection, "users");
        if (legacyUsers == null) {
            return;
        }
        List<LegacyUser> users = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT id, username, enabled FROM " + quote(legacyUsers)
             )) {
            while (resultSet.next()) {
                users.add(new LegacyUser(
                        resultSet.getLong("id"),
                        resultSet.getString("username"),
                        resultSet.getBoolean("enabled")
                ));
            }
        }

        for (LegacyUser legacyUser : users) {
            if (legacyUser.username() == null || legacyUser.username().isBlank()) {
                throw new SQLException("La tabla users contiene un usuario sin username");
            }
            String username = legacyUser.username().trim().toLowerCase(Locale.ROOT);
            Long roleId = resolveLegacyRole(connection, legacyUser.id(), defaultRoleId);
            String email = resolveEmail(username, legacyUser.id());
            if ("admin".equals(username) || ADMIN_EMAIL.equalsIgnoreCase(username)) {
                Long adminId = findUserId(connection, "email", ADMIN_EMAIL);
                if (adminId != null) {
                    updateUser(
                            connection, adminId, roleId, "admin", ADMIN_EMAIL,
                            encodeDemoPassword(), legacyUser.enabled()
                    );
                    continue;
                }
                username = "admin";
                email = ADMIN_EMAIL;
            }

            Long existingId = findUserId(connection, "username", username);
            if (existingId == null) {
                existingId = findUserId(connection, "email", email);
            }
            if (existingId != null) {
                updateUser(
                        connection, existingId, roleId, username, email,
                        encodeDemoPassword(), legacyUser.enabled()
                );
            } else {
                insertUser(
                        connection, centerId, roleId, username, email,
                        "Usuario", "Demo", uniqueNationalId(connection, legacyNationalId(legacyUser.id())),
                        legacyUser.enabled()
                );
            }
        }
    }

    /**
     * Resuelve el rol único que recibirá un usuario histórico.
     *
     * @param connection conexión JDBC
     * @param userId identificador del usuario histórico
     * @param defaultRoleId rol por defecto
     * @return identificador del rol
     * @throws SQLException si falla la consulta
     */
    private Long resolveLegacyRole(Connection connection, Long userId, Long defaultRoleId)
            throws SQLException {
        String legacyUserRoles = findTableName(connection, "user_roles");
        String legacyRoles = findTableName(connection, "roles");
        if (legacyUserRoles == null || legacyRoles == null) {
            return defaultRoleId;
        }
        String roleName = null;
        String query = "SELECT old_role.name FROM " + quote(legacyRoles) + " old_role "
                + "JOIN " + quote(legacyUserRoles) + " old_user_role "
                + "ON old_user_role.role_id = old_role.id "
                + "WHERE old_user_role.user_id = ? ORDER BY old_role.name";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    roleName = resultSet.getString(1);
                }
            }
        }
        if (roleName == null || roleName.isBlank()) {
            return defaultRoleId;
        }
        String normalizedName = roleName.trim().toUpperCase(Locale.ROOT);
        if (normalizedName.startsWith("ROLE_")) {
            normalizedName = normalizedName.substring("ROLE_".length());
        }
        String description = "Rol migrado de la tabla histórica";
        return ensureRole(connection, normalizedName, description);
    }

    /**
     * Crea o actualiza una cuenta de demostración.
     *
     * @param connection conexión JDBC
     * @param centerId centro demo
     * @param roleId rol asignado
     * @param username nombre de acceso
     * @param email correo
     * @param firstName nombre
     * @param lastName apellidos
     * @param nationalId DNI
     * @throws SQLException si falla la escritura
     */
    private void seedDemoUser(
            Connection connection,
            Long centerId,
            Long roleId,
            String username,
            String email,
            String firstName,
            String lastName,
            String nationalId
    ) throws SQLException {
        Long userId = findUserId(connection, "username", username);
        if (userId == null) {
            userId = findUserId(connection, "email", email);
        }
        if (userId == null) {
            insertUser(
                    connection, centerId, roleId, username, email, firstName, lastName,
                    uniqueNationalId(connection, nationalId), true
            );
        } else {
            updateUser(connection, userId, roleId, username, email, encodeDemoPassword(), true);
        }
    }

    /**
     * Inserta un usuario con las credenciales de demostración.
     *
     * @param connection conexión JDBC
     * @param centerId centro asociado
     * @param roleId rol asignado
     * @param username nombre de acceso
     * @param email correo
     * @param firstName nombre
     * @param lastName apellidos
     * @param nationalId DNI
     * @param enabled estado activo
     * @throws SQLException si falla la inserción
     */
    private void insertUser(
            Connection connection,
            Long centerId,
            Long roleId,
            String username,
            String email,
            String firstName,
            String lastName,
            String nationalId,
            boolean enabled
    ) throws SQLException {
        String table = requireTable(connection, "USUARIOS");
        String sql = "INSERT INTO " + quote(table)
                + " (centro_id, rol_id, nombre, apellidos, dni, email, password_hash, activo, username) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, centerId);
            statement.setLong(2, roleId);
            statement.setString(3, firstName);
            statement.setString(4, lastName);
            statement.setString(5, nationalId);
            statement.setString(6, email);
            statement.setString(7, encodeDemoPassword());
            statement.setBoolean(8, enabled);
            statement.setString(9, username);
            statement.executeUpdate();
        }
    }

    /**
     * Actualiza una cuenta migrada y su rol único.
     *
     * @param connection conexión JDBC
     * @param userId identificador del usuario
     * @param roleId rol asignado
     * @param username nombre de acceso
     * @param email correo
     * @param passwordHash contraseña codificada
     * @param enabled estado activo
     * @throws SQLException si falla la actualización
     */
    private void updateUser(
            Connection connection,
            Long userId,
            Long roleId,
            String username,
            String email,
            String passwordHash,
            boolean enabled
    ) throws SQLException {
        String table = requireTable(connection, "USUARIOS");
        String sql = "UPDATE " + quote(table)
                + " SET rol_id = ?, username = ?, email = ?, password_hash = ?, activo = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, roleId);
            statement.setString(2, username);
            statement.setString(3, email);
            statement.setString(4, passwordHash);
            statement.setBoolean(5, enabled);
            statement.setLong(6, userId);
            statement.executeUpdate();
        }
    }

    /**
     * Busca un usuario por username o email.
     *
     * @param connection conexión JDBC
     * @param field campo de búsqueda permitido
     * @param value valor buscado
     * @return identificador del usuario o null
     * @throws SQLException si falla la consulta
     */
    private Long findUserId(Connection connection, String field, String value) throws SQLException {
        if (!"username".equals(field) && !"email".equals(field)) {
            throw new IllegalArgumentException("Campo de búsqueda no permitido");
        }
        String table = requireTable(connection, "USUARIOS");
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM " + quote(table) + " WHERE " + field + " = ?"
        )) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : null;
            }
        }
    }

    /**
     * Asegura el permiso administrativo asociado al rol ADMIN.
     *
     * @param connection conexión JDBC
     * @param adminRoleId identificador del rol admin
     * @throws SQLException si falla la inserción
     */
    private void ensureAdminPermission(Connection connection, Long adminRoleId) throws SQLException {
        Long permissionId = findId(connection, "SELECT id FROM permiso WHERE codigo = ?", "ADMIN_ALL");
        if (permissionId == null) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO permiso (codigo, descripcion) VALUES (?, ?)"
            )) {
                statement.setString(1, "ADMIN_ALL");
                statement.setString(2, "Acceso administrativo a las operaciones del centro");
                statement.executeUpdate();
            }
            permissionId = findId(connection, "SELECT id FROM permiso WHERE codigo = ?", "ADMIN_ALL");
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(1) FROM rol_permiso WHERE rol_id = ? AND permiso_id = ?"
        )) {
            statement.setLong(1, adminRoleId);
            statement.setLong(2, permissionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next() && resultSet.getLong(1) > 0) {
                    return;
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO rol_permiso (rol_id, permiso_id) VALUES (?, ?)"
        )) {
            statement.setLong(1, adminRoleId);
            statement.setLong(2, permissionId);
            statement.executeUpdate();
        }
    }

    /**
     * Impone username obligatorio y único después de migrar las filas históricas.
     *
     * @param connection conexión JDBC
     * @throws SQLException si falla la restricción
     */
    private void enforceUsernameConstraint(Connection connection) throws SQLException {
        String table = requireTable(connection, "USUARIOS");
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + quote(table) + " ALTER COLUMN username SET NOT NULL");
            statement.execute(
                    "ALTER TABLE " + quote(table)
                            + " ADD CONSTRAINT uq_usuarios_username UNIQUE (username)"
            );
        }
    }

    /**
     * Elimina las tablas históricas una vez copiados sus datos.
     *
     * @param connection conexión JDBC
     * @throws SQLException si falla la eliminación
     */
    private void dropLegacyTables(Connection connection) throws SQLException {
        dropTableIfExists(connection, "user_roles");
        dropTableIfExists(connection, "users");
        dropTableIfExists(connection, "roles");
    }

    /**
     * Elimina una tabla si todavía existe.
     *
     * @param connection conexión JDBC
     * @param tableName nombre de tabla
     * @throws SQLException si falla la eliminación
     */
    private void dropTableIfExists(Connection connection, String tableName) throws SQLException {
        String actualName = findTableName(connection, tableName);
        if (actualName != null) {
            execute(connection, "DROP TABLE " + quote(actualName));
        }
    }

    /**
     * Busca un identificador de rol o permiso.
     *
     * @param connection conexión JDBC
     * @param sql consulta parametrizada
     * @param value valor buscado
     * @return identificador o null
     * @throws SQLException si falla la consulta
     */
    private Long findId(Connection connection, String sql, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : null;
            }
        }
    }

    /**
     * Devuelve el correo de migración de un usuario histórico.
     *
     * @param username username heredado
     * @param userId identificador histórico para evitar colisiones
     * @return correo único de migración
     */
    private String resolveEmail(String username, Long userId) {
        if (username.contains("@")) {
            return username;
        }
        String localPart = username.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._+-]", ".");
        return localPart + "." + userId + "@guarderiademo.test";
    }

    /**
     * Genera un identificador breve y determinista para usuarios históricos.
     *
     * @param userId identificador histórico
     * @return DNI técnico que cabe en la columna destino
     */
    private String legacyNationalId(Long userId) {
        return "L" + Long.toString(userId, 36).toUpperCase(Locale.ROOT);
    }

    /**
     * Busca un DNI técnico libre para no colisionar con datos existentes.
     *
     * @param connection conexión JDBC
     * @param baseDni DNI base
     * @return DNI libre
     * @throws SQLException si falla la consulta
     */
    private String uniqueNationalId(Connection connection, String baseDni) throws SQLException {
        String candidate = baseDni;
        int suffix = 1;
        while (findId(connection, "SELECT id FROM " + quote(requireTable(connection, "USUARIOS"))
                + " WHERE dni = ?", candidate) != null) {
            String suffixText = "-" + suffix++;
            candidate = baseDni.substring(0, Math.min(baseDni.length(), 20 - suffixText.length())) + suffixText;
        }
        return candidate;
    }

    /**
     * Genera un hash BCrypt para las cuentas de demostración.
     *
     * @return hash de la contraseña de demostración
     */
    private String encodeDemoPassword() {
        return new BCryptPasswordEncoder().encode(DEMO_PASSWORD);
    }

    /**
     * Comprueba si una columna está presente.
     *
     * @param connection conexión JDBC
     * @param table nombre físico de tabla
     * @param columnName nombre de columna
     * @return true si existe
     * @throws SQLException si falla la lectura de metadatos
     */
    private boolean columnExists(Connection connection, String table, String columnName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(
                connection.getCatalog(), connection.getSchema(), table, null
        )) {
            while (columns.next()) {
                if (columnName.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Busca una tabla de forma insensible a mayúsculas.
     *
     * @param connection conexión JDBC
     * @param expectedName nombre esperado
     * @return nombre físico o null
     * @throws SQLException si falla la lectura de metadatos
     */
    private String findTableName(Connection connection, String expectedName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet tables = metadata.getTables(
                connection.getCatalog(), connection.getSchema(), "%", new String[]{"TABLE"}
        )) {
            while (tables.next()) {
                String actualName = tables.getString("TABLE_NAME");
                if (expectedName.equalsIgnoreCase(actualName)) {
                    return actualName;
                }
            }
        }
        return null;
    }

    /**
     * Exige que exista una tabla y devuelve su nombre físico.
     *
     * @param connection conexión JDBC
     * @param tableName nombre esperado
     * @return nombre físico
     * @throws SQLException si la tabla no existe
     */
    private String requireTable(Connection connection, String tableName) throws SQLException {
        String actualName = findTableName(connection, tableName);
        if (actualName == null) {
            throw new SQLException("No existe la tabla " + tableName);
        }
        return actualName;
    }

    /**
     * Escapa un identificador JDBC obtenido de los metadatos.
     *
     * @param identifier identificador SQL
     * @return identificador entrecomillado
     * @throws SQLException si falla el escape JDBC
     */
    private String quote(String identifier) throws SQLException {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    /**
     * Ejecuta una instrucción DDL.
     *
     * @param connection conexión JDBC
     * @param sql instrucción SQL
     * @throws SQLException si falla su ejecución
     */
    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private record ExistingUser(Long id, String email) {
    }

    private record LegacyUser(Long id, String username, boolean enabled) {
    }
}
