package com.kit;

import com.kit.api.util.Internet;
import com.kit.gui.controller.*;
import jiconfont.icons.FontAwesome;
import jiconfont.swing.IconFontSwing;
import org.apache.http.client.fluent.Request;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import com.kit.api.wrappers.Skill;
import com.kit.core.Session;
import com.kit.core.model.Property;
import com.kit.gui.ControllerManager;
import com.kit.gui.component.Appender;
import com.kit.gui.laf.ColourScheme;
import com.kit.gui.laf.DarkColourScheme;
import com.kit.gui.laf.DefaultColourScheme;
import com.kit.gui.laf.ScapeColourScheme;
import com.kit.gui.view.AppletView;
import com.kit.plugins.clan.ClanPlugin;
import com.kit.plugins.map.WorldMapPlugin;
import com.kit.socket.Client;
import com.kit.socket.event.LeaveClanEvent;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

/**
 * Application entry point
 * ttttt
 *
 */
public class Application {
    public static final ColourScheme COLOUR_SCHEME;
    public static final AppletView APPLET_VIEW;
    public static final Session SESSION;
    public static boolean outdated;

    public static boolean devMode = false;
    private static final String HOOKS_URL = "http://download.07kit.com/hooks.json";
    public static Image ICON_IMAGE;

    public static final Map<Skill, Image> SKILL_IMAGE_ICONS = new HashMap<>();
    public static final Map<String, Image> FLAG_IMAGES = new HashMap<>();

    static {
        IconFontSwing.register(FontAwesome.getIconFont());
        COLOUR_SCHEME = new DarkColourScheme();
        APPLET_VIEW = new AppletView();
        SESSION = new Session();
        System.setProperty("sun.java2d.opengl","True");
        try {
            ICON_IMAGE = ImageIO.read(Application.class.getResourceAsStream("/icon.png"));

            FLAG_IMAGES.put("Germany", ImageIO.read(Application.class.getResourceAsStream("/flag_DE.png")));
            FLAG_IMAGES.put("United Kingdom", ImageIO.read(Application.class.getResourceAsStream("/flag_GB.png")));
            FLAG_IMAGES.put("United States", ImageIO.read(Application.class.getResourceAsStream("/flag_US.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (final Skill s : Skill.values()) {
            try {
                if (s.getIndex() > Skill.CONSTRUCTION.getIndex()) {
                    break;
                }
                SKILL_IMAGE_ICONS.put(s, ImageIO.read(Application.class.getResourceAsStream("/" + s.name().toLowerCase() + ".gif")));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String HOOKS;

    public static void main(String[] args) throws IOException {
        setOSXDockIcon();
        File localHooks = new File("./hooks.json");
        if (localHooks.exists()) {
            HOOKS = new String(Files.readAllBytes(localHooks.toPath()));
        } else {
            HOOKS = Internet.getText(HOOKS_URL);
        }

        if (args.length > 0 && args[0] != null && args[0].trim().equals("-dev")) {
            devMode = true;
        }

        prepareEnvironment();

        Logger.getRootLogger().addAppender(new Appender(new SimpleLayout()));

        try {
            SwingUtilities.invokeAndWait(() -> {
                IconFontSwing.register(FontAwesome.getIconFont());
                COLOUR_SCHEME.init();
                new SidebarController();
                new MainController();
                new LoginController();
                new SettingsDebugController();
                new WidgetDebugController();
                new SettingsController();
                new GalleryController();

                ControllerManager.get(LoginController.class).show();
            });
        } catch (InterruptedException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private static void prepareEnvironment() {
        final File uids = new File(System.getProperty("user.home") + "/07kit/uids/");
        if (uids.exists()) {
            for (File uid : uids.listFiles()) {
                uid.delete();
            }
        } else {
            uids.mkdirs();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                Property.getContainer().save();
            }
        }));

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            Logger logger = Logger.getLogger("EXCEPTIONS");

            @Override
            public void uncaughtException(Thread t, Throwable e) {
                logger.error("Exception on thread " + t.getName(), e);
            }
        });

        Property.getContainer().load();
    }

    private static void setOSXDockIcon() {
        if (System.getProperty("os.name").contains("OS X")) {
            try {
                Object applicationObject = Class.forName("com.apple.eawt.Application")
                        .getDeclaredMethod("getApplication", new Class[]{})
                        .invoke(null, new Class[]{});
                Class<?> applicationClass = applicationObject.getClass();

                Method setDockIconImage = applicationClass.getDeclaredMethod("setDockIconImage", new Class[]{Image.class});
                setDockIconImage.invoke(applicationObject, (Object) ICON_IMAGE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
