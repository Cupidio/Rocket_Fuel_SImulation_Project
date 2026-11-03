public class ThrustData {
    private double thrustForce;
    private double exitPressure;
    private double massFlowRate;
    private double isp;

    public ThrustData(double thrustForce, double exitPressure, double massFlowRate, double isp) {
        this.thrustForce = thrustForce;
        this.exitPressure = exitPressure;
        this.massFlowRate = massFlowRate;
        this.isp = isp;
    }

    public double getThrustForce() { 
        return thrustForce; 
    }
    public double getExitPressure() { 
        return exitPressure; 
    }
    public double getMassFlowRate() { 
        return massFlowRate; 
    }
    public double getIsp() {
        return isp;
    }
}
