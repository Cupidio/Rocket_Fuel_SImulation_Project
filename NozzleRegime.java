/** How a choked converging-diverging nozzle is operating for a given back pressure. */
public enum NozzleRegime {
    SHOCK_IN_NOZZLE("Normal shock inside the diverging section, subsonic exit"),
    OVEREXPANDED("Exit pressure below ambient, oblique shocks outside the exit"),
    DESIGN("Ideally expanded (Pe = Pa)"),
    UNDEREXPANDED("Exit pressure above ambient, expansion fans outside the exit");

    private final String description;

    NozzleRegime(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
