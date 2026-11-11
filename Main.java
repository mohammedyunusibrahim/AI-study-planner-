// AI Study Planner - single-file version
// Build: javac -encoding UTF-8 Main.java   |   Run: java Main

import java.awt.*;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

// ======================================================================
// Main
// ======================================================================
/**
 * Desktop host for {@link StudyApplet}.
 * Modern JDKs cannot run applets in a browser, so this main() method
 * places the applet panel in a normal window.
 */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SplashScreen.SplashHandle splash=SplashScreen.open();
            Path root=Path.of("").toAbsolutePath();
            javax.swing.SwingWorker<AppContext,Void> loader=new javax.swing.SwingWorker<>() {
                protected AppContext doInBackground() {
                    return new AppContext(root.resolve("data").resolve("planner.dat"),root.resolve("data").resolve("questions.csv"));
                }
                protected void done() {
                    splash.close();
                    try {
                        AppContext context = get();
                        WelcomeScreen.open(context, () -> launch(context));
                    }
                    catch(Exception ex){javax.swing.JOptionPane.showMessageDialog(null,"Could not load the planner data: "+ex.getMessage(),"Startup error",javax.swing.JOptionPane.ERROR_MESSAGE);}
                }
            };
            loader.execute();
        });
    }

    private static void launch(AppContext context) {
            StudyApplet applet = new StudyApplet();
            applet.init(context);
            if (!applet.ready) {
                return;
            }
            JFrame frame = new JFrame("Study-Wise • AI Study Planner");
            java.awt.image.BufferedImage logo = AppBranding.loadImage();
            if (logo != null) frame.setIconImage(logo);
            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            frame.setContentPane(applet);
            frame.setMinimumSize(new Dimension(1050, 690));
            frame.setSize(1200, 790);
            frame.setLocationRelativeTo(null);
            applet.attachClosing(frame);
            frame.setVisible(true);
            applet.start();
    }
}

// ======================================================================
// AppContext
// ======================================================================
/** Shared services handed to every screen. */
class AppContext {
    public final AppData data;
    public final DataStore store;
    public final RecommendationService planner;
    public final CompetencyAnalyzer competency;
    public final KnowledgeModel knowledge = new KnowledgeModel();
    public final AiProvider mentor = new ConfigurableAiProvider();
    public final QuizService quiz;
    public StudyApplet applet;

    public AppContext(Path dataFile, Path questionsFile) {
        store = new DataStore(dataFile);
        data = store.load();
        data.ensureCollections();
        Theme.get().setFontSizePercent(data.profile.fontSizePercent);
        // Migrate the previous purple/dark defaults to the new Study-Wise light-teal design.
        if ("#BA84FF".equalsIgnoreCase(data.profile.accentHex)) {
            data.profile.accentHex = "#117764";
            data.profile.darkTheme = false;
            try { store.save(data); } catch (Exception ignored) { }
        }
        competency = new CompetencyAnalyzer();
        planner = new RuleBasedPlanner(competency);
        quiz = new QuizService(new QuizBankLoader(questionsFile));
        Theme.get().setDark(data.profile.darkTheme);
        try { Theme.get().setAccent(Color.decode(data.profile.accentHex)); }
        catch (Exception ignored) { data.profile.accentHex="#BA84FF"; }
    }

    public void saveQuietly() {
        try {
            store.save(data);
        } catch (Exception ignored) {
            // UI callers that need a message should catch save() instead.
        }
    }

    public void saveOrWarn() {
        try {
            store.save(data);
        } catch (Exception ex) {
            javax.swing.JOptionPane.showMessageDialog(applet,
                    "Could not save: " + ex.getMessage(),
                    "AI Study Planner",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }
}

// ======================================================================
// SelfCheck
// ======================================================================
/** Small dependency-free checks for core adaptive-learning math and schedule insertion. */
final class SelfCheck {
    private SelfCheck() { }
    public static void main(String[] args) {
        check("SM-2 intervals and reset after a missed answer", KnowledgeModel.selfCheckSm2());
        check("Mastery rises on recall and falls on a miss", KnowledgeModel.selfCheckMasteryUpdate());
        check("Due topic is inserted into generated schedule", KnowledgeModel.selfCheckScheduler());
        check("Generated quiz returns requested 5 and 10 question sets with four valid choices", MaterialQuizGenerator.selfCheckGeneration());
        System.out.println("Study planner and quiz self-checks passed.");
    }
    private static void check(String name, boolean success) {
        if (!success) throw new IllegalStateException("Self-check failed: " + name);
        System.out.println("PASS: " + name);
    }
}

// ======================================================================
// StudyApplet
// ======================================================================
/**
 * Applet-style application root.
 * <p>
 * Real {@code javax.swing.JApplet} was removed in JDK 17, so this panel
 * keeps the same lifecycle: {@code init()}, {@code start()}, {@code stop()},
 * {@code destroy()}. {@link Main} hosts it inside a JFrame. On JDK 8 you
 * could wrap the same class with appletviewer using {@code StudyApplet.html}.
 */
class StudyApplet extends JPanel {
    public AppContext ctx;
    public String focusSubject = "General";
    public String focusTopic = "";
    public boolean ready;

    private Sidebar sidebar;
    private SlidePane slides;
    private String screen = "Dashboard";

    public StudyApplet() {
        super(new BorderLayout());
        setOpaque(false);
    }

    /** Build UI and load saved data. Return false if the user cancelled onboarding. */
    public void init() {
        Path root = Path.of("").toAbsolutePath();
        init(new AppContext(root.resolve("data").resolve("planner.dat"),
                root.resolve("data").resolve("questions.csv")));
    }

    /** Build the Swing interface using a context loaded while the startup splash is visible. */
    public void init(AppContext loadedContext) {
        ctx = loadedContext;
        ctx.applet = this;
        if (!Onboarding.run(ctx)) {
            ready = false;
            return;
        }
        DailyCheckIn.run(ctx);
        ready = true;

        sidebar = new Sidebar(ctx, this::showScreen, this::toggleTheme);
        slides = new SlidePane();
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(new AssistantLayer(slides, () -> MentorChatDialog.show(ctx)), BorderLayout.CENTER);
        center.setBorder(javax.swing.BorderFactory.createEmptyBorder());

        add(sidebar, BorderLayout.WEST);
        add(center, BorderLayout.CENTER);

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control N"), "newTask");
        getActionMap().put("newTask", new javax.swing.AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                showScreen("Planner");
                PlannerScreen.quickAdd(ctx);
            }
        });
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control Q"), "openQuiz");
        getActionMap().put("openQuiz", new javax.swing.AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { showScreen("Quiz"); }
        });
        applyThemePaint();
        showScreen("Dashboard");
    }

    public void start() {
        // Applet lifecycle hook: window is showing.
    }

    public void stop() {
        if (ctx != null) {
            ctx.saveQuietly();
        }
    }

    public void destroy() {
        if (ctx != null) {
            ctx.saveQuietly();
        }
    }

    public void showScreen(String name) {
        screen = name;
        if (sidebar != null) {
            sidebar.setActive(name);
        }
        JPanel next;
        switch (name) {
            case "Planner":
            case "Study Planner":
                next = PlannerScreen.build(ctx);
                break;
            case "Subjects":
                next = SubjectsScreen.build(ctx);
                break;
            case "Upload Material":
                next = UploadMaterialScreen.build(ctx);
                break;
            case "Daily Quiz":
                next = QuizScreen.buildDailyPractice(ctx);
                break;
            case "Quiz":
            case "Quiz Center":
                next = QuizScreen.build(ctx);
                break;
            case "Mentor":
            case "AI Mentor":
                next = MentorScreen.build(ctx);
                break;
            case "Goals":
            case "Goals & Readiness":
                next = GoalsScreen.build(ctx);
                break;
            case "Insights":
            case "Weekly Insights":
                next = InsightsScreen.build(ctx);
                break;
            case "Progress":
            case "Progress & Weak Areas":
                next = ProgressScreen.build(ctx);
                break;
            case "Revision Notes":
                next = RevisionNotesScreen.build(ctx);
                break;
            case "Current Affairs / Daily Practice":
                next = DailyPracticeScreen.build(ctx);
                break;
            case "Settings":
                next = SettingsScreen.build(ctx);
                break;
            default:
                next = DashboardScreen.build(ctx);
                break;
        }
        installTooltips(next);
        slides.showPanel(next);
    }

    private void installTooltips(java.awt.Container container) {
        for (java.awt.Component component : container.getComponents()) {
            if (component instanceof javax.swing.AbstractButton) {
                javax.swing.AbstractButton button=(javax.swing.AbstractButton)component;
                if (button.getToolTipText()==null || button.getToolTipText().isBlank())
                    button.setToolTipText(button.getText()==null?"Click to activate":button.getText());
            }
            if (component instanceof java.awt.Container) installTooltips((java.awt.Container)component);
        }
    }

    public void refreshTheme() { applyThemePaint(); if(sidebar!=null)sidebar.refreshTheme(); repaint(); }

    public void toggleTheme() {
        Theme.get().toggle();
        try { Theme.get().setAccent(java.awt.Color.decode(ctx.data.profile.accentHex)); } catch (Exception ignored) { }
        ctx.data.profile.darkTheme = Theme.get().dark;
        ctx.saveQuietly();
        applyThemePaint();
        sidebar.refreshTheme();
        showScreen(screen);
    }

    private void applyThemePaint() {
        setBackground(Theme.get().bg);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(Theme.get().bg);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }

    public void attachClosing(JFrame frame) {
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                destroy();
                frame.dispose();
            }
        });
    }

    /** Keeps the mentor shortcut visible above every screen and its scroll content. */
    private static final class AssistantLayer extends JLayeredPane {
        private final SlidePane content;
        private final AccentButton mentor = AccentButton.primary("✦  Ask your mentor");

        AssistantLayer(SlidePane content, Runnable openMentor) {
            this.content = content;
            setOpaque(false);
            mentor.setToolTipText("Open a resizable study mentor chat without leaving this screen");
            mentor.setPreferredSize(new Dimension(190, 44));
            mentor.addActionListener(e -> openMentor.run());
            add(content, Integer.valueOf(DEFAULT_LAYER));
            add(mentor, Integer.valueOf(PALETTE_LAYER));
        }

        @Override public void doLayout() {
            content.setBounds(0, 0, getWidth(), getHeight());
            Dimension size = mentor.getPreferredSize();
            mentor.setBounds(Math.max(16, getWidth() - size.width - 24),
                    Math.max(16, getHeight() - size.height - 22), size.width, size.height);
        }
    }
}

// ======================================================================
// AiProvider
// ======================================================================
/** Interface boundary for local or future online study mentors. */
interface AiProvider {
    String reply(AppContext context, String message);

    /** Context-aware extension that can answer follow-up questions about loaded material. */
    default String replyWithMaterial(AppContext context, String message, String material) {
        if (material != null && !material.isBlank()) {
            return new MaterialAnalyzer().answerFromMaterial(material, message);
        }
        return reply(context, message);
    }

    /** Produces a local study-focused material report unless a configured provider overrides it. */
    default String analyzeMaterial(AppContext context, String material) {
        return new MaterialAnalyzer().analyze(material, context.data.profile.examType);
    }
}

// ======================================================================
// CompetencyAnalyzer
// ======================================================================
/** Turns quiz history into "this topic is weak" flags the planner can use. */
class CompetencyAnalyzer {

    public static class TopicScore {
        public final String subject;
        public final String topic;
        public final int attempts;
        public final int correct;
        public final int percent;
        public final boolean weak;

        public TopicScore(String subject, String topic, int attempts, int correct, int percent, boolean weak) {
            this.subject = subject;
            this.topic = topic;
            this.attempts = attempts;
            this.correct = correct;
            this.percent = percent;
            this.weak = weak;
        }
    }

    public List<TopicScore> scores(AppData data) {
        Map<String, int[]> buckets = new LinkedHashMap<>();
        for (QuizAttempt attempt : data.quizAttempts) {
            String key = attempt.subject + "\n" + attempt.topic;
            int[] pair = buckets.get(key);
            if (pair == null) {
                pair = new int[2];
                buckets.put(key, pair);
            }
            pair[0]++;
            if (attempt.correct) {
                pair[1]++;
            }
        }
        List<TopicScore> result = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : buckets.entrySet()) {
            String[] parts = entry.getKey().split("\n", 2);
            int attempts = entry.getValue()[0];
            int correct = entry.getValue()[1];
            int percent = attempts == 0 ? 0 : (int) Math.round(100.0 * correct / attempts);
            boolean weak = attempts >= 2 && percent < data.profile.weakThreshold;
            result.add(new TopicScore(parts[0], parts[1], attempts, correct, percent, weak));
        }
        return result;
    }

    public List<TopicScore> weakTopics(AppData data) {
        List<TopicScore> weak = new ArrayList<>();
        for (CompetencyAnalyzer.TopicScore score : scores(data)) {
            if (score.weak) {
                weak.add(score);
            }
        }
        return weak;
    }

    /** Extra weight for the scheduler: 0 if unknown, up to 1.0 if very weak. */
    public double gapWeight(AppData data, String subject, String topic) {
        for (CompetencyAnalyzer.TopicScore score : scores(data)) {
            if (score.subject.equalsIgnoreCase(subject) && score.topic.equalsIgnoreCase(topic)) {
                if (score.attempts == 0) {
                    return 0.35;
                }
                double miss = 1.0 - score.percent / 100.0;
                return score.weak ? 0.5 + miss * 0.5 : miss * 0.3;
            }
        }
        return 0.35;
    }

    public int subjectPercent(AppData data, String subject) {
        int attempts = 0;
        int correct = 0;
        for (QuizAttempt attempt : data.quizAttempts) {
            if (attempt.subject.equalsIgnoreCase(subject)) {
                attempts++;
                if (attempt.correct) {
                    correct++;
                }
            }
        }
        if (attempts == 0) {
            Subject found = data.findSubject(subject);
            if (found == null) {
                return 50;
            }
            return found.strength * 20;
        }
        return (int) Math.round(100.0 * correct / attempts);
    }

    /** Push quiz gaps back onto topic strength so the next generated plan tilts toward them. */
    public void applyGapsToSubjects(AppData data) {
        for (CompetencyAnalyzer.TopicScore score : weakTopics(data)) {
            Subject subject = data.findSubject(score.subject);
            if (subject == null) {
                continue;
            }
            for (Topic topic : subject.topics) {
                if (topic.name.equalsIgnoreCase(score.topic)) {
                    topic.strength = Math.max(1, topic.strength - 1);
                }
            }
        }
    }
}

// ======================================================================
// ConfigurableAiProvider
// ======================================================================
/** Uses online AI only after an explicit app opt-in and a valid environment API key. */
final class ConfigurableAiProvider implements AiProvider {
    private final RuleBasedMentor local = new RuleBasedMentor();
    private final MaterialAnalyzer analyzer = new MaterialAnalyzer();
    private final OpenAiResponsesClient online = new OpenAiResponsesClient();

    @Override public String reply(AppContext ctx, String message) {
        return replyWithMaterial(ctx, message, ctx.data.activeMaterialText);
    }

    @Override public String replyWithMaterial(AppContext ctx, String message, String material) {
        boolean hasMaterial = material != null && !material.isBlank();
        if (onlineEnabled(ctx)) {
            try {
                return online.respond(instructions(ctx), chatInput(ctx, message, material));
            } catch (Exception ex) {
                return "I couldn't reach the online AI mentor (" + cleanError(ex) + "). "
                        + localAnswer(ctx, message, material, hasMaterial);
            }
        }
        String answer = localAnswer(ctx, message, material, hasMaterial);
        if (ctx.data.profile.onlineAiEnabled && !hasApiKey()) {
            answer += "\n\nOnline AI is enabled in Settings, but OPENAI_API_KEY is missing. Using the local mentor for now.";
        }
        return answer;
    }

    @Override public String analyzeMaterial(AppContext ctx, String material) {
        String localReport = analyzer.analyze(material, ctx.data.profile.examType);
        if (!onlineEnabled(ctx)) {
            String note = ctx.data.profile.onlineAiEnabled
                    ? "Online AI is enabled in Settings, but OPENAI_API_KEY is missing; this is a local study brief."
                    : "Local analysis is active. Enable optional online AI in Settings for generated tutor-style analysis.";
            return localReport + "\n\n" + note;
        }
        try {
            return online.respond("You are a careful " + ctx.data.profile.examType
                            + " study coach. Analyze only the supplied material. Separate summary, key points, important terms/facts/dates, difficulty, realistic study time, and a short study plan. State when a fact is not present; do not invent it.",
                    "Analyze this uploaded study material:\n\n" + truncate(material, 26000));
        } catch (Exception ex) {
            return localReport + "\n\nOnline analysis was unavailable: " + cleanError(ex) + ". Showing the local study brief.";
        }
    }

    private String localAnswer(AppContext ctx, String message, String material, boolean hasMaterial) {
        String q = message == null ? "" : message.toLowerCase(Locale.ROOT);
        boolean asksAboutFile = q.contains("material") || q.contains("document") || q.contains("file")
                || q.contains("notes") || q.contains("summar") || q.contains("define") || q.contains("explain this");
        if (hasMaterial && asksAboutFile) return analyzer.answerFromMaterial(material, message);
        return local.reply(ctx, message);
    }

    private static boolean onlineEnabled(AppContext ctx) {
        return ctx.data.profile.onlineAiEnabled && hasApiKey();
    }

    private static boolean hasApiKey() {
        String key = System.getenv("OPENAI_API_KEY");
        return key != null && !key.isBlank();
    }

    private static String instructions(AppContext ctx) {
        return "You are Study-Wise, a supportive " + ctx.data.profile.examType + " learning coach. Be concise and practical. "
                + "Use the student's saved mastery and planner information below. Do not claim an answer is based on their data unless it appears here. "
                + "For exam current affairs, do not invent recent facts; ask for a source if none is supplied.\n\n" + learnerSummary(ctx);
    }

    private static String learnerSummary(AppContext ctx) {
        StringBuilder out = new StringBuilder("Learner: ").append(ctx.data.profile.name)
                .append("; exam: ").append(ctx.data.profile.examType).append(".\n");
        KnowledgeTopic weakest = ctx.data.knowledge.values().stream().min(Comparator.comparingInt(k -> k.mastery)).orElse(null);
        if (weakest != null) out.append("Lowest mastery: ").append(weakest.subject).append(" / ").append(weakest.topic)
                .append(" at ").append(weakest.mastery).append("%.\n");
        StudyTask next = ctx.planner.nextTask(ctx.data);
        if (next != null) out.append("Next planner task: ").append(next.label()).append(" ( ").append(next.minutes).append(" min).\n");
        out.append("Recent conversation:\n");
        int from = Math.max(0, ctx.data.mentorChat.size() - 8);
        for (int i = from; i < ctx.data.mentorChat.size(); i++) {
            ChatMessage m = ctx.data.mentorChat.get(i);
            out.append(m.role).append(": ").append(truncate(m.text, 500)).append('\n');
        }
        return out.toString();
    }

    private static String chatInput(AppContext ctx, String message, String material) {
        StringBuilder out = new StringBuilder();
        if (material != null && !material.isBlank()) out.append("Current uploaded material:\n").append(truncate(material, 22000)).append("\n\n");
        out.append("Student's latest message:\n").append(message == null ? "" : message);
        return out.toString();
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) return value == null ? "" : value;
        return value.substring(0, max) + "\n[content shortened for the AI request]";
    }

    private static String cleanError(Exception ex) {
        String message = ex.getMessage();
        return message == null ? ex.getClass().getSimpleName() : message.replaceAll("[\\r\\n]+", " ");
    }
}

// ======================================================================
// DailyCheckIn
// ======================================================================
/** Once-a-day check-in and cautious, explainable burnout signals. */
final class DailyCheckIn {
    private DailyCheckIn() { }

    public static void run(AppContext ctx) {
        Profile profile = ctx.data.profile;
        LocalDate today = LocalDate.now();
        if (today.equals(profile.checkInDate)) return;
        JSpinner energy = new JSpinner(new SpinnerNumberModel(profile.energy, 1, 5, 1));
        JSpinner available = new JSpinner(new SpinnerNumberModel(
                Math.min(profile.availableMinutes, profile.todayAvailableMinutes <= 0 ? profile.availableMinutes : profile.todayAvailableMinutes),
                0, Math.max(15, profile.availableMinutes), 15));
        JComboBox<String> mood = new JComboBox<>(new String[]{"Great", "Okay", "Low", "Stressed"});
        mood.setSelectedItem(profile.mood == null ? "Okay" : profile.mood);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Energy (1 low – 5 high)")); form.add(energy);
        form.add(new JLabel("How are you feeling?")); form.add(mood);
        form.add(new JLabel("Study minutes available today")); form.add(available);
        int answer = JOptionPane.showConfirmDialog(ctx.applet, form,
                "Good morning, " + profile.name + " — quick check-in", JOptionPane.OK_CANCEL_OPTION);
        if (answer != JOptionPane.OK_OPTION) return;
        profile.energy = (Integer) energy.getValue();
        profile.mood = (String) mood.getSelectedItem();
        profile.todayAvailableMinutes = (Integer) available.getValue();
        profile.checkInDate = today;
        adjustToday(ctx);
        ctx.saveQuietly();
        String signals = burnoutAdvice(ctx);
        if (!signals.isBlank()) JOptionPane.showMessageDialog(ctx.applet, signals,
                "A gentle check on your study rhythm", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void adjustToday(AppContext ctx) {
        LocalDate today = LocalDate.now();
        int cap = ctx.data.profile.todayAvailableMinutes;
        if (cap <= 0) return;
        int used = ctx.data.minutesStudiedOn(today);
        int left = Math.max(0, cap - used);
        java.util.List<StudyTask> tasks = ctx.data.tasks.stream()
                .filter(t -> !t.done && t.day.equals(today) && !StudyTask.MANUAL.equals(t.kind))
                .sorted(java.util.Comparator.comparingInt((StudyTask t) -> t.priority).reversed())
                .toList();
        for (StudyTask task : tasks) {
            if (left <= 0) { task.day = today.plusDays(1); continue; }
            int energyLimit = ctx.data.profile.energy <= 2 ? 20 : Integer.MAX_VALUE;
            task.minutes = Math.max(0, Math.min(task.minutes, Math.min(left, energyLimit)));
            left -= task.minutes;
        }
    }

    /** Returns signals with evidence, never a diagnosis. */
    public static String burnoutAdvice(AppContext ctx) {
        LocalDate today = LocalDate.now();
        Set<LocalDate> sessionDays = new HashSet<>();
        boolean longBlock = false;
        for (StudySession session : ctx.data.sessions) {
            if (!session.date.isBefore(today.minusDays(7))) sessionDays.add(session.date);
            if (!session.date.isBefore(today.minusDays(7)) && session.actualMinutes >= 90) longBlock = true;
        }
        int plannedMisses = 0;
        for (int i = 1; i <= 7; i++) {
            LocalDate day = today.minusDays(i);
            boolean hadPlan = ctx.data.tasks.stream().anyMatch(t -> t.day.equals(day));
            if (hadPlan && !sessionDays.contains(day)) plannedMisses++;
        }
        boolean falling = false;
        if (ctx.data.quizAttempts.size() >= 12) {
            int n = ctx.data.quizAttempts.size();
            double oldRate = ctx.data.quizAttempts.subList(Math.max(0, n - 20), n - 10).stream().filter(a -> a.correct).count() / 10.0;
            double recentRate = ctx.data.quizAttempts.subList(n - 10, n).stream().filter(a -> a.correct).count() / 10.0;
            falling = recentRate <= oldRate - 0.2;
        }
        if (plannedMisses < 2 && !falling && !longBlock && !"Stressed".equals(ctx.data.profile.mood)) return "";
        StringBuilder text = new StringBuilder("Your recent records suggest it may help to take an easier day. This is only a gentle pattern check, not a diagnosis.\n\n");
        if (plannedMisses >= 2) text.append("• ").append(plannedMisses).append(" planned days in the last week had no recorded study session.\n");
        if (falling) text.append("• Accuracy in your latest 10 answers is lower than the 10 before them.\n");
        if (longBlock) text.append("• At least one recent study session was 90 minutes or longer; schedule a break between long blocks.\n");
        if ("Stressed".equals(ctx.data.profile.mood) || "Low".equals(ctx.data.profile.mood)) text.append("• You checked in as feeling ").append(ctx.data.profile.mood.toLowerCase()).append(".\n");
        text.append("Consider lowering today's goal, taking a full break, or postponing a lower-priority task.");
        return text.toString();
    }
}

// ======================================================================
// GamificationService
// ======================================================================
/** Awards modest XP for actual answered questions and records milestone badges. */
final class GamificationService {
    public static final class Award {
        public final int oldLevel, newLevel, gained, totalXp;
        public final List<String> newBadges;
        Award(int oldLevel, int newLevel, int gained, int totalXp, List<String> badges) {
            this.oldLevel=oldLevel; this.newLevel=newLevel; this.gained=gained; this.totalXp=totalXp; this.newBadges=badges;
        }
    }

    public Award awardQuiz(AppContext ctx, int correct, int total) {
        ctx.data.ensureCollections();
        int oldLevel = level(ctx.data.xp);
        int gained = Math.max(0, correct) * 10 + Math.max(0, total - correct) * 2 + (total > 0 && correct == total ? 20 : 0);
        ctx.data.xp += gained;
        List<String> unlocked = new ArrayList<>();
        badge(ctx, "7-day streak", DashboardScreenStreak.streak(ctx) >= 7, unlocked);
        badge(ctx, "100 questions answered", ctx.data.quizAttempts.size() >= 100, unlocked);
        for (KnowledgeTopic topic : ctx.data.knowledge.values()) {
            if (topic.attempts >= 3 && topic.mastery >= 80) {
                badge(ctx, "Weak topic conquered: " + topic.subject + " / " + topic.topic, true, unlocked);
            }
        }
        return new Award(oldLevel, level(ctx.data.xp), gained, ctx.data.xp, unlocked);
    }

    public static int level(int xp) { return Math.max(1, xp / 100 + 1); }
    public static int progressInLevel(int xp) { return Math.floorMod(xp, 100); }

    private static void badge(AppContext ctx, String name, boolean qualifies, List<String> unlocked) {
        if (qualifies && !ctx.data.badges.contains(name)) {
            ctx.data.badges.add(name);
            unlocked.add(name);
        }
    }

    /** Keeps streak computation local to the service and based on real sessions. */
    private static final class DashboardScreenStreak {
        static int streak(AppContext ctx) {
            java.util.Set<java.time.LocalDate> days = new java.util.HashSet<>();
            ctx.data.sessions.stream().filter(s -> s.actualMinutes > 0).forEach(s -> {days.add(s.date);if(s.minutesOn(s.date.plusDays(1))>0)days.add(s.date.plusDays(1));});
            int n=0; java.time.LocalDate d=days.contains(java.time.LocalDate.now())?java.time.LocalDate.now():java.time.LocalDate.now().minusDays(1);
            while(days.contains(d)){n++;d=d.minusDays(1);} return n;
        }
    }
}

// ======================================================================
// KnowledgeModel
// ======================================================================
/** Local adaptive knowledge model. No network or generated/fabricated scores. */
final class KnowledgeModel {
    public static String key(String subject, String topic) {
        return (subject.trim() + "\n" + topic.trim()).toLowerCase(java.util.Locale.ROOT);
    }

    public KnowledgeTopic getOrCreate(AppData data, String subject, String topic) {
        data.ensureCollections();
        String key = key(subject, topic);
        KnowledgeTopic state = data.knowledge.get(key);
        if (state == null) {
            state = new KnowledgeTopic();
            state.subject = subject;
            state.topic = topic;
            Subject found = data.findSubject(subject);
            if (found != null) {
                for (Topic item : found.topics) {
                    if (item.name.equalsIgnoreCase(topic)) {
                        state.mastery = (int) Math.round(item.strength * 20.0);
                        break;
                    }
                }
            }
            data.knowledge.put(key, state);
        }
        return state;
    }

    /** Applies a quiz answer using the SM-2 interval/ease-factor update. */
    public KnowledgeTopic recordQuiz(AppData data, String subject, String topic, boolean correct, LocalDate on) {
        return recordQuiz(data, subject, topic, correct ? 1.0 : 0.0, on);
    }

    /** One aggregate result per topic per quiz; accuracy determines SM-2 quality 0–5. */
    public KnowledgeTopic recordQuiz(AppData data, String subject, String topic, double accuracy, LocalDate on) {
        KnowledgeTopic state = getOrCreate(data, subject, topic);
        accuracy=Math.max(0,Math.min(1,accuracy));
        int quality=(int)Math.round(accuracy*5);
        state.attempts++;
        if (accuracy>=0.5) state.correct++;
        int oldMastery = state.mastery;
        state.mastery = clamp((int) Math.round(oldMastery + (accuracy>=0.6
                ? Math.max(2, (100 - oldMastery) * accuracy * 0.22)
                : -Math.max(4, oldMastery * (0.34-accuracy*0.18)))));
        state.confidence = Math.max(0.05, Math.min(0.99,
                state.confidence * 0.72 + accuracy * 0.28));

        if (quality < 3) {
            state.repetitions = 0;
            state.intervalDays = 1;
        } else {
            state.repetitions++;
            if (state.repetitions == 1) state.intervalDays = 1;
            else if (state.repetitions == 2) state.intervalDays = 6;
            else state.intervalDays = Math.max(1, (int) Math.round(state.intervalDays * state.easeFactor));
        }
        state.easeFactor = Math.max(1.3, state.easeFactor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02)));
        state.lastStudied = on;
        state.nextReview = on.plusDays(state.intervalDays);
        syncStrength(data, state);
        return state;
    }

    /** Records a real focus session without guessing that exposure equals mastery. */
    public void recordStudy(AppData data, String subject, String topic, LocalDate on) {
        if(subject==null||subject.isBlank()||topic==null||topic.isBlank())return;
        KnowledgeTopic state=getOrCreate(data,subject,topic);
        state.lastStudied=on;
        if(state.attempts==0 && (state.nextReview==null||!state.nextReview.isAfter(on))) {
            state.intervalDays=1;
            state.nextReview=on.plusDays(1);
        }
    }

    public List<KnowledgeTopic> dueTopics(AppData data, LocalDate through) {
        data.ensureCollections();
        List<KnowledgeTopic> due = new ArrayList<>();
        for (KnowledgeTopic topic : data.knowledge.values()) {
            if (topic.nextReview != null && !topic.nextReview.isAfter(through)) due.add(topic);
        }
        due.sort(Comparator.comparing((KnowledgeTopic t) -> t.nextReview)
                .thenComparingInt(t -> t.mastery));
        return due;
    }

    public String explain(KnowledgeTopic state) {
        if (state.correct < state.attempts) {
            return state.topic + " is scheduled sooner because a recent answer was missed. SM-2 set its next review for " + state.nextReview + ".";
        }
        return state.topic + " is scheduled for " + state.nextReview + " based on " + state.intervalDays
                + " day(s) of successful recall (SM-2).";
    }

    public static boolean selfCheckSm2() {
        AppData data = new AppData();
        KnowledgeModel model = new KnowledgeModel();
        LocalDate day = LocalDate.of(2026, 1, 1);
        KnowledgeTopic first = model.recordQuiz(data, "History", "Reform", true, day);
        if (first.intervalDays != 1 || !day.plusDays(1).equals(first.nextReview)) return false;
        int masteryAfterCorrect = first.mastery;
        KnowledgeTopic failed = model.recordQuiz(data, "History", "Reform", false, day.plusDays(1));
        return failed.repetitions == 0 && failed.intervalDays == 1 && failed.mastery < masteryAfterCorrect;
    }

    public static boolean selfCheckMasteryUpdate() {
        AppData data = new AppData();
        KnowledgeModel model = new KnowledgeModel();
        int initial = model.getOrCreate(data, "Science", "Cells").mastery;
        KnowledgeTopic correct = model.recordQuiz(data, "Science", "Cells", true, LocalDate.now());
        int raised = correct.mastery;
        KnowledgeTopic wrong = model.recordQuiz(data, "Science", "Cells", false, LocalDate.now());
        return raised > initial && wrong.mastery < raised && wrong.confidence > 0;
    }

    public static boolean selfCheckScheduler() {
        AppData data = new AppData();
        Subject subject = new Subject("Science", LocalDate.now().plusDays(10), 3);
        subject.topics.add(new Topic("Cells", 2));
        data.subjects.add(subject);
        KnowledgeTopic state = new KnowledgeModel().getOrCreate(data, "Science", "Cells");
        state.lastStudied = LocalDate.now().minusDays(1);
        state.nextReview = LocalDate.now();
        state.intervalDays = 1;
        List<StudyTask> tasks = new RuleBasedPlanner(new CompetencyAnalyzer())
                .generateSchedule(data, LocalDate.now());
        for (StudyTask task : tasks) {
            if (StudyTask.REVISE.equals(task.kind)
                    && task.topic.equals("Cells") && task.day.equals(LocalDate.now())) return true;
        }
        return false;
    }

    private void syncStrength(AppData data, KnowledgeTopic state) {
        Subject subject = data.findSubject(state.subject);
        if (subject == null) return;
        for (Topic topic : subject.topics) {
            if (topic.name.equalsIgnoreCase(state.topic)) {
                topic.strength = Math.max(1, Math.min(5, (int) Math.round(state.mastery / 20.0)));
                break;
            }
        }
    }

    private static int clamp(int value) { return Math.max(0, Math.min(100, value)); }
}

// ======================================================================
// MaterialAnalyzer
// ======================================================================
/** Dependency-free first-pass study report and material-grounded local Q&A. */
final class MaterialAnalyzer {
    private static final Set<String> STOP = new HashSet<>(Arrays.asList(
            "about", "after", "again", "also", "among", "because", "been", "being", "between", "could",
            "does", "during", "each", "from", "have", "into", "more", "most", "other", "over", "same",
            "some", "such", "than", "that", "their", "them", "then", "there", "these", "they", "this",
            "those", "through", "under", "very", "when", "where", "which", "while", "with", "would",
            "your", "will", "were", "what", "shall", "should", "must", "only", "being", "been", "also",
            "have", "has", "had", "the", "and", "for", "are", "was", "you", "not", "but", "can", "its",
            "who", "why", "how", "all", "any", "our", "out", "one", "two", "use", "used", "using",
            "may", "might", "not", "per", "via", "etc"));

    public String analyze(String raw, String examType) {
        String text = normalize(raw);
        if (text.isBlank()) return "No readable text was found. Try another file or paste the text directly.";
        List<String> sentences = sentences(text);
        List<String> keyTerms = topTerms(text, 8);
        List<String> facts = facts(text);
        int words = text.split("\\s+").length;
        int minutes = Math.max(5, (int) Math.ceil(words / 140.0));
        double average = sentences.stream().mapToInt(s -> s.split("\\s+").length).average().orElse(0);
        String difficulty = words > 2500 || average > 25 ? "Advanced"
                : words > 800 || average > 17 ? "Moderate" : "Introductory";

        StringBuilder out = new StringBuilder();
        out.append("MATERIAL STUDY BRIEF · ").append(examType == null ? "EXAM" : examType.toUpperCase(Locale.ROOT)).append("\n\n");
        out.append("SUMMARY\n").append(summarize(sentences, 3)).append("\n\n");
        out.append("KEY TOPICS\n");
        if (keyTerms.isEmpty()) out.append("• Add a longer source to identify topic terms.\n");
        else for (String term : keyTerms) out.append("• ").append(term).append('\n');
        out.append("\nDEFINITIONS AND FACTS\n");
        if (facts.isEmpty()) out.append("• No explicit definition or date pattern was found in this text.\n");
        else for (String fact : facts) out.append("• ").append(fact).append('\n');
        out.append("\nESTIMATED LEVEL & TIME\n").append(difficulty).append(" · about ").append(minutes)
                .append(" minute(s) for a focused first read (").append(words).append(" words).\n\n");
        out.append("SUGGESTED PLAN\n• Read once and mark key terms (about ").append(Math.max(5, minutes / 3)).append(" min).\n")
                .append("• Explain the main ideas from memory.\n• Attempt a short quiz, then review missed points tomorrow.");
        return out.toString();
    }

    public String answerFromMaterial(String material, String question) {
        String normalizedQuestion = question == null ? "" : question.toLowerCase(Locale.ROOT);
        StringBuilder matched = new StringBuilder();
        for (String sentence : sentences(normalize(material))) {
            for (String term : normalizedQuestion.split("[^\\p{L}\\p{N}]+")) {
                if (term.length() >= 4 && sentence.toLowerCase(Locale.ROOT).contains(term)) {
                    if (matched.length() > 0) matched.append(' ');
                    matched.append(sentence);
                    break;
                }
            }
            if (matched.length() > 900) break;
        }
        if (matched.length() == 0) return "I couldn't find a matching passage in the loaded material. Try asking with a topic or term from the document.";
        return "From your uploaded material: " + matched.toString().trim()
                + "\n\nThis is a local passage match. Enable the optional online mentor in Settings for a generated explanation.";
    }

    private static String normalize(String text) {
        return (text == null ? "" : text).replace('\u0000', ' ').replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n").trim();
    }

    private static List<String> sentences(String text) {
        List<String> list = new ArrayList<>();
        for (String sentence : text.split("(?<=[.!?])\\s+|\\n+")) {
            String clean = sentence.replaceAll("\\s+", " ").trim();
            if (clean.length() > 24) list.add(clean);
        }
        return list;
    }

    private static String summarize(List<String> sentences, int count) {
        if (sentences.isEmpty()) return "The source has text, but no complete sentences could be separated.";
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < Math.min(count, sentences.size()); i++) text.append("• ").append(sentences.get(i)).append('\n');
        return text.toString().trim();
    }

    private static List<String> topTerms(String text, int limit) {
        Map<String, Integer> counts = new HashMap<>();
        Matcher matcher = Pattern.compile("[\\p{L}][\\p{L}\\p{N}-]{3,}").matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String word = matcher.group();
            if (!STOP.contains(word)) counts.merge(word, 1, Integer::sum);
        }
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()));
        List<String> terms = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, sorted.size()); i++) terms.add(sorted.get(i).getKey());
        return terms;
    }

    private static List<String> facts(String text) {
        List<String> out = new ArrayList<>();
        Matcher definitions = Pattern.compile("(?i)([^.!?\\n]{3,100}\\b(?:is|are|means|refers to|defined as)\\b[^.!?\\n]{5,160}[.!?]?)").matcher(text);
        while (definitions.find() && out.size() < 8) out.add(definitions.group(1).trim());
        Matcher dates = Pattern.compile("\\b(?:\\d{1,2}[/-]\\d{1,2}[/-](?:\\d{2,4})|\\d{4}|\\d{1,2}\\s+(?:January|February|March|April|May|June|July|August|September|October|November|December)\\s+\\d{4})\\b", Pattern.CASE_INSENSITIVE).matcher(text);
        while (dates.find() && out.size() < 12) out.add("Date or year: " + dates.group());
        return out;
    }
}

// ======================================================================
// MaterialQuizGenerator
// ======================================================================
/** Creates transparent source-grounded four-option MCQs from study material. */
final class MaterialQuizGenerator {
    private static final Logger LOG = Logger.getLogger(MaterialQuizGenerator.class.getName());
    private static final Pattern WORD = Pattern.compile("[\\p{L}][\\p{L}0-9_-]{3,}");
    private static final Set<String> COMMON = new HashSet<>(java.util.Arrays.asList(
            "this", "that", "with", "from", "into", "when", "where", "which", "what", "their", "there", "these", "those", "have", "were", "will", "your", "about", "than", "then", "also", "such", "some", "each", "they", "them", "being", "using", "used", "over", "under", "more", "most", "many", "because", "while", "after", "before", "between", "through", "does", "make", "made", "only", "other", "very", "both", "much", "must", "should", "would", "could", "chapter", "section", "from", "into", "when", "were", "been", "will", "shall", "upon", "such", "also", "these", "those"));

    private MaterialQuizGenerator() { }

    public static List<String> suggestTopics(String text) {
        Map<String, Integer> counts = new HashMap<>();
        Matcher words = WORD.matcher(text == null ? "" : text);
        while (words.find()) {
            String word = words.group();
            if (!COMMON.contains(word.toLowerCase(Locale.ROOT))) counts.merge(word, 1, Integer::sum);
        }
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER)));
        List<String> result = new ArrayList<>();
        for (int i = 0; i < Math.min(12, sorted.size()); i++) result.add(sorted.get(i).getKey());
        return result;
    }

    /** Difficulty influences key-term selection and exam-style wording. */
    public static List<QuizQuestion> generate(String text, String subject, String topic,
                                               int limit, String difficulty) {
        return generate(text, subject, topic, topic, limit, difficulty);
    }

    /** Separates the requested topic filter from the saved topic label. */
    public static List<QuizQuestion> generate(String text, String subject, String topicFilter,
                                               String topicLabel, int limit, String difficulty) {
        List<QuizQuestion> result = new ArrayList<>();
        String safeText = text == null ? "" : text;
        LOG.fine("Material quiz request: sourceChars=" + safeText.length() + ", requested=" + limit
                + ", difficulty=" + difficulty + ", topic=" + topicFilter);
        String selectedTopic = topicFilter == null ? "" : topicFilter.trim();
        String savedTopic = topicLabel == null || topicLabel.isBlank() || "All topics".equalsIgnoreCase(topicLabel)
                ? "Uploaded material" : topicLabel.trim();
        Set<String> vocabulary = new LinkedHashSet<>(suggestTopics(safeText));
        Matcher all = WORD.matcher(safeText);
        while (all.find()) {
            String word = all.group();
            if (!COMMON.contains(word.toLowerCase(Locale.ROOT))) vocabulary.add(word);
        }
        if (vocabulary.size() < 4) return result;

        String level = difficulty == null ? "Medium" : difficulty;
        int minLength = "Hard".equalsIgnoreCase(level) ? 7 : "Medium".equalsIgnoreCase(level) ? 5 : 4;
        String[] sentences = safeText.replaceAll("(?m)^\\s*#.*$", " ").split("(?<=[.!?])\\s+|\\R+");
        List<SentenceSeed> seeds = new ArrayList<>();
        for (String sentence : sentences) {
            String clean = sentence.trim().replaceAll("\\s+", " ");
            if (clean.length() < 35 || clean.length() > 500) continue;
            if (!selectedTopic.isBlank() && !selectedTopic.equalsIgnoreCase("All topics")
                    && !clean.toLowerCase(Locale.ROOT).contains(selectedTopic.toLowerCase(Locale.ROOT))) continue;
            Matcher words = WORD.matcher(clean);
            List<String> candidates = new ArrayList<>();
            while (words.find()) {
                String candidate = words.group();
                if (!COMMON.contains(candidate.toLowerCase(Locale.ROOT)) && candidate.length() >= minLength
                        && candidates.stream().noneMatch(existing -> existing.equalsIgnoreCase(candidate))) candidates.add(candidate);
            }
            if (candidates.isEmpty()) continue;
            String answer = "Hard".equalsIgnoreCase(level) ? candidates.get(candidates.size() - 1) : candidates.get(0);
            seeds.add(new SentenceSeed(clean, candidates));
            QuizQuestion question = makeQuestion(clean, answer, vocabulary, subject,
                    selectedTopic.isBlank() || "All topics".equalsIgnoreCase(selectedTopic) ? savedTopic : selectedTopic, level);
            if (isValidQuestion(question)) result.add(question);
            if (result.size() >= Math.max(1, limit)) break;
        }
        // If the source has fewer complete sentences than requested, create another
        // distinct cloze from each sentence's remaining key terms before giving up.
        for (int variant = 1; result.size() < Math.max(1, limit); variant++) {
            boolean added = false;
            for (SentenceSeed seed : seeds) {
                if (result.size() >= Math.max(1, limit)) break;
                int candidateIndex = "Hard".equalsIgnoreCase(level)
                        ? seed.candidates.size() - 1 - variant : variant;
                if (candidateIndex < 0 || candidateIndex >= seed.candidates.size()) continue;
                QuizQuestion question = makeQuestion(seed.sentence, seed.candidates.get(candidateIndex), vocabulary, subject,
                        selectedTopic.isBlank() || "All topics".equalsIgnoreCase(selectedTopic) ? savedTopic : selectedTopic, level);
                if (isValidQuestion(question)) { result.add(question); added = true; }
            }
            if (!added) break;
        }
        LOG.fine("Material quiz questions created and validated: " + result.size());
        return result;
    }

    /** Shared guard for generated/imported question objects before they reach storage or Swing. */
    public static boolean isValidQuestion(QuizQuestion question) {
        if (question == null || blank(question.subject) || blank(question.topic) || blank(question.prompt)
                || blank(question.explanation) || question.choices == null || question.choices.length != 4
                || question.correctIndex < 0 || question.correctIndex >= 4) return false;
        Set<String> unique = new HashSet<>();
        for (String choice : question.choices) {
            if (blank(choice) || !unique.add(choice.trim().toLowerCase(Locale.ROOT))) return false;
        }
        return true;
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }

    private static QuizQuestion makeQuestion(String sentence, String answer, Set<String> vocabulary,
                                             String subject, String topic, String level) {
        List<String> options = new ArrayList<>(); options.add(answer);
        List<String> distractors = new ArrayList<>(vocabulary);
        distractors.sort(Comparator.comparingInt((String word) -> Math.abs(word.length() - answer.length()))
                .thenComparing(String.CASE_INSENSITIVE_ORDER));
        for (String word : distractors) {
            if (!word.equalsIgnoreCase(answer) && options.stream().noneMatch(existing -> existing.equalsIgnoreCase(word))) options.add(word);
            if (options.size() == 4) break;
        }
        if (options.size() < 4) return null;
        int rotation = Math.floorMod((sentence + answer).hashCode(), options.size());
        List<String> choices = new ArrayList<>(options);
        for (int i = 0; i < rotation; i++) choices.add(choices.remove(0));
        int correct = choices.indexOf(answer);
        String prompt = sentence.replaceFirst("(?i)\\b" + Pattern.quote(answer) + "\\b", "________");
        String style = "Easy".equalsIgnoreCase(level) ? "Complete the statement"
                : "Hard".equalsIgnoreCase(level) ? "Identify the exam concept omitted from this passage"
                : "Choose the key term that completes this source statement";
        return new QuizQuestion("material-" + UUID.randomUUID(), subject, topic,
                style + ": " + prompt, choices.toArray(new String[0]), correct,
                "Source sentence: " + sentence + " The omitted term is “" + answer + "”.");
    }

    private static final class SentenceSeed {
        final String sentence; final List<String> candidates;
        SentenceSeed(String sentence, List<String> candidates) {
            this.sentence = sentence; this.candidates = candidates;
        }
    }

    /** Dependency-free regression check for requested counts and four-option validation. */
    public static boolean selfCheckGeneration() {
        String source = "Federalism divides governmental authority between national institutions and regional governments. "
                + "Judicial review allows courts to examine legislation against constitutional principles. "
                + "Parliamentary democracy makes the cabinet accountable to elected representatives. "
                + "Fundamental rights protect individual freedoms under the written constitution. "
                + "Directive principles guide public policy and social welfare legislation. "
                + "Separation of powers distributes authority among independent branches of government. "
                + "Local governance gives communities representation in public decision making. "
                + "Constitutional amendments require a prescribed legislative procedure. "
                + "Public administration implements policies through accountable civil institutions. "
                + "Electoral commissions supervise voting through transparent national procedures.";
        List<QuizQuestion> five = generate(source, "Polity", "All topics", "Self-check", 5, "Medium");
        List<QuizQuestion> ten = generate(source, "Polity", "All topics", "Self-check", 10, "Medium");
        String focusedSource = "Constitutional federalism divides governmental authority between national institutions and regional governments. "
                + "Constitutional judicial review lets courts examine legislation against written principles. "
                + "Constitutional democracy makes the cabinet accountable to elected representatives. "
                + "Constitutional fundamental rights protect individual freedoms under written law. "
                + "Constitutional directive principles guide public policy and social welfare legislation. "
                + "Constitutional separation of powers distributes authority among independent government branches. "
                + "Constitutional local governance gives communities representation in public decision making. "
                + "Constitutional amendments require a prescribed legislative procedure. "
                + "Constitutional public administration implements policies through accountable civil institutions. "
                + "Constitutional electoral commissions supervise voting through transparent national procedures.";
        List<QuizQuestion> topicFive = generate(focusedSource, "Polity", "Constitution", "Constitution", 5, "Medium");
        List<QuizQuestion> topicTen = generate(focusedSource, "Polity", "Constitution", "Constitution", 10, "Medium");
        QuizQuestion invalid = new QuizQuestion("bad", "Polity", "Test", "Question?",
                new String[]{"A", "B", "C"}, 0, "Explanation");
        return five.size() == 5 && ten.size() == 10
                && topicFive.size() == 5 && topicTen.size() == 10
                && five.stream().allMatch(MaterialQuizGenerator::isValidQuestion)
                && ten.stream().allMatch(MaterialQuizGenerator::isValidQuestion)
                && topicFive.stream().allMatch(MaterialQuizGenerator::isValidQuestion)
                && topicTen.stream().allMatch(MaterialQuizGenerator::isValidQuestion)
                && !isValidQuestion(invalid)
                && generate("too short", "Polity", "All topics", "Empty check", 5, "Medium").isEmpty();
    }

    /** Preserves the original call signature for older local import flows. */
    public static List<QuizQuestion> generate(String text, String subject, String topic, int limit) {
        return generate(text, subject, topic, limit, "Medium");
    }
}

// ======================================================================
// OpenAiResponsesClient
// ======================================================================
/** Small JDK HTTP client for the official OpenAI Responses API; secrets stay in environment variables. */
final class OpenAiResponsesClient {
    private static final URI ENDPOINT = URI.create("https://api.openai.com/v1/responses");

    String respond(String instructions, String input) throws Exception {
        String key = System.getenv("OPENAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("OPENAI_API_KEY is not set.");
        String model = System.getenv("OPENAI_MODEL");
        if (model == null || model.isBlank()) model = "gpt-4.1-mini";
        String body = "{\"model\":" + quote(model) + ",\"instructions\":" + quote(instructions)
                + ",\"input\":" + quote(input) + ",\"max_output_tokens\":1000}";
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(12)).build();
        HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
                .timeout(Duration.ofSeconds(50))
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("OpenAI API returned HTTP " + response.statusCode() + ". Check the key, model, quota, and network connection.");
        }
        String result = extractOutputText(response.body());
        if (result.isBlank()) throw new IllegalStateException("The AI response did not contain readable text.");
        return result;
    }

    private static String quote(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 32) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
            }
        }
        return out.append('"').toString();
    }

    /** Reads text fields only from the response output array; handles JSON string escapes. */
    private static String extractOutputText(String json) {
        int output = json.indexOf("\"output\"");
        if (output < 0) return "";
        StringBuilder out = new StringBuilder();
        int cursor = output;
        while ((cursor = json.indexOf("\"text\"", cursor)) >= 0) {
            int colon = json.indexOf(':', cursor + 6);
            if (colon < 0) break;
            int start = colon + 1;
            while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
            if (start >= json.length() || json.charAt(start) != '"') { cursor = colon + 1; continue; }
            ParseString parsed = parseString(json, start);
            if (out.length() > 0) out.append('\n');
            out.append(parsed.value);
            cursor = parsed.next;
        }
        return out.toString().trim();
    }

    private static ParseString parseString(String json, int quoteAt) {
        StringBuilder value = new StringBuilder();
        int i = quoteAt + 1;
        while (i < json.length()) {
            char c = json.charAt(i++);
            if (c == '"') break;
            if (c != '\\') { value.append(c); continue; }
            if (i >= json.length()) break;
            char escaped = json.charAt(i++);
            switch (escaped) {
                case '"': case '\\': case '/': value.append(escaped); break;
                case 'n': value.append('\n'); break;
                case 'r': value.append('\r'); break;
                case 't': value.append('\t'); break;
                case 'b': value.append('\b'); break;
                case 'f': value.append('\f'); break;
                case 'u':
                    if (i + 4 <= json.length()) {
                        try { value.append((char) Integer.parseInt(json.substring(i, i + 4), 16)); } catch (NumberFormatException ignored) { }
                        i += 4;
                    }
                    break;
                default: value.append(escaped);
            }
        }
        return new ParseString(value.toString(), i);
    }

    private static final class ParseString {
        final String value; final int next;
        ParseString(String value, int next) { this.value = value; this.next = next; }
    }
}

// ======================================================================
// QuizService
// ======================================================================
/** Picks questions and records answers. Stays separate from Swing so it is easy to test. */
class QuizService {
    private final QuizBankLoader loader;
    private List<QuizQuestion> bank = new ArrayList<>();

    public QuizService(QuizBankLoader loader) {
        this.loader = loader;
        reload();
    }

    public void reload() {
        bank = loader.load();
    }

    public List<String> subjects() {
        List<String> names = new ArrayList<>();
        for (QuizQuestion question : bank) {
            if (!names.contains(question.subject)) {
                names.add(question.subject);
            }
        }
        Collections.sort(names);
        return names;
    }

    public List<QuizQuestion> startQuiz(String subject, int count) {
        List<QuizQuestion> pool = new ArrayList<>();
        for (QuizQuestion question : bank) {
            if (subject == null || subject.isBlank() || question.subject.equalsIgnoreCase(subject)) {
                pool.add(question);
            }
        }
        Collections.shuffle(pool, new Random());
        if (pool.size() > count) {
            return new ArrayList<>(pool.subList(0, count));
        }
        return pool;
    }

    /** Builds a quiz from topics the learner has missed before. */
    public List<QuizQuestion> startReviewQuiz(AppData data, int count, boolean weakOnly) {
        List<String> topicKeys = new ArrayList<>();
        if (weakOnly) {
            for (CompetencyAnalyzer.TopicScore score : new CompetencyAnalyzer().weakTopics(data)) {
                topicKeys.add((score.subject + "\n" + score.topic).toLowerCase());
            }
        } else {
            for (QuizAttempt attempt : data.quizAttempts) {
                if (!attempt.correct) topicKeys.add((attempt.subject + "\n" + attempt.topic).toLowerCase());
            }
        }
        List<QuizQuestion> pool = new ArrayList<>();
        for (QuizQuestion question : bank) {
            String key = (question.subject + "\n" + question.topic).toLowerCase();
            if (topicKeys.contains(key)) pool.add(question);
        }
        Collections.shuffle(pool, new Random());
        return pool.size() > count ? new ArrayList<>(pool.subList(0, count)) : pool;
    }

    /** Daily challenge samples up to five bank questions from the learner's lowest mastery topics. */
    public List<QuizQuestion> startDailyChallenge(AppData data) {
        data.ensureCollections();
        List<KnowledgeTopic> weakest = new ArrayList<>(data.knowledge.values());
        weakest.sort(Comparator.comparingInt((KnowledgeTopic t) -> t.mastery)
                .thenComparingDouble(t -> t.confidence));
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < Math.min(5, weakest.size()); i++) {
            keys.add((weakest.get(i).subject + "\n" + weakest.get(i).topic).toLowerCase());
        }
        List<QuizQuestion> pool = new ArrayList<>();
        for (QuizQuestion question : bank) {
            if (keys.contains((question.subject + "\n" + question.topic).toLowerCase())) pool.add(question);
        }
        Collections.shuffle(pool, new Random());
        return pool.size() > 5 ? new ArrayList<>(pool.subList(0, 5)) : pool;
    }

    /** Retakes questions whose most recent recorded answer was incorrect. */
    public List<QuizQuestion> startMistakeQuiz(AppData data, int count) {
        Map<String, QuizAttempt> latest = new TreeMap<>();
        for (QuizAttempt attempt : data.quizAttempts) latest.put(attempt.questionId, attempt);
        List<QuizQuestion> pool = new ArrayList<>();
        for (QuizQuestion question : bank) {
            QuizAttempt attempt = latest.get(question.id);
            if (attempt != null && !attempt.correct) pool.add(question);
        }
        Collections.shuffle(pool, new Random());
        return pool.size() > count ? new ArrayList<>(pool.subList(0, count)) : pool;
    }

    public static void addFlashcard(AppData data, QuizQuestion question) {
        data.ensureCollections();
        for (Flashcard card : data.flashcards) if (card.id.equals(question.id)) return;
        data.flashcards.add(new Flashcard(question.id, question.subject, question.topic,
                question.prompt, question.choices[question.correctIndex], question.explanation));
    }

    public void record(AppData data, QuizQuestion question, int chosenIndex, int secondsUsed) {
        record(data, question, chosenIndex, secondsUsed, false);
    }

    /** Saves a submitted or timer-expired answer. A timeout keeps the selected option. */
    public void record(AppData data, QuizQuestion question, int chosenIndex, int secondsUsed, boolean timedOut) {
        data.ensureCollections();
        QuizAttempt attempt = new QuizAttempt();
        attempt.questionId = question.id;
        attempt.subject = question.subject;
        attempt.topic = question.topic;
        attempt.correct = chosenIndex == question.correctIndex;
        attempt.timedOut = timedOut;
        attempt.secondsUsed = secondsUsed;
        attempt.prompt = question.prompt;
        attempt.explanation = question.explanation;
        attempt.correctAnswer = question.choices[question.correctIndex];
        attempt.learnerAnswer = chosenIndex >= 0 && chosenIndex < question.choices.length
                ? question.choices[chosenIndex] : "Skipped";
        data.quizAttempts.add(attempt);
        if (!attempt.correct) addFlashcard(data, question);
    }

    public int bankSize() {
        return bank.size();
    }

    public String bankSummary() {
        Map<String, Integer> counts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (QuizQuestion question : bank) {
            counts.merge(question.subject + " / " + question.topic, 1, Integer::sum);
        }
        if (counts.isEmpty()) return "Your quiz bank is empty. Upload a TXT or Markdown study file to create practice questions.";
        StringBuilder summary = new StringBuilder("Questions by subject and topic\n\n");
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            summary.append(entry.getKey()).append("  •  ").append(entry.getValue()).append(" questions\n");
        }
        summary.append("\nTotal: ").append(bank.size()).append(" questions");
        return summary.toString();
    }
}

// ======================================================================
// RecommendationService
// ======================================================================
/**
 * Pluggable "AI" brain.
 * Today's implementation is rule-based (see RuleBasedPlanner).
 * Later you can add a class that calls an online API and still return List&lt;StudyTask&gt;.
 */
interface RecommendationService {
    /** Build a day-by-day timetable from subjects, hours, strength, and quiz gaps. */
    List<StudyTask> generateSchedule(AppData data, LocalDate start);

    /** Pick the most useful unfinished task for right now. */
    StudyTask nextTask(AppData data);

    /** Explain why the current plan looks overloaded, without changing tasks. */
    String recoveryAdvice(AppData data);
}

// ======================================================================
// RuleBasedMentor
// ======================================================================
/** Keyword-matched mentor that answers only from this learner's saved local records. */
final class RuleBasedMentor implements AiProvider {
    @Override
    public String reply(AppContext ctx, String message) {
        String q = message == null ? "" : message.trim().toLowerCase(Locale.ROOT);
        if (q.isBlank()) return "Tell me what you want help with: your next task, a topic, a lighter week, or your progress.";
        if (q.contains("weak") || q.contains("struggle") || q.contains("hard")) return topicReply(ctx, message);
        if ((q.contains("lighter") || q.contains("lighten") || q.contains("less")) && (q.contains("week") || q.contains("plan"))) {
            int changed = 0;
            LocalDate end = LocalDate.now().plusDays(7);
            for (StudyTask task : ctx.data.tasks) {
                if (!task.done && !StudyTask.MANUAL.equals(task.kind) && !task.day.isBefore(LocalDate.now()) && !task.day.isAfter(end)) {
                    task.minutes = Math.max(15, (int) Math.round(task.minutes * 0.75)); changed++;
                }
            }
            ctx.saveQuietly();
            return changed == 0 ? "I couldn't find generated study blocks for the next seven days to adjust. Create a plan first."
                    : "I made the next seven days lighter by trimming generated study blocks by about 25% (minimum 15 minutes each). "
                    + changed + " blocks changed. Your manual tasks stayed as you set them.";
        }
        if (q.contains("study now") || q.contains("next task") || q.contains("what should i study") || q.contains("recommend")) {
            StudyTask task = ctx.planner.nextTask(ctx.data);
            if (task == null) return "There are no due tasks in your planner right now. Add a task or generate a study plan.";
            KnowledgeTopic state = ctx.knowledge.getOrCreate(ctx.data, task.subject, task.topic);
            return "Start with " + task.subject + " — " + task.topic + " for " + task.minutes + " minutes (" + task.kind + "). "
                    + "Current mastery estimate: " + state.mastery + "%. Next review: " + state.nextReview + ".";
        }
        if (q.contains("progress") || q.contains("how am i") || q.contains("explain")) return progress(ctx);
        return "I can help with a weak topic, a lighter week, your next study task, or your progress. I use your saved planner and quiz records; I don't send your data online.";
    }

    private String topicReply(AppContext ctx, String input) {
        String q = input.toLowerCase(Locale.ROOT);
        List<KnowledgeTopic> matches = new ArrayList<>();
        for (KnowledgeTopic state : ctx.data.knowledge.values())
            if (q.contains(state.subject.toLowerCase(Locale.ROOT)) || q.contains(state.topic.toLowerCase(Locale.ROOT))) matches.add(state);
        matches.sort(Comparator.comparingInt(s -> s.mastery));
        if (matches.isEmpty()) return "I don't have quiz-based knowledge data matching that topic yet. Add it under Subjects and take a quiz so I can estimate mastery.";
        KnowledgeTopic state = matches.get(0);
        return state.subject + " / " + state.topic + " is at an estimated " + state.mastery + "% mastery, based on "
                + state.attempts + " quiz answer(s). Confidence estimate: " + Math.round(state.confidence * 100)
                + "%. Next review: " + state.nextReview + ". I'd schedule a short retrieval practice block.";
    }

    private String progress(AppContext ctx) {
        int questions = ctx.data.quizAttempts.size();
        int correct = (int) ctx.data.quizAttempts.stream().filter(a -> a.correct).count();
        int minutes = ctx.data.minutesStudiedOn(LocalDate.now());
        int avg = ctx.data.knowledge.isEmpty() ? 0 : (int) Math.round(ctx.data.knowledge.values().stream().mapToInt(k -> k.mastery).average().orElse(0));
        KnowledgeTopic weakest = ctx.data.knowledge.values().stream().min(Comparator.comparingInt(k -> k.mastery)).orElse(null);
        String weakestText = weakest == null ? "No topic quiz history yet." : "Your lowest current estimate is " + weakest.subject + " / " + weakest.topic + " at " + weakest.mastery + "% (" + weakest.nextReview + " review).";
        return "Today's focused study: " + minutes + " minutes. Quiz accuracy: " + correct + "/" + questions
                + (questions == 0 ? " (no answers recorded yet)" : " (" + Math.round(100.0 * correct / questions) + "%).")
                + (avg == 0 ? "" : " Average topic mastery estimate: " + avg + "%. ") + weakestText;
    }
}

// ======================================================================
// RuleBasedPlanner
// ======================================================================
/**
 * Heuristic timetable builder (the local "AI").
 * Weak subjects and flagged quiz topics get more minutes.
 * After a LEARN block we plant revision on +1, +3, +7, +14 days (spaced repetition).
 * Revisions are placed first on those days so we do not overflow the daily hour cap.
 */
class RuleBasedPlanner implements RecommendationService {
    private final CompetencyAnalyzer competency;
    private final KnowledgeModel knowledge = new KnowledgeModel();

    public RuleBasedPlanner(CompetencyAnalyzer competency) {
        this.competency = competency;
    }

    @Override
    public List<StudyTask> generateSchedule(AppData data, LocalDate start) {
        data.ensureCollections();
        List<StudyTask> plan = new ArrayList<>();
        if (data.subjects.isEmpty()) {
            return plan;
        }

        LocalDate lastExam = start;
        for (Subject subject : data.subjects) {
            subject.ensureDefaultTopics();
            if (subject.examDate.isAfter(lastExam)) {
                lastExam = subject.examDate;
            }
        }
        if (lastExam.isAfter(start.plusDays(28))) {
            lastExam = start.plusDays(28);
        }

        Map<LocalDate, List<StudyTask>> revisions = new HashMap<>();
        int daily = Math.max(0, data.profile.availableMinutes);

        // Existing knowledge records, created by real quiz answers, drive exact review dates.
        for (KnowledgeTopic state : data.knowledge.values()) {
            Subject subject = data.findSubject(state.subject);
            if (subject == null || state.nextReview == null || state.nextReview.isAfter(lastExam) || state.mastery >= 95) continue;
            LocalDate reviewDay=state.nextReview.isBefore(start)?start:state.nextReview;
            if(reviewDay.isAfter(subject.examDate))continue;
            StudyTask revise = new StudyTask(state.subject, state.topic,
                    state.mastery < 45 ? 30 : 20, state.mastery < 45 ? 5 : 4,
                    reviewDay, subject.examDate, StudyTask.REVISE);
            revisions.computeIfAbsent(reviewDay, d -> new ArrayList<>()).add(revise);
        }

        for (LocalDate day = start; !day.isAfter(lastExam); day = day.plusDays(1)) {
            int dayBudget = daily;
            if (day.equals(start) && start.equals(data.profile.checkInDate)) {
                dayBudget = Math.max(0, data.profile.todayAvailableMinutes);
            }
            int leftover = dayBudget;

            List<StudyTask> due = revisions.getOrDefault(day, new ArrayList<>());
            for (StudyTask revise : due) {
                if (leftover < 15) {
                    break;
                }
                int minutes = Math.min(leftover, revise.minutes);
                revise.minutes = minutes;
                leftover -= minutes;
                plan.add(revise);
            }

            List<WeightedTopic> pool = new ArrayList<>();
            for (Subject subject : data.subjects) {
                if (day.isAfter(subject.examDate)) {
                    continue;
                }
                for (Topic topic : subject.topics) {
                    pool.add(new WeightedTopic(subject, topic, weight(data, subject, topic, day)));
                }
            }
            if (pool.isEmpty() || leftover < 15) {
                continue;
            }
            pool.sort(Comparator.comparingDouble((WeightedTopic w) -> w.weight).reversed());
            double totalWeight = 0;
            for (WeightedTopic item : pool) {
                totalWeight += item.weight;
            }
            int budget = leftover;
            for (int i = 0; i < pool.size() && leftover >= 15; i++) {
                WeightedTopic item = pool.get(i);
                int share = (int) Math.round(budget * (item.weight / totalWeight));
                share = Math.max(15, Math.min(leftover, share));
                if (day.equals(start) && start.equals(data.profile.checkInDate) && data.profile.energy <= 2)
                    share = Math.min(20, share);
                leftover -= share;
                int priority = item.topic.strength <= 2 ? 5 : item.subject.strength <= 2 ? 4 : 3;
                StudyTask task = new StudyTask(
                        item.subject.name, item.topic.name, share, priority,
                        day, item.subject.examDate, StudyTask.LEARN);
                plan.add(task);
            }
        }
        plan.sort(Comparator.comparing((StudyTask t) -> t.day).thenComparingInt(t -> -t.priority));
        return plan;
    }

    @Override
    public StudyTask nextTask(AppData data) {
        LocalDate today = LocalDate.now();
        return data.tasks.stream()
                .filter(task -> !task.done && !task.day.isAfter(today))
                .max(Comparator.comparingInt(this::urgency))
                .orElse(null);
    }

    @Override
    public String recoveryAdvice(AppData data) {
        List<StudyTask> open = new ArrayList<>();
        for (StudyTask task : data.tasks) {
            if (!task.done) {
                open.add(task);
            }
        }
        open.sort(Comparator.comparingInt(this::urgency).reversed());
        int total = 0;
        for (StudyTask task : open) {
            total += task.minutes;
        }
        int left = Math.max(0, data.profile.availableMinutes - data.minutesStudiedOn(LocalDate.now()));
        StringBuilder text = new StringBuilder();
        text.append("Unfinished work: ").append(TimeFmt.hoursLabel(total)).append(".\n");
        text.append("Time still free today: ").append(TimeFmt.hoursLabel(left)).append(".\n\n");
        text.append("Suggested order (weak + overdue first):\n");
        int shown = 0;
        for (StudyTask task : open) {
            text.append("• ").append(task.label()).append(" — ").append(task.minutes)
                    .append(" min, due ").append(task.due).append(" (").append(task.kind).append(")\n");
            shown++;
            if (shown >= 12) {
                break;
            }
        }
        if (total > left) {
            text.append("\nOverloaded by ").append(total - left)
                    .append(" minutes. Postpone low-priority MANUAL tasks; keep REVISE slots.");
        }
        return text.toString();
    }

    private int urgency(StudyTask task) {
        int score = task.priority * 12 - task.minutes / 5;
        LocalDate today = LocalDate.now();
        if (task.due.isBefore(today) && !task.done) {
            score += 80;
        } else if (task.due.equals(today)) {
            score += 45;
        } else if (task.due.equals(today.plusDays(1))) {
            score += 25;
        }
        if (StudyTask.REVISE.equals(task.kind)) {
            score += 10;
        }
        return score;
    }

    private double weight(AppData data, Subject subject, Topic topic, LocalDate day) {
        KnowledgeTopic state = data.knowledge.get(KnowledgeModel.key(subject.name, topic.name));
        double retention = state == null ? topic.strength / 5.0 : state.estimatedRetention(day);
        double weakness = 1.0 - retention;
        if (data.profile.energy >= 4) weakness *= 1.25;
        long daysLeft = Math.max(1, ChronoUnit.DAYS.between(day, subject.examDate));
        double urgency = 1.0 / daysLeft;
        double gap = state == null ? competency.gapWeight(data, subject.name, topic.name)
                : Math.max(competency.gapWeight(data, subject.name, topic.name), 1.0 - state.mastery / 100.0);
        double subjectWeak = (6 - subject.strength) / 5.0;
        return 0.35 * weakness + 0.25 * subjectWeak + 0.2 * Math.min(1.0, urgency * 7)
                + 0.2 * gap + (data.profile.energy >= 4 ? 0.15 * (1.0 - retention) : 0);
    }

    private static class WeightedTopic {
        final Subject subject;
        final Topic topic;
        final double weight;

        WeightedTopic(Subject subject, Topic topic, double weight) {
            this.subject = subject;
            this.topic = topic;
            this.weight = weight;
        }
    }
}

// ======================================================================
// AppData
// ======================================================================
/** Everything that is saved to disk. Keep this as one object so save/load stays simple. */
class AppData implements Serializable {
    private static final long serialVersionUID = 2L;

    public Profile profile = new Profile();
    public ArrayList<Subject> subjects = new ArrayList<>();
    public ArrayList<StudyTask> tasks = new ArrayList<>();
    public ArrayList<StudySession> sessions = new ArrayList<>();
    public ArrayList<QuizAttempt> quizAttempts = new ArrayList<>();
    public ArrayList<String> favorites = new ArrayList<>();
    public ArrayList<String> distractions = new ArrayList<>();
    public HashMap<String, String> reflections = new HashMap<>();
    /** Per-topic adaptive learning state; lazily initialized for older saved profiles. */
    public HashMap<String, KnowledgeTopic> knowledge = new HashMap<>();
    public int xp;
    public ArrayList<String> badges = new ArrayList<>();
    public ArrayList<Flashcard> flashcards = new ArrayList<>();
    public ArrayList<ChatMessage> mentorChat = new ArrayList<>();
    public String activeMaterialName = "";
    public String activeMaterialText = "";
    public String activeMaterialAnalysis = "";

    public void ensureCollections() {
        if (profile == null) profile = new Profile();
        if (profile.targetScore <= 0) profile.targetScore = 80;
        if (profile.fontSizePercent < 85 || profile.fontSizePercent > 130) profile.fontSizePercent = 100;
        if (profile.examType == null || profile.examType.isBlank()) profile.examType = "UPSC";
        if (profile.todayAvailableMinutes <= 0) profile.todayAvailableMinutes = profile.availableMinutes;
        if (profile.mood == null) profile.mood = "Okay";
        if (profile.accentHex == null) profile.accentHex = "#BA84FF";
        if (subjects == null) subjects = new ArrayList<>();
        if (tasks == null) tasks = new ArrayList<>();
        if (sessions == null) sessions = new ArrayList<>();
        if (quizAttempts == null) quizAttempts = new ArrayList<>();
        if (favorites == null) favorites = new ArrayList<>();
        if (distractions == null) distractions = new ArrayList<>();
        if (reflections == null) reflections = new HashMap<>();
        if (knowledge == null) knowledge = new HashMap<>();
        if (badges == null) badges = new ArrayList<>();
        if (flashcards == null) flashcards = new ArrayList<>();
        if (mentorChat == null) mentorChat = new ArrayList<>();
        if (activeMaterialName == null) activeMaterialName = "";
        if (activeMaterialText == null) activeMaterialText = "";
        if (activeMaterialAnalysis == null) activeMaterialAnalysis = "";
    }

    /** Used after restoring a backup so we keep the same AppData instance. */
    public void replaceWith(AppData other) {
        profile = other.profile;
        subjects = other.subjects;
        tasks = other.tasks;
        sessions = other.sessions;
        quizAttempts = other.quizAttempts;
        favorites = other.favorites;
        distractions = other.distractions;
        reflections = other.reflections;
        knowledge = other.knowledge;
        xp = other.xp;
        badges = other.badges;
        flashcards = other.flashcards;
    }

    public Subject findSubject(String name) {
        for (Subject subject : subjects) {
            if (subject.name.equalsIgnoreCase(name)) {
                return subject;
            }
        }
        return null;
    }

    public int minutesStudiedOn(LocalDate day) {
        int total = 0;
        for (StudySession session : sessions) {
            total += session.minutesOn(day);
        }
        return total;
    }
}

// ======================================================================
// ChatMessage
// ======================================================================
/** One locally saved message in the floating mentor conversation. */
class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    public String role = "assistant";
    public String text = "";
    public LocalDateTime sentAt = LocalDateTime.now();

    public ChatMessage() { }

    public ChatMessage(String role, String text) {
        this.role = role;
        this.text = text;
        this.sentAt = LocalDateTime.now();
    }
}

// ======================================================================
// Flashcard
// ======================================================================
/** Local retrieval-practice card created from a missed quiz question or imported notes. */
class Flashcard implements Serializable {
    private static final long serialVersionUID = 1L;
    public String id="";
    public String subject="";
    public String topic="";
    public String front="";
    public String back="";
    public String explanation="";
    public LocalDate nextReview=LocalDate.now();
    public int intervalDays;
    public int repetitions;
    public Flashcard() { }
    public Flashcard(String id,String subject,String topic,String front,String back,String explanation){
        this.id=id;this.subject=subject;this.topic=topic;this.front=front;this.back=back;this.explanation=explanation;
    }
    public void review(boolean recalled){
        if(recalled){repetitions++;intervalDays=repetitions==1?1:repetitions==2?6:Math.max(1,intervalDays*2);}
        else{repetitions=0;intervalDays=1;}
        nextReview=LocalDate.now().plusDays(intervalDays);
    }
}

// ======================================================================
// KnowledgeTopic
// ======================================================================
/** A topic's retained knowledge and SM-2 review state. */
class KnowledgeTopic implements Serializable {
    private static final long serialVersionUID = 1L;
    public String subject = "";
    public String topic = "";
    public int mastery = 50;
    public double confidence = 0.5;
    public LocalDate lastStudied;
    public LocalDate nextReview = LocalDate.now();
    public int intervalDays;
    public double easeFactor = 2.5;
    public int repetitions;
    public int attempts;
    public int correct;

    public double estimatedRetention(LocalDate today) {
        if (lastStudied == null) return mastery / 100.0;
        long elapsed = Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(lastStudied, today));
        double halfLife = Math.max(1.0, intervalDays * Math.max(0.5, easeFactor / 2.5));
        return Math.max(0.0, Math.min(1.0, mastery / 100.0 * Math.pow(0.5, elapsed / halfLife)));
    }
}

// ======================================================================
// Profile
// ======================================================================
/** Saved student profile, study preferences, and exam settings. */
class Profile implements Serializable {
    private static final long serialVersionUID = 1L;

    public String name = "";
    public String course = "";
    public int availableMinutes = 180;
    public int dailyGoalMinutes = 90;
    public int quizSeconds = 30;
    /** Energy 1–5 influences how today's plan is balanced. */
    public int energy = 3;
    public String mood = "Okay";
    public LocalDate checkInDate;
    public int todayAvailableMinutes = 180;
    public java.time.LocalDate dailyChallengeDate;
    public java.time.LocalDate dailyPracticeDate;
    public java.time.LocalDate targetExamDate;
    public int targetScore = 80;
    public String accentHex = "#117764";
    public int fontSizePercent = 100;
    public String examType = "UPSC";
    /** Explicit opt-in: document text may be sent to OpenAI when an API key is configured. */
    public boolean onlineAiEnabled = false;
    public boolean darkTheme = false;
    /** Quiz average below this percent is treated as a weak topic. */
    public int weakThreshold = 60;
}

// ======================================================================
// QuizAttempt
// ======================================================================
/** One answered question, used later to find weak topics. */
class QuizAttempt implements Serializable {
    private static final long serialVersionUID = 1L;

    public String questionId = "";
    public String subject = "";
    public String topic = "";
    public boolean correct;
    /** True when the countdown ended and the selected answer was auto-saved. */
    public boolean timedOut;
    public int secondsUsed;
    public String prompt = "";
    public String explanation = "";
    public String correctAnswer = "";
    public String learnerAnswer = "";
    public LocalDateTime when = LocalDateTime.now();
    public String quizSessionId = "";
    public int quizSessionSize;
    public int quizSessionScore;
    public int quizSessionSeconds;
}

// ======================================================================
// QuizQuestion
// ======================================================================
/** One multiple-choice item loaded from CSV. Not serialized; the file is the source. */
class QuizQuestion {
    public final String id;
    public final String subject;
    public final String topic;
    public final String prompt;
    public final String[] choices;
    public final int correctIndex;
    public final String explanation;

    public QuizQuestion(String id, String subject, String topic, String prompt,
                        String[] choices, int correctIndex, String explanation) {
        this.id = id;
        this.subject = subject;
        this.topic = topic;
        this.prompt = prompt;
        this.choices = choices;
        this.correctIndex = correctIndex;
        this.explanation = explanation;
    }
}

// ======================================================================
// StudySession
// ======================================================================
/** A recorded study session used by progress and insight reports. */
class StudySession implements Serializable {
    private static final long serialVersionUID = 1L;

    public LocalDate date = LocalDate.now();
    public String subject = "General";
    public String topic = "";
    public int plannedMinutes;
    public int actualMinutes;
    public boolean complete;
    /** Real start time used for time-of-day insights; null for older saved sessions. */
    public LocalDateTime startedAt;

    public StudySession() {
    }

    public StudySession(String subject, int plannedMinutes, int actualMinutes, boolean complete) {
        this(subject, plannedMinutes, actualMinutes, complete, LocalDateTime.now());
    }

    public StudySession(String subject, int plannedMinutes, int actualMinutes, boolean complete, LocalDateTime startedAt) {
        this(subject,"",plannedMinutes,actualMinutes,complete,startedAt);
    }

    public StudySession(String subject, String topic, int plannedMinutes, int actualMinutes, boolean complete, LocalDateTime startedAt) {
        this.subject = subject;
        this.topic = topic;
        this.plannedMinutes = plannedMinutes;
        this.actualMinutes = actualMinutes;
        this.complete = complete;
        this.startedAt = startedAt;
        if (startedAt != null) this.date = startedAt.toLocalDate();
    }

    /** Counts the real focus minutes overlapping a calendar day, including midnight crossings. */
    public int minutesOn(LocalDate day) {
        if (startedAt == null) return date.equals(day) ? actualMinutes : 0;
        LocalDateTime from=startedAt;
        LocalDateTime to=startedAt.plusMinutes(Math.max(0,actualMinutes));
        LocalDateTime dayStart=day.atStartOfDay(),dayEnd=day.plusDays(1).atStartOfDay();
        long seconds=Math.max(0,java.time.Duration.between(from.isAfter(dayStart)?from:dayStart,
                to.isBefore(dayEnd)?to:dayEnd).getSeconds());
        return (int)Math.round(seconds/60.0);
    }
}

// ======================================================================
// StudyTask
// ======================================================================
/** One planned block: either a generated slot or a task the student added. */
class StudyTask implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String LEARN = "LEARN";
    public static final String REVISE = "REVISE";
    public static final String QUIZ = "QUIZ";
    public static final String MANUAL = "MANUAL";

    public String subject = "";
    public String topic = "";
    public int minutes = 30;
    public int priority = 3;
    public LocalDate day = LocalDate.now();
    public LocalDate due = LocalDate.now().plusDays(1);
    public boolean done = false;
    public String kind = MANUAL;

    public StudyTask() {
    }

    public StudyTask(String subject, String topic, int minutes, int priority,
                     LocalDate day, LocalDate due, String kind) {
        this.subject = subject;
        this.topic = topic;
        this.minutes = minutes;
        this.priority = priority;
        this.day = day;
        this.due = due;
        this.kind = kind;
    }

    public String label() {
        return subject + " — " + topic;
    }
}

// ======================================================================
// Subject
// ======================================================================
/** A course the student is preparing for, with an exam date and topics. */
class Subject implements Serializable {
    private static final long serialVersionUID = 1L;

    public String name = "";
    public LocalDate examDate = LocalDate.now().plusWeeks(4);
    /** Overall comfort with the subject, 1 = weakest. */
    public int strength = 3;
    public ArrayList<Topic> topics = new ArrayList<>();

    public Subject() {
    }

    public Subject(String name, LocalDate examDate, int strength) {
        this.name = name;
        this.examDate = examDate;
        this.strength = Topic.clampStrength(strength);
    }

    public void ensureDefaultTopics() {
        if (!topics.isEmpty()) {
            return;
        }
        topics.add(new Topic("Core ideas", strength));
        topics.add(new Topic("Practice", strength));
        topics.add(new Topic("Exam review", strength));
    }
}

// ======================================================================
// Topic
// ======================================================================
/** One unit of study inside a subject. Strength is 1 (weak) to 5 (strong). */
class Topic implements Serializable {
    private static final long serialVersionUID = 1L;

    public String name = "";
    public int strength = 3;

    public Topic() {
    }

    public Topic(String name, int strength) {
        this.name = name;
        this.strength = clampStrength(strength);
    }

    public static int clampStrength(int value) {
        return Math.max(1, Math.min(5, value));
    }
}

// ======================================================================
// DataStore
// ======================================================================
/** Saves the whole AppData object next to a .bak copy from the previous save. */
class DataStore {
    private final Path file;

    public DataStore(Path file) {
        this.file = file;
    }

    public AppData load() {
        if (!Files.exists(file)) {
            return new AppData();
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            Object raw = in.readObject();
            if (raw instanceof AppData) {
                AppData data = (AppData) raw;
                data.ensureCollections();
                return data;
            }
        } catch (Exception ignored) {
            // Caller shows a friendly message; we fall back to a blank profile.
        }
        return new AppData();
    }

    public void save(AppData data) throws Exception {
        Files.createDirectories(file.getParent());
        Path tmp = Path.of(file.toString() + ".tmp");
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(tmp))) {
            out.writeObject(data);
        }
        Path backup = Path.of(file.toString() + ".bak");
        if (Files.exists(file)) {
            Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
    }

    public Path backupPath() {
        return Path.of(file.toString() + ".bak");
    }

    public void restoreBackup() throws Exception {
        Path backup = backupPath();
        if (!Files.exists(backup)) {
            throw new IllegalStateException("No backup exists yet.");
        }
        Files.copy(backup, file, StandardCopyOption.REPLACE_EXISTING);
    }

    public Path file() {
        return file;
    }
}

// ======================================================================
// MaterialExtractor
// ======================================================================
/** JDK-only readers for common study formats, plus optional local Tesseract OCR. */
final class MaterialExtractor {
    private static final long MAX_BYTES = 8L * 1024 * 1024;
    private static final int MAX_CHARS = 500_000;

    public String read(Path path) throws Exception {
        if (path == null || !Files.isRegularFile(path)) throw new IllegalArgumentException("Choose an existing file.");
        if (Files.size(path) > MAX_BYTES) throw new IllegalArgumentException("This file is larger than 8 MB. Choose a smaller file.");
        String name = path.getFileName().toString();
        String ext = extension(name);
        String result;
        switch (ext) {
            case "txt": case "md": case "markdown": case "csv":
                result = Files.readString(path, StandardCharsets.UTF_8); break;
            case "html": case "htm":
                result = htmlToText(Files.readString(path, StandardCharsets.UTF_8)); break;
            case "docx": result = officeXml(path, "word/document.xml", "word/", "w:t"); break;
            case "pptx": result = officeXml(path, null, "ppt/slides/slide", "a:t"); break;
            case "pdf": result = pdfText(Files.readAllBytes(path)); break;
            case "png": case "jpg": case "jpeg": case "bmp": case "tif": case "tiff":
                result = runOcr(path); break;
            default: throw new IllegalArgumentException("Unsupported file type: ." + ext
                    + ". Supported formats: PDF, DOCX, PPTX, TXT, Markdown, HTML, and common images.");
        }
        result = result == null ? "" : result.replace('\u0000', ' ').trim();
        if (result.length() > MAX_CHARS) result = result.substring(0, MAX_CHARS);
        if (result.isBlank()) {
            if ("pdf".equals(ext)) throw new IllegalArgumentException("No selectable PDF text was found. This may be a scanned PDF; install Tesseract OCR and upload page images, or paste readable text.");
            throw new IllegalArgumentException("No readable text was found in this file.");
        }
        return result;
    }

    public String readWebLink(String rawUrl) throws Exception {
        URI uri = URI.create(rawUrl == null ? "" : rawUrl.trim());
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("Enter a complete http:// or https:// web address.");
        }
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(12000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "StudyWise/1.0 (material text reader)");
        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) throw new IllegalArgumentException("The page returned HTTP " + status + ".");
            String contentType = connection.getContentType();
            if (contentType != null && !(contentType.toLowerCase(Locale.ROOT).contains("text/html")
                    || contentType.toLowerCase(Locale.ROOT).contains("text/plain"))) {
                throw new IllegalArgumentException("This web link is not an HTML/text page (" + contentType + "). Download the file and upload it instead.");
            }
            try (InputStream in = connection.getInputStream()) {
                byte[] bytes = readLimited(in, MAX_BYTES);
                String document = new String(bytes, StandardCharsets.UTF_8);
                return contentType != null && contentType.toLowerCase(Locale.ROOT).contains("text/plain")
                        ? document : htmlToText(document);
            }
        } finally { connection.disconnect(); }
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static byte[] readLimited(InputStream in, long limit) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        long total = 0;
        while ((read = in.read(buffer)) >= 0) {
            total += read;
            if (total > limit) throw new IllegalArgumentException("The content is larger than 8 MB.");
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static String htmlToText(String html) throws Exception {
        StringBuilder text = new StringBuilder();
        HTMLEditorKit.ParserCallback callback = new HTMLEditorKit.ParserCallback() {
            @Override public void handleText(char[] data, int pos) { text.append(data).append(' '); }
            @Override public void handleStartTag(HTML.Tag tag, MutableAttributeSet attrs, int pos) {
                if (tag == HTML.Tag.P || tag == HTML.Tag.DIV || tag == HTML.Tag.BR || tag == HTML.Tag.LI
                        || tag == HTML.Tag.H1 || tag == HTML.Tag.H2 || tag == HTML.Tag.H3) text.append('\n');
            }
        };
        new ParserDelegator().parse(new StringReader(html), callback, true);
        return text.toString();
    }

    private static String officeXml(Path path, String exact, String prefix, String tag) throws Exception {
        StringBuilder out = new StringBuilder();
        try (ZipFile zip = new ZipFile(path.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                boolean wanted = exact != null ? exact.equals(name)
                        : name.startsWith(prefix) && name.endsWith(".xml");
                if (!wanted || entry.isDirectory()) continue;
                if (entry.getSize() > MAX_BYTES) throw new IllegalArgumentException("An embedded document part is too large.");
                try (InputStream in = zip.getInputStream(entry)) {
                    byte[] data = readLimited(in, MAX_BYTES);
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                    factory.setXIncludeAware(false);
                    factory.setExpandEntityReferences(false);
                    Document doc = factory.newDocumentBuilder().parse(new InputSource(new ByteArrayInputStream(data)));
                    NodeList nodes = doc.getElementsByTagName(tag);
                    for (int i = 0; i < nodes.getLength(); i++) out.append(nodes.item(i).getTextContent()).append(' ');
                    out.append('\n');
                }
            }
        }
        return out.toString();
    }

    /** Best-effort extraction of plain PDF text streams, including standard Flate streams. */
    private static String pdfText(byte[] bytes) throws Exception {
        String raw = new String(bytes, StandardCharsets.ISO_8859_1);
        if (!raw.startsWith("%PDF-")) throw new IllegalArgumentException("This file does not appear to be a valid PDF.");
        StringBuilder all = new StringBuilder();
        int cursor = 0;
        while ((cursor = raw.indexOf("stream", cursor)) >= 0) {
            int dataStart = cursor + 6;
            if (dataStart < raw.length() && raw.charAt(dataStart) == '\r') dataStart++;
            if (dataStart < raw.length() && raw.charAt(dataStart) == '\n') dataStart++;
            int end = raw.indexOf("endstream", dataStart);
            if (end < 0) break;
            int dictStart = raw.lastIndexOf("<<", cursor);
            String dictionary = dictStart >= 0 ? raw.substring(dictStart, cursor) : "";
            byte[] chunk = raw.substring(dataStart, end).getBytes(StandardCharsets.ISO_8859_1);
            if (dictionary.contains("/FlateDecode")) {
                try (InputStream inflate = new InflaterInputStream(new ByteArrayInputStream(chunk))) {
                    chunk = readLimited(inflate, MAX_BYTES);
                } catch (Exception ignored) { cursor = end + 9; continue; }
            }
            extractPdfOperators(new String(chunk, StandardCharsets.ISO_8859_1), all);
            cursor = end + 9;
        }
        return all.toString();
    }

    private static void extractPdfOperators(String stream, StringBuilder out) {
        // Scan PDF string tokens directly. A backtracking regex over compressed or
        // malformed stream bytes can recurse until StackOverflowError on real PDFs.
        int cursor = 0;
        while (cursor < stream.length()) {
            char ch = stream.charAt(cursor);
            if (ch == '(') {
                PdfLiteral literal = readPdfLiteral(stream, cursor);
                if (literal == null) { cursor++; continue; }
                int operator = skipPdfWhitespace(stream, literal.end);
                if (operator < stream.length() && stream.startsWith("Tj", operator)) {
                    out.append(unescapePdf(literal.value)).append(' ');
                    cursor = operator + 2;
                } else if (operator < stream.length()
                        && (stream.charAt(operator) == '\'' || stream.charAt(operator) == '"')) {
                    out.append(unescapePdf(literal.value)).append(' ');
                    cursor = operator + 1;
                } else cursor = literal.end;
            } else if (ch == '[') {
                int end = cursor + 1;
                List<String> parts = new java.util.ArrayList<>();
                while (end < stream.length() && stream.charAt(end) != ']') {
                    if (stream.charAt(end) == '(') {
                        PdfLiteral literal = readPdfLiteral(stream, end);
                        if (literal == null) { end++; continue; }
                        parts.add(literal.value);
                        end = literal.end;
                    } else end++;
                }
                int operator = end < stream.length() ? skipPdfWhitespace(stream, end + 1) : end;
                if (end < stream.length() && stream.startsWith("TJ", operator)) {
                    for (String part : parts) out.append(unescapePdf(part)).append(' ');
                    cursor = operator + 2;
                } else cursor = Math.max(cursor + 1, end + 1);
            } else cursor++;
        }
    }

    private static PdfLiteral readPdfLiteral(String text, int start) {
        StringBuilder value = new StringBuilder();
        int depth = 1;
        int cursor = start + 1;
        while (cursor < text.length()) {
            char ch = text.charAt(cursor++);
            if (ch == '\\' && cursor < text.length()) {
                value.append(ch).append(text.charAt(cursor++));
            } else if (ch == '(') {
                depth++;
                value.append(ch);
            } else if (ch == ')') {
                if (--depth == 0) return new PdfLiteral(value.toString(), cursor);
                value.append(ch);
            } else value.append(ch);
        }
        return null;
    }

    private static int skipPdfWhitespace(String text, int cursor) {
        while (cursor < text.length() && Character.isWhitespace(text.charAt(cursor))) cursor++;
        return cursor;
    }

    private static final class PdfLiteral {
        final String value; final int end;
        PdfLiteral(String value, int end) { this.value = value; this.end = end; }
    }

    private static String unescapePdf(String input) {
        return input.replace("\\n", "\n").replace("\\r", "\n").replace("\\t", " ")
                .replace("\\(", "(").replace("\\)", ")").replace("\\\\", "\\");
    }

    private static String runOcr(Path image) throws Exception {
        try {
            Process process = new ProcessBuilder("tesseract", image.toAbsolutePath().toString(), "stdout")
                    .redirectErrorStream(true).start();
            byte[] result;
            try (InputStream in = process.getInputStream()) { result = readLimited(in, MAX_BYTES); }
            if (!process.waitFor(45, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalArgumentException("OCR took too long. Try a smaller or clearer image.");
            }
            if (process.exitValue() != 0) throw new IllegalArgumentException("OCR could not read this image. Check that Tesseract is installed and that the image is clear.");
            return new String(result, StandardCharsets.UTF_8);
        } catch (java.io.IOException ex) {
            throw new IllegalArgumentException("Image OCR requires Tesseract OCR on PATH. Install Tesseract, then restart Study-Wise. The image has not been uploaded.");
        }
    }
}

// ======================================================================
// QuizBankLoader
// ======================================================================
/**
 * CSV columns: id,subject,topic,question,A,B,C,D,correct,explanation
 * correct is A/B/C/D. Commas inside quotes are supported.
 */
class QuizBankLoader {
    private final Path file;

    public QuizBankLoader(Path file) {
        this.file = file;
    }

    public List<QuizQuestion> load() {
        List<QuizQuestion> questions = new ArrayList<>();
        if (!Files.exists(file)) {
            return questions;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                List<String> cells = splitCsv(line);
                if (header && cells.get(0).equalsIgnoreCase("id")) {
                    header = false;
                    continue;
                }
                header = false;
                if (cells.size() < 10) {
                    continue;
                }
                int correct = "ABCD".indexOf(cells.get(8).trim().toUpperCase());
                if (correct < 0) {
                    continue;
                }
                questions.add(new QuizQuestion(
                        cells.get(0).trim(),
                        cells.get(1).trim(),
                        cells.get(2).trim(),
                        cells.get(3).trim(),
                        new String[]{cells.get(4).trim(), cells.get(5).trim(), cells.get(6).trim(), cells.get(7).trim()},
                        correct,
                        cells.get(9).trim()
                ));
            }
        } catch (IOException ignored) {
            // Empty bank is safer than crashing the app on a bad file.
        }
        return questions;
    }

    private List<String> splitCsv(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                quoted = !quoted;
            } else if (ch == ',' && !quoted) {
                cells.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        cells.add(current.toString());
        return cells;
    }
}

// ======================================================================
// QuizBankWriter
// ======================================================================
/** CSV writer shared by imported-material and generated-quiz workflows. */
final class QuizBankWriter {
    private static final String HEADER = "id,subject,topic,question,A,B,C,D,correct,explanation\n";

    public void append(Path path, List<QuizQuestion> questions) throws Exception {
        if (questions == null || questions.isEmpty()) throw new IllegalArgumentException("There are no questions to save.");
        Files.createDirectories(path.getParent());
        boolean hasContent = Files.exists(path) && Files.size(path) > 0;
        StringBuilder csv = new StringBuilder();
        if (!hasContent) csv.append(HEADER);
        else csv.append('\n');
        for (QuizQuestion q : questions) {
            if (q.choices == null || q.choices.length != 4 || q.correctIndex < 0 || q.correctIndex > 3)
                throw new IllegalArgumentException("Every quiz question must have four options and one correct answer.");
            csv.append(cell(q.id)).append(',').append(cell(q.subject)).append(',').append(cell(q.topic)).append(',')
                    .append(cell(q.prompt));
            for (String choice : q.choices) csv.append(',').append(cell(choice));
            csv.append(',').append((char) ('A' + q.correctIndex)).append(',').append(cell(q.explanation)).append('\n');
        }
        Files.writeString(path, csv, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static String cell(String value) {
        return "\"" + (value == null ? "" : value.replace("\"", "\"\"")) + "\"";
    }
}

// ======================================================================
// AccentButton
// ======================================================================
/** Rounded primary/secondary button with subtle hover feedback. */
class AccentButton extends JButton {
    private boolean hover;
    private boolean filled;

    public AccentButton(String text, boolean filled) {
        super(text);
        this.filled = filled;
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(Theme.get().bodyFont.deriveFont(java.awt.Font.BOLD, 14f));
        setToolTipText(text);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                repaint();
            }
        });
    }

    public static AccentButton primary(String text) {
        return new AccentButton(text, true);
    }

    public static AccentButton ghost(String text) {
        return new AccentButton(text, false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Theme theme = Theme.get();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color fill = filled ? theme.accent : (hover ? theme.accentSoft : theme.card);
        if (filled && hover) fill = fill.brighter();
        int w = getWidth(), h = getHeight();
        g2.setColor(theme.shadow);
        g2.fillRoundRect(2, 3, Math.max(0, w - 4), Math.max(0, h - 5), 16, 16);
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, Math.max(0, w - 2), Math.max(0, h - 3), 16, 16);
        if (!filled) {
            g2.setColor(hover ? theme.accent : new Color(theme.accent.getRed(), theme.accent.getGreen(), theme.accent.getBlue(), 75));
            g2.drawRoundRect(0, 0, Math.max(0, w - 3), Math.max(0, h - 4), 16, 16);
        }
        g2.dispose();
        Color old = getForeground();
        setForeground(filled ? Color.WHITE : theme.text);
        super.paintComponent(g);
        setForeground(old);
    }
}

// ======================================================================
// AnimatedBar
// ======================================================================
class AnimatedBar extends JComponent {
    private float shown;
    private float target;
    private String text = "";
    private final Timer animator;

    public AnimatedBar() {
        setOpaque(false);
        setPreferredSize(new Dimension(200, 28));
        animator = new Timer(16, e -> {
            float diff = target - shown;
            if (Math.abs(diff) < 0.004f) {
                shown = target;
                ((Timer) e.getSource()).stop();
            } else {
                shown += diff * 0.14f;
            }
            repaint();
        });
    }

    public void setProgress(float value, String text) {
        this.target = Math.max(0f, Math.min(1f, value));
        this.text = text;
        if (!animator.isRunning()) {
            animator.start();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Theme theme = Theme.get();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int h = 12;
        int y = (getHeight() - h) / 2;
        g2.setColor(theme.cardAlt);
        g2.fillRoundRect(0, y, getWidth(), h, h, h);
        g2.setColor(theme.accent);
        g2.fillRoundRect(0, y, Math.max(h, (int) (getWidth() * shown)), h, h, h);
        g2.setFont(theme.smallFont);
        g2.setColor(theme.muted);
        g2.drawString(text, 0, y - 2);
        g2.dispose();
    }
}

// ======================================================================
// AppBranding
// ======================================================================
/** Loads the single Study-Wise logo asset for both the brand and window icon. */
final class AppBranding {
    /** Relative to the project folder; change this one value to use another logo. */
    public static final String LOGO_PATH = "resources/images/study-wise-logo.png";
    /** Optional welcome artwork, relative to the project folder. */
    public static final String WELCOME_BANNER_PATH = "resources/images/study-banner.png";

    private static BufferedImage logo;
    private static boolean loaded;
    private static BufferedImage welcomeBanner;
    private static boolean welcomeBannerLoaded;

    private AppBranding() { }

    /** Returns the original image for the operating-system window/taskbar icon. */
    public static synchronized BufferedImage loadImage() {
        if (!loaded) {
            loaded = true;
            Path path = Path.of(LOGO_PATH);
            if (Files.isRegularFile(path)) {
                try {
                    logo = ImageIO.read(path.toFile());
                    if (logo == null) {
                        System.err.println("Study-Wise logo is not a supported image: " + path.toAbsolutePath());
                    }
                } catch (IOException | RuntimeException ex) {
                    System.err.println("Could not load Study-Wise logo from " + path.toAbsolutePath()
                            + ": " + ex.getMessage());
                }
            } else {
                System.err.println("Study-Wise logo not found at " + path.toAbsolutePath()
                        + "; showing the app title without an image.");
            }
        }
        return logo;
    }

    /** Fits the logo to the requested height without distorting its aspect ratio. */
    public static ImageIcon loadIcon(int targetHeight) {
        BufferedImage source = loadImage();
        if (source == null || targetHeight <= 0) return null;
        int width = Math.max(1, (int) Math.round(source.getWidth() * (targetHeight / (double) source.getHeight())));
        BufferedImage scaled = new BufferedImage(width, targetHeight, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(source, 0, 0, width, targetHeight, null);
        graphics.dispose();
        return new ImageIcon(scaled);
    }

    /** Loads optional welcome artwork. A missing image leaves the gradient welcome screen intact. */
    public static synchronized BufferedImage loadWelcomeBanner() {
        if (!welcomeBannerLoaded) {
            welcomeBannerLoaded = true;
            Path path = Path.of(WELCOME_BANNER_PATH);
            if (Files.isRegularFile(path)) {
                try {
                    welcomeBanner = ImageIO.read(path.toFile());
                    if (welcomeBanner == null) {
                        System.err.println("Welcome banner is not a supported image: " + path.toAbsolutePath()
                                + "; showing the gradient welcome background only.");
                    }
                } catch (IOException | RuntimeException ex) {
                    System.err.println("Could not load welcome banner from " + path.toAbsolutePath()
                            + "; showing the gradient welcome background only. " + ex.getMessage());
                }
            } else {
                System.err.println("Welcome banner not found at " + path.toAbsolutePath()
                        + "; showing the gradient welcome background only.");
            }
        }
        return welcomeBanner;
    }
}

// ======================================================================
// CoachArtwork
// ======================================================================
/** A scalable, dependency-free Java2D study illustration used as in-app artwork. */
final class CoachArtwork extends JPanel {
    public CoachArtwork() {
        setOpaque(false);
        setPreferredSize(new Dimension(240, 150));
        setMinimumSize(new Dimension(150, 110));
        setToolTipText("Study Flow mentor artwork, drawn with Java2D");
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double scale = Math.min(getWidth() / 240.0, getHeight() / 150.0);
        g.translate((getWidth() - 240 * scale) / 2, (getHeight() - 150 * scale) / 2);
        g.scale(scale, scale);

        // Soft orbital backdrop and floating progress bubbles.
        g.setColor(new Color(255, 255, 255, 24));
        g.fill(new Ellipse2D.Double(91, 3, 140, 140));
        g.setColor(new Color(255, 255, 255, 35));
        g.fill(new Ellipse2D.Double(118, 17, 94, 94));
        g.setColor(new Color(102, 234, 207));
        g.fill(new Ellipse2D.Double(196, 18, 10, 10));
        g.setColor(new Color(255, 205, 111));
        g.fill(new Ellipse2D.Double(112, 38, 7, 7));

        // Open book, built from rounded pages and a highlighted center fold.
        g.setColor(new Color(24, 22, 55, 50));
        g.fillRoundRect(40, 108, 150, 17, 8, 8);
        g.setPaint(new GradientPaint(48, 52, new Color(255, 255, 255), 119, 113, new Color(220, 215, 255)));
        g.fill(new RoundRectangle2D.Double(40, 43, 77, 69, 12, 12));
        g.setPaint(new GradientPaint(119, 46, new Color(255, 255, 255), 190, 112, new Color(204, 245, 238)));
        g.fill(new RoundRectangle2D.Double(119, 43, 72, 69, 12, 12));
        Color tealLine = Theme.get().accent;
        g.setColor(new Color(tealLine.getRed(), tealLine.getGreen(), tealLine.getBlue(), 115));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(119, 51, 119, 106);
        for (int i = 0; i < 3; i++) {
            g.drawLine(55, 66 + i * 12, 101, 66 + i * 12);
            g.drawLine(135, 66 + i * 12, 175, 66 + i * 12);
        }

        // Rising mastery mark and spark.
        g.setColor(Theme.get().accent);
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawPolyline(new int[]{147, 160, 170, 185}, new int[]{37, 29, 33, 16}, 4);
        g.fillOval(180, 11, 10, 10);
        g.setColor(new Color(255, 201, 104));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(211, 44, 211, 57);
        g.drawLine(205, 50, 218, 50);
        g.dispose();
    }
}

// ======================================================================
// GamificationStrip
// ======================================================================
/** XP meter and medal badges painted with Java2D. */
final class GamificationStrip extends JPanel {
    private final AppData data;
    public GamificationStrip(AppData data) { this.data=data;setOpaque(false);setPreferredSize(new Dimension(300,78));setToolTipText(""); }
    @Override public String getToolTipText(MouseEvent e) {
        int index=(e.getX()-250)/58;
        return index>=0&&index<data.badges.size()?data.badges.get(index):"No badge earned here yet";
    }
    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);Theme t=Theme.get();Graphics2D x=(Graphics2D)g.create();x.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        x.setColor(t.text);x.setFont(t.headingFont.deriveFont(Font.BOLD,15f));x.drawString("LEVEL "+GamificationService.level(data.xp),12,22);
        x.setFont(t.smallFont);x.setColor(t.muted);x.drawString(data.xp+" XP total  •  "+GamificationService.progressInLevel(data.xp)+" / 100 to next level",12,43);
        x.setColor(t.cardAlt);x.fillRoundRect(12,52,210,10,10,10);x.setPaint(new GradientPaint(12,52,t.accent,222,62,t.success));x.fillRoundRect(12,52,(int)(210*GamificationService.progressInLevel(data.xp)/100.0),10,10,10);
        x.setFont(t.smallFont);x.setColor(t.muted);x.drawString("BADGES",248,22);
        int show=Math.min(4,data.badges.size());
        for(int i=0;i<4;i++){int cx=267+i*44,cy=53,r=15;x.setColor(i<show?new Color(255,198,88):t.cardAlt);x.fillOval(cx-r,cy-r,2*r,2*r);if(i<show){Path2D star=new Path2D.Double();for(int j=0;j<10;j++){double a=-Math.PI/2+j*Math.PI/5,rr=j%2==0?11:5,px=cx+rr*Math.cos(a),py=cy+rr*Math.sin(a);if(j==0)star.moveTo(px,py);else star.lineTo(px,py);}star.closePath();x.setColor(new Color(105,64,163));x.fill(star);}else{x.setColor(t.muted);x.drawOval(cx-3,cy-3,6,6);}}
        x.dispose();
    }
}

// ======================================================================
// HoverCard
// ======================================================================
/** Rounded theme card with a light animated lift and tint on hover. */
class HoverCard extends JPanel {
    private float elevation;
    private boolean hovered;
    private final Timer lift = new Timer(16, e -> {
        float target = hovered ? 1f : 0f;
        elevation += (target - elevation) * 0.28f;
        if (Math.abs(target - elevation) < 0.04f) {
            elevation = target;
            ((Timer) e.getSource()).stop();
        }
        repaint();
    });

    public HoverCard() { this(null); }

    public HoverCard(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(17, 19, 19, 19));
        installHover(this);
    }

    @Override protected void addImpl(Component component, Object constraints, int index) {
        super.addImpl(component, constraints, index);
        installHover(component);
    }

    private void installHover(Component component) {
        component.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { animate(true); }
            @Override public void mouseExited(MouseEvent e) { animate(false); }
        });
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) installHover(child);
        }
    }

    private void animate(boolean value) {
        hovered = value;
        if (!lift.isRunning()) lift.start();
    }

    @Override protected void paintComponent(Graphics graphics) {
        Theme theme = Theme.get();
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int offset = Math.round(elevation * 3f);
        g.setColor(theme.shadow);
        g.fillRoundRect(3, 5 - offset, getWidth() - 7, getHeight() - 7, theme.radius, theme.radius);
        g.setColor(hovered ? blend(theme.card, theme.cardAlt, elevation * 0.35f) : theme.card);
        g.fillRoundRect(0, 1 - offset, getWidth() - 6, getHeight() - 8, theme.radius, theme.radius);
        if (hovered) {
            g.setColor(new Color(theme.accent.getRed(), theme.accent.getGreen(), theme.accent.getBlue(), 52));
            g.drawRoundRect(0, 1 - offset, getWidth() - 7, getHeight() - 9, theme.radius, theme.radius);
        }
        g.dispose();
        super.paintComponent(graphics);
    }

    private static Color blend(Color a, Color b, float amount) {
        float p = Math.max(0f, Math.min(1f, amount));
        return new Color((int) (a.getRed() * (1 - p) + b.getRed() * p),
                (int) (a.getGreen() * (1 - p) + b.getGreen() * p),
                (int) (a.getBlue() * (1 - p) + b.getBlue() * p));
    }
}

// ======================================================================
// IconPainter
// ======================================================================
/** Vector nav icons so the app does not need image files. */
final class IconPainter {
    public enum Kind {
        DASHBOARD, PLANNER, SUBJECTS, QUIZ, PROGRESS, GOAL, INSIGHTS, MENTOR, SETTINGS, SUN, MOON
    }

    private IconPainter() {
    }

    public static void paint(Graphics2D g, Kind kind, int x, int y, int size, Color color) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(Math.max(1.6f, size / 12f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int s = size;
        switch (kind) {
            case DASHBOARD:
                g2.draw(new RoundRectangle2D.Float(x, y, s * 0.42f, s * 0.42f, 4, 4));
                g2.draw(new RoundRectangle2D.Float(x + s * 0.55f, y, s * 0.42f, s * 0.28f, 4, 4));
                g2.draw(new RoundRectangle2D.Float(x, y + s * 0.55f, s * 0.42f, s * 0.42f, 4, 4));
                g2.draw(new RoundRectangle2D.Float(x + s * 0.55f, y + s * 0.42f, s * 0.42f, s * 0.55f, 4, 4));
                break;
            case PLANNER:
                g2.drawRoundRect(x, y + 3, s, s - 3, 5, 5);
                g2.drawLine(x, y + s / 3, x + s, y + s / 3);
                g2.drawLine(x + s / 3, y, x + s / 3, y + 8);
                g2.drawLine(x + 2 * s / 3, y, x + 2 * s / 3, y + 8);
                break;
            case SUBJECTS:
                g2.drawRoundRect(x + 2, y + 2, s - 4, s - 4, 6, 6);
                g2.drawLine(x + 7, y + s / 3, x + s - 7, y + s / 3);
                g2.drawLine(x + 7, y + s / 2, x + s - 7, y + s / 2);
                g2.drawLine(x + 7, y + 2 * s / 3, x + s - 10, y + 2 * s / 3);
                break;
            case QUIZ:
                g2.drawOval(x + 1, y + 1, s - 2, s - 2);
                g2.drawArc(x + s / 4, y + s / 5, s / 2, s / 2, 200, 220);
                g2.fillOval(x + s / 2 - 2, y + s - 8, 4, 4);
                break;
            case PROGRESS:
                g2.drawLine(x, y + s, x + s, y + s);
                g2.drawLine(x, y + s, x, y);
                g2.drawPolyline(
                        new int[]{x + 3, x + s / 3, x + s / 2, x + s - 2},
                        new int[]{y + 2 * s / 3, y + s / 2, y + s / 3, y + 6},
                        4);
                break;
            case GOAL:
                g2.drawOval(x+1,y+1,s-2,s-2);g2.drawOval(x+s/4,y+s/4,s/2,s/2);g2.fillOval(x+s/2-2,y+s/2-2,4,4);
                break;
            case INSIGHTS:
                g2.drawRoundRect(x+2,y+2,s-4,s-4,5,5);g2.drawLine(x+6,y+s-5,x+6,y+s/2);g2.drawLine(x+s/2,y+s-5,x+s/2,y+s/3);g2.drawLine(x+s-6,y+s-5,x+s-6,y+5);
                break;
            case SETTINGS:
                g2.drawOval(x + s / 3, y + s / 3, s / 3, s / 3);
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    int x1 = x + s / 2 + (int) (Math.cos(a) * s * 0.28);
                    int y1 = y + s / 2 + (int) (Math.sin(a) * s * 0.28);
                    int x2 = x + s / 2 + (int) (Math.cos(a) * s * 0.46);
                    int y2 = y + s / 2 + (int) (Math.sin(a) * s * 0.46);
                    g2.drawLine(x1, y1, x2, y2);
                }
                break;
            case MENTOR:
                g2.drawRoundRect(x + 2, y + 2, s - 4, s - 6, 6, 6);
                g2.drawLine(x + 6, y + s - 3, x + 4, y + s + 2);
                g2.drawLine(x + 6, y + s / 3, x + s - 6, y + s / 3);
                g2.drawLine(x + 6, y + 2 * s / 3, x + s - 6, y + 2 * s / 3);
                break;
            case SUN:
                g2.drawOval(x + s / 4, y + s / 4, s / 2, s / 2);
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    g2.drawLine(
                            x + s / 2 + (int) (Math.cos(a) * s * 0.32),
                            y + s / 2 + (int) (Math.sin(a) * s * 0.32),
                            x + s / 2 + (int) (Math.cos(a) * s * 0.48),
                            y + s / 2 + (int) (Math.sin(a) * s * 0.48));
                }
                break;
            case MOON:
                g2.drawArc(x + 2, y + 2, s - 4, s - 4, 40, 280);
                break;
            default:
                break;
        }
        g2.dispose();
    }
}

// ======================================================================
// ProgressCharts
// ======================================================================
/** Small, dependency-free Java2D charts used by the Progress dashboard only. */
final class ProgressCharts {
    private ProgressCharts() { }

    public static final class Value {
        public final String label;
        public final int value;
        public final String detail;
        public Value(String label, int value, String detail) {
            this.label = label;
            this.value = Math.max(0, Math.min(100, value));
            this.detail = detail;
        }
    }

    public static final class DayValue {
        public final LocalDate date;
        public final int value;
        public DayValue(LocalDate date, int value) {
            this.date = date;
            this.value = value < 0 ? -1 : Math.min(100, value);
        }
    }

    private abstract static class AnimatedChart extends JPanel {
        float reveal;
        final Timer animation;
        AnimatedChart(Dimension size) {
            setOpaque(false);
            setPreferredSize(size);
            setToolTipText(" ");
            animation = new Timer(16, e -> {
                reveal = Math.min(1f, reveal + 0.035f);
                repaint();
                if (reveal >= 1f) ((Timer) e.getSource()).stop();
            });
            animation.start();
        }
        Graphics2D graphics(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            return g;
        }
    }

    /** Subject accuracy or completion bars with hover details. */
    public static final class SubjectBars extends AnimatedChart {
        private final List<Value> values;
        private final int[] positions;
        private int plotTop, plotBottom;
        public SubjectBars(List<Value> values) {
            super(new Dimension(420, Math.max(190, values.size() * 38 + 45)));
            this.values = new ArrayList<>(values);
            this.positions = new int[values.size()];
        }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Theme theme = Theme.get(); Graphics2D g = graphics(graphics);
            plotTop = 12; plotBottom = getHeight() - 16;
            int labelWidth = Math.min(145, Math.max(85, getWidth() / 3));
            int barX = labelWidth + 10, barWidth = Math.max(60, getWidth() - barX - 48);
            int rowH = Math.max(28, (plotBottom - plotTop) / Math.max(1, values.size()));
            for (int i = 0; i < values.size(); i++) {
                Value v = values.get(i); int y = plotTop + i * rowH;
                positions[i] = y + rowH / 2;
                g.setFont(theme.smallFont); g.setColor(theme.text);
                String label = clip(v.label, labelWidth - 8, g);
                g.drawString(label, 2, y + rowH / 2 + 4);
                g.setColor(theme.cardAlt); g.fillRoundRect(barX, y + 7, barWidth, 12, 12, 12);
                g.setColor(levelColor(v.value, theme));
                g.fillRoundRect(barX, y + 7, Math.max(3, (int) (barWidth * v.value / 100.0 * reveal)), 12, 12, 12);
                g.setColor(theme.muted); g.drawString(v.value + "%", barX + barWidth + 8, y + rowH / 2 + 4);
            }
            g.dispose();
        }
        @Override public String getToolTipText(MouseEvent e) {
            if (values.isEmpty()) return null;
            int best = -1, distance = Integer.MAX_VALUE;
            for (int i = 0; i < positions.length; i++) if (Math.abs(e.getY() - positions[i]) < distance) { best = i; distance = Math.abs(e.getY() - positions[i]); }
            return best >= 0 && distance < 26 ? values.get(best).label + " · " + values.get(best).value + "% · " + values.get(best).detail : null;
        }
    }

    /** Daily quiz accuracy trend for the selected range, with date/score tooltips. */
    public static final class ScoreTrend extends AnimatedChart {
        private final List<DayValue> values;
        private final int[] xPoints;
        private final int[] yPoints;
        public ScoreTrend(List<DayValue> values) {
            super(new Dimension(420, 205));
            this.values = new ArrayList<>(values);
            xPoints = new int[values.size()]; yPoints = new int[values.size()];
        }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Theme theme = Theme.get(); Graphics2D g = graphics(graphics);
            int left = 34, right = getWidth() - 16, top = 18, bottom = getHeight() - 32;
            g.setFont(theme.smallFont); g.setColor(theme.muted);
            for (int mark = 0; mark <= 100; mark += 25) {
                int y = bottom - (bottom - top) * mark / 100;
                g.setColor(theme.cardAlt); g.drawLine(left, y, right, y);
                g.setColor(theme.muted); g.drawString(mark + "%", 1, y + 4);
            }
            int count = Math.max(1, values.size());
            for (int i = 0; i < values.size(); i++) {
                DayValue point = values.get(i);
                int x = left + (right - left) * i / Math.max(1, count - 1);
                int y = bottom - (bottom - top) * Math.max(0, point.value) / 100;
                xPoints[i] = x; yPoints[i] = y;
                if (point.value < 0) continue;
                if (i > 0 && values.get(i - 1).value >= 0) {
                    int prevY = bottom - (bottom - top) * values.get(i - 1).value / 100;
                    g.setColor(theme.accent); g.setStroke(new BasicStroke(2.5f));
                    g.drawLine(xPoints[i - 1], prevY, x, prevY + (int) ((y - prevY) * reveal));
                }
                int shownY = bottom - (int) ((bottom - y) * reveal);
                g.setColor(theme.accent); g.fillOval(x - 4, shownY - 4, 8, 8);
                if (i == 0 || i == count - 1 || count <= 7) {
                    g.setColor(theme.muted); g.drawString(point.date.format(DateTimeFormatter.ofPattern("M/d")), x - 14, getHeight() - 8);
                }
            }
            if (values.stream().noneMatch(v -> v.value >= 0)) {
                g.setFont(theme.smallFont); g.setColor(theme.muted);
                g.drawString("Complete quizzes to build your score trend.", left + 8, (top + bottom) / 2);
            }
            g.dispose();
        }
        @Override public String getToolTipText(MouseEvent e) {
            int best = -1, distance = 14;
            for (int i = 0; i < xPoints.length; i++) {
                int d = (int) Math.hypot(e.getX() - xPoints[i], e.getY() - yPoints[i]);
                if (d < distance) { distance = d; best = i; }
            }
            if (best < 0 || values.get(best).value < 0) return null;
            return values.get(best).date + " · " + values.get(best).value + "% average quiz accuracy";
        }
    }

    /** Study minutes for each day in the selected week. */
    public static final class WeeklyActivity extends AnimatedChart {
        private final List<DayValue> values;
        private final int[] barX;
        private final int[] barY;
        private final int[] barW;
        private final int[] barH;
        public WeeklyActivity(List<DayValue> values) {
            super(new Dimension(420, 205));
            this.values = new ArrayList<>(values);
            barX = new int[values.size()]; barY = new int[values.size()]; barW = new int[values.size()]; barH = new int[values.size()];
        }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Theme theme = Theme.get(); Graphics2D g = graphics(graphics);
            int left = 12, top = 15, bottom = getHeight() - 30, width = Math.max(1, getWidth() - 24);
            int max = 1; for (DayValue v : values) max = Math.max(max, v.value);
            int slot = width / Math.max(1, values.size()), bw = Math.max(12, slot - 13);
            for (int i = 0; i < values.size(); i++) {
                DayValue v = values.get(i); int x = left + i * slot + (slot - bw) / 2;
                int bh = (int) ((bottom - top) * (v.value / (double) max) * reveal);
                barX[i] = x; barW[i] = bw; barH[i] = bh; barY[i] = bottom - bh;
                g.setColor(theme.cardAlt); g.fillRoundRect(x, top, bw, bottom - top, 10, 10);
                g.setColor(theme.accent); g.fillRoundRect(x, bottom - bh, bw, Math.max(0, bh), 10, 10);
                g.setColor(theme.muted); g.setFont(theme.smallFont);
                g.drawString(v.date.getDayOfWeek().toString().substring(0, 1), x + bw / 2 - 3, getHeight() - 9);
            }
            g.dispose();
        }
        @Override public String getToolTipText(MouseEvent e) {
            for (int i = 0; i < values.size(); i++) {
                if (e.getX() >= barX[i] && e.getX() <= barX[i] + barW[i] && e.getY() >= 12 && e.getY() <= getHeight() - 28)
                    return values.get(i).date + " · " + values.get(i).value + " study minutes";
            }
            return null;
        }
    }

    /** Mastery bar that uses the shared weak/average/strong color scale. */
    public static final class TopicMeter extends AnimatedChart {
        private final int score;
        public TopicMeter(int score) {
            super(new Dimension(220, 20));
            this.score = Math.max(0, Math.min(100, score));
        }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics); Theme theme = Theme.get(); Graphics2D g = graphics(graphics);
            int h = 10, y = Math.max(0, (getHeight() - h) / 2), w = getWidth();
            g.setColor(theme.cardAlt); g.fillRoundRect(0, y, w, h, h, h);
            g.setColor(levelColor(score, theme)); g.fillRoundRect(0, y, Math.max(0, (int) (w * score / 100.0 * reveal)), h, h, h);
            g.dispose();
        }
    }

    public static Color levelColor(int score, Theme theme) {
        if (score < 60) return theme.danger;
        if (score < 75) return new Color(219, 145, 45);
        return theme.success;
    }

    private static String clip(String value, int maxWidth, Graphics2D g) {
        String text = value == null ? "" : value;
        while (!text.isEmpty() && g.getFontMetrics().stringWidth(text) > maxWidth) text = text.substring(0, text.length() - 1);
        return text.equals(value) ? text : text + "…";
    }
}

// ======================================================================
// ProgressRing
// ======================================================================
/** Circular progress that eases toward a target value. */
class ProgressRing extends JComponent {
    private float shown;
    private float target;
    private String caption = "";
    private final Timer animator;

    public ProgressRing() {
        setOpaque(false);
        setPreferredSize(new Dimension(160, 160));
        animator = new Timer(16, e -> {
            float diff = target - shown;
            if (Math.abs(diff) < 0.004f) {
                shown = target;
                ((Timer) e.getSource()).stop();
            } else {
                shown += diff * 0.12f;
            }
            repaint();
        });
    }

    public void setProgress(float value, String caption) {
        this.target = Math.max(0f, Math.min(1f, value));
        this.caption = caption;
        if (!animator.isRunning()) {
            animator.start();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Theme theme = Theme.get();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int size = Math.min(getWidth(), getHeight()) - 16;
        int x = (getWidth() - size) / 2;
        int y = (getHeight() - size) / 2;
        g2.setStroke(new BasicStroke(12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(theme.cardAlt);
        g2.drawArc(x, y, size, size, 90, -360);
        g2.setColor(theme.accent);
        g2.drawArc(x, y, size, size, 90, (int) (-360 * shown));
        g2.setColor(theme.text);
        g2.setFont(theme.headingFont);
        String pct = Math.round(shown * 100) + "%";
        int tw = g2.getFontMetrics().stringWidth(pct);
        g2.drawString(pct, getWidth() / 2 - tw / 2, getHeight() / 2);
        g2.setFont(theme.smallFont);
        g2.setColor(theme.muted);
        int cw = g2.getFontMetrics().stringWidth(caption);
        g2.drawString(caption, getWidth() / 2 - cw / 2, getHeight() / 2 + 18);
        g2.dispose();
    }
}

// ======================================================================
// RoundedPanel
// ======================================================================
/** Card with a soft drop shadow and rounded corners. */
class RoundedPanel extends JPanel {
    private Color fillColor;
    public RoundedPanel() {
        this(null);
    }

    public RoundedPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);
    }

    public void setFillColor(Color color) {
        fillColor = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Theme theme = Theme.get();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(theme.shadow);
        g2.fillRoundRect(3, 5, getWidth() - 7, getHeight() - 7, theme.radius, theme.radius);
        g2.setColor(fillColor == null ? theme.card : fillColor);
        g2.fillRoundRect(0, 0, getWidth() - 6, getHeight() - 8, theme.radius, theme.radius);
        g2.dispose();
        super.paintComponent(g);
    }
}

// ======================================================================
// Sidebar
// ======================================================================
/** Persistent white navigation rail inspired by the clean Study-Wise reference. */
class Sidebar extends JPanel {
    public static final String[] ITEMS = {
            "Dashboard", "Study Planner", "Upload Material", "AI Mentor", "Quiz Center",
            "Progress & Weak Areas", "Revision Notes", "Current Affairs / Daily Practice", "Settings",
            "Weekly Insights", "Goals & Readiness", "Subjects"
    };
    private static final IconPainter.Kind[] ICONS = {
            IconPainter.Kind.DASHBOARD, IconPainter.Kind.PLANNER, IconPainter.Kind.SUBJECTS,
            IconPainter.Kind.MENTOR, IconPainter.Kind.QUIZ, IconPainter.Kind.PROGRESS,
            IconPainter.Kind.INSIGHTS, IconPainter.Kind.QUIZ, IconPainter.Kind.SETTINGS,
            IconPainter.Kind.INSIGHTS, IconPainter.Kind.GOAL, IconPainter.Kind.SUBJECTS
    };
    private final Map<String, NavBtn> buttons = new LinkedHashMap<>();
    private final ThemeToggle toggle;
    private final JLabel brandName = new JLabel("Study-Wise");
    private final RoundedPanel streakPanel = new RoundedPanel(new BorderLayout(0, 5));
    private final JLabel streakTitle = new JLabel("Study streak");
    private final JLabel streakCount = new JLabel();
    private final JLabel streakNote = new JLabel("A little each day adds up.");
    private String active = "Dashboard";

    public Sidebar(AppContext context, Consumer<String> onNavigate, Runnable onThemeToggle) {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(20, 16, 16, 16));
        setPreferredSize(new Dimension(258, 100));

        JPanel brand = new JPanel(new BorderLayout(11, 0));
        brand.setOpaque(false);
        javax.swing.ImageIcon logo = AppBranding.loadIcon(36);
        if (logo != null) brand.add(new JLabel(logo), BorderLayout.WEST);
        brandName.setFont(Theme.get().headingFont.deriveFont(17f));
        brandName.setForeground(Theme.get().text);
        brand.add(brandName, BorderLayout.CENTER);
        brand.setBorder(BorderFactory.createEmptyBorder(3, 5, 24, 4));
        add(brand, BorderLayout.NORTH);

        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        for (int i = 0; i < ITEMS.length; i++) {
            String key = ITEMS[i];
            NavBtn button = new NavBtn(key, displayName(key), ICONS[i]);
            button.addActionListener(e -> onNavigate.accept(key));
            button.setAlignmentX(LEFT_ALIGNMENT);
            buttons.put(key, button);
            nav.add(button);
            nav.add(Box.createVerticalStrut(5));
        }
        JScrollPane navScroll = new JScrollPane(nav);
        navScroll.setBorder(BorderFactory.createEmptyBorder());
        navScroll.setOpaque(false);
        navScroll.getViewport().setOpaque(false);
        navScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        navScroll.getVerticalScrollBar().setUnitIncrement(18);
        navScroll.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        add(navScroll, BorderLayout.CENTER);

        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.add(streakCard(context));
        footer.add(Box.createVerticalStrut(10));
        toggle = new ThemeToggle(onThemeToggle);
        toggle.setAlignmentX(LEFT_ALIGNMENT);
        footer.add(toggle);
        add(footer, BorderLayout.SOUTH);
        refreshTheme();
    }

    private static String displayName(String key) { return key; }

    private JPanel streakCard(AppContext context) {
        Theme theme = Theme.get();
        int streak = currentStreak(context);
        streakPanel.setFillColor(theme.cardAlt);
        streakPanel.setBorder(BorderFactory.createEmptyBorder(13, 14, 13, 10));
        streakPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
        streakTitle.setFont(theme.bodyFont.deriveFont(java.awt.Font.BOLD));
        streakCount.setText(streak + (streak == 1 ? " day in a row" : " days in a row"));
        streakCount.setFont(theme.bodyFont.deriveFont(java.awt.Font.BOLD));
        streakNote.setFont(theme.smallFont);
        streakPanel.add(streakTitle, BorderLayout.NORTH);
        streakPanel.add(streakCount, BorderLayout.CENTER);
        streakPanel.add(streakNote, BorderLayout.SOUTH);
        return streakPanel;
    }

    private static int currentStreak(AppContext context) {
        java.util.Set<LocalDate> days = new java.util.HashSet<>();
        for (StudySession session : context.data.sessions) {
            for (int offset = 0; offset <= 1; offset++) {
                LocalDate day = session.date.plusDays(offset);
                if (session.minutesOn(day) > 0) days.add(day);
            }
        }
        LocalDate day = days.contains(LocalDate.now()) ? LocalDate.now() : LocalDate.now().minusDays(1);
        int count = 0;
        while (days.contains(day)) { count++; day = day.minusDays(1); }
        return count;
    }

    public void setActive(String name) {
        active = name;
        if ("Daily Quiz".equals(name)) active = "Current Affairs / Daily Practice";
        if ("Planner".equals(name)) active = "Study Planner";
        if ("Quiz".equals(name)) active = "Quiz Center";
        if ("Progress".equals(name)) active = "Progress & Weak Areas";
        if ("Mentor".equals(name)) active = "AI Mentor";
        for (Map.Entry<String, NavBtn> item : buttons.entrySet()) {
            item.getValue().selected = item.getKey().equals(active);
            item.getValue().repaint();
        }
    }

    public void refreshTheme() {
        Theme theme = Theme.get();
        brandName.setForeground(theme.text);
        brandName.setFont(theme.headingFont.deriveFont(17f));
        streakPanel.setFillColor(theme.cardAlt);
        streakTitle.setForeground(theme.accent);
        streakTitle.setFont(theme.bodyFont.deriveFont(java.awt.Font.BOLD));
        streakCount.setForeground(theme.text);
        streakCount.setFont(theme.bodyFont.deriveFont(java.awt.Font.BOLD));
        streakNote.setForeground(theme.muted);
        streakNote.setFont(theme.smallFont);
        for (NavBtn button : buttons.values()) button.setFont(theme.bodyFont);
        toggle.refresh();
        setActive(active);
        repaint();
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setColor(Theme.get().sidebar);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(new Color(222, 228, 233));
        g.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
        g.dispose();
        super.paintComponent(graphics);
    }

    private static final class NavBtn extends JButton {
        final IconPainter.Kind icon;
        boolean selected;
        boolean hover;
        NavBtn(String key, String label, IconPainter.Kind icon) {
            super(label);
            this.icon = icon;
            setName(key);
            setToolTipText("Open the " + label + " screen");
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setHorizontalAlignment(LEFT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            setPreferredSize(new Dimension(224, 38));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics graphics) {
            Theme theme = Theme.get();
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (selected || hover) {
                g.setColor(selected ? theme.accentSoft : theme.cardAlt);
                g.fillRoundRect(0, 0, getWidth() - 2, getHeight(), 12, 12);
            }
            Color ink = selected ? theme.accent : theme.text;
            IconPainter.paint(g, icon, 12, 11, 17, selected ? theme.accent : theme.muted);
            g.setFont(selected ? theme.bodyFont.deriveFont(java.awt.Font.BOLD) : theme.bodyFont);
            g.setColor(ink);
            g.drawString(getText(), 40, 25);
            g.dispose();
        }
    }

    private static final class ThemeToggle extends JButton {
        ThemeToggle(Runnable action) {
            setFocusPainted(false); setBorderPainted(false); setContentAreaFilled(false); setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Switch between the light and dark themes");
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            addActionListener(e -> action.run());
            refresh();
        }
        void refresh() { setText(Theme.get().dark ? "  Light appearance" : "  Dark appearance"); repaint(); }
        @Override protected void paintComponent(Graphics graphics) {
            Theme theme = Theme.get();
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(theme.card);
            g.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 1, 12, 12);
            IconPainter.paint(g, theme.dark ? IconPainter.Kind.SUN : IconPainter.Kind.MOON, 12, 10, 17, theme.accent);
            g.setFont(theme.smallFont);
            g.setColor(theme.text);
            g.drawString(getText().trim(), 40, 24);
            g.dispose();
        }
    }
}

// ======================================================================
// SlidePane
// ======================================================================
/** Fades and slides the next screen in using a Swing Timer. */
class SlidePane extends JPanel {
    private JPanel current = new JPanel(new BorderLayout());
    private BufferedImage outgoing;
    private float progress = 1f;
    private Timer timer;

    public SlidePane() {
        super(new BorderLayout());
        setOpaque(false);
        current.setOpaque(false);
        add(current, BorderLayout.CENTER);
    }

    public void showPanel(JPanel next) {
        if (getWidth() > 0 && getHeight() > 0 && current.getWidth() > 0) {
            outgoing = new BufferedImage(Math.max(1, getWidth()), Math.max(1, getHeight()), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = outgoing.createGraphics();
            current.paint(g2);
            g2.dispose();
        }
        removeAll();
        current = next;
        current.setOpaque(false);
        add(current, BorderLayout.CENTER);
        progress = 0f;
        if (timer != null) {
            timer.stop();
        }
        timer = new Timer(16, e -> {
            progress = Math.min(1f, progress + 0.08f);
            revalidate();
            repaint();
            if (progress >= 1f) {
                ((Timer) e.getSource()).stop();
                outgoing = null;
            }
        });
        timer.start();
        revalidate();
        repaint();
    }

    @Override
    protected void paintChildren(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth();
        int offset = (int) ((1f - progress) * 48);
        g2.translate(offset, 0);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, progress));
        super.paintChildren(g2);
        g2.dispose();
        if (outgoing != null && progress < 1f) {
            Graphics2D old = (Graphics2D) g.create();
            old.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f - progress));
            old.drawImage(outgoing, (int) (-progress * 40), 0, null);
            old.dispose();
        }
        // silence unused warning on w in some compilers
        if (w < 0) {
            return;
        }
    }
}

// ======================================================================
// SplashScreen
// ======================================================================
/** Brief Java2D splash animation shown while the local data profile is loaded. */
final class SplashScreen {
    private SplashScreen() { }
    public static SplashHandle open() {
        JWindow window=new JWindow();
        JPanel art=new JPanel(){long began=System.currentTimeMillis();protected void paintComponent(Graphics g){Graphics2D x=(Graphics2D)g.create();x.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);x.setPaint(new GradientPaint(0,0,new Color(44,25,88),getWidth(),getHeight(),new Color(16,22,51)));x.fillRect(0,0,getWidth(),getHeight());int cx=getWidth()/2,cy=getHeight()/2-14;double phase=(System.currentTimeMillis()-began)/220.0;for(int i=0;i<3;i++){int r=30+i*18+(int)(5*Math.sin(phase+i));x.setColor(new Color(180,130,255,150-i*30));x.setStroke(new BasicStroke(2.2f));x.drawOval(cx-r,cy-r,2*r,2*r);}x.setColor(new Color(255,213,127));x.fillOval(cx-13,cy-13,26,26);x.setFont(Theme.get().headingFont.deriveFont(Font.BOLD,22f));x.setColor(Color.WHITE);String title="AI STUDY PLANNER";x.drawString(title,cx-x.getFontMetrics().stringWidth(title)/2,cy+82);x.setFont(Theme.get().bodyFont.deriveFont(13f));x.setColor(new Color(203,203,231));String line="Preparing your learning space…";x.drawString(line,cx-x.getFontMetrics().stringWidth(line)/2,cy+110);x.dispose();}};
        art.setPreferredSize(new Dimension(450,290));window.setContentPane(art);window.pack();window.setLocationRelativeTo(null);window.setVisible(true);
        Timer animation=new Timer(35,e->art.repaint());animation.start();
        return new SplashHandle(window,animation);
    }

    public static final class SplashHandle {
        private final JWindow window;private final Timer animation;
        private SplashHandle(JWindow window,Timer animation){this.window=window;this.animation=animation;}
        public void close(){animation.stop();window.dispose();}
    }
}

// ======================================================================
// StudyQuotes
// ======================================================================
/** Short original study reminders, rotated by calendar day. */
final class StudyQuotes {
    private static final String[] QUOTES = {
            "📚 Small steps every day lead to big success! 🚀",
            "🌱 One focused page today is progress you can build on tomorrow. ✨",
            "🎯 Study with purpose; every good question makes you stronger. 💡",
            "🧠 Practice turns what you read into what you remember. 🔁",
            "🏆 Your future is shaped by the effort you make today. 📖",
            "🌟 Keep showing up. Consistency is a superpower. 💪",
            "✍️ Progress beats perfection—start with the next question. 🚀",
            "🔍 Curiosity makes difficult topics easier to explore. 📚",
            "⏳ A little focused time can make a big difference. 🌈",
            "🛤️ Every revision is another step toward your goal. 🎓",
            "💡 Mistakes are clues that show you what to learn next. 🧭",
            "🌻 Take a breath, reset, and begin one small task. ☀️",
            "📘 Your effort today is an investment in tomorrow's confidence. 💫",
            "🚴 Keep moving at your own pace; steady effort adds up. 📈",
            "🎓 Big goals are reached one lesson and one day at a time. 🌟",
            "🔑 Recall, review, repeat—the path to lasting learning. 🧠",
            "🪜 You don't need to finish everything now; just take the next step. 👣",
            "🌤️ Focus on what you can learn in this moment. 📖",
            "🔥 Protect your focus and let your progress speak for itself. 💪",
            "🧩 Every topic you understand brings the bigger picture together. 🎯",
            "🌍 Stay curious, stay patient, and keep asking why. ✨",
            "💬 Explain an idea in your own words and make it yours. 🧠",
            "🌈 Your pace is valid. Keep learning and keep going. 📚",
            "🎉 Celebrate today's effort, then come back ready for more. 🚀"
    };

    private StudyQuotes() { }

    /** Returns a different quote each calendar day, cycling through the full set. */
    public static String forToday() {
        int index = Math.floorMod(LocalDate.now().getDayOfYear() - 1, QUOTES.length);
        return QUOTES[index];
    }

    public static int count() { return QUOTES.length; }
}

// ======================================================================
// Theme
// ======================================================================
/**
 * All colors, fonts and radii live here so the look can be changed in one file.
 * Screens should never hard-code a Color or Font.
 */
class Theme {
    private static final Theme INSTANCE = new Theme();

    public boolean dark = false;
    public Color bg;
    public Color sidebar;
    public Color card;
    public Color cardAlt;
    public Color accent;
    public Color accentSoft;
    public Color text;
    public Color muted;
    public Color danger;
    public Color success;
    public Color headerTop;
    public Color headerBottom;
    public Color shadow;
    public Color retroPaper;
    public Color retroEdge;
    public Color retroHighlight;
    public Color retroShadow;
    public Font titleFont;
    public Font headingFont;
    public Font bodyFont;
    public Font smallFont;
    public int radius = 18;
    public int pad = 16;
    private int fontSizePercent = 100;

    private final List<Runnable> listeners = new ArrayList<>();

    private Theme() {
        pickFonts();
        applyLight();
    }

    public static Theme get() {
        return INSTANCE;
    }

    public void toggle() {
        setDark(!dark);
    }

    public void setDark(boolean value) {
        dark = value;
        if (dark) {
            applyDark();
        } else {
            applyLight();
        }
        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    public void onChange(Runnable listener) {
        listeners.add(listener);
    }

    public void setAccent(Color color) {
        accent = color;
        accentSoft = new Color(color.getRed(), color.getGreen(), color.getBlue(), dark ? 58 : 38);
        for (Runnable listener : listeners) listener.run();
    }

    public void setFontSizePercent(int percent) {
        fontSizePercent = Math.max(85, Math.min(130, percent));
        pickFonts();
        for (Runnable listener : listeners) listener.run();
    }

    private void applyDark() {
        bg = new Color(14, 19, 39);
        sidebar = new Color(19, 22, 48);
        card = new Color(30, 37, 70);
        cardAlt = new Color(39, 47, 83);
        accent = new Color(135, 117, 255);
        accentSoft = new Color(108, 95, 238, 66);
        text = new Color(240, 244, 255);
        muted = new Color(154, 162, 190);
        danger = new Color(255, 122, 140);
        success = new Color(92, 235, 197);
        headerTop = new Color(54, 76, 160);
        headerBottom = new Color(25, 31, 68);
        shadow = new Color(8, 4, 24, 115);
        retroPaper = new Color(48, 45, 67);
        retroEdge = new Color(225, 184, 95);
        retroHighlight = new Color(255, 235, 174);
        retroShadow = new Color(11, 12, 27);
    }

    private void applyLight() {
        bg = new Color(243, 242, 249);
        sidebar = new Color(255, 255, 255);
        card = new Color(255, 255, 255);
        cardAlt = new Color(241, 247, 246);
        accent = new Color(17, 119, 100);
        accentSoft = new Color(203, 243, 231);
        text = new Color(29, 37, 50);
        muted = new Color(105, 123, 143);
        danger = new Color(191, 74, 81);
        success = new Color(20, 137, 111);
        headerTop = new Color(243, 242, 249);
        headerBottom = new Color(243, 242, 249);
        shadow = new Color(32, 45, 66, 18);
        retroPaper = new Color(250, 246, 231);
        retroEdge = new Color(142, 104, 50);
        retroHighlight = new Color(255, 255, 242);
        retroShadow = new Color(103, 77, 42);
    }

    /** One clean, modern font family across navigation, cards, titles, and controls. */
    private void pickFonts() {
        String[] available = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        String family = findFont(available,
                new String[]{"Segoe UI", "Inter", "Poppins", "Arial"}, "SansSerif");
        titleFont = new Font(family, Font.BOLD, scaled(29));
        headingFont = new Font(family, Font.BOLD, scaled(18));
        bodyFont = new Font(family, Font.PLAIN, scaled(14));
        smallFont = new Font(family, Font.PLAIN, scaled(12));
    }

    private int scaled(int size) { return Math.max(10, Math.round(size * fontSizePercent / 100f)); }

    private static String findFont(String[] available, String[] preferred, String fallback) {
        for (String want : preferred) {
            for (String have : available) {
                if (have.equalsIgnoreCase(want)) return have;
            }
        }
        return fallback;
    }
}

// ======================================================================
// Ui
// ======================================================================
/** Tiny factory methods so screens stay short. */
final class Ui {
    private Ui() {
    }

    public static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    public static JPanel page(String title, String subtitle) {
        Theme theme = Theme.get();
        JPanel page = new JPanel(new BorderLayout(0, 16));
        page.setOpaque(false);
        page.setBorder(BorderFactory.createEmptyBorder(20, 26, 22, 26));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(label(title, theme.titleFont, theme.text), BorderLayout.NORTH);
        header.add(label(subtitle, theme.smallFont, theme.muted), BorderLayout.SOUTH);
        page.add(header, BorderLayout.NORTH);
        return page;
    }

    public static RoundedPanel card() {
        RoundedPanel card = new RoundedPanel(new BorderLayout(8, 8));
        card.setBorder(BorderFactory.createEmptyBorder(20, 22, 22, 22));
        return card;
    }

    public static JScrollPane scroll(JComponent inner) {
        JScrollPane scroll = new JScrollPane(inner);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        return scroll;
    }

    public static void styleField(JComponent field) {
        Theme theme = Theme.get();
        field.setFont(theme.bodyFont);
        field.setForeground(theme.text);
        field.setBackground(theme.card);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(222, 229, 234), 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
    }

    /** Applies the shared rounded style to standard Swing buttons in modal dialogs. */
    public static void retroButton(JButton button) {
        Theme theme = Theme.get();
        button.setFont(theme.bodyFont.deriveFont(Font.BOLD, 13f));
        button.setForeground(theme.text);
        button.setBackground(theme.card);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.accentSoft, 1, true),
                BorderFactory.createEmptyBorder(6, 11, 6, 11)));
    }
}

// ======================================================================
// WelcomeScreen
// ======================================================================
/** Full-work-area welcome screen shown after local data has loaded. */
final class WelcomeScreen {
    private static final int AUTO_CONTINUE_MS = 4500;

    private WelcomeScreen() { }

    public static void open(AppContext context, Runnable onStart) {
        JWindow window = new JWindow();
        window.setFocusableWindowState(true);
        WelcomePanel welcome = new WelcomePanel(context, window, onStart);
        window.setContentPane(welcome);
        Rectangle bounds = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        window.setBounds(bounds);
        window.setVisible(true);
        welcome.startAnimations();
        welcome.requestFocusInWindow();
    }

    private static final class WelcomePanel extends javax.swing.JPanel {
        private final JWindow window;
        private final Runnable onStart;
        private final BufferedImage banner;
        private final javax.swing.JPanel copy;
        private final Timer fade;
        private final Timer autoContinue;
        private float opacity;
        private boolean continued;

        WelcomePanel(AppContext context, JWindow window, Runnable onStart) {
            super(new BorderLayout());
            this.window = window;
            this.onStart = onStart;
            this.banner = AppBranding.loadWelcomeBanner();
            setOpaque(true);
            copy = buildCopy(context);
            copy.setOpaque(false);
            copy.setBorder(BorderFactory.createEmptyBorder(40, 36, 40, 24));
            add(copy, BorderLayout.WEST);

            fade = new Timer(35, e -> {
                opacity = Math.min(1f, opacity + .045f);
                repaint();
                if (opacity >= 1f) ((Timer) e.getSource()).stop();
            });
            autoContinue = new Timer(AUTO_CONTINUE_MS, e -> continueToApp());
            autoContinue.setRepeats(false);

            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "start-study");
            getActionMap().put("start-study", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) { continueToApp(); }
            });
        }

        private javax.swing.JPanel buildCopy(AppContext context) {
            Theme theme = Theme.get();
            javax.swing.JPanel column = new javax.swing.JPanel();
            column.setOpaque(false);
            column.setLayout(new javax.swing.BoxLayout(column, javax.swing.BoxLayout.Y_AXIS));
            column.setPreferredSize(new Dimension(610, 620));

            javax.swing.JPanel brand = new javax.swing.JPanel(new BorderLayout(12, 0)); brand.setOpaque(false);
            javax.swing.ImageIcon logo = AppBranding.loadIcon(42);
            if (logo != null) brand.add(new javax.swing.JLabel(logo), BorderLayout.WEST);
            javax.swing.JLabel title = new javax.swing.JLabel("STUDY-WISE");
            title.setFont(theme.headingFont.deriveFont(Font.BOLD, 18f)); title.setForeground(Color.WHITE);
            brand.add(title, BorderLayout.CENTER);
            brand.setMaximumSize(new Dimension(560, 54));
            column.add(brand);
            column.add(javax.swing.Box.createVerticalGlue());

            String name = context.data.profile.name == null ? "" : context.data.profile.name.trim();
            String greeting = name.isBlank() ? "Welcome to Study-Wise! 👋" : "Welcome back, " + escape(name) + "! 👋";
            javax.swing.JLabel welcome = new javax.swing.JLabel("<html><div style='text-align:center;width:520px'>" + greeting + "</div></html>", javax.swing.SwingConstants.CENTER);
            welcome.setFont(theme.headingFont.deriveFont(Font.BOLD, 29f)); welcome.setForeground(Color.WHITE);
            welcome.setAlignmentX(CENTER_ALIGNMENT);
            column.add(welcome);
            column.add(javax.swing.Box.createVerticalStrut(18));

            javax.swing.JLabel quote = new javax.swing.JLabel("<html><div style='text-align:center;width:520px'>" + escape(StudyQuotes.forToday()) + "</div></html>", javax.swing.SwingConstants.CENTER);
            quote.setFont(theme.titleFont.deriveFont(Font.BOLD, 36f)); quote.setForeground(Color.WHITE);
            quote.setAlignmentX(CENTER_ALIGNMENT);
            column.add(quote);
            column.add(javax.swing.Box.createVerticalStrut(18));
            javax.swing.JLabel subtitle = new javax.swing.JLabel("A clear plan. Focused practice. Steady progress.", javax.swing.SwingConstants.CENTER);
            subtitle.setFont(theme.bodyFont.deriveFont(17f)); subtitle.setForeground(new Color(232, 240, 250));
            subtitle.setAlignmentX(CENTER_ALIGNMENT); column.add(subtitle);
            column.add(javax.swing.Box.createVerticalStrut(28));

            AccentButton start = AccentButton.primary("Start Studying  →");
            start.setFont(theme.headingFont.deriveFont(Font.BOLD, 18f));
            start.setPreferredSize(new Dimension(250, 58)); start.setMaximumSize(new Dimension(250, 58));
            start.setAlignmentX(CENTER_ALIGNMENT); start.setToolTipText("Open your personalized study dashboard");
            start.addActionListener(e -> continueToApp()); column.add(start);
            column.add(javax.swing.Box.createVerticalStrut(14));
            javax.swing.JLabel auto = new javax.swing.JLabel("Starting your dashboard in a few seconds · Esc to continue now", javax.swing.SwingConstants.CENTER);
            auto.setFont(theme.smallFont); auto.setForeground(new Color(221, 229, 242)); auto.setAlignmentX(CENTER_ALIGNMENT);
            column.add(auto);
            column.add(javax.swing.Box.createVerticalGlue());
            return column;
        }

        void startAnimations() { fade.start(); autoContinue.start(); }

        private void continueToApp() {
            if (continued) return;
            continued = true;
            fade.stop(); autoContinue.stop();
            window.dispose();
            onStart.run();
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color first = Theme.get().dark ? new Color(57, 48, 132) : new Color(12, 112, 94);
            Color second = Theme.get().dark ? new Color(12, 18, 44) : new Color(46, 65, 125);
            g.setPaint(new java.awt.GradientPaint(0, 0, first, getWidth(), getHeight(), second));
            g.fillRect(0, 0, getWidth(), getHeight());

            if (banner != null) paintBanner(g);
            // Soft contrast wash keeps the quote readable over the art and gradient.
            g.setPaint(new java.awt.GradientPaint(0, 0, new Color(4, 14, 35, 75), getWidth() * .62f, 0, new Color(4, 14, 35, 0)));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.dispose();
        }

        private void paintBanner(Graphics2D g) {
            int x = (int) (getWidth() * .55), y = (int) (getHeight() * .14);
            int boxWidth = (int) (getWidth() * .38), boxHeight = (int) (getHeight() * .72);
            int arc = 32;
            g.setColor(new Color(255, 255, 255, 34));
            g.fillRoundRect(x - 10, y - 10, boxWidth + 20, boxHeight + 20, arc + 8, arc + 8);
            double scale = Math.min(boxWidth / (double) banner.getWidth(), boxHeight / (double) banner.getHeight());
            int drawWidth = Math.max(1, (int) Math.round(banner.getWidth() * scale));
            int drawHeight = Math.max(1, (int) Math.round(banner.getHeight() * scale));
            int drawX = x + (boxWidth - drawWidth) / 2, drawY = y + (boxHeight - drawHeight) / 2;
            java.awt.Shape previousClip = g.getClip();
            g.clip(new RoundRectangle2D.Float(x, y, boxWidth, boxHeight, arc, arc));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            g.drawImage(banner, drawX, drawY, drawWidth, drawHeight, null);
            g.setComposite(AlphaComposite.SrcOver);
            g.setClip(previousClip);
            g.setColor(new Color(255, 255, 255, 120)); g.drawRoundRect(x, y, boxWidth, boxHeight, arc, arc);
        }

        @Override protected void paintChildren(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            super.paintChildren(g); g.dispose();
        }

        private static String escape(String text) {
            return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }
}

// ======================================================================
// DailyPracticeScreen
// ======================================================================
/** Daily MCQ entry page. Questions are sampled from the local bank. */
final class DailyPracticeScreen {
    private DailyPracticeScreen() { }

    public static JPanel build(AppContext ctx) {
        Theme theme = Theme.get();
        JPanel page = Ui.page("Current Affairs / Daily Practice", "A short daily MCQ session drawn from your available question bank.");
        RoundedPanel card = Ui.card();
        card.setLayout(new BorderLayout(0, 14));
        card.add(Ui.label("10 questions · one focused session", theme.headingFont, theme.text), BorderLayout.NORTH);
        card.add(Ui.label("Your saved question bank currently contains " + ctx.quiz.bankSize()
                + " question(s). Add current-affairs questions through Quiz Center or upload your study materials.",
                theme.bodyFont, theme.muted), BorderLayout.CENTER);
        AccentButton start = AccentButton.primary("Start today’s 10-question practice");
        start.addActionListener(e -> {
            if (java.time.LocalDate.now().equals(ctx.data.profile.dailyPracticeDate)) {
                javax.swing.JOptionPane.showMessageDialog(page,
                        "Today’s 10-question practice is complete. Come back tomorrow for another session.",
                        "Daily practice complete", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (ctx.quiz.bankSize() < 10) {
                javax.swing.JOptionPane.showMessageDialog(page,
                        "Add at least 10 questions to your quiz bank before starting the daily session.",
                        "More questions needed", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            ctx.applet.showScreen("Daily Quiz");
        });
        AccentButton bank = AccentButton.ghost("Open Quiz Center");
        bank.addActionListener(e -> ctx.applet.showScreen("Quiz Center"));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(start);
        actions.add(bank);
        card.add(actions, BorderLayout.SOUTH);
        page.add(card, BorderLayout.CENTER);
        return page;
    }
}

// ======================================================================
// DashboardScreen
// ======================================================================
/** Compact, single-screen daily home for the learner's next actions. */
final class DashboardScreen {
    private DashboardScreen() { }

    public static JPanel build(AppContext ctx) {
        Theme theme = Theme.get();
        JPanel page = new JPanel(new BorderLayout()); page.setOpaque(false);
        page.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        List<StudyTask> today = new ArrayList<>();
        for (StudyTask task : ctx.data.tasks) if (LocalDate.now().equals(task.day)) today.add(task);
        today.sort(Comparator.comparing((StudyTask task) -> task.done)
                .thenComparing(Comparator.comparingInt((StudyTask task) -> task.priority).reversed()));

        JPanel content = new JPanel(); content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(greeting(ctx)); content.add(Box.createVerticalStrut(9));

        MetricCard taskMetric = metric("Today's tasks", "0 / 0", "☑", new Color(20, 137, 111));
        MetricCard progressMetric = metric("Today's progress", "0%", "◉", new Color(81, 111, 218));
        MetricCard quizMetric = metric("Quiz average", "—", "✎", new Color(206, 132, 46));
        MetricCard streakMetric = metric("Study streak", streak(ctx) + " days", "🔥", new Color(201, 91, 87));
        JPanel metrics = new JPanel(new GridLayout(1, 4, 9, 0)); metrics.setOpaque(false);
        metrics.add(taskMetric.card); metrics.add(progressMetric.card); metrics.add(quizMetric.card); metrics.add(streakMetric.card);
        metrics.setMaximumSize(new Dimension(Integer.MAX_VALUE, 88));
        content.add(metrics); content.add(Box.createVerticalStrut(8));

        HoverCard gamification = new HoverCard(new BorderLayout());
        gamification.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        gamification.add(new GamificationStrip(ctx.data), BorderLayout.CENTER);
        gamification.setPreferredSize(new Dimension(700, 72));
        gamification.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        content.add(gamification); content.add(Box.createVerticalStrut(9));

        ProgressRing dailyRing = new ProgressRing(); dailyRing.setPreferredSize(new Dimension(142, 142));
        JPanel plan = planCard(ctx, today, dailyRing, taskMetric, progressMetric, quizMetric, streakMetric);
        plan.setAlignmentX(Component.LEFT_ALIGNMENT);
        plan.setPreferredSize(new Dimension(720, 245)); plan.setMaximumSize(new Dimension(Integer.MAX_VALUE, 245));
        content.add(plan); content.add(Box.createVerticalStrut(9));

        JPanel bottom = new JPanel(new GridLayout(1, 3, 9, 0)); bottom.setOpaque(false);
        bottom.add(tipCard(theme)); bottom.add(continueCard(ctx)); bottom.add(quickActions(ctx));
        bottom.setPreferredSize(new Dimension(720, 165)); bottom.setMaximumSize(new Dimension(Integer.MAX_VALUE, 165));
        content.add(bottom);
        page.add(Ui.scroll(content), BorderLayout.CENTER);

        refreshMetrics(ctx, today, dailyRing, taskMetric, progressMetric, quizMetric, streakMetric);
        return page;
    }

    private static JPanel greeting(AppContext ctx) {
        Theme theme = Theme.get();
        JPanel header = new JPanel(new BorderLayout(16, 0)); header.setOpaque(false);
        JPanel copy = new JPanel(); copy.setOpaque(false); copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(Ui.label(LocalDate.now().format(TimeFmt.LONG), theme.smallFont.deriveFont(Font.BOLD), theme.accent));
        String name = ctx.data.profile.name == null || ctx.data.profile.name.isBlank() ? "there" : ctx.data.profile.name;
        copy.add(Ui.label("Good " + timeOfDay() + ", " + name, theme.titleFont.deriveFont(Font.BOLD, 25f), theme.text));
        copy.add(Ui.label("A focused plan, one step at a time.", theme.smallFont, theme.muted));
        header.add(copy, BorderLayout.CENTER);
        AccentButton create = AccentButton.primary("＋  Create study plan");
        create.setPreferredSize(new Dimension(190, 42));
        create.setToolTipText("Create a timetable for your subjects and exam goals");
        create.addActionListener(e -> ctx.applet.showScreen(ctx.data.subjects.isEmpty() ? "Subjects" : "Planner"));
        header.add(create, BorderLayout.EAST); header.setPreferredSize(new Dimension(700, 68));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68)); header.setAlignmentX(Component.LEFT_ALIGNMENT);
        return header;
    }

    private static JPanel planCard(AppContext ctx, List<StudyTask> today, ProgressRing ring,
                                  MetricCard taskMetric, MetricCard progressMetric,
                                  MetricCard quizMetric, MetricCard streakMetric) {
        Theme theme = Theme.get();
        HoverCard card = new HoverCard(new BorderLayout(12, 0));
        JPanel left = new JPanel(new BorderLayout(0, 8)); left.setOpaque(false);
        JPanel heading = new JPanel(new BorderLayout()); heading.setOpaque(false);
        heading.add(Ui.label("Today's plan", theme.headingFont, theme.text), BorderLayout.WEST);
        JLabel count = Ui.label("", theme.smallFont, theme.muted); heading.add(count, BorderLayout.EAST);
        left.add(heading, BorderLayout.NORTH);
        JPanel tasks = new JPanel(); tasks.setOpaque(false); tasks.setLayout(new BoxLayout(tasks, BoxLayout.Y_AXIS));
        if (today.isEmpty()) {
            HoverCard empty = new HoverCard(new BorderLayout(0, 6));
            empty.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
            empty.add(Ui.label("🌱  Nothing scheduled yet", theme.bodyFont.deriveFont(Font.BOLD), theme.text), BorderLayout.NORTH);
            empty.add(Ui.label("Create a plan or take a short quiz to choose your next topic.", theme.smallFont, theme.muted), BorderLayout.CENTER);
            AccentButton first = AccentButton.ghost("Plan my study time");
            first.addActionListener(e -> ctx.applet.showScreen(ctx.data.subjects.isEmpty() ? "Subjects" : "Planner"));
            empty.add(first, BorderLayout.EAST); tasks.add(empty);
        } else {
            int visible = Math.min(3, today.size());
            for (int i = 0; i < visible; i++) {
                tasks.add(taskRow(ctx, today.get(i), () -> refreshMetrics(ctx, today, ring, taskMetric, progressMetric, quizMetric, streakMetric)));
            }
            if (today.size() > visible) {
                JLabel more = Ui.label("＋ " + (today.size() - visible) + " more in your planner", theme.smallFont, theme.muted);
                more.setBorder(BorderFactory.createEmptyBorder(3, 36, 0, 0)); tasks.add(more);
            }
        }
        left.add(Ui.scroll(tasks), BorderLayout.CENTER);
        AccentButton view = AccentButton.ghost("Open planner  →"); view.setPreferredSize(new Dimension(145, 31));
        view.addActionListener(e -> ctx.applet.showScreen("Planner"));
        left.add(view, BorderLayout.SOUTH);
        card.add(left, BorderLayout.CENTER);

        JPanel goal = new JPanel(new BorderLayout(0, 2)); goal.setOpaque(false);
        goal.add(Ui.label("Daily goal", theme.bodyFont.deriveFont(Font.BOLD), theme.text), BorderLayout.NORTH);
        goal.add(ring, BorderLayout.CENTER);
        JLabel goalLabel = Ui.label("0 / " + ctx.data.profile.dailyGoalMinutes + " min", theme.smallFont, theme.muted);
        goalLabel.setHorizontalAlignment(JLabel.CENTER); goal.add(goalLabel, BorderLayout.SOUTH);
        card.add(goal, BorderLayout.EAST);
        card.putClientProperty("goalLabel", goalLabel);
        return card;
    }

    private static JPanel taskRow(AppContext ctx, StudyTask task, Runnable refresh) {
        Theme theme = Theme.get();
        JPanel row = new JPanel(new BorderLayout(8, 0)); row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        JCheckBox complete = new JCheckBox(); complete.setOpaque(false); complete.setSelected(task.done);
        complete.setToolTipText(task.done ? "Mark this topic incomplete" : "Mark this topic complete");
        complete.addActionListener(e -> { task.done = complete.isSelected(); ctx.saveOrWarn(); refresh.run(); });
        row.add(complete, BorderLayout.WEST);
        JPanel labels = new JPanel(); labels.setOpaque(false); labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        String topic = task.topic == null || task.topic.isBlank() ? task.label() : task.topic;
        JLabel title = Ui.label(escape(topic), theme.bodyFont.deriveFont(Font.BOLD, 13f), task.done ? theme.muted : theme.text);
        labels.add(title);
        labels.add(Ui.label(escape(task.subject) + "  ·  " + task.minutes + " min  ·  " + shortKind(task.kind), theme.smallFont, theme.muted));
        row.add(labels, BorderLayout.CENTER); row.add(priorityChip(task.priority), BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45)); row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private static JPanel tipCard(Theme theme) {
        HoverCard card = new HoverCard(new BorderLayout(8, 5));
        JPanel title = new JPanel(new BorderLayout(6, 0)); title.setOpaque(false);
        JLabel icon = iconBadge("✦", theme.accent); title.add(icon, BorderLayout.WEST);
        title.add(Ui.label("AI tip of the day", theme.bodyFont.deriveFont(Font.BOLD), theme.text), BorderLayout.CENTER);
        card.add(title, BorderLayout.NORTH);
        String[] tips = {
                "Try a 5-minute recall before opening your notes. It helps ideas stick.",
                "After studying, explain one key idea in your own words.",
                "Break a large topic into a small question you can answer today.",
                "A quick review today can make tomorrow's revision easier."
        };
        final int[] todayIndex = {Math.floorMod(LocalDate.now().getDayOfYear(), tips.length)};
        JLabel message = Ui.label("<html><body style='width:210px'>" + tips[todayIndex[0]] + "</body></html>", theme.smallFont, theme.muted);
        card.add(message, BorderLayout.CENTER);
        AccentButton next = AccentButton.ghost("Another tip  →"); next.setFont(theme.smallFont);
        next.addActionListener(e -> { todayIndex[0] = (todayIndex[0] + 1) % tips.length; message.setText("<html><body style='width:210px'>" + tips[todayIndex[0]] + "</body></html>"); });
        card.add(next, BorderLayout.SOUTH);
        return card;
    }

    private static JPanel continueCard(AppContext ctx) {
        Theme theme = Theme.get(); HoverCard card = new HoverCard(new BorderLayout(8, 7));
        JPanel title = new JPanel(new BorderLayout(6, 0)); title.setOpaque(false);
        title.add(iconBadge("↻", new Color(80, 111, 215)), BorderLayout.WEST);
        title.add(Ui.label("Continue where you left off", theme.bodyFont.deriveFont(Font.BOLD), theme.text), BorderLayout.CENTER);
        card.add(title, BorderLayout.NORTH);
        String detail = lastStudyDetail(ctx);
        card.add(Ui.label("<html><body style='width:205px'>" + escape(detail) + "</body></html>", theme.smallFont, theme.muted), BorderLayout.CENTER);
        AccentButton resume = AccentButton.ghost("Continue studying  →"); resume.setFont(theme.smallFont.deriveFont(Font.BOLD));
        resume.addActionListener(e -> ctx.applet.showScreen("Planner")); card.add(resume, BorderLayout.SOUTH);
        return card;
    }

    private static JPanel quickActions(AppContext ctx) {
        Theme theme = Theme.get(); HoverCard card = new HoverCard(new BorderLayout(0, 8));
        card.add(Ui.label("Quick actions", theme.bodyFont.deriveFont(Font.BOLD), theme.text), BorderLayout.NORTH);
        JPanel actions = new JPanel(new GridLayout(3, 1, 0, 6)); actions.setOpaque(false);
        AccentButton quiz = AccentButton.primary("✎  Start Quiz");
        AccentButton material = AccentButton.ghost("▤  Upload Material");
        AccentButton mentor = AccentButton.ghost("✦  Ask AI");
        quiz.addActionListener(e -> ctx.applet.showScreen("Quiz Center"));
        material.addActionListener(e -> ctx.applet.showScreen("Upload Material"));
        mentor.addActionListener(e -> MentorChatDialog.show(ctx));
        actions.add(quiz); actions.add(material); actions.add(mentor);
        card.add(actions, BorderLayout.CENTER); return card;
    }

    private static MetricCard metric(String label, String value, String icon, Color color) {
        Theme theme = Theme.get(); HoverCard card = new HoverCard(new BorderLayout(6, 5));
        card.setBorder(BorderFactory.createEmptyBorder(10, 12, 12, 12));
        JPanel top = new JPanel(new BorderLayout(6, 0)); top.setOpaque(false);
        top.add(iconBadge(icon, color), BorderLayout.WEST);
        top.add(Ui.label(label, theme.smallFont, theme.muted), BorderLayout.CENTER);
        card.add(top, BorderLayout.NORTH);
        JLabel number = Ui.label(value, theme.headingFont.deriveFont(Font.BOLD, 20f), theme.text);
        card.add(number, BorderLayout.CENTER);
        return new MetricCard(card, number);
    }

    private static JLabel iconBadge(String icon, Color color) {
        JLabel badge = new JLabel(icon, JLabel.CENTER); badge.setOpaque(true); badge.setForeground(color);
        badge.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 35));
        badge.setFont(Theme.get().bodyFont.deriveFont(Font.BOLD, 15f)); badge.setPreferredSize(new Dimension(30, 30));
        return badge;
    }

    private static void refreshMetrics(AppContext ctx, List<StudyTask> today, ProgressRing ring,
                                       MetricCard task, MetricCard progress, MetricCard quiz, MetricCard streak) {
        int total = today.size(), done = (int) today.stream().filter(item -> item.done).count();
        task.value.setText(done + " / " + total);
        int minutes = dailyMinutes(ctx, today);
        float goalPercent = Math.min(1f, minutes / (float) Math.max(1, ctx.data.profile.dailyGoalMinutes));
        int progressPercent = total > 0 ? Math.round(100f * done / total) : Math.round(goalPercent * 100);
        progress.value.setText(progressPercent + "%");
        ring.setProgress(goalPercent, "today");
        Component parent = ring.getParent();
        if (parent instanceof JPanel) {
            for (Component child : ((JPanel) parent).getComponents()) if (child instanceof JLabel && child != null
                    && ((JLabel) child).getText() != null && ((JLabel) child).getText().contains(" / ")) {
                ((JLabel) child).setText(minutes + " / " + ctx.data.profile.dailyGoalMinutes + " min");
            }
        }
        int correct = 0;
        for (QuizAttempt attempt : ctx.data.quizAttempts) if (attempt.correct) correct++;
        quiz.value.setText(ctx.data.quizAttempts.isEmpty() ? "—" : Math.round(100f * correct / ctx.data.quizAttempts.size()) + "%");
        streak.value.setText(streak(ctx) + " days");
    }

    private static int dailyMinutes(AppContext ctx, List<StudyTask> today) {
        int tracked = ctx.data.minutesStudiedOn(LocalDate.now());
        if (tracked > 0) return tracked;
        return today.stream().filter(task -> task.done).mapToInt(task -> Math.max(0, task.minutes)).sum();
    }

    static int streak(AppContext ctx) {
        java.util.HashSet<LocalDate> days = new java.util.HashSet<>();
        for (StudySession session : ctx.data.sessions) {
            for (int offset = 0; offset <= 1; offset++) {
                LocalDate day = session.date.plusDays(offset);
                if (session.minutesOn(day) > 0) days.add(day);
            }
        }
        LocalDate day = days.contains(LocalDate.now()) ? LocalDate.now() : LocalDate.now().minusDays(1);
        int count = 0;
        while (days.contains(day)) { count++; day = day.minusDays(1); }
        return count;
    }

    private static String lastStudyDetail(AppContext ctx) {
        StudySession latest = null;
        for (StudySession session : ctx.data.sessions) {
            if (latest == null || session.startedAt != null && (latest.startedAt == null || session.startedAt.isAfter(latest.startedAt))) latest = session;
        }
        if (latest != null) {
            String topic = latest.topic == null || latest.topic.isBlank() ? latest.subject : latest.subject + " · " + latest.topic;
            return "Last studied " + topic + ". Pick up your next planned block when you're ready.";
        }
        StudyTask next = ctx.data.tasks.stream().filter(item -> !item.done)
                .min(Comparator.comparing((StudyTask item) -> item.day).thenComparing(Comparator.comparingInt((StudyTask item) -> item.priority).reversed())).orElse(null);
        return next == null ? "No recent session yet. Choose a topic and start with one small step." : "Next up: " + next.subject + " · " + next.topic + " (" + next.minutes + " min).";
    }

    private static String timeOfDay() {
        int hour = LocalTime.now().getHour(); return hour < 12 ? "morning" : hour < 17 ? "afternoon" : "evening";
    }
    private static String shortKind(String kind) {
        if (StudyTask.REVISE.equals(kind)) return "Review";
        if (StudyTask.QUIZ.equals(kind)) return "Practice";
        return "Study";
    }
    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static JLabel priorityChip(int priority) {
        Theme theme = Theme.get(); String label = priority >= 4 ? "High" : priority <= 2 ? "Low" : "Medium";
        Color color = priority >= 4 ? theme.danger : priority <= 2 ? theme.muted : new Color(205, 139, 46);
        JLabel chip = Ui.label(label, theme.smallFont, color); chip.setHorizontalAlignment(JLabel.CENTER); chip.setOpaque(true);
        chip.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 34));
        chip.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10)); return chip;
    }

    private static final class MetricCard {
        final JPanel card; final JLabel value;
        MetricCard(JPanel card, JLabel value) { this.card = card; this.value = value; }
    }
}

// ======================================================================
// FlashcardReviewDialog
// ======================================================================
/** Animated local flashcards with a small spaced-repetition schedule. */
final class FlashcardReviewDialog {
    private FlashcardReviewDialog() { }
    static void show(AppContext ctx) {
        List<Flashcard> due=new ArrayList<>();
        for(Flashcard c:ctx.data.flashcards)if(c.nextReview==null||!c.nextReview.isAfter(LocalDate.now()))due.add(c);
        if(due.isEmpty()){
            LocalDate next=ctx.data.flashcards.stream().map(c->c.nextReview).filter(java.util.Objects::nonNull).min(LocalDate::compareTo).orElse(null);
            JOptionPane.showMessageDialog(ctx.applet,ctx.data.flashcards.isEmpty()?"No flashcards yet. They are created automatically from uploaded notes and wrong answers.":"You're caught up. Next flashcard review: "+next);
            return;
        }
        new Review(ctx,due).open();
    }
    private static final class Review {
        final AppContext ctx;final List<Flashcard> cards;int index;JDialog dialog;FlipCard card;JLabel count;
        Review(AppContext c,List<Flashcard> list){ctx=c;cards=list;}
        void open(){dialog=new JDialog(SwingUtilities.getWindowAncestor(ctx.applet),"Flashcard Review",Dialog.ModalityType.APPLICATION_MODAL);dialog.setSize(640,490);dialog.setLocationRelativeTo(ctx.applet);dialog.setLayout(new BorderLayout(12,12));dialog.getContentPane().setBackground(Theme.get().bg);count=new JLabel();count.setForeground(Theme.get().muted);count.setBorder(BorderFactory.createEmptyBorder(14,18,0,18));dialog.add(count,BorderLayout.NORTH);card=new FlipCard();dialog.add(card,BorderLayout.CENTER);JPanel controls=new JPanel(new FlowLayout());controls.setOpaque(false);JButton flip=new JButton("Flip card"),again=new JButton("Again · tomorrow"),got=new JButton("Got it");for(JButton b:new JButton[]{flip,again,got})Ui.retroButton(b);flip.setToolTipText("Reveal the answer with a flip animation");again.setToolTipText("Mark difficult and review tomorrow");got.setToolTipText("Mark recalled and schedule a longer interval");flip.addActionListener(e->card.flip());again.addActionListener(e->rate(false));got.addActionListener(e->rate(true));controls.add(flip);controls.add(again);controls.add(got);dialog.add(controls,BorderLayout.SOUTH);display();dialog.setVisible(true);}
        void display(){if(index>=cards.size()){ctx.saveQuietly();dialog.dispose();JOptionPane.showMessageDialog(ctx.applet,"Flashcard session complete. Your review dates have been updated.");return;}Flashcard c=cards.get(index);count.setText("Card "+(index+1)+" of "+cards.size()+"  •  "+c.subject+" / "+c.topic);card.setContent(c);}
        void rate(boolean recalled){cards.get(index).review(recalled);index++;ctx.saveQuietly();display();}
    }
    private static final class FlipCard extends JPanel {
        Flashcard item;boolean back;float scale=1f;boolean animating;
        FlipCard(){setOpaque(false);setToolTipText("Click Flip card to reveal the answer");}
        void setContent(Flashcard c){item=c;back=false;repaint();}
        void flip(){if(animating)return;animating=true;final boolean old=back;final int[] step={0};Timer t=new Timer(18,e->{step[0]++;scale=step[0]<8?1f-step[0]/8f:(step[0]-8)/8f;if(step[0]==8)back=!old;if(step[0]>=16){scale=1;animating=false;((Timer)e.getSource()).stop();}repaint();});t.start();}
        @Override protected void paintComponent(Graphics g){super.paintComponent(g);if(item==null)return;Graphics2D x=(Graphics2D)g.create();x.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);int w=getWidth(),h=getHeight(),cw=(int)((w-35)*Math.max(.08,scale)),left=(w-cw)/2;x.setPaint(new GradientPaint(left,0,back?new Color(70,63,133):new Color(68,54,118),left+cw,h,new Color(40,86,119)));x.fillRoundRect(left,10,cw,h-20,30,30);x.setColor(new Color(255,255,255,45));x.drawRoundRect(left,10,cw,h-20,30,30);String title=back?"ANSWER":"THINK OF THE ANSWER";x.setFont(Theme.get().smallFont.deriveFont(Font.BOLD));x.setColor(new Color(205,186,255));x.drawString(title,left+24,42);String text=back?item.back+"\n\n"+item.explanation:item.front;drawWrapped(x,text,left+24,68,cw-48,Theme.get().bodyFont.deriveFont(Font.BOLD,17f),Color.WHITE);x.dispose();}
        void drawWrapped(Graphics2D g,String text,int x,int y,int width,Font font,Color color){g.setFont(font);g.setColor(color);FontMetrics fm=g.getFontMetrics();int lineY=y;for(String paragraph:text.split("\\n")){StringBuilder line=new StringBuilder();for(String word:paragraph.split(" ")){String candidate=line.length()==0?word:line+" "+word;if(fm.stringWidth(candidate)>width&&!line.isEmpty()){g.drawString(line.toString(),x,lineY);lineY+=fm.getHeight()+4;line=new StringBuilder(word);}else line=new StringBuilder(candidate);}if(!line.isEmpty()){g.drawString(line.toString(),x,lineY);lineY+=fm.getHeight()+8;}}}
    }
}

// ======================================================================
// GoalsScreen
// ======================================================================
/** Local mastery-based goal and study-time scenario estimator. */
final class GoalsScreen {
    private GoalsScreen() { }
    public static JPanel build(AppContext ctx) {
        Theme theme=Theme.get();JPanel page=Ui.page("Goal & What-if Simulator","Set your exam target and explore a study-time scenario using your current topic mastery.");
        JPanel form=new JPanel(new GridLayout(0,2,10,10));form.setOpaque(false);
        JTextField date=new JTextField((ctx.data.profile.targetExamDate==null?LocalDate.now().plusWeeks(4):ctx.data.profile.targetExamDate).toString());
        JSpinner target=new JSpinner(new SpinnerNumberModel(ctx.data.profile.targetScore,1,100,1));
        form.add(Ui.label("Target exam date (YYYY-MM-DD)",theme.bodyFont,theme.text));form.add(date);
        form.add(Ui.label("Target score (%)",theme.bodyFont,theme.text));form.add(target);
        JSlider extra=new JSlider(0,180,0);extra.setMajorTickSpacing(60);extra.setMinorTickSpacing(15);extra.setPaintTicks(true);extra.setPaintLabels(true);
        RoundedPanel meter=Ui.card();meter.setLayout(new BorderLayout(12,8));ProgressRing ring=new ProgressRing();ring.setPreferredSize(new Dimension(170,170));
        JLabel details=Ui.label("",theme.bodyFont,theme.text);meter.add(ring,BorderLayout.WEST);meter.add(details,BorderLayout.CENTER);
        JLabel sliderLabel=Ui.label("Extra study time per day: 0 minutes",theme.bodyFont,theme.text);
        AccentButton save=AccentButton.primary("Save goal");
        Runnable refresh=()->{LocalDate exam=parseDate(date.getText());int score=estimatedScore(ctx);int days=(int)Math.max(0,ChronoUnit.DAYS.between(LocalDate.now(),exam));int scenario=(int)Math.min(100,score+Math.min(15,extra.getValue()*Math.max(1,days)/360));int ready=(int)Math.min(100,100.0*scenario/Math.max(1,(Integer)target.getValue()));ring.setProgress(ready/100f,"readiness");details.setText("<html><b>Current mastery-based score estimate: "+score+"%</b><br>With +"+extra.getValue()+" min/day: "+scenario+"% estimated score<br>Target: "+target.getValue()+"%<br>Readiness in this scenario: "+ready+"%<br>Days to exam: "+days+"<br><br>This is a planning estimate based on your current topic mastery, not a calibrated exam prediction.</html>");sliderLabel.setText("Extra study time per day: "+extra.getValue()+" minutes");};
        extra.addChangeListener(e->refresh.run());target.addChangeListener(e->refresh.run());date.addActionListener(e->refresh.run());
        save.addActionListener(e->{try{ctx.data.profile.targetExamDate=LocalDate.parse(date.getText().trim());ctx.data.profile.targetScore=(Integer)target.getValue();ctx.saveOrWarn();refresh.run();JOptionPane.showMessageDialog(page,"Exam goal saved.");}catch(Exception ex){JOptionPane.showMessageDialog(page,"Enter a valid exam date as YYYY-MM-DD.");}});
        JPanel controls=new JPanel(new BorderLayout(0,8));controls.setOpaque(false);controls.add(sliderLabel,BorderLayout.NORTH);controls.add(extra,BorderLayout.CENTER);controls.add(save,BorderLayout.SOUTH);
        JPanel body=new JPanel(new BorderLayout(0,16));body.setOpaque(false);body.add(form,BorderLayout.NORTH);body.add(meter,BorderLayout.CENTER);body.add(controls,BorderLayout.SOUTH);page.add(body,BorderLayout.CENTER);refresh.run();return page;
    }
    private static LocalDate parseDate(String text){try{return LocalDate.parse(text.trim());}catch(Exception e){return LocalDate.now().plusWeeks(4);}}
    private static int estimatedScore(AppContext ctx){if(!ctx.data.knowledge.isEmpty())return (int)Math.round(ctx.data.knowledge.values().stream().mapToInt(k->k.mastery).average().orElse(0));int total=0,n=0;for(Subject s:ctx.data.subjects)for(Topic t:s.topics){total+=t.strength*20;n++;}return n==0?0:(int)Math.round(total/(double)n);}
}

// ======================================================================
// InsightsScreen
// ======================================================================
/** Weekly learning dashboard built from saved planner, quiz, and study records. */
final class InsightsScreen {
    private InsightsScreen() { }

    public static JPanel build(AppContext ctx) {
        Theme theme = Theme.get();
        JPanel page = Ui.page("Weekly Insights", "A visual check-in on your study rhythm, progress, and next best moves.");
        JPanel dashboard = new JPanel(); dashboard.setOpaque(false); dashboard.setLayout(new BoxLayout(dashboard, BoxLayout.Y_AXIS));
        int[] weekOffset = {0}; String[] reportText = {""};
        AccentButton export = AccentButton.ghost("Export weekly report (.txt)");
        export.setToolTipText("Save this week's visual summary as a text report");
        export.addActionListener(e -> export(page, reportText[0]));

        JPanel period = new JPanel(new BorderLayout(8, 0)); period.setOpaque(false);
        JPanel weekCards = new JPanel(); weekCards.setOpaque(false); weekCards.setLayout(new BoxLayout(weekCards, BoxLayout.Y_AXIS));
        AccentButton previous = AccentButton.ghost("‹  Previous week"), next = AccentButton.ghost("Next week  ›");
        period.add(previous, BorderLayout.WEST); period.add(weekCards, BorderLayout.CENTER); period.add(next, BorderLayout.EAST);
        Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> {
            LocalDate end = LocalDate.now().plusWeeks(weekOffset[0]);
            LocalDate start = end.minusDays(6);
            WeekData data = collect(ctx, start, end);
            next.setEnabled(weekOffset[0] < 0);
            weekCards.removeAll();
            weekCards.add(weekHeader(start, end, data));
            weekCards.add(Box.createVerticalStrut(14));
            weekCards.add(summaryCards(ctx, data, weekOffset[0]));
            weekCards.add(Box.createVerticalStrut(14));
            weekCards.add(chartAndGoals(ctx, data));
            weekCards.add(Box.createVerticalStrut(14));
            weekCards.add(aiInsight(ctx, data));
            weekCards.add(Box.createVerticalStrut(14));
            weekCards.add(activityMap(ctx));
            weekCards.add(Box.createVerticalStrut(8));
            weekCards.add(export);
            reportText[0] = report(ctx, data);
            weekCards.revalidate(); weekCards.repaint();
        };
        previous.addActionListener(e -> { weekOffset[0]--; refresh[0].run(); });
        next.addActionListener(e -> { if (weekOffset[0] < 0) { weekOffset[0]++; refresh[0].run(); } });
        refresh[0].run();
        page.add(Ui.scroll(period), BorderLayout.CENTER);
        return page;
    }

    private static JPanel weekHeader(LocalDate start, LocalDate end, WeekData data) {
        Theme theme = Theme.get();
        HoverCard card = new HoverCard(new BorderLayout(8, 6));
        JPanel top = new JPanel(new BorderLayout()); top.setOpaque(false);
        top.add(Ui.label("YOUR WEEK AT A GLANCE", theme.smallFont.deriveFont(Font.BOLD), theme.accent), BorderLayout.NORTH);
        top.add(Ui.label(start.format(DateTimeFormatter.ofPattern("MMM d")) + " – " + end.format(DateTimeFormatter.ofPattern("MMM d, yyyy")), theme.headingFont, theme.text), BorderLayout.SOUTH);
        card.add(top, BorderLayout.CENTER);
        card.add(Ui.label(data.studyMinutes + " focused minutes logged", theme.bodyFont, theme.muted), BorderLayout.EAST);
        card.setPreferredSize(new Dimension(760, 86));
        return card;
    }

    private static JPanel summaryCards(AppContext ctx, WeekData data, int offset) {
        JPanel cards = new JPanel(new GridLayout(1, 4, 10, 0)); cards.setOpaque(false);
        int previousStudy = minutesBetween(ctx, data.start.minusDays(7), data.end.minusDays(7));
        int previousQuiz = quizAverage(ctx, data.start.minusDays(7), data.end.minusDays(7));
        cards.add(metric("Study hours", String.format("%.1f h", data.studyMinutes / 60.0), "◷", trend(data.studyMinutes, previousStudy), Theme.get().accent));
        cards.add(metric("Topics completed", data.topicsCompleted + "", "✓", data.topicsCompleted > 0 ? "▲" : "→", Theme.get().success));
        cards.add(metric("Quiz average", data.quizAverage < 0 ? "—" : data.quizAverage + "%", "✎", trend(data.quizAverage, previousQuiz), new Color(91, 112, 215)));
        cards.add(metric("Study streak", DashboardScreen.streak(ctx) + " days", "✦", "●", new Color(210, 143, 49)));
        return cards;
    }

    private static JPanel metric(String title, String value, String icon, String trend, Color color) {
        Theme theme = Theme.get(); HoverCard card = new HoverCard(new BorderLayout(5, 8));
        JPanel row = new JPanel(new BorderLayout(5, 0)); row.setOpaque(false);
        JLabel badge = new JLabel(icon, JLabel.CENTER); badge.setOpaque(true); badge.setForeground(color); badge.setBackground(soft(color)); badge.setPreferredSize(new Dimension(32, 32));
        row.add(badge, BorderLayout.WEST); row.add(Ui.label(title, theme.smallFont, theme.muted), BorderLayout.CENTER);
        row.add(Ui.label(trend, theme.bodyFont.deriveFont(Font.BOLD), color), BorderLayout.EAST);
        card.add(row, BorderLayout.NORTH); card.add(Ui.label(value, theme.headingFont.deriveFont(Font.BOLD, 22f), theme.text), BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(150, 95)); return card;
    }

    private static JPanel chartAndGoals(AppContext ctx, WeekData data) {
        Theme theme = Theme.get(); JPanel row = new JPanel(new GridLayout(1, 2, 12, 0)); row.setOpaque(false);
        HoverCard chart = new HoverCard(new BorderLayout(0, 8));
        chart.add(Ui.label("Daily study time", theme.headingFont, theme.text), BorderLayout.NORTH);
        chart.add(new DailyStudyBars(data.start, data.dailyMinutes), BorderLayout.CENTER);
        HoverCard goals = new HoverCard(new BorderLayout(0, 8));
        goals.add(Ui.label("Goal progress", theme.headingFont, theme.text), BorderLayout.NORTH);
        JPanel rings = new JPanel(new GridLayout(1, 2, 8, 0)); rings.setOpaque(false);
        ProgressRing studyRing = new ProgressRing(); studyRing.setPreferredSize(new Dimension(135, 145));
        studyRing.setProgress(data.studyGoal == 0 ? 0 : Math.min(1f, data.studyMinutes / (float) data.studyGoal), "weekly goal");
        ProgressRing quizRing = new ProgressRing(); quizRing.setPreferredSize(new Dimension(135, 145));
        int target = Math.max(1, ctx.data.profile.targetScore);
        quizRing.setProgress(data.quizAverage < 0 ? 0 : Math.min(1f, data.quizAverage / (float) target), "score target");
        rings.add(studyRing); rings.add(quizRing); goals.add(rings, BorderLayout.CENTER);
        row.add(chart); row.add(goals); return row;
    }

    private static JPanel aiInsight(AppContext ctx, WeekData data) {
        Theme theme = Theme.get(); HoverCard card = new HoverCard(new BorderLayout(12, 0));
        JLabel icon = new JLabel("✦", JLabel.CENTER); icon.setOpaque(true); icon.setForeground(theme.accent); icon.setBackground(theme.accentSoft); icon.setPreferredSize(new Dimension(42, 42));
        card.add(icon, BorderLayout.WEST);
        JPanel copy = new JPanel(); copy.setOpaque(false); copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(Ui.label("Your coach's weekly insight", theme.bodyFont.deriveFont(Font.BOLD), theme.accent));
        KnowledgeTopic weak = ctx.data.knowledge.values().stream().min(Comparator.comparingInt(k -> k.mastery)).orElse(null);
        String message;
        if (weak != null) message = "A short recall session for " + weak.subject + " · " + weak.topic + " is your best next move.";
        else if (data.studyMinutes == 0) message = "Start with one manageable study block this week; your planner will begin tracking your rhythm.";
        else if (data.studyMinutes < data.studyGoal) message = "You're building a rhythm. Add one focused session next week to move closer to your weekly goal.";
        else message = "You met your study-time target. Keep the pace steady and protect time for rest.";
        copy.add(Ui.label("<html><body style='width:440px'>" + escape(message) + "</body></html>", theme.bodyFont, theme.text));
        String bestTime = data.bestStudyHour < 0 ? "not enough start-time data" : String.format("%02d:00–%02d:00", data.bestStudyHour, (data.bestStudyHour + 1) % 24);
        copy.add(Ui.label("Most improved: " + improvedTopic(ctx.data.quizAttempts, data.start, data.end) + "  ·  Best study window: " + bestTime, theme.smallFont, theme.muted));
        copy.add(Ui.label("Suggestion for next week: choose one topic, practice recall, and review it again after a short interval.", theme.smallFont, theme.muted));
        card.add(copy, BorderLayout.CENTER);
        AccentButton action = AccentButton.primary(weak == null ? "Open planner" : "Review topic");
        action.addActionListener(e -> ctx.applet.showScreen(weak == null ? "Planner" : "Revision Notes"));
        card.add(action, BorderLayout.EAST); return card;
    }

    private static JPanel activityMap(AppContext ctx) {
        Theme theme = Theme.get(); HoverCard card = new HoverCard(new BorderLayout(0, 8));
        JComboBox<String> filter = new JComboBox<>(); filter.addItem("All topics");
        for (KnowledgeTopic topic : ctx.data.knowledge.values()) filter.addItem(topic.subject + " / " + topic.topic);
        Heatmap heat = new Heatmap(ctx); heat.setPreferredSize(new Dimension(730, 124));
        filter.addActionListener(e -> heat.setTopic((String) filter.getSelectedItem()));
        JPanel title = new JPanel(new BorderLayout()); title.setOpaque(false);
        title.add(Ui.label("STUDY ACTIVITY · LAST 12 WEEKS", theme.smallFont.deriveFont(Font.BOLD), theme.accent), BorderLayout.WEST);
        title.add(filter, BorderLayout.EAST); card.add(title, BorderLayout.NORTH); card.add(heat, BorderLayout.CENTER);
        return card;
    }

    private static WeekData collect(AppContext ctx, LocalDate start, LocalDate end) {
        WeekData data = new WeekData(start, end); Set<String> completed = new HashSet<>(); int[] hours = new int[24];
        for (int i = 0; i < 7; i++) {
            LocalDate day = start.plusDays(i); int minutes = ctx.data.minutesStudiedOn(day);
            data.studyMinutes += minutes; data.dailyMinutes.add(minutes);
        }
        for (StudySession session : ctx.data.sessions) if (session.actualMinutes > 0 && !session.date.isBefore(start) && !session.date.isAfter(end)) {
            completed.add(session.subject + "\n" + (session.topic == null ? "" : session.topic));
            data.plannedMinutes += session.plannedMinutes;
            if (session.startedAt != null) hours[session.startedAt.getHour()] += session.actualMinutes;
        }
        for (KnowledgeTopic topic : ctx.data.knowledge.values()) if (topic.lastStudied != null && !topic.lastStudied.isBefore(start) && !topic.lastStudied.isAfter(end))
            completed.add(topic.subject + "\n" + topic.topic);
        data.topicsCompleted = completed.size(); data.quizAverage = quizAverage(ctx, start, end);
        for (int hour = 0; hour < hours.length; hour++) if (data.bestStudyHour < 0 || hours[hour] > hours[data.bestStudyHour]) data.bestStudyHour = hour;
        if (data.studyMinutes == 0 || hours[data.bestStudyHour] == 0) data.bestStudyHour = -1;
        data.studyGoal = ctx.data.profile.dailyGoalMinutes * 7;
        return data;
    }

    private static int minutesBetween(AppContext ctx, LocalDate start, LocalDate end) {
        int value = 0; for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) value += ctx.data.minutesStudiedOn(day); return value;
    }

    private static int quizAverage(AppContext ctx, LocalDate start, LocalDate end) {
        int correct = 0, count = 0;
        for (QuizAttempt attempt : ctx.data.quizAttempts) if (attempt.when != null && !attempt.when.toLocalDate().isBefore(start) && !attempt.when.toLocalDate().isAfter(end)) {
            count++; if (attempt.correct) correct++;
        }
        return count == 0 ? -1 : Math.round(100f * correct / count);
    }

    private static String improvedTopic(List<QuizAttempt> attempts, LocalDate start, LocalDate end) {
        Map<String, int[]> scores = new HashMap<>();
        for (QuizAttempt attempt : attempts) {
            if (attempt.when == null) continue;
            LocalDate date = attempt.when.toLocalDate();
            int period = !date.isBefore(start) && !date.isAfter(end) ? 0
                    : !date.isBefore(start.minusDays(7)) && date.isBefore(start) ? 1 : -1;
            if (period < 0) continue;
            int[] bucket = scores.computeIfAbsent(attempt.subject + " / " + attempt.topic, key -> new int[4]);
            bucket[period * 2]++; if (attempt.correct) bucket[period * 2 + 1]++;
        }
        String best = "not enough quiz data across two weeks"; double gain = 0;
        for (Map.Entry<String, int[]> entry : scores.entrySet()) {
            int[] count = entry.getValue(); if (count[0] == 0 || count[2] == 0) continue;
            double change = count[1] / (double) count[0] - count[3] / (double) count[2];
            if (change > gain) { gain = change; best = entry.getKey() + " · +" + Math.round(change * 100) + " points"; }
        }
        return best;
    }

    private static String trend(int value, int prior) {
        if (value < 0 || prior < 0) return "→";
        return value == prior ? "→" : value > prior ? "▲" : "▼";
    }

    private static String report(AppContext ctx, WeekData data) {
        KnowledgeTopic weak = ctx.data.knowledge.values().stream().min(Comparator.comparingInt(k -> k.mastery)).orElse(null);
        StringBuilder out = new StringBuilder("WEEKLY LEARNING REPORT\n").append(data.start).append(" to ").append(data.end).append("\n\n");
        out.append("Focused study: ").append(data.studyMinutes).append(" minutes\nTopics completed: ").append(data.topicsCompleted)
                .append("\nQuiz average: ").append(data.quizAverage < 0 ? "no quiz data" : data.quizAverage + "%")
                .append("\nStudy streak: ").append(DashboardScreen.streak(ctx)).append(" days")
                .append("\nUncompleted planned time: ").append(Math.max(0, data.plannedMinutes - data.studyMinutes)).append(" minutes")
                .append("\nBest study window: ").append(data.bestStudyHour < 0 ? "not enough start-time data" : String.format("%02d:00–%02d:00", data.bestStudyHour, (data.bestStudyHour + 1) % 24))
                .append("\nMost improved topic: ").append(improvedTopic(ctx.data.quizAttempts, data.start, data.end)).append('\n');
        KnowledgeTopic weakest = ctx.data.knowledge.values().stream().min(Comparator.comparingInt(k -> k.mastery)).orElse(null);
        out.append("Lowest current mastery: ").append(weakest == null ? "not enough quiz data" : weakest.subject + " / " + weakest.topic + " · " + weakest.mastery + "%")
                .append("\n\n");
        out.append("Recommended actions\n"); for (String action : actions(ctx, weak)) out.append("• ").append(action).append('\n');
        return out.toString();
    }

    private static List<String> actions(AppContext ctx, KnowledgeTopic weak) {
        List<String> result = new ArrayList<>();
        List<KnowledgeTopic> due = ctx.knowledge.dueTopics(ctx.data, LocalDate.now());
        if (!due.isEmpty()) result.add("Review " + due.get(0).subject + " · " + due.get(0).topic + ", which is due now.");
        else if (weak != null) result.add("Schedule a short recall session for " + weak.subject + " · " + weak.topic + ".");
        else result.add("Take a quiz to start tracking topic mastery and review dates.");
        long overdue = ctx.data.tasks.stream().filter(task -> !task.done && task.due.isBefore(LocalDate.now())).count();
        result.add(overdue > 0 ? "Re-plan " + overdue + " overdue task(s) instead of carrying them forward unchanged." : "Keep next week's plan within your available study time.");
        result.add("Use spaced recall and short breaks to keep next week's learning sustainable.");
        return result;
    }

    private static void export(java.awt.Component parent, String text) {
        JFileChooser chooser = new JFileChooser(); chooser.setSelectedFile(new File("weekly-learning-report.txt"));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        try { Files.writeString(chooser.getSelectedFile().toPath(), text, StandardCharsets.UTF_8); JOptionPane.showMessageDialog(parent, "Weekly report exported."); }
        catch (IOException ex) { JOptionPane.showMessageDialog(parent, "Could not export report: " + ex.getMessage()); }
    }

    private static Color soft(Color color) { return new Color(color.getRed(), color.getGreen(), color.getBlue(), 34); }
    private static String escape(String text) { return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }

    private static final class WeekData {
        final LocalDate start, end; int studyMinutes, topicsCompleted, quizAverage = -1, studyGoal, plannedMinutes, bestStudyHour = -1;
        final List<Integer> dailyMinutes = new ArrayList<>();
        WeekData(LocalDate start, LocalDate end) { this.start = start; this.end = end; }
    }

    /** Week-specific chart keeps raw minutes so 100+ minute sessions remain accurate. */
    private static final class DailyStudyBars extends JPanel {
        private final LocalDate start;
        private final List<Integer> minutes;
        private float reveal;
        private final Timer animation;
        DailyStudyBars(LocalDate start, List<Integer> values) {
            this.start = start; this.minutes = new ArrayList<>(values); setOpaque(false); setPreferredSize(new Dimension(420, 205)); setToolTipText(" ");
            animation = new Timer(16, e -> { reveal = Math.min(1f, reveal + .035f); repaint(); if (reveal >= 1f) ((Timer)e.getSource()).stop(); });
            animation.start();
        }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics); Theme theme = Theme.get(); Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int left=12, top=14, bottom=getHeight()-30, width=Math.max(1,getWidth()-24), max=1;
            for(int value:minutes)max=Math.max(max,value);
            int slot=width/Math.max(1,minutes.size()), barWidth=Math.max(12,slot-13);
            for(int i=0;i<minutes.size();i++){
                int x=left+i*slot+(slot-barWidth)/2, height=(int)((bottom-top)*(minutes.get(i)/(double)max)*reveal);
                g.setColor(theme.cardAlt);g.fillRoundRect(x,top,barWidth,bottom-top,10,10);
                g.setColor(theme.accent);g.fillRoundRect(x,bottom-height,barWidth,height,10,10);
                g.setColor(theme.muted);g.setFont(theme.smallFont);g.drawString(start.plusDays(i).getDayOfWeek().toString().substring(0,1),x+barWidth/2-3,getHeight()-8);
            }
            g.dispose();
        }
        @Override public String getToolTipText(MouseEvent e) {
            int left=12,width=Math.max(1,getWidth()-24),slot=width/Math.max(1,minutes.size());
            int index=(e.getX()-left)/Math.max(1,slot);
            if(index<0||index>=minutes.size())return null;
            return start.plusDays(index)+" · "+minutes.get(index)+" study minutes";
        }
    }

    private static final class Heatmap extends JPanel {
        final AppContext ctx; String topic = "All topics";
        Heatmap(AppContext ctx) { this.ctx = ctx; setOpaque(false); setToolTipText(" "); }
        void setTopic(String value) { topic = value == null ? "All topics" : value; repaint(); }
        int minutes(LocalDate day) {
            if ("All topics".equals(topic)) return ctx.data.minutesStudiedOn(day);
            int sum = 0; for (StudySession session : ctx.data.sessions)
                if (topic.equals(session.subject + " / " + (session.topic == null ? "" : session.topic))) sum += session.minutesOn(day);
            return sum;
        }
        @Override public String getToolTipText(java.awt.event.MouseEvent e) {
            int index = cell(e.getX(), e.getY());
            if (index < 0 || index >= 84) return null;
            LocalDate date = LocalDate.now().minusDays(83).plusDays(index);
            return date + " · " + minutes(date) + " study minutes · " + topic;
        }
        private int cell(int x, int y) {
            int size = Math.max(8, Math.min(14, (getWidth() - 65) / 84)), gap = 4;
            int col = (x - 52) / (size + gap), row = (y - 20) / (size + gap);
            if (x < 52 || y < 18 || col < 0 || col >= 12 || row < 0 || row >= 7) return -1;
            return col * 7 + row;
        }
        @Override protected void paintComponent(java.awt.Graphics graphics) {
            super.paintComponent(graphics); Theme theme = Theme.get(); java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.max(8, Math.min(14, (getWidth() - 65) / 84)), gap = 4;
            LocalDate first = LocalDate.now().minusDays(83);
            for (int i = 0; i < 84; i++) {
                LocalDate day = first.plusDays(i); int value = minutes(day); int column = i / 7, row = i % 7;
                Color color = theme.cardAlt;
                if (value > 0) color = value < 20 ? mix(theme.cardAlt, theme.accent, .35f) : value < 45 ? mix(theme.cardAlt, theme.accent, .58f) : mix(theme.cardAlt, theme.accent, .82f);
                g.setColor(color); g.fillRoundRect(52 + column * (size + gap), 20 + row * (size + gap), size, size, 4, 4);
            }
            g.setColor(theme.muted); g.setFont(theme.smallFont); g.drawString("12 weeks ago", 52, 14); g.drawString("Today", 52 + 11 * (size + gap), 14); g.dispose();
        }
        private static Color mix(Color a, Color b, float ratio) {
            return new Color((int)(a.getRed()*(1-ratio)+b.getRed()*ratio),(int)(a.getGreen()*(1-ratio)+b.getGreen()*ratio),(int)(a.getBlue()*(1-ratio)+b.getBlue()*ratio));
        }
    }
}

// ======================================================================
// LevelUpDialog
// ======================================================================
/** Small timed Java2D celebration shown when XP crosses a level boundary. */
final class LevelUpDialog {
    private LevelUpDialog() { }
    static void show(Component owner, GamificationService.Award award) {
        Window parent = SwingUtilities.getWindowAncestor(owner);
        JDialog dialog = new JDialog(parent, "Level up!", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel art = new JPanel() {
            long start = System.currentTimeMillis();
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D x=(Graphics2D)g.create();x.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                long elapsed=System.currentTimeMillis()-start;double pulse=1+0.08*Math.sin(elapsed/160.0);
                x.setPaint(new GradientPaint(0,0,new Color(93,50,171),getWidth(),getHeight(),new Color(27,34,74)));
                x.fillRoundRect(0,0,getWidth(),getHeight(),26,26);
                int cx=getWidth()/2,cy=getHeight()/2-8,r=(int)(48*pulse);
                x.setColor(new Color(255,204,92,75));x.fillOval(cx-r-14,cy-r-14,2*r+28,2*r+28);
                x.setColor(new Color(255,208,108));x.fillOval(cx-r,cy-r,2*r,2*r);
                Path2D star=new Path2D.Double();for(int i=0;i<10;i++){double a=-Math.PI/2+i*Math.PI/5;double rr=(i%2==0)?r*.78:r*.36;double px=cx+rr*Math.cos(a),py=cy+rr*Math.sin(a);if(i==0)star.moveTo(px,py);else star.lineTo(px,py);}star.closePath();x.setColor(new Color(111,66,186));x.fill(star);
                x.setFont(Theme.get().titleFont.deriveFont(Font.BOLD,24f));x.setColor(Color.WHITE);String text="LEVEL "+award.newLevel;x.drawString(text,cx-x.getFontMetrics().stringWidth(text)/2,cy+r+38);x.dispose();
            }
        };
        art.setPreferredSize(new Dimension(400,245));
        JLabel sub=new JLabel("You earned "+award.gained+" XP. Keep your learning rhythm going!",JLabel.CENTER);sub.setBorder(BorderFactory.createEmptyBorder(8,12,12,12));
        JPanel content=new JPanel(new BorderLayout());content.setBackground(Theme.get().bg);content.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));content.add(art);content.add(sub,BorderLayout.SOUTH);
        dialog.setContentPane(content);dialog.pack();dialog.setLocationRelativeTo(owner);
        Timer animation=new Timer(40,e->art.repaint());animation.start();
        Timer close=new Timer(2600,e->{animation.stop();dialog.dispose();});close.setRepeats(false);close.start();
        dialog.setVisible(true);
    }
}

// ======================================================================
// MentorChatDialog
// ======================================================================
/** Small non-modal assistant window. Messages are saved with the learner's local data. */
final class MentorChatDialog {
    private static JDialog openDialog;

    private MentorChatDialog() { }

    public static void show(AppContext ctx) {
        if (openDialog != null && openDialog.isDisplayable()) {
            openDialog.setVisible(true);
            openDialog.toFront();
            return;
        }
        Window owner = SwingUtilities.getWindowAncestor(ctx.applet);
        JDialog dialog = new JDialog(owner, "Study-Wise Mentor");
        openDialog = dialog;
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(true);
        dialog.setMinimumSize(new Dimension(340, 420));
        dialog.setSize(390, 540);
        dialog.getContentPane().setBackground(Theme.get().bg);
        dialog.setLayout(new BorderLayout(0, 8));

        Theme theme = Theme.get();
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setBackground(theme.card);
        header.setBorder(BorderFactory.createEmptyBorder(13, 15, 12, 15));
        JLabel title = Ui.label("Your study mentor", theme.headingFont, theme.text);
        JLabel subtitle = Ui.label("Private coaching based on your saved progress", theme.smallFont, theme.muted);
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        dialog.add(header, BorderLayout.NORTH);

        JPanel messages = new JPanel();
        messages.setOpaque(false);
        messages.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        messages.setLayout(new BoxLayout(messages, BoxLayout.Y_AXIS));
        JScrollPane scroll = Ui.scroll(messages);
        dialog.add(scroll, BorderLayout.CENTER);
        JLabel typing = Ui.label("", theme.smallFont, theme.muted);
        typing.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));

        JTextField input = new JTextField();
        Ui.styleField(input);
        input.setToolTipText("Ask about your study plan, progress, or notes");
        AccentButton send = AccentButton.primary("Send");
        send.setPreferredSize(new Dimension(76, 38));
        JPanel compose = new JPanel(new BorderLayout(8, 7));
        compose.setOpaque(false);
        compose.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        JPanel fieldRow = new JPanel(new BorderLayout(7, 0));
        fieldRow.setOpaque(false);
        fieldRow.add(input, BorderLayout.CENTER);
        fieldRow.add(send, BorderLayout.EAST);
        compose.add(typing, BorderLayout.NORTH);
        compose.add(fieldRow, BorderLayout.CENTER);
        dialog.add(compose, BorderLayout.SOUTH);

        for (ChatMessage message : ctx.data.mentorChat) addBubble(messages, message);
        if (ctx.data.mentorChat.isEmpty()) {
            addBubble(messages, new ChatMessage("assistant",
                    "Hi! Ask what to study next, how your progress is going, or tell me a topic you want to work on."));
        }

        Runnable submit = () -> {
            String text = input.getText().trim();
            if (text.isEmpty() || !send.isEnabled()) return;
            ChatMessage userMessage = new ChatMessage("user", text);
            remember(ctx, userMessage);
            addBubble(messages, userMessage);
            input.setText("");
            send.setEnabled(false);
            typing.setText("Mentor is thinking…");
            messages.revalidate();
            SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum()));

            new SwingWorker<String, Void>() {
                @Override protected String doInBackground() {
                    return ctx.mentor.replyWithMaterial(ctx, text, ctx.data.activeMaterialText);
                }
                @Override protected void done() {
                    String response;
                    try { response = get(); }
                    catch (Exception ex) { response = "I couldn't prepare a reply just now. Please try again."; }
                    ChatMessage reply = new ChatMessage("assistant", response);
                    remember(ctx, reply);
                    addBubble(messages, reply);
                    typing.setText(" ");
                    send.setEnabled(true);
                    messages.revalidate();
                    SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum()));
                    input.requestFocusInWindow();
                }
            }.execute();
        };
        send.addActionListener(e -> submit.run());
        input.addActionListener(e -> submit.run());

        JPanel suggestions = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        suggestions.setOpaque(false);
        for (String prompt : new String[]{"What next?", "Explain my progress"}) {
            AccentButton quick = AccentButton.ghost(prompt);
            quick.setPreferredSize(new Dimension(136, 30));
            quick.addActionListener(e -> { input.setText(prompt); submit.run(); });
            suggestions.add(quick);
        }
        compose.add(suggestions, BorderLayout.SOUTH);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        if (owner != null) {
            Point location = owner.getLocationOnScreen();
            dialog.setLocation(location.x + owner.getWidth() - dialog.getWidth() - 24,
                    location.y + owner.getHeight() - dialog.getHeight() - 42);
        }
    }

    private static void remember(AppContext ctx, ChatMessage message) {
        ctx.data.mentorChat.add(message);
        while (ctx.data.mentorChat.size() > 100) ctx.data.mentorChat.remove(0);
        ctx.saveQuietly();
    }

    private static void addBubble(JPanel messages, ChatMessage message) {
        Theme theme = Theme.get();
        boolean user = "user".equalsIgnoreCase(message.role);
        RoundedPanel bubble = new RoundedPanel(new BorderLayout());
        bubble.setFillColor(user ? theme.accentSoft : theme.card);
        bubble.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        JLabel body = Ui.label("<html><div style='width:260px'>" + escape(message.text) + "</div></html>", theme.bodyFont, theme.text);
        bubble.add(body, BorderLayout.CENTER);
        bubble.setAlignmentX(user ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
        bubble.setMaximumSize(new Dimension(310, Integer.MAX_VALUE));
        messages.add(bubble);
        messages.add(Box.createVerticalStrut(7));
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
    }
}

// ======================================================================
// MentorScreen
// ======================================================================
/** Local chat-style learning coach. */
final class MentorScreen {
    private MentorScreen() { }

    public static JPanel build(AppContext ctx) {
        Theme theme = Theme.get();
        JPanel page = Ui.page("AI Mentor", "A private, rule-based coach that reads your saved study data locally.");
        JPanel messages = new JPanel();
        messages.setOpaque(false);
        messages.setLayout(new BoxLayout(messages, BoxLayout.Y_AXIS));
        addMessage(messages, "Mentor", "Hi " + ctx.data.profile.name + "! Ask what to study, how a topic is going, or how to make your week lighter.", false);
        JScrollPane scroll = Ui.scroll(messages);
        JPanel compose = new JPanel(new BorderLayout(8, 0));
        compose.setOpaque(false);
        JTextField input = new JTextField();
        Ui.styleField(input);
        input.setToolTipText("Ask about your plan, progress, or a topic");
        AccentButton send = AccentButton.primary("Send");
        compose.add(input, BorderLayout.CENTER);
        compose.add(send, BorderLayout.EAST);
        JPanel suggestions = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        suggestions.setOpaque(false);
        String[] prompts = {"What should I study now?", "Explain my progress", "I'm weak in Polity", "Make my plan lighter this week"};
        for (String prompt : prompts) {
            AccentButton quick = AccentButton.ghost(prompt);
            quick.setToolTipText("Ask the mentor: " + prompt);
            quick.addActionListener(e -> { input.setText(prompt); send.doClick(); });
            suggestions.add(quick);
        }
        send.setToolTipText("Send your question to the local rule-based mentor");
        Runnable submit = () -> {
            String question = input.getText().trim();
            if (question.isEmpty()) return;
            addMessage(messages, "You", question, true);
            String response = ctx.mentor.reply(ctx, question);
            addMessage(messages, "Mentor", response, false);
            input.setText("");
            messages.revalidate();
            SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum()));
        };
        send.addActionListener(e -> submit.run());
        input.addActionListener(e -> submit.run());
        JPanel lower = new JPanel(new BorderLayout(0, 9));
        lower.setOpaque(false);
        lower.add(suggestions, BorderLayout.NORTH);
        lower.add(compose, BorderLayout.SOUTH);
        page.add(scroll, BorderLayout.CENTER);
        page.add(lower, BorderLayout.SOUTH);
        return page;
    }

    private static void addMessage(JPanel list, String speaker, String text, boolean user) {
        Theme theme = Theme.get();
        RoundedPanel bubble = new RoundedPanel(new BorderLayout(0, 5));
        bubble.setBorder(BorderFactory.createEmptyBorder(11, 14, 11, 14));
        JLabel name = Ui.label(speaker, theme.smallFont, user ? theme.accent : theme.success);
        JLabel body = Ui.label("<html><div style='width:560px'>" + escape(text) + "</div></html>", theme.bodyFont, theme.text);
        bubble.add(name, BorderLayout.NORTH);
        bubble.add(body, BorderLayout.CENTER);
        bubble.setAlignmentX(user ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
        bubble.setMaximumSize(new Dimension(720, bubble.getPreferredSize().height + 28));
        list.add(bubble);
        list.add(Box.createVerticalStrut(10));
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
    }
}

// ======================================================================
// MockTestDialog
// ======================================================================
/** Full-length local timed test with navigation palette and +4/-1 scoring. */
final class MockTestDialog {
    private MockTestDialog() { }
    static void show(AppContext ctx) {
        List<QuizQuestion> questions=ctx.quiz.startQuiz(null,20);
        if(questions.isEmpty()){JOptionPane.showMessageDialog(ctx.applet,"Add questions or upload notes before starting a mock test.");return;}
        new Test(ctx,questions).run();
    }
    private static final class Test {
        final AppContext ctx;final List<QuizQuestion> qs;final int[] answers;final boolean[] marked;final boolean[] skipped;
        int index;int remaining;long started;JDialog dialog;JLabel clock=new JLabel();JLabel question=new JLabel();JRadioButton[] choices=new JRadioButton[4];ButtonGroup group=new ButtonGroup();JButton[] palette;
        Test(AppContext c,List<QuizQuestion> q){ctx=c;qs=q;answers=new int[q.size()];java.util.Arrays.fill(answers,-1);marked=new boolean[q.size()];skipped=new boolean[q.size()];remaining=q.size()*Math.max(10,c.data.profile.quizSeconds);}
        void run(){
            dialog=new JDialog(SwingUtilities.getWindowAncestor(ctx.applet),"Mock Test · "+qs.size()+" questions · +4 / −1",Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setSize(950,640);dialog.setLocationRelativeTo(ctx.applet);dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            JPanel root=new JPanel(new BorderLayout(14,12));root.setBorder(BorderFactory.createEmptyBorder(16,18,16,18));root.setBackground(Theme.get().bg);
            JPanel header=new JPanel(new BorderLayout());header.setOpaque(false);header.add(new JLabel("Timed Mock Test"),BorderLayout.WEST);clock.setFont(Theme.get().titleFont);clock.setForeground(Theme.get().accent);header.add(clock,BorderLayout.EAST);root.add(header,BorderLayout.NORTH);
            JPanel center=new JPanel(new BorderLayout(12,10));center.setOpaque(false);question.setFont(Theme.get().headingFont);question.setForeground(Theme.get().text);center.add(question,BorderLayout.NORTH);
            JPanel options=new JPanel(new GridLayout(0,1,8,8));options.setOpaque(false);for(int i=0;i<4;i++){choices[i]=new JRadioButton();choices[i].setOpaque(false);choices[i].setForeground(Theme.get().text);choices[i].setFont(Theme.get().bodyFont);choices[i].setToolTipText("Select this answer option");group.add(choices[i]);final int choice=i;choices[i].addActionListener(e->{answers[index]=choice;skipped[index]=false;refreshPalette();});options.add(choices[i]);}center.add(options,BorderLayout.CENTER);
            JPanel palette=new JPanel(new GridLayout(0,5,5,5));palette.setPreferredSize(new Dimension(210,1));palette.setBackground(Theme.get().card);palette.setBorder(BorderFactory.createTitledBorder("Question palette"));this.palette=new JButton[qs.size()];for(int i=0;i<qs.size();i++){final int n=i;this.palette[i]=new JButton(""+(i+1));Ui.retroButton(this.palette[i]);this.palette[i].addActionListener(e->{storeCurrent();index=n;showQuestion();});palette.add(this.palette[i]);}root.add(palette,BorderLayout.EAST);
            JPanel controls=new JPanel(new FlowLayout(FlowLayout.LEFT));controls.setOpaque(false);JButton prev=new JButton("Previous"),skip=new JButton("Skip"),mark=new JButton("Mark for review"),next=new JButton("Next"),submit=new JButton("Submit test");for(JButton b:new JButton[]{prev,skip,mark,next,submit})Ui.retroButton(b);prev.setToolTipText("Go to the previous question");skip.setToolTipText("Leave this question unanswered");mark.setToolTipText("Flag this question to revisit");next.setToolTipText("Go to the next question");submit.setToolTipText("Submit answers and view the detailed score");prev.addActionListener(e->{storeCurrent();if(index>0){index--;showQuestion();}});next.addActionListener(e->{storeCurrent();if(index<qs.size()-1){index++;showQuestion();}});skip.addActionListener(e->{answers[index]=-1;skipped[index]=true;group.clearSelection();refreshPalette();if(index<qs.size()-1){index++;showQuestion();}});mark.addActionListener(e->{marked[index]=!marked[index];refreshPalette();});submit.addActionListener(e->finish());for(JButton b:new JButton[]{prev,skip,mark,next,submit})controls.add(b);root.add(center,BorderLayout.CENTER);root.add(controls,BorderLayout.SOUTH);dialog.setContentPane(root);showQuestion();started=System.currentTimeMillis();javax.swing.Timer tick=new javax.swing.Timer(500,e->{remaining=Math.max(0,qs.size()*Math.max(10,ctx.data.profile.quizSeconds)-(int)((System.currentTimeMillis()-started)/1000));clock.setText("Remaining  "+fmt(remaining));if(remaining==0){((javax.swing.Timer)e.getSource()).stop();finish();}});tick.start();dialog.setVisible(true);tick.stop();
        }
        void storeCurrent(){for(int i=0;i<4;i++)if(choices[i].isSelected())answers[index]=i;}
        void showQuestion(){QuizQuestion q=qs.get(index);question.setText("<html>"+(index+1)+" / "+qs.size()+"  ·  "+escape(q.subject+" / "+q.topic)+"<br><br>"+escape(q.prompt)+"</html>");group.clearSelection();for(int i=0;i<4;i++){choices[i].setText((char)('A'+i)+"  "+q.choices[i]);choices[i].setSelected(answers[index]==i);}refreshPalette();}
        void refreshPalette(){if(palette==null)return;for(int i=0;i<palette.length;i++){palette[i].setBackground(i==index?new Color(145,108,240):answers[i]>=0?new Color(95,194,155):marked[i]?new Color(237,171,73):skipped[i]?new Color(91,137,180):Theme.get().cardAlt);palette[i].setForeground(Color.WHITE);palette[i].setToolTipText(answers[i]>=0?(marked[i]?"Answered · marked for review":"Answered"):marked[i]?"Marked for review":skipped[i]?"Skipped":"Not answered");}}
        void finish(){
            if(!dialog.isShowing())return;
            storeCurrent();
            if(JOptionPane.showConfirmDialog(dialog,"Submit this test now? Unanswered questions score zero.","Submit mock test",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION&&remaining>0)return;
            int right=0,wrong=0,skip=0,score=0;
            int elapsed=(int)Math.max(0,(System.currentTimeMillis()-started)/1000);
            String session=java.util.UUID.randomUUID().toString();
            java.util.List<QuizAttempt> saved=new java.util.ArrayList<>();
            StringBuilder report=new StringBuilder("MOCK TEST RESULT\n\n");
            java.util.Map<String,int[]> topicScores=new java.util.HashMap<>();
            for(int i=0;i<qs.size();i++){
                QuizQuestion q=qs.get(i);int a=answers[i];
                if(a<0){skip++;ctx.quiz.record(ctx.data,q,-1,0);saved.add(ctx.data.quizAttempts.get(ctx.data.quizAttempts.size()-1));report.append(i+1).append(". SKIPPED — ").append(q.subject).append(" / ").append(q.topic).append("\n");continue;}
                boolean correct=a==q.correctIndex;
                if(correct){right++;score+=4;}else{wrong++;score--;}
                int[] ts=topicScores.computeIfAbsent(q.subject+"\n"+q.topic,k->new int[2]);ts[1]++;if(correct)ts[0]++;
                ctx.quiz.record(ctx.data,q,a,0);
                saved.add(ctx.data.quizAttempts.get(ctx.data.quizAttempts.size()-1));
                report.append(i+1).append(correct?". CORRECT — ":". INCORRECT — ").append(q.subject).append(" / ").append(q.topic)
                        .append("\n   Your answer: ").append(q.choices[a]).append(" | Correct: ").append(q.choices[q.correctIndex])
                        .append("\n   ").append(q.explanation).append("\n");
            }
            for(QuizAttempt attempt:saved){attempt.quizSessionId=session;attempt.quizSessionSize=qs.size();attempt.quizSessionScore=right;attempt.quizSessionSeconds=elapsed;}
            for(java.util.Map.Entry<String,int[]>e:topicScores.entrySet()){String[] key=e.getKey().split("\n",2);int[] v=e.getValue();ctx.knowledge.recordQuiz(ctx.data,key[0],key[1],v[0]/(double)v[1],java.time.LocalDate.now());}
            GamificationService.Award award=new GamificationService().awardQuiz(ctx,right,qs.size()-skip);
            QuizScreen.rebalancePlan(ctx);
            report.insert(0,"Score: "+score+" points (+4/−1)  •  Correct: "+right+"  •  Wrong: "+wrong+"  •  Skipped: "+skip
                    +"  •  Time: "+String.format("%d min %02d sec",elapsed/60,elapsed%60)+"  •  XP: +"+award.gained+"\n\n");
            ctx.saveQuietly();dialog.dispose();JTextArea area=new JTextArea(report.toString(),24,72);area.setEditable(false);area.setLineWrap(true);area.setWrapStyleWord(true);area.setFont(Theme.get().bodyFont);
            JOptionPane.showMessageDialog(ctx.applet,new JScrollPane(area),"Detailed mock-test report",JOptionPane.INFORMATION_MESSAGE);
            if(award.newLevel>award.oldLevel)LevelUpDialog.show(ctx.applet,award);
        }
        String fmt(int sec){return String.format("%02d:%02d",sec/60,sec%60);}String escape(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");}
    }
}

// ======================================================================
// Onboarding
// ======================================================================
/** Three-step first-run wizard kept in Swing for the desktop and legacy applet-style host. */
final class Onboarding {
    private Onboarding() { }
    public static boolean run(AppContext ctx) {
        if(ctx.data.profile.name!=null&&!ctx.data.profile.name.isBlank())return true;
        JDialog dialog=new JDialog((Frame)null,"Welcome to AI Study Planner",true);dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        CardLayout cards=new CardLayout();JPanel deck=new JPanel(cards);JTextField name=new JTextField(),course=new JTextField(),subjects=new JTextField("Math, Science, English");
        JSpinner hours=new JSpinner(new SpinnerNumberModel(3.0,0.5,12.0,0.5)),weeks=new JSpinner(new SpinnerNumberModel(4,1,24,1));
        JSpinner goal=new JSpinner(new SpinnerNumberModel(90,15,720,15));
        deck.add(step("1 · About you","A little personalization helps the planner feel like yours.","Your name",name,"Course / class",course),"0");
        deck.add(step("2 · Your studies","Add subjects. You can add detailed topics later.","Subjects (comma separated)",subjects,"Hours available each day",hours,"Weeks until your next exam",weeks),"1");
        deck.add(step("3 · Your rhythm","Set a practical starting goal. You can change these any time.","Daily study goal (minutes)",goal),"2");
        JLabel progress=new JLabel("  STEP 1 OF 3");progress.setFont(Theme.get().bodyFont.deriveFont(Font.BOLD,14f));
        JButton back=new JButton("Back"),next=new JButton("Continue"),cancel=new JButton("Cancel");Ui.retroButton(back);Ui.retroButton(next);Ui.retroButton(cancel);back.setToolTipText("Return to the previous onboarding step");next.setToolTipText("Continue to the next onboarding step");cancel.setToolTipText("Close onboarding without saving");
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT));buttons.add(cancel);buttons.add(back);buttons.add(next);
        JPanel root=new JPanel(new BorderLayout(0,12));root.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));root.add(progress,BorderLayout.NORTH);root.add(deck,BorderLayout.CENTER);root.add(buttons,BorderLayout.SOUTH);
        final int[] current={0};final boolean[] saved={false};back.setEnabled(false);
        back.addActionListener(e->{if(current[0]>0){current[0]--;cards.show(deck,""+current[0]);progress.setText("  STEP "+(current[0]+1)+" OF 3");back.setEnabled(current[0]>0);next.setText("Continue");}});
        next.addActionListener(e->{if(current[0]==0&&name.getText().isBlank()){JOptionPane.showMessageDialog(dialog,"Please enter your name to continue.");return;}if(current[0]<2){current[0]++;cards.show(deck,""+current[0]);progress.setText("  STEP "+(current[0]+1)+" OF 3");back.setEnabled(true);if(current[0]==2)next.setText("Finish setup");return;}
            ctx.data.profile.name=name.getText().trim();ctx.data.profile.course=course.getText().trim();ctx.data.profile.availableMinutes=(int)Math.round((Double)hours.getValue()*60);ctx.data.profile.todayAvailableMinutes=ctx.data.profile.availableMinutes;ctx.data.profile.dailyGoalMinutes=(Integer)goal.getValue();
            LocalDate exam=LocalDate.now().plusWeeks((Integer)weeks.getValue());for(String raw:subjects.getText().split(",")){String s=raw.trim();if(!s.isEmpty()){Subject subject=new Subject(s,exam,3);subject.ensureDefaultTopics();ctx.data.subjects.add(subject);}}
            ctx.saveQuietly();saved[0]=true;dialog.dispose();});
        cancel.addActionListener(e->dialog.dispose());dialog.setContentPane(root);dialog.setSize(520,390);dialog.setLocationRelativeTo(null);dialog.setVisible(true);return saved[0];
    }
    private static JPanel step(String title,String subtitle,Object... fields){JPanel p=new JPanel(new BorderLayout(0,12));JPanel head=new JPanel(new GridLayout(2,1));head.add(new JLabel(title));head.add(new JLabel(subtitle));p.add(head,BorderLayout.NORTH);JPanel form=new JPanel(new GridLayout(0,2,10,10));for(int i=0;i<fields.length;i+=2){JLabel label=new JLabel((String)fields[i]);JComponent control=(JComponent)fields[i+1];label.setToolTipText("Enter "+fields[i]);control.setToolTipText("Your "+((String)fields[i]).toLowerCase());form.add(label);form.add(control);}p.add(form,BorderLayout.CENTER);return p;}
}

// ======================================================================
// PlannerScreen
// ======================================================================
final class PlannerScreen {
    private PlannerScreen() {
    }

    public static void quickAdd(AppContext ctx) {
        editTask(ctx, null, new DefaultListModel<>(), new ArrayList<>());
    }

    public static JPanel build(AppContext ctx) {
        Theme theme = Theme.get();
        JPanel page = Ui.page("Study Planner",
                "Generate a spaced-repetition timetable, then tick blocks off as you go.");

        DefaultListModel<String> model = new DefaultListModel<>();
        List<StudyTask> shown = new ArrayList<>();
        JList<String> list = new JList<>(model);
        list.setFont(theme.bodyFont);
        list.setForeground(theme.text);
        list.setBackground(theme.card);
        fill(ctx, model, shown, "");

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);
        AccentButton generate = AccentButton.primary("Generate smart plan");
        AccentButton add = AccentButton.ghost("Add task");
        AccentButton search = AccentButton.ghost("Search");
        AccentButton recovery = AccentButton.ghost("Smart recovery");
        top.add(generate);
        top.add(add);
        top.add(search);
        top.add(recovery);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bottom.setOpaque(false);
        AccentButton complete = AccentButton.primary("Mark done");
        AccentButton edit = AccentButton.ghost("Edit");
        AccentButton delete = AccentButton.ghost("Delete");
        bottom.add(complete);
        bottom.add(edit);
        bottom.add(delete);

        generate.addActionListener(e -> {
            if (ctx.data.subjects.isEmpty()) {
                JOptionPane.showMessageDialog(page, "Add subjects first (Subjects tab).");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(page,
                    "Replace unfinished generated blocks with a new plan? Manual tasks are kept.",
                    "Generate plan", JOptionPane.OK_CANCEL_OPTION);
            if (confirm != JOptionPane.OK_OPTION) {
                return;
            }
            ctx.data.tasks.removeIf(t -> !t.done && !StudyTask.MANUAL.equals(t.kind));
            ctx.data.tasks.addAll(ctx.planner.generateSchedule(ctx.data, LocalDate.now()));
            ctx.saveOrWarn();
            fill(ctx, model, shown, "");
        });
        add.addActionListener(e -> editTask(ctx, null, model, shown));
        search.addActionListener(e -> {
            String q = JOptionPane.showInputDialog(page, "Search tasks:");
            fill(ctx, model, shown, q == null ? "" : q);
        });
        recovery.addActionListener(e ->
                JOptionPane.showMessageDialog(page, ctx.planner.recoveryAdvice(ctx.data),
                        "Smart recovery", JOptionPane.INFORMATION_MESSAGE));
        complete.addActionListener(e -> {
            int i = list.getSelectedIndex();
            if (i >= 0) {
                shown.get(i).done = true;
                ctx.saveOrWarn();
                fill(ctx, model, shown, "");
            }
        });
        edit.addActionListener(e -> {
            int i = list.getSelectedIndex();
            if (i >= 0) {
                editTask(ctx, shown.get(i), model, shown);
            }
        });
        delete.addActionListener(e -> {
            int i = list.getSelectedIndex();
            if (i >= 0 && JOptionPane.showConfirmDialog(page, "Delete this task?", "Confirm",
                    JOptionPane.OK_CANCEL_OPTION) == 0) {
                ctx.data.tasks.remove(shown.get(i));
                ctx.saveOrWarn();
                fill(ctx, model, shown, "");
            }
        });

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(top, BorderLayout.NORTH);
        body.add(Ui.scroll(list), BorderLayout.CENTER);
        body.add(bottom, BorderLayout.SOUTH);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private static void fill(AppContext ctx, DefaultListModel<String> model,
                             List<StudyTask> shown, String filter) {
        shown.clear();
        model.clear();
        String q = filter.toLowerCase();
        List<StudyTask> copy = new ArrayList<>(ctx.data.tasks);
        copy.sort(Comparator.comparing((StudyTask t) -> t.day).thenComparingInt(t -> -t.priority));
        for (StudyTask task : copy) {
            String row = (task.done ? "✓ " : "○ ") + task.day + "  " + task.label()
                    + "  | " + task.minutes + "m | " + task.kind
                    + (task.due.isBefore(LocalDate.now()) && !task.done ? "  OVERDUE" : "");
            if ((task.subject + " " + task.topic).toLowerCase().contains(q)) {
                shown.add(task);
                model.addElement(row);
            }
        }
    }

    private static void editTask(AppContext ctx, StudyTask old,
                                 DefaultListModel<String> model, List<StudyTask> shown) {
        JTextField subject = new JTextField(old == null ? "" : old.subject);
        JTextField topic = new JTextField(old == null ? "" : old.topic);
        JTextField day = new JTextField(old == null ? LocalDate.now().toString() : old.day.toString());
        JTextField due = new JTextField(old == null ? LocalDate.now().plusDays(1).toString() : old.due.toString());
        JSpinner min = new JSpinner(new SpinnerNumberModel(old == null ? 30 : old.minutes, 5, 600, 5));
        JSpinner pri = new JSpinner(new SpinnerNumberModel(old == null ? 3 : old.priority, 1, 5, 1));
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new javax.swing.JLabel("Subject"));
        form.add(subject);
        form.add(new javax.swing.JLabel("Topic"));
        form.add(topic);
        form.add(new javax.swing.JLabel("Minutes"));
        form.add(min);
        form.add(new javax.swing.JLabel("Priority 1–5"));
        form.add(pri);
        form.add(new javax.swing.JLabel("Plan day YYYY-MM-DD"));
        form.add(day);
        form.add(new javax.swing.JLabel("Deadline YYYY-MM-DD"));
        form.add(due);
        if (JOptionPane.showConfirmDialog(null, form, old == null ? "Add task" : "Edit task",
                JOptionPane.OK_CANCEL_OPTION) != 0) {
            return;
        }
        try {
            if (subject.getText().isBlank() || topic.getText().isBlank()) {
                throw new IllegalArgumentException();
            }
            LocalDate a = LocalDate.parse(day.getText().trim());
            LocalDate b = LocalDate.parse(due.getText().trim());
            if (old == null) {
                ctx.data.tasks.add(new StudyTask(subject.getText().trim(), topic.getText().trim(),
                        (Integer) min.getValue(), (Integer) pri.getValue(), a, b, StudyTask.MANUAL));
            } else {
                old.subject = subject.getText().trim();
                old.topic = topic.getText().trim();
                old.minutes = (Integer) min.getValue();
                old.priority = (Integer) pri.getValue();
                old.day = a;
                old.due = b;
            }
            ctx.saveOrWarn();
            fill(ctx, model, shown, "");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Enter subject, topic, and valid dates (YYYY-MM-DD).");
        }
    }
}

// ======================================================================
// ProgressScreen
// ======================================================================
/** Visual progress and learning-gap dashboard. */
final class ProgressScreen {
    private ProgressScreen() { }

    public static JPanel build(AppContext ctx) {
        Theme theme = Theme.get();
        JPanel page = Ui.page("Progress & Weak Areas", "See what is improving and where your next study session can make the biggest difference.");
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(2, 2, 14, 8));

        int progress = overallProgress(ctx);
        List<CompetencyAnalyzer.TopicScore> topics = topicScores(ctx);
        boolean hasProgress = !ctx.data.quizAttempts.isEmpty() || !ctx.data.sessions.isEmpty()
                || !ctx.data.knowledge.isEmpty();

        JPanel metrics = new JPanel(new GridLayout(1, 4, 12, 0));
        metrics.setOpaque(false);
        metrics.add(metric("Overall progress", progress + "%", "◉", theme.accent, "Average tracked mastery or quiz accuracy"));
        metrics.add(metric("Quizzes attempted", quizCount(ctx.data.quizAttempts) + "", "✎", new Color(89, 113, 221), "Completed quiz sessions in your saved results"));
        metrics.add(metric("Average score", averageScore(ctx.data.quizAttempts) + "%", "↗", new Color(208, 139, 55), "Accuracy across saved quiz answers"));
        metrics.add(metric("Study streak", DashboardScreen.streak(ctx) + " days", "✦", theme.success, "Current recorded study-session streak"));
        content.add(metrics);
        content.add(Box.createVerticalStrut(14));

        if (!hasProgress) {
            content.add(emptyState(ctx));
            content.add(Box.createVerticalStrut(14));
        } else {
            content.add(summaryCharts(ctx, progress, subjectValues(ctx)));
            content.add(Box.createVerticalStrut(14));
            content.add(chartRow("Quiz score trend", "Daily average accuracy · choose a window", trendPanel(ctx)));
            content.add(Box.createVerticalStrut(14));
            content.add(chartRow("Weekly study activity", "Recorded focused minutes over the last 7 days", new ProgressCharts.WeeklyActivity(weeklyActivity(ctx))));
            content.add(Box.createVerticalStrut(14));
            content.add(weakAreas(ctx, topics));
            content.add(Box.createVerticalStrut(14));
            content.add(recommendations(ctx, topics));
            content.add(Box.createVerticalStrut(14));
        }

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        AccentButton export = AccentButton.ghost("Export CSV");
        AccentButton reflect = AccentButton.ghost("Daily reflection");
        export.setToolTipText("Save your recorded study sessions as a CSV file");
        reflect.setToolTipText("Record a note about today's learning");
        export.addActionListener(e -> csv(ctx, page));
        reflect.addActionListener(e -> reflection(ctx, page));
        actions.add(export); actions.add(reflect);
        content.add(actions);
        page.add(Ui.scroll(content), BorderLayout.CENTER);
        return page;
    }

    private static JPanel metric(String title, String value, String icon, Color iconColor, String tip) {
        Theme theme = Theme.get();
        RoundedPanel card = Ui.card();
        card.setLayout(new BorderLayout(8, 8));
        JPanel top = new JPanel(new BorderLayout(6, 0)); top.setOpaque(false);
        top.add(Ui.label(title, theme.smallFont, theme.muted), BorderLayout.CENTER);
        JLabel badge = new JLabel(icon, JLabel.CENTER); badge.setFont(theme.headingFont.deriveFont(Font.BOLD, 17f));
        badge.setForeground(iconColor); badge.setOpaque(true); badge.setBackground(soft(iconColor));
        badge.setPreferredSize(new Dimension(34, 34)); badge.setToolTipText(tip);
        top.add(badge, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);
        JLabel number = Ui.label(value, theme.headingFont.deriveFont(Font.BOLD, 25f), theme.text);
        number.setToolTipText(tip);
        card.add(number, BorderLayout.CENTER);
        card.setToolTipText(tip);
        card.setPreferredSize(new Dimension(150, 100));
        return card;
    }

    private static JPanel summaryCharts(AppContext ctx, int progress, List<ProgressCharts.Value> subjects) {
        Theme theme = Theme.get();
        JPanel row = new JPanel(new GridLayout(1, 2, 14, 0)); row.setOpaque(false);

        RoundedPanel syllabus = Ui.card();
        syllabus.add(Ui.label("Syllabus mastery", theme.headingFont, theme.text), BorderLayout.NORTH);
        JPanel donutContent = new JPanel(new BorderLayout(16, 0)); donutContent.setOpaque(false);
        ProgressRing ring = new ProgressRing(); ring.setPreferredSize(new Dimension(160, 160));
        ring.setProgress(progress / 100f, "mastery");
        donutContent.add(ring, BorderLayout.WEST);
        JPanel context = new JPanel(); context.setOpaque(false); context.setLayout(new BoxLayout(context, BoxLayout.Y_AXIS));
        context.add(Box.createVerticalGlue());
        context.add(Ui.label(progress + "% overall", theme.headingFont.deriveFont(Font.BOLD, 22f), theme.text));
        context.add(Box.createVerticalStrut(7));
        context.add(Ui.label(trackedTopicCount(ctx) + " topics tracked", theme.bodyFont, theme.muted));
        context.add(Box.createVerticalStrut(7));
        context.add(Ui.label("Based on saved topic mastery", theme.smallFont, theme.muted));
        context.add(Box.createVerticalGlue());
        donutContent.add(context, BorderLayout.CENTER);
        syllabus.add(donutContent, BorderLayout.CENTER);

        RoundedPanel subjectCard = Ui.card();
        subjectCard.add(Ui.label("Subject scores", theme.headingFont, theme.text), BorderLayout.NORTH);
        if (subjects.isEmpty()) subjectCard.add(Ui.label("Add subjects or take a quiz to see subject scores.", theme.bodyFont, theme.muted), BorderLayout.CENTER);
        else subjectCard.add(new ProgressCharts.SubjectBars(subjects), BorderLayout.CENTER);
        row.add(syllabus); row.add(subjectCard);
        return row;
    }

    private static JPanel chartRow(String title, String subtitle, JPanel chart) {
        Theme theme = Theme.get();
        RoundedPanel card = Ui.card();
        JPanel heading = new JPanel(new BorderLayout(8, 2)); heading.setOpaque(false);
        heading.add(Ui.label(title, theme.headingFont, theme.text), BorderLayout.NORTH);
        heading.add(Ui.label(subtitle, theme.smallFont, theme.muted), BorderLayout.SOUTH);
        card.add(heading, BorderLayout.NORTH);
        card.add(chart, BorderLayout.CENTER);
        card.setMinimumSize(new Dimension(0, 235));
        card.setPreferredSize(new Dimension(700, 250));
        return card;
    }

    private static JPanel trendPanel(AppContext ctx) {
        Theme theme = Theme.get();
        JPanel panel = new JPanel(new BorderLayout(0, 6)); panel.setOpaque(false);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0)); controls.setOpaque(false);
        AccentButton seven = AccentButton.ghost("7 days"), thirty = AccentButton.ghost("30 days");
        seven.setPreferredSize(new Dimension(82, 32)); thirty.setPreferredSize(new Dimension(88, 32));
        JPanel[] chart = new JPanel[1];
        Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> {
            int days = Boolean.TRUE.equals(panel.getClientProperty("thirty")) ? 30 : 7;
            chart[0] = new ProgressCharts.ScoreTrend(quizTrend(ctx, days));
            panel.removeAll(); panel.add(controls, BorderLayout.NORTH); panel.add(chart[0], BorderLayout.CENTER);
            controls.setVisible(true); panel.revalidate(); panel.repaint();
        };
        seven.addActionListener(e -> { panel.putClientProperty("thirty", false); refresh[0].run(); });
        thirty.addActionListener(e -> { panel.putClientProperty("thirty", true); refresh[0].run(); });
        controls.add(seven); controls.add(thirty);
        panel.putClientProperty("thirty", false);
        refresh[0].run();
        return panel;
    }

    private static JPanel weakAreas(AppContext ctx, List<CompetencyAnalyzer.TopicScore> scores) {
        Theme theme = Theme.get();
        RoundedPanel card = Ui.card();
        card.add(Ui.label("Weak areas & strengths", theme.headingFont, theme.text), BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout(14, 10)); body.setOpaque(false);
        JPanel bars = new JPanel(); bars.setOpaque(false); bars.setLayout(new BoxLayout(bars, BoxLayout.Y_AXIS));
        List<CompetencyAnalyzer.TopicScore> sorted = new ArrayList<>(scores);
        sorted.sort(Comparator.comparingInt(s -> s.percent));
        int shown = Math.min(8, sorted.size());
        for (int i = 0; i < shown; i++) bars.add(topicBar(sorted.get(i)));
        if (sorted.isEmpty()) bars.add(Ui.label("Topic scores will appear after your first quiz.", theme.bodyFont, theme.muted));
        body.add(bars, BorderLayout.CENTER);

        JPanel chips = new JPanel(); chips.setOpaque(false); chips.setLayout(new BoxLayout(chips, BoxLayout.Y_AXIS));
        chips.add(Ui.label("Strong topics", theme.bodyFont.deriveFont(Font.BOLD), theme.success));
        chips.add(chipGroup(scores, true)); chips.add(Box.createVerticalStrut(10));
        chips.add(Ui.label("Needs focus", theme.bodyFont.deriveFont(Font.BOLD), theme.danger));
        chips.add(chipGroup(scores, false));
        body.add(chips, BorderLayout.EAST);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private static JPanel topicBar(CompetencyAnalyzer.TopicScore score) {
        Theme theme = Theme.get();
        JPanel row = new JPanel(new BorderLayout(10, 3)); row.setOpaque(false);
        String full = score.subject + " · " + score.topic;
        JLabel label = Ui.label(full, theme.smallFont, theme.text); label.setToolTipText(full + " · " + score.correct + " correct of " + score.attempts);
        JLabel value = Ui.label(score.percent + "%", theme.smallFont.deriveFont(Font.BOLD), ProgressCharts.levelColor(score.percent, theme));
        row.add(label, BorderLayout.CENTER); row.add(value, BorderLayout.EAST);
        JPanel track = new JPanel(new BorderLayout()); track.setOpaque(false);
        track.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        ProgressCharts.TopicMeter bar = new ProgressCharts.TopicMeter(score.percent);
        bar.setPreferredSize(new Dimension(230, 22));
        bar.setToolTipText(full + " · " + score.percent + "% · " + score.correct + "/" + score.attempts + " correct");
        track.add(bar, BorderLayout.CENTER);
        JPanel wrap = new JPanel(new BorderLayout()); wrap.setOpaque(false); wrap.add(row, BorderLayout.NORTH); wrap.add(track, BorderLayout.CENTER);
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        return wrap;
    }

    private static JPanel chipGroup(List<CompetencyAnalyzer.TopicScore> scores, boolean strong) {
        Theme theme = Theme.get(); JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 4)); chips.setOpaque(false);
        int count = 0;
        for (CompetencyAnalyzer.TopicScore score : scores) {
            if ((strong && score.percent >= 75) || (!strong && score.percent < 60)) {
                Color color = ProgressCharts.levelColor(score.percent, theme);
                JLabel chip = new JLabel(score.topic, JLabel.CENTER); chip.setFont(theme.smallFont);
                chip.setForeground(color); chip.setBackground(soft(color)); chip.setOpaque(true);
                chip.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
                chip.setToolTipText(score.subject + " · " + score.topic + " · " + score.percent + "%");
                chips.add(chip); count++;
                if (count == 6) break;
            }
        }
        if (count == 0) chips.add(Ui.label(strong ? "Building your strengths" : "No urgent gaps", theme.smallFont, theme.muted));
        return chips;
    }

    private static JPanel recommendations(AppContext ctx, List<CompetencyAnalyzer.TopicScore> scores) {
        Theme theme = Theme.get();
        JPanel section = new JPanel(new BorderLayout(0, 10)); section.setOpaque(false);
        section.add(Ui.label("Recommended next steps", theme.headingFont, theme.text), BorderLayout.NORTH);
        JPanel cards = new JPanel(new GridLayout(1, 3, 12, 0)); cards.setOpaque(false);
        List<CompetencyAnalyzer.TopicScore> picks = new ArrayList<>(scores);
        picks.sort(Comparator.comparingInt(s -> s.percent));
        if (picks.isEmpty()) {
            for (Subject subject : ctx.data.subjects) {
                if (!subject.topics.isEmpty()) picks.add(new CompetencyAnalyzer.TopicScore(subject.name, subject.topics.get(0).name, 0, 0, subject.strength * 20, false));
                if (picks.size() == 3) break;
            }
        }
        if (picks.isEmpty()) {
            RoundedPanel starter = recommendationCard(ctx, "Start with a short quiz", "A quick round helps identify what to strengthen next.", "Take a quiz", "");
            cards.add(starter);
        } else {
            for (int i = 0; i < Math.min(3, picks.size()); i++) {
                CompetencyAnalyzer.TopicScore score = picks.get(i);
                String title = "Revise " + score.subject + ": " + score.topic;
                String desc = score.attempts == 0 ? "Build a first score for this topic." : "Current accuracy is " + score.percent + "%. A short retrieval round can strengthen recall.";
                cards.add(recommendationCard(ctx, title, desc, "Start quiz", score.subject));
            }
        }
        while (cards.getComponentCount() < 3) {
            JPanel spacer = new JPanel(); spacer.setOpaque(false); cards.add(spacer);
        }
        section.add(cards, BorderLayout.CENTER);
        return section;
    }

    private static RoundedPanel recommendationCard(AppContext ctx, String title, String description, String actionText, String subject) {
        Theme theme = Theme.get(); RoundedPanel card = Ui.card();
        JPanel head = new JPanel(new BorderLayout(8, 0)); head.setOpaque(false);
        JLabel icon = new JLabel("✦", JLabel.CENTER); icon.setOpaque(true); icon.setForeground(theme.accent);
        icon.setBackground(theme.accentSoft); icon.setPreferredSize(new Dimension(30, 30));
        head.add(icon, BorderLayout.WEST);
        JLabel titleLabel = Ui.label(title, theme.bodyFont.deriveFont(Font.BOLD), theme.text); titleLabel.setToolTipText(title);
        head.add(titleLabel, BorderLayout.CENTER); card.add(head, BorderLayout.NORTH);
        JLabel descriptionLabel = Ui.label("<html><body style='width:190px'>" + escapeHtml(description) + "</body></html>", theme.smallFont, theme.muted);
        card.add(descriptionLabel, BorderLayout.CENTER);
        AccentButton action = AccentButton.primary(actionText); action.setFont(theme.smallFont.deriveFont(Font.BOLD));
        action.setToolTipText("Open the quiz center to practice " + (subject.isBlank() ? "a topic" : subject));
        action.addActionListener(e -> ctx.applet.showScreen("Quiz Center"));
        card.add(action, BorderLayout.SOUTH);
        card.setPreferredSize(new Dimension(220, 154));
        return card;
    }

    private static JPanel emptyState(AppContext ctx) {
        Theme theme = Theme.get(); RoundedPanel card = Ui.card();
        JPanel inside = new JPanel(new BorderLayout(18, 0)); inside.setOpaque(false);
        LearningIllustration illustration = new LearningIllustration(); illustration.setPreferredSize(new Dimension(160, 140));
        inside.add(illustration, BorderLayout.WEST);
        JPanel copy = new JPanel(); copy.setOpaque(false); copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(Ui.label("Your progress story starts here", theme.headingFont, theme.text));
        copy.add(Box.createVerticalStrut(8));
        copy.add(Ui.label("Finish a quiz to unlock topic strengths, score trends,", theme.bodyFont, theme.muted));
        copy.add(Ui.label("personalized recommendations, and your study heatmap.", theme.bodyFont, theme.muted));
        copy.add(Box.createVerticalStrut(14));
        AccentButton firstQuiz = AccentButton.primary("Take your first quiz");
        firstQuiz.setToolTipText("Open Quiz Center and begin your first practice round");
        firstQuiz.addActionListener(e -> ctx.applet.showScreen("Quiz Center"));
        copy.add(firstQuiz); inside.add(copy, BorderLayout.CENTER); card.add(inside, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(700, 190));
        return card;
    }

    private static List<ProgressCharts.Value> subjectValues(AppContext ctx) {
        Map<String, Boolean> names = new LinkedHashMap<>();
        for (Subject subject : ctx.data.subjects) names.put(subject.name, true);
        for (QuizAttempt attempt : ctx.data.quizAttempts) names.put(attempt.subject, true);
        List<ProgressCharts.Value> result = new ArrayList<>();
        for (String name : names.keySet()) {
            int attempted = 0; for (QuizAttempt q : ctx.data.quizAttempts) if (q.subject.equalsIgnoreCase(name)) attempted++;
            int value = ctx.competency.subjectPercent(ctx.data, name);
            result.add(new ProgressCharts.Value(name, value, attempted == 0 ? "estimated from planner strength" : attempted + " quiz answers"));
        }
        result.sort(Comparator.comparingInt(v -> v.value));
        return result;
    }

    private static List<CompetencyAnalyzer.TopicScore> topicScores(AppContext ctx) {
        Map<String, CompetencyAnalyzer.TopicScore> scores = new LinkedHashMap<>();
        for (CompetencyAnalyzer.TopicScore score : ctx.competency.scores(ctx.data)) scores.put(key(score.subject, score.topic), score);
        for (KnowledgeTopic state : ctx.data.knowledge.values()) {
            String key = key(state.subject, state.topic);
            if (!scores.containsKey(key)) scores.put(key, new CompetencyAnalyzer.TopicScore(state.subject, state.topic,
                    Math.max(0, state.attempts), Math.max(0, state.correct), state.mastery, state.mastery < ctx.data.profile.weakThreshold));
        }
        return new ArrayList<>(scores.values());
    }

    private static int overallProgress(AppContext ctx) {
        int total = 0, sum = 0;
        for (KnowledgeTopic state : ctx.data.knowledge.values()) { sum += Math.max(0, Math.min(100, state.mastery)); total++; }
        if (total > 0) return Math.round(sum / (float) total);
        List<CompetencyAnalyzer.TopicScore> scores = ctx.competency.scores(ctx.data);
        if (!scores.isEmpty()) { for (CompetencyAnalyzer.TopicScore score : scores) sum += score.percent; return Math.round(sum / (float) scores.size()); }
        return averageScore(ctx.data.quizAttempts);
    }

    private static int trackedTopicCount(AppContext ctx) {
        Set<String> keys = new LinkedHashSet<>();
        for (KnowledgeTopic state : ctx.data.knowledge.values()) keys.add(key(state.subject, state.topic));
        for (CompetencyAnalyzer.TopicScore score : ctx.competency.scores(ctx.data)) keys.add(key(score.subject, score.topic));
        return keys.size();
    }

    private static int averageScore(List<QuizAttempt> attempts) {
        if (attempts.isEmpty()) return 0;
        int correct = 0; for (QuizAttempt attempt : attempts) if (attempt.correct) correct++;
        return (int) Math.round(100.0 * correct / attempts.size());
    }

    private static int quizCount(List<QuizAttempt> attempts) {
        Set<String> sessions = new LinkedHashSet<>();
        for (QuizAttempt attempt : attempts) {
            if (attempt.quizSessionId != null && !attempt.quizSessionId.isBlank()) sessions.add("id:" + attempt.quizSessionId);
            else if (attempt.when != null) sessions.add("day:" + attempt.when.toLocalDate());
        }
        return sessions.size();
    }

    private static List<ProgressCharts.DayValue> quizTrend(AppContext ctx, int days) {
        Map<LocalDate, int[]> daily = new LinkedHashMap<>(); LocalDate today = LocalDate.now();
        for (QuizAttempt attempt : ctx.data.quizAttempts) if (attempt.when != null) {
            LocalDate date = attempt.when.toLocalDate();
            if (date.isBefore(today.minusDays(days - 1L)) || date.isAfter(today)) continue;
            int[] pair = daily.computeIfAbsent(date, d -> new int[2]); pair[1]++; if (attempt.correct) pair[0]++;
        }
        List<ProgressCharts.DayValue> result = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i); int[] pair = daily.get(day);
            result.add(new ProgressCharts.DayValue(day, pair == null ? -1 : (int) Math.round(pair[0] * 100.0 / pair[1])));
        }
        return result;
    }

    private static List<ProgressCharts.DayValue> weeklyActivity(AppContext ctx) {
        List<ProgressCharts.DayValue> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) { LocalDate day = LocalDate.now().minusDays(i); result.add(new ProgressCharts.DayValue(day, ctx.data.minutesStudiedOn(day))); }
        return result;
    }

    private static String key(String subject, String topic) { return (subject == null ? "" : subject).toLowerCase() + "\n" + (topic == null ? "" : topic).toLowerCase(); }
    private static Color soft(Color color) { return new Color(color.getRed(), color.getGreen(), color.getBlue(), 38); }
    private static String escapeHtml(String value) { return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }

    private static void csv(AppContext ctx, JPanel page) {
        JFileChooser chooser = new JFileChooser(); chooser.setSelectedFile(new File("study-report.csv"));
        if (chooser.showSaveDialog(page) != JFileChooser.APPROVE_OPTION) return;
        try (PrintWriter writer = new PrintWriter(chooser.getSelectedFile(), java.nio.charset.StandardCharsets.UTF_8)) {
            writer.println("date,subject,planned_minutes,actual_minutes,complete");
            for (StudySession session : ctx.data.sessions) writer.printf("%s,\"%s\",%d,%d,%s%n", session.date,
                    session.subject.replace("\"", "\"\""), session.plannedMinutes, session.actualMinutes, session.complete);
            JOptionPane.showMessageDialog(page, "CSV exported.");
        } catch (Exception ex) { JOptionPane.showMessageDialog(page, "CSV export failed: " + ex.getMessage()); }
    }

    private static void reflection(AppContext ctx, JPanel page) {
        String key = LocalDate.now().toString();
        JTextArea area = new JTextArea(ctx.data.reflections.getOrDefault(key, ""), 8, 40);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(new JLabel("What did I learn? What was hard? What is next?"), BorderLayout.NORTH);
        wrap.add(new javax.swing.JScrollPane(area));
        if (JOptionPane.showConfirmDialog(page, wrap, "Daily reflection", JOptionPane.OK_CANCEL_OPTION) == 0) {
            ctx.data.reflections.put(key, area.getText()); ctx.saveOrWarn();
        }
    }

    /** Small friendly, vector illustration for a learner who has not taken a quiz yet. */
    private static final class LearningIllustration extends JPanel {
        LearningIllustration() { setOpaque(false); }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics); Theme theme = Theme.get(); Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int cx = getWidth() / 2, cy = getHeight() / 2;
            g.setColor(theme.accentSoft); g.fillOval(cx - 58, cy - 58, 116, 116);
            g.setColor(theme.accent); g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawRoundRect(cx - 34, cy - 21, 31, 43, 5, 5); g.drawRoundRect(cx + 3, cy - 21, 31, 43, 5, 5);
            g.drawLine(cx, cy - 19, cx, cy + 21); g.drawLine(cx - 27, cy - 10, cx - 9, cy - 10);
            g.drawLine(cx + 9, cy - 10, cx + 27, cy - 10); g.drawLine(cx - 27, cy, cx - 9, cy);
            g.drawLine(cx + 9, cy, cx + 27, cy); g.setColor(theme.success); g.fillOval(cx + 28, cy - 43, 10, 10);
            g.setColor(theme.accent); g.fillOval(cx - 49, cy + 32, 6, 6); g.dispose();
        }
    }
}

// ======================================================================
// QuizResultDialog
// ======================================================================
/** Visual quiz completion summary with a separate answer-review view. */
final class QuizResultDialog {
    private QuizResultDialog() { }

    static void show(Component parent, int correct, int wrong, int total, int seconds, int xp,
                     int timedOut, List<QuizAttempt> attempts, String planReason, List<String> badges) {
        Theme theme = Theme.get();
        Window owner = javax.swing.SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, "Practice complete", JDialog.ModalityType.APPLICATION_MODAL);
        JPanel root = new JPanel(new BorderLayout(12, 14));
        root.setBackground(theme.bg); root.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        root.add(Ui.label("Practice complete", theme.titleFont, theme.text), BorderLayout.NORTH);

        HoverCard score = new HoverCard(new BorderLayout(18, 0));
        ProgressRing ring = new ProgressRing(); ring.setPreferredSize(new Dimension(158, 158));
        ring.setProgress(total == 0 ? 0f : correct / (float) total, "score");
        score.add(ring, BorderLayout.WEST);
        JPanel summary = new JPanel(); summary.setOpaque(false); summary.setLayout(new BoxLayout(summary, BoxLayout.Y_AXIS));
        summary.add(Box.createVerticalGlue());
        summary.add(Ui.label(correct + " / " + total + " correct", theme.headingFont.deriveFont(Font.BOLD, 25f), theme.text));
        summary.add(Box.createVerticalStrut(8));
        summary.add(Ui.label("A useful step forward. Review the missed concepts while they're fresh.", theme.bodyFont, theme.muted));
        summary.add(Box.createVerticalStrut(12));
        JPanel stats = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); stats.setOpaque(false);
        stats.add(chip("✓  " + correct + " correct", theme.success));
        stats.add(chip("↻  " + wrong + " to review", theme.danger));
        stats.add(chip("◷  " + duration(seconds), theme.accent));
        stats.add(chip("✦  +" + xp + " XP", new Color(207, 139, 44)));
        summary.add(stats);
        if (timedOut > 0) summary.add(Ui.label(timedOut + " timed-out response(s) saved automatically.", theme.smallFont, theme.muted));
        if (!badges.isEmpty()) summary.add(Ui.label("Badges earned: " + String.join(" · ", badges), theme.smallFont, theme.accent));
        summary.add(Box.createVerticalGlue()); score.add(summary, BorderLayout.CENTER);

        HoverCard reason = new HoverCard(new BorderLayout(0, 6));
        reason.add(Ui.label("Why did the plan change?", theme.headingFont, theme.text), BorderLayout.NORTH);
        javax.swing.JTextArea why = new javax.swing.JTextArea(planReason);
        why.setEditable(false); why.setLineWrap(true); why.setWrapStyleWord(true); why.setOpaque(false);
        why.setForeground(theme.muted); why.setFont(theme.bodyFont);
        reason.add(why, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); actions.setOpaque(false);
        AccentButton review = AccentButton.ghost("Review answers");
        AccentButton close = AccentButton.primary("Done");
        review.addActionListener(e -> showReview(dialog, attempts));
        close.addActionListener(e -> dialog.dispose());
        actions.add(review); actions.add(close);
        JPanel center = new JPanel(new BorderLayout(0, 12)); center.setOpaque(false);
        center.add(score, BorderLayout.CENTER); center.add(reason, BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER); root.add(actions, BorderLayout.SOUTH);
        dialog.setContentPane(root); dialog.setSize(790, 560); dialog.setMinimumSize(new Dimension(680, 480));
        dialog.setLocationRelativeTo(parent); dialog.setVisible(true);
    }

    private static JLabel chip(String text, Color color) {
        JLabel label = new JLabel(text); label.setOpaque(true); label.setForeground(color);
        label.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 32));
        label.setFont(Theme.get().smallFont.deriveFont(Font.BOLD));
        label.setBorder(BorderFactory.createEmptyBorder(6, 9, 6, 9)); return label;
    }

    private static void showReview(Component parent, List<QuizAttempt> attempts) {
        Theme theme = Theme.get(); JPanel list = new JPanel(); list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        for (int i = 0; i < attempts.size(); i++) {
            QuizAttempt attempt = attempts.get(i);
            HoverCard card = new HoverCard(new BorderLayout(0, 6));
            card.setBorder(BorderFactory.createEmptyBorder(12, 14, 14, 14));
            String status = attempt.correct ? "✓ Correct" : "↻ Review this";
            card.add(Ui.label("Question " + (i + 1) + " · " + status, theme.bodyFont.deriveFont(Font.BOLD), attempt.correct ? theme.success : theme.danger), BorderLayout.NORTH);
            JPanel detail = new JPanel(); detail.setOpaque(false); detail.setLayout(new BoxLayout(detail, BoxLayout.Y_AXIS));
            detail.add(Ui.label(attempt.subject + " · " + attempt.topic, theme.smallFont, theme.muted));
            detail.add(Ui.label(wrap(attempt.prompt), theme.bodyFont, theme.text));
            detail.add(Ui.label(wrap("Your answer: " + attempt.learnerAnswer + "   ·   Correct: " + attempt.correctAnswer), theme.smallFont, theme.muted));
            detail.add(Ui.label(wrap(attempt.explanation), theme.smallFont, theme.text));
            card.add(detail, BorderLayout.CENTER); list.add(card); list.add(Box.createVerticalStrut(8));
        }
        JScrollPane scroll = Ui.scroll(list); scroll.setPreferredSize(new Dimension(680, 410));
        JOptionPane.showMessageDialog(parent, scroll, "Review answers", JOptionPane.PLAIN_MESSAGE);
    }

    private static String duration(int seconds) { return String.format("%d:%02d", seconds / 60, seconds % 60); }
    private static String wrap(String text) {
        return "<html><body style='width:590px'>" + text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") + "</body></html>";
    }
}

// ======================================================================
// QuizScreen
// ======================================================================
final class QuizScreen {
    private static final Logger LOG = Logger.getLogger(QuizScreen.class.getName());
    private QuizScreen() {
    }

    public static JPanel build(AppContext ctx) {
        return new QuizPanel(ctx);
    }

    public static JPanel buildDailyPractice(AppContext ctx) {
        return new QuizPanel(ctx, true);
    }

    public static void openMaterialUpload(AppContext ctx) {
        new QuizPanel(ctx).importMaterial();
    }

    private static class QuizPanel extends JPanel {
        final AppContext ctx;
        List<QuizQuestion> items;
        int index;
        int correct;
        int timedOutCount;
        final Set<String> answeredTopics = new LinkedHashSet<>();
        final Set<String> missedTopics = new LinkedHashSet<>();
        final Map<String,int[]> roundScores = new HashMap<>();
        boolean dailyChallenge;
        boolean dailyMode;
        int totalSecondsUsed;
        final List<QuizAttempt> roundAttempts = new ArrayList<>();
        boolean questionTimedOut;
        int remaining;
        long questionStarted;
        Timer countdown;
        JLabel prompt = new JLabel();
        JLabel clock = new JLabel();
        JRadioButton[] choices = new JRadioButton[4];
        ButtonGroup group = new ButtonGroup();
        JLabel feedback = new JLabel(" ");
        AnimatedBar bar = new AnimatedBar();
        AccentButton submit = AccentButton.primary("Lock in answer");
        JPanel setup;
        JPanel play;
        JPanel screenCards;
        JPanel quizCatalog;
        JComboBox<String> subjectBox;
        JSpinner questionCount;
        String catalogSubject = "All subjects";
        String catalogMode = "All";
        String catalogDifficulty = "All";

        QuizPanel(AppContext ctx) {
            this(ctx, false);
        }

        QuizPanel(AppContext ctx, boolean dailyMode) {
            super(new BorderLayout(0, 12));
            this.ctx = ctx;
            this.dailyMode = dailyMode;
            setOpaque(false);
            Theme theme = Theme.get();
            JPanel page = Ui.page(dailyMode ? "Daily Practice" : "Quiz Center",
                    ctx.quiz.bankSize() + " questions loaded from data/questions.csv  •  instant feedback, timed");
            add(page, BorderLayout.NORTH);

            setup = new JPanel(new BorderLayout(0, 12));
            setup.setOpaque(false);
            JPanel controls = new JPanel(new GridLayout(0, 2, 9, 9));
            controls.setOpaque(false);
            List<String> subjects = ctx.quiz.subjects();
            subjects.add(0, "All subjects");
            subjectBox = new JComboBox<>(subjects.toArray(new String[0]));
            JSpinner questionCount = this.questionCount = new JSpinner(new SpinnerNumberModel(5, 1, 50, 1));
            questionCount.setFont(theme.bodyFont);
            controls.add(Ui.label("Subject", theme.bodyFont, theme.text));
            controls.add(subjectBox);
            controls.add(Ui.label("Questions per round", theme.bodyFont, theme.text));
            controls.add(questionCount);
            AccentButton upload = AccentButton.ghost("＋ Upload notes & create quiz");
            controls.add(upload);
            AccentButton generateMaterial = AccentButton.primary("Generate from loaded material");
            controls.add(generateMaterial);
            AccentButton start = AccentButton.primary("Start quiz");
            controls.add(start);
            AccentButton missed = AccentButton.ghost("Practice missed topics");
            controls.add(missed);
            AccentButton weak = AccentButton.ghost("Practice weak topics");
            controls.add(weak);
            AccentButton bank = AccentButton.ghost("View question bank");
            controls.add(bank);
            AccentButton challenge = AccentButton.primary("Daily challenge · 5 weakest-topic questions");
            controls.add(challenge);
            AccentButton mock = AccentButton.ghost("Start timed mock test");
            controls.add(mock);
            AccentButton notebook = AccentButton.ghost("Mistake notebook");
            controls.add(notebook);
            AccentButton retryMistakes = AccentButton.ghost("Retry missed questions");
            controls.add(retryMistakes);
            AccentButton flashcards = AccentButton.ghost("Review flashcards");
            controls.add(flashcards);
            AccentButton history = AccentButton.ghost("Quiz attempt history");
            controls.add(history);
            HoverCard quickSetup = new HoverCard(new BorderLayout(10, 10));
            quickSetup.add(Ui.label("Build a practice round", theme.headingFont, theme.text), BorderLayout.NORTH);
            quickSetup.add(controls, BorderLayout.CENTER);
            setup.add(quickSetup, BorderLayout.NORTH);
            JPanel browse = new JPanel(new BorderLayout(0, 8)); browse.setOpaque(false);
            JPanel browseTitle = new JPanel(new BorderLayout()); browseTitle.setOpaque(false);
            browseTitle.add(Ui.label("Practice library", theme.headingFont, theme.text), BorderLayout.WEST);
            browseTitle.add(filterChips(subjects), BorderLayout.EAST);
            browse.add(browseTitle, BorderLayout.NORTH);
            quizCatalog = new JPanel(new GridLayout(0, 2, 10, 10)); quizCatalog.setOpaque(false);
            refreshCatalog();
            browse.add(Ui.scroll(quizCatalog), BorderLayout.CENTER);
            JPanel libraryWrap = new JPanel(new BorderLayout()); libraryWrap.setOpaque(false);
            libraryWrap.add(browse, BorderLayout.CENTER);
            libraryWrap.add(recentAttemptCard(), BorderLayout.SOUTH);
            setup.add(libraryWrap, BorderLayout.CENTER);

            play = new JPanel(new BorderLayout(0, 10));
            play.setOpaque(false);
            prompt.setFont(theme.headingFont);
            prompt.setForeground(theme.text);
            clock.setFont(theme.titleFont);
            clock.setForeground(theme.accent);
            JPanel north = new JPanel(new BorderLayout());
            north.setOpaque(false);
            north.add(prompt, BorderLayout.CENTER);
            north.add(clock, BorderLayout.EAST);
            JPanel opts = new JPanel(new GridLayout(0, 1, 6, 6));
            opts.setOpaque(false);
            for (int i = 0; i < 4; i++) {
                choices[i] = new JRadioButton();
                choices[i].setOpaque(true);
                choices[i].setBackground(theme.card);
                choices[i].setForeground(theme.text);
                choices[i].setFont(theme.bodyFont);
                choices[i].setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(theme.cardAlt, 1, true),
                        BorderFactory.createEmptyBorder(12, 14, 12, 14)));
                choices[i].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                JRadioButton choice = choices[i];
                choices[i].addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { if (choice.isEnabled()) choice.setBackground(theme.accentSoft); }
                    @Override public void mouseExited(MouseEvent e) { if (choice.isEnabled()) choice.setBackground(theme.card); }
                });
                choices[i].setToolTipText("Select this answer");
                group.add(choices[i]);
                opts.add(choices[i]);
            }
            HoverCard questionCard = new HoverCard(new BorderLayout(0, 16));
            questionCard.add(north, BorderLayout.NORTH);
            questionCard.add(opts, BorderLayout.CENTER);
            play.add(questionCard, BorderLayout.CENTER);
            JPanel south = new JPanel(new BorderLayout(0, 8));
            south.setOpaque(false);
            feedback.setFont(theme.bodyFont);
            feedback.setForeground(theme.muted);
            south.add(feedback, BorderLayout.NORTH);
            south.add(bar, BorderLayout.CENTER);
            south.add(submit, BorderLayout.EAST);
            play.add(south, BorderLayout.SOUTH);
            play.setVisible(false);

            start.addActionListener(e -> {
                String selected = (String) subjectBox.getSelectedItem();
                String filter = "All subjects".equals(selected) ? null : selected;
                begin(ctx.quiz.startQuiz(filter, (int) questionCount.getValue()), setup, play);
            });
            missed.addActionListener(e -> begin(ctx.quiz.startReviewQuiz(ctx.data,
                    (int) questionCount.getValue(), false), setup, play));
            weak.addActionListener(e -> begin(ctx.quiz.startReviewQuiz(ctx.data,
                    (int) questionCount.getValue(), true), setup, play));
            bank.addActionListener(e -> JOptionPane.showMessageDialog(this, ctx.quiz.bankSummary(),
                    "Your question bank", JOptionPane.INFORMATION_MESSAGE));
            challenge.addActionListener(e -> {
                if(LocalDate.now().equals(ctx.data.profile.dailyChallengeDate)){
                    JOptionPane.showMessageDialog(this,"You completed today's challenge already. Come back tomorrow for a fresh weakest-topic set.");
                    return;
                }
                if (ctx.data.knowledge.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Take a quiz first. Your daily challenge is built from your lowest-mastery topics.");
                    return;
                }
                List<QuizQuestion> daily = ctx.quiz.startDailyChallenge(ctx.data);
                if (daily.size() < 5) {
                    JOptionPane.showMessageDialog(this, "This challenge needs five distinct questions from your weakest topics. Upload more study notes or add quiz questions for those topics.");
                    return;
                }
                dailyChallenge = true;
                begin(daily, setup, play);
            });
            mock.addActionListener(e -> MockTestDialog.show(ctx));
            notebook.addActionListener(e -> showMistakes());
            retryMistakes.addActionListener(e -> begin(ctx.quiz.startMistakeQuiz(ctx.data, (int) questionCount.getValue()), setup, play));
            flashcards.addActionListener(e -> FlashcardReviewDialog.show(ctx));
            history.addActionListener(e -> showHistory());
            upload.addActionListener(e -> ctx.applet.showScreen("Upload Material"));
            generateMaterial.addActionListener(e -> generateLoadedMaterialQuiz());
            submit.addActionListener(e -> lockIn());

            JPanel body = new JPanel(new BorderLayout());
            body.setOpaque(false);
            screenCards = new JPanel(new CardLayout()); screenCards.setOpaque(false);
            screenCards.add(setup, "setup"); screenCards.add(play, "play");
            body.add(screenCards, BorderLayout.CENTER);
            add(body, BorderLayout.CENTER);
            if (dailyMode) {
                begin(ctx.quiz.startQuiz(null, 10), setup, play);
            }
        }

        void begin(List<QuizQuestion> chosen, JPanel setup, JPanel play) {
            if (chosen == null || chosen.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "No matching questions are available. Try another topic or upload more study material.",
                        "No questions available", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            LOG.fine("Quiz UI updated with " + chosen.size() + " questions; opening question 1.");
            items = chosen;
            index = 0;
            correct = 0;
            timedOutCount = 0;
            totalSecondsUsed = 0;
            roundAttempts.clear();
            answeredTopics.clear();
            missedTopics.clear();
            roundScores.clear();
            // Let CardLayout alone manage child visibility. Marking both cards visible
            // first makes CardLayout.show() treat the target as already selected.
            ((CardLayout) screenCards.getLayout()).show(screenCards, "play");
            showQuestion();
            screenCards.revalidate();
            screenCards.repaint();
            revalidate();
            repaint();
        }

        private JPanel filterChips(List<String> subjects) {
            JPanel chips = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0)); chips.setOpaque(false);
            AccentButton all = AccentButton.ghost("All");
            AccentButton subject = AccentButton.ghost("Subject ▾");
            AccentButton difficulty = AccentButton.ghost("Difficulty ▾");
            AccentButton attempted = AccentButton.ghost("Attempted");
            AccentButton fresh = AccentButton.ghost("New");
            for (AccentButton chip : new AccentButton[]{all, subject, difficulty, attempted, fresh})
                chip.setPreferredSize(new Dimension(Math.max(62, chip.getPreferredSize().width), 31));
            all.addActionListener(e -> { catalogMode = "All"; catalogSubject = "All subjects"; catalogDifficulty = "All"; refreshCatalog(); });
            subject.addActionListener(e -> {
                JPopupMenu menu = new JPopupMenu();
                for (String name : subjects) {
                    JMenuItem item = new JMenuItem(name);
                    item.addActionListener(a -> { catalogMode = "Subject"; catalogSubject = name; refreshCatalog(); });
                    menu.add(item);
                }
                menu.show(subject, 0, subject.getHeight());
            });
            difficulty.addActionListener(e -> {
                JPopupMenu menu = new JPopupMenu();
                for (String level : new String[]{"All levels", "Easy", "Medium", "Hard"}) {
                    JMenuItem item = new JMenuItem(level);
                    item.addActionListener(a -> { catalogDifficulty = "All levels".equals(level) ? "All" : level; catalogMode = "All"; refreshCatalog(); });
                    menu.add(item);
                }
                menu.show(difficulty, 0, difficulty.getHeight());
            });
            attempted.addActionListener(e -> { catalogMode = "Attempted"; catalogSubject = "All subjects"; refreshCatalog(); });
            fresh.addActionListener(e -> { catalogMode = "New"; catalogSubject = "All subjects"; refreshCatalog(); });
            chips.add(all); chips.add(subject); chips.add(difficulty); chips.add(attempted); chips.add(fresh);
            return chips;
        }

        private void refreshCatalog() {
            if (quizCatalog == null) return;
            quizCatalog.removeAll();
            Theme theme = Theme.get();
            List<String> names = new ArrayList<>();
            names.add("All subjects"); names.addAll(ctx.quiz.subjects());
            Set<String> attemptedSubjects = new LinkedHashSet<>();
            for (QuizAttempt attempt : ctx.data.quizAttempts) attemptedSubjects.add(attempt.subject);
            int shown = 0;
            for (String name : names) {
                if (("Attempted".equals(catalogMode) || "New".equals(catalogMode)) && "All subjects".equals(name)) continue;
                if (!"All subjects".equals(name) && !catalogSubject.equals("All subjects") && !catalogSubject.equals(name)) continue;
                if ("Attempted".equals(catalogMode) && !attemptedSubjects.contains(name)) continue;
                if ("New".equals(catalogMode) && attemptedSubjects.contains(name)) continue;
                List<QuizQuestion> questions = availableQuestions("All subjects".equals(name) ? null : name);
                if (!"All".equals(catalogDifficulty)) questions.removeIf(q -> !difficultyOf(q).equals(catalogDifficulty));
                if (questions.isEmpty()) continue;
                String level = mostCommonDifficulty(questions);
                quizCatalog.add(quizCard(name, questions.size(), level)); shown++;
            }
            if (shown == 0) {
                HoverCard empty = new HoverCard(new BorderLayout(0, 8));
                empty.add(Ui.label(ctx.quiz.bankSize() == 0 ? "No quiz questions yet" : "No quizzes match these filters",
                        theme.headingFont, theme.text), BorderLayout.NORTH);
                empty.add(Ui.label(ctx.quiz.bankSize() == 0 ? "Upload study notes to create your first practice set." : "Try another subject, difficulty or status filter.",
                        theme.bodyFont, theme.muted), BorderLayout.CENTER);
                AccentButton open = AccentButton.primary(ctx.quiz.bankSize() == 0 ? "Upload material" : "Show all quizzes");
                open.addActionListener(e -> { if (ctx.quiz.bankSize() == 0) ctx.applet.showScreen("Upload Material"); else { catalogMode = "All"; catalogSubject = "All subjects"; catalogDifficulty = "All"; refreshCatalog(); } });
                empty.add(open, BorderLayout.SOUTH); quizCatalog.add(empty);
            }
            quizCatalog.revalidate(); quizCatalog.repaint();
        }

        private HoverCard quizCard(String subject, int count, String level) {
            Theme theme = Theme.get(); HoverCard card = new HoverCard(new BorderLayout(8, 8));
            JPanel head = new JPanel(new BorderLayout(8, 0)); head.setOpaque(false);
            JLabel icon = new JLabel("✦", SwingConstants.CENTER); icon.setOpaque(true); icon.setForeground(theme.accent); icon.setBackground(theme.accentSoft); icon.setPreferredSize(new Dimension(38, 38));
            head.add(icon, BorderLayout.WEST);
            Color levelColor = "Easy".equals(level) ? theme.success : "Hard".equals(level) ? theme.danger : new Color(208, 139, 55);
            JLabel badge = new JLabel(level.toUpperCase(), SwingConstants.CENTER); badge.setOpaque(true); badge.setForeground(levelColor);
            badge.setBackground(new Color(levelColor.getRed(), levelColor.getGreen(), levelColor.getBlue(), 34));
            badge.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            head.add(badge, BorderLayout.EAST); card.add(head, BorderLayout.NORTH);
            card.add(Ui.label("All subjects".equals(subject) ? "Mixed practice" : subject, theme.headingFont, theme.text), BorderLayout.CENTER);
            JPanel bottom = new JPanel(new BorderLayout(8, 0)); bottom.setOpaque(false);
            bottom.add(Ui.label(count + " questions · " + level + " level", theme.smallFont, theme.muted), BorderLayout.CENTER);
            AccentButton go = AccentButton.primary("Start Quiz"); go.setFont(theme.smallFont.deriveFont(Font.BOLD));
            go.addActionListener(e -> {
                List<QuizQuestion> pool = availableQuestions("All subjects".equals(subject) ? null : subject);
                if (!"All".equals(catalogDifficulty)) pool.removeIf(q -> !difficultyOf(q).equals(catalogDifficulty));
                int limit = Math.max(1, (Integer) questionCount.getValue());
                if (pool.size() > limit) pool = new ArrayList<>(pool.subList(0, limit));
                begin(pool, setup, play);
            });
            bottom.add(go, BorderLayout.EAST); card.add(bottom, BorderLayout.SOUTH);
            card.setPreferredSize(new Dimension(270, 142));
            return card;
        }

        private List<QuizQuestion> availableQuestions(String subject) {
            return ctx.quiz.startQuiz(subject, Math.max(1, ctx.quiz.bankSize()));
        }

        /** The question file has no difficulty field, so this is a visible text-length estimate. */
        private String difficultyOf(QuizQuestion question) {
            int size = question.prompt.length() + question.explanation.length() / 2;
            return size < 100 ? "Easy" : size < 190 ? "Medium" : "Hard";
        }

        private String mostCommonDifficulty(List<QuizQuestion> questions) {
            int easy = 0, medium = 0, hard = 0;
            for (QuizQuestion question : questions) {
                String level = difficultyOf(question);
                if ("Easy".equals(level)) easy++; else if ("Medium".equals(level)) medium++; else hard++;
            }
            if (easy >= medium && easy >= hard) return "Easy";
            return medium >= hard ? "Medium" : "Hard";
        }

        private JPanel recentAttemptCard() {
            Theme theme = Theme.get(); HoverCard card = new HoverCard(new BorderLayout(8, 4));
            card.setBorder(BorderFactory.createEmptyBorder(12, 15, 13, 15));
            card.add(Ui.label("Recent attempts", theme.bodyFont.deriveFont(Font.BOLD), theme.text), BorderLayout.NORTH);
            JPanel rows = new JPanel(new GridLayout(1, 3, 10, 0)); rows.setOpaque(false);
            Set<String> seen = new LinkedHashSet<>(); int count = 0;
            for (int i = ctx.data.quizAttempts.size() - 1; i >= 0 && count < 3; i--) {
                QuizAttempt a = ctx.data.quizAttempts.get(i);
                String id = a.quizSessionId == null || a.quizSessionId.isBlank() ? "legacy:" + (a.when == null ? i : a.when.toLocalDate()) : a.quizSessionId;
                if (!seen.add(id)) continue;
                int total = a.quizSessionSize > 0 ? a.quizSessionSize : 1;
                int score = a.quizSessionSize > 0 ? a.quizSessionScore : (a.correct ? 1 : 0);
                String date = a.when == null ? "Saved quiz" : a.when.toLocalDate().toString();
                rows.add(Ui.label("✎  " + date + " · " + score + "/" + total, theme.smallFont, theme.muted)); count++;
            }
            if (count == 0) rows.add(Ui.label("Your completed quiz rounds will appear here.", theme.smallFont, theme.muted));
            card.add(rows, BorderLayout.CENTER);
            return card;
        }

        void importMaterial() {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Choose study notes (.txt or .md)");
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Study notes (TXT, Markdown)", "txt", "md", "markdown"));
            if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            try {
                Path path = chooser.getSelectedFile().toPath();
                String lower = path.getFileName().toString().toLowerCase();
                if (!lower.endsWith(".txt") && !lower.endsWith(".md") && !lower.endsWith(".markdown")) {
                    JOptionPane.showMessageDialog(this, "Choose a plain text or Markdown file.");
                    return;
                }
                String material = Files.readString(path, StandardCharsets.UTF_8);
                if (material.length() > 1_000_000) {
                    JOptionPane.showMessageDialog(this, "This file is over 1 MB. Please choose a shorter set of notes.");
                    return;
                }
                JTextField subject = new JTextField("My Notes");
                JTextField topic = new JTextField(path.getFileName().toString().replaceFirst("(?i)\\.(txt|md|markdown)$", ""));
                JPanel details = new JPanel(new GridLayout(0, 2, 8, 8));
                details.add(new JLabel("Quiz subject")); details.add(subject);
                details.add(new JLabel("Topic")); details.add(topic);
                if (JOptionPane.showConfirmDialog(this, details, "Label your material", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
                if (subject.getText().isBlank() || topic.getText().isBlank()) {
                    JOptionPane.showMessageDialog(this, "Enter a subject and topic name.");
                    return;
                }
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Create up to 50 practice questions from these notes and start the quiz now? Questions are saved to your local quiz bank.",
                        "Create and start quiz", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (confirm != JOptionPane.OK_OPTION) return;
                generateFromMaterial(material, subject.getText().trim(), "All topics", topic.getText().trim(), 50, "Medium");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Could not read the notes or update the quiz bank: " + ex.getMessage(),
                        "Material import failed", JOptionPane.ERROR_MESSAGE);
            }
        }

        void showQuestion() {
            QuizQuestion q = items.get(index);
            prompt.setText("<html>Q" + (index + 1) + "/" + items.size() + "  ·  "
                    + html(q.subject) + " / " + html(q.topic) + "<br>" + html(q.prompt) + "</html>");
            group.clearSelection();
            for (int i = 0; i < 4; i++) {
                choices[i].setText((char) ('A' + i) + "  " + q.choices[i]);
                choices[i].setEnabled(true);
                choices[i].setForeground(Theme.get().text);
                choices[i].setBackground(Theme.get().card);
                choices[i].setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Theme.get().cardAlt, 1, true),
                        BorderFactory.createEmptyBorder(12, 14, 12, 14)));
            }
            feedback.setText("Pick an answer before the timer hits zero.");
            feedback.setForeground(Theme.get().muted);
            submit.setEnabled(true);
            remaining = ctx.data.profile.quizSeconds;
            questionTimedOut = false;
            questionStarted = System.currentTimeMillis();
            clock.setText(remaining + "s");
            bar.setProgress(index / (float) items.size(), "Question " + (index + 1));
            screenCards.revalidate();
            screenCards.repaint();
            if (countdown != null) {
                countdown.stop();
            }
            countdown = new Timer(1000, e -> {
                remaining--;
                clock.setText(Math.max(0, remaining) + "s");
                if (remaining <= 0) {
                    ((Timer) e.getSource()).stop();
                    questionTimedOut = true;
                    lockIn();
                }
            });
            countdown.start();
        }

        void lockIn() {
            if (items == null || index >= items.size() || !submit.isEnabled()) {
                return;
            }
            if (countdown != null) {
                countdown.stop();
            }
            QuizQuestion q = items.get(index);
            int chosen = -1;
            for (int i = 0; i < 4; i++) {
                if (choices[i].isSelected()) {
                    chosen = i;
                }
                choices[i].setEnabled(false);
            }
            int seconds = (int) Math.max(0, (System.currentTimeMillis() - questionStarted) / 1000);
            ctx.quiz.record(ctx.data, q, chosen, seconds, questionTimedOut);
            roundAttempts.add(ctx.data.quizAttempts.get(ctx.data.quizAttempts.size() - 1));
            totalSecondsUsed += seconds;
            if (questionTimedOut) timedOutCount++;
            // Persist every answer immediately, including an option selected when the clock expires.
            ctx.saveQuietly();
            boolean ok = chosen == q.correctIndex;
            answeredTopics.add(q.subject + " / " + q.topic);
            if (!ok) missedTopics.add(q.subject + " / " + q.topic);
            int[] topicScore=roundScores.computeIfAbsent(q.subject+"\n"+q.topic,k->new int[2]);
            topicScore[1]++;if(ok)topicScore[0]++;
            if (ok) {
                correct++;
            }
            Theme theme = Theme.get();
            for (int i = 0; i < 4; i++) {
                if (i == q.correctIndex) {
                    choices[i].setBackground(new Color(theme.success.getRed(), theme.success.getGreen(), theme.success.getBlue(), 55));
                    choices[i].setForeground(theme.success);
                } else if (i == chosen && !ok) {
                    choices[i].setBackground(new Color(theme.danger.getRed(), theme.danger.getGreen(), theme.danger.getBlue(), 48));
                    choices[i].setForeground(theme.danger);
                } else choices[i].setBackground(theme.card);
            }
            feedback.setForeground(ok ? theme.success : theme.danger);
            feedback.setText("<html>" + (questionTimedOut
                    ? (chosen >= 0 ? "Time is up — your selected answer was saved. " : "Time is up — this question was saved as skipped. ")
                    : "") + (ok ? "Correct. " : "Not quite. Answer is "
                    + (char) ('A' + q.correctIndex) + ". ")
                    + q.explanation + "</html>");
            submit.setEnabled(false);
            Timer pause = new Timer(ok ? 900 : 1600, e -> {
                ((Timer) e.getSource()).stop();
                index++;
                if (index >= items.size()) {
                    finish();
                } else {
                    showQuestion();
                }
            });
            pause.setRepeats(false);
            pause.start();
        }

        void finish() {
            bar.setProgress(1f, "Done");
            String sessionId = UUID.randomUUID().toString();
            int wrong = 0;
            for (QuizAttempt attempt : roundAttempts) {
                attempt.quizSessionId = sessionId;
                attempt.quizSessionSize = items.size();
                attempt.quizSessionScore = correct;
                attempt.quizSessionSeconds = totalSecondsUsed;
                if (!attempt.correct && !"Skipped".equals(attempt.learnerAnswer)) wrong++;
            }
            for(Map.Entry<String,int[]> entry:roundScores.entrySet()){
                String[] key=entry.getKey().split("\n",2);int[] score=entry.getValue();
                ctx.knowledge.recordQuiz(ctx.data,key[0],key[1],score[0]/(double)Math.max(1,score[1]),LocalDate.now());
            }
            GamificationService.Award award = new GamificationService().awardQuiz(ctx, correct, items.size());
            if (dailyChallenge) ctx.data.profile.dailyChallengeDate = LocalDate.now();
            if (dailyMode) ctx.data.profile.dailyPracticeDate = LocalDate.now();
            rebalancePlan(ctx);
            StringBuilder why = new StringBuilder("Why did the plan change?\n\n");
            if (missedTopics.isEmpty()) {
                why.append("You recalled the topics in this round, so their next SM-2 reviews are spaced farther apart. New due dates were added to your planner.");
            } else {
                why.append("A missed answer lowers that topic's mastery and brings its next review forward. The planner rebuilt upcoming generated blocks using your updated quiz results.\n\nReview soon: ");
                why.append(String.join(", ", missedTopics)).append('.');
            }
            ctx.saveQuietly();
            QuizResultDialog.show(this, correct, wrong, items.size(), totalSecondsUsed, award.gained,
                    timedOutCount, roundAttempts, why.toString(), award.newBadges);
            if (award.newLevel > award.oldLevel) LevelUpDialog.show(ctx.applet, award);
            ctx.applet.showScreen("Progress");
        }

        void generateLoadedMaterialQuiz() {
            if (ctx.data.activeMaterialText == null || ctx.data.activeMaterialText.isBlank()) {
                JOptionPane.showMessageDialog(this, "Upload and analyze study material first.", "No material loaded", JOptionPane.INFORMATION_MESSAGE);
                ctx.applet.showScreen("Upload Material");
                return;
            }
            List<String> subjects = ctx.quiz.subjects();
            if (!subjects.contains("Uploaded Material")) subjects.add("Uploaded Material");
            JComboBox<String> subject = new JComboBox<>(subjects.toArray(new String[0]));
            subject.setSelectedItem("Uploaded Material");
            List<String> topics = new ArrayList<>();
            topics.add("All topics");
            for (String term : MaterialQuizGenerator.suggestTopics(ctx.data.activeMaterialText)) if (!topics.contains(term)) topics.add(term);
            JComboBox<String> topic = new JComboBox<>(topics.toArray(new String[0]));
            JSpinner count = new JSpinner(new SpinnerNumberModel(10, 1, 50, 1));
            JComboBox<String> difficulty = new JComboBox<>(new String[]{"Easy", "Medium", "Hard"});
            JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
            form.add(new JLabel("Subject")); form.add(subject);
            form.add(new JLabel("Topic focus")); form.add(topic);
            form.add(new JLabel("Number of questions")); form.add(count);
            form.add(new JLabel("Difficulty")); form.add(difficulty);
            if (JOptionPane.showConfirmDialog(this, form, "Build a quiz from your material", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

            String chosenTopic = String.valueOf(topic.getSelectedItem());
            String labelTopic = "All topics".equals(chosenTopic) ? ctx.data.activeMaterialName : chosenTopic;
            generateFromMaterial(ctx.data.activeMaterialText, String.valueOf(subject.getSelectedItem()), chosenTopic,
                    labelTopic, (Integer) count.getValue(), String.valueOf(difficulty.getSelectedItem()));
        }

        private void generateFromMaterial(String material, String subject, String topic, String topicLabel,
                                          int count, String difficulty) {
            Window owner = SwingUtilities.getWindowAncestor(this);
            JDialog loading = new JDialog(owner, "Generating quiz", Dialog.ModalityType.APPLICATION_MODAL);
            JPanel content = new JPanel(new BorderLayout(10, 10));
            content.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
            content.add(new JLabel("Creating and checking your questions…"), BorderLayout.NORTH);
            JProgressBar progress = new JProgressBar(); progress.setIndeterminate(true);
            content.add(progress, BorderLayout.CENTER);
            loading.setContentPane(content); loading.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            loading.pack(); loading.setLocationRelativeTo(this);

            SwingWorker<List<QuizQuestion>, Void> worker = new SwingWorker<>() {
                @Override protected List<QuizQuestion> doInBackground() {
                    LOG.fine("Material quiz generation started: chars=" + material.length() + ", count=" + count
                            + ", topic=" + topic + ", difficulty=" + difficulty);
                    List<QuizQuestion> generated = MaterialQuizGenerator.generate(material, subject, topic, topicLabel, count, difficulty);
                    LOG.fine("Local generation completed; valid candidates=" + generated.size());
                    return generated;
                }

                @Override protected void done() {
                    loading.dispose();
                    try {
                        List<QuizQuestion> generated = get();
                        generated.removeIf(question -> !MaterialQuizGenerator.isValidQuestion(question));
                        if (generated.isEmpty()) {
                            showGenerationError("Could not generate questions. Please try another topic or add more explanatory material.",
                                    () -> generateFromMaterial(material, subject, topic, topicLabel, count, difficulty));
                            return;
                        }
                        new QuizBankWriter().append(Paths.get("data", "questions.csv"), generated);
                        for (QuizQuestion question : generated) QuizService.addFlashcard(ctx.data, question);
                        ctx.quiz.reload();
                        ctx.saveQuietly();
                        LOG.fine("Saved and reloaded " + generated.size() + " questions; navigating to question 1.");
                        begin(generated, setup, play);
                    } catch (Exception ex) {
                        Throwable cause = ex;
                        while (cause.getCause() != null) cause = cause.getCause();
                        LOG.log(Level.WARNING, "Quiz generation or storage failed", cause);
                        String detail = cause.getMessage();
                        showGenerationError("Could not generate questions. Please try again."
                                + (detail == null || detail.isBlank() ? "" : "\n\n" + detail),
                                () -> generateFromMaterial(material, subject, topic, topicLabel, count, difficulty));
                    }
                }
            };
            loading.addWindowListener(new WindowAdapter() {
                @Override public void windowOpened(WindowEvent event) { worker.execute(); }
            });
            loading.setVisible(true);
        }

        private void showGenerationError(String message, Runnable retry) {
            if (retry == null) {
                JOptionPane.showMessageDialog(this, message, "Quiz could not be started", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Object[] options = {"Retry", "Close"};
            int choice = JOptionPane.showOptionDialog(this, message, "Quiz generation failed",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);
            if (choice == 0) retry.run();
        }

        private static String html(String text) {
            return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }

        void showHistory() {
            StringBuilder text = new StringBuilder("RECENT QUIZ SESSIONS\n\n");
            java.util.Set<String> seen = new java.util.HashSet<>();
            int shown = 0, right = 0, answers = 0;
            for (int i = ctx.data.quizAttempts.size() - 1; i >= 0 && shown < 20; i--) {
                QuizAttempt attempt = ctx.data.quizAttempts.get(i);
                if (attempt.quizSessionId != null && !attempt.quizSessionId.isBlank()) {
                    if (!seen.add(attempt.quizSessionId)) continue;
                    shown++;
                    right += attempt.quizSessionScore;
                    answers += attempt.quizSessionSize;
                    text.append(attempt.when == null ? "Saved result" : attempt.when.toLocalDate()).append("  ·  ")
                            .append(attempt.quizSessionScore).append(" / ").append(attempt.quizSessionSize).append(" correct  ·  ")
                            .append(formatDuration(attempt.quizSessionSeconds)).append("\n");
                } else {
                    shown++; answers++;
                    if (attempt.correct) right++;
                    text.append(attempt.when == null ? "Saved result" : attempt.when.toLocalDate()).append("  ·  ")
                            .append(attempt.subject).append(" / ").append(attempt.topic).append("  ·  ")
                            .append(attempt.correct ? "Correct" : "Review");
                    if (attempt.timedOut) text.append("  ·  Timed out and saved");
                    text.append('\n');
                }
            }
            if (shown == 0) text.append("Complete a quiz and your saved attempts will appear here.");
            else text.insert("RECENT QUIZ SESSIONS\n\n".length(), "Recent accuracy: " + right + " / " + answers + " answers\n\n");
            JTextArea area = new JTextArea(text.toString(), 18, 62);
            area.setEditable(false); area.setLineWrap(true); area.setWrapStyleWord(true);
            JOptionPane.showMessageDialog(this, new javax.swing.JScrollPane(area), "Quiz attempt history", JOptionPane.INFORMATION_MESSAGE);
        }

        private String formatDuration(int seconds) {
            return String.format("%d min %02d sec", seconds / 60, seconds % 60);
        }

        void showMistakes() {
            StringBuilder text = new StringBuilder("MISTAKE NOTEBOOK\n\n");
            int count=0;
            for (int i=ctx.data.quizAttempts.size()-1;i>=0;i--) {
                QuizAttempt a=ctx.data.quizAttempts.get(i);
                if(a.correct)continue;
                count++;
                text.append(a.subject).append(" / ").append(a.topic).append("\n")
                        .append(a.prompt).append("\nYour answer: ").append(a.learnerAnswer)
                        .append(" | Correct: ").append(a.correctAnswer).append("\n")
                        .append(a.explanation).append("\n\n");
                if(count>=30)break;
            }
            if(count==0)text.append("No mistakes saved yet. Wrong quiz answers will appear here with their explanations.");
            JTextArea area=new JTextArea(text.toString(),20,65);area.setEditable(false);area.setLineWrap(true);area.setWrapStyleWord(true);
            JOptionPane.showMessageDialog(this,new javax.swing.JScrollPane(area),"Mistake notebook",JOptionPane.INFORMATION_MESSAGE);
        }
    }

    static void rebalancePlan(AppContext ctx) {
        LocalDate today=LocalDate.now();
        ctx.data.tasks.removeIf(task -> !task.done && !StudyTask.MANUAL.equals(task.kind) && !task.day.isBefore(today));
        ctx.data.tasks.addAll(ctx.planner.generateSchedule(ctx.data,today));
        ctx.saveQuietly();
    }
}

// ======================================================================
// RevisionNotesScreen
// ======================================================================
/** Notes, clickable flashcards, and due-topic revision queue. */
final class RevisionNotesScreen {
    private RevisionNotesScreen() { }

    public static JPanel build(AppContext ctx) {
        Theme theme = Theme.get();
        JPanel page = Ui.page("Revision", "A calmer way to revisit the ideas that matter most.");
        JPanel content = new JPanel(new BorderLayout(0, 14)); content.setOpaque(false);
        content.add(dueCard(ctx), BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(theme.bodyFont.deriveFont(Font.BOLD));
        tabs.setBackground(theme.bg); tabs.setForeground(theme.text);
        tabs.addTab("Notes", notesTab(ctx));
        tabs.addTab("Flashcards", flashcardsTab(ctx));
        tabs.addTab("Important Points", importantTab(ctx));
        content.add(tabs, BorderLayout.CENTER);
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    private static JPanel dueCard(AppContext ctx) {
        Theme theme = Theme.get();
        List<KnowledgeTopic> due = new ArrayList<>(ctx.knowledge.dueTopics(ctx.data, LocalDate.now()));
        HoverCard card = new HoverCard(new BorderLayout(12, 8));
        JPanel head = new JPanel(new BorderLayout(8, 0)); head.setOpaque(false);
        JLabel icon = badge("↻", theme.accent, theme.accentSoft, 38);
        head.add(icon, BorderLayout.WEST);
        JPanel titles = new JPanel(); titles.setOpaque(false); titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(Ui.label("Revise today", theme.headingFont, theme.text));
        titles.add(Ui.label(due.isEmpty() ? "You're up to date. Keep your learning rhythm steady." : due.size() + " topic(s) ready for a quick retrieval review.", theme.smallFont, theme.muted));
        head.add(titles, BorderLayout.CENTER);
        card.add(head, BorderLayout.NORTH);
        JPanel rows = new JPanel(new GridLayout(0, 2, 8, 8)); rows.setOpaque(false);
        if (due.isEmpty()) {
            rows.add(Ui.label("✓  No topics are due right now", theme.bodyFont, theme.success));
        } else {
            for (KnowledgeTopic topic : due) rows.add(dueTopic(ctx, topic));
        }
        card.add(rows, BorderLayout.CENTER);
        return card;
    }

    private static JPanel dueTopic(AppContext ctx, KnowledgeTopic topic) {
        Theme theme = Theme.get();
        JPanel row = new JPanel(new BorderLayout(8, 3)); row.setOpaque(false);
        JPanel text = new JPanel(); text.setOpaque(false); text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(Ui.label(topic.subject + " · " + topic.topic, theme.bodyFont.deriveFont(Font.BOLD), theme.text));
        text.add(Ui.label("" + topic.mastery + "% mastery  ·  " + (topic.nextReview == null ? "due" : "due " + topic.nextReview), theme.smallFont, theme.muted));
        row.add(text, BorderLayout.CENTER);
        AccentButton revised = AccentButton.ghost("Mark as Revised"); revised.setFont(theme.smallFont.deriveFont(Font.BOLD));
        revised.setToolTipText("Record that you reviewed this topic today and schedule its next pass");
        revised.addActionListener(e -> {
            LocalDate today = LocalDate.now();
            ctx.knowledge.recordStudy(ctx.data, topic.subject, topic.topic, today);
            topic.nextReview = today.plusDays(Math.max(1, topic.intervalDays));
            revised.setText("✓ Revised"); revised.setForeground(theme.success); revised.setEnabled(false);
            ctx.saveQuietly();
        });
        row.add(revised, BorderLayout.EAST);
        row.setBorder(BorderFactory.createEmptyBorder(3, 2, 3, 2));
        return row;
    }

    private static JPanel notesTab(AppContext ctx) {
        Theme theme = Theme.get();
        JPanel wrap = new JPanel(new BorderLayout(0, 10)); wrap.setOpaque(false); wrap.setBorder(BorderFactory.createEmptyBorder(12, 3, 3, 3));
        if (ctx.data.activeMaterialAnalysis != null && !ctx.data.activeMaterialAnalysis.isBlank()) {
            HoverCard note = new HoverCard(new BorderLayout(0, 10));
            note.add(Ui.label(ctx.data.activeMaterialName == null || ctx.data.activeMaterialName.isBlank()
                    ? "Material study brief" : ctx.data.activeMaterialName, theme.headingFont, theme.text), BorderLayout.NORTH);
            javax.swing.JTextArea brief = new javax.swing.JTextArea(ctx.data.activeMaterialAnalysis);
            brief.setEditable(false); brief.setLineWrap(true); brief.setWrapStyleWord(true); brief.setFont(theme.bodyFont);
            brief.setForeground(theme.text); brief.setBackground(theme.cardAlt); brief.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
            note.add(Ui.scroll(brief), BorderLayout.CENTER);
            AccentButton open = AccentButton.ghost("Manage study material"); open.addActionListener(e -> ctx.applet.showScreen("Upload Material"));
            note.add(open, BorderLayout.SOUTH); wrap.add(note, BorderLayout.CENTER);
        } else wrap.add(emptyTab("▤", "Your revision notes will live here", "Upload and analyze study material to create a concise study brief.", "Upload material", () -> ctx.applet.showScreen("Upload Material")), BorderLayout.CENTER);
        return wrap;
    }

    private static JPanel flashcardsTab(AppContext ctx) {
        Theme theme = Theme.get();
        JPanel wrap = new JPanel(new BorderLayout(0, 10)); wrap.setOpaque(false); wrap.setBorder(BorderFactory.createEmptyBorder(12, 3, 3, 3));
        JPanel top = new JPanel(new BorderLayout(8, 0)); top.setOpaque(false);
        top.add(Ui.label(ctx.data.flashcards.size() + " saved cards · click a card to flip it", theme.bodyFont, theme.muted), BorderLayout.WEST);
        AccentButton review = AccentButton.primary("Review due flashcards"); review.addActionListener(e -> FlashcardReviewDialog.show(ctx));
        top.add(review, BorderLayout.EAST); wrap.add(top, BorderLayout.NORTH);
        if (ctx.data.flashcards.isEmpty()) {
            wrap.add(emptyTab("▣", "No flashcards yet", "Cards are created from missed quiz answers and imported notes.", "Open Quiz Center", () -> ctx.applet.showScreen("Quiz Center")), BorderLayout.CENTER);
        } else {
            JPanel grid = new JPanel(new GridLayout(0, 2, 12, 12)); grid.setOpaque(false);
            for (Flashcard flashcard : ctx.data.flashcards) grid.add(new FlashcardTile(flashcard));
            wrap.add(Ui.scroll(grid), BorderLayout.CENTER);
        }
        return wrap;
    }

    private static JPanel importantTab(AppContext ctx) {
        Theme theme = Theme.get();
        JPanel wrap = new JPanel(new BorderLayout()); wrap.setOpaque(false); wrap.setBorder(BorderFactory.createEmptyBorder(12, 3, 3, 3));
        List<String> points = importantPoints(ctx);
        if (points.isEmpty()) {
            wrap.add(emptyTab("✧", "Important points appear after material analysis", "Study facts and key ideas are collected from your uploaded material.", "Upload material", () -> ctx.applet.showScreen("Upload Material")), BorderLayout.CENTER);
        } else {
            JPanel grid = new JPanel(new GridLayout(0, 2, 10, 10)); grid.setOpaque(false);
            int n = 1;
            for (String point : points) {
                HoverCard card = new HoverCard(new BorderLayout(8, 0));
                card.add(badge(String.format("%02d", n++), theme.accent, theme.accentSoft, 34), BorderLayout.WEST);
                card.add(Ui.label("<html><body style='width:250px'>" + escape(point) + "</body></html>", theme.bodyFont, theme.text), BorderLayout.CENTER);
                grid.add(card);
            }
            wrap.add(Ui.scroll(grid), BorderLayout.CENTER);
        }
        return wrap;
    }

    private static List<String> importantPoints(AppContext ctx) {
        List<String> points = new ArrayList<>();
        String brief = ctx.data.activeMaterialAnalysis == null ? "" : ctx.data.activeMaterialAnalysis;
        for (String line : brief.split("\\R")) {
            String clean = line.trim().replaceFirst("^[•*✓➤→\\-]+\\s*", "");
            if (clean.length() < 12 || clean.matches("(?i)^[A-Z][A-Z /&-]{2,}$")) continue;
            points.add(clean);
            if (points.size() == 12) break;
        }
        if (points.isEmpty()) for (KnowledgeTopic topic : ctx.data.knowledge.values()) {
            points.add(topic.subject + " · " + topic.topic + " — " + topic.mastery + "% mastery; next review " + topic.nextReview);
            if (points.size() == 12) break;
        }
        return points;
    }

    private static JPanel emptyTab(String icon, String title, String message, String actionText, Runnable action) {
        Theme theme = Theme.get(); HoverCard card = new HoverCard(new BorderLayout(14, 8));
        card.add(badge(icon, theme.accent, theme.accentSoft, 54), BorderLayout.WEST);
        JPanel copy = new JPanel(); copy.setOpaque(false); copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(Ui.label(title, theme.headingFont, theme.text)); copy.add(Box.createVerticalStrut(7));
        copy.add(Ui.label(message, theme.bodyFont, theme.muted));
        AccentButton button = AccentButton.primary(actionText); button.addActionListener(e -> action.run());
        copy.add(Box.createVerticalStrut(12)); copy.add(button); card.add(copy, BorderLayout.CENTER);
        return card;
    }

    private static JLabel badge(String text, Color foreground, Color background, int size) {
        JLabel label = new JLabel(text, JLabel.CENTER); label.setOpaque(true); label.setForeground(foreground); label.setBackground(background);
        label.setFont(Theme.get().headingFont.deriveFont(Font.BOLD, size > 40 ? 23f : 15f)); label.setPreferredSize(new Dimension(size, size));
        return label;
    }

    private static String escape(String text) { return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }

    /** Clickable flashcard tile with a short horizontal flip animation. */
    private static final class FlashcardTile extends JPanel {
        private final Flashcard item;
        private boolean back;
        private float scale = 1f;
        private boolean turning;
        private int frame;
        FlashcardTile(Flashcard item) {
            this.item = item; setOpaque(false); setPreferredSize(new Dimension(300, 170));
            setToolTipText("Click to flip between the question and answer");
            addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { flip(); } });
        }
        private void flip() {
            if (turning) return; turning = true; frame = 0;
            Timer timer = new Timer(16, e -> {
                frame++; scale = frame < 9 ? 1f - frame / 9f : (frame - 9) / 9f;
                if (frame == 9) back = !back;
                repaint();
                if (frame >= 18) { scale = 1f; turning = false; ((Timer) e.getSource()).stop(); }
            }); timer.start();
        }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics); Theme theme = Theme.get(); Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth(), height = getHeight(), cardWidth = Math.max(30, (int) ((width - 10) * Math.max(.08f, scale))), left = (width - cardWidth) / 2;
            Color base = back ? theme.headerBottom : theme.accent;
            g.setColor(theme.shadow); g.fillRoundRect(left + 3, 6, cardWidth - 4, height - 14, 22, 22);
            g.setColor(base); g.fillRoundRect(left, 2, cardWidth - 6, height - 16, 22, 22);
            g.setColor(theme.text == Color.WHITE ? new Color(255,255,255,95) : new Color(255,255,255,125));
            g.setStroke(new BasicStroke(1.3f)); g.drawRoundRect(left, 2, cardWidth - 7, height - 17, 22, 22);
            g.setFont(theme.smallFont.deriveFont(Font.BOLD)); g.setColor(Color.WHITE);
            g.drawString(back ? "ANSWER · CLICK TO FLIP" : "QUESTION · CLICK TO REVEAL", left + 18, 27);
            g.setFont(theme.bodyFont.deriveFont(Font.BOLD, 15f));
            String value = back ? item.back + "\n\n" + item.explanation : item.front;
            drawWrapped(g, value, left + 18, 54, Math.max(20, cardWidth - 42), 23, Color.WHITE);
            g.setFont(theme.smallFont); g.setColor(new Color(255,255,255,190));
            g.drawString(item.subject + " · " + item.topic, left + 18, height - 25);
            g.dispose();
        }
        private void drawWrapped(Graphics2D g, String text, int x, int y, int width, int lineHeight, Color color) {
            g.setColor(color); int lineY = y;
            for (String paragraph : text.split("\\R")) {
                StringBuilder line = new StringBuilder();
                for (String word : paragraph.split("\\s+")) {
                    String candidate = line.isEmpty() ? word : line + " " + word;
                    if (g.getFontMetrics().stringWidth(candidate) > width && !line.isEmpty()) {
                        g.drawString(line.toString(), x, lineY); lineY += lineHeight; line = new StringBuilder(word);
                    } else line = new StringBuilder(candidate);
                }
                if (!line.isEmpty()) { g.drawString(line.toString(), x, lineY); lineY += lineHeight; }
            }
        }
    }
}

// ======================================================================
// SettingsScreen
// ======================================================================
/** Grouped preferences UI; all controls write to the existing profile model. */
final class SettingsScreen {
    private SettingsScreen() { }

    public static JPanel build(AppContext ctx) {
        Theme theme = Theme.get(); Profile profile = ctx.data.profile;
        JPanel page = Ui.page("Settings", "Tune your study space while keeping your learning data safe.");
        JTextField name = field(profile.name), course = field(profile.course);
        JTextField examDate = field(profile.targetExamDate == null ? "" : profile.targetExamDate.toString());
        JSpinner available = spinner(profile.availableMinutes, 15, 960, 15);
        JSpinner goal = spinner(profile.dailyGoalMinutes, 15, 960, 15);
        JSpinner quizSeconds = spinner(profile.quizSeconds, 10, 120, 5);
        JSpinner weakThreshold = spinner(profile.weakThreshold, 30, 90, 5);
        JSpinner targetScore = spinner(profile.targetScore, 1, 100, 1);
        JComboBox<String> examType = combo(new String[]{"UPSC", "TNPSC"}, profile.examType);
        JComboBox<String> themeChoice = combo(new String[]{"Light", "Dark"}, Theme.get().dark ? "Dark" : "Light");
        SwitchToggle onlineAi = new SwitchToggle("Online AI", profile.onlineAiEnabled);
        JSlider fontSize = new JSlider(85, 130, profile.fontSizePercent);
        fontSize.setMajorTickSpacing(15); fontSize.setMinorTickSpacing(5); fontSize.setPaintTicks(true);
        JLabel fontValue = Ui.label(profile.fontSizePercent + "%", theme.bodyFont.deriveFont(Font.BOLD), theme.accent);
        JPanel fontControl = new JPanel(new BorderLayout(8, 0)); fontControl.setOpaque(false);
        fontControl.add(fontSize, BorderLayout.CENTER); fontControl.add(fontValue, BorderLayout.EAST);
        fontSize.addChangeListener(e -> fontValue.setText(fontSize.getValue() + "%"));
        JPanel accentPreview = new JPanel(); accentPreview.setOpaque(true); accentPreview.setBackground(theme.accent); accentPreview.setPreferredSize(new Dimension(42, 26));
        AccentButton chooseAccent = AccentButton.ghost("Choose accent colour");
        JPanel accentControl = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); accentControl.setOpaque(false);
        accentControl.add(accentPreview); accentControl.add(chooseAccent);

        JPanel appearancePreview = new HoverCard(new BorderLayout(8, 0));
        appearancePreview.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        appearancePreview.add(Ui.label("Live preview", theme.smallFont, theme.muted), BorderLayout.WEST);
        JLabel previewTitle = Ui.label("Small steps, strong recall.", theme.headingFont, theme.accent);
        appearancePreview.add(previewTitle, BorderLayout.CENTER);
        JPanel appearanceContent = new JPanel(new BorderLayout(0, 10)); appearanceContent.setOpaque(false);
        appearanceContent.add(form("Theme preview", themeChoice, "Interface font size", fontControl, "Accent colour", accentControl), BorderLayout.NORTH);
        appearanceContent.add(appearancePreview, BorderLayout.CENTER);

        JPanel stack = new JPanel(); stack.setOpaque(false); stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.add(section("◐  Appearance", "A comfortable theme and readable type scale.", appearanceContent));
        stack.add(Box.createVerticalStrut(12));
        stack.add(section("◷  Study preferences", "Set the daily time budget and quiz pacing.", form(
                "Available minutes / day", available, "Daily goal minutes", goal,
                "Quiz seconds / question", quizSeconds, "Weak-topic threshold", weakThreshold)));
        stack.add(Box.createVerticalStrut(12));
        stack.add(section("◎  Exam", "Keep your planner aligned to your target.", form(
                "Exam focus", examType, "Target exam date (YYYY-MM-DD)", examDate,
                "Target score (%)", targetScore)));
        stack.add(Box.createVerticalStrut(12));
        stack.add(section("✦  AI assistant", "Online AI is optional and sends chat/material text only when enabled.", form(
                "Online AI & privacy", onlineAi)));
        stack.add(Box.createVerticalStrut(12));

        JPanel accountContent = new JPanel(new BorderLayout(0, 10)); accountContent.setOpaque(false);
        accountContent.add(form("Name", name, "Course / class", course), BorderLayout.NORTH);
        JPanel accountActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0)); accountActions.setOpaque(false);
        AccentButton backup = AccentButton.ghost("Create backup"), restore = AccentButton.ghost("Restore backup");
        AccentButton resetAll = AccentButton.ghost("Reset all data");
        accountActions.add(backup); accountActions.add(restore); accountActions.add(resetAll);
        accountContent.add(accountActions, BorderLayout.CENTER);
        accountContent.add(Ui.label("Study-Wise · local-first student learning coach", theme.smallFont, theme.muted), BorderLayout.SOUTH);
        stack.add(section("♙  Account & about", "Your profile and local backup controls.", accountContent));

        themeChoice.addActionListener(e -> {
            boolean dark = "Dark".equals(themeChoice.getSelectedItem());
            Theme.get().setDark(dark); profile.darkTheme = dark; ctx.applet.refreshTheme();
            accentPreview.setBackground(Theme.get().accent); previewTitle.setForeground(Theme.get().accent);
            appearancePreview.repaint();
        });
        chooseAccent.addActionListener(e -> {
            Color picked = JColorChooser.showDialog(page, "Choose accent colour", Theme.get().accent);
            if (picked != null) {
                Theme.get().setAccent(picked); profile.accentHex = hex(picked); accentPreview.setBackground(picked);
                previewTitle.setForeground(picked); ctx.applet.refreshTheme();
            }
        });
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); actions.setOpaque(false);
        AccentButton defaults = AccentButton.ghost("Reset preferences");
        AccentButton save = AccentButton.primary("Save settings");
        actions.add(defaults); actions.add(save);
        backup.addActionListener(e -> { ctx.saveOrWarn(); JOptionPane.showMessageDialog(page, "Backup stored at " + ctx.store.backupPath()); });
        restore.addActionListener(e -> restore(ctx, page));
        resetAll.addActionListener(e -> resetAll(ctx, page));
        defaults.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(page, "Restore default preferences? Your subjects, quizzes, and notes will remain.",
                    "Reset preferences", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
            Profile defaultsProfile = new Profile();
            name.setText(profile.name); course.setText(profile.course);
            available.setValue(defaultsProfile.availableMinutes); goal.setValue(defaultsProfile.dailyGoalMinutes);
            quizSeconds.setValue(defaultsProfile.quizSeconds); weakThreshold.setValue(defaultsProfile.weakThreshold);
            targetScore.setValue(defaultsProfile.targetScore); examType.setSelectedItem(defaultsProfile.examType);
            examDate.setText(""); fontSize.setValue(defaultsProfile.fontSizePercent); onlineAi.setSelected(false);
            themeChoice.setSelectedItem("Light"); Theme.get().setAccent(new Color(17, 119, 100));
            profile.accentHex = "#117764"; accentPreview.setBackground(Theme.get().accent);
            ctx.applet.refreshTheme();
        });
        save.addActionListener(e -> {
            LocalDate parsedDate = null;
            try { if (!examDate.getText().isBlank()) parsedDate = LocalDate.parse(examDate.getText().trim()); }
            catch (Exception ex) { JOptionPane.showMessageDialog(page, "Enter the exam date as YYYY-MM-DD.", "Check exam date", JOptionPane.WARNING_MESSAGE); return; }
            profile.name = name.getText().trim(); profile.course = course.getText().trim();
            profile.availableMinutes = (Integer) available.getValue(); profile.dailyGoalMinutes = (Integer) goal.getValue();
            profile.quizSeconds = (Integer) quizSeconds.getValue(); profile.weakThreshold = (Integer) weakThreshold.getValue();
            profile.fontSizePercent = fontSize.getValue(); profile.examType = String.valueOf(examType.getSelectedItem());
            profile.targetExamDate = parsedDate; profile.targetScore = (Integer) targetScore.getValue();
            profile.onlineAiEnabled = onlineAi.isSelected(); profile.darkTheme = Theme.get().dark;
            profile.accentHex = hex(Theme.get().accent); Theme.get().setFontSizePercent(profile.fontSizePercent);
            ctx.saveOrWarn(); ctx.applet.refreshTheme();
            JOptionPane.showMessageDialog(page, "Your study preferences have been saved.", "Settings saved", JOptionPane.INFORMATION_MESSAGE);
        });

        JPanel main = new JPanel(new BorderLayout(0, 12)); main.setOpaque(false);
        main.add(Ui.scroll(stack), BorderLayout.CENTER); main.add(actions, BorderLayout.SOUTH);
        page.add(main, BorderLayout.CENTER);
        return page;
    }

    private static JPanel section(String title, String subtitle, JPanel contents) {
        Theme theme = Theme.get(); HoverCard card = new HoverCard(new BorderLayout(0, 8));
        card.add(Ui.label(title, theme.headingFont, theme.text), BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout(0, 7)); body.setOpaque(false);
        body.add(Ui.label(subtitle, theme.smallFont, theme.muted), BorderLayout.NORTH);
        body.add(contents, BorderLayout.CENTER); card.add(body, BorderLayout.CENTER);
        return card;
    }

    private static JPanel form(Object... pairs) {
        Theme theme = Theme.get(); JPanel form = new JPanel(new GridLayout(0, 2, 10, 10)); form.setOpaque(false);
        for (int i = 0; i < pairs.length; i += 2) {
            form.add(Ui.label(String.valueOf(pairs[i]), theme.bodyFont, theme.text)); form.add((java.awt.Component) pairs[i + 1]);
        }
        return form;
    }

    private static JTextField field(String value) { JTextField field = new JTextField(value == null ? "" : value); Ui.styleField(field); return field; }
    private static JSpinner spinner(int value, int min, int max, int step) { JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, min, max, step)); spinner.setFont(Theme.get().bodyFont); return spinner; }
    private static JComboBox<String> combo(String[] options, String selected) { JComboBox<String> combo = new JComboBox<>(options); combo.setSelectedItem(selected); combo.setFont(Theme.get().bodyFont); return combo; }
    private static String hex(Color color) { return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue()); }

    private static void restore(AppContext ctx, JPanel page) {
        if (!Files.exists(ctx.store.backupPath())) { JOptionPane.showMessageDialog(page, "No backup exists yet."); return; }
        if (JOptionPane.showConfirmDialog(page, "Replace current data with backup?", "Confirm restore", JOptionPane.OK_CANCEL_OPTION) != 0) return;
        try {
            ctx.store.restoreBackup(); ctx.data.replaceWith(ctx.store.load());
            Theme.get().setDark(ctx.data.profile.darkTheme);
            try { Theme.get().setAccent(Color.decode(ctx.data.profile.accentHex)); } catch (Exception ignored) { }
            ctx.applet.showScreen("Settings");
        } catch (Exception ex) { JOptionPane.showMessageDialog(page, ex.getMessage()); }
    }

    private static void resetAll(AppContext ctx, JPanel page) {
        if (JOptionPane.showConfirmDialog(page, "Delete saved profile and all data?", "Confirm reset", JOptionPane.OK_CANCEL_OPTION) != 0) return;
        ctx.data.profile = new Profile(); ctx.data.subjects.clear(); ctx.data.tasks.clear(); ctx.data.sessions.clear();
        ctx.data.quizAttempts.clear(); ctx.data.favorites.clear(); ctx.data.distractions.clear(); ctx.data.reflections.clear();
        ctx.data.knowledge.clear(); ctx.data.badges.clear(); ctx.data.flashcards.clear(); ctx.data.xp = 0; ctx.saveOrWarn();
        if (Onboarding.run(ctx)) { DailyCheckIn.run(ctx); ctx.applet.showScreen("Dashboard"); }
    }

    /** Compact switch control that remains keyboard accessible. */
    private static final class SwitchToggle extends JToggleButton {
        SwitchToggle(String label, boolean selected) {
            super(label, selected); setOpaque(false); setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
            setFont(Theme.get().bodyFont); setForeground(Theme.get().text); setToolTipText("Toggle optional online AI responses");
            setPreferredSize(new Dimension(150, 34));
        }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics); Theme theme = Theme.get(); Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int x = getWidth() - 48, y = (getHeight() - 22) / 2;
            g.setColor(isSelected() ? theme.accent : theme.cardAlt); g.fillRoundRect(x, y, 44, 22, 22, 22);
            g.setColor(Color.WHITE); g.fillOval(x + (isSelected() ? 24 : 3), y + 3, 16, 16); g.dispose();
        }
    }
}

// ======================================================================
// SubjectsScreen
// ======================================================================
final class SubjectsScreen {
    private SubjectsScreen() {
    }

    public static JPanel build(AppContext ctx) {
        Theme theme = Theme.get();
        JPanel page = Ui.page("Your Subjects", "Build a balanced exam plan around the subjects you care about.");
        AccentButton add = AccentButton.primary("Add subject");
        add.setToolTipText("Add a subject, exam date, and topic strengths");
        add.addActionListener(e -> edit(ctx, null));
        JPanel top = new JPanel(new BorderLayout()); top.setOpaque(false);
        top.add(Ui.label(ctx.data.subjects.size() + " subjects in your study plan", theme.bodyFont, theme.muted), BorderLayout.WEST);
        top.add(add, BorderLayout.EAST);
        JPanel list = new JPanel(new GridLayout(0, 2, 12, 12)); list.setOpaque(false);
        if (ctx.data.subjects.isEmpty()) {
            HoverCard empty = new HoverCard(new BorderLayout(10, 8));
            JLabel icon = new JLabel("◎", JLabel.CENTER); icon.setFont(theme.titleFont); icon.setForeground(theme.accent); icon.setOpaque(true); icon.setBackground(theme.accentSoft); icon.setPreferredSize(new Dimension(76, 76));
            empty.add(icon, BorderLayout.WEST);
            JPanel copy = new JPanel(); copy.setOpaque(false); copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
            copy.add(Ui.label("Your study plan starts with a subject", theme.headingFont, theme.text));
            copy.add(Box.createVerticalStrut(7)); copy.add(Ui.label("Add your exam subjects and topics to get personalized study blocks.", theme.bodyFont, theme.muted));
            AccentButton emptyAdd = AccentButton.primary("Add first subject"); emptyAdd.addActionListener(e -> edit(ctx, null));
            copy.add(Box.createVerticalStrut(12)); copy.add(emptyAdd); empty.add(copy, BorderLayout.CENTER); list.add(empty);
        } else for (Subject subject : ctx.data.subjects) list.add(card(ctx, subject));
        JPanel body = new JPanel(new BorderLayout(0, 12)); body.setOpaque(false); body.add(top, BorderLayout.NORTH); body.add(Ui.scroll(list), BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private static JPanel card(AppContext ctx, Subject subject) {
        Theme theme = Theme.get();
        HoverCard card = new HoverCard(new BorderLayout(0, 10));
        JPanel heading = new JPanel(new BorderLayout(10, 0)); heading.setOpaque(false);
        String initials = subject.name == null || subject.name.isBlank() ? "S" : subject.name.substring(0, 1).toUpperCase();
        JLabel emblem = new JLabel(initials, JLabel.CENTER); emblem.setOpaque(true); emblem.setForeground(theme.accent); emblem.setBackground(theme.accentSoft);
        emblem.setFont(theme.headingFont.deriveFont(Font.BOLD, 18f)); emblem.setPreferredSize(new Dimension(44, 44));
        heading.add(emblem, BorderLayout.WEST);
        JPanel title = new JPanel(); title.setOpaque(false); title.setLayout(new BoxLayout(title, BoxLayout.Y_AXIS));
        title.add(Ui.label(subject.name, theme.headingFont, theme.text));
        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), subject.examDate);
        String examLabel = days < 0 ? "Exam date passed" : days == 0 ? "Exam today" : "Exam in " + days + " days";
        title.add(Ui.label(examLabel + "  ·  " + subject.examDate, theme.smallFont, theme.muted));
        heading.add(title, BorderLayout.CENTER);
        JLabel strength = new JLabel("STRENGTH " + subject.strength + "/5", JLabel.CENTER); strength.setOpaque(true);
        strength.setForeground(strengthColor(subject.strength)); strength.setBackground(soft(strengthColor(subject.strength))); strength.setBorder(BorderFactory.createEmptyBorder(5, 7, 5, 7));
        heading.add(strength, BorderLayout.EAST); card.add(heading, BorderLayout.NORTH);
        JPanel detail = new JPanel(new BorderLayout(4, 7)); detail.setOpaque(false);
        JPanel topics = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2)); topics.setOpaque(false);
        for (Topic topic : subject.topics) {
            JLabel chip = new JLabel("•  " + topic.name); Color c = strengthColor(topic.strength);
            chip.setOpaque(true); chip.setForeground(c); chip.setBackground(soft(c)); chip.setFont(theme.smallFont);
            chip.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8)); chip.setToolTipText(topic.name + " · planner strength " + topic.strength + "/5"); topics.add(chip);
        }
        detail.add(Ui.label(subject.topics.size() + " topics · strength guides planner priority", theme.smallFont, theme.muted), BorderLayout.NORTH);
        detail.add(topics, BorderLayout.CENTER);
        AnimatedBar strengthBar = new AnimatedBar(); strengthBar.setPreferredSize(new Dimension(250, 20)); strengthBar.setProgress(subject.strength / 5f, "");
        strengthBar.setToolTipText("Overall strength " + subject.strength + " out of 5"); detail.add(strengthBar, BorderLayout.SOUTH);
        card.add(detail, BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(1, 2, 8, 0));
        actions.setOpaque(false);
        AccentButton edit = AccentButton.ghost("Edit");
        AccentButton del = AccentButton.ghost("Remove");
        edit.addActionListener(e -> edit(ctx, subject));
        del.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(card, "Remove " + subject.name + "?", "Confirm",
                    JOptionPane.OK_CANCEL_OPTION) == 0) {
                ctx.data.subjects.remove(subject);
                ctx.saveOrWarn();
                ctx.applet.showScreen("Subjects");
            }
        });
        actions.add(edit);
        actions.add(del);
        card.add(actions, BorderLayout.SOUTH);
        card.setPreferredSize(new Dimension(340, 222));
        return card;
    }

    private static Color strengthColor(int value) {
        return value <= 2 ? Theme.get().danger : value == 3 ? new Color(210, 139, 43) : Theme.get().success;
    }
    private static Color soft(Color color) { return new Color(color.getRed(), color.getGreen(), color.getBlue(), 34); }

    private static void edit(AppContext ctx, Subject old) {
        JTextField name = new JTextField(old == null ? "" : old.name);
        JTextField exam = new JTextField(old == null ? LocalDate.now().plusWeeks(4).toString() : old.examDate.toString());
        Ui.styleField(name); Ui.styleField(exam);
        JSlider strength = new JSlider(1, 5, old == null ? 3 : old.strength);
        strength.setMajorTickSpacing(1);
        strength.setPaintTicks(true);
        strength.setPaintLabels(true);
        JTextField topics = new JTextField();
        Ui.styleField(topics);
        if (old != null) {
            StringBuilder sb = new StringBuilder();
            for (Topic topic : old.topics) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(topic.name).append(":").append(topic.strength);
            }
            topics.setText(sb.toString());
        } else {
            topics.setText("Core ideas:3, Practice:3, Exam review:3");
        }
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new javax.swing.JLabel("Name"));
        form.add(name);
        form.add(new javax.swing.JLabel("Exam date YYYY-MM-DD"));
        form.add(exam);
        form.add(new javax.swing.JLabel("Overall strength"));
        form.add(strength);
        form.add(new javax.swing.JLabel("Topics name:strength"));
        form.add(topics);
        if (JOptionPane.showConfirmDialog(null, form, old == null ? "Add subject" : "Edit subject",
                JOptionPane.OK_CANCEL_OPTION) != 0) {
            return;
        }
        try {
            if (name.getText().isBlank()) {
                throw new IllegalArgumentException();
            }
            Subject subject = old == null ? new Subject() : old;
            subject.name = name.getText().trim();
            subject.examDate = LocalDate.parse(exam.getText().trim());
            subject.strength = strength.getValue();
            subject.topics.clear();
            for (String part : topics.getText().split(",")) {
                String bit = part.trim();
                if (bit.isEmpty()) {
                    continue;
                }
                String[] pair = bit.split(":");
                int s = pair.length > 1 ? Integer.parseInt(pair[1].trim()) : 3;
                subject.topics.add(new Topic(pair[0].trim(), s));
            }
            subject.ensureDefaultTopics();
            if (old == null) {
                ctx.data.subjects.add(subject);
            }
            ctx.saveOrWarn();
            ctx.applet.showScreen("Subjects");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Check the name, exam date, and topic list (Algebra:2, Geometry:4).");
        }
    }
}

// ======================================================================
// UploadMaterialScreen
// ======================================================================
/** Material intake page with progress, analysis and safe, explainable error handling. */
final class UploadMaterialScreen {
    private UploadMaterialScreen() { }

    public static JPanel build(AppContext ctx) {
        Theme theme = Theme.get();
        JPanel page = Ui.page("Upload Material", "Study notes, documents, or a source link become a grounded revision resource.");
        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setOpaque(false);
        RoundedPanel intake = Ui.card();
        intake.setLayout(new BorderLayout(0, 10));
        intake.add(Ui.label("Add material for your learning coach", theme.headingFont, theme.text), BorderLayout.NORTH);
        intake.add(Ui.label("Supported: PDF, DOCX, PPTX, TXT, Markdown, HTML, PNG, JPG and BMP. Images use local Tesseract OCR when installed. Pasted text and HTTP/HTTPS pages are also supported.", theme.bodyFont, theme.muted), BorderLayout.CENTER);

        JLabel status = Ui.label("", theme.smallFont, theme.muted);
        AccentButton file = AccentButton.primary("Choose a file");
        AccentButton paste = AccentButton.ghost("Paste text");
        AccentButton link = AccentButton.ghost("Analyze web link");
        AccentButton quiz = AccentButton.ghost("Create quiz from this material");
        AccentButton clear = AccentButton.ghost("Clear loaded material");
        file.addActionListener(e -> chooseFile(ctx, page, status, file, paste, link));
        paste.addActionListener(e -> pasteText(ctx, page, status, file, paste, link));
        link.addActionListener(e -> enterLink(ctx, page, status, file, paste, link));
        quiz.addActionListener(e -> ctx.applet.showScreen("Quiz Center"));
        clear.addActionListener(e -> {
            ctx.data.activeMaterialName = "";
            ctx.data.activeMaterialText = "";
            ctx.data.activeMaterialAnalysis = "";
            ctx.saveQuietly();
            ctx.applet.showScreen("Upload Material");
        });
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.setOpaque(false);
        controls.add(file); controls.add(paste); controls.add(link);
        intake.add(controls, BorderLayout.SOUTH);

        JPanel result = new JPanel(new BorderLayout(0, 8));
        result.setOpaque(false);
        if (ctx.data.activeMaterialText.isBlank()) {
            RoundedPanel empty = Ui.card();
            empty.add(Ui.label("No material loaded yet", theme.headingFont, theme.text), BorderLayout.NORTH);
            empty.add(Ui.label("Add material above. Its extracted text and study brief will be stored locally for follow-up questions.",
                    theme.bodyFont, theme.muted), BorderLayout.CENTER);
            result.add(empty, BorderLayout.CENTER);
        } else {
            RoundedPanel preview = Ui.card();
            preview.setLayout(new BorderLayout(0, 9));
            JPanel title = new JPanel(new BorderLayout());
            title.setOpaque(false);
            title.add(Ui.label(ctx.data.activeMaterialName, theme.headingFont, theme.text), BorderLayout.CENTER);
            title.add(clear, BorderLayout.EAST);
            preview.add(title, BorderLayout.NORTH);
            JTextArea analysis = new JTextArea(ctx.data.activeMaterialAnalysis.isBlank()
                    ? new MaterialAnalyzer().analyze(ctx.data.activeMaterialText, ctx.data.profile.examType)
                    : ctx.data.activeMaterialAnalysis);
            analysis.setEditable(false);
            analysis.setLineWrap(true);
            analysis.setWrapStyleWord(true);
            analysis.setFont(theme.bodyFont);
            analysis.setForeground(theme.text);
            analysis.setBackground(theme.cardAlt);
            analysis.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            preview.add(Ui.scroll(analysis), BorderLayout.CENTER);
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            actions.setOpaque(false);
            actions.add(quiz);
            AccentButton ask = AccentButton.ghost("Ask mentor about this material");
            ask.addActionListener(e -> MentorChatDialog.show(ctx));
            actions.add(ask);
            preview.add(actions, BorderLayout.SOUTH);
            result.add(preview, BorderLayout.CENTER);
        }
        result.add(status, BorderLayout.SOUTH);
        body.add(intake, BorderLayout.NORTH);
        body.add(result, BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private static void chooseFile(AppContext ctx, JPanel parent, JLabel status,
                                   AccentButton... controls) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose study material");
        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            Path path = chooser.getSelectedFile().toPath();
            analyze(ctx, path.getFileName().toString(), () -> new MaterialExtractor().read(path), parent, status, controls);
        }
    }

    private static void pasteText(AppContext ctx, JPanel parent, JLabel status,
                                  AccentButton... controls) {
        JTextArea area = new JTextArea(14, 54);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(Theme.get().bodyFont);
        JTextField title = new JTextField("Pasted study notes");
        JPanel form = new JPanel(new BorderLayout(0, 8));
        form.add(title, BorderLayout.NORTH);
        form.add(new JScrollPane(area), BorderLayout.CENTER);
        if (JOptionPane.showConfirmDialog(parent, form, "Paste material", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            String text = area.getText().trim();
            if (text.isBlank()) {
                JOptionPane.showMessageDialog(parent, "Paste some study text first.");
                return;
            }
            analyze(ctx, title.getText().isBlank() ? "Pasted material" : title.getText().trim(),
                    () -> text, parent, status, controls);
        }
    }

    private static void enterLink(AppContext ctx, JPanel parent, JLabel status,
                                  AccentButton... controls) {
        String url = JOptionPane.showInputDialog(parent, "HTTP/HTTPS page address:", "Analyze web page", JOptionPane.QUESTION_MESSAGE);
        if (url == null || url.isBlank()) return;
        analyze(ctx, url.trim(), () -> new MaterialExtractor().readWebLink(url), parent, status, controls);
    }

    private static void analyze(AppContext ctx, String label, TextLoader loader, JPanel parent,
                               JLabel status, AccentButton... controls) {
        for (AccentButton control : controls) control.setEnabled(false);
        status.setText("Loading and analyzing material…");
        status.setForeground(Theme.get().accent);
        new SwingWorker<String[], Void>() {
            @Override protected String[] doInBackground() throws Exception {
                String text = loader.read();
                if (text == null || text.isBlank()) throw new IllegalArgumentException("No readable text was extracted.");
                if (text.length() > 500_000) text = text.substring(0, 500_000);
                String report = ctx.mentor.analyzeMaterial(ctx, text);
                return new String[]{text, report};
            }
            @Override protected void done() {
                for (AccentButton control : controls) control.setEnabled(true);
                try {
                    String[] result = get();
                    ctx.data.activeMaterialName = label;
                    ctx.data.activeMaterialText = result[0];
                    ctx.data.activeMaterialAnalysis = result[1];
                    ctx.saveQuietly();
                    status.setText("Material saved locally · " + result[0].split("\\s+").length + " words analyzed.");
                    ctx.applet.showScreen("Upload Material");
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    status.setText(" ");
                    JOptionPane.showMessageDialog(parent, cause.getMessage() == null ? "Could not read this material." : cause.getMessage(),
                            "Material could not be analyzed", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    @FunctionalInterface private interface TextLoader { String read() throws Exception; }
}

// ======================================================================
// Quotes
// ======================================================================
/** Date-stable original quotes so the dashboard does not need a network call. */
final class Quotes {
    private static final String[] LINES = {
            "Small steps, repeated daily, become extraordinary progress.",
            "A focused minute is a vote for the future you want.",
            "Start where you are; one page is still progress.",
            "Your effort today makes tomorrow a little easier.",
            "Curiosity turns hard work into discovery.",
            "Consistency is quiet confidence in action.",
            "Make the next twenty minutes count.",
            "Progress grows when you return to the work.",
            "Every question you ask opens a door.",
            "You do not need perfect conditions to begin.",
            "Learning is built one brave attempt at a time.",
            "A clear goal gives your energy somewhere to go.",
            "Show up gently, then keep showing up.",
            "Today is a fresh chance to understand one more thing.",
            "The first step is often the most powerful one.",
            "Your pace is valid; keep moving forward.",
            "Focus on the task in front of you.",
            "Practice turns uncertainty into skill.",
            "Small wins deserve to be noticed.",
            "Give your attention to what matters now.",
            "A short study block can change your whole day.",
            "Rest is part of a thoughtful plan.",
            "Mistakes are evidence that you are stretching.",
            "Build the habit, and the results will follow.",
            "One solved problem can unlock many more.",
            "Keep your promise to your future self.",
            "Steady effort beats waiting for inspiration.",
            "You can learn difficult things one piece at a time.",
            "Begin with five minutes; momentum can follow.",
            "Make room for progress, not perfection."
    };

    private Quotes() {
    }

    public static String forToday() {
        int index = Math.floorMod((int) LocalDate.now().toEpochDay(), LINES.length);
        return LINES[index];
    }
}

// ======================================================================
// TimeFmt
// ======================================================================
/** Small date/clock helpers so screens do not copy formatting code. */
final class TimeFmt {
    public static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE, MMM d");
    public static final DateTimeFormatter LONG = DateTimeFormatter.ofPattern("EEEE, MMMM d");

    private TimeFmt() {
    }

    public static String hoursLabel(int minutes) {
        if (minutes < 60) {
            return minutes + " min";
        }
        int hours = minutes / 60;
        int rem = minutes % 60;
        if (rem == 0) {
            return hours + " h";
        }
        return hours + " h " + rem + " min";
    }

    public static String day(LocalDate date) {
        return date.format(DAY);
    }
}
