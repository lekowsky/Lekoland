package pl.skyrise.butelkomat.command;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.skyrise.butelkomat.Butelkomat;
import pl.skyrise.butelkomat.util.MessageUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainCommand implements CommandExecutor, TabCompleter {

    private final Butelkomat plugin;

    public MainCommand(Butelkomat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help" -> sendHelp(sender);

            case "reload" -> {
                if (!sender.hasPermission("butelkomat.admin")) {
                    msg(sender, "no-permission");
                    return true;
                }
                plugin.reload();
                msg(sender, "reload-success");
            }

            case "additem" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Tylko dla graczy!");
                    return true;
                }
                if (!player.hasPermission("butelkomat.admin")) {
                    plugin.getMessageUtil().send(player, "no-permission");
                    return true;
                }
                if (args.length < 2) {
                    plugin.getMessageUtil().send(player, "invalid-price");
                    return true;
                }

                double price;
                try {
                    price = Double.parseDouble(args[1]);
                } catch (NumberFormatException e) {
                    plugin.getMessageUtil().send(player, "invalid-price");
                    return true;
                }

                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand.getType() == Material.AIR) {
                    plugin.getMessageUtil().send(player, "item-not-in-hand");
                    return true;
                }

                ItemStack toSave = hand.clone();
                toSave.setAmount(1);

                plugin.addItem(toSave, price);

                plugin.getMessageUtil().send(player, "item-added",
                        MessageUtil.placeholders("{price}", String.format("%.2f", price)));
            }

            case "removeitem" -> {
                if (!sender.hasPermission("butelkomat.admin")) {
                    msg(sender, "no-permission");
                    return true;
                }
                if (args.length < 2) {
                    sendHelp(sender);
                    return true;
                }

                int index;
                try {
                    index = Integer.parseInt(args[1]) - 1;
                } catch (NumberFormatException e) {
                    msg(sender, "invalid-price");
                    return true;
                }

                if (plugin.removeItem(index)) {
                    msg(sender, "item-removed",
                            MessageUtil.placeholders("{index}", String.valueOf(index + 1)));
                } else {
                    sender.sendMessage(MessageUtil.colorize("&cNieprawidłowy numer!"));
                }
            }

            case "list" -> {
                if (!sender.hasPermission("butelkomat.admin")) {
                    msg(sender, "no-permission");
                    return true;
                }

                sender.sendMessage(MessageUtil.colorize(
                        plugin.getMessageUtil().getNoPrefix("item-list-header")));

                var items = plugin.getAcceptedItems();
                if (items.isEmpty()) {
                    sender.sendMessage(MessageUtil.colorize(
                            plugin.getMessageUtil().getNoPrefix("item-list-empty")));
                } else {
                    for (int i = 0; i < items.size(); i++) {
                        var item = items.get(i);
                        String entry = plugin.getMessageUtil().getNoPrefix("item-list-entry")
                                .replace("{index}", String.valueOf(i + 1))
                                .replace("{type}", item.itemStack.getType().name())
                                .replace("{price}", String.format("%.2f", item.price));
                        sender.sendMessage(MessageUtil.colorize(entry));
                    }
                }
            }

            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        var m = plugin.getMessageUtil();
        sender.sendMessage(MessageUtil.colorize(m.getNoPrefix("help-header")));
        sender.sendMessage(MessageUtil.colorize(m.getNoPrefix("help-additem")));
        sender.sendMessage(MessageUtil.colorize(m.getNoPrefix("help-removeitem")));
        sender.sendMessage(MessageUtil.colorize(m.getNoPrefix("help-list")));
        sender.sendMessage(MessageUtil.colorize(m.getNoPrefix("help-reload")));
        sender.sendMessage(MessageUtil.colorize(m.getNoPrefix("help-footer")));
    }

    private void msg(CommandSender sender, String key) {
        if (sender instanceof Player p) {
            plugin.getMessageUtil().send(p, key);
        } else {
            sender.sendMessage(MessageUtil.colorize(plugin.getMessageUtil().get(key)));
        }
    }

    private void msg(CommandSender sender, String key, Map<String, String> ph) {
        if (sender instanceof Player p) {
            plugin.getMessageUtil().send(p, key, ph);
        } else {
            sender.sendMessage(MessageUtil.colorize(plugin.getMessageUtil().get(key, ph)));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("help", "additem", "removeitem", "list", "reload"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("additem")) {
                completions.add("<cena>");
            } else if (args[0].equalsIgnoreCase("removeitem")) {
                for (int i = 1; i <= plugin.getAcceptedItems().size(); i++) {
                    completions.add(String.valueOf(i));
                }
            }
        }

        String last = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(last))
                .collect(Collectors.toList());
    }
}