public class Nozzle {
    
    public double stateOne;
    public double stateTwo;
    public double stateThree;
    public double stateFour;
    public double stateFive;

    // Physical attributes
    private double entranceArea;
    private double throatArea;
    private double exitArea;
    private double nozzleLength;

    public Nozzle() {
    }

    public double getEntranceArea() { 
        return entranceArea; 
    }
    public void setEntranceArea(double entranceArea) { 
        this.entranceArea = entranceArea; 
    }

    public double getThroatArea() { 
        return throatArea; 
    }
    public void setThroatArea(double throatArea) { 
        this.throatArea = throatArea; 
    }

    public double getExitArea() { 
        return exitArea; 
    }
    public void setExitArea(double exitArea) { 
        this.exitArea = exitArea; 
    }

    public double getNozzleLength() { 
        return nozzleLength; 
    }
    public void setNozzleLength(double nozzleLength) { 
        this.nozzleLength = nozzleLength; 
    }
}