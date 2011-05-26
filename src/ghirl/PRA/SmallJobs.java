package ghirl.PRA;

import edu.cmu.lti.util.file.FFile;
import edu.cmu.lti.util.run.Param;
import edu.cmu.lti.util.run.StopWatch;
import edu.cmu.lti.util.system.FSystem;
import edu.cmu.pra.CTag;
import edu.cmu.pra.learner.Query;
import edu.cmu.pra.model.ModelPathRank;
import edu.cmu.pra.model.PRAModel;
import ghirl.graph.CompactGraph;
import ghirl.graph.ICompact;
import ghirl.graph.PersistantCompactTokyoGraph;
import ghirl.graph.SparseCompactGraph;
import ghirl.util.Config;

public class SmallJobs {
	
/*
 * 		net.loadTaskFile(fnSchema);
		net.schema.onLoadSchema(net);			
		net.createPathTrees(graph);
		net.initWeights();
		net.loadPathWeights(fnWeight);
 */
	/*		walker.setNumSteps(1000); 
	walker.setNumLevels(3);
	//walker.setSamplingPolicy(false);
  walker.setGraph(g);
  walker.setUniformEdgeWeights(); 
  walker.reset();*/
	
	public static String testFile="scenarios.Woolford_JL.predict";
	public static String TESTROOT="tests";
	
	

	public static ICompact getTCGraph()throws Exception{		
		PersistantCompactTokyoGraph graph;
		FSystem.printMemoryTime();

		
		ghirl.util.Config.setProperty(Config.DBDIR, "..");
		if (FFile.exist("../"+DB+"_compactTokyo") ){//&& 1==2){
			graph = new PersistantCompactTokyoGraph(DB,'r');
		}
		else{
			graph = new PersistantCompactTokyoGraph(DB,'w');
			graph.load("../"+DB);//tests
		}
		FSystem.printMemoryTime();

		return graph;
	}


	
	
	public static void testPCRW()throws Exception{		

		PRAModel net=loadModel();

		ICompact graph=getCGraph();	//getCGraph();
		
		FFile.mkdirs("result"+net.p.code);
		
		//Walker walker= new PathWalker(graph, net);
    StopWatch sw= new StopWatch();
		for (Query q : net.loadQuery("scenarios.WJL")){
			net.predict(q);
			//System.out.println("nPart="+q.mResult.size());
		}
		sw.stopAndPrint();

		
		return;
	}
	
	
	
	public static PRAModel loadModel(){
		Param.overwriteFrom("conf");//cite.conf
		Param.overwrite("dataFolder=./");
		PRAModel net=new ModelPathRank();//,"YA-Py.WJ"./ );graph
		
		net.loadModel("model.cite");//"cite.model"
		return net;
	}
	public static String DB="Yeast2.txtLink";//"Yeast2.cite";

	public static ICompact getCGraph()throws Exception{
		FSystem.printMemoryTime();

		SparseCompactGraph graph = new SparseCompactGraph();
		graph.load("../"+DB);
		graph.loadMMGraphIdx();
		
				
		FSystem.printMemoryTime();
		
		augamentGraph(graph);
		return graph;
		

	}

	public static void augamentGraph(CompactGraph graph ){
		// 
		/*int idxA=graph.getNodeIdx("author", "Woolford_JL");
		SetI m=graph.getNodeIdx("paper"
			, "3282992 2673535 3058476 2179050".split(" "));
		graph.AddExtraLinks("Read", idxA, m);*/
		
		graph.AddExtraLinks("CRead"
			, "author", "Woolford_JL"
			,"paper"	, "3282992 2673535 3058476 2179050".split(" "));
	}
	public static void testPrediction()throws Exception{
		
		PRAModel net=loadModel();
		
		ICompact g=getCGraph();	//getCGraph();
		net.setGraph(g);
		
		//Walker walker= new PathWalker(graph, net);
		
		Query q=net.parseQuery("2010,2009,Woolford_JL,");
		
		net.predict(q);

		//System.out.println("q="+q.mResult);

		System.out.println("path width="+q.A.getVI(CTag.size));
/*
		Distribution d=new 
			CompactImmutableArrayDistribution(q.mResult, g);		
		System.out.println("d="+d.toMapID());
		
		WeightedTextGraph wtg=new WeightedTextGraph(d,g);
		Distribution r = wtg.getNodeDist();
		System.out.println("r="+r.toMapID());	
		
		int j=1;
		Iterator i = r.orderedIterator();
		for (Object o=null; i.hasNext(); ++j) {
				o = i.next();
				String id = o.toString().trim();
				double score = r.getLastWeight();
				System.out.println(id+"  "+score);
		}
		*/
		
		return;
	}
	public static void main(String args[]) {
		try{		
			//System.out.print( FFile.getFilePath("/usb2/nlao/software/nies/nies/PRA/"));
			testPrediction();
			//testPCRW();

		} catch (Exception ex) {
			System.err.print(ex);
			ex.printStackTrace();
		}
	}
}