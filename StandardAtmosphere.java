/** US Standard Atmosphere, troposphere only (0 to 11 km, linear temperature lapse). */
public class StandardAtmosphere {
    private static final double P0 = 101325.0; // Sea level standard pressure (Pa)
    private static final double T0 = 288.15; // Sea level standard temperature (K)
    private static final double L = -0.0065; // Temperature lapse rate (K/m)
    private static final double G0 = 9.80665; // Standard gravity (m/s^2)
    private static final double R_AIR = 287.05;// Specific gas constant for air
    private static final double H_MAX = 11000.0; // Top of the troposphere (m)
    private static final double EXPONENT = -G0 / (R_AIR * L);

    public static double getSeaLevelPressure() {
        return P0;
    }

    public static double getStandardGravity() {
        return G0;
    }

    public static double pressureAt(double altitude) {
        if (altitude < 0 || altitude > H_MAX) {
            throw new IllegalArgumentException("Altitude must be between 0 and 11,000 m");
        }
        return P0 * Math.pow(1.0 + L * altitude / T0, EXPONENT);
    }

    public static double temperatureAt(double altitude) {
        if (altitude < 0 || altitude > H_MAX) {
            throw new IllegalArgumentException("Altitude must be between 0 and 11,000 m");
        }
        return T0 + L * altitude;
    }
    
    public static double altitudeForPressure(double pressure) {
        if (pressure > P0) {
            return Double.NaN;
        }
        double altitude = (T0 / L) * (Math.pow(pressure / P0, 1.0 / EXPONENT) - 1.0);
        return altitude > H_MAX ? Double.POSITIVE_INFINITY : altitude;
    }
}
