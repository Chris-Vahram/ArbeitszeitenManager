package org.example.arbeitszeitenmanager.model;


import java.sql.*;

public class WorkDayDAO {
    private static final String URL = "jdbc:h2:tcp://localhost:9092/./default";
    private Connection conn;

    public WorkDayDAO() throws SQLException {
        conn = DriverManager.getConnection(URL);
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS workday (" +
                    "date DATE PRIMARY KEY, start TIME, end_time TIME, notes VARCHAR(255))");
        }
    }

    public void save(WorkDay wd) throws SQLException {
        String up = "MERGE INTO workday(date, start, end_time, notes) KEY(date) VALUES(?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(up)) {
            ps.setDate(1, Date.valueOf(wd.getDate()));
            ps.setTime(2, wd.getStart() != null ? Time.valueOf(wd.getStart()) : null);
            ps.setTime(3, wd.getEnd_time() != null ? Time.valueOf(wd.getEnd_time()) : null);
            ps.setString(4, wd.getNotes());
            ps.executeUpdate();
        }
    }

    // WorkDayDAO.java
    public java.util.List<WorkDay> loadAll() throws SQLException {
        var list = new java.util.ArrayList<WorkDay>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT date, start, end_time, notes FROM workday")) { // <-- notes ergänzt
            while (rs.next()) {
                WorkDay wd = new WorkDay(rs.getDate("date").toLocalDate());
                Time s = rs.getTime("start");
                Time e = rs.getTime("end_time");
                String n = rs.getString("notes");
                if (s != null) wd.setStart(s.toLocalTime());
                if (e != null) wd.setEnd_time(e.toLocalTime());
                if (n != null) wd.setNotes(n);
                list.add(wd);
            }
        }
        return list;
    }

    public void close() throws SQLException {
        if (conn != null) conn.close();
    }
}
