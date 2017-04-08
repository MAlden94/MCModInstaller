package com.Mitch.ModInstaller;

import com.Mitch.SplashScreen.SplashScreen;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.management.OperatingSystemMXBean;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Properties;

public class ModInstaller {
    public static final long MINIMUM_RAM  = 2000000000L;
    public static final int  MINIMUM_CORES = 2;

    public static final int  FS_IO_BUFFER  = 4096;
    public static final int  NET_IO_BUFFER = 8192;

    public static SplashScreen screen = null;

    private static Logger logger = Logger.getLogger("com.Mitch.ModInstaller");

    public static void main(String[] args) {
        try {
            logger.addHandler(new FileHandler("%t/ModInstaller-%u.log"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.setLevel(Level.INFO);
        if (args.length > 0) try {
            logger.setLevel(Level.parse(args[0]));
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Can't set logger level. Reason:", e);
        }

        logger.info("Starting...");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.log(Level.WARNING, "UIManager.setLookAndFeel", e);
        }

        Properties prop = new Properties();
        try {
           prop.load(ModInstaller.class.getResourceAsStream("Config.properties"));
        } catch (IOException e) {
           logger.log(Level.WARNING, "ModInstaller.class.getResource", e);
        }

        String ModeArchiveURL = prop.getProperty("modpackurl");
        String ProfileName    = prop.getProperty("server", "Mitch's Installer");
        int MinimumCores      = Integer.parseInt(prop.getProperty("mincore", String.valueOf(MINIMUM_CORES)));
        long MinimumRam       = Long.parseLong(prop.getProperty("minram", String.valueOf(MINIMUM_RAM)));
        String javaArgs       = prop.getProperty("javaArgs");

        try {
            screen = new SplashScreen(ImageIO.read(ModInstaller.class.getResource("Server.png")), IOUtils.toString(ModInstaller.class.getResourceAsStream("InfoText.html"), Charsets.UTF_8));
        } catch (IOException e) {
            logger.log(Level.WARNING, "ModInstaller.class.getResource", e);
        }

        screen.setProgressMax(100);
        screen.setProgress("Please Wait…", 0);
        screen.setFocusableWindowState(false);
        screen.setAutoRequestFocus(false);
        screen.setLocationRelativeTo(null);
        screen.setScreenVisible(true);
        screen.toBack();

        OperatingSystemMXBean SystemProp = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        String warning = "";
        if (SystemProp.getFreePhysicalMemorySize() < MinimumRam) {
            warning = humanReadableByteCount(MinimumRam, true) + " of ram.";
        }
        if (SystemProp.getAvailableProcessors() < MinimumCores) {
            if (!warning.isEmpty()) {
                warning = warning + "\n    ";
            }
            String coreName = String.valueOf(MinimumCores);
            switch (coreName) {
                case "1":
                    coreName = "single";
                    break;
                case "2":
                    coreName = "dual";
                    break;
                case "3":
                    coreName = "tri";
                    break;
                case "4":
                    coreName = "quad";
                    break;
            }

            warning = warning + "a " + coreName + " core cpu.";
        }
        if (!warning.isEmpty()) {
            logger.log(Level.INFO, "User has: " + warning);

            screen.toBack();
            int UserResponse = JOptionPane.showConfirmDialog(screen, "You need at least the following for this mod pack to work:\n    " + warning + "\nInstall anyways?", "Error", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

            if (UserResponse == JOptionPane.NO_OPTION) {
                logger.log(Level.INFO, "User exit");
                System.exit(0);
            }
        }

        String ForgeVersion = null;

        Path jvm_location = Paths.get(System.getProperty("java.home") + File.separator + "bin" + File.separator + (System.getProperty("os.name").startsWith("Win") ? "java.exe" : "java"));

        Pattern DupModFinder = Pattern.compile("([a-z_-]*).*(?<!server).jar", Pattern.CASE_INSENSITIVE);

        Pattern ForgeBinary = Pattern.compile("forge-(.*)-(.*)-(.*)-installer.jar", Pattern.CASE_INSENSITIVE); // bugged if I use File.separator

        Pattern MinecraftDir = Pattern.compile("Checking \"(.*)" + Pattern.quote(File.separator) + "libraries" + Pattern.quote(File.separator) + ".*", Pattern.CASE_INSENSITIVE);

        URL ModPackURL = null;
        try {
            ModPackURL = new URL(ModeArchiveURL);
        } catch (MalformedURLException e) {
            logger.log(Level.SEVERE, "ModPackURL", e);
        }

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("mods");

            logger.log(Level.INFO, "tempDir = " + tempDir);

            FileUtils.forceDeleteOnExit(tempDir.toFile());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "tempDir Failed", e);
        }

        File tempXZ = null; //new File("/home/mitch/Dropbox/Minecraft/mods.tar.xz");

        if (tempXZ == null) try {
            tempXZ = File.createTempFile("mods", ".tar.xz");

            logger.log(Level.INFO, "tempXZ = " + tempXZ);

            FileUtils.forceDeleteOnExit(tempXZ);
            downloadFromUrl(ModPackURL, tempXZ.toString());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "tempXZ Failed", e);
        }

        Path MinecraftFolder = null;

        try {
            FileInputStream fin = new FileInputStream(tempXZ);
            BufferedInputStream in = new BufferedInputStream(fin);

            XZCompressorInputStream xzIn = new XZCompressorInputStream(in);
            TarArchiveInputStream tarIn = new TarArchiveInputStream(xzIn);
            TarArchiveEntry entry;
            while ((entry = (TarArchiveEntry) tarIn.getNextEntry()) != null) {

                logger.log(Level.INFO, "Extracting: " + entry.getName());
                Path entryDestination = tempDir.resolve(entry.getName());
                Files.createDirectories(entryDestination.getParent());

                if (entry.isDirectory()) {
                    Files.createDirectories(entryDestination);
                } else {
                    FileOutputStream fos = new FileOutputStream(entryDestination.toFile());
                    BufferedOutputStream dest = new BufferedOutputStream(fos, FS_IO_BUFFER);

                    long CurrentLength = 0L;
                    long ContentLength = entry.getSize();

                    byte[] data = new byte[FS_IO_BUFFER];
                    int len;
                    while ((len = tarIn.read(data, 0, FS_IO_BUFFER)) != -1) {
                        CurrentLength += len;
                        float CurrentPercent = (float) CurrentLength / (float) ContentLength * 100.0F;
                        screen.setProgress(
                                String.format(
                                        "Extracting: %s: %s/%s (%.0f%%)",
                                        ellipsize(Paths.get(entry.getName()).getFileName().toString(), 15),
                                        humanReadableByteCount(CurrentLength, true),
                                        humanReadableByteCount(ContentLength, true),
                                        CurrentPercent
                                ), (int) CurrentPercent);

                        dest.write(data, 0, len);
                    }
                    dest.close();

                    Matcher ForgeMatcher = ForgeBinary.matcher(Paths.get(entry.getName()).getFileName().toString());

                    if (ForgeMatcher.matches()) {

                        ForgeVersion = ForgeMatcher.group(1) + "-Forge" + ForgeMatcher.group(2);

                        logger.log(Level.INFO, "Found: " + ForgeMatcher.group(0) + " Prefix: " + ForgeMatcher.group(1) + " Suffix: " + ForgeMatcher.group(2));
                        screen.setProgress("Found: " + ForgeVersion, 100);

                        if (Files.isExecutable(jvm_location)) {
                            logger.log(Level.INFO, "Found jvm at " + jvm_location);
                            screen.setProgress("Found jvm at " + jvm_location, 100);

                            Process process = new ProcessBuilder(jvm_location.toString(), "-jar", entryDestination.toString()).start();

                            BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream()));
                            String line;
                            while ((line = out.readLine()) != null) {
                                screen.setProgress(ellipsize(line, 20), 100);
                                Matcher MinecraftMatcher = MinecraftDir.matcher(line);
                                if (MinecraftMatcher.matches()) {
                                    MinecraftFolder = Paths.get(MinecraftMatcher.group(1));
                                    screen.setProgress("Found: " + MinecraftFolder, 100);
                                    break;
                                }
                            }
                        } else {
                            logger.log(Level.WARNING, "Java JVM may be broken: " + jvm_location);
                            screen.setProgress("Your Java JVM may be broken: " + jvm_location, 100);
                            JOptionPane.showMessageDialog(screen, "JAVA_HOME is not set correctly or Java has been improperly installed.\nPlease make sure Java 7 is installed.\nUnable to run \"" + entry.getName() + "\"", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
            tarIn.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to open mod pack, will now exit", e);
            JOptionPane.showMessageDialog(screen, "Failed to open mod pack!\nPlease try running Mitch's Mod Installer again.\nProgram will now exit.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        FolderChooserDialog ChosenFolder;
        if (MinecraftFolder == null || Files.notExists(MinecraftFolder)) {
            logger.log(Level.WARNING, "Forge Installer didn't give me a valid folder: " + MinecraftFolder);
            JOptionPane.showMessageDialog(screen, "Forge Installer didn't give me a .minecraft folder!\nPlease select a valid .minecraft folder", "Warning", JOptionPane.WARNING_MESSAGE);

            String osType = System.getProperty("os.name");
            File targetDir;

            if ((osType.contains("win")) && (System.getenv("APPDATA") != null)) {
                targetDir = new File(System.getenv("APPDATA"), ".minecraft");
            } else {

                if (osType.contains("mac")) {
                    targetDir = new File(new File(new File(System.getProperty("user.home", "."), "Library"), "Application Support"), "minecraft");
                } else {
                    targetDir = new File(System.getProperty("user.home", "."), ".minecraft");
                }
            }
            ChosenFolder = new FolderChooserDialog(screen);
            ChosenFolder.setCurrentFolder(targetDir.toPath());
            ChosenFolder.setTitle("Choose .minecraft folder");

            MinecraftFolder = ChosenFolder.showOpenDialog();

        }

        if ((MinecraftFolder == null) || (Files.notExists(MinecraftFolder))) {
            logger.log(Level.INFO, "User did not choose a valid folder, exiting");
            System.exit(0);
        }

        Path MinecraftModFolder = MinecraftFolder.resolve("mods");

        if (Files.notExists(MinecraftModFolder)) try {
            logger.log(Level.INFO, "Creating: " + MinecraftModFolder);
            screen.setProgress("Creating: " + MinecraftModFolder, 100);
            Files.createDirectory(MinecraftModFolder);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to create!", e);
            screen.setProgress("Failed to create!", 0);
            JOptionPane.showMessageDialog(screen, "Failed to create mods folder: " + MinecraftModFolder, "Error", JOptionPane.ERROR_MESSAGE);
        }

        // Compare local name of mod to archive name of mod and remove if old.
        try (DirectoryStream<Path> NewMods = Files.newDirectoryStream(tempDir.resolve("mods"))) {
            for (Path NewMod : NewMods) {
                Matcher NewModPrefix = DupModFinder.matcher(NewMod.getFileName().toString());
                try (DirectoryStream<Path> OldMods = Files.newDirectoryStream(MinecraftFolder.resolve("mods"))) {
                    for (Path OldMod : OldMods) {
                        Matcher OldModPrefix = DupModFinder.matcher(OldMod.getFileName().toString());
                        if ((NewModPrefix.matches()) && (OldModPrefix.matches()) &&
                                (NewModPrefix.group(1).equalsIgnoreCase(OldModPrefix.group(1)))) {
                            logger.log(Level.INFO,
                                    String.format("%s is superseded by %s: Deleting for update!",
                                            OldMod.getFileName().toString(),
                                            NewMod.getFileName().toString()
                                    ));
                            screen.setProgress(
                                    String.format("%s is superseded by %s: Deleting for update!",
                                            ellipsize(OldMod.getFileName().toString(), 15),
                                            ellipsize(NewMod.getFileName().toString(), 15)
                                    ),
                                    100);

                            try {
                                logger.log(Level.INFO, "Deleting: " + OldMod);
                                Files.deleteIfExists(OldMod);
                            } catch (IOException e) {
                                logger.log(Level.WARNING, "Failed to delete!");
                                screen.setProgress("Failed to delete!", 0);
                            }

                        }
                    }
                }
                try {
                    logger.log(Level.INFO, "Installing: " + NewMod);
                    screen.setProgress("Installing: " + NewMod.getFileName(), 0);
                    Files.deleteIfExists(MinecraftFolder.resolve("mods").resolve(NewMod.getFileName()));
                    FileUtils.moveFileToDirectory(NewMod.toFile(), MinecraftFolder.resolve("mods").toFile(), true);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Failed to install!", e);
                    screen.setProgress("Failed to install!", 0);
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to install!", e);
            screen.setProgress("Failed to install!", 0);
        }

        try {
            FileUtils.forceDelete(tempDir.toFile());
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to Delete tempDir", e);
        }

        File LauncherProfile = MinecraftFolder.resolve("launcher_profiles.json").toFile();

        logger.log(Level.INFO, "Editing: " + LauncherProfile);
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            JsonObject jsonObj = gson.fromJson(new FileReader(LauncherProfile), JsonElement.class).getAsJsonObject();
            JsonObject profiles = jsonObj.getAsJsonObject("profiles");
            String selectedProfile = jsonObj.getAsJsonPrimitive("selectedProfile").getAsString();

            ArrayList<String> Choices = new ArrayList<>();
            Choices.add("Create New Profile");

            for (Map.Entry<String, JsonElement> entry : profiles.entrySet()) {
                logger.log(Level.INFO, "Found user: " + entry.getKey());
                Choices.add(entry.getKey());
            }

            JFrame frame = new JFrame(ProfileName);

            String userSelectedProfile = (String) JOptionPane.showInputDialog(frame, "Which profile should I use?", "Choose profile", JOptionPane.QUESTION_MESSAGE, null, Choices.toArray(), selectedProfile);

            if ((userSelectedProfile != null) && (!userSelectedProfile.isEmpty())) {
                logger.log(Level.INFO, "User Choose: " + userSelectedProfile);

                if (userSelectedProfile.equals("Create New Profile")) {
                    logger.log(Level.INFO, "Creating new profile");

                    userSelectedProfile = ProfileName;

                    JsonObject NewForgeProfile = new JsonObject();
                    NewForgeProfile.addProperty("name", userSelectedProfile);

                    profiles.add(userSelectedProfile, NewForgeProfile);
                }

                JsonObject userSelectedProfileObj = profiles.getAsJsonObject(userSelectedProfile);

                if (!userSelectedProfileObj.has("javaArgs")) {
                    if (javaArgs != null){
                        logger.log(Level.INFO, "Adding jvm flags");
                        userSelectedProfileObj.addProperty("javaArgs", javaArgs);
                    }

                    if (!userSelectedProfileObj.has("javaDir")) {
                        userSelectedProfileObj.addProperty("javaDir", jvm_location.toString());
                    }

                }

                if (!selectedProfile.equals(userSelectedProfile)) {
                    int answer = JOptionPane.showConfirmDialog(frame, "Make \"" + userSelectedProfile + "\" the default profile?", "Question", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (answer == JOptionPane.YES_OPTION) {
                        logger.log(Level.INFO, "Setting \"" + userSelectedProfile + "\" to default.");
                        jsonObj.addProperty("selectedProfile", userSelectedProfile);
                    }
                }

                frame.dispose();

                logger.log(Level.INFO, "Setting lastVersionId to " + ForgeVersion);
                userSelectedProfileObj.addProperty("lastVersionId", ForgeVersion);

                logger.log(Level.INFO, "Saving: " + LauncherProfile);
                screen.setProgress("Saving: " + LauncherProfile, 100);

                logger.log(Level.INFO, "json: " + gson.toJson(jsonObj));
                
                FileWriter writer = new FileWriter(LauncherProfile);
                writer.write(gson.toJson(jsonObj));
                writer.close();
            } else {
                logger.log(Level.INFO, "User exit");
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Profile edit failed!", e);
            screen.setProgress("Profile edit failed!", 0);
        }

        screen.setScreenVisible(false);
        System.exit(0);
    }

    private static void downloadFromUrl(URL url, String localFilename)
      throws IOException {
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            URLConnection urlConn = url.openConnection();

            is = urlConn.getInputStream();
            fos = new FileOutputStream(localFilename);

            long CurrentLength = 0L;
            long ContentLength = urlConn.getContentLengthLong();

            byte[] buffer = new byte[NET_IO_BUFFER];
            int len;
            try {
                while ((len = is.read(buffer)) > 0) {
                    if (is.markSupported()) is.mark(len);

                    CurrentLength += len;
                    float CurrentPercent = (float) CurrentLength / (float) ContentLength * 100.0F;
                    screen.setProgress(String.format("Downloading %s/%s (%.0f%%)", humanReadableByteCount(CurrentLength, true), humanReadableByteCount(ContentLength, true), CurrentPercent), (int) CurrentPercent);
                    fos.write(buffer, 0, len);
                }
            } catch (IOException e) {
                if (CurrentLength != ContentLength){
                    logger.log(Level.INFO, String.format("Partial download %d/%d: %s", CurrentLength, ContentLength, e));
                    if (is.markSupported()){
                        is.reset();
                    } else {
                        //downloadFromUrl(url, localFilename); // not safe can cause recursion
                    }
                } else {
                    throw e;
                }
            }
        } finally {
            try {
                if (is != null)
                    is.close();
            } finally {
                if (fos != null)
                    fos.close();
            }
        }
    }

    private static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return String.valueOf(bytes) + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = String.valueOf((si ? "kMGTPE" : "KMGTPE").charAt(exp - 1)) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    private static String ellipsize(String input, int maxCharacters) {
        maxCharacters--;
        if (input.length() > maxCharacters) {
            maxCharacters /= 2;
            input = input.substring(0, maxCharacters) + "…" + input.substring(input.length() - maxCharacters, input.length());
        }
        return input;
    }

}
