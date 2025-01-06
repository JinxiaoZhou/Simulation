import java.util.*;
import java.io.*;

/*
* CSC 270 simulation example
* Adapted January 1998 by J. Clarke from a c++ version, itself based on a
* Turing original by M. Molle
*
* Sim: the class in charge of the simulation.
* This class contains both the main() method for the application, and the
* "global" variables controlling the execution.
*/

public class Sim {
  // "Global" quantities used throughout the simulation
  public static double simulationTime; // What time is it?
  public static double reportInterval; // How often should we report?

  // quantities that determine how we model the real world
  // In a more elaborate program, these might be input data.

  // economics: profit per litre of gas, and cost to operate one pump for a day
  public static double profit = 0.025;
  public static double pumpCost = 20;

  // demand; minimum and maximum amount of gas needed by a car
  // See Car constructor.
  public static double litresNeededMin = 10;
  public static double litresNeededRange = 50;

  // service times: constant base time+ time per litre+ random spread
  // See Pump.serviceTime().
  public static double serviceTimeBase = 150;
  public static double serviceTimePerLitre = 0.5;
  public static double serviceTimeSpread = 30;

  // customer behavior probability of balking depends on three
  // ad-hoc constants. See Arrival.doesCarBalk().
  public static double balkA = 40;
  public static double balkB = 25;
  public static double balkC = 3;

  // customer arrival rate
  // See Arrival.interarrivalTime().
  public static double meanInterarrivalTime = 50; // seconds

  // random-number streams used to model the world
  public static Random arrivalStream; // auto arrival times
  public static Random litreStream; // number of litres needed
  public static Random balkingStream; // balking probability
  public static Random serviceStream; // service times

  // major data structures
  public static EventList eventList;
  public static CarQueue carQueue;
  public static PumpStand pumpStand;
  public static Statistics stats;

  /**
   * main entrypoint - starts the application
   * 
   * @param args java.lang.String[]
   */
  public static void main(java.lang.String[] args) throws IOException {
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    // Read data and print introduction.
    reportInterval = new Double(in.readLine()).doubleValue();
    double endingTime = new Double(in.readLine()).doubleValue();
    int numPumps = Integer.parseInt(in.readLine());
    System.out.print("This simulation run uses " + numPumps + " pumps");

    // Initialize the random-nwnber streams.
    System.out.println(" and the following random number seeds:");
    int seed = Integer.parseInt(in.readLine());
    arrivalStream = new Random(seed);
    System.out.print("       " + seed);
    seed = Integer.parseInt(in.readLine());
    litreStream = new Random(seed);
    System.out.print("        " + seed);
    seed = Integer.parseInt(in.readLine());
    balkingStream = new Random(seed);
    System.out.print("         " + seed);
    seed = Integer.parseInt(in.readLine());
    serviceStream = new Random(seed);
    System.out.print("        " + seed);
    System.out.println("");

    // Create and initialize the event list,the car queue, the pump stand,
    // and the statistics collector.
    eventList = new EventList();
    carQueue = new CarQueue();
    pumpStand = new PumpStand(numPumps);
    stats = new Statistics();

    // Schedule the required events:
    // the end of the simulation;
    // the first progress report;
    // the arrival of the first car.

    EndOfSimulation lastEvent = new EndOfSimulation(endingTime);
    eventList.insert(lastEvent);
    if (reportInterval <= endingTime) {
      Report nextReport = new Report(reportInterval);
      eventList.insert(nextReport);
    }
    eventList.insert(new Arrival(0));
    // (Should the first car really arrive at time 0?)

    // The"clock driver"loop
    while (true) {
      Event currentEvent = eventList.takeNextEvent();
      simulationTime = currentEvent.getTime();
      currentEvent.makeItHappen();
      if (currentEvent instanceof EndOfSimulation)
        break;
    }
  }
}

/**
 * Statistics: the class for objects that collect statistics.
 * (There is only one such object in this program.)
 */
class Statistics {
  // The explicit initializations are not needed, but improve clarity.
  private int totalArrivals = 0;
  private int customersServed = 0;
  private int balkingCustomers = 0;
  private double totalLitresSold = 0.0;
  private double totalLitresMissed = 0.0;
  private double totalWaitingTime = 0.0;
  private double totalServiceTime = 0.0;

  /**
   * Constructor.
   */
  public Statistics() {
    printHeaders();
  }

