package lib.PatPeter.SQLibrary.Builders.MySQL;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import lib.PatPeter.SQLibrary.Database;
import lib.PatPeter.SQLibrary.DatabaseException;
import lib.PatPeter.SQLibrary.Builders.Builder;
import lib.PatPeter.SQLibrary.Builders.BuilderException;

/**
 * SELECT query builder.<br>
 * Date Created: 2012-09-09 17:45.
 * 
 * @author Nicholas Solin, a.k.a. PatPeter
 */
public class Select implements Builder {

    private enum Cache {
        SQL_CACHE,
        SQL_NO_CACHE;
    }

    private enum Duplicates {
        ALL(0),
        DISTINCT(1),
        DISTINCTROW(2);

        public static Duplicates byID(final int id) throws BuilderException {
            if (id < 0 || 2 < id) {
                throw new BuilderException("Duplicates must be between 0 and 2.");
            }
            return Duplicates.values()[id];
        }

        private final String[] strings = { "ALL", "DISTINCT", "DISTINCTROW" };

        private int id;

        private Duplicates(final int id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return this.strings[id];
        }
    }

    private enum Into {
        OUT,
        DUMP,
        VARIABLE;
    }

    private Database db;

    private final String[] conditionals = { "OR", "||", "XOR", "AND", "&&" };

    private final HashSet<String> columns = new HashSet<String>();
    private final HashSet<String> tables = new HashSet<String>();

    public Duplicates duplicates = null;
    public Cache cache = null;

    private boolean high = false;
    private boolean join = false;

    private boolean small = false;
    private boolean big = false;
    private boolean buffer = false;

    private boolean calc = false;

    private final ArrayList<String> where = new ArrayList<String>();
    private final ArrayList<String> groupBy = new ArrayList<String>();
    private final ArrayList<String> having = new ArrayList<String>();
    private final ArrayList<String> orderBy = new ArrayList<String>();

    private int[] limit = null;

    private String procedure = "";

    private Into into = null;
    private String file = "";
    private String charset = "";
    private String options = "";
    private HashSet<String> variables = new HashSet<String>();

    private Boolean update = null;

    public Select(final Database db) {
        setDatabase(db);
    }

    /* public Select(Database db, String columns, String tables) throws DatabaseException {
     * //setDatabase(db);
     * setColumns(columns);
     * setTables(tables);
     * } */

    private String addCommas(final Collection<String> strings) {
        String output = "";
        boolean first = true;
        for (final String string : strings) {
            if (first) {
                output += string;
                first = false;
            } else {
                output += ", " + string;
            }
        }
        return output;
    }

    public Select big(final boolean big) {
        this.big = big;
        return this;
    }

    public Select buffer(final boolean buffer) {
        this.buffer = buffer;
        return this;
    }

    public Select cache(final Boolean cache) {
        if (cache == null) {
            this.cache = null;
            return this;
        }

        if (cache) {
            this.cache = Cache.SQL_CACHE;
        } else if (!cache) {
            this.cache = Cache.SQL_NO_CACHE;
        }
        return this;
    }

    public Select calc(final boolean calc) {
        this.calc = calc;
        return this;
    }

    @Deprecated
    private boolean checkCondition(final String condition) {
        if (condition == null || condition.length() == 0) {
            db.writeError("Skipping null or empty WHERE condition.", false);
            return false;
        }
        return true;
    }

    private boolean checkConditional(final String conditional) {
        validString(conditional, "Skipping null or empty WHERE conditional.");
        for (final String c : conditionals) {
            if (conditional.equals(c)) {
                return true;
            }
        }
        db.writeError("Skipping unknown conditional " + conditional + ".", false);
        return false;
    }

    public Select columns(final String... columns) {
        int counter = 0;
        // int added = 0;
        for (final String column : columns) {
            if (column != null && column.length() != 0) {
                if (!column.contains("`")) {
                    this.columns.add(column);
                    // added++;
                } else {
                    db.writeError("Column " + column + " in SELECT statement cannot have backticks.", false);
                }
            } else {
                db.writeError("Column at position " + counter + " cannot be null or empty in SELECT statement.", false);
            }
            counter++;
        }
        return this;
    }

    public Select dumpfile(final String filename) {
        into = Into.DUMP;
        file = filename;
        variables = new HashSet<String>();
        return this;
    }

