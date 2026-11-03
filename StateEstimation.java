public class StateEstimation {

    // init
    private Propellant simPropellant;
    private Combustion_test simCombustor;
    private Nozzle simNozzle;
    private ThermodynamicSolver solver;

    // Constants
    // Sea level stuff this will be replaced with an atmo model (maybe)
    private double AMBIENT_PRESSURE = 101325.0;
    private double G0 = 9.80665;

    // Results 
    private double contractionRatio;
    private double expansionRatio;
    private double entranceMach;
    private double exitMach;
    private double optimalAltitude;
    private FlowState[] flowStates;
    private ThrustData performanceData;

    public StateEstimation(double gamma, double gasConstant, double chamberPressure,
            double chamberTemp, double ambientPressure,
            double entranceArea, double throatArea, double exitArea) {
        // The area-Mach solver needs A/A* > 1 on both sides of the throat
        if (throatArea <= 0 || entranceArea <= throatArea || exitArea <= throatArea) {
            throw new IllegalArgumentException(String.format(
                    "Invalid nozzle geometry: entrance (%.4f) and exit (%.4f) must both be larger than throat (%.4f) m^2",
                    entranceArea, exitArea, throatArea));
        }

        this.simPropellant = new Propellant();
        this.simCombustor = new Combustion_test();
        this.simNozzle = new Nozzle();

        this.solver = new ThermodynamicSolver(gamma, gasConstant, chamberPressure, chamberTemp);
        this.AMBIENT_PRESSURE = ambientPressure;

        this.simNozzle.setEntranceArea(entranceArea);
        this.simNozzle.setThroatArea(throatArea);
        this.simNozzle.setExitArea(exitArea);

        // Order matters: calcFinal() needs the exit Mach from calcPrelim().
        calcPrelim();
        calcFinal();
    }

    // public static void main(String[] args) {
    //     // This acts as the external code passing inputs into the constructor for
    //     // testing
    //     StateEstimation simulation = new StateEstimation(
    //             1.4, 287.05, 2_000_000, 3000, 101325.0, 0.15, 0.05, 0.2115);

    //     FlowState[] states = simulation.getFlowStates();
    //     ThrustData performance = simulation.getPerformanceData();

    //     simulation.printReport();
    // }

    private void calcPrelim() {
        this.contractionRatio = simNozzle.getEntranceArea() / simNozzle.getThroatArea();
        this.expansionRatio = simNozzle.getExitArea() / simNozzle.getThroatArea();
        this.entranceMach = solver.solveMachFromAreaRatio(contractionRatio, false); // Should be Subsonic
        this.exitMach = solver.solveMachFromAreaRatio(expansionRatio, true); // Should be Supersonic
        double throatMach = 1.0; // Choked flow assumption

        FlowState combustor = solver.computeIsentropicFlow("Combustor", 0.0);
        FlowState entrance = solver.computeIsentropicFlow("Entrance", entranceMach);
        FlowState throat = solver.computeIsentropicFlow("Throat", throatMach);
        FlowState exit = solver.computeIsentropicFlow("Exit", exitMach);

        // This is incorrect as there are no shock relations added yet
        FlowState exhaust = new FlowState(
                "Exhaust (Plume)",
                AMBIENT_PRESSURE,
                exit.getTemperature(),
                exit.getMachNumber(),
                exit.getVelocity());

        this.flowStates = new FlowState[] { combustor, entrance, throat, exit, exhaust };

        // may not be needed for this section will depend on how PreLim is created
        simNozzle.stateOne = combustor.getPressure();
        simNozzle.stateTwo = entrance.getPressure();
        simNozzle.stateThree = throat.getPressure();
        simNozzle.stateFour = exit.getPressure();
        simNozzle.stateFive = exhaust.getPressure();

        // Broken need to fix this
        // simCombustor.stateOne = combustor.getPressure();
    }

    private void calcFinal() {
        this.performanceData = solver.calculateThrustState(simNozzle.getThroatArea(), this.exitMach, AMBIENT_PRESSURE);

        double exitPressure = this.performanceData.getExitPressure();

        double tempLapseRate = -0.0065;
        double t0 = 288.15;
        double exponent = -(287.05 * tempLapseRate) / G0;
        this.optimalAltitude = (t0 / tempLapseRate) * (Math.pow((exitPressure / AMBIENT_PRESSURE), exponent) - 1.0);
    }

    public void printReport() {
        System.out.println("=== NOZZLE KINEMATICS ===");
        System.out.printf("Contraction Ratio: %.2f  -> Entrance Mach: %.3f\n", contractionRatio, entranceMach);
        System.out.printf("Expansion Ratio:   %.2f  -> Exit Mach:     %.3f\n\n", expansionRatio, exitMach);

        System.out.println("=== PRELIMINARY STATE ESTIMATION ===");
        for (FlowState state : flowStates) {
            state.printState();
        }

        double totalThrust = performanceData.getThrustForce();

        System.out.println("=== FINAL ENGINE PERFORMANCE METRICS ===");
        System.out.printf("Mass Flow Rate: %.2f kg/s\n", performanceData.getMassFlowRate());
        System.out.printf("Total Thrust:   %,.0f N (%.2f kN)\n", totalThrust, totalThrust / 1000.0);
        System.out.printf("Specific Impulse:%,.1f s\n", performanceData.getIsp());

        if (optimalAltitude > 11000) {
            System.out.printf("Optimal Alt:    > 11,000 m (Pe = %,.0f Pa)\n", performanceData.getExitPressure());
        } else {
            System.out.printf("Optimal Alt:    %,.0f meters\n", optimalAltitude);
        }
    }

    public FlowState[] getFlowStates() {
        return this.flowStates;
    }

    public ThrustData getPerformanceData() {
        return this.performanceData;
    }

    public double getOptimalAltitude() {
        return this.optimalAltitude;
    }

    // TODO Add shock relations

}
