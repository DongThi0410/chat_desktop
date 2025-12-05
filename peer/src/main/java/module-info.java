module com.example.chat {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires java.desktop;
    requires javafx.graphics;
    requires java.sql;

    // Packages export
    exports com.example.chat.UI;
    exports com.example.chat.core;
    exports com.example.chat.db; // ★ Bạn còn thiếu dòng này

    // Open for FXML reflection
    opens com.example.chat to javafx.fxml;
    opens com.example.chat.UI to javafx.fxml;
    opens com.example.chat.core to javafx.fxml;
}