  /**
   * accumBalk: record and count a lost sale.
   * 
   * @param litres double
   */
  public void accumBalk(double litres) {
    balkingCustomers += 1;
    totalLitresMissed += litres;
  }

  /**
   * accumSale: record and count a sale.
   * 
   * @param litres double
   */
  public void accumSale(double litres) {
    customersServed += 1;
    totalLitresSold += litres;
  }

  /**
   * accumServiceTime: record a customer's service time.
   * 
   * @param interval double
   */
  public void accumServiceTime(double interval) {
    totalServiceTime += interval;
  }

  /**
   * accumWaitingTime: record a customer's waiting time.
   * 
   * @param interval double
   */
  public void accumWaitingTime(double interval) {
    totalWaitingTime += interval;
  }

  /**
   * countArrival: record an arrival
   */

  public void countArrival() {
    totalArrivals += 1;
  }

  /**
   * fmtDbl: convert a double to a string of a specified width representing
   * the number rounded to the specified nwnber of digits. The string
   * returned is padded by blanks on the left if necessary. If it is too long,
   * it is not changed. If it is out of range for the 11 int 11 type, strange
   * results will be returned.
   * 
   * @return java.lang.String
   * @param number    double
   * @param width     int
   * @param precision int
   */

  private static String fmtDbl(double number, int width, int precision) {
    // round and convert to string without decimal point
    double scale = 1;
    for (int i = 0; i < precision; i++)
      scale *= 10;
    String result = "" + (int) (number * scale + 0.5);

    // insert decimal point and leading zero if necessary
    if (precision > 0) {
      for (int i = result.length(); i < precision + 1; i++)
        result = "0" + result;
      int insertPos = result.length() - precision;
      // where the decimal point goes
      result = result.substring(0, insertPos) + "."
          + result.substring(insertPos);
    }

    // pad with blanks if necessary
    for (int i = result.length(); i < width; i++)
      result = " " + result;
    return result;
  }

  /**
   * fmtInt: convert an int to a string of a specified width.
   * The string returned is padded by blanks on the left if necessary.
   * If it is too long, it is not changed.
   * 
   * @return java.lang.String
   * @param number int
   * @param width  int
   */

  private static String fmtInt(int number, int width) {
    String result = "" + number;
    for (int i = result.length(); i < width; i++)
      result = " " + result;
    return result;
  }

  /**
   * printHeaders: print column titles for the statistics summaries.
   */
  private static void printHeaders() {
    System.out.println(" Current   Total NoQueue  Car->Car Averaae Number Average  Pump  Total  Lost ");
    System.out.println(" Time      Cars  Fraction  Time    Litres  Balked  Wait  Usage Profit Profit ");
    for (int i = 0; i < 79; i++)
      System.out.print("-");
    System.out.println("");
  }

  /**
   * snapshot: print a summary of the statistics so far.
   */
  public void snapshot() {
    System.out.print(fmtDbl(Sim.simulationTime, 8, 0));
    System.out.print(fmtInt(totalArrivals, 7));
    System.out.print(fmtDbl(Sim.carQueue.getEmptyTime() / Sim.simulationTime, 8, 3));

    if (totalArrivals > 0) {
      System.out.print(fmtDbl(Sim.simulationTime / totalArrivals, 9, 3));
      System.out.print(fmtDbl((totalLitresSold + totalLitresMissed) / totalArrivals, 8, 3));
    } else
      System.out.print("Unknown Unknown");

    System.out.print(fmtInt(balkingCustomers, 7));
    if (customersServed > 0)
      System.out.print(fmtDbl(totalWaitingTime / customersServed, 9, 3));
    else
      System.out.print("Unknown");

    System.out.print(fmtDbl(totalServiceTime
        / (Sim.pumpStand.getNumberOfPumps() * Sim.simulationTime), 7, 3));
    System.out.print(fmtDbl(totalLitresSold * Sim.profit
        - Sim.pumpCost * Sim.pumpStand.getNumberOfPumps(), 9, 2));
    System.out.print(fmtDbl(totalLitresMissed * Sim.profit, 7, 2));
    System.out.println("");
  }

}

/**
 * Cars the class representing cars
 */
class Car {

  private double arrivalTime;
  private double litresNeeded;

  /**
   * constructor
   * The nwnber of litres required is a property of a car, so it belongs in
   * this class. It is also something-the car"knows 11 when it arrives, so it
   * should be calculated in the constructor
   *
   * The distribution of litres required is uniform between 10 and 60
   */
  public Car() {
    litresNeeded = Sim.litresNeededMin + Sim.litreStream.nextDouble() * Sim.litresNeededRange;
  }

