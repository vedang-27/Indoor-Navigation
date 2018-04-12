package com.example.vedang.maptestapp;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vedang on 24/3/18.
 */

public class Vertex implements Comparable<Vertex>{
    public final String name;
    //public Edge[] adjacencies;
    public List<Edge> adjacencies = new ArrayList<Edge>();
    public double minDistance = Double.POSITIVE_INFINITY;
    public Vertex previous;
    public Vertex(String argName) { name = argName; }
    public String toString() { return name; }
    public int compareTo(Vertex other)
    {
        return Double.compare(minDistance, other.minDistance);
    }

}
