public class ThermodynamicSolver {

    private static final double MAX_SUPERSONIC_MACH = 50.0; // Upper bracket for the Mach solvers
    private static final double DESIGN_TOLERANCE = 1e-3;    // |Pa - Pe| / Pe that counts as design

    private final double chamberTemp;
    private final double specificHeatRatio; // Gamma
    private final double gasConstant;
    private final double chamberPressure;

    public ThermodynamicSolver(double specificHeatRatio, double gasConstant, double chamberPressure,
            double chamberTemp)
    {
        if (!(specificHeatRatio > 1.0) || !(gasConstant > 0) || !(chamberPressure > 0) || !(chamberTemp > 0)) {
            throw new IllegalArgumentException("Need gamma > 1 and positive R, chamber pressure and chamber temperature");
        }
        this.specificHeatRatio = specificHeatRatio;
        this.gasConstant = gasConstant;
        this.chamberPressure = chamberPressure;
        this.chamberTemp = chamberTemp;
    }

    public double getChamberPressure() { return chamberPressure; }
    public double getChamberTemp() { return chamberTemp; }

    // Isentropic relations 

    public double stagnationTemperatureRatio(double machNumber) {
        return 1.0 + ((specificHeatRatio - 1.0) / 2.0) * machNumber * machNumber;
    }

    public double stagnationPressureRatio(double machNumber) {
        double gamma = specificHeatRatio;
        return Math.pow(stagnationTemperatureRatio(machNumber), gamma / (gamma - 1.0));
    }

    public double areaRatioFromMach(double machNumber) {
        double gamma = specificHeatRatio;
        double exponent = (gamma + 1.0) / (2.0 * (gamma - 1.0));
        return (1.0 / machNumber) * Math.pow((2.0 / (gamma + 1.0)) * stagnationTemperatureRatio(machNumber), exponent);
    }

    // Isentropic state using the chamber as the stagnation condition. 
    public FlowState computeIsentropicFlow(String stageName, double machNumber) {
        return computeIsentropicFlow(stageName, machNumber, chamberPressure);
    }

    // Isentropic state for a given total pressure (lower than the chamber's after a shock). 
    public FlowState computeIsentropicFlow(String stageName, double machNumber, double totalPressure) {
        double temp = chamberTemp / stagnationTemperatureRatio(machNumber);
        double pressure = totalPressure / stagnationPressureRatio(machNumber);
        double velocity = machNumber * Math.sqrt(specificHeatRatio * gasConstant * temp);
        return new FlowState(stageName, pressure, temp, machNumber, velocity, totalPressure);
    }

    public double solveMachFromAreaRatio(double areaRatio, boolean isSupersonic) {
        if (areaRatio < 1.0) {
            throw new IllegalArgumentException("Area ratio A/A* must be >= 1, got " + areaRatio);
        }
        double low = isSupersonic ? 1.0 : 1e-9;
        double high = isSupersonic ? MAX_SUPERSONIC_MACH : 1.0;

        // using bisection method cause area-Mach have no closed form answer
        double midMach = 1.0;
        for (int i = 0; i < 200 && high - low > 1e-12; i++) {
            midMach = (low + high) / 2.0;
            boolean machTooLow = isSupersonic
                    ? areaRatioFromMach(midMach) < areaRatio   // supersonic: A/A* rises with M
                    : areaRatioFromMach(midMach) > areaRatio;  // subsonic: A/A* falls with M
            if (machTooLow) low = midMach; else high = midMach;
        }
        return midMach;
    }

    //  Normal shock relations 

    //Post-shock Mach number M2. 
    public double normalShockMach(double m1) {
        double gamma = specificHeatRatio;
        double m1Sq = m1 * m1;
        return Math.sqrt(((gamma - 1.0) * m1Sq + 2.0) / (2.0 * gamma * m1Sq - (gamma - 1.0)));
    }

    // Static pressure ratio p2 / p1. 
    public double normalShockPressureRatio(double m1) {
        double gamma = specificHeatRatio;
        return 1.0 + (2.0 * gamma / (gamma + 1.0)) * (m1 * m1 - 1.0);
    }

    // Total pressure ratio P02 / P01 (the loss across the shock). 
    public double totalPressureRatio(double m1) {
        double gamma = specificHeatRatio;
        double m1Sq = m1 * m1;
        double a = Math.pow(((gamma + 1.0) * m1Sq) / ((gamma - 1.0) * m1Sq + 2.0), gamma / (gamma - 1.0));
        double b = Math.pow((gamma + 1.0) / (2.0 * gamma * m1Sq - (gamma - 1.0)), 1.0 / (gamma - 1.0));
        return a * b;
    }

    // Finds the Mach thatmakes given P02/P01 (bisection; the ratio falls as M1 rises).
    public double solveMachFromTotalPressureRatio(double ratio) {
        double low = 1.0;
        double high = MAX_SUPERSONIC_MACH;
        double mid = 1.0;
        for (int i = 0; i < 200 && high - low > 1e-12; i++) {
            mid = (low + high) / 2.0;
            if (totalPressureRatio(mid) > ratio) low = mid; else high = mid;
        }
        return mid;
    }

