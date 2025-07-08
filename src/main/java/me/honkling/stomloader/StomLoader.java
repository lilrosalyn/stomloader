package me.honkling.stomloader;

import net.fabricmc.api.DedicatedServerModInitializer;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StomLoader implements DedicatedServerModInitializer {
    private static File serverJar = new File("airbrush.jar");
    private static boolean enableViaProxy = false;
    private static String[] viaProxyArgs = new String[] {"cli", "--bind-address", "0.0.0.0:25575"};

    @Override public void onInitializeServer() {}
    public static void main() {
        try {
            initializeProperties();
        } catch (IOException exception) {
            System.err.println("An error occurred initializing stomloader.properties.");
            exception.printStackTrace();
        }

        if (!serverJar.exists()) {
            System.err.printf("The JAR file %s could not be found.", serverJar.getAbsolutePath());
            return;
        }

        System.out.printf("Found the %s JAR file. Running now.%n", serverJar.getAbsolutePath());
        Process viaProxy = null;
        Process process = null;

        try {
            if (enableViaProxy)
                viaProxy = startViaProxy();
        } catch (IOException exception) {
            System.err.println("An error occurred starting ViaProxy.");
            exception.printStackTrace();
        }

        try {
            var memory = System.getenv("SERVER_RAM");
            var processBuilder = new ProcessBuilder(getExecutablePath(), "-jar", "-Xms" + memory, "-Xmx" + memory, serverJar.getAbsolutePath())
                    .inheritIO();

            if (viaProxy != null)
                processBuilder.environment().remove("SERVER_PORT");

            process = processBuilder.start();
            Runtime.getRuntime().addShutdownHook(new Thread(process::destroy));
            process.waitFor();
        } catch (IOException exception) {
            System.err.println("An error occurred when running stomloader.");
            exception.printStackTrace();
        } catch (InterruptedException exception) {
            System.out.println("Shutting down...");
            process.destroy();

            if (viaProxy != null)
                viaProxy.destroy();
        }
    }

    private static void initializeProperties() throws IOException {
        final var propertiesFile = new File("stomloader.properties");
        final var path = propertiesFile.toPath();

        if (!propertiesFile.exists()) {
            var inputStream = StomLoader.class.getResourceAsStream("/stomloader.properties");
            assert inputStream != null;

            Files.copy(inputStream, path);
        }

        List<String> lines = Files.readAllLines(path);

        // todo: implement more robust parsing & logging
        for (String line : lines) {
            int index = line.indexOf('=');
            String key = line.substring(0, index);
            String value = line.substring(index + 1).trim();

            if (key.equalsIgnoreCase("viaproxy"))
                enableViaProxy = value.equalsIgnoreCase("true");
            else if (key.equalsIgnoreCase("viaproxyArgs"))
                viaProxyArgs = value.split(" ");
            else if (key.equalsIgnoreCase("serverJar")) {
                serverJar = new File(value);
            } else System.err.println("Found unexpected key in stomloader.properties: " + key);
        }
    }

    private static Process startViaProxy() throws IOException {
        final var directory = new File("ViaProxy/");
        final var downloadUrl = new URL("https://github.com/ViaVersion/ViaProxy/releases/download/v3.4.2/ViaProxy-3.4.2.jar");
        final var destinationFile = new File(directory, "ViaProxy.jar");

        if (!destinationFile.exists())
            try (var inputStream = downloadUrl.openStream()) {
                System.out.println("Downloading ViaProxy ...");
                directory.mkdirs();
                Files.copy(inputStream, destinationFile.toPath());
                System.out.println("Finished downloading ViaProxy.");
            }

        Process process = null;

        try {
            System.out.println("Starting ViaProxy...");
            var memory = System.getenv("SERVER_RAM");
            String[] baseArguments = new String[] {getExecutablePath(), "-jar", "-Xms" + memory, "-Xmx" + memory};
            List<String> arguments = new ArrayList<>(baseArguments.length + viaProxyArgs.length + 1);
            Collections.addAll(arguments, baseArguments);
            arguments.add(destinationFile.getAbsolutePath());
            Collections.addAll(arguments, viaProxyArgs);

            var processBuilder = new ProcessBuilder(arguments.toArray(String[]::new));
            processBuilder.directory(directory);
            process = processBuilder.start();
            Runtime.getRuntime().addShutdownHook(new Thread(process::destroy));
        } catch (IOException exception) {
            System.err.println("An error occurred when running ViaProxy.");
            exception.printStackTrace();
        }

        return process;
    }

    private static String getExecutablePath() {
        var javaHome = System.getProperty("java.home");
        var file = new File(javaHome, "bin");
        return new File(file, "java").getAbsolutePath();
    }
}