    public Select duplicates(final Integer duplicates) {
        if (duplicates == null) {
            this.duplicates = null;
            return this;
        }

        this.duplicates = Duplicates.byID(duplicates);
        return this;
    }

    public ResultSet execute() throws SQLException {
        if (columns.isEmpty()) {
            throw new BuilderException("Must specify at least one column in a SELECT statement.");
        }

        return db.query(this);
    }

    public ArrayList<String> getColumns() {
        return new ArrayList<String>(columns);
    }

    public Database getDatabase() {
        return db;
    }

    public ArrayList<String> getTables() {
        return new ArrayList<String>(tables);
    }

    public Select groupBy(final String expression) {
        if (!validString(expression, "Skipping null or empty GROUP BY expression.")) {
            return this;
        }

        groupBy.add(expression);
        return this;
    }

    public Select groupBy(final String expression, final boolean ascending) {
        if (!validString(expression, "Skipping null or empty GROUP BY expression.")) {
            return this;
        }

        groupBy.add(expression);
        groupBy.add(ascending ? "ASC" : "DESC");
        return this;
    }

    public Select having(final String condition) {
        if (!checkCondition(condition)) {
            return this;
        }

        having.add(condition);
        return this;
    }

    public Select having(final String conditional, final String condition) {
        if (having.size() != 0) {
            if (!checkConditional(conditional)) {
                return this;
            }
        } else {
            db.writeError("Cannot add conditional " + conditional + " to the front of a HAVING statement.", false);
        }
        if (!checkCondition(condition)) {
            return this;
        }

        if (having.size() != 0) {
            having.add(conditional);
        }
        having.add(condition);
        return this;
    }

    public Select high(final boolean high) {
        this.high = high;
        return this;
    }

    public Select into(final String variable) {
        into = Into.VARIABLE;
        file = "";
        variables.add(variable);
        return this;
    }

    public Select join(final boolean join) {
        this.join = join;
        return this;
    }

    public Select limit() {
        this.limit = null;
        return null;
    }

    public Select limit(final int rows) {
        this.limit = new int[2];
        this.limit[0] = 0;
        this.limit[1] = rows;
        return this;
    }

    public Select limit(final int offset, final int rows) {
        this.limit = new int[2];
        this.limit[0] = offset;
        this.limit[1] = rows;
        return this;
    }

    public Select orderBy(final String expression) {
        if (!validString(expression, "Skipping null or empty ORDER BY expression.")) {
            return this;
        }

        orderBy.add(expression);
        return this;
    }

    public Select orderBy(final String expression, final boolean ascending) {
        if (!validString(expression, "Skipping null or empty ORDER BY expression.")) {
            return this;
        }

        orderBy.add(expression);
        orderBy.add(ascending ? "ASC" : "DESC");
        return this;
    }

    public Select outfile(final String filename) {
        into = Into.OUT;
        file = filename;
        this.charset = "";
        this.options = "";
        variables = new HashSet<String>();
        return this;
    }

    public Select outfile(final String filename, final String options) {
        into = Into.OUT;
        file = filename;
        this.charset = "";
        this.options = options;
        variables = new HashSet<String>();
        return this;
    }

    public Select outfile(final String filename, final String charset, final String options) {
        into = Into.OUT;
        file = filename;
        this.charset = charset;
        this.options = options;
        variables = new HashSet<String>();
        return this;
    }

    public Select procedure(final String procedure) {
        if (!validString(procedure, "Skipped null or empty procedure.")) {
            return this;
        }

        this.procedure = procedure;
        return this;
    }

    private void setDatabase(final Database db) throws DatabaseException {
        if (db == null) {
            throw new DatabaseException("Database cannot be null in SELECT statement.");
        }

        this.db = db;
    }

    public Select small(final boolean small) {
        this.small = small;
        return this;
    }

