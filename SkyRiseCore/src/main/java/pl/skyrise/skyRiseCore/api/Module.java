package pl.skyrise.skyRiseCore.api;

public interface Module {

    /**
     * Unikalna nazwa modułu, np. "AdminChat"
     */
    String getName();

    /**
     * Wywoływane przy starcie pluginu.
     * Tu rejestrujesz listenery, komendy, ładujesz config.
     */
    void onEnable();

    /**
     * Wywoływane przy wyłączaniu pluginu.
     * Tu zapisujesz dane, wyrejestrowujesz zasoby.
     */
    void onDisable();

    /**
     * Przeładowuje config modułu bez wyłączania go.
     * Domyślnie nic nie robi — nadpisz jeśli moduł używa configa.
     */
    default void onReload() {}
}
