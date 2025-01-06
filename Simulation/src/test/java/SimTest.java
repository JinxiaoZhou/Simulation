import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.After;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
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
    String input = "20000\n200000\n3\n1\n2\n3\n3\n";
    System.setIn(new java.io.ByteArrayInputStream(input.getBytes()));

    Sim.main(new String[] {});

    String output = outContent.toString();
    System.out.println("Captured Output:\n" + output); // Print the captured output for debugging

    // Add assertions to verify the expected output
    if (!output.contains("This simulation run uses 3 pumps")) {
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
  public void testEventListInsertion() {
    Event Arr = new Arrival(15);
    Event Dep = new Departure(10);
    Sim.eventList.insert(Arr);
    Sim.eventList.insert(Dep);
    Sim.eventList.takeNextEvent();
    assertEquals(15, Sim.eventList.takeNextEvent().getTime(), 0.0001);
  }

  @Test
  public void testCarQueueInsertion() {
    Car car1 = new Car();
    Car car2 = new Car();
    Sim.carQueue.insert(car1);
    Sim.carQueue.insert(car2);
    assertEquals(2, Sim.carQueue.getQueueSize());
  }

  @Test
  public void testCarQueueRemoval() {
    Car car1 = new Car();
    Car car2 = new Car();
    Sim.carQueue.insert(car1);
    Sim.carQueue.insert(car2);
    Car removedCar = Sim.carQueue.takeFirstCar();
    assertEquals(car1, removedCar);
    assertEquals(1, Sim.carQueue.getQueueSize());
  }
}