package pl.skyrise.skyRiseCore.core;

import org.bukkit.command.CommandSender;

import java.util.*;
import java.util.function.BiFunction;

/**
 * Centralne repozytorium tab completionów.
 * Moduły rejestrują tu swoje propozycje — jeden TabCompleter obsługuje wszystko.
 */
public class TabRegistry {

    /** Komenda → funkcja zwracająca propozycje (sender, args) -> list */
    private final Map<String, BiFunction<CommandSender, String[], List<String>>> providers = new HashMap<>();

    /** Cache na czas jednego wywołania — unika filter() w wielu miejscach */
    private static final List<String> EMPTY = List.of();

    /**
     * Rejestruje tab provider dla komendy.
     * @param command np. "adminchat"
     * @param provider (sender, args) -> lista propozycji
     */
    public void register(String command, BiFunction<CommandSender, String[], List<String>> provider) {
        providers.put(command.toLowerCase(), provider);
    }

    /**
     * Wyrejestrowuje tab provider.
     */
    public void unregister(String command) {
        providers.remove(command.toLowerCase());
    }

    /**
     * Zwraca propozycje dla komendy.
     */
    public List<String> complete(String command, CommandSender sender, String[] args) {
        BiFunction<CommandSender, String[], List<String>> provider = providers.get(command.toLowerCase());
        return provider != null ? provider.apply(sender, args) : EMPTY;
    }

    /**
     * Wszystkie zarejestrowane komendy.
     */
    public Set<String> getRegisteredCommands() {
        return Collections.unmodifiableSet(providers.keySet());
    }

    // --- Pomocnicze metody dla modułów ---

    /**
     * Filtruje listę po inpucie gracza (case-insensitive).
     */
    public static List<String> filter(Collection<String> options, String input) {
        if (input.isEmpty()) return List.copyOf(options);
        String lower = input.toLowerCase();
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .toList();
    }
}
