package com.mycompany.googlecse;

import javax.swing.SwingUtilities;

public class MainApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SearchApp app = new SearchApp();
            app.setVisible(true);
        });
    }
}
