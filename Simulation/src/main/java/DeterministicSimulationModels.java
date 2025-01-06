public class DeterministicSimulationModels {
    final double rate = 1.10 / 12;
    final double payment = 10;
    double loan = 100;
    int month = 0;

    public void DeterministicSimulate() {
        System.out.printf("%15s%15s%14s\n", "end of month", "loan balance", "payment due:");
        System.out.printf("%12d%15.2f\n", month, loan);

        while (loan > 0) {
            double thispayment = payment;
            month++;
            loan = loan * (1 + rate);
            if (thispayment > loan) {
                thispayment = loan;
            }
            loan = loan - thispayment;
            System.out.printf("%12d%15 .2f%14 .2f\n", month, loan, thispayment);
        }
    }
}
