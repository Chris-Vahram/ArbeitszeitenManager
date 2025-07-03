module org.example.arbeitszeitenmanager {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens org.example.arbeitszeitenmanager to javafx.fxml;
    exports org.example.arbeitszeitenmanager;
}