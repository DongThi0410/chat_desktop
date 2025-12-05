module com.example.chat {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires java.desktop;
    requires javafx.graphics;
    requires java.sql;
    requires com.example.chat;

    opens com.example.chat to javafx.fxml;
    exports com.example.chat.UI;
    opens com.example.chat.UI to javafx.fxml;
    exports com.example.chat.core;
    opens com.example.chat.core to javafx.fxml;
}