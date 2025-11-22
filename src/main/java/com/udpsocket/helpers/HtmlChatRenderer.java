package com.udpsocket.helpers;
import javafx.application.Platform;
import javafx.scene.web.WebEngine;

public class HtmlChatRenderer {
    private final WebEngine engine;


    public HtmlChatRenderer(WebEngine engine) {
        this.engine = engine;
    }

    public void appendHtml(String html) {
        String cur = (String) engine.executeScript("document.body.innerHTML");

        engine.loadContent("<html><body style='font-family:sans-serif;background:white;color:black;'>" +
                cur + html +
                "</body></html>");

        Platform.runLater(() ->
                engine.executeScript("window.scrollTo(0, document.body.scrollHeight);")
        );
    }

    public void loadHtml(String html) {
        engine.loadContent("<html><body style='font-family:sans-serif;background:white;color:black;'>" +
                html +
                "</body></html>");

        Platform.runLater(() ->
                engine.executeScript("window.scrollTo(0, document.body.scrollHeight);")
        );
    }

}