    public FlowState computeNormalShock(String stageName, FlowState preShockState) {
        double gamma = specificHeatRatio;
        double m1 = preShockState.getMachNumber();
        double m2 = normalShockMach(m1);
        double p2 = preShockState.getPressure() * normalShockPressureRatio(m1);

        // Total temperature (T0) stays constant across a normal shock
        double t2 = chamberTemp / stagnationTemperatureRatio(m2);
        double v2 = m2 * Math.sqrt(gamma * gasConstant * t2);

        // entropy rises across the shock
        double p02 = preShockState.getTotalPressure() * totalPressureRatio(m1);

        return new FlowState(stageName, p2, t2, m2, v2, p02);
    }

    // Nozzle operating regime

    // Exit pressure for fully supersonic, shock-free flow.
    public double designExitPressure(double expansionRatio) {
        return chamberPressure / stagnationPressureRatio(solveMachFromAreaRatio(expansionRatio, true));
    }

    // Back pressure that puts a normal shock exactly at the exit plane.
    public double exitShockPressure(double expansionRatio) {
        double exitMach = solveMachFromAreaRatio(expansionRatio, true);
        return designExitPressure(expansionRatio) * normalShockPressureRatio(exitMach);
    }

    public NozzleRegime classifyRegime(double expansionRatio, double backPressure) {
        // Back pressure above this and the throat never chokes: not a working rocket nozzle
        double chokingLimit = chamberPressure/stagnationPressureRatio(solveMachFromAreaRatio(expansionRatio, false));

        if (backPressure >= chokingLimit) {
            throw new IllegalArgumentException(String.format(
                    "Nozzle does not choke: ambient pressure (%,.0f Pa) must be below %,.0f Pa",
                    backPressure, chokingLimit));
        }

        double pDesign = designExitPressure(expansionRatio);
        if (backPressure >= exitShockPressure(expansionRatio)) {     
            return NozzleRegime.SHOCK_IN_NOZZLE;
        }
        if (Math.abs(backPressure - pDesign) <= DESIGN_TOLERANCE * pDesign) {
            return NozzleRegime.DESIGN;
        }
        if (backPressure > pDesign) {                                
            return NozzleRegime.OVEREXPANDED;
        }
        return NozzleRegime.UNDEREXPANDED;
    }

    
    // Exit Mach when a normal shock sits inside the nozzle. The exit is subsonic so Pe = Pb,
    // and mass conservation (P01 * At = P0e * A*e) gives a closed form (Anderson, Eq. 5.28).
    
    public double solveShockInNozzleExitMach(double backPressure, double throatArea, double exitArea) {
        double gamma = specificHeatRatio;
        double k = (chamberPressure * throatArea) / (backPressure * exitArea);
        double a = 1.0 / (gamma - 1.0);
        double c = (2.0 / (gamma - 1.0)) * Math.pow(2.0 / (gamma + 1.0), (gamma + 1.0) / (gamma - 1.0));
        return Math.sqrt(-a + Math.sqrt(a * a + c * k * k));
    }

    //  Combustion chamber 

    // Characteristic velocity c* indep of the nozzle.
    public static double calculateCharacteristicVelocity(double gamma, double gasConstant, double chamberTemp) {
        double gammaTerm = Math.pow(2.0 / (gamma + 1.0), (gamma + 1.0) / (gamma - 1.0));
        return Math.sqrt(gamma * gasConstant * chamberTemp) / (gamma * Math.sqrt(gammaTerm));
    }

    public double getCharacteristicVelocity() {
        return calculateCharacteristicVelocity(specificHeatRatio, gasConstant, chamberTemp);
    }

// steady staet    
    public static double equilibriumChamberPressure(double burningArea, double throatArea,
            double propellantDensity, double burnRateCoefficient, double pressureExponent,
            double gamma, double gasConstant, double chamberTemp) {
        if (!(burningArea > 0) || !(throatArea > 0) || !(propellantDensity > 0) || !(burnRateCoefficient > 0)) {
            throw new IllegalArgumentException("Burning area, throat area, density and burn rate coefficient must be positive");
        }
        // n >= 1: burn rate outgrows throat outflow, so there is no stable pressure
        if (!(pressureExponent >= 0) || !(pressureExponent < 1.0)) {
            throw new IllegalArgumentException("Pressure exponent n must satisfy 0 <= n < 1");
        }
        double cStar = calculateCharacteristicVelocity(gamma, gasConstant, chamberTemp);
        double kn = burningArea / throatArea;
        return Math.pow(kn * propellantDensity * burnRateCoefficient * cStar, 1.0 / (1.0 - pressureExponent));
    }

// Mass fllow and  thrust 

    // Choked mass flow through the throat. 
    public double chokedMassFlow(double throatArea) {
        return chamberPressure * throatArea / getCharacteristicVelocity();
    }

    public ThrustData calculateThrust(double massFlowRate, FlowState exitState, double exitArea,
            double ambientPressure) {
        double momentumThrust = massFlowRate * exitState.getVelocity();
        double pressureThrust = (exitState.getPressure() - ambientPressure) * exitArea;
        double totalThrust = momentumThrust + pressureThrust;
        double isp = totalThrust / (massFlowRate * StandardAtmosphere.getStandardGravity());
        return new ThrustData(totalThrust, exitState.getPressure(), massFlowRate, isp);
    }
}
