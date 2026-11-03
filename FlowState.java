public class FlowState {
    private final String stageName;
    private final double pressure;
    private final double temperature;
    private final double machNumber;
    private final double velocity;

    public FlowState(String stageName, double pressure, double temperature, double machNumber, double velocity) {
        this.stageName = stageName;
        this.pressure = pressure;
        this.temperature = temperature;
        this.machNumber = machNumber;
        this.velocity = velocity;
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

    public void printState() {
        System.out.printf("--- %s ---\n", stageName);
        System.out.printf("Pressure:    %,.0f Pa\n", pressure);
        System.out.printf("Temperature: %.1f K\n", temperature);
        System.out.printf("Mach Number: %.2f\n", machNumber);
        System.out.printf("Velocity:    %.2f m/s\n\n", velocity);
    }
}