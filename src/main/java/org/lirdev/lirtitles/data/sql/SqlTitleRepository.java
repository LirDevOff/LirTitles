package org.lirdev.lirtitles.data.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.lirdev.lirtitles.data.model.CustomTitle;
import org.lirdev.lirtitles.data.model.CustomTitleStatus;
import org.lirdev.lirtitles.data.model.PendingModerationEntry;
import org.lirdev.lirtitles.data.model.PlayerTitleData;
import org.lirdev.lirtitles.data.store.CustomTitleModerationStore;
import org.lirdev.lirtitles.data.store.CustomTitleStore;
import org.lirdev.lirtitles.data.store.CustomTitleTokenStore;
import org.lirdev.lirtitles.data.store.PlayerTitleDataStore;
import org.lirdev.lirtitles.data.store.SelectedFontStore;
import org.lirdev.lirtitles.data.store.SelectedTitleStore;
import org.lirdev.lirtitles.data.store.TitleOwnershipStore;

public final class SqlTitleRepository implements TitleOwnershipStore, SelectedTitleStore, SelectedFontStore, CustomTitleStore,
        CustomTitleModerationStore, CustomTitleTokenStore, PlayerTitleDataStore, AutoCloseable {
    private static final String TABLE_PLAYER_TITLES = "lirtitles_player_titles";
    private static final String TABLE_PLAYER_SETTINGS = "lirtitles_player_settings";
    private static final String TABLE_CUSTOM_TITLES = "lirtitles_custom_titles";
    private static final String TABLE_METADATA = "lirtitles_metadata";
    private static final int SCHEMA_VERSION = 1;

    private static final String COLUMN_PLAYER_ID = "player_id";
    private static final String COLUMN_TITLE_ID = "title_id";
    private static final String COLUMN_NUMERIC_ID = "numeric_id";
    private static final String COLUMN_TITLE_TEXT = "title_text";
    private static final String COLUMN_TITLE_SLOT = "title_slot";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_STATUS = "status";
    private static final String COLUMN_REJECTION_REASON = "rejection_reason";
    private static final String COLUMN_DECIDED_AT = "decided_at";
    private static final String COLUMN_AUTHOR_NAME = "author_name";
    private static final String COLUMN_SELECTED_TITLE = "selected_title";
    private static final String COLUMN_SELECTED_FONT = "selected_font";
    private static final String COLUMN_CUSTOM_TITLE_TOKENS = "custom_title_tokens";

    private final ConnectionSource connectionSource;

    public SqlTitleRepository(ConnectionSource connectionSource) {
        this.connectionSource = connectionSource;
        createSchema();
    }

    @Override
    public boolean owns(UUID playerId, String titleId) {
        String sql = "SELECT 1 FROM " + TABLE_PLAYER_TITLES + " WHERE " + COLUMN_PLAYER_ID + " = ? AND " + COLUMN_TITLE_ID + " = ?";
        try (Connection connection = connectionSource.connection();
             PreparedStatement statement = prepare(connection, sql, playerId.toString(), titleId);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not check title ownership", exception);
        }
    }

    @Override
    public Set<String> owned(UUID playerId) {
        String sql = "SELECT " + COLUMN_TITLE_ID + " FROM " + TABLE_PLAYER_TITLES + " WHERE " + COLUMN_PLAYER_ID + " = ?";
        try (Connection connection = connectionSource.connection();
             PreparedStatement statement = prepare(connection, sql, playerId.toString());
             ResultSet resultSet = statement.executeQuery()) {
            Set<String> owned = new HashSet<>();
            while (resultSet.next()) {
                owned.add(resultSet.getString(COLUMN_TITLE_ID));
            }
            return Set.copyOf(owned);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load owned titles", exception);
        }
    }

    @Override
    public PlayerTitleData loadPlayerData(UUID playerId) {
        try (Connection connection = connectionSource.connection()) {
            Set<String> owned = readOwned(connection, playerId);
            PlayerSettings settings = readPlayerSettings(connection, playerId);
            List<CustomTitle> titles = readCustomTitles(connection, playerId);
            return new PlayerTitleData(owned, settings.selectedTitle(), settings.selectedFont(), titles, settings.customTitleTokens());
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load player title data", exception);
        }
    }

    private static Set<String> readOwned(Connection connection, UUID playerId) throws SQLException {
        String sql = "SELECT " + COLUMN_TITLE_ID + " FROM " + TABLE_PLAYER_TITLES + " WHERE " + COLUMN_PLAYER_ID + " = ?";
        try (PreparedStatement statement = prepare(connection, sql, playerId.toString()); ResultSet resultSet = statement.executeQuery()) {
            Set<String> owned = new HashSet<>();
            while (resultSet.next()) {
                owned.add(resultSet.getString(COLUMN_TITLE_ID));
            }
            return owned;
        }
    }

    private static PlayerSettings readPlayerSettings(Connection connection, UUID playerId) throws SQLException {
        String sql = "SELECT " + COLUMN_SELECTED_TITLE + ", " + COLUMN_SELECTED_FONT + ", " + COLUMN_CUSTOM_TITLE_TOKENS
                + " FROM " + TABLE_PLAYER_SETTINGS + " WHERE " + COLUMN_PLAYER_ID + " = ?";
        try (PreparedStatement statement = prepare(connection, sql, playerId.toString()); ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                return new PlayerSettings("", "", 0);
            }
            return new PlayerSettings(
                    valueOrEmpty(resultSet.getString(COLUMN_SELECTED_TITLE)),
                    valueOrEmpty(resultSet.getString(COLUMN_SELECTED_FONT)),
                    resultSet.getInt(COLUMN_CUSTOM_TITLE_TOKENS));
        }
    }

    private static List<CustomTitle> readCustomTitles(Connection connection, UUID playerId) throws SQLException {
        String sql = "SELECT " + COLUMN_TITLE_ID + ", " + COLUMN_NUMERIC_ID + ", " + COLUMN_TITLE_TEXT + ", " + COLUMN_TITLE_SLOT + ", "
                + COLUMN_STATUS + ", " + COLUMN_REJECTION_REASON + ", " + COLUMN_DECIDED_AT + " FROM " + TABLE_CUSTOM_TITLES
                + " WHERE " + COLUMN_PLAYER_ID + " = ? ORDER BY " + COLUMN_TITLE_SLOT + ", " + COLUMN_CREATED_AT;
        try (PreparedStatement statement = prepare(connection, sql, playerId.toString()); ResultSet resultSet = statement.executeQuery()) {
            List<CustomTitle> titles = new ArrayList<>();
            while (resultSet.next()) {
                titles.add(readTitle(resultSet, UUID.fromString(resultSet.getString(COLUMN_TITLE_ID))));
            }
            return titles;
        }
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private record PlayerSettings(String selectedTitle, String selectedFont, int customTitleTokens) {
    }

    @Override
    public boolean give(UUID playerId, String titleId) {
        if (owns(playerId, titleId)) {
            return false;
        }
        String sql = "INSERT INTO " + TABLE_PLAYER_TITLES + " (" + COLUMN_PLAYER_ID + ", " + COLUMN_TITLE_ID + ") VALUES (?, ?)";
        try (Connection connection = connectionSource.connection();
             PreparedStatement statement = prepare(connection, sql, playerId.toString(), titleId)) {
            statement.executeUpdate();
            return true;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not grant title", exception);
        }
    }

    @Override
    public String selected(UUID playerId) {
        return readSetting(playerId, COLUMN_SELECTED_TITLE);
    }

    @Override
    public boolean select(UUID playerId, String titleId) {
        writeSetting(playerId, COLUMN_SELECTED_TITLE, titleId);
        return true;
    }

    @Override
    public boolean select(UUID playerId, String titleId, String fontId) {
        String sql = "UPDATE " + TABLE_PLAYER_SETTINGS + " SET " + COLUMN_SELECTED_TITLE + " = ?, " + COLUMN_SELECTED_FONT
                + " = ? WHERE " + COLUMN_PLAYER_ID + " = ?";
        try (Connection connection = connectionSource.connection()) {
            ensurePlayerSettingsRow(connection, playerId);
            try (PreparedStatement statement = prepare(connection, sql, titleId, fontId, playerId.toString())) {
                statement.executeUpdate();
            }
            return true;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not update selected title and font", exception);
        }
    }

    @Override
    public String selectedFont(UUID playerId) {
        return readSetting(playerId, COLUMN_SELECTED_FONT);
    }

    @Override
    public void selectFont(UUID playerId, String fontId) {
        writeSetting(playerId, COLUMN_SELECTED_FONT, fontId);
    }

    @Override
    public List<CustomTitle> customTitles(UUID playerId) {
        String sql = "SELECT " + COLUMN_TITLE_ID + ", " + COLUMN_NUMERIC_ID + ", " + COLUMN_TITLE_TEXT + ", " + COLUMN_TITLE_SLOT + ", "
                + COLUMN_STATUS + ", " + COLUMN_REJECTION_REASON + ", " + COLUMN_DECIDED_AT + " FROM " + TABLE_CUSTOM_TITLES
                + " WHERE " + COLUMN_PLAYER_ID + " = ? ORDER BY " + COLUMN_TITLE_SLOT + ", " + COLUMN_CREATED_AT;
        try (Connection connection = connectionSource.connection();
             PreparedStatement statement = prepare(connection, sql, playerId.toString());
             ResultSet resultSet = statement.executeQuery()) {
            List<CustomTitle> titles = new ArrayList<>();
            while (resultSet.next()) {
                titles.add(readTitle(resultSet, UUID.fromString(resultSet.getString(COLUMN_TITLE_ID))));
            }
            return List.copyOf(titles);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load custom titles", exception);
        }
    }

    @Override
    public CustomTitle createCustomTitle(UUID playerId, String text, int slot, CustomTitleStatus status) {
        CustomTitle title = new CustomTitle(UUID.randomUUID(), maxNumericId() + 1, text, slot, status, "", 0);
        return createCustomTitle(playerId, title);
    }

    @Override
    public CustomTitle createCustomTitle(UUID playerId, CustomTitle title) {
        return createCustomTitle(playerId, title, playerId.toString().substring(0, 8));
    }

    @Override
    public CustomTitle createCustomTitle(UUID playerId, CustomTitle title, String playerName) {
        String sql = "INSERT INTO " + TABLE_CUSTOM_TITLES + " (" + COLUMN_PLAYER_ID + ", " + COLUMN_TITLE_ID + ", " + COLUMN_NUMERIC_ID + ", "
                + COLUMN_TITLE_TEXT + ", " + COLUMN_TITLE_SLOT + ", " + COLUMN_STATUS + ", " + COLUMN_REJECTION_REASON + ", " + COLUMN_DECIDED_AT
                + ", " + COLUMN_AUTHOR_NAME + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connectionSource.connection();
             PreparedStatement statement = prepare(connection, sql, playerId.toString(), title.id().toString(), title.numericId(), title.text(),
                     title.slot(), title.status().name(), title.rejectionReason(), title.decidedAt(), playerName)) {
            statement.executeUpdate();
            return title;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not create custom title", exception);
        }
    }

    @Override
    public int maxNumericId() {
        try (Connection connection = connectionSource.connection()) {
            return maxNumericId(connection);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read the highest custom title id", exception);
        }
    }

    @Override
    public boolean updateCustomTitle(UUID playerId, UUID titleId, String text, CustomTitleStatus status) {
        String sql = "UPDATE " + TABLE_CUSTOM_TITLES + " SET " + COLUMN_TITLE_TEXT + " = ?, " + COLUMN_STATUS + " = ?, "
                + COLUMN_REJECTION_REASON + " = '', " + COLUMN_DECIDED_AT + " = 0 WHERE " + COLUMN_PLAYER_ID + " = ? AND "
                + COLUMN_TITLE_ID + " = ?";
        try (Connection connection = connectionSource.connection();
             PreparedStatement statement = prepare(connection, sql, text, status.name(), playerId.toString(), titleId.toString())) {
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not update custom title", exception);
        }
    }

    @Override
    public List<PendingModerationEntry> pendingModeration() {
        String sql = "SELECT " + COLUMN_PLAYER_ID + ", " + COLUMN_AUTHOR_NAME + ", " + COLUMN_TITLE_ID + ", " + COLUMN_NUMERIC_ID + ", " + COLUMN_TITLE_TEXT + ", "
                + COLUMN_TITLE_SLOT + ", " + COLUMN_STATUS + ", " + COLUMN_REJECTION_REASON + ", " + COLUMN_DECIDED_AT + " FROM " + TABLE_CUSTOM_TITLES
                + " WHERE " + COLUMN_STATUS + " = ? ORDER BY " + COLUMN_CREATED_AT;
        try (Connection connection = connectionSource.connection();
             PreparedStatement statement = prepare(connection, sql, CustomTitleStatus.PENDING.name());
             ResultSet resultSet = statement.executeQuery()) {
            List<PendingModerationEntry> entries = new ArrayList<>();
            while (resultSet.next()) {
                UUID playerId = UUID.fromString(resultSet.getString(COLUMN_PLAYER_ID));
                CustomTitle title = readTitle(resultSet, UUID.fromString(resultSet.getString(COLUMN_TITLE_ID)));
                entries.add(new PendingModerationEntry(playerId, resultSet.getString(COLUMN_AUTHOR_NAME), title));
            }
            return List.copyOf(entries);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load pending moderation queue", exception);
        }
    }

    @Override
    public boolean decideCustomTitle(UUID playerId, UUID titleId, CustomTitleStatus status, String reason, long decidedAt) {
        String sql = "UPDATE " + TABLE_CUSTOM_TITLES + " SET " + COLUMN_STATUS + " = ?, " + COLUMN_REJECTION_REASON + " = ?, "
                + COLUMN_DECIDED_AT + " = ? WHERE " + COLUMN_PLAYER_ID + " = ? AND " + COLUMN_TITLE_ID + " = ?";
        try (Connection connection = connectionSource.connection();
             PreparedStatement statement = prepare(connection, sql, status.name(), reason, decidedAt, playerId.toString(), titleId.toString())) {
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not record moderation decision", exception);
        }
    }

    private static CustomTitle readTitle(ResultSet resultSet, UUID titleId) throws SQLException {
        CustomTitleStatus status = parseStatus(resultSet.getString(COLUMN_STATUS));
        String reason = resultSet.getString(COLUMN_REJECTION_REASON);
        return new CustomTitle(titleId, resultSet.getInt(COLUMN_NUMERIC_ID), resultSet.getString(COLUMN_TITLE_TEXT),
                resultSet.getInt(COLUMN_TITLE_SLOT), status, reason == null ? "" : reason, resultSet.getLong(COLUMN_DECIDED_AT));
    }

    private static CustomTitleStatus parseStatus(String value) {
        try {
            return CustomTitleStatus.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalStateException("Invalid custom title status in database: " + value, exception);
        }
    }

    @Override
    public boolean deleteCustomTitle(UUID playerId, UUID titleId) {
        String sql = "DELETE FROM " + TABLE_CUSTOM_TITLES + " WHERE " + COLUMN_PLAYER_ID + " = ? AND " + COLUMN_TITLE_ID + " = ?";
        try (Connection connection = connectionSource.connection();
             PreparedStatement statement = prepare(connection, sql, playerId.toString(), titleId.toString())) {
            if (statement.executeUpdate() == 0) {
                return false;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not delete custom title", exception);
        }
        if (CustomTitle.keyFor(titleId).equals(selected(playerId))) {
            writeSetting(playerId, COLUMN_SELECTED_TITLE, "");
        }
        return true;
    }

    @Override
    public CustomTitle customTitle(UUID playerId, UUID titleId) {
        String sql = "SELECT " + COLUMN_NUMERIC_ID + ", " + COLUMN_TITLE_TEXT + ", " + COLUMN_TITLE_SLOT + ", " + COLUMN_STATUS + ", "
                + COLUMN_REJECTION_REASON + ", " + COLUMN_DECIDED_AT + " FROM " + TABLE_CUSTOM_TITLES + " WHERE " + COLUMN_PLAYER_ID + " = ? AND "
                + COLUMN_TITLE_ID + " = ?";
        try (Connection connection = connectionSource.connection();
             PreparedStatement statement = prepare(connection, sql, playerId.toString(), titleId.toString());
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? readTitle(resultSet, titleId) : null;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load custom title", exception);
        }
    }

    @Override
    public boolean selectCustomTitle(UUID playerId, UUID titleId) {
        CustomTitle title = customTitle(playerId, titleId);
        if (title == null || title.status() != CustomTitleStatus.APPROVED) {
            return false;
        }
        writeSetting(playerId, COLUMN_SELECTED_TITLE, CustomTitle.keyFor(titleId));
        return true;
    }

    @Override
    public int customTitleTokens(UUID playerId) {
        String sql = "SELECT " + COLUMN_CUSTOM_TITLE_TOKENS + " FROM " + TABLE_PLAYER_SETTINGS + " WHERE " + COLUMN_PLAYER_ID + " = ?";
        try (Connection connection = connectionSource.connection();
             PreparedStatement statement = prepare(connection, sql, playerId.toString());
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(COLUMN_CUSTOM_TITLE_TOKENS) : 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read custom title tokens", exception);
        }
    }

    @Override
    public void addCustomTitleTokens(UUID playerId, int amount) {
        String sql = "UPDATE " + TABLE_PLAYER_SETTINGS + " SET " + COLUMN_CUSTOM_TITLE_TOKENS + " = " + COLUMN_CUSTOM_TITLE_TOKENS
                + " + ? WHERE " + COLUMN_PLAYER_ID + " = ?";
        try (Connection connection = connectionSource.connection()) {
            ensurePlayerSettingsRow(connection, playerId);
            try (PreparedStatement statement = prepare(connection, sql, amount, playerId.toString())) {
                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not add custom title tokens", exception);
        }
    }

    @Override
    public boolean consumeCustomTitleToken(UUID playerId) {
        String sql = "UPDATE " + TABLE_PLAYER_SETTINGS + " SET " + COLUMN_CUSTOM_TITLE_TOKENS + " = " + COLUMN_CUSTOM_TITLE_TOKENS + " - 1 "
                + "WHERE " + COLUMN_PLAYER_ID + " = ? AND " + COLUMN_CUSTOM_TITLE_TOKENS + " > 0";
        try (Connection connection = connectionSource.connection()) {
            ensurePlayerSettingsRow(connection, playerId);
            try (PreparedStatement statement = prepare(connection, sql, playerId.toString())) {
                return statement.executeUpdate() > 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not consume a custom title token", exception);
        }
    }

    @Override
    public void close() {
        connectionSource.close();
    }

    private String readSetting(UUID playerId, String column) {
        String sql = "SELECT " + column + " FROM " + TABLE_PLAYER_SETTINGS + " WHERE " + COLUMN_PLAYER_ID + " = ?";
        try (Connection connection = connectionSource.connection();
             PreparedStatement statement = prepare(connection, sql, playerId.toString());
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                return "";
            }
            String value = resultSet.getString(column);
            return value == null ? "" : value;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read player settings", exception);
        }
    }

    private void writeSetting(UUID playerId, String column, String value) {
        String updateSql = "UPDATE " + TABLE_PLAYER_SETTINGS + " SET " + column + " = ? WHERE " + COLUMN_PLAYER_ID + " = ?";
        try (Connection connection = connectionSource.connection()) {
            ensurePlayerSettingsRow(connection, playerId);
            try (PreparedStatement update = prepare(connection, updateSql, value, playerId.toString())) {
                update.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not update player settings", exception);
        }
    }

    private void ensurePlayerSettingsRow(Connection connection, UUID playerId) throws SQLException {
        if (playerSettingsRowExists(connection, playerId)) {
            return;
        }
        String insertSql = "INSERT INTO " + TABLE_PLAYER_SETTINGS + " (" + COLUMN_PLAYER_ID + ") VALUES (?)";
        try (PreparedStatement insert = prepare(connection, insertSql, playerId.toString())) {
            insert.executeUpdate();
        } catch (SQLException raceLoss) {
            if (!playerSettingsRowExists(connection, playerId)) {
                throw raceLoss;
            }
        }
    }

    private boolean playerSettingsRowExists(Connection connection, UUID playerId) throws SQLException {
        String selectSql = "SELECT 1 FROM " + TABLE_PLAYER_SETTINGS + " WHERE " + COLUMN_PLAYER_ID + " = ?";
        try (PreparedStatement select = prepare(connection, selectSql, playerId.toString());
             ResultSet resultSet = select.executeQuery()) {
            return resultSet.next();
        }
    }

    private static PreparedStatement prepare(Connection connection, String sql, Object... parameters) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        for (int index = 0; index < parameters.length; index++) {
            statement.setObject(index + 1, parameters[index]);
        }
        return statement;
    }

    private void createSchema() {
        try (Connection connection = connectionSource.connection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS " + TABLE_METADATA
                        + " (metadata_key VARCHAR(64) PRIMARY KEY, metadata_value INTEGER NOT NULL)");
                if (schemaVersion(connection) >= SCHEMA_VERSION) {
                    connection.commit();
                    return;
                }
                statement.execute("CREATE TABLE IF NOT EXISTS " + TABLE_PLAYER_TITLES + " ("
                        + COLUMN_PLAYER_ID + " VARCHAR(36) NOT NULL, " + COLUMN_TITLE_ID + " VARCHAR(64) NOT NULL, "
                        + "PRIMARY KEY (" + COLUMN_PLAYER_ID + ", " + COLUMN_TITLE_ID + "))");
                statement.execute("CREATE TABLE IF NOT EXISTS " + TABLE_PLAYER_SETTINGS + " ("
                        + COLUMN_PLAYER_ID + " VARCHAR(36) PRIMARY KEY, " + COLUMN_SELECTED_TITLE + " VARCHAR(64), "
                        + COLUMN_SELECTED_FONT + " VARCHAR(64), " + COLUMN_CUSTOM_TITLE_TOKENS + " INTEGER NOT NULL DEFAULT 0)");
                statement.execute("CREATE TABLE IF NOT EXISTS " + TABLE_CUSTOM_TITLES + " ("
                        + COLUMN_PLAYER_ID + " VARCHAR(36) NOT NULL, " + COLUMN_TITLE_ID + " VARCHAR(36) NOT NULL, " + COLUMN_TITLE_TEXT + " TEXT NOT NULL, "
                        + COLUMN_TITLE_SLOT + " INTEGER NOT NULL, " + COLUMN_CREATED_AT + " BIGINT NOT NULL DEFAULT 0, "
                        + COLUMN_AUTHOR_NAME + " VARCHAR(16) NOT NULL DEFAULT '', PRIMARY KEY (" + COLUMN_PLAYER_ID + ", " + COLUMN_TITLE_ID + "))");
                Set<String> settingsColumns = columnNames(connection, TABLE_PLAYER_SETTINGS);
                addColumnIfMissing(statement, TABLE_PLAYER_SETTINGS, COLUMN_CUSTOM_TITLE_TOKENS, "INTEGER NOT NULL DEFAULT 0", settingsColumns);
                Set<String> customTitleColumns = columnNames(connection, TABLE_CUSTOM_TITLES);
                addColumnIfMissing(statement, TABLE_CUSTOM_TITLES, COLUMN_STATUS, "VARCHAR(16) NOT NULL DEFAULT 'APPROVED'", customTitleColumns);
                addColumnIfMissing(statement, TABLE_CUSTOM_TITLES, COLUMN_REJECTION_REASON, "TEXT NOT NULL DEFAULT ''", customTitleColumns);
                addColumnIfMissing(statement, TABLE_CUSTOM_TITLES, COLUMN_DECIDED_AT, "BIGINT NOT NULL DEFAULT 0", customTitleColumns);
                addColumnIfMissing(statement, TABLE_CUSTOM_TITLES, COLUMN_NUMERIC_ID, "INTEGER NOT NULL DEFAULT 0", customTitleColumns);
                addColumnIfMissing(statement, TABLE_CUSTOM_TITLES, COLUMN_AUTHOR_NAME, "VARCHAR(16) NOT NULL DEFAULT ''", customTitleColumns);
                Set<String> customTitleIndexes = indexNames(connection, TABLE_CUSTOM_TITLES);
                createIndexIfMissing(statement, TABLE_CUSTOM_TITLES, "idx_lirtitles_custom_player_slot",
                        COLUMN_PLAYER_ID + ", " + COLUMN_TITLE_SLOT + ", " + COLUMN_CREATED_AT, customTitleIndexes);
                createIndexIfMissing(statement, TABLE_CUSTOM_TITLES, "idx_lirtitles_custom_status_created",
                        COLUMN_STATUS + ", " + COLUMN_CREATED_AT, customTitleIndexes);
                createIndexIfMissing(statement, TABLE_CUSTOM_TITLES, "idx_lirtitles_custom_numeric_created",
                        COLUMN_NUMERIC_ID + ", " + COLUMN_CREATED_AT, customTitleIndexes);
                backfillNumericIds(connection);
                storeSchemaVersion(connection, SCHEMA_VERSION);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not initialize the title storage schema", exception);
        }
    }

    private int backfillNumericIds(Connection connection) throws SQLException {
        List<String[]> unassignedRows = new ArrayList<>();
        String selectSql = "SELECT " + COLUMN_PLAYER_ID + ", " + COLUMN_TITLE_ID + " FROM " + TABLE_CUSTOM_TITLES
                + " WHERE " + COLUMN_NUMERIC_ID + " = 0 ORDER BY " + COLUMN_CREATED_AT + ", " + COLUMN_PLAYER_ID + ", " + COLUMN_TITLE_ID;
        try (PreparedStatement select = connection.prepareStatement(selectSql); ResultSet resultSet = select.executeQuery()) {
            while (resultSet.next()) {
                unassignedRows.add(new String[]{resultSet.getString(COLUMN_PLAYER_ID), resultSet.getString(COLUMN_TITLE_ID)});
            }
        }
        if (unassignedRows.isEmpty()) {
            return 0;
        }
        int nextNumericId = maxNumericId(connection);
        String updateSql = "UPDATE " + TABLE_CUSTOM_TITLES + " SET " + COLUMN_NUMERIC_ID + " = ? WHERE " + COLUMN_PLAYER_ID + " = ? AND "
                + COLUMN_TITLE_ID + " = ?";
        try (PreparedStatement update = connection.prepareStatement(updateSql)) {
            for (String[] row : unassignedRows) {
                nextNumericId++;
                update.setInt(1, nextNumericId);
                update.setString(2, row[0]);
                update.setString(3, row[1]);
                update.addBatch();
            }
            update.executeBatch();
        }
        return unassignedRows.size();
    }

    private int maxNumericId(Connection connection) throws SQLException {
        String sql = "SELECT MAX(" + COLUMN_NUMERIC_ID + ") FROM " + TABLE_CUSTOM_TITLES;
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    private static int schemaVersion(Connection connection) throws SQLException {
        String sql = "SELECT metadata_value FROM " + TABLE_METADATA + " WHERE metadata_key = ?";
        try (PreparedStatement statement = prepare(connection, sql, "schema_version"); ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    private static void storeSchemaVersion(Connection connection, int version) throws SQLException {
        String updateSql = "UPDATE " + TABLE_METADATA + " SET metadata_value = ? WHERE metadata_key = ?";
        try (PreparedStatement update = prepare(connection, updateSql, version, "schema_version")) {
            if (update.executeUpdate() > 0) {
                return;
            }
        }
        String insertSql = "INSERT INTO " + TABLE_METADATA + " (metadata_key, metadata_value) VALUES (?, ?)";
        try (PreparedStatement insert = prepare(connection, insertSql, "schema_version", version)) {
            insert.executeUpdate();
        }
    }

    private static void addColumnIfMissing(Statement statement, String table, String column, String definition, Set<String> columns)
            throws SQLException {
        if (columns.contains(column.toLowerCase(Locale.ROOT))) {
            return;
        }
        statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        columns.add(column.toLowerCase(Locale.ROOT));
    }

    private static Set<String> columnNames(Connection connection, String table) throws SQLException {
        Set<String> names = new HashSet<>();
        try (ResultSet columns = connection.getMetaData().getColumns(null, null, table, "%")) {
            while (columns.next()) {
                names.add(columns.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
            }
        }
        return names;
    }

    private static Set<String> indexNames(Connection connection, String table) throws SQLException {
        Set<String> names = new HashSet<>();
        try (ResultSet indexes = connection.getMetaData().getIndexInfo(null, null, table, false, false)) {
            while (indexes.next()) {
                String indexName = indexes.getString("INDEX_NAME");
                if (indexName != null) {
                    names.add(indexName.toLowerCase(Locale.ROOT));
                }
            }
        }
        return names;
    }

    private static void createIndexIfMissing(Statement statement, String table, String index, String columns, Set<String> indexes)
            throws SQLException {
        if (indexes.contains(index.toLowerCase(Locale.ROOT))) {
            return;
        }
        statement.execute("CREATE INDEX " + index + " ON " + table + " (" + columns + ")");
        indexes.add(index.toLowerCase(Locale.ROOT));
    }
}
