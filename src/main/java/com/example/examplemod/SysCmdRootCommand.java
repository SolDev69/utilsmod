package com.example.examplemod;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class SysCmdRootCommand {

    // Returns the command builder for /syscmdroot
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("syscmdroot")
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
            // Start the process
            Process process = new ProcessBuilder("sh", "-c", command).start();

            // Capture output in a separate thread
            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String outputLine = line;
                        source.sendSuccess(() -> Component.literal(outputLine), false);
                    }
                } catch (IOException e) {
                    source.sendFailure(Component.literal("Error reading output: " + e.getMessage()));
                }
            });

            // Capture errors in a separate thread
            Thread errorThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String errorLine = line;
                        source.sendFailure(Component.literal(errorLine));
                    }
                } catch (IOException e) {
                    source.sendFailure(Component.literal("Error reading errors: " + e.getMessage()));
                }
            });

            outputThread.start();
            errorThread.start();

            // Wait for the process to complete
            int exitCode = process.waitFor();

            // Ensure threads finish before continuing
            outputThread.join();
            errorThread.join();

            // Check exit status
            if (exitCode != 0) {
                source.sendFailure(Component.literal("Command failed with exit code: " + exitCode));
            }

            return exitCode;
        } catch (Exception e) {
            source.sendFailure(Component.literal("An error occurred: " + e.getMessage()));
            return 1;
        }
    }

}
