module bet.gen {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.json;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires java.net.http;

    opens gui to javafx.graphics, javafx.fxml;
    opens model to com.fasterxml.jackson.databind;

    exports gui;
}