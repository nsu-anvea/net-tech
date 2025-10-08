//package rip;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//
//public class Router {
//    private final String name;
//    private final Map<String, RoutingTableEntry> routingTable;
//    private final Map<String, Router> neighbors;
//    private final ScheduledExecutorService scheduler;
//    private boolean isRunning;
//    private static final int UPDATE_INTERVAL = 5;
//    private static final int INFINITY = 16;
//
//    public Router(String name) {
//        this.name = name;
//        this.routingTable = new ConcurrentHashMap<>();
//        this.neighbors = new ConcurrentHashMap<>();
//        this.scheduler = Executors.newSingleThreadScheduledExecutor();
//
//        this.routingTable.put(name, new RoutingTableEntry(0, null));
//        this.isRunning = false;
//    }
//
//    public void addNeighbor(Router neighbor, int cost) {
//        neighbors.put(neighbor.getName(), neighbor);
//        routingTable.put(neighbor.getName(), new RoutingTableEntry(cost, neighbor));
//        neighbor.neighbors.put(this.name, this);
//        neighbor.routingTable.put(this.name, new RoutingTableEntry(cost, this));
//    }
//
//    public void start() {
//        if (!isRunning) {
//            isRunning = true;
//            scheduler.scheduleAtFixedRate(this::sendUpdates, UPDATE_INTERVAL, UPDATE_INTERVAL, TimeUnit.SECONDS);
//        }
//    }
//
//    public void stop() {
//        isRunning = false;
//        scheduler.shutdown();
//    }
//
//    private void sendUpdates() {
//        System.out.println(name + " is sending updates to neighbors");
//        for (Router neighbor : neighbors.values()) {
//            // Apply split horizon: don't send routes back to the neighbor we learned them from
//            Map<String, RoutingTableEntry> filteredTable = new HashMap<>();
//            for (Map.Entry<String, RoutingTableEntry> entry : routingTable.entrySet()) {
//                String destination = entry.getKey();
//                RoutingTableEntry route = entry.getValue();
//
//                // Split horizon with poisoned reverse
//                if (route.getNextHop() == neighbor) {
//                    // Send the route with infinity metric
//                    filteredTable.put(destination, new RoutingTableEntry(INFINITY, null));
//                } else {
//                    filteredTable.put(destination, route);
//                }
//            }
//            neighbor.receiveUpdate(this, filteredTable);
//        }
//    }
//
//    private void receiveUpdate(Router sender, Map<String, RoutingTableEntry> senderTable) {
//        boolean updated = false;
//
//        for (Map.Entry<String, RoutingTableEntry> entry : senderTable.entrySet()) {
//            String destination = entry.getKey();
//            RoutingTableEntry senderRoute = entry.getValue();
//            int newCost = senderRoute.getCost() + getLinkCost(sender);
//
//            if (destination.equals(this.name)) continue;
//
//            if (senderRoute.getCost() >= INFINITY) {
//                RoutingTableEntry existingRoute = routingTable.get(destination);
//                if (existingRoute != null && existingRoute.getNextHop() == sender) {
//                    routingTable.put(destination, new RoutingTableEntry(INFINITY, null));
//                    updated = true;
//                }
//                continue;
//            }
//
//            RoutingTableEntry existingRoute = routingTable.get(destination);
//
//            if (existingRoute == null) {
//                routingTable.put(destination, new RoutingTableEntry(newCost, sender));
//                updated = true;
//            } else if (existingRoute.getNextHop() == sender) {
//                if (newCost != existingRoute.getCost()) {
//                    routingTable.put(destination, new RoutingTableEntry(newCost, sender));
//                    updated = true;
//                }
//            } else if (newCost < existingRoute.getCost()) {
//                routingTable.put(destination, new RoutingTableEntry(newCost, sender));
//                updated = true;
//            }
//        }
//
//        if (updated) {
//            System.out.println(name + " updated routing table after receiving update from " + sender.getName());
//            printRoutingTable();
//        }
//    }
//
//    private int getLinkCost(Router neighbor) {
//        return 1;
//    }
//
//    public void printRoutingTable() {
//        synchronized (System.out) {
//            System.out.println("Routing table for " + name + ":");
//            System.out.println("Destination\tCost\tNext Hop");
//            for (Map.Entry<String, RoutingTableEntry> entry : routingTable.entrySet()) {
//                String dest = entry.getKey();
//                RoutingTableEntry route = entry.getValue();
//                String nextHop = (route.getNextHop() == null) ? "-" : route.getNextHop().getName();
//                System.out.println(dest + "\t\t" + route.getCost() + "\t" + nextHop);
//            }
//            System.out.println();
//        }
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public Map<String, RoutingTableEntry> getRoutingTable() {
//        return new HashMap<>(routingTable);
//    }
//
//    private static class RoutingTableEntry {
//        private int cost;
//        private Router nextHop;
//
//        public RoutingTableEntry(int cost, Router nextHop) {
//            this.cost = cost;
//            this.nextHop = nextHop;
//        }
//
//        public int getCost() {
//            return cost;
//        }
//
//        public Router getNextHop() {
//            return nextHop;
//        }
//    }
//}

package rip;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Router {
    private final String name;
    private final Map<String, RoutingTableEntry> routingTable;
    private final Map<String, NeighborInfo> neighbors;  // Changed to store NeighborInfo instead of Router directly
    private final ScheduledExecutorService scheduler;
    private boolean isRunning;
    private static final int UPDATE_INTERVAL = 5;
    private static final int INFINITY = 16;

    public Router(String name) {
        this.name = name;
        this.routingTable = new ConcurrentHashMap<>();
        this.neighbors = new ConcurrentHashMap<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        this.routingTable.put(name, new RoutingTableEntry(0, null));
        this.isRunning = false;
    }

    public void addNeighbor(Router neighbor, int cost) {
        neighbors.put(neighbor.getName(), new NeighborInfo(neighbor, cost));
        routingTable.put(neighbor.getName(), new RoutingTableEntry(cost, neighbor));

        neighbor.neighbors.put(this.name, new NeighborInfo(this, cost));
        neighbor.routingTable.put(this.name, new RoutingTableEntry(cost, this));
    }

    public void start() {
        if (!isRunning) {
            isRunning = true;
            scheduler.scheduleAtFixedRate(this::sendUpdates, UPDATE_INTERVAL, UPDATE_INTERVAL, TimeUnit.SECONDS);
        }
    }

    public void stop() {
        isRunning = false;
        scheduler.shutdown();
    }

    private void sendUpdates() {
        System.out.println(name + " is sending updates to neighbors");
        for (NeighborInfo neighborInfo : neighbors.values()) {
            Router neighbor = neighborInfo.getRouter();
            // Apply split horizon: don't send routes back to the neighbor we learned them from
            Map<String, RoutingTableEntry> filteredTable = new HashMap<>();
            for (Map.Entry<String, RoutingTableEntry> entry : routingTable.entrySet()) {
                String destination = entry.getKey();
                RoutingTableEntry route = entry.getValue();

                // Split horizon with poisoned reverse
                if (route.getNextHop() == neighbor) {
                    // Send the route with infinity metric
                    filteredTable.put(destination, new RoutingTableEntry(INFINITY, null));
                } else {
                    filteredTable.put(destination, route);
                }
            }
            neighbor.receiveUpdate(this, filteredTable);
        }
    }

    private void receiveUpdate(Router sender, Map<String, RoutingTableEntry> senderTable) {
        boolean updated = false;

        for (Map.Entry<String, RoutingTableEntry> entry : senderTable.entrySet()) {
            String destination = entry.getKey();
            RoutingTableEntry senderRoute = entry.getValue();
            int linkCost = getLinkCost(sender);
            int newCost = senderRoute.getCost() + linkCost;

            if (destination.equals(this.name)) continue;

            if (senderRoute.getCost() >= INFINITY) {
                RoutingTableEntry existingRoute = routingTable.get(destination);
                if (existingRoute != null && existingRoute.getNextHop() == sender) {
                    routingTable.put(destination, new RoutingTableEntry(INFINITY, null));
                    updated = true;
                }
                continue;
            }

            RoutingTableEntry existingRoute = routingTable.get(destination);

            if (existingRoute == null) {
                routingTable.put(destination, new RoutingTableEntry(newCost, sender));
                updated = true;
            } else if (existingRoute.getNextHop() == sender) {
                if (newCost != existingRoute.getCost()) {
                    routingTable.put(destination, new RoutingTableEntry(newCost, sender));
                    updated = true;
                }
            } else if (newCost < existingRoute.getCost()) {
                routingTable.put(destination, new RoutingTableEntry(newCost, sender));
                updated = true;
            }
        }

        if (updated) {
            System.out.println(name + " updated routing table after receiving update from " + sender.getName());
            printRoutingTable();
        }
    }

    private int getLinkCost(Router neighbor) {
        NeighborInfo neighborInfo = neighbors.get(neighbor.getName());
        return neighborInfo != null ? neighborInfo.getCost() : INFINITY;
    }

    public void printRoutingTable() {
        synchronized (System.out) {
            System.out.println("Routing table for " + name + ":");
            System.out.println("Destination\tCost\tNext Hop");
            for (Map.Entry<String, RoutingTableEntry> entry : routingTable.entrySet()) {
                String dest = entry.getKey();
                RoutingTableEntry route = entry.getValue();
                String nextHop = (route.getNextHop() == null) ? "-" : route.getNextHop().getName();
                System.out.println(dest + "\t\t" + route.getCost() + "\t" + nextHop);
            }
            System.out.println();
        }
    }

    public String getName() {
        return name;
    }

    public Map<String, RoutingTableEntry> getRoutingTable() {
        return new HashMap<>(routingTable);
    }

    private static class RoutingTableEntry {
        private int cost;
        private Router nextHop;

        public RoutingTableEntry(int cost, Router nextHop) {
            this.cost = cost;
            this.nextHop = nextHop;
        }

        public int getCost() {
            return cost;
        }

        public Router getNextHop() {
            return nextHop;
        }
    }

    private static class NeighborInfo {
        private final Router router;
        private final int cost;

        public NeighborInfo(Router router, int cost) {
            this.router = router;
            this.cost = cost;
        }

        public Router getRouter() {
            return router;
        }

        public int getCost() {
            return cost;
        }
    }
}