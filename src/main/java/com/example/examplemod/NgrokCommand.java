package com.example.examplemod;

import com.github.alexdlaird.ngrok.NgrokClient;
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.github.alexdlaird.ngrok.protocol.CreateTunnel;
import com.github.alexdlaird.ngrok.protocol.Proto;
import com.github.alexdlaird.ngrok.protocol.Tunnel;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class NgrokCommand {
    private static final NgrokClient ngrokClient;

    static {
        final JavaNgrokConfig ngrokConfig = new JavaNgrokConfig.Builder()
                .withAuthToken("6znYwVSwpHihNVCqdoYsJ_7cjUUvJUsf8y4goifi3gq") // Set your auth token
                .build();
        ngrokClient = new NgrokClient.Builder().withJavaNgrokConfig(ngrokConfig).build();
    }

    static void runNgrok(CommandSourceStack source) {
        try {
            // Open an MTR tunnel (port 8888)
            final CreateTunnel mtrTunnelConfig = new CreateTunnel.Builder()
                    .withProto(Proto.HTTP)
                    .withAddr(8888)
                    .build();
            final Tunnel mtrTunnel = ngrokClient.connect(mtrTunnelConfig);
            source.sendSuccess(() -> Component.literal("MTR Map: " + mtrTunnel.getPublicUrl()), false);

            // Open a Create tunnel (port 3876)
            final CreateTunnel createTunnelConfig = new CreateTunnel.Builder()
                    .withProto(Proto.HTTP)
                    .withAddr(3876)
                    .build();
            final Tunnel createTunnel = ngrokClient.connect(createTunnelConfig);
            source.sendSuccess(() -> Component.literal("Create Map: " + createTunnel.getPublicUrl()), false);
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error starting ngrok: " + e.getMessage()));
        }
    }

    static void stopNgrok(CommandSourceStack source) {
        try {
            ngrokClient.kill(); // Stop all active tunnels
            source.sendSuccess(() -> Component.literal("Ngrok tunnels stopped."), false);
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error stopping ngrok: " + e.getMessage()));
        }
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("ngrok")
                .requires(source -> source.hasPermission(2))  // Only operators can use this command
                .executes(context -> {  // Default action: start tunnels
                    runNgrok(context.getSource());
                    return 1;
                })
                .then(Commands.argument("action", StringArgumentType.word())
                        .executes(context -> {
                            String action = StringArgumentType.getString(context, "action");
                            if ("stop".equalsIgnoreCase(action)) {
                                stopNgrok(context.getSource());
                            } else {
                                context.getSource().sendFailure(Component.literal("Invalid argument. Use /ngrok stop"));
                            }
                            return 1;
                        }));
    }
}
