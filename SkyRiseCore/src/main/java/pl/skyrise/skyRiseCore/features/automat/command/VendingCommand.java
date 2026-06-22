package pl.skyrise.skyRiseCore.features.automat.command;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.skyrise.skyRiseCore.features.automat.AutomatModule;
import pl.skyrise.skyRiseCore.features.automat.gui.EditorGUI;
import pl.skyrise.skyRiseCore.features.automat.gui.MachineGUI;
import pl.skyrise.skyRiseCore.features.automat.model.MachinePlacement;
import pl.skyrise.skyRiseCore.features.automat.model.MachineTemplate;
import pl.skyrise.skyRiseCore.features.automat.model.VendingItem;
import pl.skyrise.skyRiseCore.features.automat.util.ColorUtil;

import java.util.*;
import java.util.stream.Collectors;

public class VendingCommand implements CommandExecutor, TabCompleter {

    private final AutomatModule plugin;

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "create", "delete", "edit", "open", "remove",
            "stock", "restock", "restocklist", "setmodel", "list", "reload", "info", "tp", "help"
    );

    public VendingCommand(AutomatModule plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender, label); return true; }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "edit" -> handleEdit(sender, args);
            case "open" -> handleOpen(sender, args);
            case "remove" -> handleRemove(sender);
            case "stock" -> handleStock(sender, args);
            case "restock" -> handleRestock(sender, args);
            case "restocklist" -> handleRestockList(sender);
            case "setmodel" -> handleSetModel(sender, args);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            case "info" -> handleInfo(sender, args);
            case "tp" -> handleTeleport(sender, args);
            case "help" -> sendHelp(sender, label);
            default -> sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&cNieznana komenda. /" + label + " help"));
        }
        return true;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vendingmachine.create")) { noPermission(sender); return; }
        if (args.length < 2) { sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&c/automat create <nazwa>")); return; }
        String name = args[1];
        MachineTemplate t = plugin.getMachineManager().createTemplate(name);
        if (t == null) { sender.sendMessage(plugin.getPrefix() + msg("exists").replace("{name}", name)); return; }
        sender.sendMessage(plugin.getPrefix() + msg("created").replace("{name}", name));
        if (sender instanceof Player p) new EditorGUI(plugin, t, p).open();
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vendingmachine.delete")) { noPermission(sender); return; }
        if (args.length < 2) { sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&c/automat delete <nazwa>")); return; }
        if (plugin.getMachineManager().deleteTemplate(args[1])) sender.sendMessage(plugin.getPrefix() + msg("deleted").replace("{name}", args[1]));
        else sender.sendMessage(plugin.getPrefix() + msg("not-found").replace("{name}", args[1]));
    }

    private void handleEdit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&cTylko gracze mogą użyć tej komendy.")); return; }
        if (!p.hasPermission("vendingmachine.edit")) { noPermission(p); return; }
        if (args.length < 2) { p.sendMessage(plugin.getPrefix() + ColorUtil.color("&c/automat edit <nazwa>")); return; }
        MachineTemplate t = plugin.getMachineManager().getTemplate(args[1]);
        if (t == null) { p.sendMessage(plugin.getPrefix() + msg("not-found").replace("{name}", args[1])); return; }
        new EditorGUI(plugin, t, p).open();
    }

    private void handleOpen(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&cTylko gracze mogą użyć tej komendy.")); return; }
        if (args.length < 2) { p.sendMessage(plugin.getPrefix() + ColorUtil.color("&c/automat open <nazwa>")); return; }
        MachineTemplate t = plugin.getMachineManager().getTemplate(args[1]);
        if (t == null) { p.sendMessage(plugin.getPrefix() + msg("not-found").replace("{name}", args[1])); return; }
        if (!t.isEnabled() && !p.hasPermission("vendingmachine.admin")) { p.sendMessage(plugin.getPrefix() + msg("disabled")); return; }
        if (!t.getPermission().isEmpty() && !p.hasPermission(t.getPermission())) { noPermission(p); return; }
        // Otwiera template-only (bez konkretnego placementu) - admin preview
        new MachineGUI(plugin, t, p, null).open();
    }

    private void handleRemove(CommandSender sender) {
        if (!(sender instanceof Player p)) { sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&cTylko gracze mogą użyć tej komendy.")); return; }
        if (!p.hasPermission("vendingmachine.delete")) { noPermission(p); return; }
        org.bukkit.block.Block target = p.getTargetBlockExact(5);
        if (target == null) { p.sendMessage(plugin.getPrefix() + ColorUtil.color("&cMusisz patrzeć na blok!")); return; }
        if (plugin.getPlacementManager().remove(target.getLocation())) p.sendMessage(plugin.getPrefix() + msg("removed"));
        else p.sendMessage(plugin.getPrefix() + ColorUtil.color("&cTu nie ma automatu!"));
    }

    /**
     * /automat stock info <szablon>           - info defaultów stocku w szablonie
     * /automat stock fill                     - wypełnia automat na który patrzysz
     */
    private void handleStock(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vendingmachine.admin")) { noPermission(sender); return; }
        if (args.length < 2) {
            sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&c/automat stock <info|fill> [szablon]"));
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "fill" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&cTylko gracze (musisz patrzeć na automat)."));
                    return;
                }
                org.bukkit.block.Block target = p.getTargetBlockExact(5);
                if (target == null) {
                    p.sendMessage(plugin.getPrefix() + ColorUtil.color("&cMusisz patrzeć na automat!"));
                    return;
                }
                MachinePlacement placement = plugin.getPlacementManager().getPlacement(target.getLocation());
                if (placement == null) {
                    p.sendMessage(plugin.getPrefix() + ColorUtil.color("&cTu nie ma automatu!"));
                    return;
                }
                plugin.getRestockManager().fillPlacement(placement);
                p.sendMessage(plugin.getPrefix() + ColorUtil.color(
                        "&fWypełniono zapasy automatu &e" + placement.getTemplateName() + "&f do maksimum!"));
            }
            case "info" -> {
                if (args.length < 3) {
                    sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&c/automat stock info <szablon>"));
                    return;
                }
                MachineTemplate t = plugin.getMachineManager().getTemplate(args[2]);
                if (t == null) { sender.sendMessage(plugin.getPrefix() + msg("not-found").replace("{name}", args[2])); return; }

                int placements = plugin.getPlacementManager().getPlacementCount(t.getName());
                sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&6&l--- Stock szablonu: " + t.getName() + " ---"));
                sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&7Aktywnych automatów: &e" + placements));
                sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&7Auto-restock: " + (t.isAutoRestockEnabled() ? "&aWł" : "&cWył") +
                        " &7(co " + t.getAutoRestockInterval() + " min, +" + t.getAutoRestockAmount() + " szt.)"));
                sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&7Itemy (defaulty):"));
                for (VendingItem item : t.getItems().values()) {
                    String stockInfo = item.isUnlimitedStock() ? "&a∞" : "&7max: &e" + item.getMaxStock();
                    sender.sendMessage(plugin.getPrefix() + ColorUtil.color("  &7" + item.getId() + ": " + stockInfo + " &7- &f" + item.getDisplayName()));
                }
            }
        }
    }

    /**
     * /automat restock <szablon> - natychmiastowy restock wszystkich automatów danego szablonu
     */
    private void handleRestock(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vendingmachine.admin")) { noPermission(sender); return; }
        if (args.length < 2) { sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&c/automat restock <szablon>")); return; }
        MachineTemplate t = plugin.getMachineManager().getTemplate(args[1]);
        if (t == null) { sender.sendMessage(plugin.getPrefix() + msg("not-found").replace("{name}", args[1])); return; }

        int count = plugin.getRestockManager().restockAllPlacementsForTemplate(t);
        plugin.getDataManager().savePlacements();
        sender.sendMessage(plugin.getPrefix() + ColorUtil.color(
                "&fWykonano restock &e" + count + " &fautomatu/ów typu &e" + t.getName() +
                        " &f(+" + t.getAutoRestockAmount() + " szt./item)"));
    }

    /**
     * /automat restocklist - lista automatów do uzupełnienia
     */
    private void handleRestockList(CommandSender sender) {
        if (!sender.hasPermission("vendingmachine.restock.notify") && !sender.hasPermission("vendingmachine.admin")) {
            noPermission(sender); return;
        }

        int threshold = plugin.getConfig().getInt("stock.low-stock-warning.threshold", 5);

        // Zbieramy listę automatów wymagających uzupełnienia
        List<String[]> emptyList = new ArrayList<>();   // {nr, template, loc, itemName, stockInfo}
        List<String[]> lowList = new ArrayList<>();

        int nr = 1;
        for (MachinePlacement placement : plugin.getPlacementManager().getAllPlacements()) {
            MachineTemplate t = plugin.getMachineManager().getTemplate(placement.getTemplateName());
            if (t == null) continue;

            Location loc = placement.getLocation();
            String locStr = loc.getWorld().getName() + " &7(&f" + loc.getBlockX() + "&7, &f" +
                    loc.getBlockY() + "&7, &f" + loc.getBlockZ() + "&7)";

            for (VendingItem item : t.getItems().values()) {
                if (item.isUnlimitedStock()) continue;
                int stock = placement.getStock(item.getId());

                if (stock == 0) {
                    emptyList.add(new String[]{
                            String.valueOf(nr++),
                            t.getName(),
                            locStr,
                            ColorUtil.color(item.getDisplayName()),
                            "&c0&7/&e" + item.getMaxStock()
                    });
                } else if (stock <= threshold) {
                    lowList.add(new String[]{
                            String.valueOf(nr++),
                            t.getName(),
                            locStr,
                            ColorUtil.color(item.getDisplayName()),
                            "&e" + stock + "&7/&e" + item.getMaxStock()
                    });
                }
            }
        }

        sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&6&l========= AUTOMATY DO UZUPELNIENIA ========="));

        if (emptyList.isEmpty() && lowList.isEmpty()) {
            sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&fWszystkie automaty mają wystarczająco zapasów!"));
            return;
        }

        if (!emptyList.isEmpty()) {
            sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&c&l--- WYPRZEDANE (" + emptyList.size() + ") ---"));
            for (String[] row : emptyList) {
                sender.sendMessage(plugin.getPrefix() + ColorUtil.color(
                        "&7#" + row[0] + " &8[&6" + row[1] + "&8] &f" + row[3] + " &8» " + row[4] + " &8- &7" + row[2]
                ));
            }
        }

        if (!lowList.isEmpty()) {
            sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&e&l--- NISKI STOCK (" + lowList.size() + ") ---"));
            for (String[] row : lowList) {
                sender.sendMessage(plugin.getPrefix() + ColorUtil.color(
                        "&7#" + row[0] + " &8[&6" + row[1] + "&8] &f" + row[3] + " &8» " + row[4] + " &8- &7" + row[2]
                ));
            }
        }

        sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&7Łącznie: &c" + emptyList.size() + " wyprzedanych &7| &e" +
                lowList.size() + " z niskim stockiem"));
    }

    private void handleSetModel(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&cTylko gracze mogą użyć tej komendy."));
            return;
        }
        if (!player.hasPermission("vendingmachine.edit") && !player.hasPermission("vendingmachine.admin")) {
            noPermission(player);
            return;
        }
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + ColorUtil.color("&c/automat setmodel <szablon>"));
            return;
        }
        MachineTemplate template = plugin.getMachineManager().getTemplate(args[1]);
        if (template == null) {
            player.sendMessage(plugin.getPrefix() + msg("not-found").replace("{name}", args[1]));
            return;
        }
        if (plugin.getNexoManager() == null || !plugin.getNexoManager().isNexoAvailable()) {
            player.sendMessage(plugin.getPrefix() + ColorUtil.color("&cNexo nie jest dostępne na serwerze."));
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            player.sendMessage(plugin.getPrefix() + ColorUtil.color("&cMusisz trzymać item/mebel Nexo w ręce."));
            return;
        }

        String nexoId = plugin.getNexoManager().idFromItem(hand);
        if (nexoId == null || nexoId.isBlank()) {
            player.sendMessage(plugin.getPrefix() + ColorUtil.color("&cPrzedmiot w ręce nie wygląda na item Nexo."));
            return;
        }

        template.setNexoFurnitureId(nexoId);
        plugin.getNexoManager().setMapping(nexoId, template.getName());
        plugin.getMachineManager().save();
        player.sendMessage(plugin.getPrefix() + ColorUtil.color("&fPowiązano model Nexo &e" + nexoId + " &fz szablonem &e" + template.getName() + "&f."));
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("vendingmachine.admin")) { noPermission(sender); return; }
        sender.sendMessage(plugin.getPrefix() + msg("list-header"));
        var templates = plugin.getMachineManager().getAllTemplates();
        if (templates.isEmpty()) { sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&7Brak szablonów.")); return; }
        int id = 1;
        for (MachineTemplate t : templates) {
            int placements = plugin.getPlacementManager().getPlacementCount(t.getName());
            sender.sendMessage(plugin.getPrefix() + msg("list-entry")
                    .replace("{id}", String.valueOf(id)).replace("{name}", t.getName())
                    .replace("{rows}", String.valueOf(t.getRows())).replace("{items}", String.valueOf(t.getItems().size()))
                    .replace("{placements}", String.valueOf(placements)));
            id++;
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("vendingmachine.admin")) { noPermission(sender); return; }
        plugin.getDataManager().flushQueuedSaves();
        plugin.reloadConfig();
        if (plugin.getNexoManager() != null) plugin.getNexoManager().loadMappings();
        plugin.getRestockManager().stopAll();
        plugin.getRestockManager().startAll();
        sender.sendMessage(plugin.getPrefix() + msg("reload"));
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vendingmachine.admin")) { noPermission(sender); return; }
        if (args.length < 2) { sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&c/automat info <nazwa>")); return; }
        MachineTemplate t = plugin.getMachineManager().getTemplate(args[1]);
        if (t == null) { sender.sendMessage(plugin.getPrefix() + msg("not-found").replace("{name}", args[1])); return; }
        int placements = plugin.getPlacementManager().getPlacementCount(t.getName());
        sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&6&l--- Info: " + t.getName() + " ---"));
        sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&7Tytuł: &f" + t.getTitle()));
        sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&7Rzędy: &f" + t.getRows()));
        sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&7Włączony: " + (t.isEnabled() ? "&aTak" : "&cNie")));
        sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&7Nexo ID: &f" + (t.getNexoFurnitureId() != null ? t.getNexoFurnitureId() : "Brak")));
        sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&7Przedmioty: &f" + t.getItems().size()));
        sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&7Aktywne automaty: &f" + placements));
        sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&7Auto-restock: " + (t.isAutoRestockEnabled() ? "&aCo " + t.getAutoRestockInterval() + " min (+" + t.getAutoRestockAmount() + ")" : "&cWył")));
    }

    private void handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&cTylko gracze mogą użyć tej komendy.")); return; }
        if (!p.hasPermission("vendingmachine.admin")) { noPermission(p); return; }
        if (args.length < 2) { p.sendMessage(plugin.getPrefix() + ColorUtil.color("&c/automat tp <nazwa>")); return; }
        var placements = plugin.getPlacementManager().getAllPlacements().stream()
                .filter(pl -> pl.getTemplateName().equalsIgnoreCase(args[1]))
                .collect(Collectors.toList());
        if (placements.isEmpty()) { p.sendMessage(plugin.getPrefix() + ColorUtil.color("&cBrak instancji szablonu &e" + args[1])); return; }
        p.teleport(placements.get(0).getLocation().clone().add(0.5, 1, 0.5));
        p.sendMessage(plugin.getPrefix() + ColorUtil.color("&fTeleportowano do automatu."));
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(plugin.getPrefix() + ColorUtil.color(msg("help-header")));
        sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&e/" + label + " create <nazwa> &7- Utwórz szablon"));
        sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&e/" + label + " delete <nazwa> &7- Usuń szablon"));
        sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&e/" + label + " edit <nazwa> &7- Edytuj szablon"));
        sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&e/" + label + " open <nazwa> &7- Otwórz podgląd"));
        sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&e/" + label + " remove &7- Usuń automat (patrz na blok)"));
        sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&e/" + label + " stock info <szablon> &7- Info o defaultach"));
        sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&e/" + label + " stock fill &7- Wypełnij automat (patrz na blok)"));
        sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&e/" + label + " restock <szablon> &7- Restock wszystkich automatów"));
        sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&e/" + label + " restocklist &7- Lista do uzupełnienia"));
        sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&e/" + label + " setmodel <szablon> &7- Powiąż item Nexo z szablonem"));
        sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&e/" + label + " info <nazwa> &7- Informacje"));
        sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&e/" + label + " list &7- Lista szablonów"));
        sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&e/" + label + " tp <nazwa> &7- Teleport do automatu"));
        sender.sendMessage(plugin.getPrefix() + ColorUtil.color("&e/" + label + " reload &7- Przeładuj"));
    }

    private void noPermission(CommandSender s) { s.sendMessage(plugin.getPrefix() + msg("no-permission")); }
    private String msg(String key) { return ColorUtil.color(plugin.getConfig().getString("messages." + key, "&c" + key)); }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return filterByPermission(sender, SUBCOMMANDS).stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase())).sorted().collect(Collectors.toList());
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "delete", "edit", "open", "info", "tp", "restock", "setmodel" -> {
                    return plugin.getMachineManager().getTemplateNames().stream()
                            .filter(n -> n.startsWith(args[1].toLowerCase())).sorted().collect(Collectors.toList());
                }
                case "stock" -> {
                    return Stream2.of("info", "fill").filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                }
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("stock") && args[1].equalsIgnoreCase("info")) {
            return plugin.getMachineManager().getTemplateNames().stream()
                    .filter(n -> n.startsWith(args[2].toLowerCase())).sorted().collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private List<String> filterByPermission(CommandSender sender, List<String> subs) {
        List<String> result = new ArrayList<>();
        for (String sub : subs) { if (hasPermissionForSub(sender, sub)) result.add(sub); }
        return result;
    }

    private boolean hasPermissionForSub(CommandSender sender, String sub) {
        return switch (sub) {
            case "create" -> sender.hasPermission("vendingmachine.create");
            case "delete", "remove" -> sender.hasPermission("vendingmachine.delete");
            case "edit", "setmodel" -> sender.hasPermission("vendingmachine.edit") || sender.hasPermission("vendingmachine.admin");
            case "open" -> sender.hasPermission("vendingmachine.use");
            case "restocklist" -> sender.hasPermission("vendingmachine.restock.notify") || sender.hasPermission("vendingmachine.admin");
            case "list", "reload", "info", "tp", "stock", "restock" -> sender.hasPermission("vendingmachine.admin");
            case "help" -> true;
            default -> sender.hasPermission("vendingmachine.use");
        };
    }

    // mała pomocnicza klasa żeby nie używać Arrays.asList().stream() przy małych listach
    private static class Stream2 {
        static java.util.stream.Stream<String> of(String... values) {
            return Arrays.stream(values);
        }
    }
}