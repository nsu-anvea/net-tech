package rip;

public class RIP {
    public static void main(String[] args) {
        Router r1 = new Router("R1");
        Router r2 = new Router("R2");
        Router r3 = new Router("R3");
        Router r4 = new Router("R4");
        Router r5 = new Router("R5");

        r1.addNeighbor(r2, 1);
        r1.addNeighbor(r3, 1);
        r2.addNeighbor(r4, 1);
        r3.addNeighbor(r4, 4);
        r4.addNeighbor(r5, 1);

        r1.start();
        r2.start();
        r3.start();
        r4.start();
        r5.start();

        System.out.println("Initial routing tables:");
        r1.printRoutingTable();
        r2.printRoutingTable();
        r3.printRoutingTable();
        r4.printRoutingTable();
        r5.printRoutingTable();

        try {
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        r1.stop();
        r2.stop();
        r3.stop();
        r4.stop();
        r5.stop();

        System.out.println("\n\tFinal routing tables:");
        r1.printRoutingTable();
        r2.printRoutingTable();
        r3.printRoutingTable();
        r4.printRoutingTable();
        r5.printRoutingTable();
    }
}