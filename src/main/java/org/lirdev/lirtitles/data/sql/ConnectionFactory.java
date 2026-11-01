package org.lirdev.lirtitles.data.sql;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
interface ConnectionFactory {
    Connection open() throws SQLException;
}