  /**
   * getArrivalTime: return the car's arrival time.
   * 
   * @return double.
   */
  public double getArrivalTime() {
    return arrivalTime;
  }

  /**
   * getLitresNeeded: return the number of litres of fuel needed by the car.
   * 
   * @return double
   */
  public double getLitresNeeded() {
    return litresNeeded;
  }

  /**
   * setArrivalTime: set the car's arrival time.
   * 
   * @param time double
   */
  public void setArrivalTime(double time) {
    arrivalTime = time;
  }
}

/**
 * CarQueue: the class representing the lineup of cars at the gas station
 */

class CarQueue {
  // Queueitem: the class for objects stored in the car queue.
  private class Queueitem {
    // The car queue is a linked list, so each item contains a data field
    // and a "next item" field. This is just a simple record structure, so
    // we'll allow outsiders to access the fields directly instead of using
    // get and set methods.
    public Car data;
    public Queueitem next;
  }

  private Queueitem firstWaitingCar;
  private Queueitem lastWaitingCar;
  private int queueSize;
  private double totalEmptyQueueTime;

  /**
   * Constructor.
   */
  public CarQueue() {
    firstWaitingCar = null;
    lastWaitingCar = null;
    queueSize = 0;
    totalEmptyQueueTime = 0;
  }

  /**
   * getEmptyTime: return the total time the car queue has been empty.
   * 
   * @return double
   */
  public double getEmptyTime() {
    if (queueSize > 0)
      return totalEmptyQueueTime;
    else
      return totalEmptyQueueTime + Sim.simulationTime;
  }

  /**
   * getQueueSize: return the number of cars in the car queue.
   * 
   * @return int
   */
  public int getQueueSize() {
    return queueSize;
  }

  /**
   * insert: put a newly-arrived car into the car queue.
   * 
   * @param newestcar aim.car
   */

  public void insert(Car newestCar) {
    Queueitem item = new Queueitem();
    item.data = newestCar;
    item.next = null;

    if (lastWaitingCar == null) {
      // the queue is empty
      firstWaitingCar = item;
      totalEmptyQueueTime += Sim.simulationTime;
    } else {
      // the queue already had at least one car in it
      lastWaitingCar.next = item;
    }

    lastWaitingCar = item;
    queueSize += 1;
  }

  /**
   * takeFirstCar: remove first car from car queue and return it.
   * 
   * @return aim.car
   */
  public Car takeFirstCar() {
    // precondition: queueSize > O && firstWaitingCar l= null
    if (queueSize <= 0 || firstWaitingCar == null) {
      System.out.println(" Errorl car queue unexpectedly empty");
      return null;
    }

    Car carToReturn = firstWaitingCar.data;
    queueSize--;
    firstWaitingCar = firstWaitingCar.next;

    if (firstWaitingCar == null) {
      // empty queue; update the end of the queue, and start
      // counting empty queue time
      lastWaitingCar = null;
      totalEmptyQueueTime -= Sim.simulationTime;
    }
    return carToReturn;
  }

}

/**
 * Pump: the class representing single pumps at the gas station .
 */

class Pump {
  private Car carinService;

  /**
   * getCarinservice; return the car currently being served by the pump.
   * 
   * @return aim.Car
   */
  public Car getCarinService() {
    return carinService;
  }

  /**
   * serviceTime: determine how long the service will take.
   * This is a property of the pump-car combination1 so the method could have
   * been in the Car class if the appropriate information were available there .
   *
   * Service times have a normal distribution with a mean given by a constant
   * base plus an amount of time per litre1 and with a fixed standard
   * deviation.
   * 
   * @return double
   */

  private double serviceTime() {
    if (carinService == null) {
      System.out.println("Error! no car in service when expected");
      return -1.0;
    }

    return Sim.serviceTimeBase
        + Sim.serviceTimePerLitre * carinService.getLitresNeeded()
        + Sim.serviceTimeSpread * Sim.serviceStream.nextGaussian();
  }

  /**
   * startService: the start-of-service event routine.
   * Connects the car to this pump, and dete:cmines when the service will stop.
   * 
   * @param car aim.Car
   */

