import org.junit.Test;
import static org.junit.Assert.*;

public class TestDeterministicSimulationModels {
  @Test
  public void testDeterministicSimulationModels() {
    DeterministicSimulationModels deterministicSimulationModels = new DeterministicSimulationModels();
    deterministicSimulationModels.DeterministicSimulate();
    assertEquals(0, deterministicSimulationModels.loan, 0.001);
  }
}
