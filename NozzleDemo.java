public class NozzleDemo {
    public static void main(String[] args) {

        double throatArea = Math.PI * Math.pow(0.0075, 2);          // 15 mm throat
        double entranceArea = 4.0 * throatArea;
        double exitArea = 8.0 * throatArea;
        double ambientPressure = StandardAtmosphere.pressureAt(0.0); // sea level

        Propellant knsu = new Propellant();
        knsu.setName("KNSU");
        knsu.setDensity(1841.0);
        knsu.setBurnRateCoefficient(8.26e-3 / Math.pow(1e6, 0.319));
        knsu.setPressureExponent(0.319);
        knsu.setSpecificHeatRatio(1.133);
        knsu.setFlameTemperature(1720.0);
        knsu.setGasConstant(8314.46 / 39.86);

        knsu.setPropellantArea(0.045);
        new StateEstimation(knsu, ambientPressure, entranceArea, throatArea, exitArea).printReport();

    }
}