package pl.skyrise.skyRiseCore.core;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import pl.skyrise.skyRiseCore.SkyRiseCore;
import pl.skyrise.skyRiseCore.api.ModuleManager;
import pl.skyrise.skyRiseCore.utils.ColorUtil;

import java.util.List;
import java.util.Set;

public class SkyRiseCoreCommand implements CommandExecutor {

    private final SkyRiseCore plugin;
    private final TabRegistry tabRegistry;

    public SkyRiseCoreCommand(SkyRiseCore plugin, TabRegistry tabRegistry) {
        this.plugin = plugin;
        this.tabRegistry = tabRegistry;

        tabRegistry.register("skyrisecore", (sender, args) -> {
            if (args.length == 1) {
                return TabRegistry.filter(List.of("reload", "list", "version"), args[0]);
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("reload")) {
                return TabRegistry.filter(plugin.getModuleManager().getModuleNames(), args[1]);
            }
            return List.of();
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendInfo(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender, args);
            case "list" -> handleList(sender);
            case "version" -> sendInfo(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    // --- Reload ---

    private void handleReload(CommandSender sender, String[] args) {
        ModuleManager mm = plugin.getModuleManager();

        if (args.length == 1) {
            mm.reloadAll();
            sender.sendMessage(ColorUtil.mini("<#99CCFF>»</#99CCFF> <green>Przeładowano wszystkie moduły <dark_gray>(<white>" + mm.getModuleCount() + "<dark_gray>)</dark_gray>."));
            return;
        }

        String moduleName = args[1].toLowerCase();
        if (mm.reload(moduleName)) {
            sender.sendMessage(ColorUtil.mini("<#99CCFF>»</#99CCFF> <green>Moduł <gold>" + moduleName + "</gold> przeładowany."));
        } else {
            sender.sendMessage(ColorUtil.mini("<red>»</red> <red>Moduł <gold>" + moduleName + "</gold> nie istnieje."));
            sender.sendMessage(ColorUtil.mini("<#99CCFF>»</#99CCFF> <#99CCFF>Dostępne: <white>" + String.join(", ", mm.getModuleNames())));
        }
    }

    // --- Lista modułów ---

    private void handleList(CommandSender sender) {
        Set<String> names = plugin.getModuleManager().getModuleNames();
        if (names.isEmpty()) {
            sender.sendMessage(ColorUtil.mini("<#99CCFF>»</#99CCFF> <#99CCFF>Brak załadowanych modułów."));
            return;
        }

        sender.sendMessage(ColorUtil.mini("<gold>Załadowane moduły <dark_gray>(<white>" + names.size() + "<dark_gray>)</dark_gray>:"));
        for (String name : names) {
            sender.sendMessage(ColorUtil.mini("  <green>✔ <white>" + name));
        }
    }

    // --- Informacje ---

    private void sendInfo(CommandSender sender) {
        sender.sendMessage(ColorUtil.mini("<dark_gray><strikethrough>                              "));
        sender.sendMessage(ColorUtil.mini("<gold><bold>SkyRiseCore <dark_gray>- <#99CCFF>v" + plugin.getDescription().getVersion()));
        sender.sendMessage(ColorUtil.mini("  <#99CCFF>Moduły: <white>" + plugin.getModuleManager().getModuleCount()));
        sender.sendMessage(ColorUtil.mini("  <#99CCFF>Autor: <white>" + String.join(", ", plugin.getDescription().getAuthors())));
        sender.sendMessage(ColorUtil.mini("<dark_gray><strikethrough>                              "));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.mini("<dark_gray><strikethrough>                              "));
        sender.sendMessage(ColorUtil.mini("<gold><bold>SkyRiseCore <dark_gray>- <#99CCFF>Pomoc"));
        sender.sendMessage(ColorUtil.mini("<dark_gray><strikethrough>                              "));
        sender.sendMessage(ColorUtil.mini("  <yellow>/src list              <dark_gray>- <#99CCFF>Lista modułów"));
        sender.sendMessage(ColorUtil.mini("  <yellow>/src reload            <dark_gray>- <#99CCFF>Przeładuj wszystko"));
        sender.sendMessage(ColorUtil.mini("  <yellow>/src reload <moduł>    <dark_gray>- <#99CCFF>Przeładuj moduł"));
        sender.sendMessage(ColorUtil.mini("  <yellow>/src version           <dark_gray>- <#99CCFF>Informacje"));
        sender.sendMessage(ColorUtil.mini("<dark_gray><strikethrough>                              "));
    }
}
