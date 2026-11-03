public class FlowState {
    private final String stageName;
    private final double pressure;
    private final double temperature;
    private final double machNumber;
    private final double velocity;
    private final double totalPressure; // Stagnation pressure (Pa); NaN if not tracked

    public FlowState(String stageName, double pressure, double temperature, double machNumber, double velocity) {
        this(stageName, pressure, temperature, machNumber, velocity, Double.NaN);
    }

    public FlowState(String stageName, double pressure, double temperature, double machNumber,
            double velocity, double totalPressure) {
        this.stageName = stageName;
        this.pressure = pressure;
        this.temperature = temperature;
        this.machNumber = machNumber;
        this.velocity = velocity;
        this.totalPressure = totalPressure;
    }

    public double getPressure() {
        return pressure; }
    public double getTemperature() {
        return temperature; }
    public double getMachNumber() {
        return machNumber; }
    public double getVelocity() {
        return velocity; }
    public String getStageName() {
        return stageName; }
    public double getTotalPressure() {
        return totalPressure; }

    public void printState() {
        System.out.printf("--- %s ---\n", stageName);
        System.out.printf("Pressure:    %,.0f Pa\n", pressure);
        if (!Double.isNaN(totalPressure)) {
            System.out.printf("Total Press: %,.0f Pa\n", totalPressure);
        }
        System.out.printf("Temperature: %.1f K\n", temperature);
        System.out.printf("Mach Number: %.2f\n", machNumber);
        System.out.printf("Velocity:    %.2f m/s\n\n", velocity);
    }
}
