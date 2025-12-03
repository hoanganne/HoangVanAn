package com.mycompany.googlecse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.List;

public class SearchApp extends JFrame {

    // Read API key & CX from resources/config.properties (you can edit)
    private final Properties cfg = new Properties();

    // UI components
    private final DefaultListModel<String> domainModel = new DefaultListModel<>();
    private final JList<String> domainList = new JList<>(domainModel);

    private final DefaultListModel<ResultItem> resultModel = new DefaultListModel<>();
    private final JList<ResultItem> resultList = new JList<>(resultModel);

    private final JTextField tfK1 = new JTextField();
    private final JTextField tfK2 = new JTextField();
    private final JTextField tfK3 = new JTextField();
    private final JButton btnSearch = new JButton("Tìm kiếm");
    private final JLabel lbStatus = new JLabel("Sẵn sàng");
    private final JProgressBar progress = new JProgressBar();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    private List<ResultItem> allResults = new ArrayList<>();
    private String domainFilter = "Tất cả";

    public SearchApp() {
        loadConfig();

        setTitle("Tìm kiếm tin trên internet (NetBeans Maven)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        setContentPane(buildUI());

        btnSearch.addActionListener(e -> runSearch());

        domainList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String sel = domainList.getSelectedValue();
                domainFilter = (sel == null) ? "Tất cả" : sel;
                applyFilter();
            }
        });

        resultList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    ResultItem it = resultList.getSelectedValue();
                    if (it != null) openBrowser(it.link);
                }
            }
        });

        domainModel.clear();
        domainModel.addElement("Tất cả");

        // initial message
        resultModel.addElement(new ResultItem("Nhập từ khóa và bấm Tìm kiếm", "", "", ""));
    }

    private void loadConfig() {
        try (InputStream is = getClass().getResourceAsStream("/config.properties")) {
            if (is != null) cfg.load(is);
        } catch (Exception ex) {
            // ignore
        }
    }

    private JComponent buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(12,12,12,12));

        JPanel body = new JPanel(new GridLayout(1,3,12,12));
        body.add(buildDomainsCard());
        body.add(buildSearchCard());
        body.add(buildResultsCard());

        root.add(body, BorderLayout.CENTER);

        JPanel status = new JPanel(new BorderLayout());
        status.add(lbStatus, BorderLayout.WEST);
        progress.setVisible(false);
        status.add(progress, BorderLayout.EAST);
        root.add(status, BorderLayout.SOUTH);

        return root;
    }

    private JComponent buildDomainsCard() {
        JPanel card = new JPanel(new BorderLayout(6,6));
        card.setBorder(BorderFactory.createTitledBorder("Danh sách web"));

        domainList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        domainList.setFixedCellHeight(28);
        JScrollPane sp = new JScrollPane(domainList);
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildSearchCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createTitledBorder("Tìm kiếm"));

        tfK1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        tfK2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        tfK3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        card.add(new JLabel("Từ khóa 1 (bắt buộc)"));
        card.add(tfK1);
        card.add(Box.createVerticalStrut(8));
        card.add(new JLabel("Từ khóa 2 (tùy chọn)"));
        card.add(tfK2);
        card.add(Box.createVerticalStrut(8));
        card.add(new JLabel("Từ khóa 3 (tùy chọn)"));
        card.add(tfK3);
        card.add(Box.createVerticalStrut(12));
        card.add(btnSearch);
        card.add(Box.createVerticalGlue());
        return card;
    }

    private JComponent buildResultsCard() {
        JPanel card = new JPanel(new BorderLayout(6,6));
        card.setBorder(BorderFactory.createTitledBorder("Kết quả"));

        resultList.setModel(resultModel);
        resultList.setCellRenderer(new ResultRenderer());
        JScrollPane sp = new JScrollPane(resultList);
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    private List<String> collectKeywords() {
        List<String> ks = new ArrayList<>();
        addIfNotBlank(ks, tfK1.getText());
        addIfNotBlank(ks, tfK2.getText());
        addIfNotBlank(ks, tfK3.getText());
        return ks;
    }
    private void addIfNotBlank(List<String> list, String s) {
        if (s == null) return;
        s = s.trim();
        if (!s.isEmpty()) list.add(s);
    }

    private void runSearch() {
        String apiKey = cfg.getProperty("google.api.key", "").trim();
        String cx = cfg.getProperty("google.cse.id", "").trim();

        if (apiKey.isEmpty() || cx.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bạn chưa cấu hình google.api.key và google.cse.id trong src/main/resources/config.properties",
                    "Thiếu cấu hình", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<String> kws = collectKeywords();
        if (kws.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nhập ít nhất từ khóa 1.", "Thiếu dữ liệu", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String query = String.join(" ", kws);
        setBusy(true, "Đang tìm: " + query);

        SwingWorker<List<ResultItem>, Void> worker = new SwingWorker<>() {
            @Override protected List<ResultItem> doInBackground() throws Exception {
                return googleSearch(apiKey, cx, query, 10);
            }

            @Override protected void done() {
                try {
                    allResults = get();
                    buildDomainsFromResults(allResults);
                    domainFilter = "Tất cả";
                    domainList.setSelectedIndex(0);
                    applyFilter();
                    setBusy(false, "Xong. Tổng: " + allResults.size() + " kết quả");
                } catch (Exception ex) {
                    setBusy(false, "Lỗi");
                    JOptionPane.showMessageDialog(SearchApp.this, "Lỗi: " + ex.getMessage(), "API Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void setBusy(boolean busy, String status) {
        btnSearch.setEnabled(!busy);
        progress.setVisible(busy);
        progress.setIndeterminate(busy);
        lbStatus.setText(status);
        setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }

    private List<ResultItem> googleSearch(String apiKey, String cx, String query, int num) throws Exception {
        String q = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://www.googleapis.com/customsearch/v1"
                + "?key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                + "&cx="  + URLEncoder.encode(cx, StandardCharsets.UTF_8)
                + "&q=" + q
                + "&num=" + Math.min(Math.max(num, 1), 10)
                + "&hl=vi&gl=vn&safe=active";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(18))
                .header("User-Agent", "JavaHttpClient")
                .GET()
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("HTTP " + resp.statusCode() + " — " + resp.body());
        }

        JsonNode root = mapper.readTree(resp.body());
        JsonNode items = root.path("items");
        List<ResultItem> out = new ArrayList<>();
        if (items.isArray()) {
            for (JsonNode it : items) {
                String title = it.path("title").asText("");
                String link = it.path("link").asText("");
                String snippet = it.path("snippet").asText("");
                String displayLink = it.path("displayLink").asText(hostOf(link));
                if (!link.isBlank()) out.add(new ResultItem(title, link, snippet, displayLink));
            }
        }
        return out;
    }

    private void buildDomainsFromResults(List<ResultItem> items) {
        LinkedHashSet<String> domains = new LinkedHashSet<>();
        for (ResultItem it : items) domains.add(it.displayLink);
        domainModel.clear();
        domainModel.addElement("Tất cả");
        for (String d : domains) domainModel.addElement(d);
    }

    private void applyFilter() {
        resultModel.clear();
        for (ResultItem it : allResults) {
            if ("Tất cả".equals(domainFilter) || it.displayLink.equalsIgnoreCase(domainFilter)) {
                resultModel.addElement(it);
            }
        }
        if (resultModel.size() == 0) {
            resultModel.addElement(new ResultItem("Không có kết quả", "", "", ""));
        }
    }

    private static String hostOf(String url) {
        try { return URI.create(url).getHost(); }
        catch (Exception e) { return url; }
    }

    private void openBrowser(String url) {
        if (url == null || url.isBlank()) return;
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception ignored) {}
    }

    // Result item & renderer
    private static class ResultItem {
        final String title, link, snippet, displayLink;
        ResultItem(String title, String link, String snippet, String displayLink) {
            this.title = title == null ? "" : title;
            this.link = link == null ? "" : link;
            this.snippet = snippet == null ? "" : snippet;
            this.displayLink = displayLink == null ? "" : displayLink;
        }
        @Override public String toString() { return title; }
    }

    private class ResultRenderer extends JPanel implements ListCellRenderer<ResultItem> {
        private final JLabel lbTitle = new JLabel();
        private final JLabel lbMeta  = new JLabel();
        private final JTextArea taSnippet = new JTextArea();

        ResultRenderer() {
            setLayout(new BorderLayout(8, 6));
            setBorder(new EmptyBorder(10, 10, 10, 10));
            setOpaque(true);

            lbTitle.setFont(lbTitle.getFont().deriveFont(Font.BOLD, 14f));
            lbMeta.setForeground(new Color(110, 110, 110));

            taSnippet.setLineWrap(true);
            taSnippet.setWrapStyleWord(true);
            taSnippet.setEditable(false);
            taSnippet.setOpaque(false);
            taSnippet.setForeground(new Color(120, 20, 20));

            JPanel top = new JPanel(new BorderLayout(8, 0));
            top.setOpaque(false);
            top.add(lbTitle, BorderLayout.CENTER);
            top.add(lbMeta, BorderLayout.EAST);

            add(top, BorderLayout.NORTH);
            add(taSnippet, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ResultItem> list, ResultItem value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            lbTitle.setText(value.title.isBlank() ? value.link : value.title);
            lbMeta.setText(value.displayLink);
            taSnippet.setText(value.snippet);

            Color bg = isSelected ? new Color(230, 242, 255) : Color.WHITE;
            setBackground(bg);

            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(isSelected ? new Color(120, 180, 255) : new Color(238, 238, 238), 1, true),
                    new EmptyBorder(10, 10, 10, 10)
            ));
            return this;
        }
    }
}