  public void startService(Car car) {
    // precondition: Sim.pumpStand.aPumpisAvailable(),
    // Match the auto to an available pump.
    carinService = car;
    final double pumpTime = serviceTime();

    // Collect statistics.
    Sim.stats.accumWaitingTime(Sim.simulationTime - carinService.getArrivalTime());
    Sim.stats.accumServiceTime(pumpTime);

    // Schedule departure of car from this pump.
    Departure dep = new Departure(Sim.simulationTime + pumpTime);
    dep.setPump(this);
    Sim.eventList.insert(dep);
  }

}

/**
 * PumpStand: the class for the complete collection of pumps at the gas station.
 */

class PumpStand {
  private Pump[] pumps; // an array of pumps
  private int numPumps;
  private int topPump;

  /**
   * Constructor: build a PumpStand of nwnPumps pumps, and make all of them
   * available.
   * 
   * @param numPumps int
   */

  public PumpStand(int numPumps) {
    if (numPumps < 1) {
      System.out.println("Errorl pump stand needs more than 0 pumps ");
      return;
    }

    pumps = new Pump[numPumps];
    this.numPumps = numPumps;
    topPump = numPumps - 1;
    for (int p = 0; p < numPumps; p++)
      pumps[p] = new Pump();
  }

  /**
   * aPwnpisAvailable: return true/false aqcording to whether at least one
   * pump is free for use.
   * 
   * @return boolean
   */
  public boolean aPumpIsAvailable() {
    return topPump >= 0;
  }

  /**
   * getNumberOfPumps: return the number of pumps in the pump stand.
   * (This method is needed when statistics are gathered.)
   * 
   * @return int
   */
  public int getNumberOfPumps() {
    return numPumps;
  }

  /**
   * releasePump: put pump p back in the stock of available pumps.
   * 
   * @param p sim.Pump
   */
  public void releasePump(Pump p) {
    if (topPump >= numPumps) {
      System.out.println("Error! attempt to release a free pump? ");
      return;
    }
    pumps[++topPump] = p;
  }

  /**
   * takeAvailablePump: take a pump from the set of free pumps, and return that
   * pump.
   * 
   * @return aim.Pump
   */
  public Pump takeAvailablePump() {
    if (topPump < 0) {
      System.out.println("Error no pump available when needed");
      return null;
    }
    return pumps[topPump--];
  }

}

/*
 * Event: the class representing events within the simulation model.
 *
 * Remember that events are not entities in the same sense as cars and pumps
 * are, and the event queue does not have the same reality as the car queue.
 * The event queue is a data structure without a real-world equivalent, while
 * the car queue is real and you can see it. Events are not quite so imaginary,
 * but they are certainly less visible than cars.
 */

abstract class Event {
  private double time; // the time when the event happens

  /**
   * constructor.
   * 
   * @param time double
   */
  public Event(double time) {
    this.time = time;
  }

  /**
   * getTime: return the time of the event.
   * 
   * @return double
   */
  public double getTime() {
    return time;
  }

  /**
   * makeItHappen: the event routine.
   */
  public abstract void makeItHappen();

  /**
   * setTime: set the time of the event.
   * 
   * @param time double
   */

  public void setTime(double time) {
    this.time = time;
  }

}

/**
 * EventList: the class for the event list.
 * (There is only one object of this class in the program.)
 */

class EventList {

  // Listltem: the class for objects stored in the event list.
  private class ListItem {
    // The event list is a linked list, so each item contains a data field
    // and a 11 next item11 field. This is just a simple record structure, so
    // we'll allow outsiders to access the fields directly instead of using
    // get and set methods.
    public Event data;
    public ListItem next;
  }

  ListItem firstEvent;

  /**
   * Constructor
   */
  public EventList() {
    firstEvent = null; // happens automatically, but done explicitly
    // here to clarify the " empty list " state.
  }

  /**
   * insert: add an event e to the event list in the appropriate place,
   * prioritized by time.
   * 
   * @param e Sim.Event
   */
  public void insert(Event e) {
    // Create the item to go on the event list.
    ListItem item = new ListItem();
    item.data = e;

    // Find the appropriate place for the item in the event list,
    // and put it there,
    final double time = e.getTime();
    if (firstEvent == null || time < firstEvent.data.getTime()) {
      item.next = firstEvent;
      firstEvent = item;
    } else {
      ListItem behind = firstEvent;
      ListItem ahead = firstEvent.next;
      while (ahead != null && ahead.data.getTime() <= time) {
        behind = ahead;
        ahead = ahead.next;
      }
      behind.next = item;
      item.next = ahead;
    }
  }

