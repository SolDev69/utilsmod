package com.example.examplemod;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;

public class SysCmdCommand {

    // Returns the command builder for /syscmd
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("syscmd")
            .requires(source -> source.hasPermission(2))  // Only operators can use this command
            .then(Commands.argument("command", StringArgumentType.greedyString())
                .executes(context -> runSystemCommand(
                    context.getSource(),
                    StringArgumentType.getString(context, "command")
                ))
            );
    }

    // Executes the provided system command and sends the output to the command source
    private static int runSystemCommand(CommandSourceStack source, String command) {
        try {
            // Create a temporary file for capturing output
            File tempFile = File.createTempFile("syscmd_output", ".log");
            tempFile.deleteOnExit();

            // Run the command inside screen, redirecting output to the temp file
            Process process = new ProcessBuilder(
                "/usr/bin/screen", "-dmS", "mysession", "sh", "-c",
                command + " > " + tempFile.getAbsolutePath() + " 2>&1"
            ).start();

            // Wait a bit to ensure the command starts properly
            Thread.sleep(500);

            // Read the output from the temp file
            BufferedReader reader = new BufferedReader(new FileReader(tempFile));
            String line;
            while ((line = reader.readLine()) != null) {
            	final String outputLine = line;
                source.sendSuccess(() -> Component.literal(outputLine), false);
            }
            reader.close();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                source.sendFailure(Component.literal("Command failed with exit code: " + exitCode));
            }
        } catch (Exception e) {
            source.sendFailure(Component.literal("An error occurred: " + e.getMessage()));
        }
        return 1;
    }

}
