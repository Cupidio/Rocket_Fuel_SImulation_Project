import java.util.Objects;

public class StateEstimation {

    // init
    private Propellant simPropellant;
    private Combustion_test simCombustor;
    private Nozzle simNozzle;
    private ThermodynamicSolver solver;

    private final double ambientPressure;

    // Results
    private double contractionRatio;
    private double expansionRatio;
    private double entranceMach;
    private double designExitMach;
    private double designExitPressure;
    private double exitShockPressure;
    private NozzleRegime regime;
    private double shockUpstreamMach = Double.NaN; // NaN unless regime is SHOCK_IN_NOZZLE
    private double shockArea = Double.NaN;
    private double kn = Double.NaN;                // NaN unless built from a Propellant
    private double optimalAltitude;
    private FlowState exitState;
    private FlowState[] flowStates;
    private ThrustData performanceData;

    // use when chamber pressure given (testing the nozzle on its own)
    public StateEstimation(double gamma, double gasConstant, double chamberPressure,
            double chamberTemp, double ambientPressure,
            double entranceArea, double throatArea, double exitArea) {
        // The area-Mach solver needs A/A* > 1 on both sides of the throat
        if (throatArea <= 0 || entranceArea <= throatArea || exitArea <= throatArea) {
            throw new IllegalArgumentException(String.format(
                    "Invalid nozzle geometry: entrance (%.4f) and exit (%.4f) must both be larger than throat (%.4f) m^2",
                    entranceArea, exitArea, throatArea));
        }
        if (!(ambientPressure > 0)) {
            throw new IllegalArgumentException("Ambient pressure must be positive");
        }

        this.simPropellant = new Propellant();
        this.simCombustor = new Combustion_test();
        this.simNozzle = new Nozzle();

        this.solver = new ThermodynamicSolver(gamma, gasConstant, chamberPressure, chamberTemp);
        this.ambientPressure = ambientPressure;

        this.simNozzle.setEntranceArea(entranceArea);
        this.simNozzle.setThroatArea(throatArea);
        this.simNozzle.setExitArea(exitArea);

        //calcFinal() needs state from calcPrelim().
        calcPrelim();
        calcFinal();
    }

    // Chamber pressure solved from the propellant. Its area is the burning surface A_b
    public StateEstimation(Propellant propellant, double ambientPressure,
        double entranceArea, double throatArea, double exitArea) 
    {
        this(Objects.requireNonNull(propellant, "Propellant must not be null").getSpecificHeatRatio(),
                propellant.getGasConstant(), chamberPressureForSolver(propellant, throatArea), propellant.getFlameTemperature(),
                ambientPressure, entranceArea, throatArea, exitArea);
        this.simPropellant = propellant;
        this.kn = propellant.getPropellantArea()/throatArea;
    }

    private static double chamberPressureForSolver(Propellant p, double throatArea) {
        return ThermodynamicSolver.equilibriumChamberPressure(p.getPropellantArea(), throatArea,
                p.getDensity(), p.getBurnRateCoefficient(), p.getPressureExponent(),
                p.getSpecificHeatRatio(), p.getGasConstant(), p.getFlameTemperature());
    }

    private void calcPrelim() {
        double throatArea = simNozzle.getThroatArea();
        this.contractionRatio = simNozzle.getEntranceArea() / throatArea;
        this.expansionRatio = simNozzle.getExitArea() / throatArea;
        this.entranceMach = solver.solveMachFromAreaRatio(contractionRatio, false); // Subsonic
        this.designExitMach = solver.solveMachFromAreaRatio(expansionRatio, true); // Supersonic
        this.designExitPressure = solver.designExitPressure(expansionRatio);
        this.exitShockPressure = solver.exitShockPressure(expansionRatio);

        // Decide what the flow does before computing states (throws if the nozzle won't choke)
        this.regime = solver.classifyRegime(expansionRatio, ambientPressure);

        FlowState combustor = solver.computeIsentropicFlow("Combustor", 0.0);
        FlowState entrance = solver.computeIsentropicFlow("Entrance", entranceMach);
        FlowState throat = solver.computeIsentropicFlow("Throat", 1.0); // Choked

        if (regime == NozzleRegime.SHOCK_IN_NOZZLE) {
            // Subsonic exit at ambient pressure fixes the exit Mach and the post-shock total pressure
            double exitMach = solver.solveShockInNozzleExitMach(ambientPressure, throatArea, simNozzle.getExitArea());
            double exitTotalPressure = ambientPressure * solver.stagnationPressureRatio(exitMach);

            // The total pressure lost tells us the shock strength, which tells us where it sits
            this.shockUpstreamMach = solver.solveMachFromTotalPressureRatio(
                    exitTotalPressure / solver.getChamberPressure());
            this.shockArea = solver.areaRatioFromMach(shockUpstreamMach) * throatArea;

            FlowState preShock = solver.computeIsentropicFlow("Pre-Shock", shockUpstreamMach);
            FlowState postShock = solver.computeNormalShock("Post-Shock", preShock);
            this.exitState = solver.computeIsentropicFlow("Exit (subsonic)", exitMach, exitTotalPressure);
            this.flowStates = new FlowState[] { combustor, entrance, throat, preShock, postShock, exitState };
        } else {
            // Shock free inside; any mismaitch with ambient is resolved outside nozzle
            this.exitState = solver.computeIsentropicFlow("Exit", designExitMach);
            this.flowStates = new FlowState[] { combustor, entrance, throat, exitState };
        }

        simNozzle.stateOne = combustor.getPressure();
        simNozzle.stateTwo = entrance.getPressure();
        simNozzle.stateThree = throat.getPressure();
        simNozzle.stateFour = exitState.getPressure();
        simNozzle.stateFive = ambientPressure; // The exhaust jet ends up at ambient pressure
    }

