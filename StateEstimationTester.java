public class StateEstimationTester {

    // true = also show StateEstimation's own printReport() output
    private static final boolean SHOW_INTERNAL_REPORT = false;

    // ---- Inputs: change these to test other engines ----
    private static final double GAMMA = 1.4;
    private static final double GAS_CONSTANT = 287.05;     // J/(kg*K)
    private static final double CHAMBER_PRESSURE = 2_000_000; // Pa
    private static final double CHAMBER_TEMP = 3000;       // K
    private static final double AMBIENT_PRESSURE = 101325.0; // Pa
    private static final double ENTRANCE_AREA = 0.15;      // m^2
    private static final double THROAT_AREA = 0.05;        // m^2
    private static final double EXIT_AREA = 0.2115;        // m^2

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        StateEstimation sim = new StateEstimation(
                GAMMA, GAS_CONSTANT, CHAMBER_PRESSURE, CHAMBER_TEMP, AMBIENT_PRESSURE,
                ENTRANCE_AREA, THROAT_AREA, EXIT_AREA);

        // Calculations already ran in the constructor; just read the results
        FlowState[] states = sim.getFlowStates();
        ThrustData thrust = sim.getPerformanceData();

        if (SHOW_INTERNAL_REPORT) {
            sim.printReport();
            System.out.println();
        }

        System.out.println("=== INPUTS ===");
        System.out.printf("gamma = %.3f, R = %.2f J/(kg*K)%n", GAMMA, GAS_CONSTANT);
        System.out.printf("Pc = %,.0f Pa, Tc = %.1f K, Pamb = %,.0f Pa%n", CHAMBER_PRESSURE, CHAMBER_TEMP, AMBIENT_PRESSURE);
        System.out.printf("Areas (m^2): entrance %.4f, throat %.4f, exit %.4f  (Ae/A* = %.3f, Ax/A* = %.3f)%n%n",
                ENTRANCE_AREA, THROAT_AREA, EXIT_AREA, ENTRANCE_AREA / THROAT_AREA, EXIT_AREA / THROAT_AREA);

        printFlowStates(states);
        printThrustData(thrust);
        runChecks(sim, states, thrust);
    }

    // ---------------- display ----------------

    private static void printFlowStates(FlowState[] states) {
        System.out.println("=== FLOW STATES ===");
        if (states == null) {
            System.out.println("(null)\n");
            return;
        }
        System.out.printf("%-16s %14s %10s %8s %12s%n", "Stage", "Pressure (Pa)", "Temp (K)", "Mach", "Vel (m/s)");
        System.out.println("-".repeat(64));
        for (int i = 0; i < states.length; i++) {
            FlowState s = states[i];
            if (s == null) {
                System.out.printf("%-16s (null)%n", "[" + i + "]");
                continue;
            }
            System.out.printf("%-16s %,14.0f %10.1f %8.3f %12.2f%n",
                    s.getStageName(), s.getPressure(), s.getTemperature(), s.getMachNumber(), s.getVelocity());
        }
        System.out.println();
    }

    private static void printThrustData(ThrustData t) {
        System.out.println("=== THRUST DATA ===");
        if (t == null) {
            System.out.println("(null)\n");
            return;
        }
        System.out.printf("Thrust:           %,.0f N (%.2f kN)%n", t.getThrustForce(), t.getThrustForce() / 1000.0);
        System.out.printf("Mass flow rate:   %.3f kg/s%n", t.getMassFlowRate());
        System.out.printf("Exit pressure:    %,.0f Pa%n", t.getExitPressure());
        System.out.printf("Specific impulse: %.1f s%n%n", t.getIsp());
    }

    // ---------------- checks ----------------

    private static void runChecks(StateEstimation sim, FlowState[] states, ThrustData t) {
        System.out.println("=== CHECKS ===");

        boolean statesOk = states != null && states.length == 5;
        check("Flow state array has 5 entries", statesOk);

        if (statesOk) {
            FlowState comb = states[0], ent = states[1], thr = states[2], exit = states[3];

            check("All flow state values are finite", allFinite(states));
            check("Stages are in expected order", stagesInOrder(states));
            check("Throat is choked (M = 1)", Math.abs(thr.getMachNumber() - 1.0) < 1e-6);
            check("Entrance is subsonic", ent.getMachNumber() > 0 && ent.getMachNumber() < 1);
            check("Exit is supersonic", exit.getMachNumber() > 1);
            check("Combustor state matches chamber inputs",
                    relClose(comb.getPressure(), CHAMBER_PRESSURE, 1e-9)
                            && relClose(comb.getTemperature(), CHAMBER_TEMP, 1e-9));
            check("Entrance Mach reproduces input area ratio (Ae/A*)",
                    relClose(areaRatioFromMach(ent.getMachNumber()), ENTRANCE_AREA / THROAT_AREA, 1e-4));
            check("Exit Mach reproduces input area ratio (Ax/A*)",
                    relClose(areaRatioFromMach(exit.getMachNumber()), EXIT_AREA / THROAT_AREA, 1e-4));
            check("Pressure drops combustor -> exit",
                    comb.getPressure() > ent.getPressure()
                            && ent.getPressure() > thr.getPressure()
                            && thr.getPressure() > exit.getPressure());
            check("Temperature drops combustor -> exit",
                    comb.getTemperature() > ent.getTemperature()
                            && ent.getTemperature() > thr.getTemperature()
                            && thr.getTemperature() > exit.getTemperature());

            if (t != null) {
                check("ThrustData exit pressure matches exit FlowState",
                        relClose(t.getExitPressure(), exit.getPressure(), 1e-6));
            }
        }

        check("ThrustData is not null", t != null);
        if (t != null) {
            check("Mass flow rate is finite and > 0", Double.isFinite(t.getMassFlowRate()) && t.getMassFlowRate() > 0);
            check("Thrust is finite and > 0", Double.isFinite(t.getThrustForce()) && t.getThrustForce() > 0);
            check("Isp is finite and > 0", Double.isFinite(t.getIsp()) && t.getIsp() > 0);
        }

        check("Results ready right after construction", states != null && t != null);
        check("Optimal altitude is finite", Double.isFinite(sim.getOptimalAltitude()));

        System.out.printf("%n%d passed, %d failed%n", passed, failed);
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("[PASS] " + name);
        } else {
            failed++;
            System.out.println("[FAIL] " + name);
        }
    }

    private static boolean allFinite(FlowState[] states) {
        for (FlowState s : states) {
            if (s == null
                    || !Double.isFinite(s.getPressure())
                    || !Double.isFinite(s.getTemperature())
                    || !Double.isFinite(s.getMachNumber())
                    || !Double.isFinite(s.getVelocity())) {
                return false;
            }
        }
        return true;
    }

    // The positional checks above (states[0] = combustor, etc.) rely on this order
    private static boolean stagesInOrder(FlowState[] states) {
        String[] expected = { "Combustor", "Entrance", "Throat", "Exit", "Exhaust" };
        for (int i = 0; i < expected.length; i++) {
            if (states[i] == null || states[i].getStageName() == null
                    || !states[i].getStageName().startsWith(expected[i])) {
                return false;
            }
        }
        return true;
    }

    // Isentropic area-Mach relation, computed independently of ThermodynamicSolver
    private static double areaRatioFromMach(double mach) {
        double g = GAMMA;
        double term = (2.0 / (g + 1.0)) * (1.0 + (g - 1.0) / 2.0 * mach * mach);
        return (1.0 / mach) * Math.pow(term, (g + 1.0) / (2.0 * (g - 1.0)));
    }

    private static boolean relClose(double a, double b, double tol) {
        return Math.abs(a - b) <= tol * Math.max(Math.abs(a), Math.abs(b));
    }
}
