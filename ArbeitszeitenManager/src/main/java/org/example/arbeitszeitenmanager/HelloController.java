package org.example.arbeitszeitenmanager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import org.example.arbeitszeitenmanager.model.WorkDay;
import org.example.arbeitszeitenmanager.model.WorkDayDAO;

import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


public class HelloController {

    @FXML
    private ComboBox<LocalDate> comboWeeks;

    @FXML
    private TableView<WorkDay> tableWorkDays;

    @FXML
    private TableColumn<WorkDay, String> colDatum, colStart, colEnd, colNotes, colIstZeit, colSollZeit, colRestzeit, colSaldo, colSollEndzeit;
    @FXML
    private Button resetButton;

    @FXML
    private Label lblZeitausgleich;

    private final ObservableList<WorkDay> allWorkDays = FXCollections.observableArrayList();
    private final ObservableList<WorkDay> filteredWorkDays = FXCollections.observableArrayList();
    private WorkDayDAO dao;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEE, dd.MM.yyyy");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    public void initialize() {
        try {
            dao = new WorkDayDAO();
            allWorkDays.clear();
            dao.loadAll().forEach(wd -> {
                if (!allWorkDays.contains(wd)) allWorkDays.add(wd);
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
        LocalDate start = LocalDate.of(2025, 7, 1);
        LocalDate end_time = LocalDate.of(2025, 8, 11);
        for (LocalDate d = start; !d.isAfter(end_time); d = d.plusDays(1)) {
            if (!(d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY)) {
                // check ob der Tag schon existiert
                LocalDate finalD = d;
                boolean exists = allWorkDays.stream()
                        .anyMatch(wd -> wd.getDate().equals(finalD));
                if (!exists) {
                    allWorkDays.add(new WorkDay(d));
                }
            }
        }


        Set<LocalDate> mondays = allWorkDays.stream()
                .map(wd -> wd.getDate().with(DayOfWeek.MONDAY))
                .collect(Collectors.toSet());

        List<LocalDate> sortedMondays = mondays.stream().sorted().toList();

        comboWeeks.getItems().addAll(sortedMondays);
        comboWeeks.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date != null ? date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "";
            }

            @Override
            public LocalDate fromString(String s) {
                return null;
            }
        });

