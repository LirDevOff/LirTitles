package org.lirdev.lirtitles.data.sql;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionSource {
    Connection connection() throws SQLException;

    void close();
}