    /* private void setColumns(String columns) throws DatabaseException {
     * if (columns == null || columns.isEmpty())
     * throw new DatabaseException("String columns cannot be null or empty in SELECT query.");
     * if (columns.contains(",")) {
     * String[] temp = columns.split(",");
     * for (String t : temp)
     * if (t.split("`", -1).length - 1 == 2)
     * throw new DatabaseException("Invalid syntax in SELECT query. Two backticks must surround column " + t + ".");
     * this.columns = Arrays.asList(temp);
     * } else {
     * if (columns.split("`", -1).length - 1 == 2)
     * throw new DatabaseException("Invalid syntax in SELECT query. Two backticks must surround column " + columns + ".");
     * this.columns.set(0, columns);
     * }
     * }
     * private void setTables(String tables) throws DatabaseException {
     * if (tables == null || tables.isEmpty())
     * throw new DatabaseException("String tables cannot be null or empty in SELECT query.");
     * if (tables.contains(",")) {
     * String[] temp = tables.split(",");
     * for (String t : temp)
     * if (t.split("`", -1).length - 1 == 2)
     * throw new DatabaseException("Invalid syntax in SELECT query. Two backticks must surround table " + t + ".");
     * this.tables = Arrays.asList(temp);
     * } else {
     * if (tables.split("`", -1).length - 1 == 2)
     * throw new DatabaseException("Invalid syntax in SELECT query. Two backticks must surround table " + tables + ".");
     * this.tables.set(0, tables);
     * }
     * } */

    public Select tables(final String... tables) {
        int counter = 0;
        // int added = 0;
        for (final String table : tables) {
            if (table != null && !table.isEmpty()) {
                if (!table.contains("`")) {
                    this.tables.add(table);
                    // added++;
                } else {
                    db.writeError("Skipping table " + table + " in SELECT statement that has backticks.", false);
                }
            } else {
                db.writeError("Skipping table in SELECT statement at position " + counter + " for being null or empty.", false);
            }
            counter++;
        }
        return this;
    }

    @Override
    public String toString() {
        if (columns.isEmpty()) {
            throw new BuilderException("Cannot build SELECT statement");
        }

        String string = "SELECT " + (duplicates != null ? duplicates + " " : "");
        string += (high ? "HIGH_PRIORITY " : "");
        string += (join ? "STRAIGHT_JOIN " : "");
        string += (small ? "SQL_SMALL_RESULT " : "");
        string += (big ? "SQL_BIG_RESULT " : "");
        string += (buffer ? "SQL_BUFFER_RESULT " : "");
        string += (cache != null ? cache + " " : "");
        string += (calc ? "SQL_CALC_FOUND_ROWS " : "");

        string += addCommas(columns);

        if (!tables.isEmpty()) {
            string += addCommas(tables);

            if (!where.isEmpty()) {
                string += "WHERE ";
                for (final String w : where) {
                    string += w + " ";
                }
            }

            if (!groupBy.isEmpty()) {
                string += "GROUP BY ";
                string += addCommas(groupBy);
            }

            if (!having.isEmpty()) {
                string += "HAVING ";
                for (final String h : having) {
                    string += h + " ";
                }
            }

            if (!orderBy.isEmpty()) {
                string += "ORDER BY ";
                string += addCommas(orderBy);
            }

            if (limit != null) {
                string += "LIMIT " + limit[0] + ", " + limit[1];
            }

            if (procedure != "") {
                string += "PROCEDURE " + procedure;
            }

            switch (into) {
            case OUT:
                string += "INTO OUTFILE '" + file + "' ";
                if (charset != "") {
                    string += "CHARACTER SET " + charset + " ";
                }
                string += options;
            break;

            case DUMP:
                string += "INTO DUMPFILE '" + file + "' ";
            break;

            case VARIABLE:
                string += "INTO ";
                string += addCommas(variables);
            break;
            }

            string += (update != null ? (update ? "FOR UPDATE" : "LOCK IN SHARE MODE") : "");
        }

        return string;
    }

    public Select update(final Boolean update) {
        /* if (update == null) {
         * this.update = null;
         * return this;
         * } */
        this.update = update;
        return this;
    }

    private boolean validString(final String string, final String error) {
        if (string == null || string.length() == 0) {
            db.writeError(error, false);
            return false;
        }
        return true;
    }

    public Select where(final String condition) {
        if (!checkCondition(condition)) {
            return this;
        }

        where.add(condition);
        return this;
    }

    public Select where(final String conditional, final String condition) {
        if (where.size() != 0) {
            if (!checkConditional(conditional)) {
                return this;
            }
        } else {
            db.writeError("Cannot add conditional " + conditional + " to the front of a WHERE statement.", false);
        }
        if (!checkCondition(condition)) {
            return this;
        }

        if (where.size() != 0) {
            where.add(conditional);
        }
        where.add(condition);
        return this;
    }
}
