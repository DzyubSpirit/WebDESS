import PetriObj.*;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

class Place {
    public int id;
    public String name;
    public int markers;
}

class Transition {
    public int id;
    public String name;
    public double delay;
    public double probability;
}

class Arc {
    public int placeId;
    public int transitionId;
    public boolean fromPlace;
    public int channels;
}

class NetData {
    public String name;
    public Place[] places;
    public Transition[] transitions;
    public Arc[] arcs;

    public PetriNet toPetriNet() {
        HashMap<Integer, Integer> placesIndexes = new HashMap<Integer, Integer>();
        PetriP[] pplaces = new PetriP[places.length];
        for (int i = 0; i < places.length; i++) {
            pplaces[i] = new PetriP(places[i].name, places[i].markers);
            placesIndexes.put(places[i].id, i);
        }

        HashMap<Integer, Integer> transitionsIndexes = new HashMap<Integer, Integer>();
        PetriT[] ptransitions = new PetriT[transitions.length];
        for (int i = 0; i < transitions.length; i++) {
            ptransitions[i] = new PetriT(transitions[i].name, transitions[i].delay, transitions[i].probability);
            transitionsIndexes.put(transitions[i].id, i);
        }

        int inCount = 0;
        for (Arc arc : arcs) {
            if (arc.fromPlace) inCount++;
        }
        ArcIn[] parcIn = new ArcIn[inCount];
        int iIn = 0;
        ArcOut[] parcOut = new ArcOut[arcs.length - inCount];
        int iOut = 0;
        for (Arc arc : arcs) {
            if (arc.fromPlace) {
                int placeIndex = placesIndexes.get(arc.placeId);
                int transitionIndex = transitionsIndexes.get(arc.transitionId);
                parcIn[iIn] = new ArcIn(placeIndex, transitionIndex, arc.channels);
                iIn++;
            } else {
                int placeIndex = placesIndexes.get(arc.placeId);
                int transitionIndex = transitionsIndexes.get(arc.transitionId);
                parcOut[iOut] = new ArcOut(transitionIndex, placeIndex, arc.channels);
                iOut++;
            }
        }
        return new PetriNet(name, pplaces, ptransitions, parcIn, parcOut);
    }
}

class NetSimulationData {
    public NetData net;
    public int duration;
}

class PlaceStatistics {
    public int min;
    public int max;
    public double avg;
}

class PlaceWithStats {
    public int markers;
    public PlaceStatistics stats;
}

class TransitionWithStats {
    public ArrayList<Double> outputTimesBuffer;
    public TransitionStatistics stats;
}

class TransitionStatistics {
    public double min;
    public double max;
    public double avg;
}

class NetStatistics {
    public PlaceWithStats[] places;
    public TransitionWithStats[] transitions;
}

class SimError {
    public String err;
}

public class LocalNetSimulator {
    private static NetStatistics toNetStatistics(PetriSim psim) {
        PetriP[] pplaces = psim.getListP();
        PlaceWithStats[] placesStats = new PlaceWithStats[pplaces.length];
        for (int i = 0; i < placesStats.length; i++) {
            placesStats[i] = new PlaceWithStats();
            placesStats[i] = new PlaceWithStats();
            placesStats[i].markers = pplaces[i].getMark();
            placesStats[i].stats = new PlaceStatistics();
            placesStats[i].stats.min = pplaces[i].getObservedMin();
            placesStats[i].stats.max = pplaces[i].getObservedMax();
            placesStats[i].stats.avg = pplaces[i].getMean();
        }

        PetriT[] ptransitions = psim.getListT();
        TransitionWithStats[] transitionsStats = new TransitionWithStats[ptransitions.length];
        for (int i = 0; i < transitionsStats.length; i++) {
            transitionsStats[i] = new TransitionWithStats();
            transitionsStats[i].stats = new TransitionStatistics();
            transitionsStats[i].stats.min = ptransitions[i].getObservedMin();
            transitionsStats[i].stats.max = ptransitions[i].getObservedMax();
            transitionsStats[i].stats.avg = ptransitions[i].getMean();
            transitionsStats[i].outputTimesBuffer = ptransitions[i].getTimeOut();
        }

        NetStatistics netStatistics = new NetStatistics();
        netStatistics.places = placesStats;
        netStatistics.transitions = transitionsStats;
        return netStatistics;
    }

    public static String run(String str) throws Exception {
        Gson g = new Gson();
        int num = 0;

        System.out.println(str);
        NetSimulationData data = g.fromJson(str, NetSimulationData.class);

        PrintStream out = System.out;
        System.setOut(new FakePrintStream());

        PetriNet pnet = data.net.toPetriNet();
        PetriSim psim = new PetriSim(pnet);
        PetriSim.setTimeMod(data.duration);
        psim.go();

        psim.doStatistica();

        NetStatistics stats = toNetStatistics(psim);
        System.setOut(out);
        return g.toJson(stats);
    }
}

class FakePrintStream extends PrintStream {

    private static StringBuffer printBuffer = new StringBuffer();


    public FakePrintStream() {
        super(new ByteArrayOutputStream());
    }

    @Override
    public void println(String string) {
        printBuffer.append(string).append("\n");
    }

    public String getPrintedString() {
        return printBuffer.toString();
    }

    public void clear() {
        printBuffer = new StringBuffer();
    }
}
