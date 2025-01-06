import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import java.util.Random;

public class SimTest {

  @Before
  public void setUp() {
    Sim.arrivalStream = new Random(1);
    Sim.litreStream = new Random(2);
    Sim.balkingStream = new Random(3);
    Sim.serviceStream = new Random(4);
    Sim.eventList = new EventList();
    Sim.carQueue = new CarQueue();
    Sim.pumpStand = new PumpStand(3);
    Sim.stats = new Statistics();
  }

  @Test
  public void testCarCreation() {
    Car car = new Car();
    assertTrue(car.getLitresNeeded() >= Sim.litresNeededMin);
    assertTrue(car.getLitresNeeded() <= Sim.litresNeededMin + Sim.litresNeededRange);
  }

  @Test
  public void testPumpServiceTime() {
    Car car = new Car();
    Pump pump = new Pump();
    pump.startService(car);
    assertNotNull(pump.getCarinService());
  }

  @Test
  public void testEventListInsertion() {
    Event event = new Arrival(10);
    Sim.eventList.insert(event);
    assertEquals(10, Sim.eventList.takeNextEvent().getTime());
  }

  @Test
  public void testCarQueueInsertion() {
    Car car = new Car();
    Sim.carQueue.insert(car);
    assertEquals(1, Sim.carQueue.getQueueSize());
  }

  @Test
  public void testCarQueueRemoval() {
    Car car = new Car();
    Sim.carQueue.insert(car);
    Car removedCar = Sim.carQueue.takeFirstCar();
    assertEquals(car, removedCar);
    assertEquals(0, Sim.carQueue.getQueueSize());
  }

  /**
   * @Test
   *       public void testStatisticsAccumulation() {
   *       Sim.stats.accumSale(20);
   *       Sim.stats.accumBalk(10);
   *       assertEquals(1, Sim.stats.customersServed);
   *       assertEquals(1, Sim.stats.balkingCustomers);
   *       assertEquals(20, Sim.stats.totalLitresSold);
   *       assertEquals(10, Sim.stats.totalLitresMissed);
   *       }
   * 
   * @Test
   *       public void testArrivalEvent() {
   *       Arrival arrival = new Arrival(0);
   *       arrival.makeItHappen();
   *       assertEquals(1, Sim.stats.totalArrivals);
   *       }
   * 
   * @Test
   *       public void testDepartureEvent() {
   *       Car car = new Car();
   *       Pump pump = new Pump();
   *       pump.startService(car);
   *       Departure departure = new Departure(0);
   *       departure.setPump(pump);
   *       departure.makeItHappen();
   *       assertEquals(1, Sim.stats.customersServed);
   *       }
   * 
   */

  @Test
  public void testReportEvent() {
    Report report = new Report(0);
    report.makeItHappen();
    // No assertions needed, just ensure no exceptions are thrown
  }

  @Test
  public void testEndOfSimulationEvent() {
    EndOfSimulation endOfSimulation = new EndOfSimulation(0);
    endOfSimulation.makeItHappen();
    // No assertions needed, just ensure no exceptions are thrown
  }
}