        comboWeeks.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                filterByWeek(newVal);
            }
        });

        if (!comboWeeks.getItems().isEmpty()) {
            comboWeeks.getSelectionModel().select(0);
        }

        colDatum.setCellValueFactory(cdf -> new SimpleStringProperty(cdf.getValue().getDate().format(dateFormatter)));

        colStart.setCellValueFactory(cdf -> {
            LocalTime st = cdf.getValue().getStart();
            return new SimpleStringProperty(st == null ? "" : st.format(timeFormatter));
        });
        colStart.setCellFactory(tc -> new EditingCell());
        colStart.setOnEditCommit(e -> {
            WorkDay wd = e.getRowValue();
            try {
                wd.setStart(LocalTime.parse(e.getNewValue(), timeFormatter));
                dao.save(wd); // <-- hier speichern
                updateWeekView();
            } catch (Exception ignored) {}
        });

        colEnd.setCellValueFactory(cdf -> {
            LocalTime et = cdf.getValue().getEnd_time();
            return new SimpleStringProperty(et == null ? "" : et.format(timeFormatter));
        });
        colEnd.setCellFactory(tc -> new EditingCell());
        colEnd.setOnEditCommit(e -> {
            WorkDay wd = e.getRowValue();
            try {
                wd.setEnd_time(LocalTime.parse(e.getNewValue(), timeFormatter));
                dao.save(wd); // <-- hier speichern
                updateWeekView();
            } catch (Exception ignored) {}
        });

        colNotes.setCellValueFactory(cdf -> cdf.getValue().notesProperty());
        colNotes.setCellFactory(tc -> new EditingCell());
        colNotes.setOnEditCommit(e -> {
            WorkDay wd = e.getRowValue();
            wd.setNotes(e.getNewValue());
            try {
                dao.save(wd);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        colSollZeit.setCellValueFactory(cdf -> new SimpleStringProperty(formatDuration(cdf.getValue().getSollZeit())));
        colIstZeit.setCellValueFactory(cdf -> new SimpleStringProperty(formatDuration(cdf.getValue().getIstZeit())));
        colRestzeit.setCellValueFactory(cdf -> new SimpleStringProperty(formatDuration(cdf.getValue().getRestzeitHeute())));
        colSaldo.setCellValueFactory(cdf -> {
            Duration ist = cdf.getValue().getIstZeit();
            Duration rest = cdf.getValue().getRestzeitHeute();
            Duration saldo;
            if (!rest.isZero()) {
                saldo = ist.minus(rest);
            } else {
                saldo = ist.minus(cdf.getValue().getSollZeit());
            }
            return new SimpleStringProperty(formatSignedDuration(saldo));
        });

        colSollEndzeit.setCellValueFactory(cdf -> {
            LocalTime startTime = cdf.getValue().getStart();
            Duration rest = cdf.getValue().getRestzeitHeute();
            LocalTime sollEnde = null;
            if (startTime != null && rest != null && !rest.isZero() && cdf.getValue().getEnd_time() == null) {
                sollEnde = startTime.plusMinutes(rest.toMinutes());
            }
            return new SimpleStringProperty(sollEnde != null ? sollEnde.format(timeFormatter) : "");
        });

        tableWorkDays.setItems(filteredWorkDays);
        tableWorkDays.setEditable(true);
    }

    public void stop() {
        try {
            for (WorkDay wd : allWorkDays) {
                dao.save(wd);
            }
            dao.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void filterByWeek(LocalDate monday) {
        filteredWorkDays.setAll(allWorkDays.stream()
                .filter(wd -> {
                    LocalDate d = wd.getDate();
                    return !d.isBefore(monday) && d.isBefore(monday.plusDays(7));
                }).collect(Collectors.toList()));

        updateWeekView();
    }

    private void updateWeekView() {
        Duration sumSoll = Duration.ZERO;
        Duration sumIst = Duration.ZERO;

        for (WorkDay wd : filteredWorkDays) {
            sumSoll = sumSoll.plus(wd.getSollZeit());
            sumIst = sumIst.plus(wd.getIstZeit());
        }

        Duration delta = sumSoll.minus(sumIst); // Was noch fehlt insgesamt
        long verbleibendeTage = filteredWorkDays.stream()
                .filter(wd -> wd.getIstZeit().isZero())
                .count();

        Duration aufteilen = verbleibendeTage > 0 ? Duration.ofMinutes(delta.toMinutes() / verbleibendeTage) : Duration.ZERO;

        for (WorkDay wd : filteredWorkDays) {
            if (wd.getIstZeit().isZero()) {
                wd.setRestzeitHeute(aufteilen);
            } else {
                // Wenn schon Zeiten eingetragen sind, wird Restzeit auf 0 gesetzt,
                // weil für diesen Tag ist die Zeit schon „erledigt“
                wd.setRestzeitHeute(Duration.ZERO);
            }
        }

        lblZeitausgleich.setText(formatSignedDuration(sumIst.minus(sumSoll)));
        tableWorkDays.refresh();
    }


    private String formatDuration(Duration d) {
        long totalMinutes = d.toMinutes();
        return String.format("%d:%02d", totalMinutes / 60, totalMinutes % 60);
    }

    private String formatSignedDuration(Duration d) {
        long totalMinutes = d.toMinutes();
        String sign = totalMinutes < 0 ? "-" : "+";
        long absMin = Math.abs(totalMinutes);
        return sign + String.format("%d:%02d", absMin / 60, absMin % 60);
    }

    @FXML
    private void onResetClicked() throws SQLException {
        WorkDay selected = tableWorkDays.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selected.setStart(null);
            selected.setEnd_time(null);

            // Optional: auch Notes resetten
            // selected.setNotes(null);

            tableWorkDays.refresh(); // wichtig!
            dao.save(selected); // direkt speichern (falls du das willst)
        }
    }


    private static class EditingCell extends TableCell<WorkDay, String> {
        private final TextField textField = new TextField();

        public EditingCell() {
            textField.setOnAction(e -> commitEdit(textField.getText()));
            textField.setOnKeyReleased(e -> {
                switch (e.getCode()) {
                    case ESCAPE -> cancelEdit();
                    case ENTER -> commitEdit(textField.getText());
                }
            });
        }

        @Override
        public void startEdit() {
            super.startEdit();
            setText(null);
            setGraphic(textField);
            textField.setText(getItem());
            textField.requestFocus();
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem());
            setGraphic(null);
        }

        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else if (isEditing()) {
                setText(null);
                setGraphic(textField);
            } else {
                setText(item);
                setGraphic(null);
            }
        }
    }
}
