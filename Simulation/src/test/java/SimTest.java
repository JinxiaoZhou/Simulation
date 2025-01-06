import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.After;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.Random;

public class SimTest {

  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private final PrintStream originalOut = System.out;

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

    System.setOut(new PrintStream(outContent));

  }

  @After
  public void tearDown() {
    // Reset System.out to its original state
    System.setOut(originalOut);
  }

  @Test
  public void testMain() throws Exception {
    String input = "100\n50000\n2\n1\n2\n3\n3\n";
    BufferedReader in = new BufferedReader(new StringReader(input));
    System.setIn(new java.io.ByteArrayInputStream(input.getBytes()));

    Sim.main(new String[] {});

    String output = outContent.toString();
    System.out.println("Captured Output:\n" + output); // Print the captured output for debugging

    // Add assertions to verify the expected output
    if (!output.contains("This simulation run uses 2 pumps")) {
      fail("Output should contain the number of pumps");
    }
    if (!output.contains("and the following random number seeds:")) {
      fail("Output should contain the random number seeds");
    }
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
    assertEquals(pump.getCarinService(), car);
  }

  @Test
  public void testEventListInsertion() {
    Event event = new Arrival(10);
    Sim.eventList.insert(event);
    assertEquals(10, Sim.eventList.takeNextEvent().getTime(), 0.0001);
  }

  @Test
  public void testCarQueueInsertion() {
    Car car = new Car();
    Car car1 = new Car();
    Sim.carQueue.insert(car);
    Sim.carQueue.insert(car1);
    assertEquals(2, Sim.carQueue.getQueueSize());
  }

  @Test
  public void testCarQueueRemoval() {
    Car car = new Car();
    Sim.carQueue.insert(car);
    Car removedCar = Sim.carQueue.takeFirstCar();
    assertEquals(car, removedCar);
    assertEquals(0, Sim.carQueue.getQueueSize());
  }

  @Test
  public void testPumpStand() {
    PumpStand pumpStand = new PumpStand(2);
    assertEquals(2, pumpStand.getNumberOfPumps());
  }

  @Test
  public void testArrval() {
    Arrival arrival = new Arrival(0);
    arrival.makeItHappen();
    // No assertions needed, just ensure no exceptions are thrown
  }

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