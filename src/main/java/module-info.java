module com.udpsocket {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.desktop;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires org.bytedeco.javacv;
    requires org.bytedeco.opencv;
    requires org.bytedeco.libfreenect;
    requires java.sql;

    opens com.udpsocket.view to javafx.fxml, javafx.graphics;
    exports com.udpsocket.view;
    exports com.udpsocket.model;
    opens com.udpsocket.model to javafx.fxml, javafx.graphics;

}