    private void calcFinal() {
        double massFlow = solver.chokedMassFlow(simNozzle.getThroatArea());
        this.performanceData = solver.calculateThrust(massFlow, exitState, simNozzle.getExitArea(), ambientPressure);

        // Altitude where ambient pressure equals the design exit pressure
        this.optimalAltitude = StandardAtmosphere.altitudeForPressure(designExitPressure);
    }

    public void printReport() {
        System.out.println("=== COMBUSTION CHAMBER ===");
        if (simPropellant.getName() != null) {
            System.out.printf("Propellant:       %s (Kn = %.1f)\n", simPropellant.getName(), kn);
        }
        System.out.printf("Chamber Pressure: %,.0f Pa (%s)\n", solver.getChamberPressure(),
                Double.isNaN(kn) ? "given" : "solved");
        System.out.printf("Chamber Temp:     %.1f K\n", solver.getChamberTemp());
        System.out.printf("c*:               %.1f m/s\n\n", solver.getCharacteristicVelocity());

        System.out.println("=== NOZZLE ===");
        System.out.printf("Contraction Ratio: %.2f  -> Entrance Mach:    %.3f\n", contractionRatio, entranceMach);
        System.out.printf("Expansion Ratio:   %.2f  -> Design Exit Mach: %.3f\n", expansionRatio, designExitMach);
        System.out.printf("Back Pressure:     %,.0f Pa\n", ambientPressure);
        System.out.printf("Shock at exit if:  Pa >= %,.0f Pa\n", exitShockPressure);
        System.out.printf("Design exit P:     %,.0f Pa\n", designExitPressure);
        System.out.printf("Regime: %s - %s\n\n", regime, regime.getDescription());

        if (regime == NozzleRegime.SHOCK_IN_NOZZLE) {
            System.out.println("=== NORMAL SHOCK IN NOZZLE ===");
            System.out.printf("Upstream Mach: %.3f\n", shockUpstreamMach);
            System.out.printf("Shock Area:    %.4f m^2 (throat %.4f, exit %.4f)\n\n",
                    shockArea, simNozzle.getThroatArea(), simNozzle.getExitArea());
        }

        System.out.println("=== STATE ESTIMATION ===");
        for (FlowState state : flowStates) {
            state.printState();
        }

        double totalThrust = performanceData.getThrustForce();

        System.out.println("=== FINAL ENGINE PERFORMANCE METRICS ===");
        System.out.printf("Mass Flow Rate:   %.2f kg/s\n", performanceData.getMassFlowRate());
        System.out.printf("Total Thrust:     %,.0f N (%.2f kN)\n", totalThrust, totalThrust / 1000.0);
        System.out.printf("Specific Impulse: %,.1f s\n", performanceData.getIsp());

        if (Double.isNaN(optimalAltitude)) {
            System.out.println("Optimal Alt:      below sea level (design Pe above sea-level pressure)");
        } else if (Double.isInfinite(optimalAltitude)) {
            System.out.println("Optimal Alt:      above 11,000 m");
        } else {
            System.out.printf("Optimal Alt:      %,.0f meters\n", optimalAltitude);
        }
    }

    public FlowState[] getFlowStates() { 
        return flowStates; 
    }
    public FlowState getExitState() { 
        return exitState; 
    }
    public ThrustData getPerformanceData() { 
        return performanceData; 
    }
    public NozzleRegime getRegime() { 
        return regime; 
    }
    public double getChamberPressure() { 
        return solver.getChamberPressure(); 
    }
    public double getShockArea() { 
        return shockArea; 
    }
    public double getOptimalAltitude() { 
        return optimalAltitude; 
    }
}