  /**
   * takeNextEvent: remove the item at the head of the event list and
   * return it.
   * 
   * @return aim.Event
   */
  public Event takeNextEvent() {
    // precondition1 firstEvent I= null
    if (firstEvent == null) {
      System.out.println("Error! ran out of events ");
      return null;
    }

    Event eventToReturn = firstEvent.data;
    firstEvent = firstEvent.next;
    return eventToReturn;
  }

}

/**
 * Arrival: the class representing arrival events.
 */

class Arrival extends Event {
  /**
   * Constructor.
   * 
   * @param time double
   */
  public Arrival(double time) {
    super(time);
  }

  /**
   * doesCarBalk: decide whether a car should balk.
   * Deciding whether to balk is an activity that forms part of the arrival
   * event, so this method belongs among the event routines.
   *
   * The probability that a car leaves without buying gas {i.e., balks) grows
   * larger as the queue length gets larger, and grows smaller when the car
   * requires a greater number of litres of gas, so that:
   * (1) there is no balking if the queue length is zero, and
   * (2) otherwise, the probability of NOT balking is
   * (40 + litres)/(25 * (3 + queueLength))
   * 
   * @return boolean
   * @param litres      double
   * @param queueLength int
   */
  private boolean doesCarBalk(double litres, int queueLength) {
    return queueLength > 0
        && (Sim.balkingStream.nextDouble() > (Sim.balkA + litres) / (Sim.balkB * (Sim.balkC + queueLength)));
  }

  /**
   * interarrivalTime: the time uµtil the next arrival, from an exponential
   * distribution.
   * 
   * @return double
   */
  private double interarrivalTime() {
    return -Sim.meanInterarrivalTime
        * Math.log(Sim.arrivalStream.nextDouble());
  }

  /**
   * makeitHappen: arrival event routine.
   */
  public void makeItHappen() {
    // Create and initialize a new auto record.
    Car arrivingCar = new Car();
    Sim.stats.countArrival();
    final double litres = arrivingCar.getLitresNeeded();
    if (doesCarBalk(litres, Sim.carQueue.getQueueSize()))
      Sim.stats.accumBalk(litres);
    else {
      arrivingCar.setArrivalTime(Sim.simulationTime);
      if (Sim.pumpStand.aPumpIsAvailable())
        Sim.pumpStand.takeAvailablePump().startService(arrivingCar);
      else
        Sim.carQueue.insert(arrivingCar);
    }

    // Schedule the next arrival, reusing the current event object.
    setTime(Sim.simulationTime + interarrivalTime());
    Sim.eventList.insert(this);
  }
}

/**
 * Departure: the class representing departure events.
 */
class Departure extends Event {
  private Pump pump;

  /**
   * Constructor.
   * 
   * @param time double
   */
  public Departure(double time) {
    super(time);
  }

  /**
   * makeitHappen: departure event routine
   */
  public void makeItHappen() {
    // precondition: pump I= null && pump.getCarinService 1= null
    // Identify the departing car and collect statistics.
    Car departingCar = pump.getCarinService();
    Sim.stats.accumSale(departingCar.getLitresNeeded());

    // The car vanishes and the pump is free; can we serve another car?
    if (Sim.carQueue.getQueueSize() > 0)
      pump.startService(Sim.carQueue.takeFirstCar());
    else
      Sim.pumpStand.releasePump(pump);
  }

  /**
   * setPump: assign a pump to this arrival.
   * 
   * @param pump Sim.Pump
   */
  public void setPump(Pump pump) {
    this.pump = pump;
  }
}

/**
 * Report: the class representing reporting events.
 */
class Report extends Event {
  /**
   * Constructor.
   * 
   * @param time double
   */
  public Report(double time) {
    super(time);
  }

  /**
   * makeitHappen: interim reporting event routine
   */
  public void makeItHappen() {
    Sim.stats.snapshot();

    // Schedule the next interim report.
    setTime(Sim.simulationTime + Sim.reportInterval);
    Sim.eventList.insert(this);
  }

}

/**
 * EndOfSimulation: the class represent~na the final event that stops the
 * simulation
 */

class EndOfSimulation extends Event {
  /**
   * Constructor.
   * 
   * @param time double
   */
  public EndOfSimulation(double time) {
    super(time);
  }

  /**
   * makeItHappen: end of simulation event routine
   */
  public void makeItHappen() {
    Sim.stats.snapshot();
  }
}