public class Propellant {

    // All values in SI units
    private String name;
    private double propellantArea;      // Burning surface area A_b (m^2)
    private double density;             // kg/m^3
    private double burnRateCoefficient; // St. Robert's law a (m/s per Pa^n)
    private double pressureExponent;    // St. Robert's law n
    private double specificHeatRatio;   // Gamma of the combustion products
    private double flameTemperature;    // K
    private double gasConstant;         // Specific gas constant of the products, J/(kg*K)

    public String getName() { 
        return name; }
    public void setName(String name) { 
        this.name = name; }

    public double getPropellantArea() { 
        return propellantArea; }
    public void setPropellantArea(double propellantArea) { 
        this.propellantArea = propellantArea; }

    public double getDensity() { 
        return density; }
    public void setDensity(double density) { 
        this.density = density; }

    public double getBurnRateCoefficient() { 
        return burnRateCoefficient; }
    public void setBurnRateCoefficient(double burnRateCoefficient) { this.burnRateCoefficient = burnRateCoefficient; }

    public double getPressureExponent() { 
        return pressureExponent; }
    public void setPressureExponent(double pressureExponent) { 
        this.pressureExponent = pressureExponent; }

    public double getSpecificHeatRatio() { 
        return specificHeatRatio; }
    public void setSpecificHeatRatio(double specificHeatRatio) { this.specificHeatRatio = specificHeatRatio; }

    public double getFlameTemperature() { 
        return flameTemperature; }
    public void setFlameTemperature(double flameTemperature) { 
        this.flameTemperature = flameTemperature; }

    public double getGasConstant() { 
        return gasConstant; }
    public void setGasConstant(double gasConstant) { 
        this.gasConstant = gasConstant; }
}
