package ghirl.learn;

import java.util.*;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.util.gui.*;
import ghirl.graph.*;
import ghirl.util.*;

/**
 * An implementation of a Minorthird Instance that is based on a Distribution
 * over features. 
 */
public class DistributionInstance implements Instance, Visible
{
    private GraphId id;
    private Distribution d;
    private String subpop;

    public DistributionInstance(GraphId id,String subpop,Distribution d)
    { 
	    this.id=id; this.subpop=subpop; this.d=d;
    }

    public void add(double weight, Object obj){
        d.add( weight, obj);
    }
    public Object getSource() { return id; }
    public String getSubpopulationId() { return subpop; }
    
    // WARNING: these were changed from Feature.Looper on 23 Nov 2009 by Katie.
    // As far as I know, we have no tests for them -- use at your own risk, and if you fix any bugs, 
    // commit your fixes so others may benefit!
    public Iterator<Feature> featureIterator() { return (Iterator<Feature>) d.iterator(); }
    public Iterator<Feature> numericFeatureIterator() { return (Iterator<Feature>) d.iterator(); }
    public Iterator<Feature> binaryFeatureIterator() {return (Iterator<Feature>) new HashSet<Feature>().iterator(); }
	public int numFeatures() { return d.size(); }
	
    public double getWeight(Feature f) { return d.getProbability(f);}
    public Viewer toGUI() { return new GUI.InstanceViewer(this); }
    public String toString() { return "[DistInstance "+id+","+subpop+": "+d+"]"; }
}
